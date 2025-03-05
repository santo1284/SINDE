package com.santiago.sindesparches.presentation.inicio

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.santiago.sindesparches.ui.theme.facebook
import com.santiago.sindesparches.ui.theme.gmail
import com.santiago.sindesparches.ui.theme.white
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@Composable

fun InicialScreen(auth: FirebaseAuth,
                  navigateToLoging: () -> Unit = {},
                  navigatehome: () -> Unit = {}){

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null)}


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(0.8f))
        Image(
            painter = painterResource(id = R.drawable.iniciosesion), contentDescription = "inicio",
            modifier = Modifier
                .height(150.dp)
                .width(300.dp)
        )
        Text(
            "INICIAR SESIÓN", color = Color.White, fontSize = 30.sp,
            textAlign = TextAlign.Center, fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.weight(0.2f))

        Column (modifier = Modifier
                .height(140.dp)
                .width(260.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("USUARIO", color = white) },
                singleLine = true,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.bxs_user),
                        contentDescription = "",
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
                shape = RoundedCornerShape(40.dp)
            )

            Spacer(modifier = Modifier.weight(0.2f))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("CONTRASEÑA", color = white) },
                singleLine = true,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.bxs_lock),
                        contentDescription = "",
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
                shape = RoundedCornerShape(40.dp)

            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                onClick = { }, modifier = Modifier
                   , colors = ButtonDefaults.buttonColors(containerColor = boton)
            ) {
                Text(
                    text = "olvide mi contraseña",
                    color = white,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        Button(
            onClick = {
                if(email.isBlank() || password.isBlank()) {//mensaje de error por si los campos estan vacios
                    errorMessage = "correo o contraseña vacios"
                }else {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            //navegar una vez la persona este iniciada
                            Log.i("santi login", "correcto")
                            navigatehome()
                        } else {
                            //mostrar un mensaje de error
                            errorMessage = "correo o contraseña incorrectos"

                            Log.i("santi login", "incorrecto")
                        }


                    }
                }

            },
            modifier = Modifier
                .width(150.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = boton_iniciar)
        ) {
            Text(
                text = "INICIAR SESIÓN",
                color = white,
                fontWeight = FontWeight.Normal

            )
        }
        errorMessage?.let{
            Text(text = it, color = boton_texto)
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Row(
            modifier = Modifier, horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(Modifier
                .background(color = white)
                .width(100.dp)
                .height(2.dp))
            Text(
                text = "INICIAR CON",
                color = white,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier
                .background(color = white)
                .width(100.dp)
                .height(2.dp))
        }

        Spacer(modifier = Modifier.weight(0.2f))

        Row(
            modifier = Modifier, horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { }, modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(160.dp)
                    .height(50.dp)
                    .border(1.dp, color = white, shape = RoundedCornerShape(30.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = boton),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bxl_facebook),
                        contentDescription = "",
                        tint = facebook,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                    Text(
                        text = "FACEBOOK",
                        color = white,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            Button(
                onClick = { }, modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(150.dp)
                    .height(50.dp)
                    .border(1.dp, color = white, shape = RoundedCornerShape(30.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = boton)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bxl_gmail),
                        contentDescription = "",
                        tint = gmail,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                    Text(
                        text = "GMAIL",
                        color = white,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        TextButton(
            onClick = { navigateToLoging() }, modifier = Modifier
                .padding(15.dp), colors = ButtonDefaults.buttonColors(containerColor = boton)
        ) {
            Text(
                text = "CREAR CUENTA",
                color = white,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.weight(0.8f))


    }
}