package com.santiago.sindesparches.presentation.contraseña

import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.collection.intIntMapOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
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
    val user = FirebaseAuth.getInstance().currentUser
    val nombreUsuario = user?.displayName


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
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final)))
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center


    ) {
        Text(
            "Definir Contraseña",
            fontSize = 40.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(modifier = Modifier.height(20.dp)

        )

        Column(
            modifier = Modifier
                .width(325.dp)
                .height(270.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(azul, azul_mitad, azul_final),
                        start = Offset(Float.POSITIVE_INFINITY, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                    , shape = RoundedCornerShape(50.dp,10.dp,50.dp,10.dp))
                .border(1.5.dp, Color.Gray, shape = RoundedCornerShape(50.dp,10.dp,50.dp,10.dp))
                ,horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {

            Text(
                "$nombreUsuario \n ingresa tu nueva contraseña",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Nueva Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = boton,
                    unfocusedContainerColor = boton,
                    unfocusedIndicatorColor = white,
                    focusedIndicatorColor = boton_texto,
                    focusedTextColor = white,
                    unfocusedTextColor = white,
                    cursorColor = white,
                    focusedLabelColor = white,
                    unfocusedLabelColor = gris
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
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
                            errorMessage =
                                "Error al vincular cuenta: ${task.exception?.localizedMessage}"
                        }
                    }
            },
                colors = ButtonDefaults.buttonColors(containerColor = boton_texto),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("Guardar Contraseña", color = black)
            }

        }

        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(5.dp),
            contentAlignment = Alignment.Center

        ) {

            errorMessage?.let {

                Text(
                    text = it,
                    fontSize = 18.sp,
                    color = Color.Red,
                )
            }
        }
        // Efecto para limpiar el mensaje de error después de un tiempo
        LaunchedEffect(errorMessage) {
            if (errorMessage != null) {
                delay(5000) // 5 segundos
                errorMessage = null
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}



