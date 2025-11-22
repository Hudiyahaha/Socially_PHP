package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.activity.result.contract.ActivityResultContracts
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class MainActivity2 : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main2)

        val name=findViewById<EditText>(R.id.username)
        val email=findViewById<EditText>(R.id.email)
        val pass=findViewById<EditText>(R.id.pass)
        val signup=findViewById<Button>(R.id.signup)
        val login=findViewById<TextView>(R.id.login)
        val profile=findViewById<CircleImageView>(R.id.profile_image)
        var img=""
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Launcher to pick an image from gallery
        val pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                profile.setImageURI(it)

                contentResolver.openInputStream(it)?.use { ins ->
                    val b = ins.readBytes()
                    img= Base64.encodeToString(b, Base64.DEFAULT)
                }
            }
        }

        // Open gallery on profile click
        profile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        signup.setOnClickListener {
            val em = email.text.toString()
            val pa = pass.text.toString()
            val uname = name.text.toString()

            if (em.isEmpty()) {
                email.error = "Email required"
                return@setOnClickListener
            }
            if (pa.isEmpty()) {
                pass.error = "Password required"
                return@setOnClickListener
            }
            if (img.isEmpty()) {
                Toast.makeText(this, "Select Profile Image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = object : StringRequest(
                Request.Method.POST,
                "http://sociallyah.atwebpages.com/signup.php",
                { response ->

                    try {
                        val json = JSONObject(response)

                        if (json.getInt("status") == 1) {
                            val newUserId = json.getString("uid")

                            // SAVE SESSION
                            val editor = prefs.edit()
                            editor.putString("userId", newUserId)
                            editor.putBoolean("isLoggedIn", true)
                            editor.apply()

                            // Signup successful → move to MainActivity3
                            val intent = Intent(this, MainActivity3::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            // Signup failed
                            Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid server response", Toast.LENGTH_LONG).show()
                    }

                },
                { error ->
                    Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
                }
            ) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["username"] = uname
                    params["email"] = em
                    params["password"] = pa
                    params["image"] = img
                    return params
                }
            }

            val queue = Volley.newRequestQueue(this)
            queue.add(request)
        }


        login.setOnClickListener {
            val intent= Intent(this,MainActivity4::class.java)
            startActivity(intent)
            finish()
        }

    }

 override fun onStart() {
       super.onStart()

    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        selectedImageUri?.let { outState.putString("selectedImageUri", it.toString()) }
//    }
}