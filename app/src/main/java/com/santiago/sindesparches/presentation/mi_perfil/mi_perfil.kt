package com.santiago.sindesparches.presentation.mi_perfil

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.presentation.home.PlanCard
import com.santiago.sindesparches.data.models.Plan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data classes para los modelos
data class PerfilUsuario(
    val nombre: String = "",
    val celular: String = "",
    val edad: Int = 0,
    val ciudad: String = "",
    val terminosAceptados: Boolean = false,
    val fechaAceptacionTerminos: Any? = null
)

data class UsuarioBasico(
    val uid: String = "",
    val nombre: String = "",
    val imageUrl: String? = null
)

data class FlashPlan(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val hora: String = "",
    val lugar: String = "",
    val imageUrl: String? = null,
    val fechaCreacion: Any? = null
)

@Composable
fun MiPerfilScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigatehome: () -> Unit,
    onLogout: () -> Unit = {},
    navigateToPlanDetail: (String) -> Unit,
    navigateToProfile: (String) -> Unit,
    navigateToMiPerfil: () -> Unit,
    navigateToComments: (String) -> Unit,
    navigateToEditPlan: (String) -> Unit
) {
    var perfilUsuario by remember { mutableStateOf<PerfilUsuario?>(null) }
    var flashPlans by remember { mutableStateOf<List<FlashPlan>>(emptyList()) }
    var planes by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado para el diálogo de imagen expandida
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }

    val currentUserId = auth.currentUser?.uid ?: ""
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Estados para edición con validación mejorada
    var editNombre by remember { mutableStateOf("") }
    var editCelular by remember { mutableStateOf("") }
    var editEdad by remember { mutableStateOf<Int?>(null) }
    var editCiudad by remember { mutableStateOf("") }
    var nombreError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val userId = auth.currentUser?.uid

    // Variables necesarias para PlanCard
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ✅ Función para recargar todos los datos
    fun reloadAllData() {
        userId?.let { uid ->
            // Recargar FlashPlans
            db.collection("flashPlans")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("MiPerfil", "FlashPlans recargados: ${documents.size()}")
                    flashPlans = documents.map { doc ->
                        val flashPlan = doc.toObject(FlashPlan::class.java).copy(id = doc.id)
                        Log.d("MiPerfil", "FlashPlan: ${flashPlan.titulo}, imageUrl: ${flashPlan.imageUrl}")
                        flashPlan
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MiPerfil", "Error recargando FlashPlans", e)
                }

            // Recargar Planes
            db.collection("planes")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("MiPerfil", "Planes recargados: ${documents.size()}")
                    val planesList = documents.map { doc ->
                        val data = doc.data

                        val plan = Plan(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            createdAt = when (val createdAt = data["createdAt"] ?: data["fechaCreacion"]) {
                                is Long -> createdAt
                                is com.google.firebase.Timestamp -> createdAt.toDate().time
                                else -> System.currentTimeMillis()
                            },
                            title = (data["title"] as? String) ?: (data["titulo"] as? String) ?: "",
                            description = (data["description"] as? String) ?: (data["descripcion"] as? String) ?: "",
                            date = when (val dateValue = data["date"] ?: data["fecha"]) {
                                is Long -> dateValue
                                is String -> {
                                    try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            .parse(dateValue)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }
                                }
                                else -> System.currentTimeMillis()
                            },
                            timeString = (data["timeString"] as? String) ?: (data["hora"] as? String) ?: "",
                            location = (data["location"] as? String) ?: (data["lugar"] as? String) ?: "",
                            imageUrls = when {
                                data["imageUrls"] is List<*> -> (data["imageUrls"] as List<*>).mapNotNull { it as? String }
                                data["imageUrl"] is String -> listOfNotNull(data["imageUrl"] as String)
                                else -> emptyList()
                            },
                            enableWhatsapp = data["enableWhatsapp"] as? Boolean ?: false,
                            phoneNumber = data["phoneNumber"] as? String ?: "",
                            likes = (data["likes"] as? List<*>)?.mapNotNull { it as? String } as List<String>,
                            participants = (data["participants"] as? List<*>)?.mapNotNull { it as? String } as List<String>,
                            shares = (data["shares"] as? Long)?.toInt() ?: 0,
                            commentCount = (data["commentCount"] as? Long)?.toInt() ?: 0
                        )

                        Log.d("MiPerfil", "Plan: ${plan.title}, userId: ${plan.userId}, imageUrls: ${plan.imageUrls}")
                        plan
                    }
                    planes = planesList.sortedByDescending { (it.createdAt ?: 0L) as Comparable<Any> }
                }
                .addOnFailureListener { e ->
                    Log.e("MiPerfil", "Error recargando Planes", e)
                }
        }
    }

    // ✅ Función para manejar eliminación de plan con recarga automática
    fun handlePlanDeleted(planId: String) {
        // Actualizar la lista local inmediatamente
        planes = planes.filter { it.id != planId }

        // Mostrar mensaje de confirmación
        Toast.makeText(
            context,
            "Plan eliminado exitosamente",
            Toast.LENGTH_SHORT
        ).show()

        // Recargar datos para asegurar consistencia
        reloadAllData()
    }

    // Animaciones
    val profileImageScale by animateFloatAsState(
        targetValue = if (isEditing) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "profileImageScale"
    )

    // Colores modernos y atractivos
    val primaryPurple = Color(0xFF8B5CF6)
    val primaryBlue = Color(0xFF3B82F6)
    val accentPink = Color(0xFFEC4899)
    val accentOrange = Color(0xFFF97316)
    val darkBackground = Color(0xFF0F0F23)
    val cardBackground = Color(0xFF1E1E3F)

    // Launcher para seleccionar imagen
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // Cargar datos del perfil
    LaunchedEffect(userId) {
        userId?.let { uid ->
            Log.d("MiPerfil", "Cargando datos para usuario: $uid")

            // Cargar perfil
            db.collection("perfil").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("MiPerfil", "Perfil encontrado: ${document.exists()}")
                    if (document.exists()) {
                        perfilUsuario = document.toObject(PerfilUsuario::class.java)
                        perfilUsuario?.let { perfil ->
                            editNombre = perfil.nombre
                            editCelular = perfil.celular
                            editEdad = perfil.edad
                            editCiudad = perfil.ciudad
                            Log.d("MiPerfil", "Perfil cargado: ${perfil.nombre}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MiPerfil", "Error cargando perfil", e)
                }

            // Cargar imagen de perfil
            isLoadingImage = true
            FirebaseStorage.getInstance().reference
                .child("profile_pictures/$uid")
                .downloadUrl
                .addOnSuccessListener { url ->
                    profileImageUrl = url.toString()
                    isLoadingImage = false
                    Log.d("MiPerfil", "Imagen de perfil encontrada: $url")
                }
                .addOnFailureListener { e ->
                    isLoadingImage = false
                    Log.d("MiPerfil", "No se encontró imagen de perfil: ${e.message}")
                }

            // Cargar datos
            reloadAllData()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(darkBackground, cardBackground)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = primaryPurple,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cargando tu perfil...",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = darkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            darkBackground,
                            cardBackground.copy(alpha = 0.3f),
                            darkBackground
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ✅ Header mejorado sin botón de recarga y mejor centrado
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        // Fondo del header con gradiente
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            primaryPurple.copy(alpha = 0.95f),
                                            primaryBlue.copy(alpha = 0.9f),
                                        )
                                    )
                                )
                        ) {
                            // Efectos decorativos de fondo
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                drawCircle(
                                    color = Color.White.copy(alpha = 0.05f),
                                    radius = 120f,
                                    center = Offset(canvasWidth * 0.2f, canvasHeight * 0.3f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.03f),
                                    radius = 80f,
                                    center = Offset(canvasWidth * 0.8f, canvasHeight * 0.7f)
                                )
                            }

                            // ✅ Botones del header reorganizados
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón volver
                                IconButton(
                                    onClick = navigatehome,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Volver",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // ✅ Solo dos botones: editar y logout
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            isEditing = if (isEditing) {
                                                perfilUsuario?.let { perfil ->
                                                    editNombre = perfil.nombre
                                                    editCelular = perfil.celular
                                                    editEdad = perfil.edad
                                                    editCiudad = perfil.ciudad
                                                }
                                                nombreError = false
                                                imageUri = null
                                                false
                                            } else {
                                                true
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (isEditing) accentOrange.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                if (isEditing) accentOrange.copy(alpha = 0.4f)
                                                else Color.White.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                            contentDescription = if (isEditing) "Cancelar" else "Editar",
                                            tint = if (isEditing) accentOrange else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showLogoutDialog = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                Color.White.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            contentDescription = "Cerrar sesión",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // Título del header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 20.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "MI PERFIL",
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.4f),
                                                offset = Offset(0f, 4f),
                                                blurRadius = 8f
                                            )
                                        )
                                    )

                                    // Línea decorativa
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(3.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        accentOrange,
                                                        Color.Transparent
                                                    )
                                                ),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }

                        // Foto de perfil moderna flotante
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = 40.dp)
                        ) {
                            // Sombra de la foto
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .offset(y = 8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .blur(12.dp)
                            )

                            // Contenedor principal de la foto
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(profileImageScale)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                accentPink.copy(alpha = 0.8f),
                                                primaryPurple.copy(alpha = 0.9f),
                                                accentOrange.copy(alpha = 0.7f)
                                            ),
                                            radius = 250f
                                        ),
                                        CircleShape
                                    )
                                    .padding(4.dp)
                                    .clickable(enabled = isEditing) {
                                        if (isEditing) launcher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(cardBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        imageUri != null -> {
                                            AsyncImage(
                                                model = imageUri,
                                                contentDescription = "Foto de perfil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        isLoadingImage -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(
                                                                primaryPurple.copy(alpha = 0.1f),
                                                                primaryBlue.copy(alpha = 0.05f)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(40.dp),
                                                    color = primaryPurple,
                                                    strokeWidth = 3.dp
                                                )
                                            }
                                        }
                                        profileImageUrl != null -> {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(profileImageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Foto de perfil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(
                                                                primaryPurple.copy(alpha = 0.1f),
                                                                primaryBlue.copy(alpha = 0.05f)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "Sin foto",
                                                    modifier = Modifier.size(80.dp),
                                                    tint = Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }

                                    // Overlay para modo edición
                                    if (isEditing) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Color.Black.copy(alpha = 0.5f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Cambiar foto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Text(
                                                    text = "CAMBIAR",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Indicador de estado online
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-8).dp, y = (-8).dp)
                                    .background(Color.Green, CircleShape)
                                    .border(3.dp, darkBackground, CircleShape)
                            )
                        }
                    }
                }

                // Información del usuario
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, start = 24.dp, end = 24.dp, bottom = 20.dp)
                    ) {
                        perfilUsuario?.let { perfil ->
                            Text(
                                text = perfil.nombre.ifEmpty { "Sin nombre" },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.2f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Miembro desde ${
                                    SimpleDateFormat(
                                        "MMMM yyyy",
                                        Locale.getDefault()
                                    ).format(Date())}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Card de información personal
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = primaryPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Información Personal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            perfilUsuario?.let { perfil ->
                                if (isEditing) {
                                    // Modo edición
                                    OutlinedTextField(
                                        value = editNombre,
                                        onValueChange = {
                                            editNombre = it
                                            nombreError = it.isBlank()
                                        },
                                        label = { Text("Nombre *", color = Color.White.copy(alpha = 0.7f)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = nombreError,
                                        supportingText = if (nombreError) {
                                            { Text("El nombre es obligatorio", color = Color.Red) }
                                        } else null,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = primaryPurple,
                                            focusedBorderColor = primaryPurple,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            errorBorderColor = Color.Red,
                                            focusedLabelColor = primaryPurple,
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = editCelular,
                                        onValueChange = { editCelular = it },
                                        label = { Text("Celular") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = primaryBlue,
                                            focusedBorderColor = primaryBlue,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            focusedLabelColor = primaryBlue,
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    EdadDropdownEditable(editEdad) { nuevaEdad ->
                                        editEdad = nuevaEdad
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    CiudadDropdownEditable(editCiudad) { nuevaCiudad ->
                                        editCiudad = nuevaCiudad
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
                                            if (editNombre.isBlank()) {
                                                nombreError = true
                                                return@Button
                                            }

                                            isSaving = true
                                            userId?.let { uid ->
                                                val updatedData = hashMapOf(
                                                    "nombre" to editNombre.trim(),
                                                    "celular" to editCelular,
                                                    "edad" to editEdad,
                                                    "ciudad" to editCiudad
                                                )

                                                db.collection("perfil").document(uid)
                                                    .update(updatedData as Map<String, Any>)
                                                    .addOnSuccessListener {
                                                        Log.d("MiPerfil", "Perfil actualizado exitosamente")
                                                        perfilUsuario = perfilUsuario?.copy(
                                                            nombre = editNombre.trim(),
                                                            celular = editCelular,
                                                            edad = editEdad ?: 0,
                                                            ciudad = editCiudad
                                                        )

                                                        imageUri?.let { uri ->
                                                            uploadImageToFirebase(uri, uid) { url ->
                                                                profileImageUrl = url
                                                                imageUri = null
                                                                isSaving = false
                                                                isEditing = false
                                                            }
                                                        } ?: run {
                                                            isSaving = false
                                                            isEditing = false
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("MiPerfil", "Error actualizando perfil", e)
                                                        isSaving = false
                                                    }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = primaryPurple
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isSaving
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                "Guardar Cambios",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    // Modo vista
                                    InfoRowModern("Nombre", perfil.nombre)
                                    InfoRowModern("Celular", perfil.celular.ifEmpty { "No especificado" })
                                    InfoRowModern("Edad", if (perfil.edad > 0) "${perfil.edad} años" else "No especificada")
                                    InfoRowModern("Ciudad", perfil.ciudad.ifEmpty { "No especificada" })
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Tabs mejoradas
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = primaryPurple,
                                    height = 3.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "FlashPlans",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 0) primaryPurple else Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "${flashPlans.size}",
                                        fontSize = 12.sp,
                                        color = if (selectedTab == 0) accentPink else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Planes",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 1) primaryPurple else Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "${planes.size}",
                                        fontSize = 12.sp,
                                        color = if (selectedTab == 1) accentPink else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ✅ Contenido dinámico según la tab seleccionada
                if (selectedTab == 0) {
                    // ✅ FlashPlans en grid de 2 columnas
                    if (flashPlans.isEmpty()) {
                        item {
                            EmptyStateCard("No has publicado FlashPlans aún", "¡Crea tu primer FlashPlan!")
                        }
                    } else {
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((flashPlans.size / 2 + flashPlans.size % 2) * 250.dp)
                                    .padding(horizontal = 4.dp)
                            ) {
                                items(flashPlans) { flashPlan ->
                                    FlashPlanGridItem(
                                        flashPlan = flashPlan,
                                        onImageClick = { imageUrl ->
                                            expandedImageUrl = imageUrl
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ✅ Tab de Planes con recarga automática
                    if (planes.isEmpty()) {
                        item {
                            EmptyStateCard("No has publicado planes aún", "¡Crea tu primer plan!")
                        }
                    } else {
                        items(planes, key = { it.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onPlanClick = { navigateToPlanDetail(plan.id) },
                                currentUserId = currentUserId,
                                db = db,
                                coroutineScope = coroutineScope,
                                context = context,
                                searchText = "",
                                navigateToUserProfile = navigateToProfile,
                                navigateToEditPlan = navigateToEditPlan,
                                navigateToMiPerfil = navigateToMiPerfil,
                                navigateToComments = navigateToComments,
                                // ✅ Callback de eliminación con recarga automática
                                onPlanDeleted = { planId ->
                                    handlePlanDeleted(planId)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // Diálogo de imagen expandida
        expandedImageUrl?.let { imageUrl ->
            Dialog(
                onDismissRequest = { expandedImageUrl = null },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { expandedImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen expandida",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Botón de cerrar
                    IconButton(
                        onClick = { expandedImageUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Diálogo de logout
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "¿Cerrar sesión?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que quieres cerrar sesión?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            auth.signOut()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cerrar sesión",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text(
                            text = "Cancelar",
                            color = primaryPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                containerColor = cardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ✅ Nuevo componente: FlashPlan para grid (solo imagen, más limpio)
@Composable
fun FlashPlanGridItem(
    flashPlan: FlashPlan,
    onImageClick: (String) -> Unit
) {
    val primaryPurple = Color(0xFF8B5CF6)
    val cardBackground = Color(0xFF1E1E3F)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ✅ Solo imagen, sin iconos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable {
                        flashPlan.imageUrl?.let(onImageClick)
                    },
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box {
                    if (flashPlan.imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(flashPlan.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "FlashPlan imagen",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Overlay sutil para mejorar legibilidad
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f)
                                        ),
                                        startY = 80f
                                    )
                                )
                        )
                    } else {
                        // Placeholder minimalista
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            primaryPurple.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "FlashPlan",
                                modifier = Modifier.size(32.dp),
                                tint = primaryPurple.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Información del FlashPlan (compacta)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Título
                Text(
                    text = flashPlan.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                // Fecha y hora minimalistas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = flashPlan.fecha,
                        fontSize = 11.sp,
                        color = primaryPurple,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = flashPlan.hora,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Función auxiliar para el EmptyStateCard
@Composable
fun EmptyStateCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E3F)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Create,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Función auxiliar para InfoRowModern
@Composable
fun InfoRowModern(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun EdadDropdownEditable(selectedEdad: Int?, onEdadSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val primaryPurple = Color(0xFF8B5CF6)

    Box {
        OutlinedTextField(
            value = selectedEdad?.toString() ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text("Edad", color = Color.White.copy(alpha = 0.7f)) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar edad",
                    modifier = Modifier.clickable { expanded = true },
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = primaryPurple,
                focusedBorderColor = primaryPurple,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedLabelColor = primaryPurple,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.height(200.dp)
        ) {
            for (edad in 15..60) {
                DropdownMenuItem(
                    text = { Text(edad.toString()) },
                    onClick = {
                        onEdadSelected(edad)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ModernUserItem(
    usuario: UsuarioBasico,
    onClick: () -> Unit
) {
    val primaryPurple = Color(0xFF8B5CF6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen de perfil
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryPurple.copy(alpha = 0.3f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (usuario.imageUrl != null) {
                AsyncImage(
                    model = usuario.imageUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Sin foto",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Nombre
        Text(
            text = usuario.nombre,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))

        // Icono de flecha
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = primaryPurple,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Función para subir imagen
fun uploadImageToFirebase(uri: Uri, userId: String, onSuccess: (String) -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$userId")

    storageRef.putFile(uri)
        .addOnSuccessListener { taskSnapshot ->
            Log.d("Upload", "Imagen subida exitosamente: ${taskSnapshot.metadata?.path}")
            storageRef.downloadUrl.addOnSuccessListener { url ->
                onSuccess(url.toString())
                Log.d("Upload", "URL obtenida exitosamente: $url")
            }.addOnFailureListener { e ->
                Log.e("Upload", "Error obteniendo URL de descarga", e)
            }
        }
        .addOnFailureListener { e ->
            Log.e("Upload", "Error al subir imagen", e)
        }
}

@Composable
fun CiudadDropdownEditable(selectedCiudad: String, onCiudadSelected: (String) -> Unit) {
    val ciudades = listOf("Garzon", "Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena")
    var expanded by remember { mutableStateOf(false) }
    val primaryPurple = Color(0xFF8B5CF6)

    Box {
        OutlinedTextField(
            value = selectedCiudad,
            onValueChange = { },
            readOnly = true,
            label = { Text("Ciudad", color = Color.White.copy(alpha = 0.7f)) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar ciudad",
                    modifier = Modifier.clickable { expanded = true },
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = primaryPurple,
                focusedBorderColor = primaryPurple,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedLabelColor = primaryPurple,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ciudades.forEach { ciudad ->
                DropdownMenuItem(
                    text = { Text(ciudad) },
                    onClick = {
                        onCiudadSelected(ciudad)
                        expanded = false
                    }
                )
            }
        }
    }
}