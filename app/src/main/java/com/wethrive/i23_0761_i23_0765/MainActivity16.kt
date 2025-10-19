package com.wethrive.i23_0761_i23_0765

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.FileInputStream

class MainActivity16 : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private val selectedUris = mutableListOf<Uri>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main16)

        imageView = findViewById(R.id.selectedImageView)
        videoView = findViewById(R.id.selectedVideoView)

        val next=findViewById<TextView>(R.id.next)
        next.setOnClickListener {
            val selectedUri = if (imageView.visibility == View.VISIBLE) {
            imageView.tag as? Uri
        } else {
            videoView.tag as? Uri
        }

            if (selectedUris.isNotEmpty()) {
                uploadPostToFirebase()
            } else {
                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Check permissions based on Android version
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ uses READ_MEDIA_* permissions
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ),
                    100
                )
            } else {
                loadGallery()
            }
        } else {
            // Android 12 and below use READ_EXTERNAL_STORAGE
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    100
                )
            } else {
                loadGallery()
            }
        }
    }

    //  Function to load all gallery media (images + videos)
    private fun loadGallery() {
        val recyclerView = findViewById<RecyclerView>(R.id.galleryRecycler)
        val uris = mutableListOf<Uri>()

        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "(" +
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                " OR " +
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                ")"

        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor = contentResolver.query(
            queryUri,
            projection,
            selection,
            null,
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(queryUri, id.toString())
                uris.add(contentUri)
            }
        }

        if (uris.isEmpty()) {
            println("No media found")
            return
        }

        // Default: show first image/video
        showMedia(uris[0])

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = GalleryAdapter(uris, this) { uri, isSelected ->
            if (isSelected) {
                selectedUris.add(uri)
            } else {
                selectedUris.remove(uri)
            }
            updatePreviewUI()
        }


        findViewById<TextView>(R.id.cancel).setOnClickListener { finish() }
    }

    private fun updatePreviewUI() {
        val next = findViewById<TextView>(R.id.next)
        if (selectedUris.isEmpty()) {
            next.text = "Next"
            imageView.visibility = View.GONE
            videoView.visibility = View.GONE
        } else {
            next.text = "Next (${selectedUris.size})"
            showMedia(selectedUris.last()) // show the latest selected preview
        }
    }
    private fun showMedia(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        if (mimeType?.startsWith("video") == true) {
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            videoView.setVideoURI(uri)
            videoView.tag = uri
            videoView.start()
        } else {
            videoView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setImageURI(uri)
            imageView.tag = uri
        }
    }

    private fun uploadPostToFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaBase64List = mutableListOf<String>()
        val mediaTypeList = mutableListOf<String>()

        for (uri in selectedUris) {
            try {
                val pfd = contentResolver.openFileDescriptor(uri, "r")
                val fileDescriptor = pfd?.fileDescriptor
                if (fileDescriptor != null) {
                    val inputStream = FileInputStream(fileDescriptor)
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    pfd.close()

                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                    mediaBase64List.add(base64String)

                    val mimeType = contentResolver.getType(uri)
                    if (mimeType != null && mimeType.startsWith("video")) {
                        mediaTypeList.add("video")
                    } else {
                        mediaTypeList.add("image")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (mediaBase64List.isEmpty()) {
            Toast.makeText(this, "Failed to encode media", Toast.LENGTH_SHORT).show()
            return
        }

        val postId = FirebaseDatabase.getInstance()
            .getReference("Posts").child(uid).push().key ?: System.currentTimeMillis().toString()

        val post = Post(
            postId = postId,
            userId = uid,
            mediaBase64List = mediaBase64List,
            mediaTypeList = mediaTypeList,
            timestamp = System.currentTimeMillis(),
            likes = mutableListOf()
        )
        val postRef = FirebaseDatabase.getInstance()
            .getReference("Posts")
            .child(uid)
            .child(postId)

// Upload the post just once
        postRef.setValue(post)
            .addOnSuccessListener {
                // Detect if it’s an image or video post
                val finalMediaType = if (post.mediaTypeList.contains("video")) "video" else "image"
                postRef.child("mediaType").setValue(finalMediaType)

                Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity13::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadGallery()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
