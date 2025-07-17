package com.santiago.sindesparches.presentation.mi_perfil

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.santiago.sindesparches.R

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

data class Plan(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val timeString: String = "",
    val location: String = "",
    val categoria: String = "",
    val maxParticipantes: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val likes: List<String> = emptyList(),
    val participants: List<String> = emptyList(),
    val shares: Int = 0,
    val createdAt: Any? = null,
    val commentCount: Int = 0
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
    navigateToComments: (String) -> Unit
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

    var showLikesDialog by remember { mutableStateOf(false) }
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var selectedPlanLikes by remember { mutableStateOf<List<UsuarioBasico>>(emptyList()) }
    var selectedPlanParticipants by remember { mutableStateOf<List<UsuarioBasico>>(emptyList()) }
    var selectedPlanId by remember { mutableStateOf("") }
    var isLoadingUsers by remember { mutableStateOf(false) }

    val currentUserId = auth.currentUser?.uid ?: ""
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Estados para edición
    var editNombre by remember { mutableStateOf("") }
    var editCelular by remember { mutableStateOf("") }
    var editEdad by remember { mutableStateOf<Int?>(null) }
    var editCiudad by remember { mutableStateOf("") }

    val userId = auth.currentUser?.uid

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
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("MiPerfil", "Error cargando perfil", e)
                    isLoading = false
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
                    if (e is StorageException && e.errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                        Log.e("MiPerfil", "Error de permisos al cargar imagen de perfil")
                    }
                }

            // Cargar FlashPlans
            Log.d("MiPerfil", "Consultando FlashPlans para userId: $uid")
            db.collection("flashPlans")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("MiPerfil", "FlashPlans encontrados: ${documents.size()}")
                    flashPlans = documents.map { doc ->
                        val flashPlan = doc.toObject(FlashPlan::class.java).copy(id = doc.id)
                        Log.d("MiPerfil", "FlashPlan: ${flashPlan.titulo}, imageUrl: ${flashPlan.imageUrl}")
                        flashPlan
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MiPerfil", "Error cargando FlashPlans", e)
                }

            // Cargar Planes (ACTUALIZADO para manejar ambos formatos)
            Log.d("MiPerfil", "Consultando Planes para userId: $uid")
            db.collection("planes")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("MiPerfil", "Planes encontrados: ${documents.size()}")
                    val planesList = documents.map { doc ->
                        val data = doc.data

                        // Convertir los datos para manejar ambos formatos
                        val plan = Plan(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            title = (data["title"] as? String) ?: (data["titulo"] as? String) ?: "",
                            description = (data["description"] as? String) ?: (data["descripcion"] as? String) ?: "",
                            date = (data["date"] as? String) ?: (data["fecha"] as? String) ?: "",
                            timeString = (data["timeString"] as? String) ?: (data["hora"] as? String) ?: "",
                            location = (data["location"] as? String) ?: (data["lugar"] as? String) ?: "",
                            categoria = data["categoria"] as? String ?: "",
                            maxParticipantes = (data["maxParticipantes"] as? Long)?.toInt() ?: 0,
                            imageUrls = when {
                                data["imageUrls"] is List<*> -> (data["imageUrls"] as List<*>).mapNotNull { it as? String }
                                data["imageUrl"] is String -> listOfNotNull(data["imageUrl"] as String)
                                else -> emptyList()
                            },
                            likes = (data["likes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            participants = (data["participants"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            shares = (data["shares"] as? Long)?.toInt() ?: 0,
                            createdAt = when (val createdAt = data["createdAt"] ?: data["fechaCreacion"]) {
                                is Long -> createdAt
                                is com.google.firebase.Timestamp -> createdAt.toDate().time
                                else -> null
                            }
                        )

                        Log.d("MiPerfil", "Plan: ${plan.title}, userId: ${plan.userId}, imageUrls: ${plan.imageUrls}")
                        plan
                    }
                    // Ordenar por fecha de creación
                    planes = planesList.sortedByDescending { (it.createdAt ?: 0L) as Comparable<Any> }
                }
                .addOnFailureListener { e ->
                    Log.e("MiPerfil", "Error cargando Planes", e)
                }

        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2196F3), // azul
                            Color(0xFF64B5F6), // azul_mitad
                            Color(0xFF9C27B0)  // Purple
                        )
                    )
                )
        ) {
            // Header con botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = navigatehome) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "MI PERFIL",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Cancelar" else "Editar",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = {
                        showLogoutDialog = true // Solo mostrar el diálogo
                    }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color.White
                        )
                    }

                    // Agregar este diálogo al final de tu composable:
                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = {
                                Text(
                                    text = "Cerrar sesión",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "¿Estás seguro de que quieres cerrar sesión?",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showLogoutDialog = false
                                        auth.signOut()
                                        onLogout()
                                    }
                                ) {
                                    Text(
                                        text = "Cerrar sesión",
                                        color = Color.Red,
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
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // UN SOLO LazyColumn para todo el contenido
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // Sección de foto de perfil
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .clickable(enabled = isEditing) {
                                    if (isEditing) launcher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                imageUri != null -> {
                                    // Imagen seleccionada localmente
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                isLoadingImage -> {
                                    // Mostrar loading mientras se carga la imagen
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = Color.White
                                    )
                                }
                                profileImageUrl != null -> {
                                    // Imagen de Firebase
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
                                    // Sin imagen
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Sin foto",
                                        modifier = Modifier.size(60.dp),
                                        tint = Color.Gray
                                    )
                                }
                            }

                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Cambiar foto",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    // Información del perfil
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Información Personal",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            perfilUsuario?.let { perfil ->
                                if (isEditing) {
                                    // Modo edición
                                    OutlinedTextField(
                                        value = editNombre,
                                        onValueChange = { editNombre = it },
                                        label = { Text("Nombre") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = editCelular,
                                        onValueChange = { editCelular = it },
                                        label = { Text("Celular") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Dropdown para edad
                                    EdadDropdownEditable(editEdad) { nuevaEdad ->
                                        editEdad = nuevaEdad
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Dropdown para ciudad
                                    CiudadDropdownEditable(editCiudad) { nuevaCiudad ->
                                        editCiudad = nuevaCiudad
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            // Guardar cambios
                                            userId?.let { uid ->
                                                val updatedData = hashMapOf(
                                                    "nombre" to editNombre,
                                                    "celular" to editCelular,
                                                    "edad" to editEdad,
                                                    "ciudad" to editCiudad
                                                )

                                                db.collection("perfil").document(uid)
                                                    .update(updatedData as Map<String, Any>)
                                                    .addOnSuccessListener {
                                                        Log.d("MiPerfil", "Perfil actualizado exitosamente")
                                                        perfilUsuario = perfilUsuario?.copy(
                                                            nombre = editNombre,
                                                            celular = editCelular,
                                                            edad = editEdad ?: 0,
                                                            ciudad = editCiudad
                                                        )

                                                        // Subir nueva imagen si se seleccionó
                                                        imageUri?.let { uri ->
                                                            uploadImageToFirebase(uri, uid) { url ->
                                                                profileImageUrl = url
                                                                imageUri = null
                                                            }
                                                        }

                                                        isEditing = false
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("MiPerfil", "Error actualizando perfil", e)
                                                    }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Guardar Cambios")
                                    }
                                } else {
                                    // Modo vista
                                    InfoRow("Nombre", perfil.nombre)
                                    InfoRow("Celular", perfil.celular)
                                    InfoRow("Edad", "${perfil.edad} años")
                                    InfoRow("Ciudad", perfil.ciudad)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Tabs para FlashPlans y Planes
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color.Black
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("FlashPlans (${flashPlans.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Planes (${planes.size})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Contenido dinámico según la tab seleccionada
                if (selectedTab == 0) {
                    // FlashPlans
                    if (flashPlans.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No has publicado FlashPlans aún",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(flashPlans) { flashPlan ->
                            FlashPlanCard(flashPlan)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    // Planes
                    if (planes.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No has publicado Planes aún",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Usar items() directamente en lugar de LazyColumn anidado
                        items(planes) { plan ->
                            PlanCard(
                                plan = plan,
                                onPlanClick = { navigateToPlanDetail(plan.id) },
                                onLikeClick = { planId ->
                                    // Toggle like
                                    val currentUserId = auth.currentUser?.uid ?: return@PlanCard
                                    val isLiked = plan.likes.contains(currentUserId)

                                    val updatedLikes = if (isLiked) {
                                        plan.likes - currentUserId
                                    } else {
                                        plan.likes + currentUserId
                                    }

                                    // Actualizar en Firebase
                                    db.collection("planes").document(planId)
                                        .update("likes", updatedLikes)
                                        .addOnSuccessListener {
                                            // Actualizar lista local
                                            planes = planes.map { p ->
                                                if (p.id == planId) p.copy(likes = updatedLikes) else p
                                            }
                                        }
                                },
                                onParticipateClick = { planId ->
                                    val currentUserId = auth.currentUser?.uid ?: return@PlanCard

                                    // Obtener datos actuales desde Firebase
                                    db.collection("planes").document(planId)
                                        .get()
                                        .addOnSuccessListener { document ->
                                            if (document.exists()) {
                                                val currentParticipants = document.get("participants") as? List<String> ?: emptyList()
                                                val isParticipating = currentParticipants.contains(currentUserId)

                                                if (isParticipating) {
                                                    // SALIR del plan
                                                    val updatedParticipants = currentParticipants - currentUserId
                                                    db.collection("planes").document(planId)
                                                        .update("participants", updatedParticipants)
                                                        .addOnSuccessListener {
                                                            planes = planes.map { p ->
                                                                if (p.id == planId) p.copy(participants = updatedParticipants) else p
                                                            }
                                                            Log.d("Participate", "Saliste del plan exitosamente")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("Participate", "Error al salir del plan", e)
                                                        }
                                                } else {
                                                    // UNIRSE al plan (sin límite)
                                                    val updatedParticipants = currentParticipants + currentUserId
                                                    db.collection("planes").document(planId)
                                                        .update("participants", updatedParticipants)
                                                        .addOnSuccessListener {
                                                            planes = planes.map { p ->
                                                                if (p.id == planId) p.copy(participants = updatedParticipants) else p
                                                            }
                                                            Log.d("Participate", "Te uniste al plan exitosamente")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("Participate", "Error al unirse al plan", e)
                                                        }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Participate", "Error al obtener datos del plan", e)
                                        }
                                },
                                onShareClick = { planId ->
                                    // Incrementar contador de shares
                                    val updatedShares = plan.shares + 1
                                    db.collection("planes").document(planId)
                                        .update("shares", updatedShares)
                                        .addOnSuccessListener {
                                            planes = planes.map { p ->
                                                if (p.id == planId) p.copy(shares = updatedShares) else p
                                            }
                                        }
                                },
                                onLikesDialogClick = { planId ->
                                    selectedPlanId = planId
                                    loadUsersData(plan.likes, db) { usuarios ->
                                        selectedPlanLikes = usuarios
                                        showLikesDialog = true
                                    }
                                },
                                onParticipantsDialogClick = { planId ->
                                    selectedPlanId = planId
                                    loadUsersData(plan.participants, db) { usuarios ->
                                        selectedPlanParticipants = usuarios
                                        showParticipantsDialog = true
                                    }
                                },
                                currentUserId = auth.currentUser?.uid ?: "",
                                onCommentClick = { planId ->
                                    navigateToComments(planId)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Los diálogos van FUERA del LazyColumn, al final del Scaffold
        if (showLikesDialog) {
            UsersDialog(
                title = "Me gusta",
                usuarios = selectedPlanLikes,
                onDismiss = { showLikesDialog = false },
                onUserClick = { userId ->
                    showLikesDialog = false
                    if (userId == currentUserId) {
                        navigateToMiPerfil()
                    } else {
                        navigateToProfile(userId)
                    }
                }
            )
        }

        if (showParticipantsDialog) {
            UsersDialog(
                title = "Participantes",
                usuarios = selectedPlanParticipants,
                onDismiss = { showLikesDialog = false },
                onUserClick = { userId ->
                    showLikesDialog = false
                    if (userId == currentUserId) {
                        navigateToMiPerfil()
                    } else {
                        navigateToProfile(userId)
                    }
                }
            )
        }
    }
}

@Composable
fun UsersDialog(
    title: String,
    usuarios: List<UsuarioBasico>,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            if (usuarios.isEmpty()) {
                Text(
                    text = "No hay usuarios aún",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(usuarios) { usuario ->
                        UserItem(
                            usuario = usuario,
                            onClick = { onUserClick(usuario.uid) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

// 5. FUNCIÓN PARA CARGAR DATOS DE USUARIOS
fun loadUsersData(
    userIds: List<String>,
    db: FirebaseFirestore,
    onComplete: (List<UsuarioBasico>) -> Unit
) {
    if (userIds.isEmpty()) {
        onComplete(emptyList())
        return
    }

    val usuarios = mutableListOf<UsuarioBasico>()
    var completed = 0

    userIds.forEach { userId ->
        db.collection("perfil").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: "Usuario"

                    // Cargar imagen de perfil
                    FirebaseStorage.getInstance().reference
                        .child("profile_pictures/$userId")
                        .downloadUrl
                        .addOnSuccessListener { url ->
                            usuarios.add(
                                UsuarioBasico(
                                    uid = userId,
                                    nombre = nombre,
                                    imageUrl = url.toString()
                                )
                            )
                            completed++
                            if (completed == userIds.size) {
                                onComplete(usuarios)
                            }
                        }
                        .addOnFailureListener {
                            usuarios.add(
                                UsuarioBasico(
                                    uid = userId,
                                    nombre = nombre,
                                    imageUrl = null
                                )
                            )
                            completed++
                            if (completed == userIds.size) {
                                onComplete(usuarios)
                            }
                        }
                } else {
                    completed++
                    if (completed == userIds.size) {
                        onComplete(usuarios)
                    }
                }
            }
            .addOnFailureListener {
                completed++
                if (completed == userIds.size) {
                    onComplete(usuarios)
                }
            }
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
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = value,
            color = Color.Black
        )
    }
}

@Composable
fun FlashPlanCard(flashPlan: FlashPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        // Solo mostrar la imagen si existe
        flashPlan.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagen del FlashPlan",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        } ?: run {
            // Si no hay imagen, mostrar un placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Sin imagen",
                    modifier = Modifier.size(60.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: Plan,
    onPlanClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onParticipateClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onLikesDialogClick: (String) -> Unit,
    onParticipantsDialogClick: (String) -> Unit,
    currentUserId: String,
    onCommentClick: (String) -> Unit
) {
    val isLiked = plan.likes.contains(currentUserId)
    val isParticipating = plan.participants.contains(currentUserId)
    val canParticipate = plan.participants.size < plan.maxParticipantes
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Imágenes del plan
            if (plan.imageUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable(onClick = onPlanClick)
                ) {
                    if (plan.imageUrls.size == 1) {
                        AsyncImage(
                            model = plan.imageUrls.first(),
                            contentDescription = "Imagen del plan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(plan.imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Imagen del plan",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillParentMaxHeight()
                                        .width(300.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Información del plan
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(onClick = onPlanClick)
            ) {
                // Título y categoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = plan.categoria,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                Color(0xFF2196F3),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción
                Text(
                    text = plan.description,
                    color = Color.Black,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha y hora
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Fecha",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${plan.date} - ${plan.timeString}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Lugar",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plan.location,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones de interacción mejorados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón de Me gusta (Rojo)
                    ImprovedInteractiveButton(
                        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        count = plan.likes.size,
                        isActive = isLiked,
                        activeColor = Color(0xFFE91E63), // Rosa/Rojo
                        onIconClick = { onLikeClick(plan.id) },
                        onCountClick = { onLikesDialogClick(plan.id) }
                    )

                    // Botón de Participar (Verde)
                    ImprovedInteractiveButton(
                        icon = if (isParticipating) Icons.Default.Check else Icons.Default.Add,
                        count = plan.participants.size,
                        isActive = isParticipating,
                        activeColor = Color(0xFF4CAF50), // Verde
                        onIconClick = { onParticipateClick(plan.id) },
                        onCountClick = { onParticipantsDialogClick(plan.id) },
                        enabled = canParticipate || isParticipating
                    )

                    // Botón de Compartir (Azul)
                    ImprovedInteractiveButton(
                        icon = Icons.Default.Share,
                        count = plan.shares,
                        isActive = false,
                        activeColor = Color(0xFF2196F3), // Azul
                        onIconClick = {
                            // Incrementar contador Y abrir compartir
                            val updatedShares = plan.shares + 1
                            FirebaseFirestore.getInstance().collection("planes").document(plan.id)
                                .update("shares", updatedShares)
                                .addOnSuccessListener {
                                    onShareClick(plan.id) // Actualizar la lista local
                                    shareContent(context, plan) // Abrir compartir
                                }
                        },
                        onCountClick = { /* No hace nada para compartir */ }
                    )

                    // Botón de Comentarios
                    ImprovedInteractiveButton(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_comment),
                        count = plan.commentCount,
                        isActive = false,
                        activeColor = Color(0xFF008FFF), // Azul
                        onIconClick = { onCommentClick(plan.id) },
                        onCountClick = { onCommentClick(plan.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ImprovedInteractiveButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isActive: Boolean,
    activeColor: Color,
    onIconClick: () -> Unit,
    onCountClick: () -> Unit,
    enabled: Boolean = true
) {
    val iconColor = when {
        !enabled -> Color.Gray
        isActive -> activeColor
        else -> Color.Gray
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Icono clickeable
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.1f) else Color.Transparent
                )
                .clickable(enabled = enabled) { onIconClick() }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Contador clickeable
        Text(
            text = count.toString(),
            color = iconColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(enabled = enabled && count > 0) { onCountClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun UserItem(
    usuario: UsuarioBasico,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen de perfil
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.3f)),
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
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Nombre
        Text(
            text = usuario.nombre,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
fun EdadDropdownEditable(selectedEdad: Int?, onEdadSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedEdad?.toString() ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text("Edad") },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar edad",
                    modifier = Modifier.clickable { expanded = true }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
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
fun CiudadDropdownEditable(selectedCiudad: String, onCiudadSelected: (String) -> Unit) {
    val ciudades = listOf("Garzon", "Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena")
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedCiudad,
            onValueChange = { },
            readOnly = true,
            label = { Text("Ciudad") },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar ciudad",
                    modifier = Modifier.clickable { expanded = true }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
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

fun shareContent(context: Context, plan: Plan) {
    val shareText = """
        🎉 ¡Únete a mi plan!
        
        📍 ${plan.title}
        📝 ${plan.description}
        📅 ${plan.date} - ${plan.timeString}
        📍 ${plan.location}
        👥 ${plan.participants.size}/${plan.maxParticipantes} participantes
        
        #${plan.categoria}
    """.trimIndent()

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Compartir plan")
    context.startActivity(shareIntent)
}