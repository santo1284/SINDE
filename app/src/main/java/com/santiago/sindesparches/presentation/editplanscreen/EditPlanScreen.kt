package com.santiago.sindesparches.presentation.editplanscreen

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPlanData by remember { mutableStateOf(planId != null) }

    // Estados para WhatsApp
    var enableWhatsapp by rememberSaveable { mutableStateOf(false) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    // Variables reactivas ubicación
    // Variables reactivas ubicación (mantén estas como están)
    var locationAddress by rememberSaveable { mutableStateOf("") }
    var locationLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var locationLng by rememberSaveable { mutableStateOf<Double?>(null) }

// Agregar un estado para forzar recomposición
    var locationUpdateTrigger by remember { mutableStateOf(0) }

// Observer que se ejecuta cuando regresa de navegación
    LaunchedEffect(locationUpdateTrigger, navController.currentBackStackEntry) {
        delay(100) // Pequeña demora para asegurar que la navegación se complete

        val currentEntry = navController.currentBackStackEntry
        val previousEntry = navController.previousBackStackEntry

        Log.d("PublicacionScreen", "🔄 Checking navigation state...")
        Log.d("PublicacionScreen", "   Current: ${currentEntry?.destination?.route}")
        Log.d("PublicacionScreen", "   Previous: ${previousEntry?.destination?.route}")

        // Buscar datos en ambas entradas
        listOf(currentEntry, previousEntry).forEach { entry ->
            entry?.savedStateHandle?.let { handle ->
                val address = handle.get<String>("location_address")
                val lat = handle.get<Double>("location_lat")
                val lng = handle.get<Double>("location_lng")

                if (address != null && lat != null && lng != null) {
                    Log.d("PublicacionScreen", "✅ Ubicación encontrada en ${entry.destination.route}")
                    Log.d("PublicacionScreen", "   Address: $address")
                    Log.d("PublicacionScreen", "   Coordinates: $lat, $lng")

                    // Actualizar solo si es diferente
                    if (locationAddress != address) {
                        locationAddress = address
                        locationLat = lat
                        locationLng = lng

                        // Limpiar para evitar reutilización
                        handle.remove<String>("location_address")
                        handle.remove<Double>("location_lat")
                        handle.remove<Double>("location_lng")

                        Log.d("PublicacionScreen", "🎉 UI actualizada con nueva ubicación")
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    // Aquí tu UI usando locationAddress, locationLat, locationLng
    Column {
        Text(text = "Dirección: ${locationAddress ?: "Sin ubicación"}")
        Text(text = "Latitud: ${locationLat ?: "N/A"}")
        Text(text = "Longitud: ${locationLng ?: "N/A"}")
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

    // Función de navegación al mapa
    val navigateToMapWithPreservation = {
        Log.d("PublicacionScreen", "🚀 Navegando al mapa...")
        locationUpdateTrigger += 1 // Incrementar para forzar recomposición al regresar
        navigateToMapPicker()
    }

    // Selector de imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImageUris = selectedImageUris + uris
    }

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
                    IconButton(onClick = navigateToHome) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                    onTitleChange = { title = it },
                    onDescriptionChange = { description = it },
                    onLocationClick = navigateToMapWithPreservation,
                    context = context
                )

                // Fecha y hora
                DateTimeCard(
                    dateInMillis = dateInMillis,
                    timeString = timeString,
                    onDateChange = { newDateInMillis ->  // <-- AGREGAR ESTA LÍNEA
                        dateInMillis = newDateInMillis
                    },
                    onTimeChange = { timeString = it },
                    context = context
                )

                // Contacto por WhatsApp
                WhatsAppContactCard(
                    enableWhatsapp = enableWhatsapp,
                    phoneNumber = phoneNumber,
                    onEnableWhatsappChange = { enableWhatsapp = it },
                    onPhoneNumberChange = { phoneNumber = it }
                )

                // Imágenes del plan
                ImagesCard(
                    existingImageUrls = existingImageUrls,
                    selectedImageUris = selectedImageUris,
                    onRemoveExistingImage = { imageUrl ->
                        existingImageUrls = existingImageUrls.filter { it != imageUrl }
                    },
                    onRemoveSelectedImage = { uri ->
                        selectedImageUris = selectedImageUris.filter { it != uri }
                    },
                    onAddImages = { imagePickerLauncher.launch("image/*") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para guardar el plan
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (validateForm(title, description, locationAddress, enableWhatsapp, phoneNumber)) {
                                try {
                                    isLoading = true
                                    Log.d("PublicacionScreen", "Iniciando guardado. PlanId: $planId")

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
                                        Log.d("PublicacionScreen", "Plan creado con ID: $newPlanId")
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
                                        Log.d("PublicacionScreen", "Plan actualizado: $planId")
                                        snackbarHostState.showSnackbar("Plan actualizado correctamente")
                                    }

                                    delay(1500)
                                    navigateToHome()

                                } catch (e: Exception) {
                                    Log.e("PublicacionScreen", "Error al guardar plan", e)
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
}

// Componentes separados para mejor organización
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
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                color = MaterialTheme.colorScheme.onSurface
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                context = context
            )
        }
    }
}

@Composable
private fun DateTimeCard(
    dateInMillis: Long,
    timeString: String,
    onDateChange: (Long) -> Unit, // Agregar esta función
    onTimeChange: (String) -> Unit,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de fecha - USANDO BOX PARA INTERCEPTAR CLICKS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // Mostrar DatePickerDialog
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

                            // Configurar fecha mínima (hoy)
                            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
                            datePickerDialog.show()
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
                        enabled = false, // Deshabilitado para que no tome focus
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }

                // Selector de hora - USANDO BOX PARA INTERCEPTAR CLICKS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // Mostrar TimePickerDialog
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
                                true // Formato 24 horas
                            ).show()
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
                        enabled = false, // Deshabilitado para que no tome focus
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.bx_time),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "El evento será el $dayOfWeek a las $timeString",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
    onPhoneNumberChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enableWhatsapp)
                Color(0xFF25D366).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                color = MaterialTheme.colorScheme.onSurface
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
                        tint = Color(0xFF25D366),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (enableWhatsapp) "Los usuarios podrán contactarte" else "Función desactivada",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enableWhatsapp,
                    onCheckedChange = onEnableWhatsappChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF25D366),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = Color(0xFF25D366),
                        focusedBorderColor = Color(0xFF25D366),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onAddImages: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                color = MaterialTheme.colorScheme.onSurface
            )

            // Imágenes existentes (solo en modo edición)
            if (existingImageUrls.isNotEmpty()) {
                Text(
                    text = "Imágenes actuales",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Imagen del plan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

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

            // Imágenes seleccionadas nuevas
            if (selectedImageUris.isNotEmpty()) {
                Text(
                    text = "Nuevas imágenes seleccionadas",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Imagen seleccionada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

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

            // Botón para agregar imágenes
            Button(
                onClick = onAddImages,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Agregar imágenes",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
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
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
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
                    tint = accentSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ubicación del evento *",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                            accentSecondary
                        } else {
                            Color.Green
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

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

            Spacer(modifier = Modifier.height(12.dp))

            if (locationAddress.isNotEmpty() && locationAddress != "Seleccionar ubicación") {
                Text(
                    text = locationAddress,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (locationLat != null && locationLng != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%.6f", locationLat)}, ${String.format("%.6f", locationLng)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "Agregar ubicación",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para seleccionar dónde será tu evento",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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

// Función para crear un nuevo plan con coordenadas
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
        Log.d("CreatePlan", "Creando plan para usuario: $userId")

        val planId = UUID.randomUUID().toString()

        // Subir imágenes optimizadas
        val imageUrls = uploadOptimizedImages(context, storage, userId, planId, selectedImageUris)
        Log.d("CreatePlan", "Imágenes subidas: ${imageUrls.size}")

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

        db.collection("planes").document(planId).set(plan).await()
        Log.d("CreatePlan", "Plan guardado en Firestore: $planId")

        return@withContext planId
    } catch (e: Exception) {
        Log.e("CreatePlan", "Error al crear plan", e)
        throw e
    }
}

// Función para actualizar un plan existente con coordenadas
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
        Log.d("UpdatePlan", "Actualizando plan: $planId para usuario: $userId")

        // Verificar permisos
        val planDoc = db.collection("planes").document(planId).get().await()
        val currentPlan = planDoc.toObject(Plan::class.java)

        if (currentPlan?.userId != userId) {
            throw Exception("No tienes permiso para editar este plan")
        }

        // Subir nuevas imágenes
        val newImageUrls = uploadOptimizedImages(context, storage, userId, planId, newImageUris)
        Log.d("UpdatePlan", "Nuevas imágenes subidas: ${newImageUrls.size}")

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

        db.collection("planes").document(planId).set(updatedPlan).await()
        Log.d("UpdatePlan", "Plan actualizado en Firestore: $planId")

        return@withContext planId
    } catch (e: Exception) {
        Log.e("UpdatePlan", "Error al actualizar plan", e)
        throw e
    }
}

// Función para subir imágenes optimizadas
private suspend fun uploadOptimizedImages(
    context: Context,
    storage: FirebaseStorage,
    userId: String,
    planId: String,
    imageUris: List<Uri>
): List<String> = withContext(Dispatchers.IO) {
    try {
        if (imageUris.isEmpty()) return@withContext emptyList()

        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            try {
                // Optimizar imagen
                val optimizedBitmap = optimizeImage(context, uri)
                val baos = ByteArrayOutputStream()
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val data = baos.toByteArray()

                // Subir a Firebase Storage
                val fileName = "${userId}_${planId}_${System.currentTimeMillis()}_$index.jpg"
                val storageRef = storage.reference.child("plan_images/$fileName")

                val uploadTask = storageRef.putBytes(data).await()
                val downloadUrl = storageRef.downloadUrl.await()

                uploadedUrls.add(downloadUrl.toString())
                Log.d("UploadImages", "Imagen subida: $fileName")

            } catch (e: Exception) {
                Log.e("UploadImages", "Error al subir imagen en posición $index", e)
            }
        }

        return@withContext uploadedUrls
    } catch (e: Exception) {
        Log.e("UploadImages", "Error general al subir imágenes", e)
        return@withContext emptyList()
    }
}

// Función para optimizar imágenes
private fun optimizeImage(context: Context, uri: Uri): Bitmap {
    val inputStream = context.contentResolver.openInputStream(uri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    // Calcular nuevo tamaño manteniendo la proporción
    val maxDimension = 1024
    val width = originalBitmap.width
    val height = originalBitmap.height

    val scaleFactor = if (width > height) {
        maxDimension.toFloat() / width
    } else {
        maxDimension.toFloat() / height
    }

    val newWidth = (width * scaleFactor).toInt()
    val newHeight = (height * scaleFactor).toInt()

    return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
}