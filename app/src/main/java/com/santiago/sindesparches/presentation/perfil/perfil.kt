package com.santiago.sindesparches.presentation.perfil

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    // Colores vibrantes pero equilibrados para app de eventos
    val primaryColor = Color(0xFF6366F1) // Indigo vibrante
    val accentColor = Color(0xFF06B6D4) // Cyan brillante
    val secondaryAccent = Color(0xFFEC4899) // Rosa vibrante
    val tertiaryAccent = Color(0xFFF59E0B) // Amarillo dorado
    val errorColor = Color(0xFFEF4444)
    val successColor = Color(0xFF10B981)
    val darkBackground = Color(0xFF0F172A) // Azul muy oscuro
    val cardBackground = Color(0xFF1E293B) // Azul grisáceo oscuro
    val surfaceColor = Color(0xFF334155) // Gris azulado
    val textSecondary = Color(0xFF94A3B8)

    // Estados
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

    // Verificar si todos los campos están completos
    val todosLosCamposCompletos = imageUri != null &&
            nombre.isNotEmpty() &&
            celular.length == 10 &&
            edad != null &&
            selectedCiudad != "seleccionar ciudad" &&
            terminosAceptados

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

    // Diálogo de cierre de sesión
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
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Advertencia",
                        tint = errorColor,
                        modifier = Modifier.size(48.dp)
                    )

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
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { showDialog = false },
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
                                containerColor = errorColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sí, salir", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Diálogo de términos
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
                        color = primaryColor,
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
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
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

    // Pantalla principal con mejor distribución
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(top = 10.dp)
        ) {
            // Header compacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retroceder",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "MI PERFIL",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Foto de perfil con borde degradado
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor,
                                accentColor,
                                secondaryAccent,
                                tertiaryAccent,
                                primaryColor
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { launcher.launch("image/*") },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                    tint = accentColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Añadir foto",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Campos de formulario con colores vibrantes
            ModernTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Nombre",
                placeholder = "Tu nombre aquí" ,
                leadingIcon = Icons.Default.Person,
                accentColor = primaryColor,

            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernTextField(
                value = celular,
                onValueChange = { celular = it },
                label = "Celular",
                placeholder = "Tu número de celular",
                leadingIcon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone,
                accentColor = secondaryAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdowns en fila para ahorrar espacio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernEdadDropdown(
                    selectedEdad = edad,
                    onEdadSelected = { edad = it },
                    modifier = Modifier.weight(1f)
                )

                ModernCiudadDropdown(
                    selectedCiudad = selectedCiudad,
                    onCiudadSelected = { selectedCiudad = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Términos y condiciones con estilo vibrante
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(16.dp)
                        .clickable { mostrarTerminos = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = terminosAceptados,
                        onCheckedChange = { terminosAceptados = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = tertiaryAccent,
                            uncheckedColor = textSecondary,
                            checkmarkColor = Color.Black
                        )
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Acepto los términos y condiciones",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Toca para leer términos completos",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Mensaje de error/éxito
            if (mensaje.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mensaje.contains("Error") || mensaje.contains("Debes"))
                            errorColor.copy(alpha = 0.2f)
                        else
                            successColor.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = mensaje,
                        color = if (mensaje.contains("Error") || mensaje.contains("Debes"))
                            errorColor
                        else
                            successColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón dinámico con estados visuales atractivos
            Button(
                onClick = {
                    // Validaciones
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
                            mensaje = "¡Perfil guardado exitosamente!"
                            navigate_registro_completo(nombre)
                        } else {
                            mensaje = "Error al guardar el perfil"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (todosLosCamposCompletos) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        successColor,
                                        Color(0xFF059669), // Verde más intenso
                                        accentColor
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        surfaceColor,
                                        surfaceColor.copy(alpha = 0.8f)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (todosLosCamposCompletos) {
                            // Icono animado cuando está listo
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = if (todosLosCamposCompletos)
                                "¡COMENZAR AVENTURA!"
                            else
                                "COMPLETA TUS DATOS",
                            color = if (todosLosCamposCompletos) Color.White else textSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// TextField moderno con colores personalizables
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    accentColor: Color,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val textSecondary = Color(0xFF94A3B8)
    val cardBackground = Color(0xFF1E293B)

    Column {
        Text(
            text = label,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBackground
            )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (value.isNotEmpty()) accentColor else textSecondary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = accentColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.Gray,

                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Dropdown de edad compacto
@Composable
fun ModernEdadDropdown(
    selectedEdad: Int?,
    onEdadSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF3F51B5)
    val textSecondary = Color(0xFFB0B0B0)

    Column(modifier = modifier) {
        Text(
            text = "Edad",
            color = primaryColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedEdad?.toString() ?: "Edad",
                        color = if (selectedEdad != null) Color.White else textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = primaryColor
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF2A2A2A))
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

// Dropdown de ciudad compacto
@Composable
fun ModernCiudadDropdown(
    selectedCiudad: String,
    onCiudadSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ciudades = listOf("Garzon", "Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena")
    var expanded by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF3F51B5)
    val textSecondary = Color(0xFFB0B0B0)

    Column(modifier = modifier) {
        Text(
            text = "Ciudad",
            color = primaryColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCiudad == "seleccionar ciudad") "Ciudad" else selectedCiudad,
                        color = if (selectedCiudad == "seleccionar ciudad") textSecondary else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = primaryColor
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
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

// Función para guardar perfil (sin cambios)
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