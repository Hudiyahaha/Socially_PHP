package com.wethrive.i23_0761_i23_0765

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.widget.EditText
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import android.util.Log
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

class MainActivity9 : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnAttach: ImageView
    // Header views
    private lateinit var profileName: TextView
    private lateinit var profileIcon: CircleImageView
    private lateinit var backArrow: ImageView

    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: ChatAdapter

    private val messageList = mutableListOf<Message>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var otherUserId: String
    private lateinit var chatId: String

    private val IMAGE_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        otherUserId = intent.getStringExtra("receiverId")!!
        chatId = if (currentUserId < otherUserId)
            currentUserId + "_" + otherUserId
        else
            otherUserId + "_" + currentUserId

        // Explicitly target the default Realtime Database for this Firebase project
        val dbUrl = "https://i-0761-23i-0765-default-rtdb.firebaseio.com"
        dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Messages").child(chatId)

        // Diagnostics: log which backend instances we are talking to
        try {
            Log.d("MainActivity9", "Realtime DB url: $dbUrl, root: ${dbRef.root}")
        } catch (_: Exception) { }

        recyclerChat = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        // Bind header views
        profileName = findViewById(R.id.profile_name)
        profileIcon = findViewById(R.id.profile_icon)
        backArrow = findViewById(R.id.back_arrow)

        // Optional: Use provided name immediately while we fetch fresh data
        intent.getStringExtra("chatName")?.let { if (it.isNotBlank()) profileName.text = it }

        // Back navigation
        backArrow.setOnClickListener { finish() }

        adapter = ChatAdapter(messageList, currentUserId)
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = adapter

        loadMessages()
        loadReceiverProfile()

        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { pickImage() }
    }

    private fun loadReceiverProfile() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uname = snapshot.child("uname").getValue(String::class.java)
                val dpBase64 = snapshot.child("dp").getValue(String::class.java)

                // Name fallback: keep existing text or use Unknown
                if (!uname.isNullOrBlank()) {
                    profileName.text = uname
                } else if (profileName.text.isNullOrBlank()) {
                    profileName.setText("Unknown")
                }

                // Decode base64 dp and set to CircleImageView
                if (!dpBase64.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            profileIcon.setImageBitmap(bmp)
                            profileIcon.contentDescription = profileName.text
                        }
                    } catch (_: IllegalArgumentException) {
                        // Ignore invalid base64, keep default avatar
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // No-op; keep defaults
            }
        })
    }

    private fun loadMessages() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (msgSnap in snapshot.children) {
                    val message = msgSnap.getValue(Message::class.java)
                    if (message != null) messageList.add(message)
                }
                Log.d("MainActivity9", "Loaded ${messageList.size} messages for chatId=$chatId")
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    recyclerChat.scrollToPosition(messageList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity9", "loadMessages cancelled: ${error.toException()}")
            }
        })
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val messageId = dbRef.push().key!!
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        dbRef.child(messageId).setValue(message)
        etMessage.text.clear()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != IMAGE_REQUEST_CODE) return

        if (resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                Log.w("MainActivity9", "Image picker returned OK but data URI was null")
                return
            }
            // Encode and send as Base64 via Realtime Database (no Firebase Storage)
            sendImageBase64(imageUri)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Image selection failed ($resultCode)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendImageBase64(uri: Uri) {
        // Decode + downscale + compress
        val base64 = try {
            val b64 = compressImageToBase64(uri)
            if (b64.isNullOrBlank()) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_LONG).show()
                Log.e("MainActivity9", "compressImageToBase64 returned null/blank for $uri")
                return
            }
            b64
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to process image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "compressImageToBase64 exception", e)
            return
        }

        val messageId = dbRef.push().key!!
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            imageBase64 = base64,
            timestamp = System.currentTimeMillis()
        )
        dbRef.child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                Toast.makeText(this, "Image sent", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity9", "Image message saved (base64, length=${base64.length})")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save message: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity9", "DB write failed (base64)", e)
            }
    }

    private fun compressImageToBase64(uri: Uri, maxDim: Int = 1024, quality: Int = 80): String? {
        return try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val src = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    if (w > 0 && h > 0) {
                        val ratio = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
                        val targetW = (w * ratio).toInt().coerceAtLeast(1)
                        val targetH = (h * ratio).toInt().coerceAtLeast(1)
                        if (w > maxDim || h > maxDim) decoder.setTargetSize(targetW, targetH)
                    }
                }
            } else {
                // Fallback for older devices: bounds decode + inSampleSize + optional post-scale
                val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, optsBounds)
                } ?: return null

                val (w, h) = optsBounds.outWidth to optsBounds.outHeight
                if (w <= 0 || h <= 0) return null

                var inSample = 1
                val halfW = w / 2
                val halfH = h / 2
                while ((halfW / inSample) >= maxDim || (halfH / inSample) >= maxDim) {
                    inSample *= 2
                }

                val opts = BitmapFactory.Options().apply { inSampleSize = max(1, inSample) }
                val decoded = contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, opts)
                } ?: return null

                val scaled = scaleBitmap(decoded, maxDim)
                if (scaled != decoded) decoded.recycle()
                scaled
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val bytes = baos.toByteArray()
            baos.close()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (!bitmap.isRecycled) bitmap.recycle()
            b64
        } catch (e: Exception) {
            Log.e("MainActivity9", "compressImageToBase64 failed", e)
            null
        }
    }

    private fun scaleBitmap(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
        val newW = (w * ratio).toInt()
        val newH = (h * ratio).toInt()
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
