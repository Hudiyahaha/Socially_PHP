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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView


class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        var btn=findViewById<MaterialButton>(R.id.button)
        var switch_akont=findViewById<TextView>(R.id.switch_account)
        var profile=findViewById<CircleImageView>(R.id.profile)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
            databaseRef.child("dp").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val imageString = snapshot.getValue(String::class.java)

                        if (imageString != null) {
                            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profile.setImageBitmap(bitmap)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Error: ${it.message}")
                }
        }

        btn.setOnClickListener{
            var intent= Intent(this, MainActivity5::class.java)
            startActivity(intent)
            finish()
        }

        switch_akont.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity4::class.java)
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}