package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        val btn = findViewById<MaterialButton?>(R.id.button)
        val switchAccount = findViewById<TextView?>(R.id.switch_account)
        val profile = findViewById<CircleImageView?>(R.id.profile)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""

        if (!userId.isNullOrEmpty()) {
            val request = object : StringRequest(Method.POST, "http://sociallyah.atwebpages.com/getdp.php",
                { response ->
                    Toast.makeText(this, "RAW: " + response, Toast.LENGTH_LONG).show()
                    Log.e("DP_FETCH", "RAW RESPONSE: [$response]")


                    if (response.isNotEmpty()) {
                        val bytes = Base64.decode(response.trim(), Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        profile.setImageBitmap(bitmap)

                    }
                },
                { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf("userId" to userId)
                }
            }

            Volley.newRequestQueue(this).add(request)
        } else {
            Toast.makeText(this, "UserId is empty", Toast.LENGTH_LONG).show()
        }
        btn?.setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
            finish()
        }

        switchAccount?.setOnClickListener {
            startActivity(Intent(this, MainActivity4::class.java))
            finish()
        }
    }
}
