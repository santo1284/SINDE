package com.santiago.sindesparches

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Manejar datos personalizados
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            val planId = remoteMessage.data["planId"]
            val senderId = remoteMessage.data["senderId"]
            val type = remoteMessage.data["type"]

            sendNotification(title, body, planId, senderId, type)
        }

        // Manejar payload de notificación estándar
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body, null, null, null)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Guardar token localmente
        saveTokenToPreferences(token)

        // Enviar token al servidor si el usuario está autenticado
        val userId = auth.currentUser?.uid
        if (userId != null) {
            sendRegistrationToServer(token, userId)
        }
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs: SharedPreferences = getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    private fun sendRegistrationToServer(token: String, userId: String) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")

        // Actualizar token en Firestore
        db.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "Token actualizado en servidor")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar token en servidor", e)
            }
    }

    private fun sendNotification(
        title: String?,
        messageBody: String?,
        planId: String?,
        senderId: String?,
        type: String?
    ) {
        val channelId = "social_notifications"

        // Crear intent para abrir la actividad específica
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("planId", planId)
            putExtra("senderId", senderId)
            putExtra("notificationType", type)
            putExtra("fromNotification", true)
        }

        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.icono_logo)
            .setContentTitle(title ?: "SinDesparches")
            .setContentText(messageBody ?: "Nueva notificación")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setGroup("social_notifications")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación para Android 8.0+
        createNotificationChannel(notificationManager)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "Notification displayed with ID: $notificationId")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "social_notifications"
            val channelName = "Notificaciones Sociales"
            val channelDescription = "Notificaciones de interacciones en planes"

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}