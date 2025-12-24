package com.wethrive.i23_0761_i23_0765

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray

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
            add@{ response ->
                val trimmed = response.trim()
                
                // Check for server error response (JSON object with error field)
                if (trimmed.startsWith("{")) {
                    try {
                        val jsonObj = org.json.JSONObject(trimmed)
                        if (jsonObj.optBoolean("error", false)) {
                            val errorMsg = jsonObj.optString("message", "Unknown server error")
                            Log.e("FETCH_STORIES", "Server error: $errorMsg")
                            Toast.makeText(this, "Connection limit exceeded. Please try again later.", Toast.LENGTH_SHORT).show()
                            finish()
                            return@add
                        }
                    } catch (e: Exception) {
                        Log.e("FETCH_STORIES", "Error parsing error response", e)
                    }
                }
                
                try {
                    val jsonArray = JSONArray(trimmed)
                    val now = System.currentTimeMillis() / 1000 // seconds
                    val storyAgeLimit = 24 * 60 * 60 // 24 hours in seconds

                    val clickedUserId = intent.getStringExtra("userId") ?: userId

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        // ONLY THIS USER'S STORIES
                        if (obj.getString("userId") != clickedUserId) continue

                        val timestamp = obj.getLong("timestamp")
                        if (now - timestamp <= storyAgeLimit) {
                            stories.add(
                                Story(
                                    id = obj.getString("id"),
                                    userId = obj.getString("userId"),
                                    mediaUrl = "http://sociallyah.atwebpages.com/i.php?p=${obj.getString("media")}",
                                    mediaType = obj.getString("type"),
                                    timestamp = timestamp,
                                    username = obj.optString("username", "Unknown"),
                                    dpUrl = obj.optString("dp")
                                )
                            )
                        }
                    }


                    if (stories.isNotEmpty()) {
                        showStory(0)
                        loadProfile(stories[0].dpUrl, stories[0].username)
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
                val bytes = android.util.Base64.decode(dpBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profile.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                // If decoding fails, fallback to default image
                profile.setImageResource(R.drawable.dummy)
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
        Log.d("STORY_URL", "Loading media: ${story.mediaUrl}")
        Log.d("STORY_TYPE", "Media type: ${story.mediaType}")

        if (story.mediaType == "image") {
            Picasso.get()
                .load(story.mediaUrl)
                .placeholder(R.drawable.me)
                .error(R.drawable.me)
                .into(storyImage)

            storyImage.visibility = View.VISIBLE
            storyVideo.visibility = View.GONE

            // Move to next story after 5 seconds
            handler.postDelayed({ showStory(index + 1) }, 5000)

        } else if (story.mediaType == "video") {
            storyVideo.setVideoURI(Uri.parse(story.mediaUrl))
            storyVideo.visibility = View.VISIBLE
            storyImage.visibility = View.GONE

            storyVideo.setOnCompletionListener {
                showStory(index + 1)
            }

            storyVideo.start()
        }
    }
}