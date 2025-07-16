package com.santiago.sindesparches.presentation.participar

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.white
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

// Data class para los planes
data class Plan(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val timeString: String = "",
    val userId: String = "",
    val imageUrls: List<String> = emptyList(),
    val likes: List<String>? = null,
    val participants: List<String>? = null,
    val shares: Int = 0,
    val createdAt: Long? = null,
    val commentCount: Int = 0
)

// Data class para perfiles de usuario
data class UserProfile(
    val id: String = "",
    val displayName: String? = null,
    val profileImageUrl: String? = null,
    val email: String? = null
)

// Nueva data class para mostrar información de usuario en los diálogos
data class UserInfo(
    val id: String,
    val name: String,
    val profileImageUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun planesParticipoScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigatehome: () -> Unit,
    navigateToUserProfile: (String) -> Unit = {},
    navigateToDetail_Plan: (String) -> Unit = {},
    navigateToMiPerfil: () -> Unit = {},
    navigateToComments: (String) -> Unit = {}
) {
    val currentUserId = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var participatingPlans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Función para recargar los datos
    fun reloadData() {
        coroutineScope.launch {
            if (currentUserId.isNotEmpty()) {
                try {
                    isLoading = true
                    errorMessage = null
                    Log.d("PlanesParticipoScreen", "Recargando planes para usuario: $currentUserId")

                    val query = db.collection("planes")
                        .whereArrayContains("participants", currentUserId)

                    val snapshot = query.get().await()
                    Log.d("PlanesParticipoScreen", "Planes encontrados: ${snapshot.documents.size}")

                    val plans = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dateValue = doc.get("date")
                            val dateString = when (dateValue) {
                                is String -> dateValue
                                is Long -> {
                                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                    dateFormat.format(java.util.Date(dateValue))
                                }
                                else -> ""
                            }

                            Plan(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                location = doc.getString("location") ?: "",
                                date = dateString,
                                timeString = doc.getString("timeString") ?: "",
                                userId = doc.getString("userId") ?: "",
                                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                                likes = doc.get("likes") as? List<String> ?: emptyList(),
                                participants = doc.get("participants") as? List<String> ?: emptyList(),
                                shares = (doc.getLong("shares") ?: 0).toInt(),
                                createdAt = doc.getLong("createdAt")
                            )
                        } catch (e: Exception) {
                            Log.e("PlanesParticipoScreen", "Error parsing plan: ${doc.id}", e)
                            null
                        }
                    }

                    participatingPlans = plans.sortedByDescending { it.createdAt ?: 0 }
                    isLoading = false
                } catch (e: Exception) {
                    Log.e("PlanesParticipoScreen", "Error loading participating plans", e)
                    errorMessage = "Error al cargar los planes: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    // Cargar planes inicialmente
    LaunchedEffect(currentUserId) {
        reloadData()
    }

    Scaffold(containerColor = black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Planes donde participo",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigatehome) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cargando planes...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                errorMessage!!,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { reloadData() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                participatingPlans.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No participas en ningún plan aún",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Explora planes y únete a los que te interesen",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(participatingPlans, key = { it.id }) { plan ->
                            PlanCardParticipating(
                                plan = plan,
                                onPlanClick = { navigateToDetail_Plan(plan.id) },
                                currentUserId = currentUserId,
                                db = db,
                                coroutineScope = coroutineScope,
                                context = context,
                                navigateToUserProfile = navigateToUserProfile,
                                navigateToMiPerfil = navigateToMiPerfil,
                                onPlanUpdated = { updatedPlan ->
                                    // Actualizar el plan en la lista local
                                    participatingPlans = participatingPlans.map {
                                        if (it.id == updatedPlan.id) updatedPlan else it
                                    }
                                },
                                onPlanRemoved = {
                                    // Remover el plan de la lista si ya no participa
                                    reloadData()
                                },
                                navigateToComments = navigateToComments
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCardParticipating(
    plan: Plan,
    onPlanClick: () -> Unit,
    currentUserId: String,
    db: FirebaseFirestore,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    navigateToUserProfile: (String) -> Unit,
    navigateToMiPerfil: () -> Unit,
    onPlanUpdated: (Plan) -> Unit,
    onPlanRemoved: () -> Unit,
    navigateToComments: (String) -> Unit
) {
    val isOwner = plan.userId == currentUserId

    // Estados para las interacciones (usando los valores actuales del plan)
    var localPlan by remember(plan.id) { mutableStateOf(plan) }

    // Estados para los diálogos
    var showLikesDialog by remember { mutableStateOf(false) }
    var showParticipantsDialog by remember { mutableStateOf(false) }

    // Actualizar cuando cambie el plan
    LaunchedEffect(plan) {
        localPlan = plan
    }

    // Estados para el perfil del usuario que publicó
    var imagenUsuario by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }

    // Cargar información del usuario que publicó
    LaunchedEffect(localPlan.userId) {
        try {
            isLoadingUser = true
            val userDoc = db.collection("perfil").document(localPlan.userId).get().await()
            userName = userDoc.getString("nombre") ?: "Usuario"

            val storage = FirebaseStorage.getInstance().reference
            val storageRef = storage.child("profile_pictures/${localPlan.userId}")

            try {
                val uri = storageRef.downloadUrl.await()
                imagenUsuario = uri.toString()
            } catch (e: Exception) {
                imagenUsuario = userDoc.getString("profileImageUrl")
            }

            isLoadingUser = false
        } catch (e: Exception) {
            Log.e("PlanCardParticipating", "Error al cargar perfil del usuario", e)
            userName = "Usuario"
            imagenUsuario = null
            isLoadingUser = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlanClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color.Gray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // Header con información del usuario que publicó
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen de perfil del usuario
                Box(modifier = Modifier.size(40.dp)) {
                    if (isLoadingUser) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        AsyncImage(
                            model = imagenUsuario,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isOwner) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (!isOwner) navigateToUserProfile(localPlan.userId)
                                },
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.ic_menu_myplaces),
                            placeholder = painterResource(id = android.R.drawable.ic_menu_myplaces)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre del usuario
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName ?: "Cargando...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isOwner) {
                        Text(
                            text = "Mi Plan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Participando",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Timestamp del plan
                Text(
                    text = formatTimeAgo(localPlan.createdAt ?: System.currentTimeMillis()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            // Imágenes del plan
            if (localPlan.imageUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (localPlan.imageUrls.size == 1) {
                        AsyncImage(
                            model = localPlan.imageUrls.first(),
                            contentDescription = "Imagen del plan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val pagerState =
                            rememberPagerState(pageCount = { localPlan.imageUrls.size })

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = localPlan.imageUrls[page],
                                contentDescription = "Imagen del plan ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Indicadores de página
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(localPlan.imageUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) Color.White
                                            else Color.White.copy(alpha = 0.5f)
                                        )
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
            ) {
                // Título
                Text(
                    text = localPlan.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha y hora
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(localPlan.date),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = localPlan.timeString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ubicación
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = localPlan.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción
                Text(
                    text = localPlan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de interacción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón Me Gusta
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val isCurrentlyLiked = localPlan.likes?.contains(currentUserId) == true
                                        val planRef = db.collection("planes").document(localPlan.id)

                                        if (isCurrentlyLiked) {
                                            // Quitar like
                                            planRef.update("likes", FieldValue.arrayRemove(currentUserId)).await()

                                            // Actualizar estado local
                                            val newLikes = localPlan.likes?.minus(currentUserId) ?: emptyList()
                                            val updatedPlan = localPlan.copy(likes = newLikes)
                                            localPlan = updatedPlan
                                            onPlanUpdated(updatedPlan)

                                            // Si ya no le gusta, remover de la lista
                                            if (newLikes.isEmpty() || !newLikes.contains(currentUserId)) {
                                                onPlanRemoved()
                                            }

                                            Toast.makeText(context, "Ya no te gusta este plan", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Agregar like
                                            planRef.update("likes", FieldValue.arrayUnion(currentUserId)).await()

                                            // Actualizar estado local
                                            val newLikes = (localPlan.likes ?: emptyList()) + currentUserId
                                            val updatedPlan = localPlan.copy(likes = newLikes)
                                            localPlan = updatedPlan
                                            onPlanUpdated(updatedPlan)

                                            Toast.makeText(context, "Te gusta este plan", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("PlanCardLiked", "Error al actualizar like", e)
                                        Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Me gusta",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )

                        }
                        // Botón "Ver" para likes
                        TextButton(
                            onClick = { showLikesDialog = true },
                        ) {
                            Text(
                                text = com.santiago.sindesparches.presentation.megusta.formatCount(
                                    localPlan.likes?.size ?: 0
                                ),
                                color = white,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,

                                )

                        }

                    }

                    // Botón Participar
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val isCurrentlyParticipating = localPlan.participants?.contains(currentUserId) == true
                                        val planRef = db.collection("planes").document(localPlan.id)

                                        if (isCurrentlyParticipating) {
                                            // Quitar participación
                                            planRef.update("participants", FieldValue.arrayRemove(currentUserId)).await()

                                            // Actualizar estado local
                                            val newParticipants = localPlan.participants?.minus(currentUserId) ?: emptyList()
                                            val updatedPlan = localPlan.copy(participants = newParticipants)
                                            localPlan = updatedPlan
                                            onPlanUpdated(updatedPlan)

                                            Toast.makeText(context, "Ya no participas en este plan", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Agregar participación
                                            planRef.update("participants", FieldValue.arrayUnion(currentUserId)).await()

                                            // Actualizar estado local
                                            val newParticipants = (localPlan.participants ?: emptyList()) + currentUserId
                                            val updatedPlan = localPlan.copy(participants = newParticipants)
                                            localPlan = updatedPlan
                                            onPlanUpdated(updatedPlan)

                                            Toast.makeText(context, "Ahora participas en este plan", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("PlanCardLiked", "Error al actualizar participación", e)
                                        Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            val isParticipating = localPlan.participants?.contains(currentUserId) == true
                            Icon(
                                if (isParticipating) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Participar",
                                tint = if (isParticipating) Color.Green else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Botón "Ver" para participantes
                        TextButton(
                            onClick = { showParticipantsDialog = true },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = com.santiago.sindesparches.presentation.megusta.formatCount(
                                    localPlan.participants?.size ?: 0
                                ),
                                color = white,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    }

                    // Botón Compartir
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(5.dp))
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        // Incrementar contador de shares
                                        val planRef = db.collection("planes").document(localPlan.id)
                                        planRef.update("shares", FieldValue.increment(1)).await()

                                        // Actualizar estado local
                                        val updatedPlan = localPlan.copy(shares = localPlan.shares + 1)
                                        localPlan = updatedPlan
                                        onPlanUpdated(updatedPlan)

                                        // Crear intent para compartir
                                        val shareText = """
                                            ¡Mira este plan genial!
                                            
                                            📅 ${localPlan.title}
                                            📍 ${localPlan.location}
                                            🗓️ ${localPlan.date} - ${localPlan.timeString}
                                            
                                            ${localPlan.description}
                                        """.trimIndent()

                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }

                                        context.startActivity(Intent.createChooser(shareIntent, "Compartir plan"))

                                        Toast.makeText(context, "Plan compartido", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("PlanCardLiked", "Error al compartir", e)
                                        Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp) )
                        Text(
                            text = com.santiago.sindesparches.presentation.megusta.formatCount(
                                localPlan.shares
                            ),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Botón Comentarios
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { navigateToComments(localPlan.id) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_comment),
                                contentDescription = "Comentarios",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(localPlan.commentCount),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Diálogo para mostrar usuarios que dieron "Me gusta"
    if (showLikesDialog) {
        UserListDialog(
            title = "Les gusta este plan",
            userIds = localPlan.likes ?: emptyList(),
            db = db,
            onDismiss = { showLikesDialog = false },
            onUserClick = { userId ->
                showLikesDialog = false
                if (userId == currentUserId) {
                    navigateToMiPerfil()
                } else {
                    navigateToUserProfile(userId)
                }
            }
        )
    }

//  DIÁLOGO PARA LOS PARTICIPANTES
    if (showParticipantsDialog) {
        UserListDialog(
            title = "Participantes del plan",
            userIds = localPlan.participants ?: emptyList(),
            db = db,
            onDismiss = { showParticipantsDialog = false },
            onUserClick = { userId ->
                showParticipantsDialog = false
                if (userId == currentUserId) {
                    navigateToMiPerfil()
                } else {
                    navigateToUserProfile(userId)
                }
            }
        )
    }

}
@Composable
fun UserListDialog(
    title: String,
    userIds: List<String>,
    db: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    var users by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cargar información de los usuarios
    LaunchedEffect(userIds) {
        if (userIds.isNotEmpty()) {
            try {
                isLoading = true
                errorMessage = null

                val userInfoList = mutableListOf<UserInfo>()
                val storage = FirebaseStorage.getInstance().reference

                for (userId in userIds) {
                    try {
                        val userDoc = db.collection("perfil").document(userId).get().await()
                        val userName = userDoc.getString("nombre") ?: "Usuario"

                        // Intentar obtener la imagen del storage primero
                        var profileImageUrl: String? = null
                        try {
                            val storageRef = storage.child("profile_pictures/$userId")
                            val uri = storageRef.downloadUrl.await()
                            profileImageUrl = uri.toString()
                        } catch (e: Exception) {
                            // Si falla, usar la URL del documento
                            profileImageUrl = userDoc.getString("profileImageUrl")
                        }

                        userInfoList.add(
                            UserInfo(
                                id = userId,
                                name = userName,
                                profileImageUrl = profileImageUrl
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("UserListDialog", "Error al cargar usuario $userId", e)
                        // Añadir usuario con información básica si hay error
                        userInfoList.add(
                            UserInfo(
                                id = userId,
                                name = "Usuario",
                                profileImageUrl = null
                            )
                        )
                    }
                }

                users = userInfoList
                isLoading = false
            } catch (e: Exception) {
                Log.e("UserListDialog", "Error general al cargar usuarios", e)
                errorMessage = "Error al cargar usuarios"
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    users.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay usuarios para mostrar",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(users) { user ->
                                UserItem(
                                    user = user,
                                    onClick = { onUserClick(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun UserItem(
    user: UserInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen de perfil
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = "Foto de perfil de ${user.name}",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = CircleShape
                ),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_myplaces),
            placeholder = painterResource(id = android.R.drawable.ic_menu_myplaces)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Nombre del usuario
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Icono de flecha para indicar que es clickeable
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ir al perfil",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}
fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> "${count / 1000}k"
        else -> "${count / 1000000}m"
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Hace un momento"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 2592000000 -> "${diff / 86400000}d"
        else -> "${diff / 2592000000}m"
    }
}

fun formatDate(dateString: String): String {
    return dateString
}