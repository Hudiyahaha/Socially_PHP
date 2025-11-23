package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ViewPost : AppCompatActivity() {

    private lateinit var likeButton: ImageView
    private lateinit var likeCountText: TextView
    private lateinit var postId: String
    private lateinit var uid: String
    private var isLiked = false
    private val likes = mutableListOf<String>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var sendComment: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_post)

        likeButton = findViewById(R.id.likeButton)
        likeCountText = findViewById(R.id.likeCount)
        recyclerView = findViewById(R.id.postImagesRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        commentInput = findViewById(R.id.commentInput)
        sendComment = findViewById(R.id.sendComment)

        postId = intent.getStringExtra("postId")!!
        uid = intent.getStringExtra("uid")!!

        fetchPostData()
    }

    private fun fetchPostData() {
        val url = "http://sociallyah.atwebpages.com/get_post.php"
        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Request.Method.POST, url,
            StringRequest@{ response ->
                try {
                    val jsonObj = JSONObject(response)
                    val postsArray = jsonObj.getJSONArray("posts")
                    var postObj: JSONObject? = null

                    for (i in 0 until postsArray.length()) {
                        val obj = postsArray.getJSONObject(i)
                        if (obj.getString("post_id") == postId) {
                            postObj = obj
                            break
                        }
                    }

                    if (postObj == null) {
                        Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show()
                        finish()
                        return@StringRequest
                    }

                    displayPost(postObj)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to parse post", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(
                    this,
                    "Fetch failed: ${error.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("userId" to uid)
            }
        }
        queue.add(request)
    }


    private fun displayPost(postObj: JSONObject) {
        val mediaUrlList = mutableListOf<String>()
        val mediaTypeList = mutableListOf<String>()

        try {
            val mediaPath = postObj.getString("file_path")  // server file path
            val mediaType = postObj.getString("media_type")
            val mediaUrl = "http://sociallyah.atwebpages.com/i.php?p=$mediaPath"

            mediaUrlList.add(mediaUrl)
            mediaTypeList.add(mediaType)

            recyclerView.adapter = PostMediaAdapter(mediaUrlList, mediaTypeList)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading media", Toast.LENGTH_SHORT).show()
        }

        // Username & profile (still Base64)
        val usernameText = findViewById<TextView>(R.id.username)
        val profileImage = findViewById<CircleImageView>(R.id.profile)
        usernameText.text = postObj.optString("username", "Unknown")
        val dpBase64 = postObj.optString("dp", "")
        if (dpBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profileImage.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Likes
        likes.clear()
        val likesStr = postObj.optString("likes", "")
        if (likesStr.isNotEmpty()) likes.addAll(likesStr.split(","))
        updateLikeUI()

        likeButton.setOnClickListener {
            // handle like API call if needed
        }

        // Comments
        val commentsRecycler = findViewById<RecyclerView>(R.id.commentsRecycler)
        val commentsList = mutableListOf<Comment>()
        val commentAdapter = CommentAdapter(commentsList)
        commentsRecycler.layoutManager = LinearLayoutManager(this)
        commentsRecycler.adapter = commentAdapter

        sendComment.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                // postComment API logic if needed
                commentInput.text.clear()
            }
        }
    }


    private fun updateLikeUI() {
        val likeCount = likes.size
        likeCountText.text = "$likeCount likes"
        isLiked = likes.contains(uid)
        likeButton.setImageResource(if (isLiked) R.drawable.heart_filled else R.drawable.like)
    }
}
