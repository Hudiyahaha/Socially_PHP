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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray
import java.io.File

class ViewStory : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private lateinit var storyVideo: VideoView
    private lateinit var cross: ImageView
    private lateinit var profile: CircleImageView
    private lateinit var usernameView: TextView

    private var stories = mutableListOf<Story>()
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_story)

        storyImage = findViewById(R.id.storyImageView)
        storyVideo = findViewById(R.id.storyVideoView)
        cross = findViewById(R.id.cross)
        profile = findViewById(R.id.profile)
        usernameView = findViewById(R.id.username)

        cross.setOnClickListener { finish() }

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = intent.getStringExtra("userId") ?: prefs.getString("userId", null) ?: run {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        fetchStories(userId)
    }

    private fun fetchStories(userId: String) {
        stories.clear()
        val request = object : StringRequest(
            Method.POST,
            "http://sociallyah.atwebpages.com/get_story.php",
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    val now = System.currentTimeMillis() / 1000 // seconds
                    val storyAgeLimit = 24 * 60 * 60 // 24 hours in seconds

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val timestamp = obj.getLong("timestamp")
                        if (now - timestamp <= storyAgeLimit) {
                            stories.add(
                                Story(
                                    id = obj.getString("id"),
                                    userId = obj.getString("userId"),
                                    mediaBase64 = obj.getString("media"),
                                    mediaType = obj.getString("type"),
                                    timestamp = timestamp,
                                    username = obj.optString("username", "Unknown"),
                                    dp = obj.optString("dp")
                                )
                            )
                        }
                    }

                    if (stories.isNotEmpty()) {
                        showStory(0)
                        loadProfile(stories[0].dp, stories[0].username)
                    } else {
                        Toast.makeText(this, "No stories available", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                } catch (e: Exception) {
                    Log.e("FETCH_STORIES", "JSON parse error", e)
                    Toast.makeText(this, "Failed to parse stories", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                Log.e("FETCH_STORIES", error.toString())
                Toast.makeText(this, "Failed to fetch stories", Toast.LENGTH_SHORT).show()
                finish()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("userId" to userId)
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadProfile(dpBase64: String?, username: String?) {
        if (!dpBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                profile.setImageResource(R.drawable.me)
            }
        } else {
            profile.setImageResource(R.drawable.me)
        }

        usernameView.text = username ?: "Unknown"
    }

    private fun showStory(index: Int) {
        if (index >= stories.size) {
            finish()
            return
        }

        currentIndex = index
        val story = stories[index]

        try {
            val bytes = Base64.decode(story.mediaBase64, Base64.DEFAULT)
            if (story.mediaType == "image") {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                storyImage.setImageBitmap(bitmap)
                storyImage.visibility = View.VISIBLE
                storyVideo.visibility = View.GONE

                // Move to next story after 5 seconds
                handler.postDelayed({ showStory(index + 1) }, 5000)

            } else if (story.mediaType == "video") {
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
