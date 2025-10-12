package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ViewStory : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_story)

        val storyImage = findViewById<ImageView>(R.id.storyImageView)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance()
            .getReference("stories")
            .child(currentUser.uid)
        ref.limitToLast(1).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, "No story found!", Toast.LENGTH_LONG).show()
            }
            for (child in snapshot.children) {
                val map = child.value as? Map<*, *>
                val base64String = map?.get("imageBase64") as? String
                val userIdStored = map?.get("userId") as? String
                Toast.makeText(this, "Found story by user: $userIdStored", Toast.LENGTH_LONG).show()

                if (base64String != null) {
                    val bytes =
                        android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                    val bitmap =
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    findViewById<ImageView>(R.id.storyImageView).setImageBitmap(bitmap)
                }
            }
        }
    }
}