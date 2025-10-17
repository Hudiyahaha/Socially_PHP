package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity6 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main6)

        val home=findViewById<ImageView>(R.id.home)
        val search=findViewById<ImageView>(R.id.submit)
        val profile=findViewById<CircleImageView>(R.id.profile)
        val create = findViewById<ImageView>(R.id.create)
        val notis = findViewById<ImageView>(R.id.heart)
        val friendsButton=findViewById<MaterialButton>(R.id.friends)


        friendsButton.setOnClickListener {
            val intent= Intent(this, friends::class.java)
            startActivity(intent)
        }

        notis.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        create.setOnClickListener {
            val intent = Intent(this, MainActivity16::class.java)
            startActivity(intent)
        }

        profile.setOnClickListener {
            val intent= Intent(this, MainActivity13::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        home.setOnClickListener {
            val intent= Intent(this, MainActivity5::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        search.setOnClickListener {
            val intent= Intent(this, MainActivity7::class.java)
            startActivity(intent)
        }

    }
}