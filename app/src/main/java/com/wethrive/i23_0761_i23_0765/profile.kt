package com.wethrive.i23_0761_i23_0765

import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

import android.graphics.BitmapFactory
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class profile : AppCompatActivity() {
    private var targetId: String? = null

    // Declare views as class-level properties
    private lateinit var dp: CircleImageView
    private lateinit var uname: TextView
    private lateinit var bio: TextView
    private lateinit var name: TextView
    private lateinit var follow: MaterialButton
    private lateinit var profile_bottom: CircleImageView

    private val NOTIF_CHANNEL_ID = "follow_req_channel"
    private val NOTIF_ID_BASE = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile)

        // Initialize views
        dp = findViewById(R.id.dp)
        uname = findViewById(R.id.uname)
        bio = findViewById(R.id.bio)
        name = findViewById(R.id.name)
        profile_bottom=findViewById(R.id.profile_bottom)
        follow=findViewById(R.id.follow)
        val posts= findViewById<TextView>(R.id.posts)
        val followers= findViewById<TextView>(R.id.followers)
        val following= findViewById<TextView>(R.id.following)

        val current= getSharedPreferences("user_session", MODE_PRIVATE).getString("userId", "") ?: ""

        // Receive the targetId from the intent
        targetId = intent.getStringExtra("userId")
        val target = targetId ?: return

        val requestsRef = FirebaseDatabase.getInstance().getReference("Requests")
        val followingRef = FirebaseDatabase.getInstance().getReference("Following")
        val followersRef = FirebaseDatabase.getInstance().getReference("Followers")

        fun updateButtonState(state: String) {
            when (state) {
                "Follow" -> {
                    follow.text = "Follow"
                    follow.backgroundTintList = resources.getColorStateList(R.color.SMD)
                    follow.setTextColor(resources.getColor(R.color.white))
                }
                "Requested" -> {
                    follow.text = "Requested"
                    follow.backgroundTintList = resources.getColorStateList(R.color.purpleblue)
                    follow.setTextColor(resources.getColor(R.color.white))
                }
                "Following" -> {
                    follow.text = "Following"
                    follow.backgroundTintList = resources.getColorStateList(R.color.bluish)
                    follow.setTextColor(resources.getColor(R.color.SMD))
                }
            }
        }

        // --- STEP 1: Detect current relationship state ---
        fun detectState() {
            followingRef.child(current).child(target).get().addOnSuccessListener { followingSnap ->
                if (followingSnap.exists()) {
                    updateButtonState("Following")
                } else {
                    requestsRef.child(target).child(current).get().addOnSuccessListener { requestSnap ->
                        if (requestSnap.exists()) {
                            updateButtonState("Requested")
                        } else {
                            updateButtonState("Follow")
                        }
                    }
                }
            }
        }

// --- STEP 2: Handle button clicks ---
        follow.setOnClickListener {
            when (follow.text.toString()) {
                "Follow" -> {
                    requestsRef.child(target).child(current).setValue("pending").addOnSuccessListener {
                        Toast.makeText(this, "Follow request sent!", Toast.LENGTH_SHORT).show()
                        updateButtonState("Requested")
                    }
                }

                "Requested" -> {
                    requestsRef.child(target).child(current).removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Follow request canceled", Toast.LENGTH_SHORT).show()
                        updateButtonState("Follow")
                    }
                }

                "Following" -> {
                    // Remove from Followers and Following
                    followingRef.child(current).child(target).removeValue()
                    followersRef.child(target).child(current).removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()
                        updateButtonState("Follow")
                    }
                }
            }
        }

        detectState()

        // YAHAN PY DATABASE SY UTH K NUMBER OF POSTS AUR FOLLOWERS AUR FOLLOWING AARHY HAIN
//        var ref=FirebaseDatabase.getInstance().getReference("Followers").child(targetId!!)
//
//        ref.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val count = snapshot.childrenCount
//                followers.text = count.toString()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Handle error if needed
//                followers.text="-1"
//            }
//        })
//
//        ref=FirebaseDatabase.getInstance().getReference("Following").child(targetId!!)
//
//        ref.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val count = snapshot.childrenCount
//                following.text = count.toString()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Handle error if needed
//                following.text="-1"
//            }
//        })
//
//        ref=FirebaseDatabase.getInstance().getReference("Posts").child(targetId!!)
//
//        ref.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val count = snapshot.childrenCount
//                posts.text = count.toString()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Handle error if needed
//                posts.text="-1"
//            }
//        })
//----------------------------------------------------------------------------separator

        if (targetId.isNullOrEmpty()) {
            Toast.makeText(this, "No user ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Create notification channel once
        createFollowRequestChannel()
        // Attach in-app listener for current user's incoming follow requests
        attachFollowRequestListener(current)

        loadUserProfile(targetId!!,current)
    }

    private fun attachFollowRequestListener(currentUserId: String) {
        val reqRef = FirebaseDatabase.getInstance().getReference("Requests").child(currentUserId)
        // Listen for children added under Requests/currentUserId
        reqRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Show a notification per requester (basic approach)
                for (child in snapshot.children) {
                    val requesterId = child.key ?: continue
                    // Lookup requester username
                    FirebaseDatabase.getInstance().getReference("Users").child(requesterId).child("uname").get()
                        .addOnSuccessListener { unameSnap ->
                            val uname = unameSnap.getValue(String::class.java) ?: "Someone"
                            showFollowRequestNotification(uname)
                        }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createFollowRequestChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Follow Requests"
            val desc = "Notifications for new follow requests"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIF_CHANNEL_ID, name, importance).apply {
                description = desc
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showFollowRequestNotification(requesterName: String) {
        // Pending intent to open MainActivity12
        val intent = Intent(this, MainActivity12::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("source", "follow_request")
        }
        val pending = androidx.core.app.TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(101, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }

        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New follow request")
            .setContentText("$requesterName requested to follow you")
            .setColor(getColor(R.color.button))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val nm = NotificationManagerCompat.from(this)
        // On Android 13+ ensure POST_NOTIFICATIONS permission is granted
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // Don't crash; you can optionally request permission elsewhere
                return
            }
        }
        nm.notify(NOTIF_ID_BASE + (requesterName.hashCode() and 0x0FFF), notif)
    }

    private fun loadUserProfile(target: String, current: String) {
        Toast.makeText(this, "Loading profile for user: $target", Toast.LENGTH_SHORT).show()

        val databaseref = FirebaseDatabase.getInstance().getReference("Users").child(target)
        val currentref = FirebaseDatabase.getInstance().getReference("Users").child(current)

        val request = object : StringRequest(
            Request.Method.POST,
            "http://sociallyah.atwebpages.com/get_dp.php",
            { response ->
                try {
                    val json = JSONObject(response)

                    if (json.getInt("status") == 1) {
                        val username = json.getString("username")
                        uname.text = username
                        name.text = username
                        val biography = json.getString("bio")
                        bio.text = biography

                        val profile = json.getString("dp")
                        if (!profile.isNullOrBlank()) {

                            val imageBytes = Base64.decode(profile, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            dp.setImageBitmap(bitmap)
                        }

                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = hashMapOf<String, String>()
                params["userId"] = target
                return params

            }
        }

        Volley.newRequestQueue(this).add(request)

        // Load profile picture from SharedPreferences
        val sharedPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val base64String = sharedPrefs.getString("dp", null)

        if (!base64String.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    profile_bottom.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Handle decoding error
                profile_bottom.setImageResource(R.drawable.me)
            }
        }
    }
}