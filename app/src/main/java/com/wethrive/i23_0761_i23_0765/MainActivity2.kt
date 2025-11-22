package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.ByteArrayOutputStream

class MainActivity2 : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var encodedImage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)

        val name = findViewById<EditText>(R.id.username)
        val email = findViewById<EditText>(R.id.email)
        val pass = findViewById<EditText>(R.id.pass)
        val signup = findViewById<Button>(R.id.signup)
        val login = findViewById<TextView>(R.id.login)
        val profile = findViewById<CircleImageView>(R.id.profile_image)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().clear().apply()

        // SAFE IMAGE PICKER
        val pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                selectedImageUri = it
                profile.setImageURI(it)
                processImageSafely(it)
            }
        }

        profile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        signup.setOnClickListener {
            val em = email.text.toString().trim()
            val pa = pass.text.toString().trim()
            val uname = name.text.toString().trim()

            if (em.isEmpty()) {
                email.error = "Email required"
                return@setOnClickListener
            }
            if (pa.isEmpty()) {
                pass.error = "Password required"
                return@setOnClickListener
            }
            if (encodedImage.isEmpty()) {
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

                            val editor = prefs.edit()
                            editor.putString("userId", newUserId)
                            editor.putBoolean("isLoggedIn", true)
                            editor.apply()

                            startActivity(Intent(this, MainActivity3::class.java))
                            finish()

                        } else {
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
                    return mapOf(
                        "username" to uname,
                        "email" to em,
                        "password" to pa,
                        "image" to encodedImage
                    )
                }
            }

            Volley.newRequestQueue(this).add(request)
        }

        login.setOnClickListener {
            startActivity(Intent(this, MainActivity4::class.java))
            finish()
        }
    }

    /**
     * SAFE IMAGE PROCESSING (Prevents phone shutdowns)
     */
    private fun processImageSafely(uri: Uri) {
        try {
            val stream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(stream)

            if (originalBitmap == null) {
                Toast.makeText(this, "Image loading failed", Toast.LENGTH_SHORT).show()
                return
            }

            // Resize image to max 600x600 to avoid huge memory load
            val resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                600,
                (600f / originalBitmap.width * originalBitmap.height).toInt(),
                true
            )

            val output = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)

            encodedImage = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT)

        } catch (e: Exception) {
            Toast.makeText(this, "Image processing error", Toast.LENGTH_SHORT).show()
        }
    }
}
