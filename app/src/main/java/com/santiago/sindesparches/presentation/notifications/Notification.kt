package com.santiago.sindesparches.presentation.notifications

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "", // "like", "participate", "share", "comment"
    val planId: String = "",
    val planTitle: String = "",
    val message: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val read: Boolean = false,
    val sent: Boolean = false,
    val senderProfileImageUrl: String? = null,
    // Nuevos campos para manejo de estado
    val sentAt: Timestamp? = null,
    val error: String? = null,
    val errorAt: Timestamp? = null,
    val messageId: String? = null
) {
    // Constructor vacío requerido por Firestore
    constructor() : this(
        id = "",
        recipientId = "",
        senderId = "",
        senderName = "",
        type = "",
        planId = "",
        planTitle = "",
        message = "",
        timestamp = null,
        read = false,
        sent = false,
        senderProfileImageUrl = null,
        sentAt = null,
        error = null,
        errorAt = null,
        messageId = null
    )
}