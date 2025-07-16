package com.santiago.sindesparches.presentation.perfilusuario

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    navigateToPlanDetail: (String) -> Unit = {},
    navigateToProfile: (String) -> Unit = {},
    navigateToMiPerfil: () -> Unit = {},
    navigateToComments: (String) -> Unit = {},
    currentUserId: String
) {
    Log.d("UserProfileScreen", "Recibido userId: '$userId'")

    val coroutineScope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userPlans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cargar información del usuario y sus planes
    LaunchedEffect(userId) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                Log.d("UserProfileScreen", "Iniciando carga de datos para userId: $userId")

                // Cargar perfil del usuario
                userProfile = getUserProfileById(db, userId)
                Log.d("UserProfileScreen", "Perfil cargado: ${userProfile?.nombre}")

                // Cargar imagen de perfil
                profileImageUrl = getUserProfileImageUrl(FirebaseStorage.getInstance(), userId)
                Log.d("UserProfileScreen", "Imagen de perfil: $profileImageUrl")

                // Cargar planes del usuario
                userPlans = getUserPlans(db, userId)
                Log.d("UserProfileScreen", "Planes cargados: ${userPlans.size}")

                isLoading = false
            } catch (e: Exception) {
                Log.e("UserProfileScreen", "Error al cargar el perfil: ${e.message}", e)
                errorMessage = "Error al cargar el perfil: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Usuario") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF000000),
                            Color(0xFF1A001A),
                            Color(0xFF330033)
                        )
                    )
                )
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Error desconocido",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        errorMessage = null
                                        isLoading = true
                                        userProfile = getUserProfileById(db, userId)
                                        profileImageUrl = getUserProfileImageUrl(FirebaseStorage.getInstance(), userId)
                                        userPlans = getUserPlans(db, userId)
                                        isLoading = false
                                    } catch (e: Exception) {
                                        Log.e("UserProfileScreen", "Error al reintentar: ${e.message}", e)
                                        errorMessage = "Error al cargar el perfil: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Reintentar", color = Color.Black)
                        }
                    }
                }
                userProfile == null -> {
                    Text(
                        text = "No se encontró el perfil del usuario",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Información del perfil
                        item {
                            ProfileHeader(
                                userProfile = userProfile!!,
                                profileImageUrl = profileImageUrl,
                                planCount = userPlans.size
                            )
                        }

                        // Título de publicaciones
                        item {
                            Text(
                                text = "Publicaciones (${userPlans.size})",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Lista de planes del usuario
                        if (userPlans.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Este usuario no ha publicado ningún evento aún",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(userPlans) { plan ->
                                PlanCard(
                                    plan = plan,
                                    currentUserId = currentUserId,
                                    db = db,
                                    onClick = {
                                        plan.id?.let { planId ->
                                            if (planId.isNotBlank()) {
                                                Log.d("UserProfileScreen", "Navegando a plan detail con ID: $planId")
                                                navigateToPlanDetail(planId)
                                            } else {
                                                Log.e("UserProfileScreen", "ID del plan está vacío")
                                            }
                                        } ?: run {
                                            Log.e("UserProfileScreen", "Plan sin ID, no se puede navegar")
                                        }
                                    },
                                    onNavigateToProfile = navigateToProfile,
                                    navigateToMiPerfil = navigateToMiPerfil,
                                    // Callback para actualizar el plan en la lista local
                                    onPlanUpdated = { updatedPlan ->
                                        userPlans = userPlans.map {
                                            if (it.id == updatedPlan.id) updatedPlan else it
                                        }
                                    },
                                    onCommentClick = { planId ->
                                        navigateToComments(planId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    userProfile: UserProfile,
    profileImageUrl: String?,
    planCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagen de perfil
            AsyncImage(
                model = profileImageUrl ?: R.drawable.bxs_user,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.bxs_user)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre del usuario
            Text(
                text = userProfile.nombre.takeIf { it.isNotBlank() } ?: "Usuario",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ciudad del usuario
            userProfile.ciudad?.takeIf { it.isNotBlank() }?.let { ciudad ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ciudad,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Email del usuario
            userProfile.email?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estadísticas
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticItem(
                    value = planCount.toString(),
                    label = "Eventos"
                )
                userProfile.edad?.let { edad ->
                    StatisticItem(
                        value = edad.toString(),
                        label = "Años"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanCard(
    plan: Plan,
    currentUserId: String,
    db: FirebaseFirestore,
    onClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    navigateToMiPerfil: () -> Unit,
    onPlanUpdated: (Plan) -> Unit, // Nuevo callback para actualizar el plan
    onCommentClick: (String) -> Unit
) {
    // Estado local del plan para actualizaciones inmediatas
    var localPlan by remember(plan.id) { mutableStateOf(plan) }

    val context = LocalContext.current

    // Actualizar cuando cambie el plan original
    LaunchedEffect(plan) {
        localPlan = plan
    }

    val coroutineScope = rememberCoroutineScope()

    Log.d("PlanCard", "Renderizando plan: ${localPlan.title} con ID: ${localPlan.id}")

    Card(
        onClick = {
            if (!localPlan.id.isNullOrBlank()) {
                Log.d("PlanCard", "Click en plan con ID: ${localPlan.id}")
                onClick()
            } else {
                Log.e("PlanCard", "Plan sin ID, no se puede navegar")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Imagen del plan (si tiene)
            if (localPlan.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = localPlan.imageUrls.first(),
                    contentDescription = "Imagen del evento",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Título del plan
            Text(
                text = localPlan.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fecha
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDate(localPlan.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ubicación
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = localPlan.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Descripción (breve)
            Text(
                text = localPlan.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sección de interacciones sociales mejorada
            SocialInteractionsSection(
                plan = localPlan,
                currentUserId = currentUserId,
                db = db,
                onNavigateToProfile = onNavigateToProfile,
                navigateToMiPerfil = navigateToMiPerfil,
                onLikeToggle = { planId ->
                    val isCurrentlyLiked = localPlan.likes.contains(currentUserId)
                    val newLikes = if (isCurrentlyLiked) {
                        localPlan.likes - currentUserId
                    } else {
                        localPlan.likes + currentUserId
                    }

                    // Actualización inmediata en la UI
                    val updatedPlan = localPlan.copy(likes = newLikes)
                    localPlan = updatedPlan
                    onPlanUpdated(updatedPlan)

                    // Actualización en Firebase en segundo plano
                    coroutineScope.launch {
                        try {
                            toggleLike(db, planId, currentUserId, !isCurrentlyLiked)
                            Log.d("PlanCard", "Like actualizado en Firebase")
                        } catch (e: Exception) {
                            // Si falla, revertir el cambio
                            localPlan = localPlan.copy(likes = localPlan.likes)
                            onPlanUpdated(localPlan)
                            Log.e("PlanCard", "Error al actualizar like: ${e.message}")
                        }
                    }
                },
                onParticipateToggle = { planId ->
                    val isCurrentlyParticipating = localPlan.participants.contains(currentUserId)
                    val newParticipants = if (isCurrentlyParticipating) {
                        localPlan.participants - currentUserId
                    } else {
                        localPlan.participants + currentUserId
                    }

                    // Actualización inmediata en la UI
                    val updatedPlan = localPlan.copy(participants = newParticipants)
                    localPlan = updatedPlan
                    onPlanUpdated(updatedPlan)

                    // Actualización en Firebase en segundo plano
                    coroutineScope.launch {
                        try {
                            toggleParticipation(db, planId, currentUserId, !isCurrentlyParticipating)
                            Log.d("PlanCard", "Participación actualizada en Firebase")
                        } catch (e: Exception) {
                            // Si falla, revertir el cambio
                            localPlan = localPlan.copy(participants = localPlan.participants)
                            onPlanUpdated(localPlan)
                            Log.e("PlanCard", "Error al actualizar participación: ${e.message}")
                        }
                    }
                },
                onShare = { planId ->
                    coroutineScope.launch {
                        try {
                            // Actualización inmediata del contador
                            val updatedPlan = localPlan.copy(shares = localPlan.shares + 1)
                            localPlan = updatedPlan
                            onPlanUpdated(updatedPlan)

                            incrementShareCount(db, planId)

                            // Llamar a la función de compartir con más información
                            shareEvent(
                                context = context,
                                planId = planId,
                                planTitle = localPlan.title,
                                planDescription = localPlan.description,
                                planLocation = localPlan.location,
                                planDate = formatDate(localPlan.date)
                            )
                        } catch (e: Exception) {
                            // Si falla, revertir el cambio
                            localPlan = localPlan.copy(shares = localPlan.shares - 1)
                            onPlanUpdated(localPlan)
                            Log.e("PlanCard", "Error al compartir: ${e.message}")
                        }
                    }
                },
                onCommentClick = { planId ->
                    onCommentClick(planId)
                }
            )
        }
    }
}

// Función para compartir evento
private fun shareEvent(planId: String, planTitle: String) {
    // Implementa aquí la lógica de compartir
    // Por ejemplo, crear un Intent para compartir
    Log.d("shareEvent", "Compartiendo evento: $planTitle")

}

@Composable
fun SocialInteractionsSection(
    plan: Plan,
    currentUserId: String,
    db: FirebaseFirestore,
    onNavigateToProfile: (String) -> Unit,
    onLikeToggle: (String) -> Unit,
    onParticipateToggle: (String) -> Unit,
    onShare: (String) -> Unit,
    navigateToMiPerfil: () -> Unit,
    onCommentClick: (String) -> Unit
) {
    var showLikesDialog by remember { mutableStateOf(false) }
    var showParticipantsDialog by remember { mutableStateOf(false) }

    val isLiked = plan.likes.contains(currentUserId)
    val isParticipating = plan.participants.contains(currentUserId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Me Gusta
            InteractionButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                count = plan.likes.size,
                label = "Me gusta",
                isActive = isLiked,
                activeColor = Color(0xFFE91E63), // Rosa/Rojo
                onIconClick = {
                    plan.id?.let { onLikeToggle(it) }
                },
                onCountClick = {
                    if (plan.likes.isNotEmpty()) {
                        showLikesDialog = true
                    }
                }
            )

            // Botón Participar
            InteractionButton(
                icon = if (isParticipating) Icons.Default.Check else Icons.Default.Add,
                count = plan.participants.size,
                label = "Participar",
                isActive = isParticipating,
                activeColor = Color(0xFF4CAF50), // Verde
                onIconClick = {
                    plan.id?.let { onParticipateToggle(it) }
                },
                onCountClick = {
                    if (plan.participants.isNotEmpty()) {
                        showParticipantsDialog = true
                    }
                }
            )

            // Botón Compartir
            InteractionButton(
                icon = Icons.Default.Share,
                count = plan.shares,
                label = "Compartir",
                isActive = false,
                activeColor = Color.Blue,
                onIconClick = {
                    plan.id?.let { onShare(it) }
                },
                onCountClick = { } // No hace nada al presionar el contador
            )

            // Botón de Comentarios
            InteractionButton(
                icon = Icons.Default.Comment,
                count = 0, // Replace with actual comment count if available
                label = "Comentar",
                isActive = false,
                activeColor = Color.Blue,
                onIconClick = {
                    plan.id?.let { onCommentClick(it) }
                },
                onCountClick = {
                    plan.id?.let { onCommentClick(it) }
                }
            )
        }
    }

    // Diálogos para mostrar usuarios
    if (showLikesDialog) {
        UserListDialog(
            title = "Les gusta este evento",
            userIds = plan.likes,
            db = db,
            onDismiss = { showLikesDialog = false },
            onUserClick = { userId ->
                showLikesDialog = false
                if (userId == currentUserId) {
                    navigateToMiPerfil()
                } else {
                    onNavigateToProfile(userId)
                }
            }
        )
    }

    if (showParticipantsDialog) {
        UserListDialog(
            title = "Participantes del plan",
            userIds = plan.participants,
            db = db,
            onDismiss = { showParticipantsDialog = false },
            onUserClick = { userId ->
                showParticipantsDialog = false
                if (userId == currentUserId) {
                    navigateToMiPerfil()
                } else {
                    onNavigateToProfile(userId)
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
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar información de usuarios
    LaunchedEffect(userIds) {
        isLoading = true
        try {
            val userProfiles = mutableListOf<UserProfile>()
            userIds.forEach { userId ->
                getUserProfileById(db, userId)?.let { profile ->
                    userProfiles.add(profile)
                }
            }
            users = userProfiles
        } catch (e: Exception) {
            Log.e("UserListDialog", "Error cargando usuarios: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (users.isEmpty()) {
                Text(
                    text = "No hay usuarios para mostrar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        UserListItem(
                            user = user,
                            onClick = { onUserClick(user.id) }
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


@Composable
private fun InteractionButton(
    icon: ImageVector,
    count: Int,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onIconClick: () -> Unit,
    onCountClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // Icono clickeable
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isActive) activeColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) activeColor else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable { onIconClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contador clickeable
        Text(
            text = count.toString(),
            color = if (count > 0) Color.White else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clickable(
                    enabled = count > 0,
                    onClick = onCountClick
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Etiqueta
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
private fun UserListItem(
    user: UserProfile,
    onClick: () -> Unit
) {
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    // Cargar imagen de perfil
    LaunchedEffect(user.id) {
        try {
            profileImageUrl = getUserProfileImageUrl(FirebaseStorage.getInstance(), user.id)
        } catch (e: Exception) {
            Log.e("UserListItem", "Error cargando imagen: ${e.message}")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen de perfil
            AsyncImage(
                model = profileImageUrl ?: R.drawable.bxs_user,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.bxs_user)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información del usuario
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.nombre.takeIf { it.isNotBlank() } ?: "Usuario",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                user.ciudad?.takeIf { it.isNotBlank() }?.let { ciudad ->
                    Text(
                        text = ciudad,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Icono de navegación
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Ver perfil",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Funciones para manejar las interacciones
suspend fun toggleLike(db: FirebaseFirestore, planId: String, userId: String, isLiked: Boolean) {
    try {
        val planRef = db.collection("planes").document(planId)

        if (isLiked) {
            // Agregar like
            planRef.update("likes", FieldValue.arrayUnion(userId)).await()
        } else {
            // Quitar like
            planRef.update("likes", FieldValue.arrayRemove(userId)).await()
        }

        Log.d("toggleLike", "Like ${if (isLiked) "agregado" else "removido"} para plan $planId")
    } catch (e: Exception) {
        Log.e("toggleLike", "Error al actualizar like: ${e.message}")
        throw e
    }
}

suspend fun toggleParticipation(db: FirebaseFirestore, planId: String, userId: String, isParticipating: Boolean) {
    try {
        val planRef = db.collection("planes").document(planId)

        if (isParticipating) {
            // Agregar participante
            planRef.update("participants", FieldValue.arrayUnion(userId)).await()
        } else {
            // Quitar participante
            planRef.update("participants", FieldValue.arrayRemove(userId)).await()
        }

        Log.d("toggleParticipation", "Participación ${if (isParticipating) "agregada" else "removida"} para plan $planId")
    } catch (e: Exception) {
        Log.e("toggleParticipation", "Error al actualizar participación: ${e.message}")
        throw e
    }
}
suspend fun incrementShareCount(db: FirebaseFirestore, planId: String) {
    try {
        val planRef = db.collection("planes").document(planId)
        planRef.update("shares", FieldValue.increment(1)).await()

        Log.d("incrementShareCount", "Contador de compartidos incrementado para plan $planId")
    } catch (e: Exception) {
        Log.e("incrementShareCount", "Error al incrementar shares: ${e.message}")
        throw e
    }
}

private fun shareEvent(context: Context, planId: String, planTitle: String, planDescription: String = "", planLocation: String = "", planDate: String = "") {
    try {
        val shareText = buildString {
            append("¡Te invito a este evento!\n\n")
            append("📅 $planTitle\n")
            if (planDescription.isNotBlank()) {
                append("📝 $planDescription\n")
            }
            if (planLocation.isNotBlank()) {
                append("📍 $planLocation\n")
            }
            if (planDate.isNotBlank()) {
                append("🕐 $planDate\n")
            }
            append("\n¡Únete y participa!")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Invitación a evento: $planTitle")
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartir evento")
        context.startActivity(chooserIntent)

        Log.d("shareEvent", "Intent de compartir creado para: $planTitle")
    } catch (e: Exception) {
        Log.e("shareEvent", "Error al compartir evento: ${e.message}")
    }
}

// ✅ FUNCIONES AUXILIARES MEJORADAS
private suspend fun getUserProfileById(db: FirebaseFirestore, userId: String): UserProfile? = withContext(Dispatchers.IO) {
    try {
        Log.d("getUserProfileById", "Buscando perfil para userId: '$userId'")

        if (userId.isBlank()) {
            Log.e("getUserProfileById", "UserId está vacío")
            return@withContext null
        }

        val document = db.collection("perfil")
            .document(userId.trim())
            .get()
            .await()

        if (document.exists()) {
            val profile = document.toObject(UserProfile::class.java)?.copy(id = document.id)
            Log.d("getUserProfileById", "Perfil encontrado: ${profile?.nombre}")
            return@withContext profile
        } else {
            Log.w("getUserProfileById", "Documento no existe para userId: '$userId'")
            return@withContext null
        }
    } catch (e: Exception) {
        Log.e("getUserProfileById", "Error obteniendo perfil: ${e.message}", e)
        return@withContext null
    }
}

private suspend fun getUserProfileImageUrl(storage: FirebaseStorage, userId: String): String? = withContext(Dispatchers.IO) {
    try {
        if (userId.isBlank()) {
            Log.e("getUserProfileImageUrl", "UserId está vacío")
            return@withContext null
        }

        Log.d("getUserProfileImageUrl", "Buscando imagen para userId: $userId")

        // Intentar diferentes formatos de imagen
        val possiblePaths = listOf(
            "profile_pictures/$userId",
            "profile_pictures/$userId.jpg",
            "profile_pictures/$userId.png",
            "profile_pictures/$userId.jpeg"
        )

        for (path in possiblePaths) {
            try {
                val imageRef = storage.reference.child(path)
                val url = imageRef.downloadUrl.await().toString()
                Log.d("getUserProfileImageUrl", "Imagen encontrada en: $path")
                return@withContext url
            } catch (e: Exception) {
                // Continuar con el siguiente path
                continue
            }
        }

        Log.w("getUserProfileImageUrl", "No se encontró imagen de perfil para userId: $userId")
        return@withContext null
    } catch (e: Exception) {
        Log.w("getUserProfileImageUrl", "Error general al cargar imagen: ${e.message}")
        return@withContext null
    }
}

private suspend fun getUserPlans(db: FirebaseFirestore, userId: String): List<Plan> = withContext(Dispatchers.IO) {
    try {
        if (userId.isBlank()) {
            Log.e("getUserPlans", "UserId está vacío")
            return@withContext emptyList()
        }

        Log.d("getUserPlans", "Buscando planes para userId: '$userId'")

        val documents = db.collection("planes")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        Log.d("getUserPlans", "Documentos encontrados: ${documents.size()}")

        val plans = documents.documents.mapNotNull { document ->
            try {
                val plan = document.toObject(Plan::class.java)
                if (plan != null) {
                    val planWithId = plan.copy(id = document.id)
                    Log.d("getUserPlans", "Plan cargado: '${planWithId.title}' con ID: '${planWithId.id}'")
                    planWithId
                } else {
                    Log.w("getUserPlans", "No se pudo convertir documento ${document.id}")
                    null
                }
            } catch (e: Exception) {
                Log.w("getUserPlans", "Error convirtiendo documento ${document.id}: ${e.message}")
                null
            }
        }

        // Ordenar por fecha de creación (más reciente primero)
        val sortedPlans = plans.sortedByDescending { it.createdAt }

        Log.d("getUserPlans", "Total planes cargados y ordenados: ${sortedPlans.size}")
        return@withContext sortedPlans

    } catch (e: Exception) {
        Log.e("getUserPlans", "Error obteniendo planes: ${e.message}", e)
        return@withContext emptyList()
    }
}

private fun formatDate(dateMillis: Long): String {
    return try {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.format(calendar.time)
    } catch (e: Exception) {
        "Fecha inválida"
    }
}

// ✅ DATA CLASSES CONSISTENTES
data class UserProfile(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val ciudad: String? = null,
    val celular: String? = null,
    val edad: Int? = null
)

data class Plan(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: Long = 0L,
    val timeString: String = "",
    val userId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Timestamp? = null,
    val imageUrls: List<String> = emptyList(),
    val enableWhatsapp: Boolean? = false,
    val phoneNumber: String? = null,
    val likes: List<String> = emptyList(),
    val participants: List<String> = emptyList(),
    val shares: Int = 0
)

