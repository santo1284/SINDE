package com.santiago.sindesparches.presentation.estado_registro

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.white

@Composable
fun estado_registro(auth: FirebaseAuth, navigateToperfil: () -> Unit = {} ,usuario:String, onLogout: () -> Unit = {}){

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.succes))

    var showDialog by remember { mutableStateOf(false) }
    // Intercepta el botón de retroceso
    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("¿Estás seguro de que quieres salir? se eliminara tu usuario") },
            confirmButton = {
                Button(onClick = {
                    val user = FirebaseAuth.getInstance().currentUser

                    user?.delete()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FirebaseAuth", "Usuario eliminado correctamente")
                            } else {
                                Log.e("FirebaseAuth", "Error al eliminar usuario", task.exception)
                            }
                        }
                    auth.signOut() // Cierra la sesión
                    onLogout() // Navega a la pantalla de login
                }) {
                    Text("Sí, salir")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .width(280.dp)
                .height(350.dp)
                .background(color = boton_texto, shape = RoundedCornerShape(20.dp))
                .border(2.dp, color = white, shape = RoundedCornerShape(20.dp)),
            horizontalAlignment = Alignment.CenterHorizontally

        ){
            Spacer(modifier = Modifier.weight(0.2f))

            Text(
                text = "Hola $usuario",
                color = white,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(150.dp)
            )
            Text(
                text = "Su registro fue exitoso, comenzaremos a configurar tu perfil, no te preocupes solo es un paso mas ",
                color = white,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = 20.dp
                )
            )
            Spacer(modifier = Modifier.weight(0.2f))
        }

        Spacer(modifier = Modifier.weight(0.5f))

        Button(
            onClick = { navigateToperfil() },
            modifier = Modifier
                .width(150.dp)
                .height(50.dp)
        ) {
            Text(text = "aceptar")

        }
        Spacer(modifier = Modifier.weight(1f))

    }
}



