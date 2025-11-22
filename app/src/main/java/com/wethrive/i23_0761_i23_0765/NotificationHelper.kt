package com.wethrive.i23_0761_i23_0765

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.Request.Method
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object NotificationHelper {
    private const val CHANNEL_ID = "notifications_channel"
    private const val NOTIF_ID_BASE = 3000
    private const val PREFS = "notif_prefs"
    private const val BASE_URL = "http://sociallyah.atwebpages.com/"

    @Volatile private var activeChatId: String? = null
    private var notificationHandler: Handler? = null
    private var notificationRunnable: Runnable? = null
    private val pollIntervalMs = 5_000L // Poll every 5 seconds
    private var lastNotificationCheck: Long = 0L
    private var currentUserId: String? = null
    private var isPolling: Boolean = false

    fun setActiveChatId(chatId: String?) {
        activeChatId = chatId
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Notifications for messages, follow requests, and screenshot alerts"
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

    // Start polling for all notifications (messages, follow requests, screenshots)
    fun startNotificationPolling(context: Context, userId: String) {
        if (isPolling && currentUserId == userId) return

        currentUserId = userId
        isPolling = true
        ensureChannel(context)

        if (notificationHandler == null) {
            notificationHandler = Handler(Looper.getMainLooper())
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        lastNotificationCheck = prefs.getLong("last_notification_check", System.currentTimeMillis())

        notificationRunnable = object : Runnable {
            override fun run() {
                if (!isPolling) return
                fetchNotifications(context, userId)
                notificationHandler?.postDelayed(this, pollIntervalMs)
            }
        }
        notificationHandler?.post(notificationRunnable!!)
    }

    fun stopNotificationPolling() {
        isPolling = false
        notificationRunnable?.let { notificationHandler?.removeCallbacks(it) }
        notificationRunnable = null
    }

    private fun fetchNotifications(context: Context, userId: String) {
        if (!isPolling) return // Stop if polling was stopped
        
        val url = BASE_URL + "notifications_fetch.php"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val since = lastNotificationCheck

        val req = object : StringRequest(
            Method.POST, url,
            { resp ->
                if (isPolling) {
                    try {
                        val trimmedResp = resp.trim()
                        if (trimmedResp.isNotEmpty()) {
                            Log.d("NotificationHelper", "Notification response: ${trimmedResp.take(500)}")
                            val j = JSONObject(trimmedResp)
                            if (j.optInt("status", 0) == 1) {
                                val arr = j.optJSONArray("notifications") ?: JSONArray()
                                Log.d("NotificationHelper", "Found ${arr.length()} notifications")
                                for (i in 0 until arr.length()) {
                                    try {
                                        val notif = arr.getJSONObject(i)
                                        processNotification(context, notif, prefs)
                                    } catch (e: Exception) {
                                        Log.e("NotificationHelper", "Error processing notification at index $i: ${e.localizedMessage}", e)
                                    }
                                }
                                // Update last check time
                                lastNotificationCheck = System.currentTimeMillis()
                                prefs.edit().putLong("last_notification_check", lastNotificationCheck).apply()
                            } else {
                                Log.w("NotificationHelper", "Server returned status != 1: ${j.optString("error", "Unknown error")}")
                            }
                        } else {
                            Log.w("NotificationHelper", "Empty response from server")
                        }
                    } catch (e: JSONException) {
                        Log.e("NotificationHelper", "JSON parse error: ${e.localizedMessage}. Response: ${resp.take(200)}", e)
                    } catch (e: Exception) {
                        Log.e("NotificationHelper", "Error parsing notifications: ${e.localizedMessage}", e)
                    }
                }
            },
            { err ->
                Log.e("NotificationHelper", "Error fetching notifications: ${err.message}", err)
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["user_id"] = userId
                if (since > 0) {
                    map["since"] = since.toString()
                }
                return map
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store"
                headers["Pragma"] = "no-cache"
                return headers
            }
        }
        req.setShouldCache(false)
        try {
            Volley.newRequestQueue(context).add(req)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error adding request to queue: ${e.localizedMessage}", e)
        }
    }

    private fun processNotification(context: Context, notif: JSONObject, prefs: SharedPreferences) {
        try {
            val type = notif.optString("type")
            val notifId = notif.optString("notification_id")

            if (type.isBlank() || notifId.isBlank()) {
                Log.w("NotificationHelper", "Invalid notification: missing type or id")
                return
            }

            Log.d("NotificationHelper", "Processing notification: type=$type, id=$notifId")

            // Skip if already notified
            if (wasNotified(prefs, notifId)) {
                Log.d("NotificationHelper", "Skipping already notified: $notifId")
                return
            }

            when (type) {
                "message" -> {
                    val chatId = notif.optString("chat_id")
                    // Don't notify if user is currently viewing this chat
                    if (activeChatId == chatId) {
                        Log.d("NotificationHelper", "Skipping message notification - user is viewing this chat")
                        return
                    }

                    val senderId = notif.optString("sender_id")
                    val senderName = notif.optString("sender_name", "Someone")
                    val hasImage = notif.optInt("has_image", 0) == 1
                    val hasPost = notif.optInt("has_post", 0) == 1
                    val text = notif.optString("text", "")

                    val summary = when {
                        hasImage -> "sent you a photo"
                        hasPost -> "shared a post"
                        text.isNotBlank() -> text.take(50)
                        else -> "sent you a message"
                    }

                    showMessageNotification(context, chatId, senderId, senderName, summary)
                    markNotified(prefs, notifId)
                }
                "follow_request" -> {
                    val followerId = notif.optString("follower_id")
                    val followerName = notif.optString("follower_name", "Someone")
                    showFollowRequestNotification(context, followerName)
                    markNotified(prefs, notifId)
                }
                "screenshot" -> {
                    val chatId = notif.optString("chat_id")
                    // Don't notify if user is currently viewing this chat
                    if (activeChatId == chatId) {
                        Log.d("NotificationHelper", "Skipping screenshot notification - user is viewing this chat")
                        return
                    }

                    val senderId = notif.optString("sender_id")
                    val senderName = notif.optString("sender_name", "Someone")
                    if (chatId.isNotBlank() && senderId.isNotBlank()) {
                        showScreenshotNotification(context, chatId, senderId, senderName)
                        markNotified(prefs, notifId)
                    } else {
                        Log.w("NotificationHelper", "Screenshot notification missing chat_id or sender_id")
                    }
                }
                else -> {
                    Log.w("NotificationHelper", "Unknown notification type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error processing notification: ${e.localizedMessage}", e)
        }
    }

    private fun wasNotified(prefs: SharedPreferences, key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    private fun markNotified(prefs: SharedPreferences, key: String) {
        prefs.edit().putBoolean(key, true).apply()
    }

    private fun showFollowRequestNotification(context: Context, requesterName: String) {
        try {
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
                if (!granted) {
                    Log.d("NotificationHelper", "Notification permission not granted")
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (requesterName.hashCode() and 0x0FFF), notif)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing follow request notification: ${e.localizedMessage}", e)
        }
    }

    private fun showMessageNotification(context: Context, chatId: String, otherUid: String, senderName: String, summary: String) {
        try {
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
                if (!granted) {
                    Log.d("NotificationHelper", "Notification permission not granted")
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (chatId.hashCode() and 0x0FFF), notif)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing message notification: ${e.localizedMessage}", e)
        }
    }


    private fun showScreenshotNotification(context: Context, chatId: String, otherUid: String, otherName: String) {
        try {
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
                if (!granted) {
                    Log.d("NotificationHelper", "Notification permission not granted")
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (chatId.hashCode() and 0x0FFF) + 77, notif)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing screenshot notification: ${e.localizedMessage}", e)
        }
    }

    // Legacy methods for backward compatibility - now just call startNotificationPolling
    fun startFollowRequestListener(context: Context) {
        // This is now handled by startNotificationPolling
        // Keep for backward compatibility
    }

    fun startMessageListeners(context: Context) {
        // This is now handled by startNotificationPolling
        // Keep for backward compatibility
    }

    fun startScreenshotListeners(context: Context) {
        // This is now handled by startNotificationPolling
        // Keep for backward compatibility
    }
}