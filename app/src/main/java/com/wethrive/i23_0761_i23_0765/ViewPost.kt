package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class ViewPost : AppCompatActivity() {

    private lateinit var likeButton: ImageView
    private lateinit var likeCountText: TextView
    private lateinit var postRef: DatabaseReference
    private lateinit var currentUid: String
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_post)


        likeButton = findViewById(R.id.likeButton)
        likeCountText = findViewById(R.id.likeCount)
        val recyclerView = findViewById<RecyclerView>(R.id.postImagesRecycler)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val uid = intent.getStringExtra("uid")!!
        val postId = intent.getStringExtra("postId")!!
        currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        postRef = FirebaseDatabase.getInstance()
            .getReference("Posts").child(uid).child(postId)

        // ---------- Load media ----------
        val imagesRef = postRef.child("mediaBase64List")
        val imageList = mutableListOf<String>()
        val adapter = ImageAdapter(imageList)
        recyclerView.adapter = adapter

        imagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                imageList.clear()
                for (imgSnapshot in snapshot.children) {
                    val base64 = imgSnapshot.getValue(String::class.java)
                    if (base64 != null) imageList.add(base64)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
      //Comments
        val commentsRef = postRef.child("comments")
        val commentsList = mutableListOf<Comment>()
        val commentRecycler=findViewById<RecyclerView>(R.id.commentsRecycler)
        val commentInput=findViewById<EditText>(R.id.commentInput)
        val sendComment=findViewById<ImageView>(R.id.sendComment)
        val commentsAdapter = CommentAdapter(commentsList)
        commentRecycler.layoutManager= LinearLayoutManager(this)
        commentRecycler.adapter=commentsAdapter

        commentsRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
               commentsList.clear()
                for(commentSnap in snapshot.children){
                    val comment=commentSnap.getValue(Comment::class.java)
                    if(comment!=null) {
                       commentsList.add(comment)
                    }
                    commentsAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ViewPost", "Failed to load comments: ${error.message}")
            }
        })

        sendComment.setOnClickListener {
            val text=commentInput.text.toString()
            if(text.isNotEmpty()) {
                val user = FirebaseAuth.getInstance().currentUser
                val currentUserId = user?.uid ?: return@setOnClickListener
                val commentId = commentsRef.push().key ?: System.currentTimeMillis().toString()
                val userRef2 =
                    FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
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

        // ---------- Handle Like Button ----------
        val likesRef = postRef.child("likes")
        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likesList = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                val likeCount = likesList.size
                likeCountText.text = "$likeCount likes"

                isLiked = likesList.contains(currentUid)
                likeButton.setImageResource(
                    if (isLiked) R.drawable.heart_filled else R.drawable.like
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        likeButton.setOnClickListener {
            toggleLike()
        }

        // ---------- Load profile ----------
        val profile = findViewById<CircleImageView>(R.id.profile)
        val usernameText = findViewById<TextView>(R.id.username)

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        userRef.get().addOnSuccessListener { snapshot ->
            val uname = snapshot.child("uname").getValue(String::class.java) ?: "Unknown"
            usernameText.text = uname

            val dpBase64 = snapshot.child("dp").getValue(String::class.java)
            if (!dpBase64.isNullOrEmpty()) {
                val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profile.setImageBitmap(bitmap)
            }
        }
    }

    private fun toggleLike() {
        val likesRef = postRef.child("likes")

        likesRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentLikes = mutableData.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                if (currentLikes.contains(currentUid)) {
                    currentLikes.remove(currentUid)
                } else {
                    currentLikes.add(currentUid)
                }
                mutableData.value = currentLikes
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Toast.makeText(this@ViewPost, "Failed to like post: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
