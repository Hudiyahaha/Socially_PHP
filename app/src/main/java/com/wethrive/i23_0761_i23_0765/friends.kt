package com.wethrive.i23_0761_i23_0765

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class friends : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FriendsAdapter
    private lateinit var emptyView: TextView
    private lateinit var searchBar: EditText
    private lateinit var btnFollowers: Button
    private lateinit var btnFollowing: Button

    private val usersRef: DatabaseReference by lazy { FirebaseDatabase.getInstance().getReference("Users") }
    private val followersRef by lazy { FirebaseDatabase.getInstance().getReference("Followers") }
    private val followingRef by lazy { FirebaseDatabase.getInstance().getReference("Following") }
    private var usersListener: ValueEventListener? = null
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friends)

        recycler = findViewById(R.id.recycler)
        emptyView = findViewById(R.id.emptyView)
        searchBar = findViewById(R.id.searchBar)
        btnFollowers = findViewById(R.id.btnFollowers)
        btnFollowing = findViewById(R.id.btnFollowing)

        adapter = FriendsAdapter(mutableListOf())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUsers(currentUid!!)

        // Search functionality
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(query: CharSequence?, start: Int, before: Int, count: Int) {
                val q = query.toString().trim()
                if (q.isEmpty()) {
                    loadUsers(currentUid!!)
                } else {
                    searchUsers(q)
                }
            }
        })

        // Filter by followers/following
        btnFollowers.setOnClickListener { loadFollowers(currentUid!!) }
        btnFollowing.setOnClickListener { loadFollowing(currentUid!!) }
    }

    private fun loadUsers(currentUid: String) {
        usersListener?.let { usersRef.removeEventListener(it) }
        usersListener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserData>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    if (uid == currentUid) continue
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

    private fun searchUsers(query: String) {
        usersRef.orderByChild("uname").startAt(query).endAt(query + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<UserData>()
                    for (child in snapshot.children) {
                        val uid = child.key ?: continue
                        if (uid == currentUid) continue
                        val uname = child.child("uname").getValue(String::class.java) ?: ""
                        val email = child.child("email").getValue(String::class.java) ?: ""
                        val dp = child.child("dp").getValue(String::class.java) ?: ""
                        val bio = child.child("bio").getValue(String::class.java) ?: ""
                        list.add(UserData(id = uid, uname = uname, email = email, dp = dp, bio = bio))
                    }
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadFollowers(uid: String) {
        followersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserData>()
                val usersRef = FirebaseDatabase.getInstance().getReference("Users")
                for (child in snapshot.children) {
                    val followerId = child.key ?: continue
                    usersRef.child(followerId).get().addOnSuccessListener {
                        val user = it.getValue(UserData::class.java)
                        if (user != null) {
                            list.add(user)
                            adapter.submitList(list)
                            emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadFollowing(uid: String) {
        followingRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserData>()
                val usersRef = FirebaseDatabase.getInstance().getReference("Users")
                for (child in snapshot.children) {
                    val followingId = child.key ?: continue
                    usersRef.child(followingId).get().addOnSuccessListener {
                        val user = it.getValue(UserData::class.java)
                        if (user != null) {
                            list.add(user)
                            adapter.submitList(list)
                            emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.let { usersRef.removeEventListener(it) }
    }

}
