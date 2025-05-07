package com.santiago.sindesparches.presentation.inicio

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.delay
import kotlin.system.exitProcess

@Composable
fun InicialScreen(
    navController: NavController,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateToLoging: () -> Unit = {},
    navigatehome: () -> Unit = {},
    navigatePerfil: () -> Unit = {}
) {
    var exitDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var forgotPasswordDialog by remember { mutableStateOf(false) }
    var resetPasswordEmail by remember { mutableStateOf("") }

    // Configurar Manager de Facebook
    val callbackManager = remember { CallbackManager.Factory.create() }

    // Contexto
    val context = LocalContext.current

    // Función para redirigir a la pantalla de definir contraseña
    val navigateToDefinirContrasena: (String) -> Unit = { userEmail ->
        navController.navigate("definir_contrasena/$userEmail")
    }

    // Configurar Google Sign In
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("1022521936843-kmoousfn5s5ieohtapq7tolgjabgugm4.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    // Efecto para limpiar el mensaje de error después de un tiempo
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(5000) // 5 segundos
            errorMessage = null
        }
    }

    // Función para procesar el usuario después de la autenticación
    // IMPORTANTE: Ahora es una función de nivel superior dentro del composable
    fun processUserAfterAuth(user: FirebaseUser?) {
        // Verificar si el usuario es nulo
        if (user == null) {
            errorMessage = "Error: No se pudo obtener el usuario"
            isLoading = false
            return
        }

        val uid = user.uid
        val userEmail = user.email ?: ""

        // 1. Verificar si tiene contraseña
        val tieneContraseña = user.providerData.any { it.providerId == "password" }

        if (!tieneContraseña) {
            Log.d("Authentication", "Usuario sin contraseña. Redirigiendo a DefinirContraseña")
            isLoading = false
            navigateToDefinirContrasena(userEmail)
            return
        }

        // 2. Verificar si el usuario existe en la colección "usuarios" (datos básicos)
        // y en la colección "perfil" (datos completos)

        db.collection("perfil").document(uid).get()
            .addOnSuccessListener { perfilDocument ->
                if (perfilDocument.exists() && perfilDocument.data?.isNotEmpty() == true) {
                    // El usuario tiene perfil completo, navegar a Home

                    Log.d("Navigation", "Perfil completo encontrado. Navegando a Home")
                    isLoading = false
                    navigatehome()
                } else {
                    // El usuario no tiene perfil completo, navegar a PerfilScreen

                    Log.d("Navigation", "Perfil no encontrado o incompleto. Navegando a Perfil")
                    isLoading = false
                    navigatePerfil()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al verificar datos del usuario: ${exception.message}", exception)
                errorMessage = "Error al acceder a los datos del perfil"
                isLoading = false
            }
    }

    // Función para manejar el token de Facebook
    fun handleFacebookAccessToken(token: AccessToken) {
        isLoading = true
        val credential = FacebookAuthProvider.getCredential(token.token)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    processUserAfterAuth(auth.currentUser)
                } else {
                    Log.e("FacebookAuth", "Error en autenticación con Facebook", task.exception)
                    errorMessage = "Error al iniciar sesión con Facebook: ${task.exception?.message}"
                    isLoading = false
                }
            }
    }

    // Lanzador para Google Sign In
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        processUserAfterAuth(auth.currentUser)
                    } else {
                        errorMessage = "Error al iniciar sesión con Google"
                        isLoading = false
                    }
                }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Error al obtener cuenta de Google", e)
            errorMessage = "Error al acceder a la cuenta de Google"
            isLoading = false
        }
    }

    // Configurar el callback de Facebook antes de la UI
    LaunchedEffect(callbackManager) {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("FacebookAuth", "Login success")
                handleFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                Log.d("FacebookAuth", "Login cancelled")
                errorMessage = "Inicio de sesión con Facebook cancelado"
            }

            override fun onError(error: FacebookException) {
                Log.e("FacebookAuth", "Login error", error)
                errorMessage = "Error al iniciar sesión con Facebook: ${error.message}"
            }
        })
    }

    // Manejar el botón de atrás
    BackHandler {
        exitDialog = true
    }

    // UI principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Diálogo de confirmación para salir
        if (exitDialog) {
            AlertDialog(
                onDismissRequest = { exitDialog = false },
                title = { Text("Salir de la app") },
                text = { Text("¿Estás seguro de que quieres salir?") },
                confirmButton = {
                    Button(onClick = { exitProcess(0) }) {
                        Text("Salir")
                    }
                },
                dismissButton = {
                    Button(onClick = { exitDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo para restablecer contraseña
        if (forgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { forgotPasswordDialog = false },
                title = { Text("Restablecer contraseña") },
                text = {
                    Column{
                        Text("Ingresa tu correo electrónico para recibir instrucciones")
                        OutlinedTextField(
                            value = resetPasswordEmail,
                            onValueChange = { resetPasswordEmail = it },
                            placeholder = { Text("Correo") },
                            singleLine = true,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(colors = ButtonDefaults.buttonColors(containerColor = boton_iniciar),
                        onClick = {
                        if (resetPasswordEmail.isNotEmpty()) {
                            auth.sendPasswordResetEmail(resetPasswordEmail)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        errorMessage = "Se ha enviado un correo para restablecer tu contraseña"
                                    } else {
                                        errorMessage = "Error al enviar correo: ${task.exception?.message}"
                                    }
                                    forgotPasswordDialog = false
                                }
                        } else {
                            errorMessage = "Ingresa un correo válido"
                            forgotPasswordDialog = false
                        }
                    }) {
                        Text("Enviar")
                    }
                },
                dismissButton = {
                    Button(colors = ButtonDefaults.buttonColors(containerColor = boton_iniciar),
                        onClick = { forgotPasswordDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

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

        Column(
            modifier = Modifier
                .height(140.dp)
                .width(260.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            horizontalArrangement = Arrangement.End,
            modifier = Modifier .width(260.dp)
        ) {
            TextButton(
                onClick = { forgotPasswordDialog = true },
                modifier = Modifier,
                colors = ButtonDefaults.buttonColors(containerColor = boton)
            ) {
                Text(
                    text = "olvide mi contraseña",
                    color = white,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Left
                )
            }
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "correo o contraseña vacios"
                } else {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.i("santi login", "Autenticación correcta")
                                // Obtenemos el usuario actual
                                val user = auth.currentUser
                                if (user != null) {
                                    // Verificar si el usuario tiene datos en Firestore
                                    val uid = user.uid
                                    db.collection("perfil").document(uid).get()
                                        .addOnSuccessListener { document ->
                                            isLoading = false
                                            if (document.exists() && document.data?.isNotEmpty() == true) {
                                                // El usuario tiene perfil completo, navegar a Home
                                                Log.i("santi login", "Perfil completo encontrado. Navegando a Home")
                                                navigatehome()
                                            } else {
                                                // El usuario no tiene perfil completo, navegar a PerfilScreen
                                                Log.i("santi login", "Perfil no encontrado. Navegando a Perfil")
                                                navigatePerfil()
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            isLoading = false
                                            errorMessage = "Error al verificar datos del perfil"
                                            Log.e("santi login", "Error al verificar perfil", exception)
                                        }
                                } else {
                                    isLoading = false
                                    errorMessage = "Error al obtener usuario"
                                }
                            } else {
                                isLoading = false
                                errorMessage = "correo o contraseña incorrectos"
                                Log.i("santi login", "Autenticación incorrecta")
                            }
                        }
                }
            },
            modifier = Modifier
                .width(150.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = boton_iniciar),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = white
                )
            } else {
                Text(
                    text = "INICIAR SESIÓN",
                    color = white,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = boton_texto,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                Modifier
                    .background(color = white)
                    .width(100.dp)
                    .height(2.dp)
            )
            Text(
                text = "INICIAR CON",
                color = white,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(
                Modifier
                    .background(color = white)
                    .width(100.dp)
                    .height(2.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.2f))

        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isLoading) return@Button

                    LoginManager.getInstance().logInWithReadPermissions(
                        context as androidx.activity.ComponentActivity,
                        callbackManager,
                        listOf("email", "public_profile")
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(160.dp)
                    .height(50.dp)
                    .border(1.dp, color = white, shape = RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = boton),
                enabled = !isLoading
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
                onClick = {
                    if (isLoading) return@Button

                    val signInIntent = googleSignInClient.signInIntent
                    googleLauncher.launch(signInIntent)
                },
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(150.dp)
                    .height(50.dp)
                    .border(1.dp, color = Color.White, shape = RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = boton),
                enabled = !isLoading
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
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        TextButton(
            onClick = {
                if (!isLoading) navigateToLoging()
            },
            modifier = Modifier.padding(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = boton),
            enabled = !isLoading
        ) {
            Text(
                text = "CREAR CUENTA",
                fontSize = 20.sp,
                color = white,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.weight(0.8f))
    }
}