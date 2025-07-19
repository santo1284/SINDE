package com.santiago.sindesparches.presentation.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    navigateToPlanDetail: (String) -> Unit,
    navigateToUserProfile: (String) -> Unit,
    navigateBack: () -> Unit
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = auth.currentUser?.uid

    // Listener en tiempo real para las notificaciones
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            try {
                isLoading = true
                Log.d("NotificationsScreen", "Setting up real-time listener for user: $currentUserId")

                val listenerRegistration = db.collection("notifications")
                    .whereEqualTo("recipientId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("NotificationsScreen", "Error listening to notifications", error)
                            isLoading = false
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            Log.d("NotificationsScreen", "Real-time update: ${snapshot.documents.size} notifications")

                            val notificationsList = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val notification = doc.toObject<Notification>()?.copy(id = doc.id)
                                    Log.d("NotificationsScreen", "Notification: $notification")
                                    notification
                                } catch (e: Exception) {
                                    Log.e("NotificationsScreen", "Error parsing notification from doc ${doc.id}", e)
                                    null
                                }
                            }

                            // Fetch sender profile images asíncrono
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val notificationsWithImages = notificationsList.map { notification ->
                                        Log.d("NotificationsScreen", "Fetching profile for sender: ${notification.senderId}")
                                        val senderDoc = db.collection("perfil").document(notification.senderId).get().await()
                                        val profileImageUrl = senderDoc.getString("profileImageUrl")
                                        val senderName = senderDoc.getString("nombre") ?: notification.senderName
                                        Log.d("NotificationsScreen", "Profile image url: $profileImageUrl")
                                        notification.copy(
                                            senderProfileImageUrl = profileImageUrl,
                                            senderName = senderName
                                        )
                                    }

                                    // Actualizar en el hilo principal
                                    withContext(Dispatchers.Main) {
                                        notifications = notificationsWithImages
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    Log.e("NotificationsScreen", "Error fetching profile images", e)
                                    withContext(Dispatchers.Main) {
                                        notifications = notificationsList
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }

                // Cleanup del listener cuando el componente se destruya
                // (En una implementación real, deberías guardarlo en un DisposableEffect)
            } catch (e: Exception) {
                Log.e("NotificationsScreen", "Error setting up notifications listener", e)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Marcar notificaciones como leídas cuando se abra la pantalla
    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty() && currentUserId != null) {
            val unreadNotifications = notifications.filter { !it.read }
            if (unreadNotifications.isNotEmpty()) {
                Log.d("NotificationsScreen", "Marking ${unreadNotifications.size} notifications as read")
                unreadNotifications.forEach { notification ->
                    db.collection("notifications").document(notification.id)
                        .update("read", true)
                        .addOnFailureListener { e ->
                            Log.e("NotificationsScreen", "Error marking notification as read", e)
                        }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notificaciones", color = MaterialTheme.colorScheme.onSurface)
                        if (notifications.any { !it.read }) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = notifications.count { !it.read }.toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // Botón para marcar todas como leídas
                    if (notifications.any { !it.read }) {
                        IconButton(
                            onClick = {
                                notifications.filter { !it.read }.forEach { notification ->
                                    db.collection("notifications").document(notification.id)
                                        .update("read", true)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Marcar todas como leídas",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No tienes notificaciones",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Cuando alguien interactúe con tus planes, aparecerán aquí",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            onNotificationClick = {
                                // Marcar como leída al hacer clic
                                if (!notification.read) {
                                    db.collection("notifications").document(notification.id)
                                        .update("read", true)
                                }
                                navigateToPlanDetail(notification.planId)
                            },
                            onProfileClick = {
                                navigateToUserProfile(notification.senderId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.read) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.read) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = notification.senderProfileImageUrl,
                        error = painterResource(id = android.R.drawable.ic_menu_myplaces)
                    ),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    contentScale = ContentScale.Crop
                )

                // Indicador de tipo de notificación
                val (icon, color) = when (notification.type) {
                    "like" -> Icons.Default.Favorite to Color.Red
                    "comment" -> Icons.Default.Email to Color.Blue
                    "participate" -> Icons.Default.Face to Color.Green
                    "share" -> Icons.Default.Share to Color.Black
                    else -> Icons.Default.Notifications to MaterialTheme.colorScheme.primary
                }

                Icon(
                    icon,
                    contentDescription = notification.type,
                    modifier = Modifier
                        .size(16.dp)
                        .background(color, CircleShape)
                        .padding(2.dp)
                        .align(Alignment.BottomEnd),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.message,
                    fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                notification.timestamp?.let { timestamp ->
                    Text(
                        text = formatTimestamp(timestamp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Indicador de no leída
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val now = System.currentTimeMillis()
    val time = timestamp.toDate().time
    val diff = now - time

    return when {
        diff < 60_000 -> "Ahora"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> "${diff / 604_800_000}sem"
    }
}
