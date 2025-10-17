package com.wethrive.i23_0761_i23_0765


data class Message(
    var messageId: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var text: String? = null,
    var imageUrl: String? = null,
    var postId: String? = null,
    var timestamp: Long = 0L,
    var edited: Boolean = false,
    var deleted: Boolean = false
)
