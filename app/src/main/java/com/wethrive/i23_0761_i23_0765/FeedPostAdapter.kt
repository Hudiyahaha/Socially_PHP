package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class FeedPostAdapter(private val postList: MutableList<Post>) :
    RecyclerView.Adapter<FeedPostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImagesRecycler: RecyclerView = itemView.findViewById(R.id.postImagesRecycler)
        val username: TextView = itemView.findViewById(R.id.username)
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val likeCount: TextView = itemView.findViewById(R.id.likeCount)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_feed, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Setup inner RecyclerView for post images/videos
        holder.postImagesRecycler.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.postImagesRecycler.adapter =
            PostMediaAdapter(post.mediaBase64List, post.mediaTypeList)

        // Load username and profile pic
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(post.userId)
        userRef.child("uname").get().addOnSuccessListener {
            holder.username.text = it.getValue(String::class.java) ?: "Unknown"
        }
        userRef.child("dp").get().addOnSuccessListener { snapshot ->
            val base64 = snapshot.getValue(String::class.java)
            if (!base64.isNullOrEmpty()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            }
        }

        // --- LIKE LOGIC ---
        val postRef = FirebaseDatabase.getInstance()
            .getReference("Posts")
            .child(post.userId)
            .child(post.postId)
            .child("likes")

        // Update like button & count UI
        val likes = post.likes ?: mutableListOf()
        val isLiked = likes.contains(currentUid)
        holder.likeButton.setImageResource(
            if (isLiked) R.drawable.heart_filled else R.drawable.like
        )
        holder.likeCount.text = "${likes.size} likes"

        // Like button click
        holder.likeButton.setOnClickListener {
            if (isLiked) {
                // Unlike post
                postRef.get().addOnSuccessListener { snapshot ->
                    val updatedLikes = likes.toMutableList().apply { remove(currentUid) }
                    postRef.setValue(updatedLikes)
                    post.likes = updatedLikes
                    notifyItemChanged(position)
                }
            } else {
                // Like post
                postRef.get().addOnSuccessListener { snapshot ->
                    val updatedLikes = likes.toMutableList().apply { add(currentUid) }
                    postRef.setValue(updatedLikes)
                    post.likes = updatedLikes
                    notifyItemChanged(position)
                }
            }
        }
        // COMMENTS
        val commentsRecycler = holder.itemView.findViewById<RecyclerView>(R.id.commentsRecycler)
        val commentInput = holder.itemView.findViewById<EditText>(R.id.commentInput)
        val sendComment = holder.itemView.findViewById<ImageView>(R.id.sendComment)

        val commentsRef = FirebaseDatabase.getInstance()
            .getReference("Posts")
            .child(post.userId)
            .child(post.postId)
            .child("comments")

        val commentList = mutableListOf<Comment>()
        val commentAdapter = CommentAdapter(commentList)
        commentsRecycler.layoutManager = LinearLayoutManager(holder.itemView.context)
        commentsRecycler.adapter = commentAdapter

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (commentSnap in snapshot.children) {
                    val comment = commentSnap.getValue(Comment::class.java)
                    if (comment != null) commentList.add(comment)
                }
                commentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        sendComment.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val user = FirebaseAuth.getInstance().currentUser
                val commentId = commentsRef.push().key ?: System.currentTimeMillis().toString()
                val currentUserId = user?.uid ?: return@setOnClickListener

                val userRef2 = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                userRef2.child("uname").get().addOnSuccessListener { unameSnap ->
                    val uname = unameSnap.getValue(String::class.java) ?: "User"
                    val comment = Comment(
                        commentId = commentId,
                        userId = currentUserId,
                        username = uname,
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                    commentsRef.child(commentId).setValue(comment)
                    commentInput.text.clear()
                }
            }
        }


    }

    override fun getItemCount(): Int = postList.size
}
