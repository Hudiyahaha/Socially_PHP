package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class ViewPost : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_post)

        val recyclerView = findViewById<RecyclerView>(R.id.postImagesRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val uid = intent.getStringExtra("uid")!!
        val postId = intent.getStringExtra("postId")!!

        val imagesRef = FirebaseDatabase.getInstance()
            .getReference("Posts").child(uid).child(postId).child("images")

        val imageList = mutableListOf<String>()
        val adapter = ImageAdapter(imageList)
        recyclerView.adapter = adapter

        imagesRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                imageList.clear()
                for (imgSnapshot in snapshot.children) {
                    val base64 = imgSnapshot.getValue(String::class.java)
                    if (base64 != null) imageList.add(base64)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        val profile = findViewById<CircleImageView>(R.id.profile)

        // Load profile picture
        val uid2 = FirebaseAuth.getInstance().currentUser?.uid
        if (uid2 != null) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid2)
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
            val uid2 = user.uid
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid2)

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
}
