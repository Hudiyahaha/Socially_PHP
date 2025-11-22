package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity4 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main4)

        val email = findViewById<EditText>(R.id.email)
        val pass = findViewById<EditText>(R.id.password)
        val login_button = findViewById<Button>(R.id.login_button)
        val sign_up = findViewById<TextView>(R.id.signup)

        sign_up.setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
        }

        login_button.setOnClickListener {
            val em = email.text.toString()
            val pa = pass.text.toString()

            if (em.isEmpty() || pa.isEmpty()) {
                email.error = "Email required"
                pass.error = "Password required"
                return@setOnClickListener
            }

            val url = "http://sociallyah.atwebpages.com/login.php"

            val request = object : StringRequest(
                Method.POST, url,
                { response ->

                    Toast.makeText(this, response, Toast.LENGTH_LONG).show()

                    val json = JSONObject(response)
                    Log.e("LOGIN", "UID RECEIVED = " + json.getString("uid"))

                    if (json.getInt("status") == 1) {

                        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        prefs.edit().apply {
                            putBoolean("isLoggedIn", true)
                            putBoolean("isFirstTime", false)

                            putString("userId", json.getString("uid"))
                            putString("username", json.getString("username"))
                            putString("email", json.getString("email"))
                            putString("dp", json.getString("dp"))
                            putString("bio", json.getString("bio"))
                            putInt("online", json.getInt("online"))

                            apply()
                        }

                        val intent = Intent(this, MainActivity3::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }

                },
                { error ->
                    Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
                }
            ) {

                // 🔹 Send POST parameters
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["email"] = em
                    params["password"] = pa
                    return params
                }

            }


            Volley.newRequestQueue(this).add(request)
        }
    }
}
