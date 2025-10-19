package com.wethrive.i23_0761_i23_0765

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

object NotificationHelper {
    private const val CHANNEL_ID = "follow_req_channel"
    private const val NOTIF_ID_BASE = 3000
    private const val PREFS = "notif_prefs"

    private var attachedUid: String? = null
    private var followChildListener: ChildEventListener? = null

    // Messages listeners
    private var messagesRootListener: ChildEventListener? = null
    private val chatListeners = mutableMapOf<String, ChildEventListener>()
    private var attachStartMs: Long = 0L
    @Volatile private var activeChatId: String? = null

    // Screenshot events
    private var screenshotRootListener: ChildEventListener? = null
    private val screenshotChatListeners = mutableMapOf<String, ChildEventListener>()

    fun setActiveChatId(chatId: String?) {
        activeChatId = chatId
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Notifications for new follow requests and messages"
            nm.createNotificationChannel(channel)
        }
    }

    fun maybeRequestPostNotifications(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 && context is android.app.Activity) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                context.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    // FOLLOW REQUESTS
    fun startFollowRequestListener(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        if (attachedUid == uid && followChildListener != null) return

        ensureChannel(context)

        // Detach previous if any
        followChildListener?.let {
            FirebaseDatabase.getInstance().getReference("Requests").child(attachedUid ?: return).removeEventListener(it)
        }
        attachedUid = uid

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val requestsRef = FirebaseDatabase.getInstance().getReference("Requests").child(uid)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val requesterId = snapshot.key ?: return
                if (wasNotified(prefs, "req_" + requesterId)) return
                FirebaseDatabase.getInstance().getReference("Users").child(requesterId).child("uname").get()
                    .addOnSuccessListener { unameSnap ->
                        val uname = unameSnap.getValue(String::class.java) ?: "Someone"
                        showFollowRequestNotification(context, uname)
                        markNotified(prefs, "req_" + requesterId)
                    }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        requestsRef.addChildEventListener(listener)
        followChildListener = listener
    }

    // MESSAGES
    fun startMessageListeners(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        // If already attached to root for this uid, skip
        if (messagesRootListener != null && attachedUid == uid) return
        attachedUid = uid
        attachStartMs = System.currentTimeMillis()
        ensureChannel(context)

        // Detach previous
        messagesRootListener?.let {
            FirebaseDatabase.getInstance().getReference("Messages").removeEventListener(it)
        }
        chatListeners.forEach { (chatId, l) ->
            FirebaseDatabase.getInstance().getReference("Messages").child(chatId).removeEventListener(l)
        }
        chatListeners.clear()

        val rootRef = FirebaseDatabase.getInstance().getReference("Messages")
        val rootListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                // ChatId format: smallerUid_biggerUid; we just check contains current uid
                if (!chatId.contains(uid)) return
                attachChatListener(context, chatId)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                if (!chatId.contains(uid)) return
                // Ensure listener exists
                attachChatListener(context, chatId)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chatId = snapshot.key ?: return
                chatListeners.remove(chatId)?.let {
                    FirebaseDatabase.getInstance().getReference("Messages").child(chatId).removeEventListener(it)
                }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.addChildEventListener(rootListener)
        messagesRootListener = rootListener
    }

    private fun attachChatListener(context: Context, chatId: String) {
        if (chatListeners.containsKey(chatId)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ref = FirebaseDatabase.getInstance().getReference("Messages").child(chatId)
        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) ?: return
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
                // Only notify incoming, not deleted, not while viewing this chat, not old
                if (message.receiverId != currentUid) return
                if (message.deleted == true) return
                if (message.timestamp < attachStartMs) return
                if (activeChatId == chatId) return
                val msgKey = "msg_" + (message.messageId ?: snapshot.key ?: return)
                if (wasNotified(prefs, msgKey)) return

                // Decide summary
                val summary = when {
                    !message.imageBase64.isNullOrBlank() || !message.imageUrl.isNullOrBlank() -> "sent you a photo"
                    !message.postId.isNullOrBlank() -> "shared a post"
                    !message.text.isNullOrBlank() -> "sent you a message"
                    else -> "sent you a message"
                }

                val otherUid = otherIdFromChat(chatId, currentUid) ?: return
                // Lookup sender name (other user)
                FirebaseDatabase.getInstance().getReference("Users").child(otherUid).child("uname").get()
                    .addOnSuccessListener { unameSnap ->
                        val uname = unameSnap.getValue(String::class.java) ?: "Someone"
                        showMessageNotification(context, chatId, otherUid, uname, summary)
                        markNotified(prefs, msgKey)
                    }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addChildEventListener(l)
        chatListeners[chatId] = l
    }

    private fun otherIdFromChat(chatId: String, currentUid: String): String? {
        return when {
            chatId.startsWith(currentUid + "_") -> chatId.substring(currentUid.length + 1)
            chatId.endsWith("_" + currentUid) -> chatId.substring(0, chatId.length - currentUid.length - 1)
            else -> null
        }
    }

    private fun wasNotified(prefs: SharedPreferences, key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    private fun markNotified(prefs: SharedPreferences, key: String) {
        prefs.edit().putBoolean(key, true).apply()
    }

    private fun showFollowRequestNotification(context: Context, requesterName: String) {
        val intent = Intent(context, MainActivity12::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("source", "follow_request")
        }
        val pending = androidx.core.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(201, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New follow request")
            .setContentText("$requesterName requested to follow you")
            .setColor(context.getColor(R.color.button))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (requesterName.hashCode() and 0x0FFF), notif)
    }

    private fun showMessageNotification(context: Context, chatId: String, otherUid: String, senderName: String, summary: String) {
        val intent = Intent(context, MainActivity9::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("receiverId", otherUid)
            putExtra("chatName", senderName)
        }
        val pending = androidx.core.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(301, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderName)
            .setContentText("$senderName $summary")
            .setColor(context.getColor(R.color.button))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (chatId.hashCode() and 0x0FFF), notif)
    }

    fun startScreenshotListeners(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        ensureChannel(context)

        // Detach previous
        screenshotRootListener?.let { FirebaseDatabase.getInstance().getReference("Screenshots").removeEventListener(it) }
        screenshotChatListeners.forEach { (chatId, l) ->
            FirebaseDatabase.getInstance().getReference("Screenshots").child(chatId).removeEventListener(l)
        }
        screenshotChatListeners.clear()

        val root = FirebaseDatabase.getInstance().getReference("Screenshots")
        val rootListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                if (!chatId.contains(uid)) return
                attachScreenshotChatListener(context, chatId)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                if (!chatId.contains(uid)) return
                attachScreenshotChatListener(context, chatId)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chatId = snapshot.key ?: return
                screenshotChatListeners.remove(chatId)?.let {
                    FirebaseDatabase.getInstance().getReference("Screenshots").child(chatId).removeEventListener(it)
                }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        root.addChildEventListener(rootListener)
        screenshotRootListener = rootListener
    }

    private fun attachScreenshotChatListener(context: Context, chatId: String) {
        if (screenshotChatListeners.containsKey(chatId)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ref = FirebaseDatabase.getInstance().getReference("Screenshots").child(chatId)
        val l = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val map = snapshot.value as? Map<*, *> ?: return
                val by = map["by"] as? String ?: return
                val to = map["to"] as? String ?: return
                val ts = (map["timestamp"] as? Number)?.toLong() ?: 0L
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
                // Only notify if current user is the recipient
                if (to != currentUid) return
                if (activeChatId == chatId) return
                val key = "shot_" + snapshot.key
                if (wasNotified(prefs, key)) return

                val otherUid = otherIdFromChat(chatId, currentUid) ?: return
                FirebaseDatabase.getInstance().getReference("Users").child(otherUid).child("uname").get()
                    .addOnSuccessListener { unameSnap ->
                        val uname = unameSnap.getValue(String::class.java) ?: "Someone"
                        showScreenshotNotification(context, chatId, otherUid, uname)
                        markNotified(prefs, key)
                    }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addChildEventListener(l)
        screenshotChatListeners[chatId] = l
    }

    private fun showScreenshotNotification(context: Context, chatId: String, otherUid: String, otherName: String) {
        val intent = Intent(context, MainActivity9::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("receiverId", otherUid)
            putExtra("chatName", otherName)
        }
        val pending = androidx.core.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(401, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(otherName)
            .setContentText("$otherName took a screenshot of the chat")
            .setColor(context.getColor(R.color.button))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (chatId.hashCode() and 0x0FFF) + 77, notif)
    }
}
