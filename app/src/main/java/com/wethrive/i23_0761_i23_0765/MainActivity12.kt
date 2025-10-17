package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity12 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main12)

        val home=findViewById<ImageView>(R.id.home)
        val search=findViewById<ImageView>(R.id.search)
        val profile=findViewById<CircleImageView>(R.id.profile)
        val create = findViewById<ImageView>(R.id.create)
        val follow=findViewById<RelativeLayout>(R.id.follow)
        val requests=findViewById<TextView>(R.id.follow_count)
        val following=findViewById<TextView>(R.id.following)

        val user=FirebaseAuth.getInstance().currentUser!!.uid
        val dbref=FirebaseDatabase.getInstance().getReference("Requests").child(user)

        dbref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                requests.text = count.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
                requests.text="-1"
            }
        })

        follow.setOnClickListener {
            val intent= Intent(this, Requests::class.java)
            startActivity(intent)
            finish()
        }

        following.setOnClickListener {
            val intent= Intent(this, MainActivity11::class.java)
            startActivity(intent)
            finish()
        }

        profile.setOnClickListener {
            val intent= Intent(this, MainActivity13::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }


        home.setOnClickListener{
            val intent= Intent(this, MainActivity5::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        search.setOnClickListener{
            val intent= Intent(this, MainActivity6::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        create.setOnClickListener {
            val intent = Intent(this, MainActivity16::class.java)
            startActivity(intent)
        }






    }
}