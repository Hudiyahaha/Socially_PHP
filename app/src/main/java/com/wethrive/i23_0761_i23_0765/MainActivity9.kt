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
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import android.view.SurfaceView
import android.widget.FrameLayout
import java.io.ByteArrayOutputStream
import kotlin.math.max
// Agora imports
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import java.io.File
import android.text.InputType

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

    private lateinit var audio: ImageView
    private lateinit var video: ImageView
    private val messageList = mutableListOf<Message>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var otherUserId: String
    private lateinit var chatId: String

    private val IMAGE_REQUEST_CODE = 101

    // Call status UI
    private lateinit var callStatusContainer: LinearLayout
    private lateinit var callStatusText: TextView
    private lateinit var callTimerText: TextView
    private lateinit var callLevelBar: View

    // Video call UI containers
    private var videoCallContainer: FrameLayout? = null
    private var localVideoContainer: FrameLayout? = null
    private var remoteVideoContainer: FrameLayout? = null
    private var endCallButton: ImageView? = null
    private var switchCameraButton: ImageView? = null
    private var muteButton: ImageView? = null

    // Timer
    private var callStartTimeMs: Long = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if ((isInVoiceCall || isInVideoCall) && callStartTimeMs > 0L) {
                val elapsed = System.currentTimeMillis() - callStartTimeMs
                callTimerText.text = formatElapsed(elapsed)
                timerHandler.postDelayed(this, 1000L)
            }
        }
    }

    // Agora call state/config
    private var rtcEngine: RtcEngine? = null
    private var isInVoiceCall: Boolean = false
    private var isInVideoCall: Boolean = false
    private var isMuted: Boolean = false
    private val AUDIO_PERMISSION_REQ_CODE = 201
    private val VIDEO_PERMISSION_REQ_CODE = 202
    private val agoraAppId: String = "941f2bca958848af98ccea5d2bda5ab5"
    private val agoraToken: String? = "007eJxTYJguOWWbVajq8kS9OsfdxhnShwy9xUM1P7ZNL9uudzT43R4FBksTwzSjpORES1MLCxOLxDRLi+Tk1ETTFKOklETTxCTT7POsmQ2BjAztp6xYGRkgEMS3ZEiqqAx2K3GJMAsKMvFP90mJNCjI8CpOMih2KzSMT09NzzXJ9cwozkwLMMkJ9S3yy3T3Ci2uMncyN2ZgAAAufjB0"

    // Event handler for Agora callbacks
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d("MainActivity9", "Agora joined channel=$channel uid=$uid")
            runOnUiThread {
                callStatusText.text = getString(R.string.call_connected)
                startCallTimer()
                val callType = if (isInVideoCall) "Video" else "Voice"
                Toast.makeText(this@MainActivity9, "$callType call started", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("MainActivity9", "Remote user joined: $uid")
            runOnUiThread {
                if (isInVideoCall) {
                    setupRemoteVideo(uid)
                }
            }
        }
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("MainActivity9", "Remote user offline: $uid reason=$reason")
            runOnUiThread {
                if (isInVoiceCall || isInVideoCall) {
                    leaveChannel()
                    Toast.makeText(this@MainActivity9, "Remote user left. Call ended.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onError(err: Int) {
            Log.e("MainActivity9", "Agora onError: $err")
            runOnUiThread {
                Toast.makeText(this@MainActivity9, "Agora error: $err", Toast.LENGTH_LONG).show()
                if (!isInVoiceCall && !isInVideoCall) {
                    hideCallStatusUI()
                }
            }
        }
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d("MainActivity9", "Connection state=$state reason=$reason")
            runOnUiThread {
                when (state) {
                    Constants.CONNECTION_STATE_CONNECTING -> callStatusText.text = getString(R.string.call_connecting)
                    Constants.CONNECTION_STATE_CONNECTED -> callStatusText.text = getString(R.string.call_connected)
                    Constants.CONNECTION_STATE_RECONNECTING -> callStatusText.text = getString(R.string.call_connecting)
                    Constants.CONNECTION_STATE_FAILED, Constants.CONNECTION_STATE_DISCONNECTED -> {
                        if (!isInVoiceCall && !isInVideoCall) hideCallStatusUI()
                    }
                }
            }
        }
        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            val level = (totalVolume.coerceIn(0, 255)) / 255f
            runOnUiThread {
                callLevelBar.alpha = 0.2f + 0.8f * level
                callLevelBar.scaleX = 0.5f + 1.5f * level
            }
        }
    }

    // Call invite signaling (Firebase)
    private lateinit var callsRef: DatabaseReference
    private var callInviteListener: ValueEventListener? = null
    private var incomingDialog: AlertDialog? = null
    private var suppressInviteOnJoin: Boolean = false
    private var pendingCallType: String = "audio" // "audio" or "video"

    // Share extras handling for post sharing
    private var sharePostOwnerId: String? = null
    private var sharePostId: String? = null
    private var sharedPostSentOnce: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        otherUserId = intent.getStringExtra("receiverId")!!
        chatId = if (currentUserId < otherUserId)
            currentUserId + "_" + otherUserId
        else
            otherUserId + "_" + currentUserId

        // Read share extras if present
        sharePostOwnerId = intent.getStringExtra("sharePostOwnerId")
        sharePostId = intent.getStringExtra("sharePostId")

        // Explicitly target the default Realtime Database for this Firebase project
        val dbUrl = "https://i-0761-23i-0765-default-rtdb.firebaseio.com"
        dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Messages").child(chatId)
        callsRef = FirebaseDatabase.getInstance(dbUrl).getReference("Calls").child(chatId)

        try {
            Log.d("MainActivity9", "Realtime DB url: $dbUrl, root: ${dbRef.root}")
        } catch (_: Exception) { }

        recyclerChat = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)

        profileName = findViewById(R.id.profile_name)
        profileIcon = findViewById(R.id.profile_icon)
        backArrow = findViewById(R.id.back_arrow)

        audio = findViewById(R.id.audio)
        video = findViewById(R.id.video)

        // Call status UI
        callStatusContainer = findViewById(R.id.call_status_container)
        callStatusText = findViewById(R.id.call_status_text)
        callTimerText = findViewById(R.id.call_timer_text)
        callLevelBar = findViewById(R.id.call_level_bar)

        intent.getStringExtra("chatName")?.let { if (it.isNotBlank()) profileName.text = it }

        backArrow.setOnClickListener { finish() }

        adapter = ChatAdapter(messageList, currentUserId) { message ->
            handleMessageLongPress(message)
        }
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = adapter

        loadMessages()
        loadReceiverProfile()

        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { pickImage() }

        // If a post was shared from the feed, send it once upon entering this chat
        maybeSendSharedPost()

        // Start/end voice call when audio icon is tapped
        audio.setOnClickListener {
            if (!isInVoiceCall && !isInVideoCall) {
                pendingCallType = "audio"
                if (!hasAudioPermissions()) {
                    requestAudioPermissions()
                } else {
                    startVoiceCalling()
                }
            } else {
                leaveChannel()
                Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
            }
        }

        // Start/end video call when video icon is tapped
        video.setOnClickListener {
            if (!isInVoiceCall && !isInVideoCall) {
                pendingCallType = "video"
                if (!hasVideoPermissions()) {
                    requestVideoPermissions()
                } else {
                    startVideoCalling()
                }
            } else {
                leaveChannel()
                Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
            }
        }

        // Start listening for incoming call invites
        attachCallInviteListener()
    }

    // ============ Call signaling ============
    private fun attachCallInviteListener() {
        if (callInviteListener != null) return
        callInviteListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    incomingDialog?.dismiss()
                    incomingDialog = null
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java)
                val callerId = snapshot.child("callerId").getValue(String::class.java)
                val calleeId = snapshot.child("calleeId").getValue(String::class.java)
                val callType = snapshot.child("callType").getValue(String::class.java) ?: "audio"

                if (status == "ringing" && calleeId == currentUserId && callerId != currentUserId && !isInVoiceCall && !isInVideoCall) {
                    if (incomingDialog?.isShowing == true) return
                    // Safely handle nullable callerId and callType
                    showIncomingCallDialog(callerId ?: "Unknown", callType)
                } else if (status == "ended") {
                    incomingDialog?.dismiss()
                    incomingDialog = null
                    if (isInVoiceCall || isInVideoCall) {
                        leaveChannel()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity9", "Calls listener cancelled: ${error.toException()}")
            }
        }
        callsRef.addValueEventListener(callInviteListener!!)
    }

    private fun showIncomingCallDialog(callerId: String, callType: String) {
        val callTypeText = if (callType == "video") "video" else "audio"
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.incoming_call_title)
            .setMessage("Incoming $callTypeText call from $callerId...")
            .setCancelable(false)
            .setPositiveButton(R.string.accept) { dialog, _ ->
                suppressInviteOnJoin = true
                pendingCallType = callType
                callsRef.child("status").setValue("ongoing")
                if (callType == "video") {
                    if (hasVideoPermissions()) {
                        startVideoCalling()
                    } else {
                        requestVideoPermissions()
                    }
                } else {
                    if (hasAudioPermissions()) {
                        startVoiceCalling()
                    } else {
                        requestAudioPermissions()
                    }
                }
                dialog.dismiss()
                incomingDialog = null
            }
            .setNegativeButton(R.string.decline) { dialog, _ ->
                callsRef.child("status").setValue("ended")
                dialog.dismiss()
                incomingDialog = null
            }
        incomingDialog = builder.create()
        incomingDialog?.show()
    }

    private fun sendCallInvite(callType: String) {
        val invite = mapOf(
            "channel" to chatId,
            "callerId" to currentUserId,
            "calleeId" to otherUserId,
            "callType" to callType,
            "status" to "ringing",
            "timestamp" to System.currentTimeMillis()
        )
        callsRef.setValue(invite)
            .addOnFailureListener { e -> Log.e("MainActivity9", "Failed to write call invite", e) }
    }

    // ========================= Message loading/sending =========================
    private fun loadReceiverProfile() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uname = snapshot.child("uname").getValue(String::class.java)
                val dpBase64 = snapshot.child("dp").getValue(String::class.java)

                if (!uname.isNullOrBlank()) {
                    profileName.text = uname
                } else if (profileName.text.isNullOrBlank()) {
                    profileName.setText(R.string.unknown_user)
                }

                if (!dpBase64.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            profileIcon.setImageBitmap(bmp)
                            profileIcon.contentDescription = profileName.text
                        }
                    } catch (_: IllegalArgumentException) {
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
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

            sendImageBase64(imageUri)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Image selection failed ($resultCode)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendImageBase64(uri: Uri) {
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

    // ========================= Agora integration and UI =========================
    private fun getAudioPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun getVideoPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        }
    }

    private fun hasAudioPermissions(): Boolean {
        return getAudioPermissions().all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasVideoPermissions(): Boolean {
        return getVideoPermissions().all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAudioPermissions() {
        ActivityCompat.requestPermissions(this, getAudioPermissions(), AUDIO_PERMISSION_REQ_CODE)
    }

    private fun requestVideoPermissions() {
        ActivityCompat.requestPermissions(this, getVideoPermissions(), VIDEO_PERMISSION_REQ_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startVoiceCalling()
            } else {
                Toast.makeText(this, "Microphone permission is required for audio calls", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == VIDEO_PERMISSION_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startVideoCalling()
            } else {
                Toast.makeText(this, "Camera and microphone permissions are required for video calls", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startVoiceCalling() {
        if (isInVoiceCall || isInVideoCall) {
            Toast.makeText(this, "Already in call", Toast.LENGTH_SHORT).show()
            return
        }
        if (agoraAppId.isBlank() || agoraAppId.startsWith("<")) {
            Toast.makeText(this, "Set your Agora App ID in MainActivity9", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "Agora App ID is not set")
            return
        }
        if (!suppressInviteOnJoin) {
            sendCallInvite("audio")
        }
        showCallStatusUI(connecting = true)
        initializeAgoraVoiceSDK()
        joinVoiceChannel()
        suppressInviteOnJoin = false
    }

    private fun startVideoCalling() {
        if (isInVoiceCall || isInVideoCall) {
            Toast.makeText(this, "Already in call", Toast.LENGTH_SHORT).show()
            return
        }
        if (agoraAppId.isBlank() || agoraAppId.startsWith("<")) {
            Toast.makeText(this, "Set your Agora App ID in MainActivity9", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "Agora App ID is not set")
            return
        }
        if (!suppressInviteOnJoin) {
            sendCallInvite("video")
        }
        showVideoCallUI()
        initializeAgoraVideoSDK()
        enableVideo()
        setupLocalVideo()
        joinVideoChannel()
        suppressInviteOnJoin = false
    }

    private fun initializeAgoraVoiceSDK() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = agoraAppId
                mEventHandler = rtcEventHandler
                // Write logs to app files dir for troubleshooting
                mLogConfig = RtcEngineConfig.LogConfig().apply {
                    filePath = File(filesDir, "agora.log").absolutePath
                }
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            try { rtcEngine?.enableAudioVolumeIndication(200, 3, true) } catch (_: Throwable) {}
        } catch (e: Exception) {
            Log.e("MainActivity9", "Error initializing Agora RTC engine", e)
            Toast.makeText(this, "Failed to init voice engine: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            hideCallStatusUI()
        }
    }

    private fun initializeAgoraVideoSDK() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = agoraAppId
                mEventHandler = rtcEventHandler
                // Write logs to app files dir for troubleshooting
                mLogConfig = RtcEngineConfig.LogConfig().apply {
                    filePath = File(filesDir, "agora_video.log").absolutePath
                }
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        } catch (e: Exception) {
            Log.e("MainActivity9", "Error initializing Agora video engine", e)
            Toast.makeText(this, "Failed to init video engine: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            hideVideoCallUI()
        }
    }

    private fun enableVideo() {
        rtcEngine?.apply {
            enableVideo()
            startPreview()
        }
    }

    private fun setupLocalVideo() {
        val container = localVideoContainer ?: run {
            Log.w("MainActivity9", "Local video container is null, cannot setup local video")
            return
        }

        container.removeAllViews()

        val surfaceView = SurfaceView(baseContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Local video should be on top of the container
            setZOrderMediaOverlay(true)
        }
        container.addView(surfaceView)
        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        Log.d("MainActivity9", "Local video setup complete")
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = remoteVideoContainer ?: run {
            Log.w("MainActivity9", "Remote video container is null")
            return
        }
        runOnUiThread {
            container.removeAllViews()

            val surfaceView = SurfaceView(applicationContext).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                // Remote video stays in background
            }
            container.addView(surfaceView)
            rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            Log.d("MainActivity9", "Remote video setup complete for uid=$uid")

            // Refresh local video to ensure it stays visible
            setupLocalVideo()
        }
    }

    private fun joinVoiceChannel() {
        val engine = rtcEngine ?: return
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
            publishCameraTrack = false
            autoSubscribeAudio = true
            autoSubscribeVideo = false
        }
        val rc = engine.joinChannel(agoraToken, chatId, 0, options)
        Log.d("MainActivity9", "joinChannel rc=$rc token=${agoraToken != null} appIdSet=${agoraAppId.isNotBlank()} channel=$chatId")
        if (rc == 0) {
            isInVoiceCall = true
            callStatusText.text = getString(R.string.call_connecting)
        } else {
            Toast.makeText(this, "Failed to join channel: $rc", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "joinChannel failed rc=$rc")
            hideCallStatusUI()
        }
    }

    private fun joinVideoChannel() {
        val engine = rtcEngine ?: return
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
            publishCameraTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        val rc = engine.joinChannel(agoraToken, chatId, 0, options)
        Log.d("MainActivity9", "joinVideoChannel rc=$rc channel=$chatId")
        if (rc == 0) {
            isInVideoCall = true
            callStatusText.text = getString(R.string.call_connecting)
        } else {
            Toast.makeText(this, "Failed to join video channel: $rc", Toast.LENGTH_LONG).show()
            Log.e("MainActivity9", "joinVideoChannel failed rc=$rc")
            hideVideoCallUI()
        }
    }

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()

        if (isInVideoCall) {
            rtcEngine?.stopPreview()
            localVideoContainer?.removeAllViews()
            remoteVideoContainer?.removeAllViews()
            hideVideoCallUI()
        }

        isInVoiceCall = false
        isInVideoCall = false
        isMuted = false
        stopCallTimer()
        hideCallStatusUI()
        callsRef.child("status").setValue("ended")
    }

    private fun leaveVoiceChannel() {
        leaveChannel()
    }

    private fun cleanupAgoraEngine() {
        try {
            leaveChannel()
            RtcEngine.destroy()
        } catch (_: Exception) { }
        rtcEngine = null
    }

    override fun onDestroy() {
        super.onDestroy()
        callInviteListener?.let { callsRef.removeEventListener(it) }
        callInviteListener = null
        incomingDialog?.dismiss()
        incomingDialog = null
        cleanupAgoraEngine()
    }

    // ========================= Call UI helpers =========================
    private fun showCallStatusUI(connecting: Boolean) {
        callStatusContainer.visibility = View.VISIBLE
        callStatusText.text = if (connecting) getString(R.string.call_connecting) else getString(R.string.call_connected)
        callTimerText.text = getString(R.string.call_timer_default)
        callLevelBar.alpha = 0.2f
        callLevelBar.scaleX = 0.5f
    }

    private fun hideCallStatusUI() {
        callStatusContainer.visibility = View.GONE
    }

    private fun showVideoCallUI() {
        // Create video call overlay if it doesn't exist
        if (videoCallContainer == null) {
            videoCallContainer = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xFF1A1A1A.toInt())
                id = View.generateViewId()
            }

            // Remote video container (full screen)
            remoteVideoContainer = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                id = View.generateViewId()
            }
            videoCallContainer?.addView(remoteVideoContainer)

            // Local video container (rounded preview in top-right)
            localVideoContainer = FrameLayout(this).apply {
                val width = (140 * resources.displayMetrics.density).toInt()
                val height = (200 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(width, height).apply {
                    topMargin = (60 * resources.displayMetrics.density).toInt()
                    marginEnd = (20 * resources.displayMetrics.density).toInt()
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                }
                setBackgroundResource(R.drawable.bg_local_video)
                elevation = 8f * resources.displayMetrics.density
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 12f * resources.displayMetrics.density)
                    }
                }
                id = View.generateViewId()
            }
            videoCallContainer?.addView(localVideoContainer)

            // Control buttons container at bottom
            val controlsContainer = LinearLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (50 * resources.displayMetrics.density).toInt()
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                }
                orientation = LinearLayout.HORIZONTAL
                setPadding(
                    (24 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (24 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                )
                setBackgroundResource(R.drawable.bg_local_video)
                elevation = 4f * resources.displayMetrics.density
            }

            // Mute button (left)
            muteButton = ImageView(this).apply {
                val buttonSize = (56 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                    marginEnd = (20 * resources.displayMetrics.density).toInt()
                }
                setBackgroundResource(R.drawable.bg_call_button)
                setImageResource(R.drawable.ic_mic)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt()
                )
                elevation = 2f * resources.displayMetrics.density
                setOnClickListener {
                    isMuted = !isMuted
                    rtcEngine?.muteLocalAudioStream(isMuted)
                    if (isMuted) {
                        setImageResource(R.drawable.ic_mic_off)
                        setColorFilter(0xFFFF5252.toInt())
                    } else {
                        setImageResource(R.drawable.ic_mic)
                        clearColorFilter()
                    }
                }
            }
            controlsContainer.addView(muteButton)

            // End call button (center, larger and red)
            endCallButton = ImageView(this).apply {
                val buttonSize = (70 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                    marginStart = (8 * resources.displayMetrics.density).toInt()
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                setBackgroundResource(R.drawable.bg_end_call_button)
                setImageResource(R.drawable.ic_call_end)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(
                    (18 * resources.displayMetrics.density).toInt(),
                    (18 * resources.displayMetrics.density).toInt(),
                    (18 * resources.displayMetrics.density).toInt(),
                    (18 * resources.displayMetrics.density).toInt()
                )
                elevation = 4f * resources.displayMetrics.density
                rotation = 135f
                setOnClickListener {
                    leaveChannel()
                    Toast.makeText(this@MainActivity9, "Call ended", Toast.LENGTH_SHORT).show()
                }
            }
            controlsContainer.addView(endCallButton)

            // Switch camera button (right)
            switchCameraButton = ImageView(this).apply {
                val buttonSize = (56 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                    marginStart = (20 * resources.displayMetrics.density).toInt()
                }
                setBackgroundResource(R.drawable.bg_call_button)
                setImageResource(R.drawable.ic_flip_camera)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt()
                )
                elevation = 2f * resources.displayMetrics.density
                setOnClickListener {
                    rtcEngine?.switchCamera()
                    // Add a subtle animation
                    animate().rotationBy(180f).setDuration(300).start()
                }
            }
            controlsContainer.addView(switchCameraButton)

            videoCallContainer?.addView(controlsContainer)

            // Add to root view
            val rootView = window.decorView.findViewById<FrameLayout>(android.R.id.content)
            rootView.addView(videoCallContainer)
        }

        videoCallContainer?.visibility = View.VISIBLE
        showCallStatusUI(connecting = true)
    }

    private fun hideVideoCallUI() {
        videoCallContainer?.visibility = View.GONE
    }

    private fun startCallTimer() {
        callStartTimeMs = System.currentTimeMillis()
        timerHandler.removeCallbacksAndMessages(null)
        timerHandler.post(timerRunnable)
    }

    private fun stopCallTimer() {
        callStartTimeMs = 0L
        timerHandler.removeCallbacksAndMessages(null)
        callTimerText.text = getString(R.string.call_timer_default)
    }

    private fun formatElapsed(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
    }

    // Share extras handling for post sharing
    private fun maybeSendSharedPost() {
        if (sharedPostSentOnce) return
        val owner = sharePostOwnerId
        val pid = sharePostId
        if (!owner.isNullOrBlank() && !pid.isNullOrBlank()) {
            val messageId = dbRef.push().key!!
            val message = Message(
                messageId = messageId,
                senderId = currentUserId,
                receiverId = otherUserId,
                text = "Shared a post",
                postId = "${owner}:${pid}",
                timestamp = System.currentTimeMillis()
            )
            dbRef.child(messageId)
                .setValue(message)
                .addOnSuccessListener {
                    sharedPostSentOnce = true
                    Toast.makeText(this, "Post shared", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity9", "Failed to share post: ${e.localizedMessage}")
                }
        }
    }

    private fun handleMessageLongPress(message: Message) {
        // Only allow edit/delete for messages sent by current user within 5 minutes and not already deleted
        val isOwn = message.senderId == currentUserId
        val withinWindow = System.currentTimeMillis() - (message.timestamp) <= 5 * 60 * 1000
        if (!isOwn || !withinWindow || message.deleted == true) return

        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Message options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> promptEditMessage(message)
                    1 -> confirmDeleteMessage(message)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun promptEditMessage(message: Message) {
        // Only support editing plain text messages (not images or shared posts)
        if (!message.imageBase64.isNullOrBlank() || !message.imageUrl.isNullOrBlank() || !message.postId.isNullOrBlank()) {
            Toast.makeText(this, "Only text messages can be edited", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(message.text ?: "")
            setSelection(text?.length ?: 0)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit message")
            .setView(input)
            .setPositiveButton("Save") { d, _ ->
                val newText = input.text.toString().trim()
                if (newText.isEmpty()) {
                    Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val id = message.messageId ?: return@setPositiveButton
                    val updates = mapOf(
                        "text" to newText,
                        "edited" to true
                    )
                    dbRef.child(id).updateChildren(updates)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun confirmDeleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Delete message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { d, _ ->
                val id = message.messageId ?: return@setPositiveButton
                // Soft delete: mark as deleted and clear content fields
                val updates = hashMapOf<String, Any>(
                    "deleted" to true
                )
                message.text?.let { if (it.isNotBlank()) updates["text"] = "" }
                message.imageBase64?.let { if (it.isNotBlank()) updates["imageBase64"] = "" }
                message.imageUrl?.let { if (it.isNotBlank()) updates["imageUrl"] = "" }
                message.postId?.let { if (it.isNotBlank()) updates["postId"] = "" }
                dbRef.child(id).updateChildren(updates)
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }
}
