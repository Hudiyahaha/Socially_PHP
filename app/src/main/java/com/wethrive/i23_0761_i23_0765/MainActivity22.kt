package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity22 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main22)

        val follow=findViewById<MaterialButton>(R.id.follow)

        follow.setOnClickListener {
            val intent=Intent(this, MainActivity21::class.java)
            startActivity(intent)
            finish()
        }


    }
}