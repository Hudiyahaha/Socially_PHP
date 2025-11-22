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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class friends : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FriendsAdapter
    private lateinit var emptyView: TextView
    private lateinit var searchBar: EditText
    private lateinit var btnFollowers: Button
    private lateinit var btnFollowing: Button

    private val allUsers: MutableList<UserData> = mutableListOf()
    private var currentUsername: String? = null

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

        // Retrieve current username from shared preferences (ensure earlier login code saved it)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUsername = prefs.getString("username", "")

        fetchFriends()

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(query: CharSequence?, start: Int, before: Int, count: Int) {
                val q = query?.toString()?.trim()?.lowercase() ?: ""
                val list = if (q.isEmpty()) allUsers else allUsers.filter { it.uname.lowercase().contains(q) }
                adapter.submitList(list)
                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        btnFollowers.visibility = View.GONE
        btnFollowing.visibility = View.GONE
    }

    private fun fetchFriends() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""
        val url = "http://sociallyah.atwebpages.com/get_friends.php"
        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(Method.POST, url,
            { response -> parseFriendsResponse(response) },
            { error ->
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_LONG).show()
                emptyView.visibility = View.VISIBLE
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("userId" to userId)
        }
        queue.add(request)
    }

    private fun parseFriendsResponse(response: String) {
        try {
            val jsonArray = JSONArray(response)
            allUsers.clear()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val username = obj.optString("username")
                if (username.isNullOrBlank()) continue

                if (!currentUsername.isNullOrBlank() && username == currentUsername) continue // skip self

                val id=obj.optString("uid")
                val dp = obj.optString("dp")
                // We don't have email/bio from API; use defaults
                val userData = UserData(id = id, uname = username, email = "", dp = dp, bio = "Hey there! I am using Socially.")
                allUsers.add(userData)
            }
            adapter.submitList(allUsers)
            emptyView.visibility = if (allUsers.isEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show()
            emptyView.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() { super.onDestroy() }
}
