package com.santiago.sindesparches.presentation.loging


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_iniciar
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.white

@Composable

fun logingScreen(auth: FirebaseAuth,
                 navigatetoinicialScreen: () -> Unit = {},
                 navigateToEstadoRegistro: (String) -> Unit = {}) {

    var usuario by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password_registro by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .align(Alignment.Start)
            ) {
                TextButton(
                    onClick = { navigatetoinicialScreen() },
                    modifier = Modifier
                        .padding(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = boton)
                ) {
                    Icon(
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp),
                        painter = painterResource(id = R.drawable.bx_arrow_back),
                        contentDescription = "",
                        tint = white,
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.crear_cuenta),
                contentDescription = "inicio",
                modifier = Modifier
                    .height(170.dp)
                    .width(350.dp)
            )
            Text(
                text = "Crear Cuenta",
                fontSize = 50.sp,
                color = Color.White,
                modifier = Modifier

            )
            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                placeholder = { Text("USUARIO", color = black) },
                singleLine = true,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.bxs_user),
                        contentDescription = "",
                        tint = if (usuario.isEmpty()) black else white
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = boton,
                    unfocusedContainerColor = boton_texto,
                    unfocusedIndicatorColor = boton,
                    focusedIndicatorColor = white,
                    focusedTextColor = white,
                    unfocusedTextColor = white,
                    cursorColor = white,
                ),
                shape = RoundedCornerShape(40.dp)
            )
            Spacer(modifier = Modifier.weight(0.25f))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                placeholder = { Text("CORREO", color = black) },
                singleLine = true,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.bxs_envelope),
                        contentDescription = "",
                        tint = if (correo.isEmpty()) black else white
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = boton,
                    unfocusedContainerColor = boton_texto,
                    unfocusedIndicatorColor = boton,
                    focusedIndicatorColor = white,
                    focusedTextColor = white,
                    unfocusedTextColor = white,
                    cursorColor = white
                ),
                shape = RoundedCornerShape(40.dp)

            )

            Spacer(modifier = Modifier.weight(0.25f))

            OutlinedTextField(
                value = password_registro,
                onValueChange = { password_registro = it },
                placeholder = { Text("CONTRASEÑA", color = black) },
                singleLine = true,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                ),
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.bxs_lock),
                        contentDescription = "",
                        tint = if (password_registro.isEmpty()) black else white
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = boton,
                    unfocusedContainerColor = boton_texto,
                    unfocusedIndicatorColor = boton,
                    focusedIndicatorColor = white,
                    focusedTextColor = white,
                    unfocusedTextColor = white,
                    cursorColor = white
                ),
                shape = RoundedCornerShape(40.dp)

            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
                Text(
                    text = if (checked) "aceptaste terminos y condiciones"
                    else "acepta terminos y condiciones",
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 8.dp),
                    color = white
                )
            }
            errorMessage?.let {
                Text(text = it, color = boton_texto)
            }

            Button(
                onClick = {
                    if (correo.isBlank() || password_registro.isBlank() || usuario.isBlank()) {
                        errorMessage = "se encuentran campos vacios"
                    } else if (!checked) {
                        errorMessage = "acepta los terminos y condiciones"
                    } else {

                        //auth.signInAnonymously() //forma para que las personas no se tengan que resgistrarce cada vez que salgan de la app
                        auth.createUserWithEmailAndPassword(correo, password_registro)
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {
                                    //navegar una vez la persona este iniciada
                                    Log.i("santi login", "registro correcto")
                                    navigateToEstadoRegistro(usuario)

                                } else {
                                    Log.i("santi login", "registro incorrecto")
                                    errorMessage = "usuaio no admitido"

                                }
                            }
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = boton_texto)
            ) {
                Text(
                    text = "REGISTRAR",
                    color = black,
                    fontWeight = FontWeight.Normal

                )
            }

            Spacer(modifier = Modifier.weight(2f))
        }
    }





