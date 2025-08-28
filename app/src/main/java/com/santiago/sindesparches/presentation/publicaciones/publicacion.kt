package com.santiago.sindesparches.presentation.publicaciones

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.data.models.Plan
import com.santiago.sindesparches.ui.theme.white
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun publicacion_screen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navController: androidx.navigation.NavController,
    navigateToHome: () -> Unit,
    navigateToMapPicker: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados persistentes
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var locationAddress by rememberSaveable { mutableStateOf("Seleccionar ubicación") }
    var locationLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var locationLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var selectedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var formattedDate by rememberSaveable { mutableStateOf("") }
    var selectedTime by rememberSaveable { mutableStateOf("") }
    var enableWhatsapp by rememberSaveable { mutableStateOf(false) }

    // Estados para animaciones
    var isFormVisible by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingNumero by remember { mutableStateOf(true) }
    var phoneNumber by remember { mutableStateOf("") }

    // 🆕 Estados para el overlay de carga
    var showUploadOverlay by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("Preparando...") }

    val selectedImages = remember { mutableStateListOf<Uri>() }

    // Estados de pickers con fecha mínima
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val datePickerState = rememberDatePickerState(
        // 🆕 Configurar fecha mínima para hoy
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= today.timeInMillis
            }
        }
    )
    val timePickerState = rememberTimePickerState()

    var hasProcessedLocation by rememberSaveable { mutableStateOf(false) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(100)
        isFormVisible = true
    }

    // Observer de ubicación (sin cambios)
    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        val handle = navController.currentBackStackEntry?.savedStateHandle

        if (handle != null) {
            val address = handle.get<String>("location_address")
            val lat = handle.get<Double>("location_lat")
            val lng = handle.get<Double>("location_lng")

            Log.d("PublicacionScreen", "🔍 Verificando datos de ubicación:")
            Log.d("PublicacionScreen", "   Address: $address")
            Log.d("PublicacionScreen", "   Current Address: $locationAddress")
            Log.d("PublicacionScreen", "   Has processed: $hasProcessedLocation")

            if (address != null && lat != null && lng != null && !hasProcessedLocation) {
                Log.d("PublicacionScreen", "✅ Procesando nueva ubicación...")

                locationAddress = address
                locationLat = lat
                locationLng = lng
                hasProcessedLocation = true

                handle.remove<String>("location_address")
                handle.remove<Double>("location_lat")
                handle.remove<Double>("location_lng")

                Log.d("PublicacionScreen", "🎉 Ubicación actualizada: $locationAddress")
                Log.d("PublicacionScreen", "📝 Datos preservados - Título: '$title', Desc: '${description.take(30)}...'")
            }
        }
    }

    LaunchedEffect(navController.currentBackStackEntry?.destination?.route) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute?.contains("publicacion") != true) {
            hasProcessedLocation = false
        }
    }

    // Cargar número de teléfono
    LaunchedEffect(auth.currentUser?.uid) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            try {
                val snapshot = db.collection("perfil")
                    .document(currentUserId)
                    .get()
                    .await()
                phoneNumber = snapshot.getString("celular") ?: "No registrado"
            } catch (e: Exception) {
                phoneNumber = "Error al cargar"
            } finally {
                isLoadingNumero = false
            }
        }
    }

    // Colores del tema
    val primaryColor = Color(0xFF6C5CE7)
    val accentColor = Color(0xFF00D2FF)
    val accentSecondary = Color(0xFFFF6B6B)
    val backgroundDark = Color(0xFF0F0F23)
    val surfaceDark = Color(0xFF1A1A2E)
    val surfaceLight = Color(0xFF252547)

    // 🆕 Función de subida mejorada con overlay y navegación automática
    suspend fun uploadWithProgress(): Boolean {
        return try {
            showUploadOverlay = true
            uploadProgress = 0f
            uploadStatus = "Preparando imágenes..."
            delay(500)

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val planId = UUID.randomUUID().toString()

                uploadStatus = "Subiendo imágenes..."
                uploadProgress = 0.3f
                delay(300)

                val imageUrls = uploadImagesToFirebase(context, selectedImages, currentUser.uid, planId)

                uploadProgress = 0.7f
                uploadStatus = "Guardando evento..."
                delay(300)

                val plan = Plan(
                    id = planId,
                    userId = currentUser.uid,
                    createdAt = System.currentTimeMillis(),
                    title = title,
                    description = description,
                    date = selectedDate ?: 0L,
                    timeString = selectedTime,
                    location = locationAddress,
                    latitude = locationLat,
                    longitude = locationLng,
                    imageUrls = imageUrls,
                    enableWhatsapp = enableWhatsapp,
                    phoneNumber = if (enableWhatsapp) phoneNumber else ""
                )

                savePlanToFirestore(db, plan)

                // 🚀 Completar animación y navegar inmediatamente
                uploadProgress = 1f
                uploadStatus = "¡Completado!"
                delay(300) // Delay muy corto solo para mostrar el 100%

                // 🏠 Navegar inmediatamente sin mostrar mensaje de éxito en snackbar aquí
                showUploadOverlay = false
                navigateToHome()

                true
            } else {
                false
            }
        } catch (e: Exception) {
            uploadStatus = "Error: ${e.message}"
            delay(1500)
            false
        } finally {
            showUploadOverlay = false
        }
    }

    val navigateToMapWithPreservation = {
        Log.d("PublicacionScreen", "🚀 Navegando al mapa con datos preservados:")
        hasProcessedLocation = false
        navigateToMapPicker()
    }

    // 🆕 Box principal con overlay
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Crear Evento",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (!showUploadOverlay) {
                                    if (formHasContent(title, description, locationAddress, selectedDate, selectedImages)) {
                                        showExitDialog = true
                                    } else {
                                        navigateToHome()
                                    }
                                }
                            },
                            enabled = !showUploadOverlay // 🆕 Deshabilitar cuando se está subiendo
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = if (showUploadOverlay) Color.Gray else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundDark
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = backgroundDark
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundDark,
                                Color(0xFF16213E),
                                backgroundDark
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                        .animateContentSize()
                        // 🆕 Aplicar alpha cuando está cargando
                        .alpha(if (showUploadOverlay) 0.3f else 1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Indicador de progreso (sin cambios)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = surfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val progress = when {
                            title.isNotBlank() && description.isNotBlank() && locationAddress.isNotBlank() && locationAddress != "Seleccionar ubicación"
                                    && selectedDate != null && selectedTime.isNotBlank() && selectedImages.isNotEmpty() -> 1f
                            title.isNotBlank() && description.isNotBlank() && locationAddress.isNotBlank() && locationAddress != "Seleccionar ubicación"
                                    && selectedDate != null && selectedTime.isNotBlank() -> 0.8f
                            title.isNotBlank() && description.isNotBlank() && locationAddress.isNotBlank() && locationAddress != "Seleccionar ubicación" -> 0.6f
                            title.isNotBlank() && description.isNotBlank() -> 0.4f
                            title.isNotBlank() -> 0.2f
                            else -> 0f
                        }

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Progreso del evento",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when {
                                    progress >= 0.8f -> accentColor
                                    progress >= 0.5f -> primaryColor
                                    else -> accentSecondary
                                },
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}% completado",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Campos de formulario (sin cambios en su estructura)
                    AnimatedTextField(
                        value = title,
                        onValueChange = { newValue ->
                            if (!showUploadOverlay) { // 🆕 Solo permitir cambios si no está cargando
                                title = newValue
                                Log.d("PublicacionScreen", "✏️ Título actualizado: '$newValue'")
                            }
                        },
                        label = "Nombre del evento",
                        icon = Icons.Default.Star,
                        isVisible = isFormVisible,
                        delay = 100,
                        primaryColor = primaryColor,
                        surfaceColor = surfaceLight,
                        enabled = !showUploadOverlay // 🆕 Deshabilitar durante carga
                    )

                    AnimatedTextField(
                        value = description,
                        onValueChange = { newValue ->
                            if (!showUploadOverlay) {
                                description = newValue
                                Log.d("PublicacionScreen", "✏️ Descripción actualizada: '${newValue.take(30)}...'")
                            }
                        },
                        label = "¿Qué tienes planeado?",
                        icon = Icons.Default.Create,
                        isVisible = isFormVisible,
                        delay = 200,
                        primaryColor = primaryColor,
                        surfaceColor = surfaceLight,
                        multiline = true,
                        maxLines = 4,
                        enabled = !showUploadOverlay
                    )

                    // Fecha y Hora
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Fecha
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !showUploadOverlay) { // 🆕 Deshabilitar clic durante carga
                                    showDatePicker = true
                                },
                            colors = CardDefaults.cardColors(containerColor = surfaceLight),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = if (showUploadOverlay) Color.Gray else primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (formattedDate.isNotEmpty()) formattedDate else "Fecha",
                                    color = if (formattedDate.isNotEmpty()) Color.White else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Hora
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !showUploadOverlay) {
                                    showTimePicker = true
                                },
                            colors = CardDefaults.cardColors(containerColor = surfaceLight),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.bx_time),
                                    contentDescription = null,
                                    tint = if (showUploadOverlay) Color.Gray else accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedTime.isNotEmpty()) selectedTime else "Hora",
                                    color = if (selectedTime.isNotEmpty()) Color.White else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Ubicación
                    EnhancedLocationDisplay(
                        locationAddress = locationAddress,
                        onClick = if (!showUploadOverlay) navigateToMapWithPreservation else {{}}, // 🆕 Deshabilitar durante carga
                        accentSecondary = accentSecondary,
                        surfaceLight = surfaceLight,
                        locationLat = locationLat,
                        locationLng = locationLng,
                        context = context,
                        enabled = !showUploadOverlay
                    )

                    // Sección de imágenes
                    ImagesSection(
                        selectedImages = selectedImages,
                        primaryColor = primaryColor,
                        accentSecondary = accentSecondary,
                        surfaceDark = surfaceDark,
                        enabled = !showUploadOverlay // 🆕 Pasar estado de habilitado
                    )

                    // Sección de WhatsApp
                    WhatsAppSection(
                        enableWhatsapp = enableWhatsapp,
                        onWhatsAppToggle = { if (!showUploadOverlay) enableWhatsapp = it }, // 🆕 Controlar cambios
                        phoneNumber = phoneNumber,
                        isLoadingNumero = isLoadingNumero,
                        surfaceColor = surfaceDark,
                        enabled = !showUploadOverlay
                    )

                    // Botón de publicar
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (validateForm(title, description, locationAddress, selectedDate, selectedImages, snackbarHostState)) {
                                    isLoading = true
                                    try {
                                        val success = uploadWithProgress()
                                        // 🚀 La navegación ya se maneja dentro de uploadWithProgress()
                                        // Solo manejar errores aquí
                                        if (!success) {
                                            snackbarHostState.showSnackbar("Error al publicar el evento")
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading && !showUploadOverlay && selectedImages.isNotEmpty() && title.isNotBlank() &&
                                description.isNotBlank() && locationAddress != "Seleccionar ubicación" && selectedDate != null && selectedTime.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading || showUploadOverlay) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Publicar Evento",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 🆕 OVERLAY DE CARGA CON ANIMACIÓN
        AnimatedVisibility(
            visible = showUploadOverlay,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(300)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.1f),
                                backgroundDark.copy(alpha = 0.95f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            radius = 800f
                        )
                    )
                    .clickable(enabled = false) {} // Bloquear interacciones
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceDark.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 🎭 Animación de carga circular con pulso
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Círculo de fondo con pulso
                            val infiniteTransition = rememberInfiniteTransition()
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(pulseScale)
                                    .background(
                                        primaryColor.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            )

                            // Indicador de progreso circular
                            CircularProgressIndicator(
                                progress = uploadProgress,
                                modifier = Modifier.size(60.dp),
                                color = primaryColor,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 6.dp
                            )

                            // Icono central
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = uploadStatus,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Barra de progreso lineal adicional
                        LinearProgressIndicator(
                            progress = uploadProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = accentColor,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Por favor espera...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // DatePicker Dialog con restricción de fechas pasadas
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                if (!showUploadOverlay) showDatePicker = false // 🆕 Solo cerrar si no está cargando
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            selectedDate = dateMillis
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formattedDate = sdf.format(Date(dateMillis))
                            Log.d("PublicacionScreen", "📅 Fecha seleccionada: $formattedDate")
                        }
                        showDatePicker = false
                    },
                    enabled = !showUploadOverlay // 🆕 Deshabilitar durante carga
                ) {
                    Text("Confirmar", color = if (showUploadOverlay) Color.Gray else primaryColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    enabled = !showUploadOverlay
                ) {
                    Text("Cancelar", color = if (showUploadOverlay) Color.Gray else Color.Gray)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = surfaceDark
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = surfaceDark,
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White,
                    subheadContentColor = Color.White,
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = primaryColor,
                    todayContentColor = primaryColor,
                    todayDateBorderColor = primaryColor,
                    // 🆕 Colorear fechas no seleccionables
                    disabledDayContentColor = Color.Gray.copy(alpha = 0.4f)
                )
            )
        }
    }

    // TimePicker Dialog (sin cambios significativos, solo agregar verificación de overlay)
    if (showTimePicker) {
        Dialog(onDismissRequest = {
            if (!showUploadOverlay) showTimePicker = false
        }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceDark,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seleccionar hora",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            containerColor = surfaceLight,
                            clockDialColor = surfaceLight,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = Color.White.copy(alpha = 0.7f),
                            selectorColor = primaryColor,
                            periodSelectorBorderColor = primaryColor,
                            periodSelectorSelectedContainerColor = primaryColor,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContentColor = Color.White,
                            timeSelectorSelectedContainerColor = primaryColor,
                            timeSelectorUnselectedContainerColor = surfaceLight,
                            timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContentColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showTimePicker = false },
                            enabled = !showUploadOverlay
                        ) {
                            Text("Cancelar", color = if (showUploadOverlay) Color.Gray else Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = {
                                val hour = timePickerState.hour
                                val minute = timePickerState.minute
                                val formattedHour = hour.toString().padStart(2, '0')
                                val formattedMinute = minute.toString().padStart(2, '0')
                                selectedTime = "$formattedHour:$formattedMinute"
                                showTimePicker = false
                                Log.d("PublicacionScreen", "⏰ Hora seleccionada: $selectedTime")
                            },
                            enabled = !showUploadOverlay
                        ) {
                            Text("Confirmar", color = if (showUploadOverlay) Color.Gray else primaryColor)
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para salir
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!showUploadOverlay) showExitDialog = false
            },
            containerColor = surfaceDark,
            title = {
                Text(
                    "Confirmar salida",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que quieres salir? Se perderán los cambios no guardados.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        navigateToHome()
                    },
                    enabled = !showUploadOverlay
                ) {
                    Text("Salir", color = if (showUploadOverlay) Color.Gray else accentSecondary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    enabled = !showUploadOverlay
                ) {
                    Text("Cancelar", color = if (showUploadOverlay) Color.Gray else Color.Gray)
                }
            }
        )
    }
}


