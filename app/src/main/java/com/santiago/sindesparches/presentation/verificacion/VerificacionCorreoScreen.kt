package com.santiago.sindesparches.presentation.verificacion

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.santiago.sindesparches.R
import kotlinx.coroutines.delay

@Composable
fun VerificacionCorreoScreen(
    correo: String,
    usuario: String,
    auth: FirebaseAuth,
    navigateToEstadoRegistro: (String) -> Unit = {},
    navigateToLoging: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var countdownTime by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var verificationTimeout by remember { mutableStateOf(1800) }
    var userExplicitlyLeft by remember { mutableStateOf(false) }

    // Animaciones más sutiles
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")

    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val floatingAnimation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Función para eliminar usuario no verificado
    fun deleteUnverifiedUser(reason: String) {
        val currentUser = auth.currentUser
        currentUser?.delete()?.addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                Log.d("VerificacionCorreo", "Usuario eliminado por: $reason")
            } else {
                Log.e("VerificacionCorreo", "Error al eliminar usuario: ${deleteTask.exception?.message}")
            }
            auth.signOut()
            navigateToLoging()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("VerificacionCorreo", "App pausada - NO eliminando cuenta automáticamente")
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("VerificacionCorreo", "App reanudada - verificando estado del email")
                    val currentUser = auth.currentUser
                    currentUser?.reload()?.addOnCompleteListener { task ->
                        if (task.isSuccessful && currentUser.isEmailVerified && !isVerified) {
                            isVerified = true
                            Log.d("VerificacionCorreo", "Email verificado al regresar a la app")
                            navigateToEstadoRegistro(usuario)
                        }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    if (!isVerified && userExplicitlyLeft) {
                        Log.d("VerificacionCorreo", "Destrucción con salida explícita, eliminando cuenta")
                        deleteUnverifiedUser("Destrucción explícita")
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        dialogMessage = "⚠️ Si sales ahora sin verificar tu correo, tu cuenta será eliminada permanentemente.\n\n" +
                "Debes verificar tu email para poder continuar. ¿Realmente quieres salir?"
        showDialog = true
    }

    LaunchedEffect(Unit) {
        while (verificationTimeout > 0 && !isVerified) {
            delay(1000)
            verificationTimeout--
        }
        if (!isVerified && verificationTimeout <= 0) {
            Log.d("VerificacionCorreo", "Timeout de verificación alcanzado (30 min)")
            dialogMessage = "⏰ Han pasado 30 minutos sin verificación. Tu cuenta será eliminada por seguridad."
            showDialog = true
            delay(5000)
            deleteUnverifiedUser("Timeout de 30 minutos")
        }
    }

    LaunchedEffect(countdownTime) {
        if (countdownTime > 0) {
            while (countdownTime > 0 && !isVerified) {
                delay(1000)
                countdownTime--
            }
            canResend = true
        }
    }

    LaunchedEffect(Unit) {
        while (!isVerified) {
            delay(2000)
            val currentUser = auth.currentUser
            currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (currentUser.isEmailVerified) {
                        isVerified = true
                        Log.d("VerificacionCorreo", "Email verificado exitosamente")
                        navigateToEstadoRegistro(usuario)
                    }
                } else {
                    Log.e("VerificacionCorreo", "Error al verificar email: ${task.exception?.message}")
                }
            }
        }
    }

    // Colores más neutros
    val primaryBlue = Color(0xFF2196F3)
    val darkBlue = Color(0xFF1976D2)
    val lightBlue = Color(0xFF64B5F6)
    val warningOrange = Color(0xFFFF9800)
    val errorRed = Color(0xFFF44336)
    val successGreen = Color(0xFF4CAF50)
    val neutralGray = Color(0xFF9E9E9E)
    val darkBackground = Color(0xFF0F172A) // Azul muy oscuro


    val gradientColors = listOf(
        Color(0xFF1E3A8A),
        Color(0xFF1E40AF),
        Color(0xFF3B82F6)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo más sutil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            darkBackground,
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
        ) {
            // Elementos decorativos más neutros
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = 80f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f + floatingAnimation),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 60f,
                    center = Offset(size.width * 0.1f, size.height * 0.3f - floatingAnimation)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = 40f,
                    center = Offset(size.width * 0.9f, size.height * 0.7f + floatingAnimation * 0.5f)
                )
            }
        }

        // Contenido principal con scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header con icono
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(y = floatingAnimation.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                lightBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = 100f
                        ),
                        radius = 50.dp.toPx()
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.bxs_envelope),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .scale(pulseAnimation)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Verifica tu correo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Te hemos enviado un link de verificación a:",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Correo destacado
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = correo,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ADVERTENCIA SOBRE SPAM - MUY DESTACADA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = warningOrange.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, warningOrange)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulseAnimation)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "¡IMPORTANTE!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "📧 REVISA TU CARPETA DE SPAM",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Muchos correos de verificación llegan a la carpeta de SPAM o correo no deseado. Si no lo encuentras en tu bandeja principal, ¡búscalo ahí!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Indicador de tiempo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (verificationTimeout > 300) {
                        successGreen.copy(alpha = 0.2f)
                    } else {
                        errorRed.copy(alpha = 0.2f)
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (verificationTimeout > 300) successGreen else errorRed
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = if (verificationTimeout > 300) successGreen else errorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tiempo restante: ${verificationTimeout / 60}:${String.format("%02d", verificationTimeout % 60)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Instrucciones simples
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = lightBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instrucciones",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = lightBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val instructions = listOf(
                        "1. Abre tu aplicación de correo",
                        "2. Busca el email en SPAM si no está en la bandeja principal",
                        "3. Haz clic en el enlace de verificación",
                        "4. Regresa a la app para continuar"
                    )

                    instructions.forEach { instruction ->
                        Text(
                            text = instruction,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(vertical = 4.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = lightBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verificando...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Botón para reenviar correo
            Button(
                onClick = {
                    if (canResend) {
                        isLoading = true
                        val currentUser = auth.currentUser
                        currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    countdownTime = 60
                                    canResend = false
                                    Log.d("VerificacionCorreo", "Email de verificación reenviado")
                                    dialogMessage = "✅ ¡Correo reenviado! Revisa tu bandeja de entrada y SPAM."
                                    showDialog = true
                                } else {
                                    Log.e("VerificacionCorreo", "Error al reenviar email: ${task.exception?.message}")
                                    dialogMessage = "❌ Error al reenviar correo. Inténtalo nuevamente."
                                    showDialog = true
                                }
                            }
                    }
                },
                enabled = canResend && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canResend) primaryBlue else neutralGray,
                    disabledContainerColor = neutralGray.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (canResend) "Reenviar correo" else "Reenviar en ${countdownTime}s",
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de verificación manual
            OutlinedButton(
                onClick = {
                    isLoading = true
                    val currentUser = auth.currentUser
                    currentUser?.reload()?.addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            if (currentUser.isEmailVerified) {
                                isVerified = true
                                navigateToEstadoRegistro(usuario)
                            } else {
                                dialogMessage = "El correo aún no ha sido verificado. Recuerda revisar tu carpeta de SPAM."
                                showDialog = true
                            }
                        } else {
                            Log.e("VerificacionCorreo", "Error al verificar manualmente: ${task.exception?.message}")
                            dialogMessage = "❌ Error al verificar. Inténtalo nuevamente."
                            showDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(2.dp, lightBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    "Ya verifiqué mi correo",
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de cancelar
            TextButton(
                onClick = {
                    userExplicitlyLeft = true
                    dialogMessage = "⚠️ Si sales sin verificar tu correo, tu cuenta será eliminada permanentemente.\n\n¿Estás seguro de que quieres cancelar el registro?"
                    showDialog = true
                }
            ) {
                Text(
                    "Cancelar registro",
                    color = errorRed.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Diálogo
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!dialogMessage.contains("agotado") && !dialogMessage.contains("cancelar")) {
                    showDialog = false
                }
            },
            containerColor = darkBackground,
            title = {
                val title = when {
                    dialogMessage.contains("salir") || dialogMessage.contains("cancelar") -> "⚠️ Confirmar Salida"
                    dialogMessage.contains("agotado") -> "⏰ Tiempo Agotado"
                    dialogMessage.contains("reenviado") -> "✅ Correo Enviado"
                    dialogMessage.contains("Error") -> "❌ Error"
                    else -> "ℹ️ Información"
                }
                Text(
                    title,
                    color = when {
                        dialogMessage.contains("salir") || dialogMessage.contains("cancelar") || dialogMessage.contains("agotado") -> errorRed
                        dialogMessage.contains("reenviado") -> successGreen
                        dialogMessage.contains("Error") -> warningOrange
                        else -> lightBlue
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    dialogMessage,
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                when {
                    dialogMessage.contains("cancelar") -> {
                        Button(
                            onClick = {
                                showDialog = false
                                deleteUnverifiedUser("Usuario canceló explícitamente")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = errorRed
                            )
                        ) {
                            Text("Sí, cancelar", color = Color.White)
                        }
                    }
                    dialogMessage.contains("agotado") -> {
                        // Sin botón para timeout automático
                    }
                    else -> {
                        Button(
                            onClick = { showDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue
                            )
                        ) {
                            Text("Entendido", color = Color.White)
                        }
                    }
                }
            },
            dismissButton = {
                if (dialogMessage.contains("cancelar")) {
                    TextButton(
                        onClick = {
                            showDialog = false
                            userExplicitlyLeft = false
                        }
                    ) {
                        Text("Continuar", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        )
    }
}