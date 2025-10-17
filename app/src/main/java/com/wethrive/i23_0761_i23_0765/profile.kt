package com.wethrive.i23_0761_i23_0765

import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView


import android.graphics.BitmapFactory
import androidx.collection.emptyLongSet
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class profile : AppCompatActivity() {
    private var targetId: String? = null

    // Declare views as class-level properties
    private lateinit var dp: CircleImageView
    private lateinit var uname: TextView
    private lateinit var bio: TextView
    private lateinit var name: TextView
    private lateinit var follow: MaterialButton
    private lateinit var profile_bottom: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile)

        // Initialize views
        dp = findViewById(R.id.dp)
        uname = findViewById(R.id.uname)
        bio = findViewById(R.id.bio)
        name = findViewById(R.id.name)
        profile_bottom=findViewById(R.id.profile_bottom)
        follow=findViewById(R.id.follow)
        val posts= findViewById<TextView>(R.id.posts)
        val followers= findViewById<TextView>(R.id.followers)
        val following= findViewById<TextView>(R.id.following)

        val current= FirebaseAuth.getInstance().currentUser!!.uid
        current.toString()

        val databaseref= FirebaseDatabase.getInstance().getReference("Requests")

        // Receive the targetId from the intent
        targetId = intent.getStringExtra("id")
        val target = targetId ?: return

        val requestsRef = FirebaseDatabase.getInstance().getReference("Requests")
        val followingRef = FirebaseDatabase.getInstance().getReference("Following")
        val followersRef = FirebaseDatabase.getInstance().getReference("Followers")

        fun updateButtonState(state: String) {
            when (state) {
                "Follow" -> {
                    follow.text = "Follow"
                    follow.backgroundTintList = resources.getColorStateList(R.color.SMD)
                    follow.setTextColor(resources.getColor(R.color.white))
                }
                "Requested" -> {
                    follow.text = "Requested"
                    follow.backgroundTintList = resources.getColorStateList(R.color.purpleblue)
                    follow.setTextColor(resources.getColor(R.color.white))
                }
                "Following" -> {
                    follow.text = "Following"
                    follow.backgroundTintList = resources.getColorStateList(R.color.bluish)
                    follow.setTextColor(resources.getColor(R.color.SMD))
                }
            }
        }

        // --- STEP 1: Detect current relationship state ---
        fun detectState() {
            followingRef.child(current).child(target).get().addOnSuccessListener { followingSnap ->
                if (followingSnap.exists()) {
                    updateButtonState("Following")
                } else {
                    requestsRef.child(target).child(current).get().addOnSuccessListener { requestSnap ->
                        if (requestSnap.exists()) {
                            updateButtonState("Requested")
                        } else {
                            updateButtonState("Follow")
                        }
                    }
                }
            }
        }

// --- STEP 2: Handle button clicks ---
        follow.setOnClickListener {
            when (follow.text.toString()) {
                "Follow" -> {
                    requestsRef.child(target).child(current).setValue("pending").addOnSuccessListener {
                        Toast.makeText(this, "Follow request sent!", Toast.LENGTH_SHORT).show()
                        updateButtonState("Requested")
                    }
                }

                "Requested" -> {
                    requestsRef.child(target).child(current).removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Follow request canceled", Toast.LENGTH_SHORT).show()
                        updateButtonState("Follow")
                    }
                }

                "Following" -> {
                    // Remove from Followers and Following
                    followingRef.child(current).child(target).removeValue()
                    followersRef.child(target).child(current).removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()
                        updateButtonState("Follow")
                    }
                }
            }
        }

        detectState()

        // YAHAN PY DATABASE SY UTH K NUMBER OF POSTS AUR FOLLOWERS AUR FOLLOWING AARHY HAIN
        var ref=FirebaseDatabase.getInstance().getReference("Followers").child(targetId!!)

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

        ref=FirebaseDatabase.getInstance().getReference("Following").child(targetId!!)

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

        ref=FirebaseDatabase.getInstance().getReference("Posts").child(targetId!!)

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

        if (targetId.isNullOrEmpty()) {
            Toast.makeText(this, "No user ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserProfile(targetId!!,current)
    }

    private fun loadUserProfile(target: String, current: String) {
        Toast.makeText(this, "Loading profile for user: $target", Toast.LENGTH_SHORT).show()

        val databaseref= FirebaseDatabase.getInstance().getReference("Users").child(target)
        val currentref= FirebaseDatabase.getInstance().getReference("Users").child(current)

        if(target!= null)
        {
            databaseref.child("bio").get().addOnSuccessListener {
                if(it.exists())
                {
                    val biography= it.getValue(String::class.java)
                    bio.text=biography
                }
            }

            databaseref.child("uname").get().addOnSuccessListener {
                if(it.exists())
                {
                    val username= it.getValue(String::class.java)
                    uname.text=username
                    name.text=username

                }
            }

            databaseref.child("dp").get().addOnSuccessListener {
                if(it.exists())
                {
                    val profile= it.getValue(String::class.java)
                    // Attempt to decode base64 avatar if present
                    if (!profile.isNullOrBlank()) {

                        val imageBytes = Base64.decode(profile, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        dp.setImageBitmap(bitmap)
                    }
                }
            }

            currentref.child("dp").get().addOnSuccessListener {
                if(it.exists())
                {
                    val profile= it.getValue(String::class.java)
                    // Attempt to decode base64 avatar if present
                    if (!profile.isNullOrBlank()) {

                        val imageBytes = Base64.decode(profile, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        profile_bottom.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}