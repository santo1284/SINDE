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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_iniciar
import com.santiago.sindesparches.ui.theme.white
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
    var verificationTimeout by remember { mutableStateOf(1800) } // 30 minutos timeout (más realista)
    var userExplicitlyLeft by remember { mutableStateOf(false) } // Nuevo: controla si el usuario salió explícitamente

    // Animaciones (mantener igual)
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val floatingAnimation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Función para eliminar usuario no verificado (solo cuando sea explícito)
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

    // CAMBIO PRINCIPAL: Solo observar cuando el usuario EXPLÍCITAMENTE decide salir
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Ya NO eliminamos la cuenta automáticamente
                    Log.d("VerificacionCorreo", "App pausada - NO eliminando cuenta automáticamente")
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Cuando regrese a la app, verificar si el email fue confirmado
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
                    // Solo eliminar si el usuario explícitamente salió
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

    // CAMBIO: BackHandler ahora pregunta antes de eliminar
    BackHandler {
        dialogMessage = "⚠️ Si sales ahora sin verificar tu correo, tu cuenta será eliminada permanentemente.\n\n" +
                "Debes verificar tu email para poder continuar. ¿Realmente quieres salir?"
        showDialog = true
    }

    // Timeout automático AUMENTADO a 30 minutos
    LaunchedEffect(Unit) {
        while (verificationTimeout > 0 && !isVerified) {
            delay(1000)
            verificationTimeout--
        }
        if (!isVerified && verificationTimeout <= 0) {
            Log.d("VerificacionCorreo", "Timeout de verificación alcanzado (30 min)")
            dialogMessage = "⏰ Han pasado 30 minutos sin verificación. Tu cuenta será eliminada por seguridad."
            showDialog = true
            delay(5000) // Más tiempo para leer el mensaje
            deleteUnverifiedUser("Timeout de 30 minutos")
        }
    }

    // Countdown timer para reenvío
    LaunchedEffect(countdownTime) {
        if (countdownTime > 0) {
            while (countdownTime > 0 && !isVerified) {
                delay(1000)
                countdownTime--
            }
            canResend = true
        }
    }

    // MEJORA: Verificar email más frecuentemente cuando la app está activa
    LaunchedEffect(Unit) {
        while (!isVerified) {
            delay(2000) // Verificar cada 2 segundos en lugar de 3
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

    // Colores (mantener igual)
    val darkBackground = Color(0xFF0A0A0F)
    val primaryPurple = Color(0xFF9C27B0)
    val neonPink = Color(0xFFE91E63)
    val electricBlue = Color(0xFF03DAC6)
    val neonYellow = Color(0xFFFFEB3B)
    val vibrantOrange = Color(0xFFFF5722)
    val acidGreen = Color(0xFF76FF03)
    val hotMagenta = Color(0xFFFF1744)

    val gradientColors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo con gradiente dinámico (mantener igual)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors
                    )
                )
        ) {
            // Elementos decorativos animados (mantener igual)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = neonPink.copy(alpha = pulseAnimation * 0.4f),
                    radius = 120f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f + floatingAnimation)
                )
                drawCircle(
                    color = electricBlue.copy(alpha = pulseAnimation * 0.3f),
                    radius = 90f,
                    center = Offset(size.width * 0.1f, size.height * 0.3f - floatingAnimation)
                )
                drawCircle(
                    color = acidGreen.copy(alpha = pulseAnimation * 0.35f),
                    radius = 70f,
                    center = Offset(size.width * 0.9f, size.height * 0.7f + floatingAnimation * 0.5f)
                )
                drawCircle(
                    color = vibrantOrange.copy(alpha = pulseAnimation * 0.25f),
                    radius = 100f,
                    center = Offset(size.width * 0.2f, size.height * 0.8f - floatingAnimation * 0.7f)
                )

                rotate(rotationAnimation) {
                    drawCircle(
                        color = neonYellow.copy(alpha = 0.1f),
                        radius = 200f,
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                rotate(-rotationAnimation * 0.5f) {
                    drawCircle(
                        color = hotMagenta.copy(alpha = 0.15f),
                        radius = 150f,
                        center = Offset(size.width * 0.5f, size.height * 0.6f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Header con icono animado (mantener igual)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryPurple.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        radius = 400f
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(y = floatingAnimation.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    electricBlue.copy(alpha = pulseAnimation * 0.5f),
                                    Color.Transparent
                                ),
                                radius = 140f
                            ),
                            radius = 70.dp.toPx()
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.bxs_envelope),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.Center)
                            .scale(1f + pulseAnimation * 0.1f)
                    )
                }
            }
        }

        // Contenido principal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 220.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E).copy(alpha = 0.95f),
                            Color(0xFF0F0F1A).copy(alpha = 0.98f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡VERIFICA TU CORREO!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CAMBIO: Indicador de tiempo más realista (30 minutos)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (verificationTimeout > 300) {
                            acidGreen.copy(alpha = 0.2f)
                        } else {
                            hotMagenta.copy(alpha = 0.3f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (verificationTimeout > 300) acidGreen else hotMagenta
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
                            tint = if (verificationTimeout > 300) acidGreen else hotMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tiempo restante: ${verificationTimeout / 60}:${String.format("%02d", verificationTimeout % 60)}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "✨ ¡Ya casi puedes disfrutar de los mejores eventos! ✨",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hemos enviado un link de verificación a:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(electricBlue.copy(alpha = 0.3f), neonPink.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = correo,
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CAMBIO: Instrucciones actualizadas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E2E).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, electricBlue.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = neonYellow,
                                modifier = Modifier
                                    .size(28.dp)
                                    .scale(1f + pulseAnimation * 0.1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INSTRUCCIONES",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = electricBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val instructions = listOf(
                            "📧 Revisa tu bandeja de entrada y SPAM",
                            "🔗 Haz clic en el enlace de verificación",
                            "✅ Puedes salir de la app para verificar",
                            "🔄 Al regresar, verificaremos automáticamente",
                            "⏰ Tienes 30 minutos para completar la verificación"
                        )

                        instructions.forEach { instruction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = instruction,
                                    fontSize = 14.sp,
                                    color = if (instruction.contains("30 minutos")) {
                                        neonYellow.copy(alpha = 0.9f)
                                    } else {
                                        Color.White.copy(alpha = 0.9f)
                                    },
                                    lineHeight = 20.sp,
                                    fontWeight = if (instruction.contains("30 minutos")) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        electricBlue.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = electricBlue,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🔄 Verificando tu email...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botón para reenviar correo (mantener igual)
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
                                        dialogMessage = "✅ ¡Correo reenviado exitosamente! Revisa tu bandeja de entrada."
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
                        .width(300.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (canResend) {
                                    Brush.horizontalGradient(
                                        colors = listOf(neonPink, vibrantOrange, electricBlue)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Gray.copy(alpha = 0.5f),
                                            Color.Gray.copy(alpha = 0.3f)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (canResend) {
                            Text(
                                "🚀 REENVIAR EMAIL",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        } else {
                            Text(
                                "⏰ REENVIAR EN ${countdownTime}s",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
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
                                    dialogMessage = "📧 El correo aún no ha sido verificado. Por favor, verifica tu email y regresa."
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
                        .width(300.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(electricBlue, acidGreen)
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        "✅ YA VERIFIQUÉ MI EMAIL",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CAMBIO: Botón de salir ahora es explícito
                TextButton(
                    onClick = {
                        userExplicitlyLeft = true
                        dialogMessage = "⚠️ Si sales sin verificar tu correo, tu cuenta será eliminada permanentemente.\n\n¿Estás seguro de que quieres cancelar el registro?"
                        showDialog = true
                    }
                ) {
                    Text(
                        "🚪 Cancelar Registro",
                        color = hotMagenta.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // CAMBIO: Diálogo mejorado con diferentes tipos de mensajes
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!dialogMessage.contains("agotado") && !dialogMessage.contains("cancelar")) {
                    showDialog = false
                }
            },
            containerColor = Color(0xFF1A1A2E),
            title = {
                val title = when {
                    dialogMessage.contains("salir") || dialogMessage.contains("cancelar") -> "⚠️ Confirmar Salida"
                    dialogMessage.contains("agotado") -> "⏰ Tiempo Agotado"
                    dialogMessage.contains("exitosamente") -> "✅ Éxito"
                    dialogMessage.contains("Error") -> "❌ Error"
                    else -> "ℹ️ Información"
                }
                Text(
                    title,
                    color = when {
                        dialogMessage.contains("salir") || dialogMessage.contains("cancelar") || dialogMessage.contains("agotado") -> hotMagenta
                        dialogMessage.contains("exitosamente") -> acidGreen
                        dialogMessage.contains("Error") -> vibrantOrange
                        else -> neonYellow
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
                                containerColor = hotMagenta
                            )
                        ) {
                            Text("🗑️ Sí, cancelar registro", color = Color.White)
                        }
                    }
                    dialogMessage.contains("agotado") -> {
                        // Sin botón para timeout automático
                    }
                    else -> {
                        Button(
                            onClick = { showDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = electricBlue
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
                            userExplicitlyLeft = false // Reset flag
                        }
                    ) {
                        Text("Continuar verificando", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        )
    }
}