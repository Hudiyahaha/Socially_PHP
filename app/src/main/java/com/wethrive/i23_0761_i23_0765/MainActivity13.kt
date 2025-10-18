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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
        val profile=findViewById<CircleImageView>(R.id.profile)
        val profile_bottom=findViewById<CircleImageView>(R.id.profile_bottom)
        val user=findViewById<TextView>(R.id.user)
        val bio= findViewById<TextView>(R.id.bio)
        val posts= findViewById<TextView>(R.id.posts)
        val followers= findViewById<TextView>(R.id.followers)
        val following= findViewById<TextView>(R.id.following)
        val followers_screen= findViewById<LinearLayout>(R.id.follower_screen)
        val following_screen= findViewById<LinearLayout>(R.id.following_screen)





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

// YAHAN PY DATABASE SY UTH K NUMBER OF POSTS AUR FOLLOWERS AUR FOLLOWING AARHY HAIN
            var ref=FirebaseDatabase.getInstance().getReference("Followers").child(uid)

           ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    followers.text = count.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                    followers.text="-1"
                }
            })

             ref=FirebaseDatabase.getInstance().getReference("Following").child(uid)

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    following.text = count.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                    following.text="-1"
                }
            })

            ref=FirebaseDatabase.getInstance().getReference("Posts").child(uid)

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    posts.text = count.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                    posts.text="-1"
                }
            })
//----------------------------------------------------------------------------separator


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
            val postsRef = FirebaseDatabase.getInstance()
                .getReference("Posts").child(uid)
            val postList = mutableListOf<String>()
            val postIds = mutableListOf<String>()
            val mediaTypes = mutableListOf<String>()
            val adapter = PostAdapter(postList, postIds, mediaTypes) { postId ->
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
                    mediaTypes.clear()
                    for (postSnapshot in snapshot.children) {
                        val mediaList = postSnapshot.child("mediaBase64List").children
                        val firstMedia = mediaList.firstOrNull()?.getValue(String::class.java)
                        val mediaType = postSnapshot.child("mediaType").getValue(String::class.java) ?: "image"

                        if (firstMedia != null) {
                            postList.add(firstMedia)
                            postIds.add(postSnapshot.key!!)
                            mediaTypes.add(mediaType) // ✅ add the correct type
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

        // Open followers / following screens
        followers_screen.setOnClickListener {
            val intent = Intent(this, FollowersActivity::class.java)
            startActivity(intent)
        }

        following_screen.setOnClickListener {
            val intent = Intent(this, FollowingActivity::class.java)
            startActivity(intent)
        }

    }
}