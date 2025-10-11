package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity2 : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth= FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        setContentView(R.layout.activity_main2)
        val name=findViewById<EditText>(R.id.username)
        val email=findViewById<EditText>(R.id.email)
        val pass=findViewById<EditText>(R.id.pass)
        val signup=findViewById<Button>(R.id.signup)
        signup.setOnClickListener {
            val em=email.text.toString()
            val pa=pass.text.toString()
            val uname=name.text.toString()
            if (em.isEmpty() || pa.isEmpty()) {
                email.error = "Email required"
                pass.error = "Password required"
                return@setOnClickListener
            }



            mAuth.createUserWithEmailAndPassword(em,pa)
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        val uid = mAuth.currentUser?.uid
                        val user = UserData(uid, uname, em,pa)
                        if (uid != null) {
                            database.getReference("Users").child(uid).setValue(user)
                        }
                        val intent= Intent(this,MainActivity3::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else{
                        val msg=it.exception?.localizedMessage
                        email.error=msg.toString()
                    }
                }

        }



   }
 override fun onStart() {
       super.onStart()
    if (mAuth.currentUser!=null)
        {
            val i=Intent(this, MainActivity5::class.java)
           startActivity(i)
            finish()
        }
    }
}