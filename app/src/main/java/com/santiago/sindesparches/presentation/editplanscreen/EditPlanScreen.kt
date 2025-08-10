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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateToHome: () -> Unit,
    planId: String? = null // Si no es null, estamos en modo edición
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    // Estado para almacenar los datos del plan
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var timeString by remember { mutableStateOf("12:00") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPlanData by remember { mutableStateOf(planId != null) }

    // Estados para WhatsApp
    var enableWhatsapp by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }

    // Si estamos en modo edición, cargamos los datos del plan
    LaunchedEffect(planId) {
        if (planId != null) {
            try {
                isLoadingPlanData = true
                val plan = getPlanById(db, planId)
                plan?.let {
                    title = it.title
                    description = it.description
                    location = it.location
                    dateInMillis = it.date
                    timeString = it.timeString
                    existingImageUrls = it.imageUrls
                    enableWhatsapp = it.enableWhatsapp
                    phoneNumber = it.phoneNumber
                }
            } catch (e: Exception) {
                Log.e("PublicacionScreen", "Error al cargar plan", e)
                snackbarHostState.showSnackbar("Error al cargar el plan: ${e.message}")
            } finally {
                isLoadingPlanData = false
            }
        }
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
                            onValueChange = { title = it },
                            label = {
                                Text(
                                    "Título del plan",
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
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Descripción
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = {
                                Text(
                                    "Descripción detallada",
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
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Ubicación
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = {
                                Text(
                                    "Ubicación del evento",
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
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }

                // Fecha y hora
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
                            // Selector de fecha
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
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = true,
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
                                trailingIcon = {
                                    IconButton(onClick = {
                                        // Aquí puedes implementar un DatePicker si lo deseas
                                    }) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Seleccionar fecha",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            // Selector de hora mejorado con TimePicker
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
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = true,
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
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            // Obtener hora y minuto actuales del timeString
                                            val timeParts = timeString.split(":")
                                            val currentHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 12
                                            val currentMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                                            // Mostrar TimePickerDialog
                                            TimePickerDialog(
                                                context,
                                                { _, selectedHour, selectedMinute ->
                                                    timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                                                },
                                                currentHour,
                                                currentMinute,
                                                true // formato 24 horas
                                            ).show()
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.bx_time),
                                            contentDescription = "Seleccionar hora",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // Sección de contacto por WhatsApp mejorada
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

                        // Switch para habilitar/deshabilitar WhatsApp
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
                                onCheckedChange = { enableWhatsapp = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF25D366),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        // Campo de número de teléfono (solo se muestra si WhatsApp está habilitado)
                        if (enableWhatsapp) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = {
                                    Text(
                                        "Número de WhatsApp",
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
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = Color(0xFF25D366),
                                    focusedIndicatorColor = Color(0xFF25D366),
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // Sección de imágenes
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

                                        // Botón para eliminar imagen
                                        IconButton(
                                            onClick = {
                                                existingImageUrls = existingImageUrls.filter { it != imageUrl }
                                            },
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

                                        // Botón para eliminar imagen
                                        IconButton(
                                            onClick = {
                                                selectedImageUris = selectedImageUris.filter { it != uri }
                                            },
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

                        // Botón para agregar imágenes mejorado
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
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

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para guardar el plan
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (validateForm(title, description, location)) {
                                // Validar número de teléfono si WhatsApp está habilitado
                                if (enableWhatsapp && phoneNumber.isBlank()) {
                                    snackbarHostState.showSnackbar("Por favor ingresa un número de WhatsApp")
                                    return@launch
                                }

                                try {
                                    isLoading = true
                                    Log.d("PublicacionScreen", "Iniciando guardado. PlanId: $planId")

                                    if (planId == null) {
                                        // Crear nuevo plan
                                        val newPlanId = createPlan(
                                            context = context,
                                            auth = auth,
                                            db = db,
                                            storage = storage,
                                            title = title,
                                            description = description,
                                            location = location,
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
                                        updatePlan(
                                            context = context,
                                            planId = planId,
                                            auth = auth,
                                            db = db,
                                            storage = storage,
                                            title = title,
                                            description = description,
                                            location = location,
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

                                    // Esperar un poco para mostrar el mensaje y navegar
                                    delay(1500)
                                    navigateToHome()

                                } catch (e: Exception) {
                                    Log.e("PublicacionScreen", "Error al guardar plan", e)
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            } else {
                                snackbarHostState.showSnackbar("Por favor completa todos los campos")
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

                // Espaciado final para mejor UX
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Función para validar el formulario
private fun validateForm(
    title: String,
    description: String,
    location: String
): Boolean {
    return title.isNotBlank() && description.isNotBlank() && location.isNotBlank()
}

// Función para crear un nuevo plan
private suspend fun createPlan(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    title: String,
    description: String,
    location: String,
    dateInMillis: Long,
    timeString: String,
    selectedImageUris: List<Uri>,
    enableWhatsapp: Boolean = false,
    phoneNumber: String = ""
): String = withContext(Dispatchers.IO) {
    try {
        val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
        Log.d("CreatePlan", "Creando plan para usuario: $userId")

        // Generar ID del plan antes de subir imágenes
        val planId = UUID.randomUUID().toString()

        // Subir imágenes si hay alguna seleccionada (ahora con planId)
        val imageUrls = uploadOptimizedImages(context, storage, userId, planId, selectedImageUris)
        Log.d("CreatePlan", "Imágenes subidas: ${imageUrls.size}")

        // Crear documento de plan
        val createdAt = System.currentTimeMillis()

        val plan = Plan(
            id = planId,
            title = title,
            description = description,
            location = location,
            date = dateInMillis,
            timeString = timeString,
            imageUrls = imageUrls,
            userId = userId,
            createdAt = createdAt,
            enableWhatsapp = enableWhatsapp,
            phoneNumber = phoneNumber
        )

        // Guardar en Firestore
        db.collection("planes").document(planId).set(plan).await()
        Log.d("CreatePlan", "Plan guardado en Firestore: $planId")

        return@withContext planId
    } catch (e: Exception) {
        Log.e("CreatePlan", "Error al crear plan", e)
        throw e
    }
}

// Función para actualizar un plan existente
private suspend fun updatePlan(
    context: Context,
    planId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    title: String,
    description: String,
    location: String,
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

        // Verificar que el usuario sea el propietario del plan
        val planDoc = db.collection("planes").document(planId).get().await()
        val currentPlan = planDoc.toObject(Plan::class.java)

        if (currentPlan?.userId != userId) {
            throw Exception("No tienes permiso para editar este plan")
        }

        // Subir nuevas imágenes si hay alguna seleccionada (ahora con planId)
        val newImageUrls = uploadOptimizedImages(context, storage, userId, planId, newImageUris)
        Log.d("UpdatePlan", "Nuevas imágenes subidas: ${newImageUrls.size}")

        // Combinar imágenes existentes y nuevas
        val allImageUrls = existingImageUrls + newImageUrls

        // Crear el plan actualizado con nueva fecha de edición
        val updatedAt = System.currentTimeMillis()
        val updatedPlan = Plan(
            id = planId,
            title = title,
            description = description,
            location = location,
            date = dateInMillis,
            timeString = timeString,
            imageUrls = allImageUrls,
            userId = userId,
            createdAt = currentPlan.createdAt,
            updatedAt = updatedAt, // Nueva fecha de edición
            enableWhatsapp = enableWhatsapp,
            phoneNumber = phoneNumber
        )

        // Actualizar en Firestore
        db.collection("planes").document(planId).set(updatedPlan).await()
        Log.d("UpdatePlan", "Plan actualizado en Firestore: $planId")

        return@withContext planId
    } catch (e: Exception) {
        Log.e("UpdatePlan", "Error al actualizar plan", e)
        throw e
    }
}

// Función optimizada para subir imágenes a Firebase Storage
private suspend fun uploadOptimizedImages(
    context: Context,
    storage: FirebaseStorage,
    userId: String,
    planId: String, // Agregado parámetro planId
    imageUris: List<Uri>
): List<String> = withContext(Dispatchers.IO) {
    val imageUrls = mutableListOf<String>()

    for (uri in imageUris) {
        try {
            // Optimizar imagen antes de subirla
            val optimizedImageBytes = optimizeImage(context, uri)

            val timestamp = System.currentTimeMillis()
            val fileName = "${timestamp}_${UUID.randomUUID()}.jpg"
            // Ruta actualizada según las reglas de Firebase Storage
            val storageRef = storage.reference.child("planes/$userId/$planId/$fileName")

            Log.d("UploadImages", "Subiendo imagen optimizada: planes/$userId/$planId/$fileName")

            // Subir bytes optimizados
            val uploadTask = storageRef.putBytes(optimizedImageBytes).await()
            val downloadUrl = storageRef.downloadUrl.await()

            imageUrls.add(downloadUrl.toString())
            Log.d("UploadImages", "Imagen subida exitosamente: $downloadUrl")
        } catch (e: Exception) {
            Log.e("UploadImages", "Error al subir imagen", e)
            // Continuar con la siguiente imagen en caso de error
        }
    }

    return@withContext imageUrls
}

// Función para optimizar imágenes usando solo APIs nativas
private suspend fun optimizeImage(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // Redimensionar si es muy grande
        val maxWidth = 1200
        val maxHeight = 1200
        val scaledBitmap = if (originalBitmap.width > maxWidth || originalBitmap.height > maxHeight) {
            val ratio = minOf(
                maxWidth.toFloat() / originalBitmap.width,
                maxHeight.toFloat() / originalBitmap.height
            )
            val newWidth = (originalBitmap.width * ratio).toInt()
            val newHeight = (originalBitmap.height * ratio).toInt()

            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        // Convertir a JPEG con compresión (más compatible que WebP)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

        // Limpiar memoria
        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()

        return@withContext outputStream.toByteArray()
    } catch (e: Exception) {
        Log.e("OptimizeImage", "Error al optimizar imagen", e)
        throw e
    }
}

// Función adicional para eliminar imágenes de Firebase Storage (opcional)
private suspend fun deleteImageFromStorage(
    storage: FirebaseStorage,
    imageUrl: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val storageRef = storage.getReferenceFromUrl(imageUrl)
        storageRef.delete().await()
        Log.d("DeleteImage", "Imagen eliminada exitosamente: $imageUrl")
        return@withContext true
    } catch (e: Exception) {
        Log.e("DeleteImage", "Error al eliminar imagen", e)
        return@withContext false
    }
}

// Función para obtener un plan por su ID
suspend fun getPlanById(db: FirebaseFirestore, planId: String): Plan? = withContext(Dispatchers.IO) {
    try {
        Log.d("GetPlanById", "Obteniendo plan: $planId")
        val document = db.collection("planes").document(planId).get().await()
        val plan = document.toObject(Plan::class.java)
        Log.d("GetPlanById", "Plan obtenido: ${plan?.title}")
        return@withContext plan
    } catch (e: Exception) {
        Log.e("GetPlanById", "Error al obtener plan", e)
        throw e
    }
}

// Función para mostrar cuándo fue creado o editado un plan
fun getPublicationDateText(plan: Plan): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())

    return if (plan.updatedAt != null && plan.updatedAt > plan.createdAt) {
        "Editado el ${dateFormat.format(Date(plan.updatedAt))}"
    } else {
        "Publicado el ${dateFormat.format(Date(plan.createdAt))}"
    }
}

// Función para mostrar tiempo relativo (hace X tiempo)
fun getRelativeTimeText(plan: Plan): String {
    val now = System.currentTimeMillis()
    val targetTime = plan.updatedAt ?: plan.createdAt
    val diff = now - targetTime

    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    val timeText = when {
        minutes < 1 -> "hace un momento"
        minutes < 60 -> "hace ${minutes}m"
        hours < 24 -> "hace ${hours}h"
        days < 7 -> "hace ${days}d"
        else -> {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.format(Date(targetTime))
        }
    }

    val editedText = if (plan.updatedAt != null && plan.updatedAt > plan.createdAt) {
        "Editado $timeText"
    } else {
        "Publicado $timeText"
    }

    return editedText
}

// Función para formatear fecha de timestamp a formato legible
private fun formatDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(calendar.time)
}