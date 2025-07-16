package com.santiago.sindesparches.presentation.chat

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val senderProfilePictureUrl: String? = null
)

data class Chat(
    val id: String = "",
    val planId: String = "",
    val participants: List<String> = emptyList()
)
