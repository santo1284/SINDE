package com.santiago.sindesparches.presentation.notifications

import com.google.firebase.firestore.FieldValue

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "",
    val planId: String = "",
    val planTitle: String = "",
    val message: String = "",
    val timestamp: Any? = FieldValue.serverTimestamp(),
    val read: Boolean = false,
    val senderProfileImageUrl: String? = null
)
