package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity13 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)

        val highlight=findViewById<LinearLayout>(R.id.highlight)
        val edit_profile=findViewById<Button>(R.id.edit_profile)

        val uname=findViewById<TextView>(R.id.uname)
        val home=findViewById<ImageView>(R.id.home)
        val create = findViewById<ImageView>(R.id.create)
        val notis = findViewById<ImageView>(R.id.heart)
        val search = findViewById<ImageView>(R.id.search)
        val logoutButton = findViewById<TextView>(R.id.logout)
        var profile=findViewById<CircleImageView>(R.id.profile)
        var profile_bottom=findViewById<CircleImageView>(R.id.profile_bottom)
        val user=findViewById<TextView>(R.id.user)
        val bio= findViewById<TextView>(R.id.bio)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            databaseRef.child("bio").get().addOnSuccessListener {
                if(it.exists())
                {
                    val biography= it.getValue(String::class.java)
                    bio.text=biography
                }
            }

            databaseRef.child("uname").get().addOnSuccessListener {
                if(it.exists())
                {
                    val username= it.getValue(String::class.java)
                    uname.text=username
                    user.text=username

                }
            }
            databaseRef.child("dp").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val imageString = snapshot.getValue(String::class.java)

                        if (imageString != null) {
                            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profile.setImageBitmap(bitmap)
                            profile_bottom.setImageBitmap(bitmap)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Error: ${it.message}")
                }
        }
        val recyclerView = findViewById<RecyclerView>(R.id.postRecycler)
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)


        if (uid != null) {
            val postsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Posts").child(uid)
            val postList = mutableListOf<String>()
            val postIds = mutableListOf<String>()
            val adapter = PostAdapter(postList, postIds) { postId ->
                val intent = Intent(this, ViewPost::class.java)
                intent.putExtra("uid", uid)
                intent.putExtra("postId", postId)
                startActivity(intent)
            }
            recyclerView.adapter = adapter

            postsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    postList.clear()
                    postIds.clear()

                    for (postSnapshot in snapshot.children) {
                        val imagesNode = postSnapshot.child("images")
                        val firstImage = imagesNode.children.firstOrNull()?.getValue(String::class.java)
                        if (firstImage != null) {
                            postList.add(firstImage)
                            postIds.add(postSnapshot.key!!)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("Firebase", "Failed to load posts: ${error.message}")
                }
            })



        }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity4::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        search.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        notis.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        create.setOnClickListener {
            val intent = Intent(this, MainActivity16::class.java)
            startActivity(intent)
        }

        home.setOnClickListener {
            val intent= Intent(this, MainActivity5::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        highlight.setOnClickListener {
            val intent= Intent(this, MainActivity14::class.java)
            startActivity(intent)
        }

        edit_profile.setOnClickListener {
            val intent= Intent(this, MainActivity15::class.java)
            startActivity(intent)
        }

    }
}