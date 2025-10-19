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
    private const val PREFS_KEY_ATTACHED_UID = "attached_uid"

    private var attachedUid: String? = null
    private var childListener: ChildEventListener? = null

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Notifications for new follow requests"
            nm.createNotificationChannel(channel)
        }
    }

    fun maybeRequestPostNotifications(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted && context is android.app.Activity) {
                context.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    fun startFollowRequestListener(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        if (attachedUid == uid && childListener != null) return

        ensureChannel(context)

        // Detach previous if any
        childListener?.let {
            FirebaseDatabase.getInstance().getReference("Requests").child(attachedUid ?: return).removeEventListener(it)
        }
        attachedUid = uid

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val requestsRef = FirebaseDatabase.getInstance().getReference("Requests").child(uid)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val requesterId = snapshot.key ?: return
                // Avoid duplicate notify if already seen
                if (wasNotified(prefs, requesterId)) return
                FirebaseDatabase.getInstance().getReference("Users").child(requesterId).child("uname").get()
                    .addOnSuccessListener { unameSnap ->
                        val uname = unameSnap.getValue(String::class.java) ?: "Someone"
                        showFollowRequestNotification(context, uname)
                        markNotified(prefs, requesterId)
                    }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        requestsRef.addChildEventListener(listener)
        childListener = listener
    }

    private fun wasNotified(prefs: SharedPreferences, id: String): Boolean {
        return prefs.getBoolean("req_$id", false)
    }

    private fun markNotified(prefs: SharedPreferences, id: String) {
        prefs.edit().putBoolean("req_$id", true).apply()
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
}

