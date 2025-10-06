package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity9 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main9)

        val back=findViewById<ImageView>(R.id.back_arrow)
        val video=findViewById<ImageView>(R.id.video_call)

        back.setOnClickListener {
            finish()
        }

        video.setOnClickListener {
            val intent= Intent(this, MainActivity10::class.java)
            startActivity(intent)
        }
    }
}