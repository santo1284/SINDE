package com.santiago.sindesparches

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.facebook.FacebookSdk
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.santiago.sindesparches.presentation.notifications.NotificationHelper
import com.santiago.sindesparches.ui.theme.SinDesparchesTheme
import com.santiago.sindesparches.ui.theme.black
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var navControlller: NavHostController
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    private lateinit var analytics: FirebaseAnalytics

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de Firebase y otros servicios
        setupFirebase()
        setupNotifications()
        checkAuthAndSaveToken()

        // Manejar el intent de notificación si la app se abre desde una
        handleNotificationIntent(intent)

        enableEdgeToEdge()
        setContent {
            navControlller = rememberNavController()
            SinDesparchesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Navegacion(navControlller, auth, db, intent)
                }
            }
        }
    }

    // Nueva función para verificar autenticación y guardar token
    private fun checkAuthAndSaveToken() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya está logueado, guardar token FCM
            lifecycleScope.launch {
                val notificationHelper = NotificationHelper()
                notificationHelper.saveUserFCMToken(db, currentUser.uid)
            }
            Log.d(TAG, "Usuario autenticado, guardando token FCM para: ${currentUser.uid}")
        } else {
            Log.d(TAG, "Usuario no autenticado, esperando login")
        }
    }

    private fun setupNotifications() {
        // 1. Solicitar permiso de notificaciones para Android 13+
        requestNotificationPermission()

        // 2. Crear canal de notificación
        createNotificationChannel()

        // 3. Obtener token FCM
        getFCMToken()

    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "Notification permission already granted")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default_channel"
            val name = "Notificaciones SinDesparches"
            val descriptionText = "Canal principal para notificaciones de la app"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Notification channel created: $channelId")
        }
    }

    // Modificar getFCMToken para que también guarde el token después del login
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")

            // Guardar token si hay usuario autenticado
            val currentUser = auth.currentUser
            if (currentUser != null) {
                lifecycleScope.launch {
                    val notificationHelper = NotificationHelper()
                    notificationHelper.saveUserFCMToken(db, currentUser.uid)
                }
            }

            sendTokenToServer(token)
        })
    }

    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "Token to send to server: $token")
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission granted")
                } else {
                    Log.w(TAG, "Notification permission denied")
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun setupFirebase() {
        FirebaseApp.initializeApp(this)
        analytics = Firebase.analytics
        auth = Firebase.auth
        db = Firebase.firestore

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("TOKEN FCM", "Token: ${task.result}")
            } else {
                Log.e("TOKEN FCM", "Error al obtener el token", task.exception)
            }
        }
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent?.action == "OPEN_PLAN_DETAIL") {
            val planId = intent.getStringExtra("planId")
            if (!planId.isNullOrEmpty()) {
                // Navegar a la pantalla de detalles del plan
                navControlller.navigate("plan_detail/$planId")
                // Limpiar el intent para que no se vuelva a procesar
                setIntent(android.content.Intent())
            }
        }
    }
}