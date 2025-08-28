package com.santiago.sindesparches.presentation.editplanscreen

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.data.models.Plan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import kotlinx.coroutines.withTimeout
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateToHome: () -> Unit,
    navigateToMapPicker: () -> Unit,
    navController: androidx.navigation.NavController,
    planId: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    // Estados principales del formulario
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var dateInMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var timeString by rememberSaveable { mutableStateOf("12:00") }
    var selectedImageUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImageUrls by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPlanData by remember { mutableStateOf(planId != null) }

    // Estados para WhatsApp
    var enableWhatsapp by rememberSaveable { mutableStateOf(false) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    // Variables reactivas ubicación
    var locationAddress by rememberSaveable { mutableStateOf("") }
    var locationLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var locationLng by rememberSaveable { mutableStateOf<Double?>(null) }

    // Estado para detectar cambios de ubicación
    var locationUpdateTrigger by remember { mutableStateOf(0) }

    // Función para navegar al mapa preservando datos
    val navigateToMapWithPreservation = {
        Log.d("PublicacionScreen", "🚀 Navegando al mapa preservando datos...")
        Log.d("PublicacionScreen", "Datos actuales - Título: $title, Descripción: $description")

        // Guardar datos en el savedStateHandle antes de navegar
        navController.currentBackStackEntry?.savedStateHandle?.apply {
            set("preserved_title", title)
            set("preserved_description", description)
            set("preserved_date", dateInMillis)
            set("preserved_time", timeString)
            set("preserved_enable_whatsapp", enableWhatsapp)
            set("preserved_phone", phoneNumber)
        }

        locationUpdateTrigger += 1
        navigateToMapPicker()
    }

    // Observer mejorado para ubicación que preserva datos
    LaunchedEffect(locationUpdateTrigger, navController.currentBackStackEntry) {
        delay(100)

        val currentEntry = navController.currentBackStackEntry
        val previousEntry = navController.previousBackStackEntry

        Log.d("PublicacionScreen", "🔄 Verificando navegación...")

        // Restaurar datos preservados primero
        currentEntry?.savedStateHandle?.let { handle ->
            handle.get<String>("preserved_title")?.let { preserved ->
                if (preserved.isNotEmpty() && title != preserved) {
                    title = preserved
                    Log.d("PublicacionScreen", "✅ Título restaurado: $preserved")
                }
            }

            handle.get<String>("preserved_description")?.let { preserved ->
                if (preserved.isNotEmpty() && description != preserved) {
                    description = preserved
                    Log.d("PublicacionScreen", "✅ Descripción restaurada: $preserved")
                }
            }

            handle.get<Long>("preserved_date")?.let { preserved ->
                if (dateInMillis != preserved) {
                    dateInMillis = preserved
                    Log.d("PublicacionScreen", "✅ Fecha restaurada")
                }
            }

            handle.get<String>("preserved_time")?.let { preserved ->
                if (timeString != preserved) {
                    timeString = preserved
                    Log.d("PublicacionScreen", "✅ Hora restaurada: $preserved")
                }
            }

            handle.get<Boolean>("preserved_enable_whatsapp")?.let { preserved ->
                if (enableWhatsapp != preserved) {
                    enableWhatsapp = preserved
                    Log.d("PublicacionScreen", "✅ WhatsApp restaurado: $preserved")
                }
            }

            handle.get<String>("preserved_phone")?.let { preserved ->
                if (phoneNumber != preserved) {
                    phoneNumber = preserved
                    Log.d("PublicacionScreen", "✅ Teléfono restaurado: $preserved")
                }
            }
        }

        // Buscar datos de ubicación
        listOf(currentEntry, previousEntry).forEach { entry ->
            entry?.savedStateHandle?.let { handle ->
                val address = handle.get<String>("location_address")
                val lat = handle.get<Double>("location_lat")
                val lng = handle.get<Double>("location_lng")

                if (address != null && lat != null && lng != null) {
                    Log.d("PublicacionScreen", "✅ Ubicación encontrada: $address")

                    if (locationAddress != address) {
                        locationAddress = address
                        locationLat = lat
                        locationLng = lng

                        // Limpiar datos de ubicación
                        handle.remove<String>("location_address")
                        handle.remove<Double>("location_lat")
                        handle.remove<Double>("location_lng")

                        Log.d("PublicacionScreen", "🎉 Ubicación actualizada exitosamente")
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    // Cargar datos del plan en modo edición
    LaunchedEffect(planId) {
        if (planId != null && planId.isNotBlank()) {
            try {
                isLoadingPlanData = true
                val plan = getPlanById(db, planId)
                plan?.let { loadedPlan ->
                    title = loadedPlan.title
                    description = loadedPlan.description
                    location = loadedPlan.location
                    locationAddress = loadedPlan.location
                    locationLat = loadedPlan.latitude
                    locationLng = loadedPlan.longitude
                    dateInMillis = loadedPlan.date
                    timeString = loadedPlan.timeString
                    existingImageUrls = loadedPlan.imageUrls
                    enableWhatsapp = loadedPlan.enableWhatsapp ?: false
                    phoneNumber = loadedPlan.phoneNumber ?: ""
                }
            } catch (e: Exception) {
                Log.e("PublicacionScreen", "Error al cargar plan", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error al cargar el plan: ${e.message}")
                }
            } finally {
                isLoadingPlanData = false
            }
        }
    }

    // Selector de imágenes mejorado
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
            Log.d("PublicacionScreen", "📸 Imágenes agregadas: ${uris.size}, Total: ${selectedImageUris.size}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (planId == null) "Crear Plan" else "Editar Plan",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isLoading) navigateToHome() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = if (isLoading)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(
                            alpha = if (isLoading) 0.7f else 1f
                        )
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            if (isLoadingPlanData) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .alpha(if (isLoading) 0.3f else 1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header con descripción
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (planId == null) "Nuevo Plan" else "Editando Plan",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Completa la información para ${if (planId == null) "crear" else "actualizar"} tu plan",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Información básica
                    InformationBasicCard(
                        title = title,
                        description = description,
                        locationAddress = locationAddress,
                        locationLat = locationLat,
                        locationLng = locationLng,
                        onTitleChange = { if (!isLoading) title = it },
                        onDescriptionChange = { if (!isLoading) description = it },
                        onLocationClick = { if (!isLoading) navigateToMapWithPreservation() },
                        context = context,
                        isEnabled = !isLoading
                    )

                    // Fecha y hora
                    DateTimeCard(
                        dateInMillis = dateInMillis,
                        timeString = timeString,
                        onDateChange = { if (!isLoading) dateInMillis = it },
                        onTimeChange = { if (!isLoading) timeString = it },
                        context = context,
                        isEnabled = !isLoading
                    )

                    // Contacto por WhatsApp
                    WhatsAppContactCard(
                        enableWhatsapp = enableWhatsapp,
                        phoneNumber = phoneNumber,
                        onEnableWhatsappChange = { if (!isLoading) enableWhatsapp = it },
                        onPhoneNumberChange = { if (!isLoading) phoneNumber = it },
                        isEnabled = !isLoading
                    )

                    // Imágenes del plan
                    ImagesCard(
                        existingImageUrls = existingImageUrls,
                        selectedImageUris = selectedImageUris,
                        onRemoveExistingImage = { imageUrl ->
                            if (!isLoading) {
                                existingImageUrls = existingImageUrls.filter { it != imageUrl }
                            }
                        },
                        onRemoveSelectedImage = { uri ->
                            if (!isLoading) {
                                selectedImageUris = selectedImageUris.filter { it != uri }
                            }
                        },
                        onAddImages = { if (!isLoading) imagePickerLauncher.launch("image/*") },
                        isEnabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón para guardar el plan
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (validateForm(title, description, locationAddress, enableWhatsapp, phoneNumber)) {
                                    try {
                                        isLoading = true
                                        Log.d("PublicacionScreen", "💾 Iniciando guardado. PlanId: $planId")
                                        Log.d("PublicacionScreen", "📸 Imágenes a guardar - Existentes: ${existingImageUrls.size}, Nuevas: ${selectedImageUris.size}")

                                        if (planId == null || planId.isBlank()) {
                                            // Crear nuevo plan
                                            val newPlanId = createPlanWithCoordinates(
                                                context = context,
                                                auth = auth,
                                                db = db,
                                                storage = storage,
                                                title = title,
                                                description = description,
                                                location = locationAddress,
                                                latitude = locationLat,
                                                longitude = locationLng,
                                                dateInMillis = dateInMillis,
                                                timeString = timeString,
                                                selectedImageUris = selectedImageUris,
                                                enableWhatsapp = enableWhatsapp,
                                                phoneNumber = if (enableWhatsapp) phoneNumber else ""
                                            )
                                            Log.d("PublicacionScreen", "✅ Plan creado con ID: $newPlanId")
                                            snackbarHostState.showSnackbar("Plan creado correctamente")
                                        } else {
                                            // Actualizar plan existente
                                            updatePlanWithCoordinates(
                                                context = context,
                                                planId = planId,
                                                auth = auth,
                                                db = db,
                                                storage = storage,
                                                title = title,
                                                description = description,
                                                location = locationAddress,
                                                latitude = locationLat,
                                                longitude = locationLng,
                                                dateInMillis = dateInMillis,
                                                timeString = timeString,
                                                existingImageUrls = existingImageUrls,
                                                newImageUris = selectedImageUris,
                                                enableWhatsapp = enableWhatsapp,
                                                phoneNumber = if (enableWhatsapp) phoneNumber else ""
                                            )
                                            Log.d("PublicacionScreen", "✅ Plan actualizado: $planId")
                                            snackbarHostState.showSnackbar("Plan actualizado correctamente")
                                        }

                                        delay(2000) // Mostrar mensaje por más tiempo
                                        navigateToHome()

                                    } catch (e: Exception) {
                                        Log.e("PublicacionScreen", "❌ Error al guardar plan", e)
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    } finally {
                                        isLoading = false
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Por favor completa todos los campos obligatorios")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        contentPadding = PaddingValues(vertical = 18.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (planId == null) "Creando..." else "Guardando...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            )
                        } else {
                            Icon(
                                if (planId == null) Icons.Default.Add else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (planId == null) "Crear Plan" else "Guardar Cambios",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Overlay de carga con animación
        if (isLoading) {
            LoadingOverlay()
        }
    }
}

// Overlay de carga animado
@Composable
private fun LoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha * 0.7f)
                    ),
                    radius = 800f
                )
            )
            .zIndex(1000f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(120.dp)
                .alpha(alpha),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(scale),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Guardando...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Componentes actualizados con habilitación/deshabilitación

@Composable
private fun InformationBasicCard(
    title: String,
    description: String,
    locationAddress: String,
    locationLat: Double?,
    locationLng: Double?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLocationClick: () -> Unit,
    context: Context,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (isEnabled) 0.4f else 0.2f
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Información básica",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isEnabled) 1f else 0.6f
                )
            )

            // Título
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = {
                    Text(
                        "Título del plan *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = isEnabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            )

            // Descripción
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = {
                    Text(
                        "Descripción detallada *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = isEnabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            )

            // Ubicación
            EnhancedLocationDisplayEdit(
                locationAddress = locationAddress,
                onClick = onLocationClick,
                accentSecondary = MaterialTheme.colorScheme.error,
                surfaceLight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                locationLat = locationLat,
                locationLng = locationLng,
                context = context,
                isEnabled = isEnabled
            )
        }
    }
}

@Composable
private fun DateTimeCard(
    dateInMillis: Long,
    timeString: String,
    onDateChange: (Long) -> Unit,
    onTimeChange: (String) -> Unit,
    context: Context,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (isEnabled) 0.4f else 0.2f
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Fecha y hora",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isEnabled) 1f else 0.6f
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de fecha
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isEnabled) {
                            if (isEnabled) {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = dateInMillis

                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCalendar = Calendar.getInstance()
                                        newCalendar.set(year, month, dayOfMonth)
                                        onDateChange(newCalendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                datePickerDialog.datePicker.minDate = System.currentTimeMillis()
                                datePickerDialog.show()
                            }
                        }
                ) {
                    OutlinedTextField(
                        value = formatDate(dateInMillis),
                        onValueChange = { },
                        label = {
                            Text(
                                "Fecha",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (isEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isEnabled) 1f else 0.6f
                            ),
                            disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                        )
                    )
                }

                // Selector de hora
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isEnabled) {
                            if (isEnabled) {
                                val timeParts = timeString.split(":")
                                val currentHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 12
                                val currentMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                                TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        onTimeChange(String.format("%02d:%02d", selectedHour, selectedMinute))
                                    },
                                    currentHour,
                                    currentMinute,
                                    true
                                ).show()
                            }
                        }
                ) {
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { },
                        label = {
                            Text(
                                "Hora",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.bx_time),
                                contentDescription = null,
                                tint = if (isEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isEnabled) 1f else 0.6f
                            ),
                            disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                        )
                    )
                }
            }

            // Información adicional sobre la fecha/hora seleccionada
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dateInMillis
            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = if (isEnabled) 0.3f else 0.1f
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isEnabled) 1f else 0.6f
                        ),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "El evento será el $dayOfWeek a las $timeString",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isEnabled) 0.8f else 0.5f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsAppContactCard(
    enableWhatsapp: Boolean,
    phoneNumber: String,
    onEnableWhatsappChange: (Boolean) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enableWhatsapp && isEnabled)
                Color(0xFF25D366).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (isEnabled) 0.4f else 0.2f
                )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Opciones de contacto",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isEnabled) 1f else 0.6f
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bxl_whatsapp),
                        contentDescription = null,
                        tint = Color(0xFF25D366).copy(
                            alpha = if (isEnabled) 1f else 0.4f
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Contacto por WhatsApp",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isEnabled) 1f else 0.6f
                            )
                        )
                        Text(
                            text = if (enableWhatsapp) "Los usuarios podrán contactarte" else "Función desactivada",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 1f else 0.6f
                            )
                        )
                    }
                }

                Switch(
                    checked = enableWhatsapp,
                    onCheckedChange = onEnableWhatsappChange,
                    enabled = isEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF25D366),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        disabledCheckedTrackColor = Color(0xFF25D366).copy(alpha = 0.4f),
                        disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }

            if (enableWhatsapp) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = {
                        Text(
                            "Número de WhatsApp *",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isEnabled,
                    placeholder = {
                        Text(
                            "Ej: +57 300 123 4567",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bxl_whatsapp),
                            contentDescription = null,
                            tint = Color(0xFF25D366).copy(
                                alpha = if (isEnabled) 1f else 0.4f
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = Color(0xFF25D366),
                        focusedBorderColor = Color(0xFF25D366),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
private fun ImagesCard(
    existingImageUrls: List<String>,
    selectedImageUris: List<Uri>,
    onRemoveExistingImage: (String) -> Unit,
    onRemoveSelectedImage: (Uri) -> Unit,
    onAddImages: () -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (isEnabled) 0.4f else 0.2f
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Imágenes del plan",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isEnabled) 1f else 0.6f
                )
            )

            // Imágenes existentes (solo en modo edición)
            if (existingImageUrls.isNotEmpty()) {
                Text(
                    text = "Imágenes actuales",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isEnabled) 1f else 0.6f
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(existingImageUrls) { imageUrl ->
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .alpha(if (isEnabled) 1f else 0.6f)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Imagen del plan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isEnabled) {
                                IconButton(
                                    onClick = { onRemoveExistingImage(imageUrl) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(50)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar imagen",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Imágenes seleccionadas nuevas
            if (selectedImageUris.isNotEmpty()) {
                Text(
                    text = "Nuevas imágenes seleccionadas",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isEnabled) 1f else 0.6f
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .alpha(if (isEnabled) 1f else 0.6f)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Imagen seleccionada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isEnabled) {
                                IconButton(
                                    onClick = { onRemoveSelectedImage(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(50)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar imagen",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botón para agregar imágenes
            Button(
                onClick = onAddImages,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled,
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isEnabled)
                        MaterialTheme.colorScheme.onSecondary
                    else
                        MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Agregar imágenes",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onSecondary
                    else
                        MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Componente de ubicación mejorado
@Composable
fun EnhancedLocationDisplayEdit(
    locationAddress: String,
    onClick: () -> Unit,
    accentSecondary: Color,
    surfaceLight: Color,
    locationLat: Double? = null,
    locationLng: Double? = null,
    context: Context,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = surfaceLight.copy(
                alpha = if (isEnabled) 1f else 0.6f
            )
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    tint = accentSecondary.copy(
                        alpha = if (isEnabled) 1f else 0.6f
                    ),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ubicación del evento *",
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isEnabled) 0.7f else 0.4f
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (locationAddress.isEmpty() || locationAddress == "Seleccionar ubicación") {
                            "Toca para seleccionar ubicación"
                        } else {
                            "Ubicación seleccionada"
                        },
                        color = if (locationAddress.isEmpty() || locationAddress == "Seleccionar ubicación") {
                            accentSecondary.copy(alpha = if (isEnabled) 1f else 0.6f)
                        } else {
                            Color.Green.copy(alpha = if (isEnabled) 1f else 0.6f)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isEnabled) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar ubicación",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (locationAddress.isNotEmpty() && locationAddress != "Seleccionar ubicación") {
                Text(
                    text = locationAddress,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isEnabled) 1f else 0.6f
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (locationLat != null && locationLng != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%.6f", locationLat)}, ${String.format("%.6f", locationLng)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isEnabled) 0.6f else 0.4f
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "Agregar ubicación",
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isEnabled) 1f else 0.6f
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para seleccionar dónde será tu evento",
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isEnabled) 0.6f else 0.4f
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Funciones auxiliares
private fun validateForm(
    title: String,
    description: String,
    locationAddress: String,
    enableWhatsapp: Boolean,
    phoneNumber: String
): Boolean {
    val basicValidation = title.isNotBlank() &&
            description.isNotBlank() &&
            locationAddress.isNotEmpty() &&
            locationAddress != "Seleccionar ubicación"

    val whatsappValidation = if (enableWhatsapp) {
        phoneNumber.isNotBlank()
    } else {
        true
    }

    return basicValidation && whatsappValidation
}

private fun formatDate(dateInMillis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(dateInMillis))
}

// Función para obtener un plan por ID
private suspend fun getPlanById(db: FirebaseFirestore, planId: String): Plan? {
    return try {
        val doc = db.collection("planes").document(planId).get().await()
        if (doc.exists()) {
            doc.toObject(Plan::class.java)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("getPlanById", "Error al obtener plan", e)
        null
    }
}

// Función mejorada para crear un nuevo plan con coordenadas
private suspend fun createPlanWithCoordinates(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    title: String,
    description: String,
    location: String,
    latitude: Double?,
    longitude: Double?,
    dateInMillis: Long,
    timeString: String,
    selectedImageUris: List<Uri>,
    enableWhatsapp: Boolean = false,
    phoneNumber: String = ""
): String = withContext(Dispatchers.IO) {
    try {
        val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
        Log.d("CreatePlan", "🆕 Creando plan para usuario: $userId")
        Log.d("CreatePlan", "📸 Imágenes a subir: ${selectedImageUris.size}")

        // Verificar autenticación
        if (auth.currentUser == null) {
            throw Exception("Usuario no autenticado")
        }

        val planId = UUID.randomUUID().toString()
        Log.d("CreatePlan", "🆔 Nuevo Plan ID: $planId")

        // Subir imágenes con manejo de errores mejorado
        val imageUrls = if (selectedImageUris.isNotEmpty()) {
            try {
                uploadOptimizedImages(context, storage, userId, planId, selectedImageUris)
            } catch (e: Exception) {
                Log.e("CreatePlan", "❌ Error al subir imágenes", e)
                // Continuar sin imágenes en lugar de fallar completamente
                emptyList()
            }
        } else {
            emptyList()
        }
        Log.d("CreatePlan", "✅ Imágenes procesadas: ${imageUrls.size}")

        val createdAt = System.currentTimeMillis()

        val plan = Plan(
            id = planId,
            title = title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            date = dateInMillis,
            timeString = timeString,
            imageUrls = imageUrls,
            userId = userId,
            createdAt = createdAt,
            enableWhatsapp = enableWhatsapp,
            phoneNumber = phoneNumber
        )

        // Guardar en Firestore con reintentos
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                db.collection("planes").document(planId).set(plan).await()
                Log.d("CreatePlan", "✅ Plan guardado en Firestore: $planId")
                break
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    throw e
                }
                Log.w("CreatePlan", "⚠️ Reintentando guardar plan ($retryCount/$maxRetries)")
                delay(100L * retryCount) // Espera progresiva
            }
        }

        return@withContext planId

    } catch (e: Exception) {
        Log.e("CreatePlan", "❌ Error al crear plan", e)
        throw Exception("Error al crear el plan: ${e.localizedMessage}")
    }
}

// Función mejorada para actualizar un plan existente con coordenadas
private suspend fun updatePlanWithCoordinates(
    context: Context,
    planId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    title: String,
    description: String,
    location: String,
    latitude: Double?,
    longitude: Double?,
    dateInMillis: Long,
    timeString: String,
    existingImageUrls: List<String>,
    newImageUris: List<Uri>,
    enableWhatsapp: Boolean = false,
    phoneNumber: String = ""
): String = withContext(Dispatchers.IO) {
    try {
        val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
        Log.d("UpdatePlan", "🔄 Actualizando plan: $planId para usuario: $userId")
        Log.d("UpdatePlan", "📸 Imágenes existentes: ${existingImageUrls.size}, Nuevas: ${newImageUris.size}")

        // Verificar permisos con mejor manejo de errores
        val planDoc = try {
            db.collection("planes").document(planId).get().await()
        } catch (e: Exception) {
            Log.e("UpdatePlan", "❌ Error al obtener plan", e)
            throw Exception("No se pudo cargar el plan: ${e.localizedMessage}")
        }

        val currentPlan = planDoc.toObject(Plan::class.java)
            ?: throw Exception("Plan no encontrado")

        if (currentPlan.userId != userId) {
            throw Exception("No tienes permiso para editar este plan")
        }

        // Subir nuevas imágenes con manejo de errores mejorado
        val newImageUrls = if (newImageUris.isNotEmpty()) {
            try {
                uploadOptimizedImages(context, storage, userId, planId, newImageUris)
            } catch (e: Exception) {
                Log.e("UpdatePlan", "❌ Error al subir nuevas imágenes", e)
                // Continuar con las imágenes existentes si falla la subida
                emptyList()
            }
        } else {
            emptyList()
        }

        Log.d("UpdatePlan", "✅ Nuevas imágenes procesadas: ${newImageUrls.size}")

        // Combinar imágenes existentes con las nuevas
        val allImageUrls = existingImageUrls + newImageUrls

        val updatedAt = System.currentTimeMillis()
        val updatedPlan = Plan(
            id = planId,
            title = title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            date = dateInMillis,
            timeString = timeString,
            imageUrls = allImageUrls,
            userId = userId,
            createdAt = currentPlan.createdAt,
            updatedAt = updatedAt,
            enableWhatsapp = enableWhatsapp,
            phoneNumber = phoneNumber,
            likes = currentPlan.likes,
            participants = currentPlan.participants,
            shares = currentPlan.shares,
            commentCount = currentPlan.commentCount
        )

        // Actualizar con reintentos
        var retryCount = 0
        val maxRetries = 3


        while (retryCount < maxRetries) {
            try {
                db.collection("planes").document(planId).set(updatedPlan).await()
                Log.d("UpdatePlan", "✅ Plan actualizado en Firestore: $planId")
                break
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    throw e
                }
                Log.w("UpdatePlan", "⚠️ Reintentando actualizar plan ($retryCount/$maxRetries)")
                delay(1000L * retryCount)
            }
        }

        return@withContext planId

    } catch (e: Exception) {
        Log.e("UpdatePlan", "❌ Error al actualizar plan", e)
        throw Exception("Error al actualizar el plan: ${e.localizedMessage}")
    }
}

private suspend fun uploadOptimizedImages(
    context: Context,
    storage: FirebaseStorage,
    userId: String,
    planId: String,
    imageUris: List<Uri>
): List<String> = withContext(Dispatchers.IO) {
    try {
        Log.d("UploadImages", "🚀 INICIANDO SUBIDA DE ${imageUris.size} IMÁGENES")
        Log.d("UploadImages", "👤 Usuario: $userId")
        Log.d("UploadImages", "📋 Plan ID: $planId")

        if (imageUris.isEmpty()) {
            Log.w("UploadImages", "⚠️ Lista de imágenes está vacía")
            return@withContext emptyList()
        }

        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            try {
                Log.d("UploadImages", "📸 Procesando imagen $index: $uri")

                // Verificar que la URI sea válida
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e("UploadImages", "❌ No se puede abrir el InputStream para la URI: $uri")
                    return@forEachIndexed
                }
                inputStream.close()

                // Optimizar imagen
                val optimizedBitmap = optimizeImage(context, uri)
                Log.d("UploadImages", "✅ Imagen optimizada: ${optimizedBitmap.width}x${optimizedBitmap.height}")

                val baos = ByteArrayOutputStream()
                val compressionSuccess = optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)

                if (!compressionSuccess) {
                    Log.e("UploadImages", "❌ Error al comprimir imagen $index")
                    return@forEachIndexed
                }

                val data = baos.toByteArray()
                Log.d("UploadImages", "📦 Imagen comprimida: ${data.size} bytes")

                // ✅ RUTA CORREGIDA según las reglas de Firebase
                val fileName = "${System.currentTimeMillis()}_$index.jpg"
                val storagePath = "planes/$userId/$planId/$fileName"
                val storageRef = storage.reference.child(storagePath)

                Log.d("UploadImages", "☁️ Subiendo a Firebase: $storagePath")

                // Metadatos para mejorar la identificación del archivo
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("planId", planId)
                    .setCustomMetadata("userId", userId)
                    .setCustomMetadata("uploadTime", System.currentTimeMillis().toString())
                    .build()

                val uploadTask = storageRef.putBytes(data, metadata)

                // Esperar con timeout
                val uploadResult = withTimeout(30000) { // 30 segundos timeout
                    uploadTask.await()
                }

                Log.d("UploadImages", "✅ Upload completado para: $storagePath")

                // Obtener URL de descarga
                val downloadUrl = withTimeout(10000) { // 10 segundos timeout
                    storageRef.downloadUrl.await()
                }

                val urlString = downloadUrl.toString()
                Log.d("UploadImages", "🔗 URL obtenida: $urlString")

                uploadedUrls.add(urlString)
                Log.d("UploadImages", "✅ Imagen $index subida exitosamente")

                // Limpiar memoria
                optimizedBitmap.recycle()

            } catch (e: java.util.concurrent.TimeoutException) {
                Log.e("UploadImages", "⏰ Timeout al subir imagen $index", e)
            } catch (e: com.google.firebase.storage.StorageException) {
                Log.e("UploadImages", "🔥 Error de Storage al subir imagen $index: Código ${e.errorCode}", e)
                when (e.errorCode) {
                    com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED -> {
                        Log.e("UploadImages", "❌ Error de autorización - verificar autenticación y reglas")
                    }
                    com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED -> {
                        Log.e("UploadImages", "❌ Cuota de almacenamiento excedida")
                    }
                    com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> {
                        Log.e("UploadImages", "❌ Límite de reintentos excedido")
                    }
                }
            } catch (e: Exception) {
                Log.e("UploadImages", "❌ Error general al subir imagen $index: ${e.message}", e)
                e.printStackTrace()
            }
        }

        Log.d("UploadImages", "🎉 SUBIDA COMPLETADA: ${uploadedUrls.size}/${imageUris.size} imágenes")
        uploadedUrls.forEach { url ->
            Log.d("UploadImages", "🔗 URL final: $url")
        }

        return@withContext uploadedUrls
    } catch (e: Exception) {
        Log.e("UploadImages", "❌ Error general al subir imágenes: ${e.message}", e)
        e.printStackTrace()
        return@withContext emptyList()
    }
}

// 2. Función optimizada de imagen mejorada
private fun optimizeImage(context: Context, uri: Uri): Bitmap {
    Log.d("OptimizeImage", "🔧 Optimizando imagen: $uri")

    try {
        // Primero obtener las dimensiones sin cargar la imagen completa
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val inputStream1 = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream1, null, options)
        inputStream1?.close()

        Log.d("OptimizeImage", "📏 Dimensiones originales: ${options.outWidth}x${options.outHeight}")

        // Calcular factor de escala
        val maxDimension = 1024
        val scaleFactor = maxOf(
            options.outWidth.toFloat() / maxDimension,
            options.outHeight.toFloat() / maxDimension
        ).coerceAtLeast(1f)

        // Configurar opciones de decodificación
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scaleFactor.toInt()
            inPreferredConfig = Bitmap.Config.RGB_565 // Menos memoria
            inDither = false
            inPurgeable = true
        }

        val inputStream2 = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se puede abrir el archivo: $uri")

        val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            ?: throw Exception("No se puede decodificar la imagen: $uri")

        inputStream2.close()

        Log.d("OptimizeImage", "📏 Imagen final: ${bitmap.width}x${bitmap.height}")

        return bitmap

    } catch (e: Exception) {
        Log.e("OptimizeImage", "❌ Error optimizando imagen", e)
        throw e
    }
}