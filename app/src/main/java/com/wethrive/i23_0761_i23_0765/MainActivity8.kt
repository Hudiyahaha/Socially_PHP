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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class MainActivity8 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)

        val exit = findViewById<ImageView>(R.id.exit_dm)
        val plus = findViewById<ImageView>(R.id.plus)
        val titleUsername = findViewById<TextView>(R.id.username)

        // SharedPreferences session data
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: ""

        val BASE_URL = "http://sociallyah.atwebpages.com/"

        val pref=getSharedPreferences("user_session", MODE_PRIVATE)
         titleUsername.text=pref.getString("username","")

        // Read share extras if this screen was opened from the feed share button
        val shareOwnerId = intent.getStringExtra("sharePostOwnerId")
        val shareId = intent.getStringExtra("sharePostId")

        // RecyclerView for DM chats
        val recycler = findViewById<RecyclerView>(R.id.recyclerDM)
        val emptyView = findViewById<TextView>(R.id.empty_dm)

        val adapter = DMAdapter(mutableListOf())
        // Inject share extras into adapter so they are forwarded on row tap
        adapter.sharePostOwnerId = shareOwnerId
        adapter.sharePostId = shareId

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        recycler.setHasFixedSize(true)

        fun loadUsers() {
            if (currentUserId.isBlank()) {
                emptyView.visibility = android.view.View.VISIBLE
                return
            }
            emptyView.visibility = android.view.View.GONE
            val req = object : StringRequest(Method.POST, BASE_URL + "get_friends.php",
                { response ->
                    try {
                        val arr = JSONArray(response) // friends.php returns a JSON array directly
                        val list = mutableListOf<DMItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val uid = obj.optString("uid")
                            val uname = obj.optString("username")
                            val dp = obj.optString("dp")
                            if (uid.isBlank() || uname.isBlank()) continue
                            if (uid == currentUserId) continue // exclude self
                            list.add(
                                DMItem(
                                    id = uid,
                                    name = uname,
                                    lastMessage = "", // not available here
                                    time = "", // not available
                                    dp = if (dp.isBlank()) null else dp
                                )
                            )
                        }
                        // Provide share extras when submitting list as well (for any callers using submitList signature)
                        adapter.submitList(list, shareOwnerId, shareId)
                        emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    } catch (e: Exception) {
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show()
                        emptyView.visibility = android.view.View.VISIBLE
                    }
                },
                { _ ->
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                    emptyView.visibility = android.view.View.VISIBLE
                }
            ) {
                override fun getParams(): MutableMap<String, String> = hashMapOf("userId" to currentUserId)
            }
            Volley.newRequestQueue(this).add(req)
        }

        loadUsers()

        exit.setOnClickListener {
            finish()
        }
    }
}