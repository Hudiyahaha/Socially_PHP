package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity8 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)

        val exit=findViewById<ImageView>(R.id.exit_dm)
        val plus=findViewById<ImageView>(R.id.plus)

        exit.setOnClickListener {
            finish()
        }

        plus.setOnClickListener {
            val intent= Intent(this, MainActivity9::class.java)
            startActivity(intent)

        }

    }
}