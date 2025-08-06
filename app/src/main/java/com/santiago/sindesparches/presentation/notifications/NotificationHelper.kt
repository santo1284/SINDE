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
        private const val NOTIFICATION_COOLDOWN_MINUTES = 5 // Tiempo mínimo entre notificaciones del mismo tipo
    }

    // ✅ NUEVA COLECCIÓN: user_actions - Para rastrear estados de acciones
    // Estructura: users/{userId}/actions/{planId}
    // Contenido: { hasLiked: boolean, hasParticipated: boolean, hasShared: boolean, lastLikeNotification: timestamp, lastParticipateNotification: timestamp }

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

    // ✅ NUEVA FUNCIÓN: Registrar estado de acción del usuario
    private suspend fun updateUserActionState(
        db: FirebaseFirestore,
        userId: String,
        planId: String,
        actionType: String,
        actionValue: Boolean
    ) {
        try {
            val actionDoc = db.collection("users")
                .document(userId)
                .collection("actions")
                .document(planId)

            val updateData = hashMapOf<String, Any>(
                actionType to actionValue,
                "lastUpdate" to FieldValue.serverTimestamp()
            )

            actionDoc.set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d(TAG, "✅ Estado de acción actualizado: $userId -> $planId: $actionType = $actionValue")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al actualizar estado de acción", e)
        }
    }

    // ✅ NUEVA FUNCIÓN: Verificar si ya se envió notificación por esta acción
    private suspend fun hasAlreadySentNotificationForAction(
        db: FirebaseFirestore,
        userId: String,
        planId: String,
        actionType: String
    ): Boolean {
        return try {
            val actionDoc = db.collection("users")
                .document(userId)
                .collection("actions")
                .document(planId)
                .get()
                .await()

            if (actionDoc.exists()) {
                val hasNotificationField = "has${actionType.capitalize()}Notification"
                actionDoc.getBoolean(hasNotificationField) == true
            } else {
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar notificación previa", e)
            false
        }
    }

    // ✅ NUEVA FUNCIÓN: Marcar que ya se envió notificación por esta acción
    private suspend fun markNotificationSentForAction(
        db: FirebaseFirestore,
        userId: String,
        planId: String,
        actionType: String
    ) {
        try {
            val actionDoc = db.collection("users")
                .document(userId)
                .collection("actions")
                .document(planId)

            val updateData = hashMapOf<String, Any>(
                "has${actionType.capitalize()}Notification" to true,
                "last${actionType.capitalize()}NotificationTime" to FieldValue.serverTimestamp()
            )

            actionDoc.set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d(TAG, "✅ Marcada notificación enviada para: $actionType")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al marcar notificación enviada", e)
        }
    }

    // ✅ FUNCIÓN PRINCIPAL MEJORADA con control de estado
    suspend fun sendNotification(
        db: FirebaseFirestore,
        recipientId: String,
        senderId: String,
        type: String,
        planId: String,
        context: Context
    ) {
        try {
            // ✅ VALIDACIÓN PRINCIPAL: No enviar notificación si el usuario se está notificando a sí mismo
            if (recipientId == senderId) {
                Log.d(TAG, "🚫 No se envía notificación - mismo usuario (recipientId: $recipientId == senderId: $senderId)")
                return
            }

            // ✅ NUEVA VALIDACIÓN: Verificar si ya se envió notificación por esta acción específica
            if (hasAlreadySentNotificationForAction(db, senderId, planId, type)) {
                Log.d(TAG, "🚫 Ya se envió notificación por esta acción: $type para plan $planId")
                return
            }

            // ✅ Crear ID único basado en la combinación de parámetros
            val notificationId = "${recipientId}_${senderId}_${type}_${planId}"

            // ✅ Verificar cooldown (como backup adicional)
            val cooldownTime = System.currentTimeMillis() - (NOTIFICATION_COOLDOWN_MINUTES * 60 * 1000)
            val cooldownTimestamp = com.google.firebase.Timestamp(cooldownTime / 1000, 0)

            val recentNotification = db.collection("notifications")
                .document(notificationId)
                .get()
                .await()

            if (recentNotification.exists()) {
                val notificationTime = recentNotification.getTimestamp("timestamp")
                if (notificationTime != null && notificationTime.compareTo(cooldownTimestamp) > 0) {
                    Log.d(TAG, "🚫 Notificación reciente encontrada - cooldown activo para: $notificationId")
                    return
                }
            }

            // ✅ Si no hay conflicto, proceder con el envío
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

            // ✅ Crear la notificación
            val notificationData = hashMapOf(
                "notificationId" to notificationId,
                "recipientId" to recipientId,
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to type,
                "planId" to planId,
                "planTitle" to planTitle,
                "message" to message,
                "timestamp" to FieldValue.serverTimestamp(),
                "read" to false,
                "fcmToken" to fcmToken,
                "lastUpdated" to FieldValue.serverTimestamp(),
                "cooldownMinutes" to NOTIFICATION_COOLDOWN_MINUTES
            )

            // ✅ Guardar notificación y marcar que se envió
            db.collection("notifications")
                .document(notificationId)
                .set(notificationData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Notificación procesada correctamente: $notificationId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error al procesar notificación: $notificationId", e)
                }

            // ✅ IMPORTANTE: Marcar que ya se envió notificación por esta acción
            markNotificationSentForAction(db, senderId, planId, type)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar notificación", e)
        }
    }

    // ✅ FUNCIÓN MEJORADA: Like con control de estado
    suspend fun onPlanLiked(db: FirebaseFirestore, planId: String, context: Context, isLiking: Boolean = true) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "Usuario no autenticado")
                return
            }

            // ✅ Actualizar estado de like del usuario
            updateUserActionState(db, currentUserId, planId, "hasLiked", isLiking)

            // ✅ Si está quitando el like, no enviar notificación
            if (!isLiking) {
                Log.d(TAG, "👍➡️👎 Usuario quitó el like - no se envía notificación")
                return
            }

            // Obtener el creador del plan
            val planDoc = db.collection("planes").document(planId).get().await()
            val planOwnerId = planDoc.getString("userId")

            if (shouldSendNotification(currentUserId, planOwnerId)) {
                Log.d(TAG, "📱 Procesando like con control de estado: $currentUserId → $planOwnerId")
                sendNotification(db, planOwnerId!!, currentUserId, "like", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar like del plan", e)
        }
    }

    // ✅ FUNCIÓN MEJORADA: Participación con control de estado
    suspend fun onPlanParticipation(db: FirebaseFirestore, planId: String, context: Context, isParticipating: Boolean = true) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "Usuario no autenticado")
                return
            }

            // ✅ Actualizar estado de participación del usuario
            updateUserActionState(db, currentUserId, planId, "hasParticipated", isParticipating)

            // ✅ Si está cancelando la participación, no enviar notificación
            if (!isParticipating) {
                Log.d(TAG, "✋➡️❌ Usuario canceló participación - no se envía notificación")
                return
            }

            // Obtener el creador del plan
            val planDoc = db.collection("planes").document(planId).get().await()
            val planOwnerId = planDoc.getString("userId")

            if (shouldSendNotification(currentUserId, planOwnerId)) {
                Log.d(TAG, "📱 Procesando participación con control de estado: $currentUserId → $planOwnerId")
                sendNotification(db, planOwnerId!!, currentUserId, "participate", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar participación del plan", e)
        }
    }

    // ✅ NUEVA FUNCIÓN: Resetear estado de notificaciones para un usuario y plan específico
    suspend fun resetNotificationStateForPlan(db: FirebaseFirestore, userId: String, planId: String) {
        try {
            val actionDoc = db.collection("users")
                .document(userId)
                .collection("actions")
                .document(planId)

            val resetData = hashMapOf<String, Any>(
                "hasLikeNotification" to false,
                "hasParticipateNotification" to false,
                "hasShareNotification" to false,
                "hasCommentNotification" to false,
                "resetTime" to FieldValue.serverTimestamp()
            )

            actionDoc.set(resetData, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d(TAG, "🔄 Estado de notificaciones reseteado para: $userId -> $planId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al resetear estado de notificaciones", e)
        }
    }

    // ✅ NUEVA FUNCIÓN: Obtener estado actual de acciones de un usuario para un plan
    suspend fun getUserActionState(db: FirebaseFirestore, userId: String, planId: String): Map<String, Any>? {
        return try {
            val actionDoc = db.collection("users")
                .document(userId)
                .collection("actions")
                .document(planId)
                .get()
                .await()

            if (actionDoc.exists()) {
                actionDoc.data
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener estado de acciones", e)
            null
        }
    }

    // ✅ Función helper para validar si debe enviarse notificación
    private fun shouldSendNotification(currentUserId: String?, planOwnerId: String?): Boolean {
        return when {
            currentUserId == null -> {
                Log.w(TAG, "⚠️ Usuario no autenticado")
                false
            }
            planOwnerId == null -> {
                Log.w(TAG, "⚠️ Plan sin propietario")
                false
            }
            currentUserId == planOwnerId -> {
                Log.d(TAG, "🚫 Mismo usuario - no se envía notificación")
                false
            }
            else -> {
                Log.d(TAG, "✅ Diferentes usuarios - se puede enviar notificación")
                true
            }
        }
    }

    // ✅ Función para comentarios (sin cambios mayores, pero con el nuevo sistema)
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

            if (shouldSendNotification(currentUserId, planOwnerId)) {
                Log.d(TAG, "📱 Procesando comentario: $currentUserId → $planOwnerId")
                sendNotification(db, planOwnerId!!, currentUserId, "comment", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar comentario del plan", e)
        }
    }

    // ✅ Función para shares (compartir es generalmente una acción única)
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

            if (shouldSendNotification(currentUserId, planOwnerId)) {
                Log.d(TAG, "📱 Procesando share: $currentUserId → $planOwnerId")
                sendNotification(db, planOwnerId!!, currentUserId, "share", planId, context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar share del plan", e)
        }
    }

    // ===== MANTENER TODAS LAS OTRAS FUNCIONES EXISTENTES =====

    // Función para obtener todas las notificaciones no leídas de un usuario
    suspend fun getUnreadNotifications(db: FirebaseFirestore, userId: String): List<Map<String, Any>>? {
        return try {
            val result = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            result.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener notificaciones no leídas", e)
            null
        }
    }

    // Función para marcar una notificación como leída
    suspend fun markNotificationAsRead(db: FirebaseFirestore, notificationId: String) {
        try {
            db.collection("notifications")
                .document(notificationId)
                .update("read", true, "readTimestamp", FieldValue.serverTimestamp())
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Notificación marcada como leída: $notificationId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error al marcar notificación como leída", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al marcar notificación como leída", e)
        }
    }

    // Función para marcar todas las notificaciones de un usuario como leídas
    suspend fun markAllNotificationsAsRead(db: FirebaseFirestore, userId: String) {
        try {
            val unreadNotifications = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            val batch = db.batch()
            val updateData = mapOf(
                "read" to true,
                "readTimestamp" to FieldValue.serverTimestamp()
            )

            for (document in unreadNotifications) {
                batch.update(document.reference, updateData)
            }

            if (unreadNotifications.size() > 0) {
                batch.commit().await()
                Log.d(TAG, "✅ ${unreadNotifications.size()} notificaciones marcadas como leídas")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al marcar todas las notificaciones como leídas", e)
        }
    }

    // ✅ Función para limpiar notificaciones antiguas (ejecutar periódicamente)
    suspend fun cleanupOldNotifications(db: FirebaseFirestore, daysOld: Int = 30) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            val cutoffTimestamp = com.google.firebase.Timestamp(cutoffTime / 1000, 0)

            val oldNotifications = db.collection("notifications")
                .whereLessThan("timestamp", cutoffTimestamp)
                .limit(500) // Procesar en lotes para evitar timeout
                .get()
                .await()

            val batch = db.batch()
            var count = 0

            for (document in oldNotifications) {
                batch.delete(document.reference)
                count++
            }

            if (count > 0) {
                batch.commit().await()
                Log.d(TAG, "🧹 Eliminadas $count notificaciones antiguas (más de $daysOld días)")
            } else {
                Log.d(TAG, "🧹 No hay notificaciones antiguas para eliminar")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al limpiar notificaciones antiguas", e)
        }
    }

    // ✅ Función para obtener estadísticas de notificaciones (útil para debugging)
    suspend fun getNotificationStats(db: FirebaseFirestore, userId: String): Map<String, Any>? {
        return try {
            val allNotifications = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .get()
                .await()

            val unreadCount = allNotifications.count { it.getBoolean("read") != true }
            val totalCount = allNotifications.size()

            val typeCount = allNotifications.groupingBy {
                it.getString("type") ?: "unknown"
            }.eachCount()

            val lastNotificationTime = allNotifications.maxByOrNull {
                it.getTimestamp("timestamp")?.seconds ?: 0
            }?.getTimestamp("timestamp")

            mapOf<String, Any>(
                "totalNotifications" to totalCount,
                "unreadNotifications" to unreadCount,
                "readNotifications" to (totalCount - unreadCount),
                "notificationsByType" to typeCount,
                "lastNotificationTime" to (lastNotificationTime ?: "No notifications")
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener estadísticas de notificaciones", e)
            null
        }
    }

    // ✅ Función para eliminar notificaciones específicas (por tipo o plan)
    suspend fun deleteNotificationsByFilter(
        db: FirebaseFirestore,
        userId: String,
        planId: String? = null,
        type: String? = null
    ) {
        try {
            var query = db.collection("notifications")
                .whereEqualTo("recipientId", userId)

            planId?.let { query = query.whereEqualTo("planId", it) }
            type?.let { query = query.whereEqualTo("type", it) }

            val notificationsToDelete = query.get().await()
            val batch = db.batch()

            for (document in notificationsToDelete) {
                batch.delete(document.reference)
            }

            if (notificationsToDelete.size() > 0) {
                batch.commit().await()
                Log.d(TAG, "🗑️ Eliminadas ${notificationsToDelete.size()} notificaciones filtradas")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al eliminar notificaciones filtradas", e)
        }
    }
}