package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity4 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main4)

        val login_button=findViewById<Button>(R.id.login_button)

        login_button.setOnClickListener {
            android.widget.Toast.makeText(this, "Opening Activity 5", android.widget.Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity5::class.java)
            startActivity(intent)
        }

    }
}