// 🔧 Componente AnimatedTextField mejorado
@Composable
fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isVisible: Boolean,
    delay: Long,
    primaryColor: Color,
    surfaceColor: Color,
    multiline: Boolean = false,
    maxLines: Int = 1,
    enabled: Boolean = true // 🆕 Parámetro agregado
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = delay.toInt(),
            easing = FastOutSlowInEasing
        )
    )

    val animatedOffset by animateIntAsState(
        targetValue = if (isVisible) 0 else 50,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = delay.toInt(),
            easing = FastOutSlowInEasing
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = animatedOffset.dp)
            .alpha(animatedAlpha),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) primaryColor else Color.Gray.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = enabled, // 🆕 Usar parámetro enabled
            singleLine = !multiline,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.Gray.copy(alpha = 0.5f), // 🆕 Color deshabilitado
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                disabledBorderColor = Color.Gray.copy(alpha = 0.3f), // 🆕 Borde deshabilitado
                cursorColor = primaryColor
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun EnhancedLocationDisplay(
    locationAddress: String,
    onClick: () -> Unit,
    accentSecondary: Color,
    surfaceLight: Color,
    locationLat: Double?,
    locationLng: Double?,
    context: Context,
    enabled: Boolean = true // 🆕 Parámetro agregado
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }, // 🆕 Controlar clickable
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (enabled) {
                    if (locationAddress == "Seleccionar ubicación") accentSecondary else Color.Green
                } else {
                    Color.Gray.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Ubicación del evento",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = locationAddress,
                    color = if (enabled) Color.White else Color.Gray.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (locationLat != null && locationLng != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 ${String.format("%.4f", locationLat)}, ${String.format("%.4f", locationLng)}",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}

// 📸 ImagesSection mejorado
@Composable
fun ImagesSection(
    selectedImages: MutableList<Uri>,
    primaryColor: Color,
    accentSecondary: Color,
    surfaceDark: Color,
    enabled: Boolean = true
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (enabled) {
            // 🔧 FIX: En lugar de limpiar, agregamos solo las nuevas imágenes
            val remainingSlots = 5 - selectedImages.size
            if (remainingSlots > 0) {
                selectedImages.addAll(uris.take(remainingSlots))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.bxs_camera_plus),
                    contentDescription = null,
                    tint = if (enabled) primaryColor else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Fotos del evento",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${selectedImages.size}/5",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid de imágenes o botón para agregar
            if (selectedImages.isEmpty()) {
                Button(
                    onClick = {
                        if (enabled) launcher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(
                        2.dp,
                        if (enabled) accentSecondary else Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = if (enabled) accentSecondary else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Agregar fotos",
                            color = if (enabled) Color.White else Color.Gray.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(selectedImages.size) { index ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .alpha(if (enabled) 1f else 0.5f)
                        ) {
                            AsyncImage(
                                model = selectedImages[index],
                                contentDescription = "Imagen $index",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Botón para eliminar imagen (solo si está habilitado)
                            if (enabled) {
                                IconButton(
                                    onClick = { selectedImages.removeAt(index) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.6f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Botón para agregar más imágenes
                    if (selectedImages.size < 5 && enabled) {
                        item {
                            Button(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                border = BorderStroke(2.dp, accentSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Agregar más",
                                    tint = accentSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 📱 WhatsAppSection mejorado
@Composable
fun WhatsAppSection(
    enableWhatsapp: Boolean,
    onWhatsAppToggle: (Boolean) -> Unit,
    phoneNumber: String,
    isLoadingNumero: Boolean,
    surfaceColor: Color,
    enabled: Boolean = true // 🆕 Parámetro agregado
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.bxl_whatsapp), // Asegúrate de tener este ícono
                    contentDescription = null,
                    tint = if (enabled && enableWhatsapp) Color(0xFF25D366) else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Contacto por WhatsApp",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Permitir que te contacten por WhatsApp",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = enableWhatsapp,
                    onCheckedChange = onWhatsAppToggle,
                    enabled = enabled, // 🆕 Controlar enabled
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF25D366),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f),
                        disabledCheckedThumbColor = Color.Gray.copy(alpha = 0.5f),
                        disabledCheckedTrackColor = Color.Gray.copy(alpha = 0.3f),
                        disabledUncheckedThumbColor = Color.Gray.copy(alpha = 0.4f),
                        disabledUncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                )
            }

            if (enableWhatsapp) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Número: ",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isLoadingNumero) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF25D366),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = phoneNumber,
                            color = if (enabled) Color.White else Color.Gray.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
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
    return title.isNotBlank() || description.isNotBlank() || (location != "Seleccionar ubicación") ||
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
        location == "Seleccionar ubicación" -> {
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