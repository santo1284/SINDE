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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

    // Estados para los campos del formulario
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("Seleccionar ubicación") }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }

    // Estados para animaciones
    var isFormVisible by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(100)
        isFormVisible = true
    }

    // Observer para el resultado del MapPickerScreen
    val lifecycleOwner = LocalLifecycleOwner.current
    val navBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = navBackStackEntry?.savedStateHandle

    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<String>("location_address")?.observe(lifecycleOwner) { address ->
            locationAddress = address
            savedStateHandle.remove<String>("location_address")
        }
        savedStateHandle?.getLiveData<Double>("location_lat")?.observe(lifecycleOwner) { lat ->
            locationLat = lat
            savedStateHandle.remove<Double>("location_lat")
        }
        savedStateHandle?.getLiveData<Double>("location_lng")?.observe(lifecycleOwner) { lng ->
            locationLng = lng
            savedStateHandle.remove<Double>("location_lng")
        }
    }

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
            val remainingSlots = 5 - selectedImages.size
            if (remainingSlots > 0) {
                val newImages = uris.take(remainingSlots)
                selectedImages.addAll(newImages)
            }
        }
    }

    // Estados para WhatsApp
    var enableWhatsapp by remember { mutableStateOf(false) }
    val currentUserId = auth.currentUser?.uid ?: ""
    var phoneNumber by remember { mutableStateOf("") }
    var isLoadingNumero by remember { mutableStateOf(true) }

    // Cargar número de teléfono
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
                            if (formHasContent(title, description, locationAddress, selectedDate, selectedImages)) {
                                showExitDialog = true
                            } else {
                                navigateToHome()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
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
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Indicador de progreso
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val progress = when {
                        title.isNotBlank() && description.isNotBlank() && location.isNotBlank()
                                && selectedDate != null && selectedTime.isNotBlank() && selectedImages.isNotEmpty() -> 1f
                        title.isNotBlank() && description.isNotBlank() && location.isNotBlank()
                                && selectedDate != null && selectedTime.isNotBlank() -> 0.8f
                        title.isNotBlank() && description.isNotBlank() && location.isNotBlank() -> 0.6f
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

                // Título del evento
                AnimatedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nombre del evento",
                    icon = Icons.Default.Star,
                    isVisible = isFormVisible,
                    delay = 100,
                    primaryColor = primaryColor,
                    surfaceColor = surfaceLight
                )

                // Descripción
                AnimatedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "¿Qué tienes planeado?",
                    icon = Icons.Default.Create,
                    isVisible = isFormVisible,
                    delay = 200,
                    primaryColor = primaryColor,
                    surfaceColor = surfaceLight,
                    multiline = true,
                    maxLines = 4
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
                            .clickable { showDatePicker = true },
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
                                tint = primaryColor,
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
                            .clickable { showTimePicker = true },
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
                                tint = accentColor,
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
                LocationDisplay(
                    locationAddress = locationAddress,
                    onClick = navigateToMapPicker,
                    accentSecondary = accentSecondary,
                    surfaceLight = surfaceLight
                )

                // Sección de imágenes
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Imágenes del evento",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${selectedImages.size}/5",
                                color = primaryColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(selectedImages) { uri ->
                                ImageCard(
                                    uri = uri,
                                    onRemove = { selectedImages.remove(uri) }
                                )
                            }

                            if (selectedImages.size < 5) {
                                item {
                                    AddImageCard(
                                        onClick = { galleryLauncher.launch("image/*") }
                                    )
                                }
                            }
                        }

                        if (selectedImages.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Agrega al menos una imagen para mostrar tu evento",
                                color = accentSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Sección de WhatsApp
                WhatsAppSection(
                    enableWhatsapp = enableWhatsapp,
                    onWhatsAppToggle = { enableWhatsapp = it },
                    phoneNumber = phoneNumber,
                    isLoadingNumero = isLoadingNumero,
                    surfaceColor = surfaceDark
                )

                // Botón de publicar
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (validateForm(title, description, locationAddress, selectedDate, selectedImages, snackbarHostState)) {
                                isLoading = true
                                try {
                                    // Lógica de publicación aquí
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
                                            location = locationAddress,
                                            latitude = locationLat,
                                            longitude = locationLng,
                                            imageUrls = imageUrls,
                                            enableWhatsapp = enableWhatsapp,
                                            phoneNumber = if (enableWhatsapp) phoneNumber else ""
                                        )

                                        savePlanToFirestore(db, plan)
                                        snackbarHostState.showSnackbar("¡Evento publicado con éxito!")
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && selectedImages.isNotEmpty() && title.isNotBlank() &&
                            description.isNotBlank() && locationAddress != "Seleccionar ubicación" && selectedDate != null && selectedTime.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brush.horizontalGradient(
                            colors = listOf(primaryColor, accentColor)
                        ).let { primaryColor }, // Fallback para el gradient
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
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

                // Espacio final
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            selectedDate = dateMillis
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formattedDate = sdf.format(Date(dateMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = Color.Gray)
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
                    todayDateBorderColor = primaryColor
                )
            )
        }
    }

    // TimePicker Dialog
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
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
                            onClick = { showTimePicker = false }
                        ) {
                            Text("Cancelar", color = Color.Gray)
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
                            }
                        ) {
                            Text("Confirmar", color = primaryColor)
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
                    }
                ) {
                    Text("Salir", color = accentSecondary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun LocationDisplay(
    locationAddress: String,
    onClick: () -> Unit,
    accentSecondary: Color,
    surfaceLight: Color
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(300)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = surfaceLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    tint = accentSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = locationAddress,
                    color = if (locationAddress == "Seleccionar ubicación") Color.Gray else Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isVisible: Boolean,
    delay: Long,
    primaryColor: Color,
    surfaceColor: Color,
    multiline: Boolean = false,
    maxLines: Int = 1
) {
    var isFieldVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(delay)
            isFieldVisible = true
        }
    }

    AnimatedVisibility(
        visible = isFieldVisible,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(300)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, color = Color.Gray) },
                leadingIcon = {
                    Icon(icon, contentDescription = null, tint = primaryColor)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (multiline) it.heightIn(min = 120.dp) else it },
                singleLine = !multiline,
                maxLines = maxLines,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = primaryColor,
                    focusedIndicatorColor = primaryColor,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun ImageCard(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Imagen del evento",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
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

@Composable
fun AddImageCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Agregar imagen",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun WhatsAppSection(
    enableWhatsapp: Boolean,
    onWhatsAppToggle: (Boolean) -> Unit,
    phoneNumber: String,
    isLoadingNumero: Boolean,
    surfaceColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.bxl_whatsapp),
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF25D366)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Contacto por WhatsApp",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingNumero) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF25D366)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cargando número...",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    text = "Tu número: $phoneNumber",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = enableWhatsapp,
                        onCheckedChange = onWhatsAppToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF25D366),
                            checkedTrackColor = Color(0xFF25D366).copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (enableWhatsapp)
                            "Los usuarios pueden contactarte por WhatsApp"
                        else
                            "Permitir contacto por WhatsApp",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
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