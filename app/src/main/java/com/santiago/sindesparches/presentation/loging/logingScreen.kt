package com.santiago.sindesparches.presentation.loging

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_iniciar
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.gris
import com.santiago.sindesparches.ui.theme.white

@Composable
fun logingScreen(
    auth: FirebaseAuth,
    navigatetoinicialScreen: () -> Unit = {},
    navigateToVerificacionCorreo: (String, String) -> Unit = { _, _ -> }
) {
    var usuario by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password_registro by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenHeight < 700.dp

    // Tamaños adaptativos
    val headerHeight = if (isSmallScreen) (screenHeight * 0.25f) else (screenHeight * 0.35f)
    val imageSize = if (isSmallScreen) 200.dp else 300.dp
    val titleSize = if (isSmallScreen) 28.sp else 36.sp
    val textFieldWidth = (screenWidth * 0.85f).coerceAtMost(350.dp)
    val buttonHeight = if (isSmallScreen) 48.dp else 56.dp

    val aquaBlue = Color(0xFF00E5FF)
    val neonYellow = Color(0xFFFFEB3B)
    val hotPink = Color(0xFFFF1744)

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Header con imagen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(azul_comienzo, azul_mitad, azul_final)
                    )
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.crear_cuenta),
                    contentDescription = null,
                    modifier = Modifier.size(imageSize),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Contenedor principal del formulario
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight - 30.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(azul_mitad)
        ) {
            // Elementos decorativos de fondo
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircle(
                    color = aquaBlue.copy(alpha = 0.2f),
                    radius = 120f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f)
                )
                drawCircle(
                    color = hotPink.copy(alpha = 0.2f),
                    radius = 80f,
                    center = Offset(size.width * 0.2f, size.height * 0.7f)
                )
                drawCircle(
                    color = neonYellow.copy(alpha = 0.2f),
                    radius = 60f,
                    center = Offset(size.width * 0.9f, size.height * 0.8f)
                )
            }

            // Columna scrolleable con el contenido del formulario
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(
                        top = if (isSmallScreen) 12.dp else 20.dp,
                        bottom = 20.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CREAR CUENTA",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = white,
                    modifier = Modifier.padding(bottom = if (isSmallScreen) 12.dp else 16.dp)
                )

                // Campo usuario
                OutlinedTextField(
                    value = usuario,
                    onValueChange = { usuario = it },
                    placeholder = { Text("NOMBRE DE USUARIO", color = white.copy(alpha = 0.7f)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isSmallScreen) 16.sp else 18.sp
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxs_user),
                            contentDescription = null,
                            tint = white,
                            modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = boton_texto,
                        unfocusedContainerColor = boton,
                        unfocusedIndicatorColor = white,
                        focusedIndicatorColor = white,
                        focusedTextColor = black,
                        unfocusedTextColor = white,
                        cursorColor = white
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier
                        .width(textFieldWidth)
                        .padding(vertical = if (isSmallScreen) 4.dp else 6.dp)
                )

                // Campo correo
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = { Text("CORREO", color = white.copy(alpha = 0.7f)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isSmallScreen) 16.sp else 18.sp
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxs_envelope),
                            contentDescription = null,
                            tint = white,
                            modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = boton_texto,
                        unfocusedContainerColor = boton,
                        unfocusedIndicatorColor = white,
                        focusedIndicatorColor = white,
                        focusedTextColor = black,
                        unfocusedTextColor = white,
                        cursorColor = white
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier
                        .width(textFieldWidth)
                        .padding(vertical = if (isSmallScreen) 4.dp else 6.dp)
                )

                // Campo contraseña
                OutlinedTextField(
                    value = password_registro,
                    onValueChange = { password_registro = it },
                    placeholder = { Text("CONTRASEÑA", color = white.copy(alpha = 0.7f)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isSmallScreen) 16.sp else 18.sp
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxs_lock),
                            contentDescription = null,
                            tint = white,
                            modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = boton_texto,
                        unfocusedContainerColor = boton,
                        unfocusedIndicatorColor = white,
                        focusedIndicatorColor = white,
                        focusedTextColor = black,
                        unfocusedTextColor = white,
                        cursorColor = white
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier
                        .width(textFieldWidth)
                        .padding(vertical = if (isSmallScreen) 4.dp else 6.dp)
                )

                // Mensaje de error
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .width(textFieldWidth)
                            .padding(horizontal = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium,
                            fontSize = if (isSmallScreen) 12.sp else 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                // Botón crear cuenta
                Button(
                    onClick = {
                        if (correo.isBlank() || password_registro.isBlank() || usuario.isBlank()) {
                            errorMessage = "Se encuentran campos vacíos"
                        } else {
                            isLoading = true
                            errorMessage = null

                            auth.createUserWithEmailAndPassword(correo, password_registro)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        user?.sendEmailVerification()
                                            ?.addOnCompleteListener { verificationTask ->
                                                isLoading = false
                                                if (verificationTask.isSuccessful) {
                                                    navigateToVerificacionCorreo(correo, usuario)
                                                } else {
                                                    errorMessage = "Error al enviar correo de verificación"
                                                    user.delete()
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        errorMessage = task.exception?.message ?: "Usuario no admitido"
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .width(textFieldWidth)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = boton_iniciar),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = white,
                            modifier = Modifier.size(if (isSmallScreen) 16.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Creando cuenta...",
                            color = white,
                            fontSize = if (isSmallScreen) 14.sp else 16.sp
                        )
                    } else {
                        Text(
                            "CREAR CUENTA",
                            fontWeight = FontWeight.Bold,
                            color = white,
                            fontSize = if (isSmallScreen) 14.sp else 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))

                // Botón volver
                TextButton(
                    onClick = { navigatetoinicialScreen() },
                    modifier = Modifier.padding(bottom = if (isSmallScreen) 8.dp else 12.dp)
                ) {
                    Text(
                        "Volver",
                        color = white.copy(alpha = 0.8f),
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    )
                }

                // Espaciado adicional para asegurar que todo sea visible
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}