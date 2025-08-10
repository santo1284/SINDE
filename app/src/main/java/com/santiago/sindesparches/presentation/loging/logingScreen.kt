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
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val aquaBlue = Color(0xFF00E5FF)
    val neonYellow = Color(0xFFFFEB3B)
    val hotPink = Color(0xFFFF1744)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(azul_comienzo, azul_mitad, azul_final)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.crear_cuenta),
                    contentDescription = null,
                    modifier = Modifier.size(350.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 300.dp)
                .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
                .background(azul_mitad)
        ) {
            // Elementos decorativos de fondo
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Círculos decorativos
                drawCircle(
                    color = aquaBlue.copy(alpha = 0.3f),
                    radius = 150f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f)
                )
                drawCircle(
                    color = hotPink.copy(alpha = 0.3f),
                    radius = 100f,
                    center = Offset(size.width * 0.2f, size.height * 0.7f)
                )
                drawCircle(
                    color = neonYellow.copy(alpha = 0.3f),
                    radius = 80f,
                    center = Offset(size.width * 0.9f, size.height * 0.8f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CREAR CUENTA",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = white,
                    modifier = Modifier.padding(15.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = usuario,
                    onValueChange = { usuario = it },
                    placeholder = { Text("NOMBRE DE USUARIO", color = white) },
                    singleLine = true,
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxs_user),
                            contentDescription = null,
                            tint = white
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = boton_texto,
                        unfocusedContainerColor = boton,
                        unfocusedIndicatorColor = white,
                        focusedIndicatorColor = boton,
                        focusedTextColor = black,
                        unfocusedTextColor = white,
                        cursorColor = white
                    ),
                    shape = RoundedCornerShape(40.dp),
                    modifier = Modifier.width(350.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = { Text("CORREO", color = white) },
                    singleLine = true,
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxs_envelope),
                            contentDescription = null,
                            tint = white
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = boton_texto,
                        unfocusedContainerColor = boton,
                        unfocusedIndicatorColor = white,
                        focusedIndicatorColor = boton,
                        focusedTextColor = black,
                        unfocusedTextColor = white,
                        cursorColor = white
                    ),
                    shape = RoundedCornerShape(40.dp),
                    modifier = Modifier.width(350.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password_registro,
                    onValueChange = { password_registro = it },
                    placeholder = { Text("CONTRASEÑA", color = white) },
                    singleLine = true,
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxs_lock),
                            contentDescription = null,
                            tint = white
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = boton_texto,
                        unfocusedContainerColor = boton,
                        unfocusedIndicatorColor = white,
                        focusedIndicatorColor = boton,
                        focusedTextColor = black,
                        unfocusedTextColor = white,
                        cursorColor = white
                    ),
                    shape = RoundedCornerShape(40.dp),
                    modifier = Modifier.width(350.dp)
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        if (correo.isBlank() || password_registro.isBlank() || usuario.isBlank()) {
                            errorMessage = "Se encuentran campos vacíos"
                        } else {
                            isLoading = true
                            errorMessage = null

                            // Crear cuenta y enviar verificación
                            auth.createUserWithEmailAndPassword(correo, password_registro)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        user?.sendEmailVerification()
                                            ?.addOnCompleteListener { verificationTask ->
                                                isLoading = false
                                                if (verificationTask.isSuccessful) {
                                                    // Navegar a pantalla de verificación
                                                    navigateToVerificacionCorreo(correo, usuario)
                                                } else {
                                                    errorMessage = "Error al enviar correo de verificación"
                                                    // Eliminar usuario si no se pudo enviar el correo
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
                        .width(350.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = boton_iniciar)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = white, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creando cuenta...", color = white)
                    } else {
                        Text("CREAR CUENTA", fontWeight = FontWeight.Bold, color = white)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = { navigatetoinicialScreen() }) {
                    Text("Volver", color = Color.Gray)
                }
            }
        }
    }
}