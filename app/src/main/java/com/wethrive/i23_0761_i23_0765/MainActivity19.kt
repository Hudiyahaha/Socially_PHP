package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity19 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main19)

        val button = findViewById<View>(R.id.button)

        button.setOnClickListener {
            val intent= Intent(this, MainActivity20::class.java)
            startActivity(intent)
            finish()
        }

    }
}