package com.santiago.sindesparches.presentation.chat

import com.google.firebase.Timestamp

enum class MessageStatus {
    SENDING, SENT, FAILED
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val senderProfilePictureUrl: String? = null,
    val status: MessageStatus = MessageStatus.SENT
)

data class Conversation(
    val id: String = "",
    val planId: String = "",
    val participants: List<String> = emptyList(), // Siempre 2 participantes: creador y interesado
    val lastMessage: ChatMessage? = null
)
