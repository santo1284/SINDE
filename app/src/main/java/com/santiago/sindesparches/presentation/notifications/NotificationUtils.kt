package com.santiago.sindesparches.presentation.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

private const val TAG = "NotificationFunctions"

// Función directa que puedes llamar desde cualquier parte
suspend fun sendNotification(
    db: FirebaseFirestore,
    recipientId: String,
    senderId: String,
    type: String,
    planId: String,
    context: Context
) {
    try {
        // ✅ NUEVA: Verificar autenticación antes de cualquier operación
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado - no se puede enviar notificación")
            return
        }

        Log.d(TAG, "Usuario autenticado: ${currentUser.uid}")

        // No enviar notificación si el usuario se está notificando a sí mismo
        if (recipientId == senderId) {
            Log.d(TAG, "No se envía notificación - mismo usuario")
            return
        }

        // ✅ MEJORADO: Obtener información del remitente con manejo de errores
        val senderDoc = try {
            db.collection("perfil").document(senderId).get().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener perfil del remitente: $senderId", e)
            return
        }
        val senderName = senderDoc.getString("nombre") ?: "Usuario desconocido"

        // ✅ MEJORADO: Obtener información del plan con manejo de errores
        val planDoc = try {
            db.collection("planes").document(planId).get().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener plan: $planId", e)
            return
        }
        val planTitle = planDoc.getString("title") ?: "Plan"

        // Crear el mensaje de notificación
        val message = when (type) {
            "like" -> "$senderName le gustó tu plan: $planTitle"
            "participate" -> "$senderName va a participar en tu plan: $planTitle"
            "share" -> "$senderName compartió tu plan: $planTitle"
            "comment" -> "$senderName comentó en tu plan: $planTitle"
            else -> "$senderName interactuó con tu plan: $planTitle"
        }

        // ✅ MEJORADO: Obtener el token FCM del destinatario con manejo de errores
        val recipientDoc = try {
            db.collection("users").document(recipientId).get().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario destinatario: $recipientId", e)
            // Continuamos sin el token FCM
            null
        }
        val fcmToken = recipientDoc?.getString("fcmToken")

        if (fcmToken.isNullOrEmpty()) {
            Log.w(TAG, "No FCM token found for recipient: $recipientId")
            // Aún así guardamos la notificación en Firestore para que aparezca en la app
        }

        // ✅ MEJORADO: Crear la notificación en Firestore con manejo de errores
        val notificationData = hashMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName,
            "type" to type,
            "planId" to planId,
            "planTitle" to planTitle,
            "message" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false,
            "fcmToken" to fcmToken
        )

        try {
            db.collection("notifications")
                .add(notificationData)
                .await()
            Log.d(TAG, "Notificación guardada en Firestore para $recipientId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar notificación", e)
        }

    } catch (e: Exception) {
        Log.e(TAG, "Error al procesar notificación", e)
    }
}

// ✅ MEJORADO: Función para guardar el token FCM del usuario actual
suspend fun saveUserFCMToken(db: FirebaseFirestore, userId: String) {
    try {
        // Verificar autenticación
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado - no se puede guardar token FCM")
            return
        }

        val token = FirebaseMessaging.getInstance().token.await()

        val userData = hashMapOf(
            "fcmToken" to token,
            "lastTokenUpdate" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .await()

        Log.d(TAG, "Token FCM guardado correctamente: $token")

    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener/guardar token FCM", e)
    }
}

// ✅ MEJORADO: Función para actualizar el token FCM cuando se renueve
fun updateFCMToken(token: String, db: FirebaseFirestore, userId: String) {
    // Verificar autenticación
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Log.e(TAG, "Usuario no autenticado - no se puede actualizar token FCM")
        return
    }

    val userData = hashMapOf(
        "fcmToken" to token,
        "lastTokenUpdate" to FieldValue.serverTimestamp()
    )

    db.collection("users").document(userId)
        .set(userData, com.google.firebase.firestore.SetOptions.merge())
        .addOnSuccessListener {
            Log.d(TAG, "Token FCM actualizado: $token")
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error al actualizar token FCM", e)
        }
}