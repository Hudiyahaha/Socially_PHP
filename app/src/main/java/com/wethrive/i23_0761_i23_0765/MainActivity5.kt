package com.wethrive.i23_0761_i23_0765

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException


class MainActivity5 : AppCompatActivity() {

    private var photoUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openCamera()
        }

    private val mediaPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                for (uri in uris) {
                    val mimeType = contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video") == true
                    uploadStoryToFirebase(uri, if (isVideo) "video" else "image")
                }
            } else {
                Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            }
        }


    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                photoUri?.let { uri ->
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                    uploadStoryToFirebase(uri, "image")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)

        val search = findViewById<ImageView>(R.id.search_bar)
        val dm = findViewById<ImageView>(R.id.message_icon)
        val notis = findViewById<ImageView>(R.id.heart)
        val profile_bottom = findViewById<CircleImageView>(R.id.profile2)
        val create = findViewById<ImageView>(R.id.create)
        val camera = findViewById<ImageView>(R.id.camera_icon)
        val your_story = findViewById<LinearLayout>(R.id.your_story)
        val addstory = findViewById<ImageView>(R.id.addStoryIcon)
        val profile = findViewById<CircleImageView>(R.id.profile)

        // Load profile picture
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
                            profile_bottom.setImageBitmap(bitmap)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Error: ${it.message}")
                }
        }

        // Upload image/video story
        addstory.setOnClickListener {
            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
        }

        // Open last story
        your_story.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val intent = Intent(this, ViewStory::class.java)
                intent.putExtra("userId", user.uid)
                startActivity(intent)
            }
        }

        // Navigation buttons
        create.setOnClickListener { startActivity(Intent(this, MainActivity16::class.java)) }
        profile_bottom.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
        search.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
        dm.setOnClickListener { startActivity(Intent(this, MainActivity8::class.java)) }
        notis.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        camera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun uploadStoryToFirebase(uri: Uri, mediaType: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()

            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Unable to read file", Toast.LENGTH_SHORT).show()
                return
            }

            if (bytes.size > 3_000_000) {
                Toast.makeText(this, "File too large! Keep under ~3MB.", Toast.LENGTH_SHORT).show()
                return
            }

            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
            val user = FirebaseAuth.getInstance().currentUser ?: return

            val ref = FirebaseDatabase.getInstance()
                .getReference("stories")
                .child(user.uid)
                .push()

            val story = Story(
                id = ref.key,
                userId = user.uid,
                mediaBase64 = base64String,
                mediaType = mediaType,
                timestamp = System.currentTimeMillis()
            )

            ref.setValue(story).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Story uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to upload story", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error uploading story", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "IMG_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        photoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (photoUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(intent)
    }
}
