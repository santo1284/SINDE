package com.santiago.sindesparches.presentation.notifications
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.santiago.sindesparches.R
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    navigateToPlanDetail: (String) -> Unit,
    navigateToUserProfile: (String) -> Unit,
    navigateBack: () -> Unit
) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val currentUserId = auth.currentUser?.uid

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_animation"
    )

    // Colores modernos para modo oscuro (mismo esquema que CommentsScreen)
    val primaryColor = Color(0xFF6C63FF)
    val secondaryColor = Color(0xFF00D4AA)
    val accentColor = Color(0xFFFF6B9D)
    val surfaceColor = Color(0xFF1A1A2E)
    val backgroundDark = Color(0xFF0F0F1E)
    val cardColor = Color(0xFF252543)

    // Cargar notificaciones
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            loadNotifications(db, currentUserId) { result ->
                when (result) {
                    is Result.Success -> {
                        notifications = result.data
                        isLoading = false
                    }
                    is Result.Error -> {
                        error = result.message
                        isLoading = false
                    }
                }
            }
        }
    }

    // Marcar notificaciones como leídas cuando se abra la pantalla
    LaunchedEffect(notifications) {
        if (currentUserId != null && notifications.isNotEmpty()) {
            markNotificationsAsRead(db, currentUserId, notifications.filter { !it.read })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        backgroundDark,
                        surfaceColor.copy(alpha = 0.6f),
                        backgroundDark
                    ),
                    center = Offset(
                        animatedFloat * 1000,
                        animatedFloat * 800
                    ),
                    radius = 1000f
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Notificaciones ${if (notifications.isNotEmpty()) "(${notifications.size})" else ""}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = navigateBack,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    primaryColor.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (notifications.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (currentUserId != null) {
                                        clearAllNotifications(db, currentUserId) {
                                            notifications = emptyList()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        accentColor.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar todo",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                surfaceColor.copy(alpha = 0.9f),
                                cardColor.copy(alpha = 0.9f)
                            )
                        )
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cargando notificaciones...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error al cargar notificaciones",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    error = null
                                    if (currentUserId != null) {
                                        loadNotifications(db, currentUserId) { result ->
                                            when (result) {
                                                is Result.Success -> {
                                                    notifications = result.data
                                                    isLoading = false
                                                }
                                                is Result.Error -> {
                                                    error = result.message
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Text(
                                    "Reintentar",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            // Ícono animado
                            val scale by animateFloatAsState(
                                targetValue = 1f + (animatedFloat * 0.1f),
                                animationSpec = tween(1000, easing = FastOutSlowInEasing),
                                label = "icon_scale"
                            )

                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(scale),
                                tint = primaryColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "✨ Todo en calma",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tienes notificaciones por ahora.\nCuando alguien interactúe con tus planes increíbles, aparecerán aquí.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notification ->
                            ModernNotificationCard(
                                notification = notification,
                                onNotificationClick = {
                                    navigateToPlanDetail(notification.planId)
                                },
                                onUserClick = {
                                    navigateToUserProfile(notification.senderId)
                                },
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
                                cardColor = cardColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernNotificationCard(
    notification: NotificationItem,
    onNotificationClick: () -> Unit,
    onUserClick: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    cardColor: Color
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .clickable { onNotificationClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.read)
                primaryColor.copy(alpha = 0.15f)
            else
                cardColor.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Efecto de brillo sutil para no leídas
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 1000f)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar del usuario con indicador de tipo
                Box {
                    AsyncImage(
                        model = notification.senderImageUrl,
                        contentDescription = "Foto de ${notification.senderName}",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                getNotificationTypeColor(notification.type, primaryColor, secondaryColor, accentColor),
                                CircleShape
                            )
                            .clickable { onUserClick() },
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = android.R.drawable.ic_menu_myplaces),
                        placeholder = painterResource(id = android.R.drawable.ic_menu_myplaces)
                    )

                    // Indicador de tipo de notificación
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .background(
                                color = getNotificationTypeColor(notification.type, primaryColor, secondaryColor, accentColor),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = cardColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getNotificationTypeIcon(notification.type),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Contenido de la notificación
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Texto principal con formato mejorado
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            ) {
                                append(notification.senderName)
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            ) {
                                append(" ${getNotificationAction(notification.type)} ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryColor
                                )
                            ) {
                                append("\"${notification.planTitle}\"")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tiempo con diseño mejorado
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatNotificationTime(notification.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Indicador de no leída mejorado
                if (!notification.read) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            primaryColor,
                                            accentColor
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "NUEVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Funciones auxiliares mejoradas
@Composable
fun getNotificationTypeColor(
    type: String,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
): Color {
    return when (type) {
        "like" -> accentColor // Rosa
        "comment" -> primaryColor // Azul
        "participate" -> secondaryColor // Verde
        "share" -> Color(0xFFFF9800) // Naranja
        else -> primaryColor
    }
}

fun getNotificationTypeIcon(type: String): ImageVector {
    return when (type) {
        "like" -> Icons.Default.Favorite
        "comment" -> Icons.Default.Email
        "participate" -> Icons.Default.Person
        "share" -> Icons.Default.Share
        else -> Icons.Default.Notifications
    }
}

fun getNotificationAction(type: String): String {
    return when (type) {
        "like" -> "le gustó tu plan"
        "comment" -> "comentó en tu plan"
        "participate" -> "se unió a tu plan"
        "share" -> "compartió tu plan"
        else -> "interactuó con tu plan"
    }
}

// Función mejorada para formatear el tiempo de notificaciones
fun formatNotificationTime(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val notificationTime = timestamp.toDate().time
    val diffMillis = now - notificationTime

    // Convertir a minutos
    val diffMinutes = (diffMillis / (1000 * 60)).toInt()

    return when {
        diffMinutes < 1 -> "Ahora"
        diffMinutes == 1 -> "1 minuto"
        diffMinutes < 60 -> "$diffMinutes minutos"
        diffMinutes < 120 -> "1 hora"
        diffMinutes < 1440 -> "${diffMinutes / 60} horas" // menos de 24 horas
        diffMinutes < 2880 -> "1 día" // menos de 48 horas
        diffMinutes < 10080 -> "${diffMinutes / 1440} días" // menos de 7 días
        diffMinutes < 20160 -> "1 semana" // menos de 2 semanas
        diffMinutes < 43200 -> "${diffMinutes / 10080} semanas" // menos de 30 días
        diffMinutes < 86400 -> "1 mes" // menos de 60 días
        diffMinutes < 525600 -> "${diffMinutes / 43200} meses" // menos de 1 año
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            sdf.format(timestamp.toDate())
        }
    }
}

// ✅ FUNCIÓN MEJORADA QUE CARGA DESDE FIRESTORE Y FIREBASE STORAGE
private fun loadNotifications(
    db: FirebaseFirestore,
    userId: String,
    onResult: (Result<List<NotificationItem>>) -> Unit
) {
    Log.d("NotificationsScreen", "Loading notifications for user: $userId")

    db.collection("notifications")
        .whereEqualTo("recipientId", userId)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("NotificationsScreen", "Error: ${error.message}")
                onResult(Result.Error("Error al cargar notificaciones: ${error.message}"))
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val notifications = mutableListOf<NotificationItem>()
                val userIds = mutableSetOf<String>()

                // Crear notificaciones básicas y extraer IDs de usuarios
                for (document in snapshot.documents) {
                    try {
                        val senderId = document.getString("senderId") ?: continue
                        userIds.add(senderId)

                        val notification = NotificationItem(
                            id = document.id,
                            senderId = senderId,
                            senderName = document.getString("senderName") ?: "Usuario desconocido",
                            senderImageUrl = null, // Lo cargaremos después
                            recipientId = document.getString("recipientId") ?: "",
                            planId = document.getString("planId") ?: "",
                            planTitle = document.getString("planTitle") ?: "Plan desconocido",
                            type = document.getString("type") ?: "notification",
                            message = document.getString("message") ?: "",
                            read = document.getBoolean("read") ?: false,
                            createdAt = document.getTimestamp("timestamp")
                                ?: document.getTimestamp("createdAt")
                                ?: Timestamp(Date()),
                            readAt = document.getTimestamp("readAt")
                        )

                        notifications.add(notification)
                    } catch (e: Exception) {
                        Log.e("NotificationsScreen", "Error parsing notification: ${e.message}")
                        continue
                    }
                }

                // Cargar imágenes de usuarios desde Firestore y Storage
                if (userIds.isNotEmpty()) {
                    loadUserImagesFromFirestoreAndStorage(db, userIds.toList()) { userImagesMap ->
                        // Actualizar las notificaciones con las imágenes
                        val updatedNotifications = notifications.map { notification ->
                            notification.copy(
                                senderImageUrl = userImagesMap[notification.senderId]
                            )
                        }

                        // Ordenar por fecha
                        val sortedNotifications = updatedNotifications.sortedWith { n1, n2 ->
                            n2.createdAt.seconds.compareTo(n1.createdAt.seconds)
                        }

                        Log.d("NotificationsScreen", "Successfully loaded ${sortedNotifications.size} notifications with images")
                        onResult(Result.Success(sortedNotifications))
                    }
                } else {
                    // Sin usuarios para cargar imágenes
                    val sortedNotifications = notifications.sortedWith { n1, n2 ->
                        n2.createdAt.seconds.compareTo(n1.createdAt.seconds)
                    }
                    onResult(Result.Success(sortedNotifications))
                }
            } else {
                Log.d("NotificationsScreen", "No notifications found")
                onResult(Result.Success(emptyList()))
            }
        }
}

// ✅ NUEVA FUNCIÓN QUE CARGA DESDE FIRESTORE Y STORAGE
private fun loadUserImagesFromFirestoreAndStorage(
    db: FirebaseFirestore,
    userIds: List<String>,
    onResult: (Map<String, String?>) -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val userImagesMap = mutableMapOf<String, String?>()

    if (userIds.isEmpty()) {
        onResult(userImagesMap)
        return
    }

    var completed = 0

    userIds.forEach { userId ->
        // Primero intentar cargar desde Firestore
        db.collection("perfil")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Buscar imagen en diferentes campos de Firestore
                        val imageFields = listOf("imageUrl", "profileImageUrl", "photoUrl", "avatar", "profilePicture", "foto")

                        var imageUrl: String? = null
                        for (field in imageFields) {
                            val url = data[field] as? String
                            if (!url.isNullOrBlank()) {
                                imageUrl = url
                                Log.d("NotificationsScreen", "Found image for user $userId in Firestore field $field")
                                break
                            }
                        }

                        if (imageUrl != null) {
                            userImagesMap[userId] = imageUrl
                            completed++
                            if (completed >= userIds.size) {
                                onResult(userImagesMap)
                            }
                        } else {
                            // Si no hay imagen en Firestore, intentar cargar desde Storage
                            loadImageFromStorage(storage, userId) { storageUrl ->
                                if (storageUrl != null) {
                                    userImagesMap[userId] = storageUrl
                                    Log.d("NotificationsScreen", "Found image for user $userId in Storage")
                                } else {
                                    Log.d("NotificationsScreen", "No image found for user $userId in Firestore or Storage")
                                }

                                completed++
                                if (completed >= userIds.size) {
                                    onResult(userImagesMap)
                                }
                            }
                        }
                    } else {
                        // Documento existe pero sin datos, intentar Storage
                        loadImageFromStorage(storage, userId) { storageUrl ->
                            if (storageUrl != null) {
                                userImagesMap[userId] = storageUrl
                                Log.d("NotificationsScreen", "Found image for user $userId in Storage (no Firestore data)")
                            }

                            completed++
                            if (completed >= userIds.size) {
                                onResult(userImagesMap)
                            }
                        }
                    }
                } else {
                    // Documento no existe en Firestore, intentar Storage
                    Log.w("NotificationsScreen", "User $userId not found in perfil collection, trying Storage")
                    loadImageFromStorage(storage, userId) { storageUrl ->
                        if (storageUrl != null) {
                            userImagesMap[userId] = storageUrl
                            Log.d("NotificationsScreen", "Found image for user $userId in Storage (no Firestore doc)")
                        }

                        completed++
                        if (completed >= userIds.size) {
                            onResult(userImagesMap)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationsScreen", "Error loading user $userId from Firestore: ${e.message}")
                // En caso de error en Firestore, intentar Storage
                loadImageFromStorage(storage, userId) { storageUrl ->
                    if (storageUrl != null) {
                        userImagesMap[userId] = storageUrl
                        Log.d("NotificationsScreen", "Found image for user $userId in Storage (Firestore error)")
                    }

                    completed++
                    if (completed >= userIds.size) {
                        onResult(userImagesMap)
                    }
                }
            }
    }
}

// ✅ FUNCIÓN PARA CARGAR IMAGEN DESDE FIREBASE STORAGE
private fun loadImageFromStorage(
    storage: FirebaseStorage,
    userId: String,
    onResult: (String?) -> Unit
) {
    // Según las reglas de Storage, la ruta correcta es profile_pictures/{userId}
    val profilePicturePath = "profile_pictures/$userId"

    Log.d("NotificationsScreen", "Trying to load image from Storage path: $profilePicturePath")

    storage.reference.child(profilePicturePath)
        .downloadUrl
        .addOnSuccessListener { uri ->
            Log.d("NotificationsScreen", "Found image for user $userId at path: $profilePicturePath")
            onResult(uri.toString())
        }
        .addOnFailureListener { e ->
            Log.d("NotificationsScreen", "Image not found for user $userId at path: $profilePicturePath - ${e.message}")
            onResult(null)
        }
}

// Función para marcar notificaciones como leídas
private fun markNotificationsAsRead(
    db: FirebaseFirestore,
    userId: String,
    unreadNotifications: List<NotificationItem>
) {
    if (unreadNotifications.isEmpty()) return

    val batch = db.batch()

    unreadNotifications.forEach { notification ->
        val notificationRef = db.collection("notifications").document(notification.id)
        batch.update(notificationRef, mapOf(
            "read" to true,
            "readAt" to FieldValue.serverTimestamp()
        ))
    }

    batch.commit()
        .addOnFailureListener { exception ->
            Log.e("NotificationsScreen", "Error marking notifications as read: ${exception.message}")
        }
}

// Función para limpiar todas las notificaciones
private fun clearAllNotifications(
    db: FirebaseFirestore,
    userId: String,
    onComplete: () -> Unit
) {
    db.collection("notifications")
        .whereEqualTo("recipientId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                onComplete()
                return@addOnSuccessListener
            }

            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit()
                .addOnSuccessListener {
                    onComplete()
                }
                .addOnFailureListener { exception ->
                    Log.e("NotificationsScreen", "Error clearing notifications: ${exception.message}")
                    onComplete()
                }
        }
        .addOnFailureListener { exception ->
            Log.e("NotificationsScreen", "Error fetching notifications to clear: ${exception.message}")
            onComplete()
        }
}

// Data class para las notificaciones
data class NotificationItem(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderImageUrl: String?,
    val recipientId: String,
    val planId: String,
    val planTitle: String,
    val type: String, // "like", "comment", "participate", "share"
    val message: String,
    val read: Boolean,
    val createdAt: Timestamp,
    val readAt: Timestamp?
)

// Sealed class para manejar resultados
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}