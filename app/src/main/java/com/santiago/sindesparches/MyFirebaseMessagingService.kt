package com.santiago.sindesparches

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.santiago.sindesparches.presentation.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "social_notifications"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")

        // Debug completo de los datos recibidos
        Log.d(TAG, "Data payload: ${remoteMessage.data}")
        remoteMessage.data.forEach { (key, value) ->
            Log.d(TAG, "Data key-value: $key = $value")
        }

        // Debug del notification payload si existe
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
        }

        // ✅ NUEVO: Extraer notificationId del payload
        val notificationId = remoteMessage.data["notificationId"]
            ?: remoteMessage.data["notification_id"]

        // Extraer datos importantes
        val planId = remoteMessage.data["planId"]
            ?: remoteMessage.data["plan_id"] // Alternativa por si viene con guión bajo
            ?: remoteMessage.data["PLAN_ID"] // Alternativa en mayúsculas

        val type = remoteMessage.data["type"] ?: "notification"
        val senderId = remoteMessage.data["senderId"] ?: remoteMessage.data["sender_id"]
        val recipientId = remoteMessage.data["recipientId"] ?: remoteMessage.data["recipient_id"]

        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "Nueva notificación"
        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: "Tienes una nueva notificación"

        Log.d(TAG, "Extracted notificationId: $notificationId")
        Log.d(TAG, "Extracted planId: $planId")
        Log.d(TAG, "Extracted type: $type")
        Log.d(TAG, "Extracted senderId: $senderId")
        Log.d(TAG, "Extracted recipientId: $recipientId")
        Log.d(TAG, "Extracted title: $title")
        Log.d(TAG, "Extracted body: $body")

        // ✅ MEJORA: Validaciones mejoradas antes de mostrar notificación
        if (planId != null && notificationId != null) {
            // ✅ NUEVO: Validar que el usuario actual es el destinatario
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null && (recipientId == null || recipientId == currentUserId)) {
                createNotificationWithPlanId(planId, title, body, type, notificationId, senderId)
            } else {
                Log.w(TAG, "⚠️ Notificación no es para el usuario actual: $currentUserId vs $recipientId")
            }
        } else {
            Log.w(TAG, "⚠️ Faltan datos importantes - planId: $planId, notificationId: $notificationId")
            if (planId != null) {
                // Fallback para notificaciones sin notificationId
                createNotificationWithPlanId(planId, title, body, type, null, senderId)
            } else {
                createBasicNotification(title, body)
            }
        }
    }

    private fun createNotificationWithPlanId(
        planId: String,
        title: String,
        body: String,
        type: String,
        notificationId: String? = null,
        senderId: String? = null
    ) {
        Log.d(TAG, "🔔 Creando notificación con planId: $planId, notificationId: $notificationId")

        // ✅ MEJORA: Intent más completo con todos los datos necesarios
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            // Datos principales
            putExtra("planId", planId)
            putExtra("fromNotification", true)
            putExtra("notificationType", type)
            putExtra("senderId", senderId)

            // ✅ IMPORTANTE: Incluir notificationId para poder marcarlo como leído
            notificationId?.let {
                putExtra("notificationId", it)
            }

            // ✅ MEJORA: Agregar timestamp para forzar intent único
            val timestamp = System.currentTimeMillis()
            putExtra("notificationTimestamp", timestamp)

            // Data URI como respaldo con más información
            val uriBuilder = StringBuilder("sindesparches://plan/$planId?fromNotification=true&timestamp=$timestamp")
            notificationId?.let { uriBuilder.append("&notificationId=$it") }
            type.let { uriBuilder.append("&type=$it") }
            senderId?.let { uriBuilder.append("&senderId=$it") }

            data = Uri.parse(uriBuilder.toString())

            // ✅ MEJORA: Acción más específica
            action = "NOTIFICATION_CLICK_${planId}_${type}_$timestamp"
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId?.hashCode() ?: planId.hashCode(), // Usar notificationId como ID único
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ MEJORA: Íconos específicos por tipo de notificación
        val iconRes = when (type) {
            "like" -> R.drawable.bxs_heart // Asegúrate de tener estos íconos
            "comment" -> R.drawable.ic_comment
            "participate" -> R.drawable.bxs_user
            "share" -> R.drawable.bx_share_alt
            else -> R.drawable.bxs_invader
        }

        // ✅ MEJORA: Estilos de notificación por tipo
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(iconRes)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // ✅ NUEVO: Colores por tipo de notificación
        val color = when (type) {
            "like" -> ContextCompat.getColor(this, R.color.notification_like_color)
            "comment" -> ContextCompat.getColor(this, R.color.notification_comment_color)
            "participate" -> ContextCompat.getColor(this, R.color.notification_participate_color)
            "share" -> ContextCompat.getColor(this, R.color.notification_share_color)
            else -> ContextCompat.getColor(this, R.color.notification_default_color)
        }
        notificationBuilder.setColor(color)

        // ✅ MEJORA: Agregar todos los datos extras para debugging y tracking
        val extras = Bundle().apply {
            putString("planId", planId)
            putString("type", type)
            putString("senderId", senderId)
            putString("notificationId", notificationId)
            putBoolean("fromNotification", true)
            putLong("receivedTimestamp", System.currentTimeMillis())
        }
        notificationBuilder.setExtras(extras)

        // ✅ NUEVO: Agregar acciones rápidas para algunos tipos
        if (type == "comment" || type == "like") {
            addQuickActions(notificationBuilder, planId, type)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val displayNotificationId = notificationId?.hashCode() ?: planId.hashCode()

        notificationManager.notify(displayNotificationId, notificationBuilder.build())

        // ✅ NUEVO: Log completo para debugging
        Log.d(TAG, "📱 Notificación creada con:")
        Log.d(TAG, "   - planId: $planId")
        Log.d(TAG, "   - notificationId: $notificationId")
        Log.d(TAG, "   - type: $type")
        Log.d(TAG, "   - senderId: $senderId")
        Log.d(TAG, "   - displayId: $displayNotificationId")
        Log.d(TAG, "   - action: NOTIFICATION_CLICK_${planId}_${type}_${System.currentTimeMillis()}")
    }

    // ✅ NUEVO: Agregar acciones rápidas a las notificaciones
    private fun addQuickActions(
        builder: NotificationCompat.Builder,
        planId: String,
        type: String
    ) {
        // Acción "Ver Plan"
        val viewIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("planId", planId)
            putExtra("fromNotification", true)
            putExtra("quickAction", "view")
        }
        val viewPendingIntent = PendingIntent.getActivity(
            this,
            ("view_$planId").hashCode(),
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.visibility, "Ver", viewPendingIntent)

        // Acción específica por tipo
        when (type) {
            "comment" -> {
                val replyIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("planId", planId)
                    putExtra("fromNotification", true)
                    putExtra("quickAction", "reply")
                }
                val replyPendingIntent = PendingIntent.getActivity(
                    this,
                    ("reply_$planId").hashCode(),
                    replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_comment, "Responder", replyPendingIntent)
            }
            "like" -> {
                val likeBackIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("planId", planId)
                    putExtra("fromNotification", true)
                    putExtra("quickAction", "like_back")
                }
                val likeBackPendingIntent = PendingIntent.getActivity(
                    this,
                    ("like_back_$planId").hashCode(),
                    likeBackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.bxs_user, "Me gusta", likeBackPendingIntent)
            }
        }
    }

    private fun createBasicNotification(title: String, body: String) {
        Log.d(TAG, "Creando notificación básica sin planId")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_comment)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM token: $token")

        // ✅ MEJORA: Usar NotificationHelper para consistencia
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Usar corrutina para llamar la función suspend
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val notificationHelper = NotificationHelper()
                    val db = FirebaseFirestore.getInstance()
                    notificationHelper.saveUserFCMToken(db, currentUser.uid)
                    Log.d(TAG, "✅ Nuevo token FCM guardado usando NotificationHelper")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al guardar nuevo token con NotificationHelper", e)
                    // Fallback al método original
                    saveTokenDirectly(token, currentUser.uid)
                }
            }
        }
    }

    // ✅ NUEVO: Método de respaldo para guardar token
    private fun saveTokenDirectly(token: String, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val userData = hashMapOf(
            "fcmToken" to token,
            "lastTokenUpdate" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "✅ Nuevo token FCM guardado directamente")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error al guardar nuevo token FCM directamente", e)
            }
    }
}