package com.santiago.sindesparches

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
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
import com.google.firebase.messaging.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.santiago.sindesparches.presentation.notifications.NotificationHelper
import com.santiago.sindesparches.ui.theme.SinDesparchesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
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

        // Splash
        setTheme(R.style.splash_screen)

        // Inicializar SDKs
        FacebookSdk.sdkInitialize(applicationContext)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        analytics = Firebase.analytics
        auth = Firebase.auth
        db = Firebase.firestore

        setupNotifications()
        checkAuthAndSaveToken()

        // ✅ NUEVO: Limpiar notificaciones antiguas al iniciar
        performStartupTasks()

        enableEdgeToEdge()
        setContent {
            SinDesparchesTheme {
                navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) {
                    // Simplificamos: solo pasamos el intent, sin callback de navegación
                    Navegacion(
                        navController = navController,
                        auth = auth,
                        db = db,
                        intent = intent
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        Log.d(TAG, "=== onNewIntent ===")
        Log.d(TAG, "planId: ${intent.getStringExtra("planId")}")
        Log.d(TAG, "fromNotification: ${intent.getBooleanExtra("fromNotification", false)}")

        // ✅ IMPORTANTE: Actualizar el intent actual
        setIntent(intent)

        // ✅ NUEVO: Marcar notificación como leída si viene de una notificación
        handleNotificationIntent(intent)

        // ✅ MEJORA: Forzar recomposición si es necesario
        if (intent.getBooleanExtra("fromNotification", false)) {
            recreate() // Esto reiniciará la actividad con el nuevo intent
        }
    }

    // ✅ NUEVO: Manejar intents de notificación
    private fun handleNotificationIntent(intent: Intent) {
        if (intent.getBooleanExtra("fromNotification", false)) {
            val notificationId = intent.getStringExtra("notificationId")

            if (!notificationId.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val notificationHelper = NotificationHelper()
                    notificationHelper.markNotificationAsRead(db, notificationId)
                    Log.d(TAG, "Notificación marcada como leída: $notificationId")
                }
            }
        }
    }

    // ✅ NUEVO: Tareas de inicio de la aplicación
    private fun performStartupTasks() {
        lifecycleScope.launch {
            try {
                val notificationHelper = NotificationHelper()

                // Limpiar notificaciones de más de 30 días
                notificationHelper.cleanupOldNotifications(db, daysOld = 30)

                // Opcional: Mostrar estadísticas en desarrollo (solo para debugging)
                if (BuildConfig.DEBUG) {
                    val currentUser = auth.currentUser
                    currentUser?.let { user ->
                        val stats = notificationHelper.getNotificationStats(db, user.uid)
                        Log.d(TAG, "Estadísticas de notificaciones: $stats")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en tareas de inicio", e)
            }
        }
    }

    private fun checkAuthAndSaveToken() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                val notificationHelper = NotificationHelper()
                notificationHelper.saveUserFCMToken(db, currentUser.uid)
            }
            Log.d(TAG, "Usuario autenticado, guardando token FCM: ${currentUser.uid}")
        } else {
            Log.d(TAG, "Usuario no autenticado aún")
        }
    }

    private fun setupNotifications() {
        requestNotificationPermission()
        createNotificationChannel()
        getFCMToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "Permiso de notificaciones ya otorgado")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "social_notifications",
                "Notificaciones SinDesparches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal principal de notificaciones"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificación creado")
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "Token FCM: $token")

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    lifecycleScope.launch {
                        val helper = NotificationHelper()
                        helper.saveUserFCMToken(db, currentUser.uid)
                    }
                }
                sendTokenToServer(token)
            } else {
                Log.e(TAG, "Error al obtener token FCM", task.exception)
            }
        }
    }

    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "Enviando token al servidor: $token")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de notificación concedido")
            } else {
                Log.w(TAG, "Permiso de notificación denegado")
            }
        }
    }

    // ✅ NUEVO: Función para limpiar notificaciones al cerrar sesión
    fun onUserSignOut() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                currentUser?.let { user ->
                    val notificationHelper = NotificationHelper()
                    // Opcional: eliminar notificaciones del usuario al cerrar sesión
                    // notificationHelper.deleteNotificationsByFilter(db, user.uid)
                    Log.d(TAG, "Limpieza de notificaciones al cerrar sesión")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al limpiar notificaciones en sign out", e)
            }
        }
    }
}