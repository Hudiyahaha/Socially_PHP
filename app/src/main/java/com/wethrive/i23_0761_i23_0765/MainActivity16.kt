package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity16 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main16)

        val cancel = findViewById<TextView>(R.id.cancel)
        val library = findViewById<TextView>(R.id.Library)
        var photoButton = findViewById<TextView>(R.id.photo)


        cancel.setOnClickListener {
            finish()
        }

        // open Gallery
        library.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                // open default gallery / photos app
                action = Intent.ACTION_PICK
                data = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            startActivity(intent)
        }

        photoButton.setOnClickListener {
            var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivity(intent)
        }

    }
}
