package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class MainActivity12 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main12)

        val home=findViewById<ImageView>(R.id.home)
        val search=findViewById<ImageView>(R.id.search)
        val profile=findViewById<CircleImageView>(R.id.profile)
        val create = findViewById<ImageView>(R.id.create)
        val follow=findViewById<RelativeLayout>(R.id.follow)
        val requests=findViewById<TextView>(R.id.follow_count)
        val following=findViewById<TextView>(R.id.following)

        val BASE_URL = "http://sociallyah.atwebpages.com/"

        val pref=getSharedPreferences("user_session", MODE_PRIVATE)
        val current = pref.getString("userId", "") ?: ""

        fun loadPendingRequests() {
            val url = BASE_URL + "request.php"

            val req = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val json = JSONObject(response)

                        if (json.getInt("status") == 1) {

                            val count = json.getInt("count")
                            requests.text = "$count"

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                {
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "following_id" to current
                    )
                }
            }

            Volley.newRequestQueue(this).add(req)
        }

        loadPendingRequests()

        follow.setOnClickListener {
            val intent= Intent(this, Requests::class.java)
            startActivity(intent)
            finish()
        }

        following.setOnClickListener {
            val intent= Intent(this, MainActivity11::class.java)
            startActivity(intent)
            finish()
        }

        profile.setOnClickListener {
            val intent= Intent(this, MainActivity13::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        home.setOnClickListener{
            val intent= Intent(this, MainActivity5::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        search.setOnClickListener{
            val intent= Intent(this, MainActivity6::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        create.setOnClickListener {
            val intent = Intent(this, MainActivity16::class.java)
            startActivity(intent)
        }
    }
}