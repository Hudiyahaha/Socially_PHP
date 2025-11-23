package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity13 : AppCompatActivity() {
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)

        val highlight=findViewById<LinearLayout>(R.id.highlight)
        val edit_profile=findViewById<Button>(R.id.edit_profile)

        val uname=findViewById<TextView>(R.id.uname)
        val home=findViewById<ImageView>(R.id.home)
        val create = findViewById<ImageView>(R.id.create)
        val notis = findViewById<ImageView>(R.id.heart)
        val search = findViewById<ImageView>(R.id.search)
        val logoutButton = findViewById<TextView>(R.id.logout)
        val profile=findViewById<CircleImageView>(R.id.profile)
        val profile_bottom=findViewById<CircleImageView>(R.id.profile_bottom)
        val user=findViewById<TextView>(R.id.user)
        val bio= findViewById<TextView>(R.id.bio)
        val posts= findViewById<TextView>(R.id.posts)
        val followers= findViewById<TextView>(R.id.followers)
        val following= findViewById<TextView>(R.id.following)
        val followers_screen= findViewById<LinearLayout>(R.id.follower_screen)
        val following_screen= findViewById<LinearLayout>(R.id.following_screen)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        uid = prefs.getString("userId", "")?.takeIf { it.isNotEmpty() }
            ?: run {
                Toast.makeText(this, "UserId is empty", Toast.LENGTH_LONG).show()
                return
            }
        val savedDp = prefs.getString("dp", "")
        user.text = prefs.getString("username", "User")
        uname.text = prefs.getString("username", "User")
        bio.text = prefs.getString("bio", "")
        posts.text=prefs.getInt("post_count", 0).toString()

        if (!savedDp.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(savedDp, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profile.setImageBitmap(bitmap)
                profile_bottom.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                // If decoding fails, fallback to default image
                profile.setImageResource(R.drawable.me)
                profile_bottom.setImageResource(R.drawable.me)
            }
        } else {
            profile.setImageResource(R.drawable.me)
            profile_bottom.setImageResource(R.drawable.me)
        }




        val recyclerView = findViewById<RecyclerView>(R.id.postRecycler)
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)


        if (uid.isNotEmpty()) {
            loadUserPosts(recyclerView)
        }



        logoutButton.setOnClickListener {
            val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
            prefs.edit().clear().apply()   // FULL CLEAR

            val intent = Intent(this, MainActivity4::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        search.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        notis.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        create.setOnClickListener {
            val intent = Intent(this, MainActivity16::class.java)
            startActivity(intent)
        }

        home.setOnClickListener {
            val intent= Intent(this, MainActivity5::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        highlight.setOnClickListener {
            val intent= Intent(this, MainActivity14::class.java)
            startActivity(intent)
        }

        edit_profile.setOnClickListener {
            val intent= Intent(this, MainActivity15::class.java)
            startActivity(intent)
        }

        // Open followers / following screens
        followers_screen.setOnClickListener {
            val intent = Intent(this, FollowersActivity::class.java)
            startActivity(intent)
        }

        following_screen.setOnClickListener {
            val intent = Intent(this, FollowingActivity::class.java)
            startActivity(intent)
        }

    }
    private fun loadUserPosts(recyclerView: RecyclerView) {
        val url = "http://sociallyah.atwebpages.com/get_post.php"
        val queue = Volley.newRequestQueue(this)

        val postList = mutableListOf<String>()     // Base64 strings
        val postIds = mutableListOf<String>()      // post IDs
        val mediaTypes = mutableListOf<String>()   // "image" or "video"

        val adapter = PostAdapter(postList, postIds, mediaTypes) { postId ->
            val intent = Intent(this, ViewPost::class.java)
            intent.putExtra("uid", uid)
            intent.putExtra("postId", postId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                Log.e("API_RAW", "Response: $response")
                try {
                    val jsonObj = org.json.JSONObject(response)

                    // Update post count
                    val postCount = jsonObj.getInt("post_count")
                    findViewById<TextView>(R.id.posts).text = postCount.toString()

                    val array = jsonObj.getJSONArray("posts")
                    postList.clear()
                    postIds.clear()
                    mediaTypes.clear()

                    for (i in 0 until array.length()) {
                        val post = array.getJSONObject(i)
                        val mediaBase64 = post.getString("media")
                        val type = post.getString("media_type")
                        val id = post.getString("post_id")

                        postList.add(mediaBase64)
                        postIds.add(id)
                        mediaTypes.add(type)
                    }

                    adapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    Log.e("API_PARSE", "Error parsing posts", e)
                }
            },
            { error ->
                Log.e("API", "Error loading posts: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("userId" to uid)
            }
        }

        queue.add(request)
    }


}

