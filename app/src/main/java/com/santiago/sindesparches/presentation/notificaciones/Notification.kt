package com.santiago.sindesparches.presentation.notificaciones

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "",
    val planId: String = "",
    val planTitle: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val read: Boolean = false,
    val senderProfilePictureUrl: String? = null
)
