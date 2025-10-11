package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity4 : AppCompatActivity() {
    lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_main4)

        val email = findViewById<EditText>(R.id.email)
        val pass = findViewById<EditText>(R.id.password)
        val login_button=findViewById<Button>(R.id.login_button)
        var sign_up=findViewById<TextView>(R.id.signup)

        sign_up.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
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

            mAuth.signInWithEmailAndPassword(em, pa)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, MainActivity3::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        email.error = it.exception?.localizedMessage
                    }
                }
        }

    }
}