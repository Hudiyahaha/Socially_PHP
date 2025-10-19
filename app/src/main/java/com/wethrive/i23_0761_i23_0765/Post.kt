package com.wethrive.i23_0761_i23_0765

data class Post(
    val postId: String = "",
    val userId: String = "",
    val mediaBase64List: List<String> = emptyList(),
    val mediaTypeList: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val username: String? = null,           // Username of poster
    val caption: String? = null,            // Caption text
    val userProfileBase64: String? = null,  // Base64 profile picture
    var likes: MutableList<String> = mutableListOf()
)
