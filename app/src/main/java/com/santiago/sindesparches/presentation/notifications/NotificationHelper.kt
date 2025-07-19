package com.santiago.sindesparches.presentation.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationHelper {

    companion object {
        private const val TAG = "NotificationHelper"
    }

    // Función para guardar el token FCM del usuario actual
    suspend fun saveUserFCMToken(db: FirebaseFirestore, userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()

            val userData = hashMapOf(
                "fcmToken" to token,
                "lastTokenUpdate" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(userId)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Token FCM guardado correctamente: $token")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar token FCM", e)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener token FCM", e)
        }
    }

    // Función principal para enviar notificación
    suspend fun sendNotification(
        db: FirebaseFirestore,
        recipientId: String,
        senderId: String,
        type: String,
        planId: String,
        context: Context
    ) {
        try {
            // No enviar notificación si el usuario se está notificando a sí mismo
            if (recipientId == senderId) {
                Log.d(TAG, "No se envía notificación - mismo usuario")
                return
            }

            // Obtener información del remitente
            val senderDoc = db.collection("perfil").document(senderId).get().await()
            val senderName = senderDoc.getString("nombre") ?: "Usuario desconocido"

            // Obtener información del plan
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

            // Obtener el token FCM del destinatario
            val recipientDoc = db.collection("users").document(recipientId).get().await()
            val fcmToken = recipientDoc.getString("fcmToken")

            if (fcmToken.isNullOrEmpty()) {
                Log.w(TAG, "No FCM token found for recipient: $recipientId")
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
                "read" to false,
                "fcmToken" to fcmToken
            )

            db.collection("notifications")
                .add(notificationData)
                .addOnSuccessListener {
                    Log.d(TAG, "Notificación guardada en Firestore para $recipientId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar notificación", e)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar notificación", e)
        }
    }

    // Función específica para when someone likes a plan
    suspend fun onPlanLiked(db: FirebaseFirestore, planId: String, context: Context) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "Usuario no autenticado")
                return
            }

            // Obtener el creador del plan
            val planDoc = db.collection("planes").document(planId).get().await()
            val planOwnerId = planDoc.getString("userId")

            if (planOwnerId != null) {
                sendNotification(db, planOwnerId, currentUserId, "like", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar like del plan", e)
        }
    }

    // Función específica para when someone participates in a plan
    suspend fun onPlanParticipation(db: FirebaseFirestore, planId: String, context: Context) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "Usuario no autenticado")
                return
            }

            // Obtener el creador del plan
            val planDoc = db.collection("planes").document(planId).get().await()
            val planOwnerId = planDoc.getString("userId")

            if (planOwnerId != null) {
                sendNotification(db, planOwnerId, currentUserId, "participate", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar participación del plan", e)
        }
    }

    // Función específica para when someone comments on a plan
    suspend fun onPlanCommented(db: FirebaseFirestore, planId: String, context: Context) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "Usuario no autenticado")
                return
            }

            // Obtener el creador del plan
            val planDoc = db.collection("planes").document(planId).get().await()
            val planOwnerId = planDoc.getString("userId")

            if (planOwnerId != null) {
                sendNotification(db, planOwnerId, currentUserId, "comment", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar comentario del plan", e)
        }
    }

    // Función específica para when someone shares a plan
    suspend fun onPlanShared(db: FirebaseFirestore, planId: String, context: Context) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "Usuario no autenticado")
                return
            }

            // Obtener el creador del plan
            val planDoc = db.collection("planes").document(planId).get().await()
            val planOwnerId = planDoc.getString("userId")

            if (planOwnerId != null) {
                sendNotification(db, planOwnerId, currentUserId, "share", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar share del plan", e)
        }
    }
}