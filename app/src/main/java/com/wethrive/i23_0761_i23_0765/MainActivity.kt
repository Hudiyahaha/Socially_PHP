package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Handler(Looper.getMainLooper()).postDelayed({
            decideNextScreen()
        }, 2000)
    }

    private fun decideNextScreen() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("isLoggedIn", false)
        val firstTime = prefs.getBoolean("isFirstTime", true)

        when {
            !loggedIn -> startActivity(Intent(this, MainActivity4::class.java))
            firstTime -> startActivity(Intent(this, MainActivity2::class.java))
            else -> startActivity(Intent(this, MainActivity5::class.java))
        }
        finish()
    }
}
