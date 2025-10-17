package com.wethrive.i23_0761_i23_0765

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class friends : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FriendsAdapter
    private lateinit var emptyView: TextView
    private val usersRef: DatabaseReference by lazy { FirebaseDatabase.getInstance().getReference("Users") }
    private var usersListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friends)

        recycler = findViewById(R.id.recycler)
        emptyView = findViewById(R.id.emptyView)

        adapter = FriendsAdapter(mutableListOf())

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadUsers(currentUid)
    }

    private fun loadUsers(currentUid: String) {
        // Remove previous listener if any (avoid duplicates on configuration changes without recreation)
        usersListener?.let { usersRef.removeEventListener(it) }

        usersListener = usersRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val list = mutableListOf<UserData>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    if (uid == currentUid) continue // exclude current user

                    val uname = child.child("uname").getValue(String::class.java) ?: "Unknown"
                    val email = child.child("email").getValue(String::class.java) ?: ""
                    val dp = child.child("dp").getValue(String::class.java) ?: ""
                    val bio = child.child("bio").getValue(String::class.java) ?: ""
                    list.add(UserData(id = uid, uname = uname, email = email, dp = dp, bio = bio))
                }
                adapter.submitList(list)
                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@friends, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.let { usersRef.removeEventListener(it) }
    }
}