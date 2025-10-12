package com.wethrive.i23_0761_i23_0765

data class Story(
    val id: String? = null,
    val userId: String? = null,
    val mediaBase64: String? = null,
    val mediaType: String? = null, // "image" or "video"
    val timestamp: Long = System.currentTimeMillis()
)
