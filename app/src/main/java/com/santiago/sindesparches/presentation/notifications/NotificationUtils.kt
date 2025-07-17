package com.santiago.sindesparches.presentation.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun sendNotification(
    db: FirebaseFirestore,
    recipientId: String,
    senderId: String,
    type: String,
    planId: String,
    context: Context
) {
    try {
        // Obtener el nombre del usuario que realiza la acción
        val senderDoc = db.collection("perfil").document(senderId).get().await()
        val senderName = senderDoc.getString("nombre") ?: "Usuario desconocido"

        // Obtener el título del plan
        val planDoc = db.collection("planes").document(planId).get().await()
        val planTitle = planDoc.getString("title") ?: "Plan"

        // Crear el mensaje de notificación
        val message = when (type) {
            "like" -> "$senderName le gustó tu plan: $planTitle"
            "participate" -> "$senderName va a participar en tu plan: $planTitle"
            "share" -> "$senderName compartió tu plan: $planTitle"
            "comment" -> "$senderName comentó en tu plan: $planTitle"
            else -> "$senderName interactuó con tu plan: $planTitle"
        }

        // Crear la notificación en Firestore
        val notificationData = hashMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName,
            "type" to type,
            "planId" to planId,
            "planTitle" to planTitle,
            "message" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("Notification", "Notificación enviada correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error al enviar notificación", e)
            }

    } catch (e: Exception) {
        Log.e("sendNotification", "Error al enviar notificación", e)
    }
}
