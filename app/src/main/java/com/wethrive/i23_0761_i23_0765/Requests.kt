package com.wethrive.i23_0761_i23_0765

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class Requests : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: RequestsAdapter
    private lateinit var emptyView: TextView
    private var current: String = ""

    // In-flight tracking
    private val pendingUsers: MutableList<String> = mutableListOf()
    private val loadedUsers: MutableList<UserData> = mutableListOf()

    private val BASE_URL = "http://sociallyah.atwebpages.com/"

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

        // Retrieve current userId from shared preferences (set during login elsewhere)
        current = getSharedPreferences("user_session", MODE_PRIVATE).getString("userId", "") ?: ""

        if (current.isBlank()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadFollowRequests()
    }

    private fun loadFollowRequests() {
        emptyView.visibility = View.GONE
        adapter.submitList(emptyList())
        pendingUsers.clear()
        loadedUsers.clear()

        val url = BASE_URL + "request.php"

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optInt("status") == 1) {

                        val arr: JSONArray = json.optJSONArray("requests") ?: JSONArray()

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val followerId = obj.optString("follower_id")
                            if (followerId.isNotBlank()) {
                                pendingUsers.add(followerId)
                            }
                        }

                        if (pendingUsers.isEmpty()) {
                            emptyView.visibility = View.VISIBLE
                        } else {
                            // Fetch details for each requester
                            for (id in pendingUsers) {
                                fetchUserDetails(id)
                            }
                        }
                    } else {
                        emptyView.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    emptyView.visibility = View.VISIBLE
                    Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                emptyView.visibility = View.VISIBLE
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "following_id" to current
            )
        }

        Volley.newRequestQueue(this).add(req)
    }

    private fun fetchUserDetails(userId: String) {
        val url = BASE_URL + "get_dp.php"

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optInt("status") == 1) {
                        val username = json.optString("username", "Unknown")
                        val bio = json.optString("bio", "Hey there! I am using Socially.")
                        val dp = json.optString("dp", "")

                        loadedUsers.add(
                            UserData(
                                id = userId,
                                uname = username,
                                email = "", // not provided
                                dp = dp,
                                bio = bio
                            )
                        )
                    }
                } catch (_: Exception) {}
                checkAllLoaded()
            },
            { _ ->
                checkAllLoaded()
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "userId" to userId
            )
        }
        Volley.newRequestQueue(this).add(req)
    }

    private fun checkAllLoaded() {
        if (loadedUsers.size == pendingUsers.size) {
            adapter.submitList(loadedUsers.toList())
            emptyView.visibility = if (loadedUsers.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun acceptRequest(user: UserData, position: Int) {
        val followerId = user.id ?: return
        val url = BASE_URL + "accept_follow_request.php"

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optInt("status") == 1) {

                        adapter.removeItem(position)
                        Toast.makeText(this, "Request accepted", Toast.LENGTH_SHORT).show()
                        if (adapter.itemCount == 0) emptyView.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, json.optString("message", "Failed"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                }
            },
            { _ ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "follower_id" to followerId,
                "following_id" to current
            )
        }
        Volley.newRequestQueue(this).add(req)
    }
    private fun rejectRequest(user: UserData, position: Int) {
        val followerId = user.id ?: return
        val url = BASE_URL + "reject_follow_request.php"

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optInt("status") == 1) {
                        adapter.removeItem(position)
                        Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
                        if (adapter.itemCount == 0) emptyView.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, json.optString("message", "Failed"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                }
            },
            { _ ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "follower_id" to followerId,
                "following_id" to current
            )
        }
        Volley.newRequestQueue(this).add(req)
    }

}
