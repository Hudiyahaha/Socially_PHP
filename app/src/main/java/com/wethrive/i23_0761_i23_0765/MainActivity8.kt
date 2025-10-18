package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity8 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)

        val exit = findViewById<ImageView>(R.id.exit_dm)
        val plus = findViewById<ImageView>(R.id.plus)
        val titleUsername = findViewById<TextView>(R.id.username)

        // Load current user's username to show in the top bar
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid
        if (currentUid == null) {
            titleUsername.text = "Guest"
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(currentUid).child("uname")
            ref.get().addOnSuccessListener { snapshot ->
                val uname = snapshot.getValue(String::class.java) ?: "Unknown"
                titleUsername.text = uname
            }.addOnFailureListener {
                titleUsername.text = "Unknown"
                Toast.makeText(this, "Failed to load username", Toast.LENGTH_SHORT).show()
            }
        }

        // RecyclerView for DM chats
        val recycler = findViewById<RecyclerView>(R.id.recyclerDM)
        val emptyView = findViewById<TextView>(R.id.empty_dm)

        val adapter = DMAdapter(mutableListOf())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        recycler.setHasFixedSize(true)

        // Load all registered users from Firebase, excluding current user
        if (currentUid == null) {
            // Not signed in: show empty message
            emptyView.visibility = android.view.View.VISIBLE
        } else {
            val usersRef = FirebaseDatabase.getInstance().getReference("Users")
            usersRef.get().addOnSuccessListener { snapshot ->
                val list = mutableListOf<DMItem>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    if (uid == currentUid) continue

                    val uname = child.child("uname").getValue(String::class.java) ?: "Unknown"
                    val dp = child.child("dp").getValue(String::class.java) ?: ""
                    // lastMessage and time are not available here; leave blank for now
                    list.add(DMItem(id = uid, name = uname, lastMessage = "", time = "", dp = if (dp.isBlank()) null else dp))
                }
                adapter.submitList(list)
                emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load users: ${e.message}", Toast.LENGTH_SHORT).show()
                emptyView.visibility = android.view.View.VISIBLE
            }
        }

        exit.setOnClickListener {
            finish()
        }

        plus.setOnClickListener {
            val intent = Intent(this, MainActivity9::class.java)
            startActivity(intent)
        }

    }
}