package com.wethrive.i23_0761_i23_0765

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainActivity5 : AppCompatActivity() {

    private var photoUri: Uri? = null
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: FeedPostAdapter
    private val postList = mutableListOf<Post>()




    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openCamera()
        }

    private val mediaPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                for (uri in uris) {
                    val mimeType = contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video") == true
                    uploadStory(uri, if (isVideo) "video" else "image")
                }
            } else {
                Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            }
        }
    private val postPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                val intent = Intent(this, MainActivity16::class.java)
                intent.putParcelableArrayListExtra("mediaUris", ArrayList(uris))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                photoUri?.let { uri ->
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                    uploadStory(uri, "image")

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)




        val search = findViewById<ImageView>(R.id.search_bar)
        val dm = findViewById<ImageView>(R.id.message_icon)
        val notis = findViewById<ImageView>(R.id.heart)
        val profile_bottom = findViewById<CircleImageView>(R.id.profile2)
        val create = findViewById<ImageView>(R.id.create)
        val camera = findViewById<ImageView>(R.id.camera_icon)
        val your_story = findViewById<LinearLayout>(R.id.your_story)
        val addstory = findViewById<ImageView>(R.id.addStoryIcon)
        val profile = findViewById<CircleImageView>(R.id.profile)
        val storyRecyclerView = findViewById<RecyclerView>(R.id.storyRecyclerView)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""
        val savedDp = prefs.getString("dp", "")

        if (!savedDp.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(savedDp, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profile.setImageBitmap(bitmap)
                profile_bottom.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                // If decoding fails, fallback to default image
                profile.setImageResource(R.drawable.me)
                profile_bottom.setImageResource(R.drawable.me)
            }
        } else {
            profile.setImageResource(R.drawable.me)
            profile_bottom.setImageResource(R.drawable.me)
        }


        val storyList = mutableListOf<Story>()
        storyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(storyList) { story ->
            val intent = Intent(this, ViewStory::class.java)
            intent.putExtra("userId", story.userId)
            startActivity(intent)
        }
        storyRecyclerView.adapter = storyAdapter
        fetchStories()



        val postRecycler= findViewById<RecyclerView>(R.id.postRecycler)
        postRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postAdapter = FeedPostAdapter(postList)
        postRecycler.adapter = postAdapter
        fetchPosts()



        // Upload image/video story
        addstory.setOnClickListener {
            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
        }

        // Open last story
        your_story.setOnClickListener {

            if (userId != null) {
                val intent = Intent(this, ViewStory::class.java)

                startActivity(intent)
            }
        }


        create.setOnClickListener {
            startActivity(Intent(this, MainActivity16::class.java))
        }
        profile_bottom.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
        search.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
        dm.setOnClickListener { startActivity(Intent(this, MainActivity8::class.java)) }
        notis.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        camera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // In-app notifications for follow requests (Android 13+ permission safe)
        NotificationHelper.ensureChannel(this)
        NotificationHelper.maybeRequestPostNotifications(this)
        NotificationHelper.startFollowRequestListener(this)
        NotificationHelper.startMessageListeners(this)
        NotificationHelper.startScreenshotListeners(this)
    }

    fun uploadStory(uri: Uri, type: String) {

        val input = contentResolver.openInputStream(uri)
        if (input == null) {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = BitmapFactory.decodeStream(input)

        // Resize and compress the image
        val resized = Bitmap.createScaledBitmap(bitmap, 720, 720, true)

        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val bytes = stream.toByteArray()

        // Convert to Base64 without wrapping (important)
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val request = object : StringRequest(
            Method.POST,
            "http://sociallyah.atwebpages.com/upload_story.php",
            { response ->
                Log.d("UPLOAD", response)
                Toast.makeText(this, "Story uploaded", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("UPLOAD_ERROR", error.toString())
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                val userId = prefs.getString("userId", "") ?: ""
                val params = HashMap<String, String>()
                params["userId"] = userId
                params["media"] = base64
                params["type"] = type
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    fun fetchStories() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: return

        val request = object : StringRequest(
            Method.POST,
            "http://sociallyah.atwebpages.com/get_story.php",
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    val storyList = mutableListOf<Story>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val storyUserId = obj.getString("userId")

                        // Skip your own story
                        if (storyUserId == currentUserId) continue

                        storyList.add(
                            Story(
                                id = obj.getString("id"),
                                userId = storyUserId,
                                mediaBase64 = obj.getString("media"),
                                mediaType = obj.getString("type"),
                                timestamp = obj.getLong("timestamp"),
                                username = obj.getString("username"),
                                dp = obj.getString("dp")
                            )
                        )
                    }

                    storyAdapter.apply {
                        stories.clear()
                        stories.addAll(storyList)
                        notifyDataSetChanged()
                    }

                } catch (e: Exception) {
                    Log.e("FETCH_STORIES", "JSON parse error", e)
                }
            },
            { error -> Log.e("FETCH_STORIES", error.toString()) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("userId" to currentUserId)
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
    fun fetchPosts() {
        val url = "http://sociallyah.atwebpages.com/get_post.php"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    if (response.isEmpty()) {
                        Toast.makeText(this, "No posts found", Toast.LENGTH_LONG).show()
                        return@StringRequest
                    }

                    val jsonArray = JSONArray(response)
                    postList.clear()

                    if (jsonArray.length() > 0) {
                        val obj = jsonArray.getJSONObject(0) // Only the most recent post
                        val mediaBase64 = obj.getString("media")
                        val mediaType = obj.getString("media_type")

                        val post = Post(
                            postId = obj.getString("post_id"),
                            userId = obj.getString("user_id"),
                            mediaBase64List = mutableListOf(mediaBase64),
                            mediaTypeList = mutableListOf(mediaType),
                            timestamp = obj.getLong("timestamp"),
                            username = obj.optString("username", ""),
                            caption = "", // Add captions if you store them
                            userProfileBase64 = obj.optString("dp", ""),
                            likes = mutableListOf()
                        )

                        postList.add(post)
                    }

                    postAdapter.notifyDataSetChanged()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to parse posts", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Fetch failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }




    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "IMG_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        photoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (photoUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(intent)
    }
    override fun onStart(){
        super.onStart()

    }
    override fun onStop(){
        super.onStop()

    }
}
