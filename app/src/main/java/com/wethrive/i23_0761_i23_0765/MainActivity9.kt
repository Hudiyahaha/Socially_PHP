package com.wethrive.i23_0761_i23_0765

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.widget.EditText
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import android.util.Log
import android.graphics.Bitmap
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import java.io.ByteArrayOutputStream
import kotlin.math.max
// Agora imports
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import java.io.File

class MainActivity9 : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnAttach: ImageView
    // Header views
    private lateinit var profileName: TextView
    private lateinit var profileIcon: CircleImageView
    private lateinit var backArrow: ImageView

    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: ChatAdapter

    private lateinit var audio: ImageView
    private lateinit var video: ImageView
    private val messageList = mutableListOf<Message>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var otherUserId: String
    private lateinit var chatId: String

    private val IMAGE_REQUEST_CODE = 101

    // Call status UI
    private lateinit var callStatusContainer: LinearLayout
    private lateinit var callStatusText: TextView
    private lateinit var callTimerText: TextView
    private lateinit var callLevelBar: View

    // Timer
    private var callStartTimeMs: Long = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isInVoiceCall && callStartTimeMs > 0L) {
                val elapsed = System.currentTimeMillis() - callStartTimeMs
                callTimerText.text = formatElapsed(elapsed)
                timerHandler.postDelayed(this, 1000L)
            }
        }
    }

    // Agora voice call state/config
    private var rtcEngine: RtcEngine? = null
    private var isInVoiceCall: Boolean = false
    private val AUDIO_PERMISSION_REQ_CODE = 201
    // TODO: Replace with your real Agora App ID and token. If your project has no App Certificate, token can be null.
    private val agoraAppId: String = "941f2bca958848af98ccea5d2bda5ab5"
    private val agoraToken: String? = "007eJxTYPATXZ+5gTWJZ7Hx9cOXk+I5ItandhRxVeyw4W5U26G+5LYCg6WJYZpRUnKipamFhYlFYpqlRXJyaqJpilFSSqJpYpJpQvLnjIZARgazilUMjFAI4lsyGBsYRPp6GReamCdHhbiZ5KV7FeaWRqX5moY4GsWbRHoWVOUFGAQXRbhZGJlXhJi45uRG5VUaOJsaMzAAAM4qLpo=" // or "<Your token>"

    // Event handler for Agora callbacks
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d("MainActivity9", "Agora joined channel=$channel uid=$uid")
            runOnUiThread {
                callStatusText.text = getString(R.string.call_connected)
                startCallTimer()
                Toast.makeText(this@MainActivity9, "Voice call started", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("MainActivity9", "Remote user joined: $uid")
        }
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("MainActivity9", "Remote user offline: $uid reason=$reason")
            runOnUiThread {
                if (isInVoiceCall) {
                    leaveVoiceChannel()
                    Toast.makeText(this@MainActivity9, "Remote left. Call ended.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun onError(err: Int) {
            Log.e("MainActivity9", "Agora onError: $err")
            runOnUiThread {
                Toast.makeText(this@MainActivity9, "Agora error: $err", Toast.LENGTH_LONG).show()
                // If we're stuck connecting, surface the error and stop UI
                if (isInVoiceCall.not()) {
                    hideCallStatusUI()
                }
            }
        }
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d("MainActivity9", "Connection state=$state reason=$reason")
            runOnUiThread {
                when (state) {
                    Constants.CONNECTION_STATE_CONNECTING -> callStatusText.text = getString(R.string.call_connecting)
                    Constants.CONNECTION_STATE_CONNECTED -> callStatusText.text = getString(R.string.call_connected)
                    Constants.CONNECTION_STATE_RECONNECTING -> callStatusText.text = getString(R.string.call_connecting)
                    Constants.CONNECTION_STATE_FAILED, Constants.CONNECTION_STATE_DISCONNECTED -> {
                        if (!isInVoiceCall) hideCallStatusUI()
                    }
                }
            }
        }
        // Audio volume callback: reflect mic activity level in the UI
        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            val level = (totalVolume.coerceIn(0, 255)) / 255f
            runOnUiThread {
                callLevelBar.alpha = 0.2f + 0.8f * level
                callLevelBar.scaleX = 0.5f + 1.5f * level
            }
        }
    }

    // Call invite signaling (Firebase)
    private lateinit var callsRef: DatabaseReference
    private var callInviteListener: ValueEventListener? = null
    private var incomingDialog: AlertDialog? = null
    private var suppressInviteOnJoin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        otherUserId = intent.getStringExtra("receiverId")!!
        chatId = if (currentUserId < otherUserId)
            currentUserId + "_" + otherUserId
        else
            otherUserId + "_" + currentUserId

        // Explicitly target the default Realtime Database for this Firebase project
        val dbUrl = "https://i-0761-23i-0765-default-rtdb.firebaseio.com"
        dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Messages").child(chatId)
        callsRef = FirebaseDatabase.getInstance(dbUrl).getReference("Calls").child(chatId)

        try {
            Log.d("MainActivity9", "Realtime DB url: $dbUrl, root: ${dbRef.root}")
        } catch (_: Exception) { }

        recyclerChat = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)

        profileName = findViewById(R.id.profile_name)
        profileIcon = findViewById(R.id.profile_icon)
        backArrow = findViewById(R.id.back_arrow)

        audio = findViewById(R.id.audio)
        video = findViewById(R.id.video)

        // Call status UI
        callStatusContainer = findViewById(R.id.call_status_container)
        callStatusText = findViewById(R.id.call_status_text)
        callTimerText = findViewById(R.id.call_timer_text)
        callLevelBar = findViewById(R.id.call_level_bar)

        intent.getStringExtra("chatName")?.let { if (it.isNotBlank()) profileName.text = it }

        backArrow.setOnClickListener { finish() }

        adapter = ChatAdapter(messageList, currentUserId)
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = adapter

        loadMessages()
        loadReceiverProfile()

        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { pickImage() }
        // Start/end voice call when audio icon is tapped
        audio.setOnClickListener {
            if (!isInVoiceCall) {
                if (!hasAgoraPermissions()) {
                    requestAgoraPermissions()
                } else {
                    startVoiceCalling()
                }
            } else {
                leaveVoiceChannel()
                Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
            }
        }

        // Start listening for incoming call invites
        attachCallInviteListener()
    }

    // ============ Call signaling ============
    private fun attachCallInviteListener() {
        if (callInviteListener != null) return
        callInviteListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // No invite present; ensure dialog is closed
                    incomingDialog?.dismiss()
                    incomingDialog = null
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java)
                val callerId = snapshot.child("callerId").getValue(String::class.java)
                val calleeId = snapshot.child("calleeId").getValue(String::class.java)
                // Only show to the intended callee, and do not prompt the caller
                if (status == "ringing" && calleeId == currentUserId && callerId != currentUserId && !isInVoiceCall) {
                    if (incomingDialog?.isShowing == true) return
                    showIncomingCallDialog(callerId ?: "")
                } else if (status == "ended") {
                    // Clean up dialog and local state if needed
                    incomingDialog?.dismiss()
                    incomingDialog = null
                    if (isInVoiceCall) {
                        leaveVoiceChannel()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity9", "Calls listener cancelled: ${error.toException()}")
            }
        }
        callsRef.addValueEventListener(callInviteListener!!)
    }

    private fun showIncomingCallDialog(callerId: String) {
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.incoming_call_title)
            .setMessage(getString(R.string.incoming_call_message))
            .setCancelable(false)
            .setPositiveButton(R.string.accept) { dialog, _ ->
                // Accept: join without sending a new invite
                suppressInviteOnJoin = true
                callsRef.child("status").setValue("ongoing")
                startVoiceCalling()
                dialog.dismiss()
                incomingDialog = null
            }
            .setNegativeButton(R.string.decline) { dialog, _ ->
                callsRef.child("status").setValue("ended")
                dialog.dismiss()
                incomingDialog = null
            }
        incomingDialog = builder.create()
        incomingDialog?.show()
    }

    private fun sendCallInvite() {
        val invite = mapOf(
            "channel" to chatId,
            "callerId" to currentUserId,
            "calleeId" to otherUserId,
            "status" to "ringing",
            "timestamp" to System.currentTimeMillis()
        )
        callsRef.setValue(invite)
            .addOnFailureListener { e -> Log.e("MainActivity9", "Failed to write call invite", e) }
    }

    // ========================= Message loading/sending =========================
    private fun loadReceiverProfile() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uname = snapshot.child("uname").getValue(String::class.java)
                val dpBase64 = snapshot.child("dp").getValue(String::class.java)

                if (!uname.isNullOrBlank()) {
                    profileName.text = uname
                } else if (profileName.text.isNullOrBlank()) {
                    profileName.setText(R.string.unknown_user)
                }

                if (!dpBase64.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            profileIcon.setImageBitmap(bmp)
                            profileIcon.contentDescription = profileName.text
                        }
                    } catch (_: IllegalArgumentException) {
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun loadMessages() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (msgSnap in snapshot.children) {
                    val message = msgSnap.getValue(Message::class.java)
                    if (message != null) messageList.add(message)
                }
                Log.d("MainActivity9", "Loaded ${messageList.size} messages for chatId=$chatId")
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    recyclerChat.scrollToPosition(messageList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity9", "loadMessages cancelled: ${error.toException()}")
            }
        })
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val messageId = dbRef.push().key!!
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        dbRef.child(messageId).setValue(message)
        etMessage.text.clear()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != IMAGE_REQUEST_CODE) return

        if (resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                Log.w("MainActivity9", "Image picker returned OK but data URI was null")
                return
            }

            sendImageBase64(imageUri)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Image selection failed ($resultCode)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendImageBase64(uri: Uri) {
        val base64 = try {
            val b64 = compressImageToBase64(uri)
            if (b64.isNullOrBlank()) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_LONG).show()
                Log.e("MainActivity9", "compressImageToBase64 returned null/blank for $uri")
                return
            }
            b64
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to process image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "compressImageToBase64 exception", e)
            return
        }

        val messageId = dbRef.push().key!!
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            imageBase64 = base64,
            timestamp = System.currentTimeMillis()
        )
        dbRef.child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                Toast.makeText(this, "Image sent", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity9", "Image message saved (base64, length=${base64.length})")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save message: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity9", "DB write failed (base64)", e)
            }
    }

    private fun compressImageToBase64(uri: Uri, maxDim: Int = 1024, quality: Int = 80): String? {
        return try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val src = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    if (w > 0 && h > 0) {
                        val ratio = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
                        val targetW = (w * ratio).toInt().coerceAtLeast(1)
                        val targetH = (h * ratio).toInt().coerceAtLeast(1)
                        if (w > maxDim || h > maxDim) decoder.setTargetSize(targetW, targetH)
                    }
                }
            } else {
                val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, optsBounds)
                } ?: return null

                val (w, h) = optsBounds.outWidth to optsBounds.outHeight
                if (w <= 0 || h <= 0) return null

                var inSample = 1
                val halfW = w / 2
                val halfH = h / 2
                while ((halfW / inSample) >= maxDim || (halfH / inSample) >= maxDim) {
                    inSample *= 2
                }

                val opts = BitmapFactory.Options().apply { inSampleSize = max(1, inSample) }
                val decoded = contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, opts)
                } ?: return null

                val scaled = scaleBitmap(decoded, maxDim)
                if (scaled != decoded) decoded.recycle()
                scaled
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val bytes = baos.toByteArray()
            baos.close()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (!bitmap.isRecycled) bitmap.recycle()
            b64
        } catch (e: Exception) {
            Log.e("MainActivity9", "compressImageToBase64 failed", e)
            null
        }
    }

    private fun scaleBitmap(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
        val newW = (w * ratio).toInt()
        val newH = (h * ratio).toInt()
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    // ========================= Agora integration and UI =========================
    private fun getAgoraPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasAgoraPermissions(): Boolean {
        return getAgoraPermissions().all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAgoraPermissions() {
        ActivityCompat.requestPermissions(this, getAgoraPermissions(), AUDIO_PERMISSION_REQ_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startVoiceCalling()
            } else {
                Toast.makeText(this, "Microphone permission is required to start a voice call", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startVoiceCalling() {
        if (isInVoiceCall) {
            Toast.makeText(this, "Already in call", Toast.LENGTH_SHORT).show()
            return
        }
        if (agoraAppId.isBlank() || agoraAppId.startsWith("<")) {
            Toast.makeText(this, "Set your Agora App ID in MainActivity9", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "Agora App ID is not set")
            return
        }
        // If this device initiated the call via UI, send an invite; if accepting, skip
        if (!suppressInviteOnJoin) {
            sendCallInvite()
        }
        // Show connecting UI
        showCallStatusUI(connecting = true)
        initializeAgoraVoiceSDK()
        joinVoiceChannel()
        // Reset the suppression flag so future manual calls still send invites
        suppressInviteOnJoin = false
    }

    private fun initializeAgoraVoiceSDK() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = agoraAppId
                mEventHandler = rtcEventHandler
                // Write logs to app files dir for troubleshooting
                mLogConfig = RtcEngineConfig.LogConfig().apply {
                    filePath = File(filesDir, "agora.log").absolutePath
                }
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            try { rtcEngine?.enableAudioVolumeIndication(200, 3, true) } catch (_: Throwable) {}
        } catch (e: Exception) {
            Log.e("MainActivity9", "Error initializing Agora RTC engine", e)
            Toast.makeText(this, "Failed to init voice engine: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            hideCallStatusUI()
        }
    }

    private fun joinVoiceChannel() {
        val engine = rtcEngine ?: return
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
        }
        val rc = engine.joinChannel(agoraToken, chatId, 0, options)
        Log.d("MainActivity9", "joinChannel rc=$rc token=${agoraToken != null} appIdSet=${agoraAppId.isNotBlank()} channel=$chatId")
        if (rc == 0) {
            isInVoiceCall = true
            callStatusText.text = getString(R.string.call_connecting)
        } else {
            Toast.makeText(this, "Failed to join channel: $rc", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "joinChannel failed rc=$rc")
            hideCallStatusUI()
        }
    }

    private fun leaveVoiceChannel() {
        rtcEngine?.leaveChannel()
        isInVoiceCall = false
        stopCallTimer()
        hideCallStatusUI()
        // Mark ended in signaling so the peer can stop ringing
        callsRef.child("status").setValue("ended")
    }

    private fun cleanupAgoraEngine() {
        try {
            leaveVoiceChannel()
            RtcEngine.destroy()
        } catch (_: Exception) { }
        rtcEngine = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detach listener to avoid leaks
        callInviteListener?.let { callsRef.removeEventListener(it) }
        callInviteListener = null
        incomingDialog?.dismiss()
        incomingDialog = null
        cleanupAgoraEngine()
    }

    // ========================= Call UI helpers =========================
    private fun showCallStatusUI(connecting: Boolean) {
        callStatusContainer.visibility = View.VISIBLE
        callStatusText.text = if (connecting) getString(R.string.call_connecting) else getString(R.string.call_connected)
        callTimerText.text = "00:00"
        callLevelBar.alpha = 0.2f
        callLevelBar.scaleX = 0.5f
    }

    private fun hideCallStatusUI() {
        callStatusContainer.visibility = View.GONE
    }

    private fun startCallTimer() {
        callStartTimeMs = System.currentTimeMillis()
        timerHandler.removeCallbacksAndMessages(null)
        timerHandler.post(timerRunnable)
    }

    private fun stopCallTimer() {
        callStartTimeMs = 0L
        timerHandler.removeCallbacksAndMessages(null)
        callTimerText.text = "00:00"
    }

    private fun formatElapsed(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }
}
