package com.santiago.sindesparches.presentation.contraseña

import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.collection.intIntMapOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.Purple
import com.santiago.sindesparches.ui.theme.azul
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.gris
import com.santiago.sindesparches.ui.theme.white
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun DefinirContraseñaScreen(email: String, auth: FirebaseAuth, onLogout: () -> Unit, onPasswordDefined: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val user = FirebaseAuth.getInstance().currentUser
    val nombreUsuario = user?.displayName

    // Colores del tema
    val darkBackground = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFF6C63FF) // Púrpura vibrante
    val accentColor = Color(0xFF00D4FF) // Azul cyan
    val successColor = Color(0xFF00C896) // Verde menta
    val errorColor = Color(0xFFFF6B6B) // Rojo coral
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition()
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val cardScale by animateFloatAsState(
        targetValue = if (errorMessage != null) 0.98f else 1f,
        animationSpec = tween(200)
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (password.length >= 6) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // Intercepta el botón de retroceso
    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    "¿Cerrar sesión?",
                    color = textPrimary
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que quieres salir? Se eliminará tu usuario",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.delete()
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("FirebaseAuth", "Usuario eliminado correctamente")
                                } else {
                                    Log.e("FirebaseAuth", "Error al eliminar usuario", task.exception)
                                }
                            }
                        auth.signOut()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) {
                    Text("Sí, salir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = primaryColor)
                }
            },
            containerColor = surfaceColor
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        darkBackground,
                        Color(0xFF1A1A1A),
                        Color(0xFF0F0F0F)
                    )
                )
            )
    ) {
        // Elementos decorativos de fondo
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = center
            )

            // Círculos decorativos animados
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.05f + gradientShift * 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.3f),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(size.width * 0.2f, size.height * 0.3f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        successColor.copy(alpha = 0.05f + (1f - gradientShift) * 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.7f),
                    radius = 150f
                ),
                radius = 150f,
                center = Offset(size.width * 0.8f, size.height * 0.7f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título con animación
            Text(
                text = "Definir Contraseña",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Crea una contraseña segura para tu cuenta",
                fontSize = 16.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Tarjeta principal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(cardScale),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Información del usuario
                    Row(
                        modifier = Modifier.padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(primaryColor, accentColor)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Usuario",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Hola,",
                                fontSize = 14.sp,
                                color = textSecondary
                            )
                            Text(
                                text = nombreUsuario ?: "Usuario",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }
                    }

                    // Campo de contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Nueva Contraseña") },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Image(
                                    painter = painterResource(id = if (isPasswordVisible) R.drawable.visibility else R.drawable.visibility_off),
                                    contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    colorFilter = ColorFilter.tint(if (password.length >= 6) successColor else textSecondary),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Contraseña",
                                tint = if (password.length >= 6) successColor else textSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = if (password.length >= 6) successColor else primaryColor,
                            unfocusedBorderColor = Color(0xFF404040),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = primaryColor,
                            focusedLabelColor = if (password.length >= 6) successColor else primaryColor,
                            unfocusedLabelColor = textSecondary,
                            focusedPlaceholderColor = textSecondary,
                            unfocusedPlaceholderColor = textSecondary
                        )
                    )

                    // Indicador de fortaleza de contraseña
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(4.dp)
                                        .padding(end = 4.dp)
                                        .background(
                                            color = when {
                                                password.length >= 6 && index < 3 -> successColor
                                                password.length >= 4 && index < 2 -> accentColor
                                                password.length >= 2 && index < 1 -> errorColor
                                                else -> Color(0xFF404040)
                                            },
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }

                        Text(
                            text = when {
                                password.length >= 6 -> "Fuerte"
                                password.length >= 4 -> "Media"
                                password.length >= 2 -> "Débil"
                                else -> ""
                            },
                            fontSize = 12.sp,
                            color = when {
                                password.length >= 6 -> successColor
                                password.length >= 4 -> accentColor
                                password.length >= 2 -> errorColor
                                else -> textSecondary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón de guardar
                    Button(
                        onClick = {
                            if (password.length < 6) {
                                errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                return@Button
                            }

                            val credential = EmailAuthProvider.getCredential(email, password)

                            user?.linkWithCredential(credential)
                                ?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("Auth", "Cuenta vinculada con correo y contraseña")
                                        onPasswordDefined()
                                    } else {
                                        errorMessage = "Error al vincular cuenta: ${task.exception?.localizedMessage}"
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .scale(buttonScale),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (password.length >= 6) successColor else Color(0xFF404040)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = password.length >= 6
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar Contraseña",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Mensaje de error
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = errorColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = errorColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 14.sp,
                            color = errorColor
                        )
                    }
                }
            }
        }
    }

    // Efecto para limpiar el mensaje de error
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(5000)
            errorMessage = null
        }
    }
}