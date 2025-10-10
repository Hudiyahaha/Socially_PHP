package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity2 : AppCompatActivity() {
    lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth= FirebaseAuth.getInstance()
        setContentView(R.layout.activity_main2)
        var name=findViewById<EditText>(R.id.username)
        var email=findViewById<EditText>(R.id.email)
        var pass=findViewById<EditText>(R.id.pass)
        var signup=findViewById<Button>(R.id.signup)
        signup.setOnClickListener {
            var em=email.text.toString()
            var pa=pass.text.toString()
            var uname=name.text.toString()
            if (em.isEmpty() || pa.isEmpty()) {
                email.error = "Email required"
                pass.error = "Password required"
                return@setOnClickListener
            }



            mAuth.createUserWithEmailAndPassword(em,pa)
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        val user = UserData(uid, uname, em,pa)
                        var intent= Intent(this,MainActivity3::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else{
                        var msg=it.exception?.localizedMessage
                        email.error=msg.toString()
                    }
                }

        }



    }
//    override fun onStart() {
//        super.onStart()
//        if (mAuth.currentUser!=null)
//        {
//            var i=Intent(this, MainActivity5::class.java)
//            startActivity(i)
//            finish()
//        }
//    }
}