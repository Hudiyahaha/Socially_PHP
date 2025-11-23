package com.wethrive.i23_0761_i23_0765

import com.google.firebase.database.Exclude

data class Post(
    val postId: String = "",
    val userId: String = "",
    val mediaUrlList: List<String> = emptyList(),
    val mediaTypeList: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val username: String? = null,           // Username of poster
    val caption: String? = null,            // Caption text
    val userProfileBase64: String? = null,  // Base64 profile picture
    var likes: MutableList<String> = mutableListOf(),
    @get:Exclude
    @set:Exclude
    var comments: MutableList<Comment> = mutableListOf()
)
