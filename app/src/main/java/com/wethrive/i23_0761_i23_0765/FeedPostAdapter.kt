package com.wethrive.i23_0761_i23_0765

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class FeedPostAdapter(private val postList: MutableList<Post>, private val currentUserId: String, private val context: Context) :
    RecyclerView.Adapter<FeedPostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImagesRecycler: RecyclerView = itemView.findViewById(R.id.postImagesRecycler)
        val username: TextView = itemView.findViewById(R.id.username)
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val likeCount: TextView = itemView.findViewById(R.id.likeCount)
        val sharePost: ImageView = itemView.findViewById(R.id.sharePost)

        val commentsRecycler: RecyclerView = itemView.findViewById(R.id.commentsRecycler)
        val commentInput: EditText = itemView.findViewById(R.id.commentInput)
        val sendComment: ImageView = itemView.findViewById(R.id.sendComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_feed, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val post = postList[position]
        val ctx = holder.itemView.context
        val queue = Volley.newRequestQueue(ctx)

        // --- POST MEDIA ---
        holder.postImagesRecycler.layoutManager =
            LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        holder.postImagesRecycler.adapter =
            PostMediaAdapter(post.mediaUrlList, post.mediaTypeList)

        // --- USERNAME ---
        holder.username.text = post.username ?: "Unknown"

        // --- PROFILE PIC ---
        if (!post.userProfileBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(post.userProfileBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bmp)
            } catch (e: Exception) {}
        }

        // --- LIKE BUTTON UI ---
        // --- LIKE BUTTON UI ---
        holder.likeButton.setImageResource(
            if (post.isLikedByCurrentUser) R.drawable.heart_filled else R.drawable.like
        )
        holder.likeCount.text = "${post.likesCount} likes"
        Log.d("DEBUG_LIKE", "Sending like request -> userId: $currentUserId, postId: ${post.postId}")


// --- LIKE CLICK ---
        holder.likeButton.setOnClickListener {
            val url = "http://sociallyah.atwebpages.com/like.php"
            val request = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val obj = org.json.JSONObject(response)
                        val status = obj.getString("status") // liked or unliked
                        val likeCount = obj.getInt("likeCount")

                        // Update post object and UI
                        post.isLikedByCurrentUser = status == "liked"
                        post.likesCount = likeCount
                        notifyItemChanged(position)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Failed to update like", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(ctx, "Like request failed", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "postId" to post.postId,
                        "userId" to currentUserId
                    )
                }
            }
            Volley.newRequestQueue(context).add(request)
        }

        // --- COMMENTS ---
        val commentList = post.comments
        val commentAdapter = CommentAdapter(commentList)
        holder.commentsRecycler.layoutManager = LinearLayoutManager(ctx)
        holder.commentsRecycler.adapter = commentAdapter
        fetchCommentsForPost(post) {
            commentAdapter.notifyDataSetChanged()
        }

        holder.sendComment.setOnClickListener {
            val txt = holder.commentInput.text.toString().trim()
            if (txt.isEmpty()) return@setOnClickListener

            val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val currentUsername = prefs.getString("username", "User") ?: "User"

            val url = "http://sociallyah.atwebpages.com/comment_post.php"
            val request = object : StringRequest(Method.POST, url,
                { response ->
                    // Add comment locally after server confirms
                    val newComment = Comment(
                        commentId = System.currentTimeMillis().toString(),
                        userId = currentUserId,              // Correct userId
                        username = currentUsername,                     // Or fetch current user's username
                        text = txt,
                        timestamp = System.currentTimeMillis()
                    )
                    commentList.add(newComment)
                    commentAdapter.notifyDataSetChanged()
                    holder.commentInput.text.clear()
                },
                {
                    Toast.makeText(ctx, "Comment failed", Toast.LENGTH_SHORT).show()
                }
            ){
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "postId" to post.postId,
                        "userId" to currentUserId,           // Correct userId
                        "comment" to txt
                    )
                }
            }
            queue.add(request)
        }

        // --- SHARE POST ---
        holder.sharePost.setOnClickListener {
            val intent = Intent(ctx, MainActivity8::class.java)
            intent.putExtra("sharePostOwnerId", post.userId)
            intent.putExtra("sharePostId", post.postId)
            ctx.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = postList.size

    // --- New function to fetch likes for all posts ---
    fun fetchLikesForPosts() {
        val queue = Volley.newRequestQueue(context)
        val url = "http://sociallyah.atwebpages.com/getlikes.php"
        val postIds = postList.map { it.postId }

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val jsonArray = org.json.JSONArray(response)
                    for(i in 0 until jsonArray.length()){
                        val obj = jsonArray.getJSONObject(i)
                        val postId = obj.getString("postId")
                        val post = postList.find { it.postId == postId } ?: continue
                        post.likesCount = obj.getInt("likeCount")
                        post.isLikedByCurrentUser = obj.getBoolean("likedByUser")
                    }
                    notifyDataSetChanged()
                } catch(e: Exception){
                    Log.e("FETCH_LIKES", "Error parsing JSON: ${e.message}")
                }
            },
            { error ->
                Log.e("FETCH_LIKES", "Error fetching likes: ${error.message}")
            }){
            override fun getParams(): MutableMap<String, String> {
                val map = hashMapOf<String, String>()
                map["userId"] = currentUserId
                postIds.forEachIndexed { index, id ->
                    map["postIds[$index]"] = id
                }
                return map
            }
        }
        queue.add(request)
    }
    fun fetchCommentsForPost(post: Post, onComplete: () -> Unit = {}) {
        val url = "http://sociallyah.atwebpages.com/get_comments.php"
        val queue = Volley.newRequestQueue(context)

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val jsonArray = org.json.JSONArray(response)
                    post.comments.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        post.comments.add(
                            Comment(
                                commentId = obj.getString("comment_id"),
                                userId = obj.getString("user_id"),
                                username = obj.getString("username"),
                                text = obj.getString("text"),
                                timestamp = obj.getLong("timestamp")
                            )
                        )
                    }
                    onComplete()
                } catch(e: Exception) {
                    Log.e("FETCH_COMMENTS", "Error parsing JSON: ${e.message}")
                }
            },
            { error ->
                Log.e("FETCH_COMMENTS", "Error fetching comments: ${error.message}")
            }
        ){
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "postId" to post.postId
                )
            }
        }
        queue.add(request)
    }

}
