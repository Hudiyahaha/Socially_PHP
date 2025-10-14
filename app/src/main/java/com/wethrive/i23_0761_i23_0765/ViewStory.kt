package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class ViewStory : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private lateinit var storyVideo: VideoView
    private lateinit var cross: ImageView

    private var stories = mutableListOf<Map<String, Any>>()
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_story)

        storyImage = findViewById(R.id.storyImageView)
        storyVideo = findViewById(R.id.storyVideoView)
        cross = findViewById(R.id.cross)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        cross.setOnClickListener { finish() }

        val ref = FirebaseDatabase.getInstance()
            .getReference("stories")
            .child(currentUser.uid)

        val storyAgeLimit = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
        val now = System.currentTimeMillis()

        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, "No stories found!", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            // Filter and delete expired stories
            for (child in snapshot.children) {
                val map = child.value as? Map<String, Any> ?: continue
                val timestamp = map["timestamp"] as? Long ?: 0L

                if (now - timestamp > storyAgeLimit) {
                    // Delete old story
                    child.ref.removeValue()
                } else {
                    // Keep valid story
                    stories.add(map)
                }
            }

            if (stories.isNotEmpty()) {
                // Sort stories by timestamp (oldest → newest)
                stories.sortBy { it["timestamp"] as? Long ?: 0L }
                showStory(0)
            } else {
                Toast.makeText(this, "All stories expired!", Toast.LENGTH_SHORT).show()
                finish()
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load stories", Toast.LENGTH_SHORT).show()
        }

        val profile = findViewById<CircleImageView>(R.id.profile)

        // Load profile picture
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
            databaseRef.child("dp").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val imageString = snapshot.getValue(String::class.java)
                        if (imageString != null) {
                            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profile.setImageBitmap(bitmap)

                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Error: ${it.message}")
                }
        }
        val name=findViewById<TextView>(R.id.username)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            ref.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("uname").getValue(String::class.java)
                    if (username != null) {
                        name.text = username
                    } else {
                        name.text = "Unknown User"
                    }
                }
            }.addOnFailureListener {
                Log.e("Firebase", "Failed to get username", it)
            }
        }
    }

    private fun showStory(index: Int) {
        if (index >= stories.size) {
            finish()
            return
        }

        val story = stories[index]
        val base64String = story["mediaBase64"] as? String ?: return
        val mediaType = story["mediaType"] as? String ?: "image"

        try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)

            if (mediaType == "image") {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                storyImage.setImageBitmap(bitmap)
                storyImage.visibility = View.VISIBLE
                storyVideo.visibility = View.GONE

                // Move to next story after 5 seconds
                handler.postDelayed({ showStory(index + 1) }, 5000)

            } else if (mediaType == "video") {
                val tempFile = File.createTempFile("story_temp", ".mp4", cacheDir)
                tempFile.writeBytes(bytes)
                storyVideo.setVideoURI(Uri.fromFile(tempFile))
                storyVideo.visibility = View.VISIBLE
                storyImage.visibility = View.GONE

                storyVideo.setOnCompletionListener {
                    showStory(index + 1)
                }

                storyVideo.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error showing story", Toast.LENGTH_SHORT).show()
            showStory(index + 1)
        }
    }
}
