package com.santiago.sindesparches.presentation.home

import android.content.Context
import android.content.Intent
import android.util.Log
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.presentation.plan_detail.UserProfile
import com.santiago.sindesparches.presentation.publicaciones.Plan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlanCard(
    plan: Plan,
    onPlanClick: () -> Unit,
    currentUserId: String,
    db: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    context: Context,
    searchText: String = "",
    navigateToUserProfile: (String) -> Unit,
    navigateToEditPlan: (String) -> Unit,
    navigateToMiPerfil: () -> Unit // Agregar este parámetro
) {
    val isOwner = plan.userId == currentUserId

    // Estados para las interacciones
    var likes by remember { mutableIntStateOf(plan.likes?.size ?: 0) }
    var isLiked by remember { mutableStateOf(plan.likes?.contains(currentUserId) == true) }
    var participants by remember { mutableIntStateOf(plan.participants?.size ?: 0) }
    var isParticipating by remember { mutableStateOf(plan.participants?.contains(currentUserId) == true) }
    var shares by remember { mutableIntStateOf(plan.shares ?: 0) }

    // Estados para mostrar diálogos de información
    var showLikesDialog by remember { mutableStateOf(false) }
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Estados para usuarios con imágenes de perfil - usando la clase común
    var likesUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var participantsUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingLikes by remember { mutableStateOf(false) }
    var isLoadingParticipants by remember { mutableStateOf(false) }

    // Estados para el perfil del usuario que publicó
    var imagenUsuario by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }

    // Función para cargar usuarios con imágenes de perfil
    suspend fun getUserProfiles(userIds: List<String>): List<UserProfile> {
        return try {
            val userProfiles = mutableListOf<UserProfile>()
            val storage = FirebaseStorage.getInstance().reference

            for (userId in userIds) {
                try {
                    val userDoc = db.collection("perfil").document(userId).get().await()
                    val name = userDoc.getString("nombre") ?: "Usuario"

                    // Intentar cargar imagen desde Firebase Storage
                    val storageRef = storage.child("profile_pictures/$userId")
                    var profileImageUrl: String? = null

                    try {
                        profileImageUrl = storageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        // Si no hay imagen en Storage, usar la de Firestore como respaldo
                        profileImageUrl = userDoc.getString("profileImageUrl")
                    }

                    userProfiles.add(
                        UserProfile(
                            userId = userId,
                            nombre = name,
                            profileImageUrl = profileImageUrl
                        )
                    )
                } catch (e: Exception) {
                    // Si hay error con un usuario, agregarlo con valores por defecto
                    userProfiles.add(
                        UserProfile(
                            userId = userId,
                            nombre = "Usuario",
                            profileImageUrl = null
                        )
                    )
                }
            }
            userProfiles
        } catch (e: Exception) {
            Log.e("PlanCard", "Error al cargar perfiles de usuarios", e)
            emptyList()
        }
    }


    // Cargar información del usuario que publicó
    LaunchedEffect(plan.userId) {
        try {
            isLoadingUser = true
            val userDoc = db.collection("perfil").document(plan.userId).get().await()
            userName = userDoc.getString("nombre") ?: "Usuario"

            val storage = FirebaseStorage.getInstance().reference
            val storageRef = storage.child("profile_pictures/${plan.userId}")

            try {
                val uri = storageRef.downloadUrl.await()
                imagenUsuario = uri.toString()
            } catch (e: Exception) {
                // Si no hay imagen en Storage, usar la de Firestore como respaldo
                imagenUsuario = userDoc.getString("profileImageUrl")
            }

            isLoadingUser = false
        } catch (e: Exception) {
            Log.e("PlanCard", "Error al cargar perfil del usuario", e)
            userName = "Usuario"
            imagenUsuario = null
            isLoadingUser = false
        }
    }

    // Función para destacar texto de búsqueda
    @Composable
    fun HighlightedText(
        text: String,
        searchText: String,
        style: androidx.compose.ui.text.TextStyle,
        color: Color = Color.White,
        highlightColor: Color = Color.Yellow
    ) {
        if (searchText.isBlank()) {
            Text(text = text, style = style, color = color)
        } else {
            val annotatedString = buildAnnotatedString {
                val startIndex = text.indexOf(searchText, ignoreCase = true)
                if (startIndex >= 0) {
                    append(text.substring(0, startIndex))
                    withStyle(
                        style = SpanStyle(
                            background = highlightColor,
                            color = Color.Black
                        )
                    ) {
                        append(text.substring(startIndex, startIndex + searchText.length))
                    }
                    append(text.substring(startIndex + searchText.length))
                } else {
                    append(text)
                }
            }
            Text(text = annotatedString, style = style, color = color)
        }
    }

    // Componente para mostrar usuario con imagen de perfil
    @Composable
    fun UserListItem(
        userProfile: UserProfile,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen de perfil
            AsyncImage(
                model = userProfile.profileImageUrl,
                contentDescription = "Foto de perfil de ${userProfile.nombre}",
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
            userProfile.nombre?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            // Icono de flecha para indicar que es clickeable
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver perfil",
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
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
                // Imagen de perfil del usuario con estado de carga
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
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
                                    if (!isOwner) navigateToUserProfile(plan.userId)
                                },
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.ic_menu_myplaces),
                            placeholder = painterResource(id = android.R.drawable.ic_menu_myplaces)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre del usuario y etiqueta "Mi Plan" si es el dueño
                Column(
                    modifier = Modifier.weight(1f)
                ) {
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
                    }
                }

                // Timestamp del plan
                Text(
                    text = formatTimeAgo(plan.createdAt ?: System.currentTimeMillis()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                // Botón de opciones (solo para el propietario)
                if (isOwner) {
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = {
                                    showOptionsMenu = false
                                    navigateToEditPlan(plan.id)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = Color.Red) },
                                onClick = {
                                    showOptionsMenu = false
                                    // TODO: Implementar eliminación
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                }
                            )
                        }
                    }
                }
            }

            // Imágenes del plan con indicadores
            if (plan.imageUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (plan.imageUrls.size == 1) {
                        AsyncImage(
                            model = plan.imageUrls.first(),
                            contentDescription = "Imagen del plan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { plan.imageUrls.size })

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = plan.imageUrls[page],
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
                            repeat(plan.imageUrls.size) { index ->
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
                // Título con destacado de búsqueda
                HighlightedText(
                    text = plan.title,
                    searchText = searchText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha y hora
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(plan.date),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plan.timeString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ubicación con destacado de búsqueda
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    HighlightedText(
                        text = plan.location,
                        searchText = searchText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción con destacado de búsqueda
                HighlightedText(
                    text = plan.description,
                    searchText = searchText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de interacción mejorados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón Me Gusta
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    handleLikeAction(
                                        planId = plan.id,
                                        currentUserId = currentUserId,
                                        isLiked = isLiked,
                                        planOwnerId = plan.userId,
                                        db = db,
                                        context = context,
                                        onSuccess = { newIsLiked, newLikesCount ->
                                            isLiked = newIsLiked
                                            likes = newLikesCount
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Me gusta",
                                tint = if (isLiked) Color.Red else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(likes),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                if (likes > 0) {
                                    coroutineScope.launch {
                                        isLoadingLikes = true
                                        likesUsers = getUserProfiles(plan.likes ?: emptyList())
                                        isLoadingLikes = false
                                        showLikesDialog = true
                                    }
                                }
                            }
                        )
                    }

                    // Botón Participar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    handleParticipateAction(
                                        planId = plan.id,
                                        currentUserId = currentUserId,
                                        isParticipating = isParticipating,
                                        planOwnerId = plan.userId,
                                        db = db,
                                        context = context,
                                        onSuccess = { newIsParticipating, newParticipantsCount ->
                                            isParticipating = newIsParticipating
                                            participants = newParticipantsCount
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                if (isParticipating) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Participar",
                                tint = if (isParticipating) Color.Green else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(participants),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isParticipating) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                if (participants > 0) {
                                    coroutineScope.launch {
                                        isLoadingParticipants = true
                                        participantsUsers = getUserProfiles(plan.participants ?: emptyList())
                                        isLoadingParticipants = false
                                        showParticipantsDialog = true
                                    }
                                }
                            }
                        )
                    }

                    // Botón Compartir
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    handleShareAction(
                                        planId = plan.id,
                                        currentUserId = currentUserId,
                                        planOwnerId = plan.userId,
                                        planTitle = plan.title,
                                        db = db,
                                        context = context,
                                        onSuccess = { newSharesCount ->
                                            shares = newSharesCount
                                        }
                                    )
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
                        Text(
                            text = formatCount(shares),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Diálogo mejorado para "Me gusta" con imágenes de perfil
    if (showLikesDialog) {
        AlertDialog(
            onDismissRequest = { showLikesDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Me gusta ($likes)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                if (isLoadingLikes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cargando perfiles...")
                        }
                    }
                } else if (likesUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay me gusta aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(likesUsers) { userProfile ->
                            UserListItem(
                                userProfile = userProfile,
                                onClick = {
                                    showLikesDialog = false
                                    // Verificar si es el usuario actual para redirigir correctamente
                                    if (userProfile.userId == currentUserId) {
                                        navigateToMiPerfil()
                                    } else {
                                        navigateToUserProfile(userProfile.userId)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLikesDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo mejorado para "Participantes" con imágenes de perfil
    if (showParticipantsDialog) {
        AlertDialog(
            onDismissRequest = { showParticipantsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Participantes ($participants)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                if (isLoadingParticipants) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cargando perfiles...")
                        }
                    }
                } else if (participantsUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay participantes aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(participantsUsers) { userProfile ->
                            UserListItem(
                                userProfile = userProfile,
                                onClick = {
                                    showParticipantsDialog = false
                                    // Verificar si es el usuario actual para redirigir correctamente
                                    if (userProfile.userId == currentUserId) {
                                        navigateToMiPerfil()
                                    } else {
                                        navigateToUserProfile(userProfile.userId)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParticipantsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// Función helper para formatear números
private fun formatCount(count: Int): String {
    return if (count > 999) String.format("%.1fk", count / 1000.0) else {
        count.toString()
    }
}


// ✅ Función de extensión para formatear números
private fun Double.format(digits: Int) = "%.${digits}f".format(this)

// Función para formatear el tiempo transcurrido
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Ahora"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> "${diff / 604800000}sem"
    }
}

// Funciones quien dio me gusta
suspend fun handleLikeAction(
    planId: String,
    currentUserId: String,
    isLiked: Boolean,
    planOwnerId: String,
    db: FirebaseFirestore,
    context: Context,
    onSuccess: (Boolean, Int) -> Unit
) {
    try {
        val planRef = db.collection("planes").document(planId)

        if (isLiked) {
            // Quitar like
            planRef.update("likes", FieldValue.arrayRemove(currentUserId)).await()

            val document = planRef.get().await()
            val newLikes = document.get("likes") as? List<String> ?: emptyList()
            onSuccess(false, newLikes.size)

        } else {
            // Agregar like
            planRef.update("likes", FieldValue.arrayUnion(currentUserId)).await()

            val document = planRef.get().await()
            val newLikes = document.get("likes") as? List<String> ?: emptyList()
            onSuccess(true, newLikes.size)

            // Enviar notificación al dueño del plan
            if (planOwnerId != currentUserId) {
                sendNotification(
                    db = db,
                    recipientId = planOwnerId,
                    senderId = currentUserId,
                    type = "like",
                    planId = planId,
                    context = context
                )
            }
        }

    } catch (e: Exception) {
        Log.e("PlanCard", "Error al manejar like", e)
        Toast.makeText(context, "Error al procesar like", Toast.LENGTH_SHORT).show()
    }
}

// funcion de quien va  aparticiapr
suspend fun handleParticipateAction(
    planId: String,
    currentUserId: String,
    isParticipating: Boolean,
    planOwnerId: String,
    db: FirebaseFirestore,
    context: Context,
    onSuccess: (Boolean, Int) -> Unit
) {
    try {
        val planRef = db.collection("planes").document(planId)

        if (isParticipating) {
            // Dejar de participar
            planRef.update("participants", FieldValue.arrayRemove(currentUserId)).await()

            val document = planRef.get().await()
            val newParticipants = document.get("participants") as? List<String> ?: emptyList()
            onSuccess(false, newParticipants.size)

        } else {
            // Participar
            planRef.update("participants", FieldValue.arrayUnion(currentUserId)).await()

            val document = planRef.get().await()
            val newParticipants = document.get("participants") as? List<String> ?: emptyList()
            onSuccess(true, newParticipants.size)

            // Enviar notificación al dueño del plan
            if (planOwnerId != currentUserId) {
                sendNotification(
                    db = db,
                    recipientId = planOwnerId,
                    senderId = currentUserId,
                    type = "participate",
                    planId = planId,
                    context = context
                )
            }
        }
    } catch (e: Exception) {
        Log.e("PlanCard", "Error al manejar participación", e)
        Toast.makeText(context, "Error al procesar participación", Toast.LENGTH_SHORT).show()
    }
}

//funcion para ver quien compartio
suspend fun handleShareAction(
    planId: String,
    currentUserId: String,
    planOwnerId: String,
    planTitle: String,
    db: FirebaseFirestore,
    context: Context,
    onSuccess: (Int) -> Unit
) {
    try {
        val planRef = db.collection("planes").document(planId)

        // Obtener el documento del plan
        val document = planRef.get().await()

        val currentShares = document.getLong("shares")?.toInt() ?: 0
        val newShares = currentShares + 1

        // Actualizar el contador de shares
        planRef.update("shares", newShares).await()

        // Callback de éxito
        onSuccess(newShares)

        // Enviar notificación al dueño del plan
        if (planOwnerId != currentUserId) {
            sendNotification(
                db = db,
                recipientId = planOwnerId,
                senderId = currentUserId,
                type = "share",
                planId = planId,
                context = context
            )
        }

        // Mostrar opciones de compartir nativas
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Mira este plan increíble: $planTitle!")
            type = "text/plain"
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Compartir plan"))
        } catch (e: Exception) {
            Log.e("Share", "Error al compartir", e)
        }

    } catch (e: Exception) {
        Log.e("PlanCard", "Error al compartir", e)
        Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show()
    }
}


suspend fun getUserNames(userIds: List<String>, db: FirebaseFirestore): List<String> {
    return try {
        val userNames = mutableListOf<String>()

        for (userId in userIds) {
            val userDoc = db.collection("perfil").document(userId).get().await()
            val userName = userDoc.getString("nombre") ?: "Usuario desconocido"
            userNames.add(userName)
        }

        userNames
    } catch (e: Exception) {
        Log.e("getUserNames", "Error al obtener nombres", e)
        emptyList()
    }
}


// funcion para enviar notificaciones
suspend fun sendNotification(
    db: FirebaseFirestore,
    recipientId: String,
    senderId: String,
    type: String,
    planId: String,
    context: Context
) {
    try {
        // Obtener el nombre del usuario que realiza la acción
        val senderDoc = db.collection("perfil").document(senderId).get().await()
        val senderName = senderDoc.getString("nombre") ?: "Usuario desconocido"

        // Obtener el título del plan
        val planDoc = db.collection("planes").document(planId).get().await()
        val planTitle = planDoc.getString("title") ?: "Plan"

        // Crear el mensaje de notificación
        val message = when (type) {
            "like" -> "$senderName le gustó tu plan: $planTitle"
            "participate" -> "$senderName va a participar en tu plan: $planTitle"
            "share" -> "$senderName compartió tu plan: $planTitle"
            else -> "$senderName interactuó con tu plan: $planTitle"
        }

        // Crear la notificación en Firestore
        val notificationData = hashMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName,
            "type" to type,
            "planId" to planId,
            "planTitle" to planTitle,
            "message" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("Notification", "Notificación enviada correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error al enviar notificación", e)
            }

    } catch (e: Exception) {
        Log.e("sendNotification", "Error al enviar notificación", e)
    }
}

// Función para obtener los planes desde Firestore
private suspend fun getPlanes(db: FirebaseFirestore): List<Plan> = withContext(Dispatchers.IO) {
    try {
        val documents = db.collection("planes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return@withContext documents.documents.mapNotNull { document ->
            try {
                val plan = document.toObject(Plan::class.java)
                plan?.copy(id = document.id) // ✅ Siempre asignar el ID
            } catch (e: Exception) {
                Log.w("getAllPlans", "Error convirtiendo documento ${document.id}: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("getAllPlans", "Error obteniendo planes: ${e.message}", e)
        emptyList()
    }
}

// Función para formatear fecha de timestamp a formato legible
private fun formatDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(calendar.time)
}
