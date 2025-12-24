package com.wethrive.i23_0761_i23_0765

data class Message(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val text: String? = null,
    val imageBase64: String? = null,
    val imageUrl: String? = null,
    val postId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var edited: Boolean = false,
    var deleted: Boolean = false,
    var isPending: Boolean = false,   // true = queued / sending
    var vanishMode: Boolean = false,
    var seen: Boolean = false,
    var deliveryState: Int = 0 // 0 = queued/sending, 1 = sent, 2 = failed
)
