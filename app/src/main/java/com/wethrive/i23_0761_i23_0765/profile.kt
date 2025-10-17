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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

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

        val databaseref= FirebaseDatabase.getInstance().getReference("Requests")


        follow.setOnClickListener {

            follow.text="Requested"
            follow.backgroundTintList=resources.getColorStateList(R.color.purpleblue)
            follow.setTextColor(resources.getColor(R.color.white))

            val sender= FirebaseAuth.getInstance().currentUser!!.uid
            databaseref.child(targetId!!).child(sender).setValue("pending")
                .addOnSuccessListener {
                    Toast.makeText(this, "Follow request sent!", Toast.LENGTH_SHORT).show()
                }

        }

        // Receive the targetId from the intent
        targetId = intent.getStringExtra("id")

        if (targetId.isNullOrEmpty()) {
            Toast.makeText(this, "No user ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Now you can use the targetId to load user profile data
        loadUserProfile(targetId!!)
    }

    private fun loadUserProfile(uid: String) {
        // TODO: Load user profile data from Firebase using the uid
        // For now, just showing that we received the ID
        Toast.makeText(this, "Loading profile for user: $uid", Toast.LENGTH_SHORT).show()

        val databaseref= FirebaseDatabase.getInstance().getReference("Users").child(uid)

        if(uid!= null)
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
                        profile_bottom.setImageBitmap(bitmap)
                        dp.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}