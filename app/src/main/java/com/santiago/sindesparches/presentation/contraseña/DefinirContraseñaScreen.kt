package com.santiago.sindesparches.presentation.contraseña

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DefinirContraseñaScreen(email: String, auth: FirebaseAuth, onLogout: () -> Unit, onPasswordDefined: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val user = FirebaseAuth.getInstance().currentUser

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tu correo: $email", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Nueva Contraseña") },
            visualTransformation = PasswordVisualTransformation()
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
                        errorMessage = "Error al vincular cuenta: ${task.exception?.localizedMessage}"
                    }
                }
        }) {
            Text("Guardar Contraseña")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color.Red)
        }
    }
}
