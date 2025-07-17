package com.santiago.sindesparches.presentation.perfil

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.placeholder
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.presentation.mi_perfil.uploadImageToFirebase
import com.santiago.sindesparches.ui.theme.Purple
import com.santiago.sindesparches.ui.theme.azul
import com.santiago.sindesparches.ui.theme.azul_borde
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_iniciar
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.gris
import com.santiago.sindesparches.ui.theme.white
import kotlinx.coroutines.delay

@Composable
fun PerfilScreen(
    usuario: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onLogout: () -> Unit = {},
    navigate_registro_completo: (nombre: String) -> Unit = {},
    navigateToInicial: () -> Unit = {}
) {
    // Colores modernos para eventos
    val primaryBlue = Color(0xFF2196F3)
    val aquaBlue = Color(0xFF00E5FF)
    val neonYellow = Color(0x79FFEB3B)
    val vibrantPurple = Color(0xFF9C27B0)
    val hotPink = Color(0xFFFF1744)
    val darkBackground = Color(0xFF0A0A0A)
    val cardBackground = Color(0xFF1A1A1A)
    val surfaceColor = Color(0xFF2D2D2D)

    // Estados existentes
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val user = FirebaseAuth.getInstance().currentUser
    val nombreUsuario = user?.displayName
    val login_user = usuario
    var nombre by remember { mutableStateOf(nombreUsuario ?: login_user) }
    var celular by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    LaunchedEffect(mensaje) {
        if (mensaje.isNotEmpty()) {
            delay(5000)
            mensaje = ""
        }
    }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var edad by remember { mutableStateOf<Int?>(null) }
    var selectedCiudad by remember { mutableStateOf("seleccionar ciudad") }
    val userId = auth.currentUser?.uid
    var terminosAceptados by remember { mutableStateOf(false) }
    var mostrarTerminos by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    // Launcher para imágenes
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // BackHandler
    BackHandler {
        showDialog = true
    }

    // Diálogo de cierre de sesión moderno
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icono de advertencia con gradiente
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(hotPink, vibrantPurple)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Advertencia",
                            tint = Color.White,
                            modifier = Modifier.size(35.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¿Cerrar sesión?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "¿Estás seguro? Se eliminará tu usuario permanentemente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = aquaBlue,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceColor
                            ),
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                user?.delete()?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("FirebaseAuth", "Usuario eliminado")
                                    }
                                }
                                auth.signOut()
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(hotPink, vibrantPurple)
                                    ),
                                    shape = RoundedCornerShape(25.dp)
                                )
                        ) {
                            Text("Sí, salir", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Diálogo de términos moderno
    if (mostrarTerminos) {
        Dialog(onDismissRequest = { mostrarTerminos = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Términos y Condiciones",
                        style = MaterialTheme.typography.headlineSmall,
                        color = neonYellow,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Text(
                                text = "Al utilizar la aplicación Sindesparches, usted acepta que:\n\n" +
                                        "1. Sindesparches no se hace responsable de la veracidad, exactitud o legitimidad de los planes publicados en la plataforma.\n\n" +
                                        "2. Sindesparches actúa únicamente como intermediario entre usuarios y no garantiza que los eventos o planes se lleven a cabo según lo anunciado.\n\n" +
                                        "3. Sindesparches queda eximido de toda responsabilidad legal por cualquier consecuencia derivada de la participación en actividades organizadas a través de la plataforma.\n\n" +
                                        "4. Los usuarios asumen toda la responsabilidad al participar en los planes publicados.\n\n" +
                                        "5. La información proporcionada será almacenada en nuestra base de datos conforme a nuestra política de privacidad.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { mostrarTerminos = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(aquaBlue, primaryBlue)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Entendido",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Pantalla principal con diseño moderno
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

        Spacer(modifier = Modifier.height(36.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header moderno
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    // Botón de retroceso moderno
                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = boton
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retroceder",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),

                        )
                    }

                    // Título con efecto neón
                    Text(
                        text = "MI PERFIL",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            shadow = Shadow(
                                color = aquaBlue,
                                offset = Offset(0f, 0f),
                                blurRadius = 10f
                            )
                        ),
                        color = Color.White
                    )

                    // Spacer para balance
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }

            // Foto de perfil moderna
            item {
                Card(
                    modifier = Modifier
                        .size(180.dp)
                        .padding(16.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        aquaBlue,
                                        neonYellow,
                                        hotPink,
                                        vibrantPurple,
                                        aquaBlue
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .background(
                                color = cardBackground,
                                shape = CircleShape
                            )
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Agregar foto",
                                    tint = white,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Añadir foto",
                                    color = white,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Campos de formulario modernos
            item {
                Spacer(modifier = Modifier.height(24.dp))

                ModernTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "Nombre",
                    placeholder = "Tu nombre aquí",
                    leadingIcon = Icons.Default.Person,
                    gradientColors = listOf(aquaBlue, primaryBlue)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernTextField(
                    value = celular,
                    onValueChange = { celular = it },
                    label = "Celular",
                    placeholder = "Tu número de celular",
                    leadingIcon = Icons.Default.Phone,
                    gradientColors = listOf(hotPink, vibrantPurple),
                    keyboardType = KeyboardType.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dropdowns modernos
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ModernEdadDropdown(
                        selectedEdad = edad,
                        onEdadSelected = { edad = it },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    ModernCiudadDropdown(
                        selectedCiudad = selectedCiudad,
                        onCiudadSelected = { selectedCiudad = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Términos y condiciones modernos
            item {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = terminosAceptados,
                            onCheckedChange = { terminosAceptados = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = neonYellow,
                                uncheckedColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.clickable { mostrarTerminos = true }
                        ) {
                            Text(
                                text = "Acepto los términos y condiciones",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toca aquí para leer términos completos",
                                color = aquaBlue,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // Mensaje de error
            item {
                if (mensaje.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mensaje.contains("Error") || mensaje.contains("Debes"))
                                Color.Red.copy(alpha = 0.2f)
                            else
                                Color.Green.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = mensaje,
                            color = if (mensaje.contains("Error") || mensaje.contains("Debes"))
                                Color.Red
                            else
                                Color.Green,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Botón de guardar épico
            item {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        // Validaciones existentes
                        if (!terminosAceptados) {
                            mensaje = "Debes aceptar los términos y condiciones"
                            return@Button
                        }

                        if (imageUri == null) {
                            mensaje = "Por favor selecciona una imagen"
                            return@Button
                        }

                        if (nombre.isEmpty()) {
                            mensaje = "Por favor ingresa un nombre"
                            return@Button
                        }

                        if (celular.isEmpty()) {
                            mensaje = "Por favor ingresa un número de celular"
                            return@Button
                        }

                        if (celular.length != 10) {
                            mensaje = "Por favor ingresa un número de celular válido"
                            return@Button
                        }

                        if (edad == null) {
                            mensaje = "Por favor selecciona una edad"
                            return@Button
                        }

                        if (selectedCiudad == "seleccionar ciudad") {
                            mensaje = "Por favor selecciona una ciudad"
                            return@Button
                        }

                        // Procesar datos
                        imageUri?.let { uri ->
                            userId?.let { uid ->
                                uploadImageToFirebase(uri, uid) { url ->
                                    imageUrl = url
                                }
                            }
                        }

                        guardarPerfilEnFirestore(
                            userId, nombre, celular, edad!!, selectedCiudad, terminosAceptados
                        ) { success ->
                            if (success) {
                                mensaje = "¡Perfil guardado! ¡Listo para la diversión!"
                                navigate_registro_completo(nombre)
                            } else {
                                mensaje = "Error al guardar el perfil"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(30.dp),
                    enabled = nombre.isNotEmpty()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(boton_texto, shape = RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GUARDAR Y EMPEZAR A DISFRUTAR",
                                color = black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Componente de TextField moderno
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    gradientColors: List<Color>,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xA62D2D2D)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                color = gradientColors[0],
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = gradientColors[0]
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gradientColors[0],
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = gradientColors[0]
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Dropdown moderno para edad
@Composable
fun ModernEdadDropdown(
    selectedEdad: Int?,
    onEdadSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Edad",
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF6B35).copy(alpha = 0.2f),
                                Color(0xFFE91E63).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedEdad?.toString() ?: "Seleccionar",
                        color = if (selectedEdad != null) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF2D2D2D))
                    .heightIn(max = 200.dp)
            ) {
                (15..60).forEach { edad ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = edad.toString(),
                                color = Color.White
                            )
                        },
                        onClick = {
                            onEdadSelected(edad)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Dropdown moderno para ciudad
@Composable
fun ModernCiudadDropdown(
    selectedCiudad: String,
    onCiudadSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ciudades = listOf("Garzon", "Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena")
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ciudad",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.2f),
                                Color(0xFF2196F3).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCiudad == "seleccionar ciudad") "Seleccionar" else selectedCiudad,
                        color = if (selectedCiudad == "seleccionar ciudad") Color.Gray else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2D2D2D))
            ) {
                ciudades.forEach { ciudad ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = ciudad,
                                color = Color.White
                            )
                        },
                        onClick = {
                            onCiudadSelected(ciudad)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
// Función actualizada para guardar el perfil con términos y condiciones
fun guardarPerfilEnFirestore(
    userId: String?,
    nombre: String,
    celular: String,
    edad: Int,
    ciudad: String,
    terminosAceptados: Boolean,
    onResult: (Boolean) -> Unit
) {
    if (userId == null) {
        Log.e("Firestore", "Usuario no autenticado")
        onResult(false)
        return
    }

    val db = FirebaseFirestore.getInstance()
    val perfilData = hashMapOf(
        "nombre" to nombre,
        "celular" to celular,
        "edad" to edad,
        "ciudad" to ciudad,
        "terminosAceptados" to terminosAceptados,
        "fechaAceptacionTerminos" to FieldValue.serverTimestamp()
    )

    db.collection("perfil").document(userId)
        .set(perfilData)
        .addOnSuccessListener {
            Log.d("Firestore", "Perfil guardado correctamente")
            onResult(true)
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error al guardar perfil", e)
            onResult(false)
        }
}


