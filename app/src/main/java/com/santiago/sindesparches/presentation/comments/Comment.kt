package com.santiago.sindesparches.presentation.comments

import com.google.firebase.firestore.FieldValue

data class Comment(
    val id: String = "",
    val planId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String? = null,
    val text: String = "",
    val timestamp: Any? = FieldValue.serverTimestamp()
)
