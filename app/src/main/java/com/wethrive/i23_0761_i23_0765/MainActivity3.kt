package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        var btn=findViewById<MaterialButton>(R.id.button)

        btn.setOnClickListener{
            var intent= Intent(this, MainActivity4::class.java)
            startActivity(intent)
            finish()

        }

    }
}