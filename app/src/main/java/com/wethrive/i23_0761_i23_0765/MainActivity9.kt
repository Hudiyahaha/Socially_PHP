package com.wethrive.i23_0761_i23_0765

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

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
    private lateinit var storage: FirebaseStorage
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

        dbRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId)
        storage = FirebaseStorage.getInstance()

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
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    recyclerChat.scrollToPosition(messageList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
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
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data ?: return
            uploadImage(imageUri)
        }
    }

    private fun uploadImage(uri: Uri) {
        val messageId = dbRef.push().key!!
        val ref = storage.reference.child("chat_images/$chatId/$messageId.jpg")

        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                val message = Message(
                    messageId = messageId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    imageUrl = downloadUrl.toString(),
                    timestamp = System.currentTimeMillis()
                )
                dbRef.child(messageId).setValue(message)
            }
        }
    }
}
