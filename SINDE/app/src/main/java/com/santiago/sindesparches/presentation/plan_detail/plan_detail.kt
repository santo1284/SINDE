package com.santiago.sindesparches.presentation.plan_detail

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.presentation.publicaciones.Plan
import com.santiago.sindesparches.ui.theme.Purple
import com.santiago.sindesparches.ui.theme.azul
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.santiago.sindesparches.ui.theme.boton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToUserProfile: (String) -> Unit // Nuevo parámetro para navegar al perfil
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var plan by remember { mutableStateOf<Plan?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) } // Estado para el perfil del usuario
    var userProfileImageUrl by remember { mutableStateOf<String?>(null) } // URL de la imagen de perfil
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    //contexto para abrir WhatsApp
    val context = LocalContext.current

    //imagen desde firebase
    var imagen_usuario by remember { mutableStateOf<String?>(null) }
    val storage = FirebaseStorage.getInstance().reference
    val storageRef = storage.child("profile_pictures/${plan?.userId}")
    storageRef.downloadUrl.addOnSuccessListener { uri ->
        imagen_usuario = uri.toString()
    }.addOnSuccessListener {  }

    // Cargar los detalles del plan al iniciar
    LaunchedEffect(planId) {
        coroutineScope.launch {
            try {
                isLoading = true
                plan = getPlanById(db, planId)
                // Cargar información del perfil del usuario creador
                plan?.userId?.let { userId ->
                    userProfile = getUserProfileById(db, userId)
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error al cargar el plan: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.title ?: "Detalles del Plan") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Solo mostrar opciones de edición si el usuario es el creador
                    if (plan?.userId == auth.currentUser?.uid) {
                        IconButton(onClick = { navigateToEdit(planId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Plan")
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Plan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.background(black)) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(azul, azul_mitad, Purple),
                        start = Offset(Float.POSITIVE_INFINITY, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "Error desconocido",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    errorMessage = null
                                    isLoading = true
                                    plan = getPlanById(db, planId)
                                    plan?.userId?.let { userId ->
                                        userProfile = getUserProfileById(db, userId)
                                    }
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorMessage = "Error al cargar el plan: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            } else if (plan == null) {
                Text(
                    text = "No se encontró el plan",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {

                // Contenido del detalle del plan
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Información del usuario creador
                    userProfile?.let { profile ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // ✅ VALIDAR que tengamos userId antes de navegar
                                    plan?.userId?.let { userId ->
                                        if (userId.isNotBlank()) {
                                            Log.d("PlanDetail", "Navegando al perfil de usuario: $userId")
                                            navigateToUserProfile(userId)
                                        } else {
                                            Log.e("PlanDetail", "UserId del plan está vacío")
                                        }
                                    } ?: Log.e("PlanDetail", "Plan o userId es null")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Imagen de perfil redondeada
                                AsyncImage(
                                    model = imagen_usuario ?: R.drawable.bxs_user, // Placeholder si no hay imagen
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Creado por",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = profile.nombre ?: "Usuario",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Icono para indicar que es clickable
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Ver perfil",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Imágenes del plan
                    if (plan!!.imageUrls.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(plan!!.imageUrls) { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Imagen del plan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillParentMaxHeight()
                                            .width(400.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Información detallada del plan
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Título
                        Text(
                            text = plan!!.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )



                        Spacer(modifier = Modifier.height(16.dp))

                        // Fecha y hora
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatDate(plan!!.date),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = plan!!.timeString,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Ubicación
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = plan!!.location,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Descripción
                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = plan!!.description,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fecha de creación
                        Text(
                            text = "Publicado el ${formatFullDate(plan!!.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón de WhatsApp - Solo se muestra si enableWhatsapp es true
                        if (plan!!.enableWhatsapp == true && plan!!.phoneNumber != null) {
                            val message = "Hola, estoy interesado en el plan: ${plan!!.title}"
                            val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
                            val whatsappUrl = "https://api.whatsapp.com/send?phone=${plan!!.phoneNumber}&text=$encodedMessage"

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("WhatsApp no instalado")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366) // Color verde de WhatsApp
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Aquí deberías añadir un icono de WhatsApp
                                    // Puedes usar un icono personalizado o alguno similar de Material Icons
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "WhatsApp",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Contactar por WhatsApp")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar plan") },
            text = { Text("¿Estás seguro de que quieres eliminar este plan? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                deletePlan(db, planId)
                                snackbarHostState.showSnackbar("Plan eliminado correctamente")
                                navigateBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al eliminar el plan: ${e.message}")
                            }
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Función para obtener un plan por su ID
private suspend fun getPlanById(db: FirebaseFirestore, planId: String): Plan = withContext(Dispatchers.IO) {
    try {
        Log.d("getPlanById", "Buscando plan con ID: $planId")

        val document = db.collection("planes").document(planId).get().await()

        if (document.exists()) {
            val plan = document.toObject(Plan::class.java)
                ?: throw Exception("No se pudo convertir el documento a Plan")

            // ✅ IMPORTANTE: Asignar el ID del documento al plan
            val planWithId = plan.copy(id = document.id)

            Log.d("getPlanById", "Plan encontrado: ${planWithId.title} con ID: ${planWithId.id}")
            return@withContext planWithId
        } else {
            throw Exception("El plan no existe")
        }
    } catch (e: Exception) {
        Log.e("getPlanById", "Error obteniendo plan: ${e.message}", e)
        throw e
    }
}

// Función para obtener el perfil del usuario por su ID
private suspend fun getUserProfileById(db: FirebaseFirestore, userId: String): UserProfile? = withContext(Dispatchers.IO) {
    try {
        val document = db.collection("perfil").document(userId).get().await()
        return@withContext document.toObject(UserProfile::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

// Función para obtener la URL de la imagen de perfil del usuario
private suspend fun getUserProfileImageUrl(storage: FirebaseStorage, userId: String): String? = withContext(Dispatchers.IO) {
    try {
        val imageRef = storage.reference.child("profile_pictures/$userId.jpg")
        return@withContext imageRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        // Si no encuentra la imagen, intentar con diferentes extensiones
        try {
            val imageRef = storage.reference.child("profile_pictures/$userId.png")
            return@withContext imageRef.downloadUrl.await().toString()
        } catch (e2: Exception) {
            try {
                val imageRef = storage.reference.child("profile_pictures/$userId.jpeg")
                return@withContext imageRef.downloadUrl.await().toString()
            } catch (e3: Exception) {
                e3.printStackTrace()
                return@withContext null
            }
        }
    }
}

// Función para eliminar un plan
private suspend fun deletePlan(db: FirebaseFirestore, planId: String) = withContext(Dispatchers.IO) {
    try {
        db.collection("planes").document(planId).delete().await()
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

// Función para formatear fecha de timestamp a formato legible
private fun formatDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

// Función para formatear fecha completa con hora
private fun formatFullDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

// Data class para el perfil del usuario (ajusta según tu estructura)
data class UserProfile(
    val userId: String,
    val nombre: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val createdAt: Long? = null
    // Añade otros campos que tengas en tu colección perfil
)