package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.activity.result.contract.ActivityResultContracts
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity2 : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageUri: Uri? = null

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
        val login=findViewById<TextView>(R.id.login)
        val profile=findViewById<CircleImageView>(R.id.profile_image)
        var img=""

        // Restore previously selected image if any
//        selectedImageUri = savedInstanceState?.getString("selectedImageUri")?.toUri()
//        selectedImageUri?.let { profile.setImageURI(it) }

        // Launcher to pick an image from gallery
        val pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                profile.setImageURI(it)

                contentResolver.openInputStream(it)?.use { ins ->
                    val b = ins.readBytes()
                    img= Base64.encodeToString(b, Base64.DEFAULT)
                }
            }
        }

        // Open gallery on profile click
        profile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        signup.setOnClickListener {
            val em=email.text.toString()
            val pa=pass.text.toString()
            val uname=name.text.toString()
            if (em.isEmpty()) {
                email.error = "Email required"
            }
            if (pa.isEmpty()) {
                pass.error = "Password required"
            }
            if (img.isEmpty()) {
                Toast.makeText(this, "Select Profile Image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(em,pa)
                .addOnCompleteListener {
                    if(it.isSuccessful){

                        //Jo bhi user create hua hai uski Id dedo hamey
                        val uid = mAuth.currentUser?.uid

                        // yeh uid hamny apny userdata k object user mai daaldi
                        val user = UserData(uid, uname, em, img, "Hey there! I'm using Socially.")

                        // ab yeh user object ko database mai daldo
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

        login.setOnClickListener {
            val intent= Intent(this,MainActivity4::class.java)
            startActivity(intent)
            finish()
        }

    }

 override fun onStart() {
       super.onStart()
    if (mAuth.currentUser!=null) // user is logged in
        {
            val i=Intent(this, MainActivity5::class.java)
           startActivity(i)
            finish()
        }
    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        selectedImageUri?.let { outState.putString("selectedImageUri", it.toString()) }
//    }
}