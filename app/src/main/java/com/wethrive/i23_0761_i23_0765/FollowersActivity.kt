package com.wethrive.i23_0761_i23_0765

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FollowersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_followers)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val emptyView = findViewById<TextView>(R.id.emptyView)
        val adapter = FriendsAdapter(mutableListOf())

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val followersRef = FirebaseDatabase.getInstance().getReference("Followers").child(currentUid)
        followersRef.get().addOnSuccessListener { snap ->
            val ids = snap.children.mapNotNull { it.key }.toList()
            if (ids.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            val usersRef = FirebaseDatabase.getInstance().getReference("Users")
            val list = mutableListOf<UserData>()
            var processed = 0

            for (id in ids) {
                usersRef.child(id).get().addOnSuccessListener { s ->
                    if (s.exists()) {
                        val uname = s.child("uname").getValue(String::class.java) ?: "Unknown"
                        val email = s.child("email").getValue(String::class.java) ?: ""
                        val dp = s.child("dp").getValue(String::class.java) ?: ""
                        val bio = s.child("bio").getValue(String::class.java) ?: ""
                        list.add(UserData(id = id, uname = uname, email = email, dp = dp, bio = bio))
                    }
                    processed++
                    if (processed == ids.size) {
                        adapter.submitList(list)
                        emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }.addOnFailureListener {
                    processed++
                    if (processed == ids.size) {
                        adapter.submitList(list)
                        emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load followers: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

