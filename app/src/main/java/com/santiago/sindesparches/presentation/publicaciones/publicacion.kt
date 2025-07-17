package com.santiago.sindesparches.presentation.publicaciones

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.white
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min


data class Plan(
    val id: String = "",
    val userId: String = "",
    val createdAt: Long = 0,
    val title: String = "",
    val description: String = "",
    val date: Long = 0,
    val timeString: String = "",
    val location: String = "",
    val imageUrls: List<String> = emptyList(),
    val enableWhatsapp: Boolean? = false,  // Nuevo campo para habilitar WhatsApp
    val phoneNumber: String = "",  // Número de teléfono para WhatsApp
    val likes: List<String>? = emptyList(),        // IDs de usuarios que dieron like
    val participants: List<String>? = emptyList(), // IDs de usuarios que participan
    val shares: Int = 0,
    val commentCount: Int = 0
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun publicacion_screen(auth: FirebaseAuth, db: FirebaseFirestore, navigateToHome: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados para los campos del formulario
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // Estado para el DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var formattedDate by remember { mutableStateOf("") }

    // Estado para el TimePicker
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var selectedTime by remember { mutableStateOf("") }

    // Estado para las imágenes
    val selectedImages = remember { mutableStateListOf<Uri>() }
    var isLoading by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Galería de imágenes launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // No sobrepasar el máximo de 5 imágenes
            val remainingSlots = 5 - selectedImages.size
            if (remainingSlots > 0) {
                val newImages = uris.take(remainingSlots)
                selectedImages.addAll(newImages)
            }
        }
    }

    // boton de whatsapp
    var enableWhatsapp by remember { mutableStateOf(false) }
    val currentUserId = auth.currentUser?.uid ?: ""
    var phoneNumber by remember { mutableStateOf("") }
    var isLoadingNumero by remember { mutableStateOf(true) }

    // Cargar número de teléfono del usuario desde Firebase
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val snapshot = db.collection("perfil")
                    .document(currentUserId)
                    .get()
                    .await()

                phoneNumber = snapshot.getString("celular") ?: "No registrado"
            } catch (e: Exception) {
                phoneNumber = "Error al cargar"
                Log.e("UserProfile", "Error loading phone", e)
            } finally {
                isLoadingNumero = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Plan", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (formHasContent(title, description, location, selectedDate, selectedImages)) {
                            showExitDialog = true
                        } else {
                            navigateToHome()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título del plan
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nombre del plan", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color(0xFF8A2BE2),
                    unfocusedIndicatorColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color(0xFF8A2BE2),
                    unfocusedIndicatorColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = { },
                    label = { Text("Fecha", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color(0xFF8A2BE2),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showDatePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2))
                ) {
                    Text("Seleccionar")
                }
            }

            // Hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { },
                    label = { Text("Hora", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color(0xFF8A2BE2),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2))
                ) {
                    Text("Seleccionar")
                }
            }

            // Ubicación
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Ubicación", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color(0xFF8A2BE2),
                    unfocusedIndicatorColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Sección para cargar imágenes
            Text(
                text = "Imágenes (1-5)",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Imágenes seleccionadas
                selectedImages.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen seleccionada $index",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Botón para eliminar imagen
                        IconButton(
                            onClick = { selectedImages.removeAt(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Eliminar imagen",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Botón para agregar más imágenes (solo si hay menos de 5)
                if (selectedImages.size < 5) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                BorderStroke(1.dp, Color.Gray),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar imagen",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            if (selectedImages.isEmpty()) {
                Text(
                    text = "Debes seleccionar al menos una imagen",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Sección de WhatsApp con Card para destacarla
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Título de la sección de WhatsApp
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bxl_whatsapp),
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF25D366) // Color oficial de WhatsApp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contacto por WhatsApp",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Estado de carga del número
                    if (isLoadingNumero) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cargando número de contacto...")
                        }
                    } else {
                        // Número de teléfono
                        Text(
                            text = "Tu número: $phoneNumber",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Checkbox para habilitar WhatsApp
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = enableWhatsapp,
                                onCheckedChange = { enableWhatsapp = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (enableWhatsapp)
                                    "Los usuarios podrán contactarte por WhatsApp"
                                else
                                    "Permitir que los usuarios te contacten por WhatsApp",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Mensaje informativo
                        if (phoneNumber == "No registrado" || phoneNumber == "Error al cargar") {
                            Text(
                                text = "Para habilitar el contacto por WhatsApp, actualiza tu número en tu perfil",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de publicar
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (validateForm(title, description, location, selectedDate, selectedImages, snackbarHostState)) {
                            isLoading = true
                            try {
                                val currentUser = auth.currentUser
                                if (currentUser != null) {
                                    val planId = UUID.randomUUID().toString()
                                    val imageUrls = uploadImagesToFirebase(context, selectedImages, currentUser.uid, planId)

                                    val plan = Plan(
                                        id = planId,
                                        userId = currentUser.uid,
                                        createdAt = System.currentTimeMillis(),
                                        title = title,
                                        description = description,
                                        date = selectedDate ?: 0L,
                                        timeString = selectedTime,
                                        location = location,
                                        imageUrls = imageUrls,
                                        enableWhatsapp = enableWhatsapp,
                                        phoneNumber = if (enableWhatsapp) phoneNumber else ""
                                    )

                                    savePlanToFirestore(db, plan)
                                    snackbarHostState.showSnackbar("¡Plan publicado con éxito!")
                                    navigateToHome()
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && selectedImages.isNotEmpty() && title.isNotBlank() &&
                        description.isNotBlank() && location.isNotBlank() && selectedDate != null && selectedTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publicar Plan")
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        selectedDate = dateMillis
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        formattedDate = sdf.format(Date(dateMillis))
                    }
                    showDatePicker = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Dialog
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seleccionar hora",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancelar")
                        }

                        TextButton(onClick = {
                            val hour = timePickerState.hour
                            val minute = timePickerState.minute
                            val formattedHour = hour.toString().padStart(2, '0')
                            val formattedMinute = minute.toString().padStart(2, '0')
                            selectedTime = "$formattedHour:$formattedMinute"
                            showTimePicker = false
                        }) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para salir
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Confirmar salida") },
            text = { Text("¿Estás seguro de que quieres salir? Se perderán los cambios no guardados.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        navigateToHome()
                    }
                ) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Función para comprobar si el formulario tiene algún contenido
private fun formHasContent(
    title: String,
    description: String,
    location: String,
    selectedDate: Long?,
    selectedImages: List<Uri>
): Boolean {
    return title.isNotBlank() || description.isNotBlank() || location.isNotBlank() ||
            selectedDate != null || selectedImages.isNotEmpty()
}

// Función para validar el formulario
private suspend fun validateForm(
    title: String,
    description: String,
    location: String,
    selectedDate: Long?,
    selectedImages: List<Uri>,
    snackbarHostState: SnackbarHostState
): Boolean {
    when {
        title.isBlank() -> {
            snackbarHostState.showSnackbar("Por favor, introduce un nombre para el plan")
            return false
        }
        description.isBlank() -> {
            snackbarHostState.showSnackbar("Por favor, introduce una descripción")
            return false
        }
        location.isBlank() -> {
            snackbarHostState.showSnackbar("Por favor, introduce una ubicación")
            return false
        }
        selectedDate == null -> {
            snackbarHostState.showSnackbar("Por favor, selecciona una fecha")
            return false
        }
        selectedImages.isEmpty() -> {
            snackbarHostState.showSnackbar("Por favor, selecciona al menos una imagen")
            return false
        }
    }
    return true
}

// Función para subir imágenes a Firebase Storage
private suspend fun uploadImagesToFirebase(
    context: Context,
    imageUris: List<Uri>,
    userId: String,
    planId: String
): List<String> = withContext(Dispatchers.IO) {
    val storage = FirebaseStorage.getInstance()
    val imageUrls = mutableListOf<String>()

    for (uri in imageUris) {
        try {
            // Optimizar la imagen antes de subirla
            val compressedImageBytes = compressImage(context, uri)

            // Crear referencia al storage para esta imagen
            val imageName = "${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child("planes/$userId/$planId/$imageName")

            // Subir la imagen optimizada
            val uploadTask = imageRef.putBytes(compressedImageBytes).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            imageUrls.add(downloadUrl)
        } catch (e: Exception) {
            // Manejar error
            e.printStackTrace()
        }
    }

    return@withContext imageUrls
}

// Función para comprimir la imagen
private suspend fun compressImage(context: Context, imageUri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)

    // Calcular nuevas dimensiones manteniendo el aspect ratio
    val maxDimension = 1024 // Tamaño máximo para cualquier dimensión
    val width = originalBitmap.width
    val height = originalBitmap.height

    val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
    val newWidth = max((width * scale).toInt(), 1)
    val newHeight = max((height * scale).toInt(), 1)

    // Redimensionar la imagen
    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

    // Comprimir la imagen
    val outputStream = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

    // Liberar recursos
    if (resizedBitmap != originalBitmap) {
        originalBitmap.recycle()
    }
    resizedBitmap.recycle()

    return@withContext outputStream.toByteArray()
}

// Función para guardar el Plan en Firestore
private suspend fun savePlanToFirestore(db: FirebaseFirestore, plan: Plan) = withContext(Dispatchers.IO) {
    db.collection("planes")
        .document(plan.id)
        .set(plan)
        .await()
}