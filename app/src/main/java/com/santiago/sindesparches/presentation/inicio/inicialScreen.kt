package com.santiago.sindesparches.presentation.inicio

import android.util.Log
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    navigatePerfil: () -> Unit = {},
    navigateToVerificacionCorreo: (String, String) -> Unit = { _, _ -> },
    navigateToDefinirContrasena: (String) -> Unit = {}
) {
    var exitDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var forgotPasswordDialog by remember { mutableStateOf(false) }
    var resetPasswordEmail by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf("") }

    // Nuevos estados para la validación de email
    var emailVerificationDialog by remember { mutableStateOf(false) }
    var unverifiedUserEmail by remember { mutableStateOf("") }
    var unverifiedUserName by remember { mutableStateOf("") }

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

    //colores dialog
    val primaryBlue = Color(0xFF2196F3)
    val aquaBlue = Color(0xFF00BCD4)
    val neonYellow = Color(0xFFFFEB3B)
    val vibrantPurple = Color(0xFF9C27B0)
    val hotPink = Color(0xFFE91E63)
    val darkBackground = Color(0xFF1A1A1A)
    val cardBackground = Color(0xFF2D2D2D)

    // Función para procesar el usuario después de la autenticación
    // Función para procesar el usuario después de la autenticación
    fun processUserAfterAuth(user: FirebaseUser?) {
        // Verificar si el usuario es nulo
        if (user == null) {
            errorMessage = "Error: No se pudo obtener el usuario"
            isLoading = false
            return
        }

        // Verificar si el usuario se autenticó con proveedores externos (Facebook/Google)
        val isExternalProvider = user.providerData.any { providerInfo ->
            providerInfo.providerId == "facebook.com" || providerInfo.providerId == "google.com"
        }

        // Solo verificar email para usuarios que se registraron con email/contraseña
        if (!isExternalProvider && !user.isEmailVerified) {
            Log.d("Authentication", "Email no verificado para usuario: ${user.email}")

            // Obtener información del usuario para la pantalla de verificación
            unverifiedUserEmail = user.email ?: ""

            // Intentar obtener el nombre del usuario desde Firestore
            val uid = user.uid
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    unverifiedUserName = if (document.exists()) {
                        document.getString("nombre") ?: user.displayName ?: "Usuario"
                    } else {
                        user.displayName ?: "Usuario"
                    }

                    isLoading = false
                    emailVerificationDialog = true
                }
                .addOnFailureListener {
                    // Si no se puede obtener el nombre, usar valores por defecto
                    unverifiedUserName = user.displayName ?: "Usuario"
                    isLoading = false
                    emailVerificationDialog = true
                }
            return
        }

        val uid = user.uid
        val userEmail = user.email ?: ""

        // 1. Verificar si tiene contraseña (solo para usuarios de email/contraseña)
        val tieneContraseña = user.providerData.any { it.providerId == "password" }

        // Para usuarios externos (Facebook/Google), no necesitan definir contraseña
        if (!isExternalProvider && !tieneContraseña) {
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

    //launcher para verificar si el usuario ya habia inicado secion antes
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            isLoading = true
            processUserAfterAuth(user)
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

        // NUEVO: Diálogo para email no verificado
        if (emailVerificationDialog) {
            Dialog(
                onDismissRequest = {
                    emailVerificationDialog = false
                    // Cerrar sesión del usuario no verificado
                    auth.signOut()
                }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono de advertencia llamativo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Red,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Email no verificado",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Título principal
                        Text(
                            text = "¡Email no verificado!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = neonYellow,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mensaje informativo
                        Text(
                            text = "Necesitas verificar tu correo electrónico antes de continuar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email del usuario
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            primaryBlue.copy(alpha = 0.3f),
                                            aquaBlue.copy(alpha = 0.3f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = unverifiedUserEmail,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón principal - Ir a verificación
                        Button(
                            onClick = {
                                emailVerificationDialog = false
                                navigateToVerificacionCorreo(unverifiedUserEmail, unverifiedUserName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(aquaBlue, primaryBlue, vibrantPurple)
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Verificar Email Ahora",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botón secundario - Cancelar
                        OutlinedButton(
                            onClick = {
                                emailVerificationDialog = false
                                auth.signOut() // Cerrar sesión del usuario no verificado
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(2.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "Cancelar",
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Texto informativo adicional
                        Text(
                            text = "💡 Revisa tu bandeja de entrada y carpeta de spam",
                            style = MaterialTheme.typography.bodySmall,
                            color = aquaBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Diálogo para restablecer contraseña - Diseño moderno y llamativo
        if (forgotPasswordDialog) {
            Dialog(
                onDismissRequest = { forgotPasswordDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono llamativo con gradiente
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(hotPink, vibrantPurple, primaryBlue),
                                        radius = 100f
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Restablecer contraseña",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Título principal
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtítulo con degradado
                        Text(
                            text = "¡No te preocupes! Te ayudamos a recuperarla",
                            style = MaterialTheme.typography.bodyMedium,
                            color = aquaBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Campo de email con diseño moderno
                        OutlinedTextField(
                            value = resetPasswordEmail,
                            onValueChange = { resetPasswordEmail = it },
                            label = {
                                Text(
                                    "Correo electrónico",
                                    color = aquaBlue
                                )
                            },
                            placeholder = {
                                Text(
                                    "ejemplo@correo.com",
                                    color = Color.Gray
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = aquaBlue
                                )
                            },
                            trailingIcon = {
                                if (resetPasswordEmail.isNotEmpty()) {
                                    IconButton(
                                        onClick = { resetPasswordEmail = "" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = aquaBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = aquaBlue
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botón principal con gradiente
                        Button(
                            onClick = {
                                if (resetPasswordEmail.isNotEmpty() &&
                                    Patterns.EMAIL_ADDRESS.matcher(resetPasswordEmail).matches()) {

                                    // Primero verificamos si el correo existe
                                    auth.fetchSignInMethodsForEmail(resetPasswordEmail)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val signInMethods = task.result?.signInMethods

                                                if (signInMethods.isNullOrEmpty()) {
                                                    // El correo no está registrado
                                                    errorMessage = "❌ Este correo no está registrado."
                                                    forgotPasswordDialog = false
                                                } else {
                                                    // El correo existe, enviamos el email de restablecimiento
                                                    auth.sendPasswordResetEmail(resetPasswordEmail)
                                                        .addOnCompleteListener { resetTask ->
                                                            if (resetTask.isSuccessful) {
                                                                errorMessage = "✅ ¡Correo enviado! Revisa tu bandeja de entrada"
                                                            } else {
                                                                errorMessage = "❌ Error al enviar correo: ${resetTask.exception?.message}"
                                                            }
                                                            forgotPasswordDialog = false
                                                        }
                                                }
                                            } else {
                                                // Error al verificar el correo
                                                errorMessage = "❌ Error al verificar el correo: ${task.exception?.message}"
                                                forgotPasswordDialog = false
                                            }
                                        }
                                } else {
                                    errorMessage = "⚠️ Por favor, ingresa un correo válido"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(hotPink, vibrantPurple, primaryBlue)
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Enviar correo",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón cancelar moderno
                        TextButton(
                            onClick = { forgotPasswordDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancelar",
                                color = aquaBlue,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Texto de ayuda adicional
                        Text(
                            text = "¿No recibes el correo? Revisa tu carpeta de spam",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))
        Image(
            painter = painterResource(id = R.drawable.iniciosesion), contentDescription = "inicio",
            modifier = Modifier
                .height(150.dp)
                .width(300.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Text(
            "INICIAR SESIÓN",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 42.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )

        Spacer(modifier = Modifier.weight(0.2f))

        Column(
            modifier = Modifier
                .height(140.dp)
                .width(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                                // MODIFICACIÓN: Usar la función processUserAfterAuth que ya incluye la validación de email
                                processUserAfterAuth(auth.currentUser)
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
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
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
                        painter = painterResource(R.drawable.bxl_google ),
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