package com.wethrive.i23_0761_i23_0765

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity9 : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnAttach: ImageView
    private lateinit var vanishModeToggle: ImageView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private var isVanishModeEnabled = false

    // replace with your session user id retrieval
    private val currentUserId: String by lazy {
        getSharedPreferences("user_session", MODE_PRIVATE).getString("userId", "user_a") ?: "user_a"
    }
    private lateinit var receiverId: String
    private lateinit var chatId: String

    private val sendQueueDbHelper: SendQueueDbHelper by lazy { SendQueueDbHelper(this) }
    private val messagesDbHelper: MessagesDbHelper by lazy { MessagesDbHelper.getInstance(this) }

    private val handler = Handler(Looper.getMainLooper())
    private val retryIntervalMs = 3_000L  // Poll every 3 seconds

    // base url - change to your server
    private val BASE_URL = "http://sociallyah.atwebpages.com/"

    private val IMAGE_REQ = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        receiverId = intent.getStringExtra("receiverId") ?: "user_b"
        chatId = if (currentUserId < receiverId) currentUserId + "_" + receiverId else receiverId + "_" + currentUserId

        recycler = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        vanishModeToggle = findViewById(R.id.btnVanishMode)

        adapter = ChatAdapter(messages, currentUserId,
            onLongPress = { /* show menu if needed */ },
            onRetryClick = { msg -> retrySendSingle(msg) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnSend.setOnClickListener { onSendClicked() }
        btnAttach.setOnClickListener { pickImage() }
        vanishModeToggle.setOnClickListener { toggleVanishMode() }

        // Set up back arrow
        findViewById<ImageView>(R.id.back_arrow)?.setOnClickListener {
            finish()
        }

        // Long press on back arrow to clear local cache (for debugging/testing)
        findViewById<ImageView>(R.id.back_arrow)?.setOnLongClickListener {
            clearLocalCache()
            true
        }

        // Load user profile (name and dp)
        loadUserProfile()

        // Load messages from local database first
        loadMessagesFromDb()

        // initial fetch
        fetchMessages(initial = true)

        // listen to network up events
        registerReceiver(netReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onResume() {
        super.onResume()
        // Fetch messages immediately when chat becomes visible
        Log.d("MessagingActivity", "=== CHAT RESUMED - Fetching messages ===")
        fetchMessages(initial = false)
        // Start polling
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(fetchAndRetryRunnable, retryIntervalMs)
    }

    override fun onPause() {
        super.onPause()
        // Stop polling when chat is not visible
        Log.d("MessagingActivity", "=== CHAT PAUSED - Stopping polling ===")
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Delete vanish mode messages when closing the chat
        deleteVanishModeMessages()

        try { unregisterReceiver(netReceiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }

    // ----------------- Sending flow -----------------

    private fun onSendClicked() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        hideKeyboard()
        val localId = "local_" + UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        val msg = Message(
            messageId = localId,
            chatId = chatId,
            senderId = currentUserId,
            receiverId = receiverId,
            text = text,
            timestamp = ts,
            isPending = true,
            deliveryState = 0,
            vanishMode = isVanishModeEnabled
        )

        // show optimistic
        adapter.addOrUpdateMessage(msg)
        recycler.scrollToPosition(messages.lastIndex)
        etMessage.setText("")

        // save to local database
        messagesDbHelper.insertOrUpdateMessage(msg)

        // enqueue locally
        sendQueueDbHelper.insertMessage(msg)

        // attempt immediate send
        if (isNetworkAvailable()) sendPendingQueueOnce()
        else Toast.makeText(this, "Queued (offline)", Toast.LENGTH_SHORT).show()
    }

    private fun retrySendSingle(msg: Message) {
        // user tapped retry on failed message
        // ensure it's in DB, then attempt send
        sendQueueDbHelper.insertMessage(msg)
        if (isNetworkAvailable()) sendPendingQueueOnce()
        else Toast.makeText(this, "Still offline", Toast.LENGTH_SHORT).show()
    }

    private fun sendPendingQueueOnce() {
        val pending = sendQueueDbHelper.getAllPendingMessages()
        if (pending.isEmpty()) return
        for (m in pending) {
            postMessageToServer(m) { ok, serverMsgJson ->
                if (ok) {
                    // remove from queue
                    sendQueueDbHelper.deleteMessage(m.messageId)

                    // build server message object (serverMsgJson may be null)
                    val serverMsg = serverMsgJson?.let { parseServerMessage(it) } ?: m.copy(isPending = false, deliveryState = 1)

                    // CRITICAL FIX: If server returned a different message ID, we need to:
                    // 1. Delete the old local message from database
                    // 2. Replace it in the UI with the server message
                    if (serverMsg.messageId != m.messageId) {
                        Log.d("MessagingActivity", "Replacing local ID ${m.messageId} with server ID ${serverMsg.messageId}")
                        
                        // Delete old local message from database
                        messagesDbHelper.deleteMessage(m.messageId)
                        
                        // Remove old message from UI and add new one
                        val idx = messages.indexOfFirst { it.messageId == m.messageId }
                        if (idx >= 0) {
                            messages[idx] = serverMsg
                            adapter.notifyItemChanged(idx)
                        } else {
                            adapter.addOrUpdateMessage(serverMsg)
                        }
                    } else {
                        adapter.addOrUpdateMessage(serverMsg)
                        adapter.updateDeliveryState(m.messageId, 1)
                    }

                    // Save server message to local database
                    messagesDbHelper.insertOrUpdateMessage(serverMsg)
                } else {
                    adapter.updateDeliveryState(m.messageId, 2)
                }
            }
        }
    }

    private fun postMessageToServer(msg: Message, cb: (Boolean, JSONObject?) -> Unit) {
        val url = BASE_URL + "messages_send.php"
        val req = object : StringRequest(
            Method.POST, url,
            { resp ->
                try {
                    Log.d("MessagingActivity", "Send response: $resp")
                    val j = JSONObject(resp)
                    val ok = j.optInt("status", 0) == 1
                    cb(ok, if (ok) j.optJSONObject("message") else null)
                } catch (e: Exception) {
                    Log.e("MessagingActivity", "Parse error: ${e.message}")
                    cb(false, null)
                }
            },
            { err -> 
                Log.e("MessagingActivity", "Send error: ${err.message}")
                cb(false, null) 
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["message_id"] = msg.messageId
                map["chat_id"] = msg.chatId
                map["sender_id"] = msg.senderId
                map["receiver_id"] = msg.receiverId
                map["text"] = msg.text ?: ""
                map["image"] = msg.imageBase64 ?: ""
                map["post_id"] = msg.postId ?: ""
                map["vanish_mode"] = if (msg.vanishMode) "1" else "0"
                return map
            }
            
            // Disable Volley caching for real-time sync
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store"
                headers["Pragma"] = "no-cache"
                return headers
            }
        }
        req.setShouldCache(false)
        Volley.newRequestQueue(this).add(req)
    }

    // ----------------- Fetching flow -----------------

    private fun fetchMessages(initial: Boolean = false) {
        val url = BASE_URL + "messages_fetch.php"
        Log.d("MessagingActivity", ">>> Fetching messages for chat: $chatId (currentUserId: $currentUserId, receiverId: $receiverId)")

        val req = object : StringRequest(
            Method.POST, url,
            { resp ->
                try {
                    Log.d("MessagingActivity", ">>> Server response received: ${resp.take(500)}")
                    val j = JSONObject(resp)
                    if (j.optInt("status", 0) == 1) {
                        val arr = j.optJSONArray("messages") ?: JSONArray()
                        val serverCount = j.optInt("count", 0)
                        Log.d("MessagingActivity", ">>> Server returned $serverCount messages (array length: ${arr.length()})")

                        // Get all server message IDs
                        val serverMessageIds = mutableSetOf<String>()

                        val incoming = mutableListOf<Message>()
                        val existingIds = messages.mapNotNull { it.messageId }.toHashSet()
                        
                        // Also track local_ IDs that might be pending
                        val pendingLocalIds = messages.filter { it.messageId.startsWith("local_") }.map { it.messageId }.toSet()

                        Log.d("MessagingActivity", ">>> Currently have ${existingIds.size} messages in UI, ${pendingLocalIds.size} pending")

                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val msgId = o.optString("message_id")
                            val sender = o.optString("sender_id")
                            val receiver = o.optString("receiver_id")
                            val text = o.optString("text")
                            val ts = o.optLong("timestamp", System.currentTimeMillis())

                            Log.d("MessagingActivity", ">>> Processing msg[$i]: id=$msgId, from=$sender, to=$receiver, text=${text.take(20)}")

                            // Track server message IDs
                            if (msgId.isNotBlank()) serverMessageIds.add(msgId)

                            // skip duplicates by message ID
                            if (msgId.isNotBlank() && existingIds.contains(msgId)) {
                                Log.d("MessagingActivity", ">>> SKIPPED: Already have this message ID")
                                continue
                            }

                            // Skip if this is our own message that's still pending (has local_ ID)
                            // We check by matching sender + text + approximate timestamp
                            if (sender == currentUserId && pendingLocalIds.isNotEmpty()) {
                                val matchingPending = messages.find { 
                                    it.messageId.startsWith("local_") && 
                                    it.senderId == sender && 
                                    it.text == text &&
                                    kotlin.math.abs(it.timestamp - ts) < 60000 // within 1 minute
                                }
                                if (matchingPending != null) {
                                    Log.d("MessagingActivity", ">>> SKIPPED: This is our pending message (local: ${matchingPending.messageId})")
                                    continue
                                }
                            }

                            val m = Message(
                                messageId = msgId,
                                chatId = chatId,
                                senderId = sender,
                                receiverId = receiver,
                                text = text,
                                imageUrl = o.optString("image"),
                                postId = o.optString("post_id"),
                                timestamp = ts,
                                edited = o.optInt("edited",0)==1,
                                deleted = o.optInt("deleted",0)==1,
                                vanishMode = o.optInt("vanish_mode",0)==1,
                                seen = o.optInt("seen",0)==1,
                                deliveryState = 1,
                                isPending = false
                            )
                            incoming.add(m)
                            Log.d("MessagingActivity", ">>> ADDED to incoming list: ${m.messageId}")
                        }

                        Log.d("MessagingActivity", ">>> ${incoming.size} NEW messages to display")

                        if (incoming.isNotEmpty()) {
                            for (m in incoming) {
                                // Mark message as seen if current user is receiver
                                val seenMessage = if (m.receiverId == currentUserId) m.copy(seen = true) else m
                                // Save to local database
                                messagesDbHelper.insertOrUpdateMessage(seenMessage)
                                // Add to UI
                                adapter.addOrUpdateMessage(seenMessage)
                                Log.d("MessagingActivity", ">>> Added message to UI: ${m.messageId} - ${m.text?.take(20)}")
                            }

                            // Auto-scroll to show new messages
                            if (messages.isNotEmpty()) {
                                recycler.smoothScrollToPosition(messages.lastIndex)
                            }

                            // mark seen on server too (only if we received new messages for us)
                            val receivedForMe = incoming.any { it.receiverId == currentUserId }
                            if (receivedForMe) {
                                markMessagesSeenOnServer()
                            }
                        } else {
                            Log.d("MessagingActivity", ">>> No new messages to display")
                        }

                        // Sync: Remove messages from local DB that don't exist on server
                        syncLocalDbWithServer(serverMessageIds)
                    } else {
                        Log.w("MessagingActivity", ">>> Server returned status != 1: $resp")
                    }
                } catch (e: Exception) {
                    Log.e("MessagingActivity", ">>> FETCH ERROR: ${e.message}", e)
                }
            },
            { err -> Log.e("MessagingActivity", ">>> NETWORK ERROR: ${err.message}", err) }
        ) {
            override fun getParams(): MutableMap<String, String> =
                hashMapOf("chat_id" to chatId, "user_id" to currentUserId)
            
            // Disable Volley caching for real-time sync
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store"
                headers["Pragma"] = "no-cache"
                return headers
            }
        }
        req.setShouldCache(false)
        Volley.newRequestQueue(this).add(req)
    }

    private fun markMessagesSeenOnServer() {
        val url = BASE_URL + "messages_seen.php"
        val req = object : StringRequest(Method.POST, url,
            { resp -> Log.d("MessagingActivity", "Mark seen response: $resp") }, 
            { err -> Log.w("MessagingActivity", "Mark seen error: ${err.message}") }) {
            override fun getParams(): MutableMap<String, String> =
                hashMapOf("chat_id" to chatId, "user_id" to currentUserId)  // FIX: Use currentUserId, not receiverId
            
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store"
                return headers
            }
        }
        req.setShouldCache(false)
        Volley.newRequestQueue(this).add(req)
    }

    private fun syncLocalDbWithServer(serverMessageIds: Set<String>) {
        // Get all local messages for this chat
        val localMessages = messagesDbHelper.getMessagesForChat(chatId)

        Log.d("MessagingActivity", "Sync: Server has ${serverMessageIds.size} messages, Local DB has ${localMessages.size} messages")

        // Find messages that exist locally but not on server
        val messagesToDelete = localMessages.filter {
            // Keep pending messages (still being sent)
            if (it.isPending) return@filter false

            // If message has a blank ID, keep it for now
            if (it.messageId.isBlank()) return@filter false

            // If it's a local-only message that's still pending/sending, keep it
            if (it.messageId.startsWith("local_") && it.deliveryState == 0) return@filter false

            // Otherwise, if it's not on the server, mark for deletion
            !serverMessageIds.contains(it.messageId)
        }

        if (messagesToDelete.isNotEmpty()) {
            Log.d("MessagingActivity", "Syncing: Deleting ${messagesToDelete.size} messages from local DB that don't exist on server")

            // Delete from database in background thread
            Thread {
                for (msg in messagesToDelete) {
                    messagesDbHelper.deleteMessage(msg.messageId)
                }

                // Update UI on main thread
                runOnUiThread {
                    for (msg in messagesToDelete) {
                        val index = messages.indexOfFirst { it.messageId == msg.messageId }
                        if (index >= 0) {
                            messages.removeAt(index)
                            adapter.notifyItemRemoved(index)
                        }
                    }
                }
            }.start()
        } else {
            Log.d("MessagingActivity", "Sync: No messages to delete, local DB is in sync with server")
        }
    }

    private fun buildContentKey(sender: String?, text: String?, ts: Long): String {
        val s = sender ?: ""
        val t = text ?: ""
        // Use 10-second buckets to avoid false positives while catching real duplicates
        val bucket = if (ts>0) ts/10000L else 0L
        return "$s|$t|$bucket"
    }

    // ----------------- retry & scheduling -----------------

    private val fetchAndRetryRunnable = object : Runnable {
        override fun run() {
            try {
                fetchMessages(initial = false)
                if (isNetworkAvailable()) sendPendingQueueOnce()
            } finally {
                handler.postDelayed(this, retryIntervalMs)
            }
        }
    }

    private val netReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isNetworkAvailable()) sendPendingQueueOnce()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ----------------- image pick & compress -----------------

    private fun pickImage() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(i, IMAGE_REQ)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQ && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val b64 = compressImageToBase64(uri)
            if (b64 != null) {
                val localId = "local_" + UUID.randomUUID().toString()
                val msg = Message(
                    messageId = localId,
                    chatId = chatId,
                    senderId = currentUserId,
                    receiverId = receiverId,
                    imageBase64 = b64,
                    timestamp = System.currentTimeMillis(),
                    isPending = true,
                    deliveryState = 0,
                    vanishMode = isVanishModeEnabled
                )
                // Save to local database
                messagesDbHelper.insertOrUpdateMessage(msg)
                adapter.addOrUpdateMessage(msg)
                recycler.scrollToPosition(messages.lastIndex)
                sendQueueDbHelper.insertMessage(msg)
                if (isNetworkAvailable()) sendPendingQueueOnce()
                else Toast.makeText(this, "Image queued (offline)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun compressImageToBase64(uri: Uri, maxDim: Int = 1024, quality: Int = 80): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val src = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(src)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val scaled = scaleBitmap(bitmap, maxDim)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val b = baos.toByteArray()
            baos.close()
            Base64.encodeToString(b, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w("MessagingActivity", "compress failed ${e.localizedMessage}")
            null
        }
    }

    private fun scaleBitmap(src: android.graphics.Bitmap, maxDim: Int): android.graphics.Bitmap {
        val w = src.width; val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = if (w >= h) maxDim.toFloat()/w else maxDim.toFloat()/h
        return android.graphics.Bitmap.createScaledBitmap(src, (w*ratio).toInt(), (h*ratio).toInt(), true)
    }

    // ----------------- helpers -----------------

    private fun loadMessagesFromDb() {
        val localMessages = messagesDbHelper.getMessagesForChat(chatId)

        // Batch: collect messages that need to be marked as seen
        val messagesToMarkSeen = mutableListOf<Message>()

        for (m in localMessages) {
            // Mark messages as seen if current user is the receiver
            if (m.receiverId == currentUserId && !m.seen) {
                val seenMessage = m.copy(seen = true)
                messagesToMarkSeen.add(seenMessage)
                adapter.addOrUpdateMessage(seenMessage)
            } else {
                adapter.addOrUpdateMessage(m)
            }
        }

        // Batch update: mark all messages as seen in one go (off main thread)
        if (messagesToMarkSeen.isNotEmpty()) {
            Thread {
                for (msg in messagesToMarkSeen) {
                    messagesDbHelper.insertOrUpdateMessage(msg)
                }
                Log.d("MessagingActivity", "Marked ${messagesToMarkSeen.size} messages as seen in DB")
            }.start()
        }

        if (localMessages.isNotEmpty()) {
            recycler.scrollToPosition(messages.lastIndex)
        }

        // Mark messages as seen on server (only if there are unseen messages)
        if (messagesToMarkSeen.isNotEmpty()) {
            markMessagesSeenOnServer()
        }
    }

    private fun loadUserProfile() {
        // Try to get chat name from intent first
        val chatName = intent.getStringExtra("chatName")
        if (!chatName.isNullOrBlank()) {
            findViewById<TextView>(R.id.profile_name)?.text = chatName
        }

        // Fetch profile from server
        val url = BASE_URL + "get_dp.php"
        val req = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)

                    // Set username
                    val username = json.optString("username", "User")
                    findViewById<TextView>(R.id.profile_name)?.text = username

                    // Set profile picture
                    val dpBase64 = json.optString("dp", "")
                    if (dpBase64.isNotBlank()) {
                        try {
                            val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_icon)?.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.w("MessagingActivity", "Failed to decode profile picture: ${e.localizedMessage}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MessagingActivity", "Failed to parse profile response: ${e.localizedMessage}")
                }
            },
            { error ->
                Log.w("MessagingActivity", "Failed to load profile: ${error.localizedMessage}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("userId" to receiverId)
            }
        }
        Volley.newRequestQueue(this).add(req)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
    }

    private fun toggleVanishMode() {
        isVanishModeEnabled = !isVanishModeEnabled
        // Update button appearance - you'll need vanish on/off icons
        if (isVanishModeEnabled) {
            vanishModeToggle.setImageResource(android.R.drawable.ic_delete) // Temporary icon
            Toast.makeText(this, "Vanish mode ON - Messages will disappear after being viewed", Toast.LENGTH_SHORT).show()
        } else {
            vanishModeToggle.setImageResource(android.R.drawable.ic_menu_info_details) // Temporary icon
            Toast.makeText(this, "Vanish mode OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearLocalCache() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Clear Local Cache")
            .setMessage("This will delete all local messages for this chat and reload from server. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                // Clear local database for this chat
                messagesDbHelper.clearChatMessages(chatId)

                // Clear UI
                messages.clear()
                adapter.notifyDataSetChanged()

                // Reload from server
                fetchMessages(initial = true)

                Toast.makeText(this, "Local cache cleared. Reloading from server...", Toast.LENGTH_SHORT).show()
                Log.d("MessagingActivity", "Local cache cleared for chat: $chatId")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVanishModeMessages() {
        try {
            Log.d("MessagingActivity", "Attempting to delete vanish mode messages for chat: $chatId, receiver: $currentUserId")

            // Delete vanish mode messages from local database in background thread
            Thread {
                try {
                    val deletedCount = messagesDbHelper.deleteVanishModeMessages(chatId, currentUserId)
                    Log.d("MessagingActivity", "Deleted $deletedCount vanish mode messages from local DB")
                } catch (e: Exception) {
                    Log.e("MessagingActivity", "Error deleting vanish messages from DB: ${e.localizedMessage}")
                }
            }.start()

            // Notify server to delete vanish mode messages (async, won't block)
            deleteVanishModeMessagesOnServer()
        } catch (e: Exception) {
            Log.e("MessagingActivity", "Error in deleteVanishModeMessages: ${e.localizedMessage}")
        }
    }

    private fun deleteVanishModeMessagesOnServer() {
        val url = BASE_URL + "messages_vanish_delete.php"
        val req = object : StringRequest(Method.POST, url,
            { resp ->
                try {
                    val j = JSONObject(resp)
                    if (j.optInt("status", 0) == 1) {
                        Log.d("MessagingActivity", "Vanish mode messages deleted on server: ${j.optInt("deleted_count", 0)}")
                    }
                } catch (e: Exception) {
                    Log.w("MessagingActivity", "vanish delete error: ${e.localizedMessage}")
                }
            },
            { err -> Log.w("MessagingActivity", "vanish delete network error ${err.localizedMessage}") }
        ) {
            override fun getParams(): MutableMap<String, String> =
                hashMapOf("chat_id" to chatId, "user_id" to currentUserId)
            
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store"
                return headers
            }
        }
        req.setShouldCache(false)
        Volley.newRequestQueue(this).add(req)
    }

    private fun parseServerMessage(obj: JSONObject): Message {
        return Message(
            messageId = obj.optString("message_id"),
            chatId = chatId,
            senderId = obj.optString("sender_id"),
            receiverId = obj.optString("receiver_id"),
            text = obj.optString("text"),
            imageUrl = obj.optString("image"),
            postId = obj.optString("post_id"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            vanishMode = obj.optInt("vanish_mode",0)==1,
            edited = obj.optInt("edited",0)==1,
            deleted = obj.optInt("deleted",0)==1,
            isPending = false,
            deliveryState = 1
        )
    }
}
