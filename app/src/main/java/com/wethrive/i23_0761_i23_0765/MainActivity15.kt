package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity15 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main15)

        val profile=findViewById<TextView>(R.id.profile)
        val cancel=findViewById<TextView>(R.id.cancel)
        val done=findViewById<TextView>(R.id.done)

        val listener= View.OnClickListener{
            finish()
        }

        cancel.setOnClickListener(listener)
        done.setOnClickListener(listener)

        profile.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                // open default gallery / photos app
                action = Intent.ACTION_PICK
                data = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            startActivity(intent)
        }
    }
}