package com.santiago.sindesparches.presentation.registro_completo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.white

@Composable


fun registro_completo(auth: FirebaseAuth,
                      nombre: String,
                      navigateToHome: ()-> Unit = {},
                      navigateToLoging: ()-> Unit = {}){
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.registro))
    var showDialog by remember { mutableStateOf(false) }
    val aquaBlue = Color(0xFF00E5FF)
    val neonYellow = Color(0xFFFFEB3B)
    val darkBackground = Color(0xFF0A0A0A)
    val hotPink = Color(0xFFFF1744)

    // Intercepta el botón de retroceso
    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("¿Estás seguro de que quieres salir?") },
            confirmButton = {
                Button(onClick = {
                    auth.signOut() // Cierra la sesión
                    navigateToLoging() // Navega a la pantalla de login
                }) {
                    Text("Sí, salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
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
        // Elementos decorativos de fondo
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Círculos decorativos
            drawCircle(
                color = aquaBlue.copy(alpha = 0.1f),
                radius = 150f,
                center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
            drawCircle(
                color = hotPink.copy(alpha = 0.1f),
                radius = 100f,
                center = Offset(size.width * 0.2f, size.height * 0.7f)
            )
            drawCircle(
                color = neonYellow.copy(alpha = 0.1f),
                radius = 80f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .width(320.dp)
                    .height(420.dp)
                    .background(color = boton_texto, shape = RoundedCornerShape(20.dp))
                    .border(2.dp, color = white, shape = RoundedCornerShape(20.dp)),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Spacer(modifier = Modifier.weight(0.2f))

                Text(
                    text = "Bienvenido \n \n $nombre",
                    color = white,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(200.dp)
                )

                Text(
                    text = "Tu perfil se a creado con exito, espero encuentres tu plan y recuerda  vive sin desparche",
                    color = white,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = 20.dp
                    )
                )
                Spacer(modifier = Modifier.weight(0.2f))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Button(
                onClick = { navigateToHome() },
                modifier = Modifier
                    .width(350.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = azul_final)
            ) {
                Text(text = "aceptar", color = white)

            }
            Spacer(modifier = Modifier.weight(1f))

        }
    }

}