package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView


class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        var btn=findViewById<MaterialButton>(R.id.button)
        var switch_akont=findViewById<TextView>(R.id.switch_account)
        var profile=findViewById<CircleImageView>(R.id.profile)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val image = prefs.getString("image", "")
        val username = prefs.getString("username", "")
        val email = prefs.getString("email", "")

        if (!image.isNullOrEmpty()) {
            val bytes = Base64.decode(image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            profile.setImageBitmap(bitmap)
        }



        btn.setOnClickListener{
            var intent= Intent(this, MainActivity5::class.java)
            startActivity(intent)
            finish()
        }

        switch_akont.setOnClickListener {

            val intent = Intent(this, MainActivity4::class.java)
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}