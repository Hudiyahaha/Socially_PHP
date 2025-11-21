package com.wethrive.i23_0761_i23_0765

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
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

class FeedPostAdapter(private val postList: MutableList<Post>) :
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
            PostMediaAdapter(post.mediaBase64List, post.mediaTypeList)

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
        val isLiked = post.likes.contains("1")   // you can replace "1" with actual session user id later
        holder.likeButton.setImageResource(if (isLiked) R.drawable.heart_filled else R.drawable.like)
        holder.likeCount.text = "${post.likes.size} likes"

        // --- LIKE CLICK ---
        holder.likeButton.setOnClickListener {
            val url = "http://sociallyah.atwebpages.com/like_post.php"

            val request = object : StringRequest(Method.POST, url,
                { response ->
                    if (isLiked) post.likes.remove("1") else post.likes.add("1")
                    notifyItemChanged(position)
                },
                { error ->
                    Toast.makeText(ctx, "Like failed", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "postId" to post.postId,
                        "userId" to post.userId  // or session id
                    )
                }
            }

            queue.add(request)
        }

        // --- COMMENTS ---
        val commentList = post.comments
        val commentAdapter = CommentAdapter(commentList)
        holder.commentsRecycler.layoutManager = LinearLayoutManager(ctx)
        holder.commentsRecycler.adapter = commentAdapter

        // Send comment
        holder.sendComment.setOnClickListener {
            val txt = holder.commentInput.text.toString().trim()
            if (txt.isEmpty()) return@setOnClickListener

            val url = "http://sociallyah.atwebpages.com/comment_post.php"

            val request = object : StringRequest(Method.POST, url,
                {
                    val newComment = Comment(
                        commentId = System.currentTimeMillis().toString(),
                        userId = post.userId,
                        username = post.username ?: "User",
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
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "postId" to post.postId,
                        "userId" to post.userId,
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
}
