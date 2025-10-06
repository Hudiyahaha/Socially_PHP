package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.text.clear

class MainActivity7 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main7)

        val clear= findViewById<TextView>(R.id.clear)
        val kyan= findViewById<LinearLayout>(R.id.kyan)

        kyan.setOnClickListener {
            val intent= Intent(this, MainActivity22::class.java)
            startActivity(intent)

        }

        clear.setOnClickListener {
            finish()
        }
    }
}