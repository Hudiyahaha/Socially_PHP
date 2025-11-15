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
import com.google.firebase.database.*

class Requests : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: RequestsAdapter
    private lateinit var emptyView: TextView
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val usersRef: DatabaseReference by lazy { database.getReference("Users") }
    private var requestsListener: ValueEventListener? = null
    private lateinit var currentUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_requests)

        recycler = findViewById(R.id.recycler)
        emptyView = findViewById(R.id.emptyView)

        adapter = RequestsAdapter(
            mutableListOf(),
            onAccept = { user, position -> acceptRequest(user, position) },
            onReject = { user, position -> rejectRequest(user, position) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUid = currentUser.uid
        loadFollowRequests()
    }

    private fun loadFollowRequests() {
        // Listen to follow requests for the current user
        val requestsRef = database.getReference("Requests").child(currentUid)

        requestsListener = requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("Requests", "onDataChange called. Snapshot exists: ${snapshot.exists()}")
                android.util.Log.d("Requests", "Snapshot children count: ${snapshot.childrenCount}")

                val list = mutableListOf<UserData>()
                val pendingUsers = mutableListOf<String>()

                // Get all user IDs who have requested to follow
                for (child in snapshot.children) {
                    val userId = child.key ?: continue
                    val status = child.getValue(String::class.java)
                    android.util.Log.d("Requests", "Found child: userId=$userId, status=$status")
                    if (status == "pending") {
                        pendingUsers.add(userId)
                    }
                }

                android.util.Log.d("Requests", "Total pending users: ${pendingUsers.size}")

                if (pendingUsers.isEmpty()) {
                    adapter.submitList(list)
                    emptyView.visibility = View.VISIBLE
                    return
                }

                // Fetch user details for each pending request
                var loadedCount = 0
                for (userId in pendingUsers) {
                    android.util.Log.d("Requests", "Loading user details for: $userId")
                    usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            android.util.Log.d("Requests", "User data loaded for $userId, exists: ${userSnapshot.exists()}")
                            if (userSnapshot.exists()) {
                                val uname = userSnapshot.child("uname").getValue(String::class.java) ?: "Unknown"
                                val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                                val dp = userSnapshot.child("dp").getValue(String::class.java) ?: ""
                                val bio = userSnapshot.child("bio").getValue(String::class.java) ?: ""
                                android.util.Log.d("Requests", "Adding user: $uname")
                                list.add(UserData(id = userId, uname = uname, email = email, dp = dp, bio = bio))
                            }
                            loadedCount++
                            if (loadedCount == pendingUsers.size) {
                                android.util.Log.d("Requests", "All users loaded. Total: ${list.size}")
                                adapter.submitList(list)
                                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            android.util.Log.e("Requests", "Failed to load user $userId: ${error.message}")
                            loadedCount++
                            if (loadedCount == pendingUsers.size) {
                                adapter.submitList(list)
                                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("Requests", "Database error: ${error.message}")
                Toast.makeText(this@Requests, "Failed to load requests: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun acceptRequest(user: UserData, position: Int) {
        val userId = user.id ?: return

        // Add to followers list
        database.getReference("Followers").child(currentUid).child(userId).setValue(true)
            .addOnSuccessListener {
                // Add current user to the requester's following list
                database.getReference("Following").child(userId).child(currentUid).setValue(true)

                // Remove the request
                database.getReference("Requests").child(currentUid).child(userId).removeValue()
                    .addOnSuccessListener {
                        adapter.removeItem(position)
                        Toast.makeText(this, "Request accepted", Toast.LENGTH_SHORT).show()

                        // Show empty view if no more requests
                        if (adapter.itemCount == 0) {
                            emptyView.visibility = View.VISIBLE
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectRequest(user: UserData, position: Int) {
        val userId = user.id ?: return

        // Remove the request
        database.getReference("Requests").child(currentUid).child(userId).removeValue()
            .addOnSuccessListener {
                adapter.removeItem(position)
                Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()

                // Show empty view if no more requests
                if (adapter.itemCount == 0) {
                    emptyView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reject request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        requestsListener?.let {
            database.getReference("Requests").child(currentUid).removeEventListener(it)
        }
    }
}
