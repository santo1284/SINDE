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

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            try {
                isLoading = true
                Log.d("NotificationsScreen", "Fetching notifications for user: $currentUserId")
                val notificationsQuery = db.collection("notifications")
                    .whereEqualTo("recipientId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                Log.d("NotificationsScreen", "Found ${notificationsQuery.documents.size} notifications")

                val notificationsList = notificationsQuery.documents.mapNotNull { doc ->
                    val notification = doc.toObject<Notification>()?.copy(id = doc.id)
                    Log.d("NotificationsScreen", "Notification: $notification")
                    notification
                }

                // Fetch sender profile images
                val notificationsWithImages = notificationsList.map { notification ->
                    Log.d("NotificationsScreen", "Fetching profile for sender: ${notification.senderId}")
                    val senderDoc = db.collection("perfil").document(notification.senderId).get().await()
                    val profileImageUrl = senderDoc.getString("profileImageUrl")
                    Log.d("NotificationsScreen", "Profile image url: $profileImageUrl")
                    notification.copy(senderProfileImageUrl = profileImageUrl)
                }

                notifications = notificationsWithImages
                isLoading = false
            } catch (e: Exception) {
                Log.e("NotificationsScreen", "Error fetching notifications", e)
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (notifications.isEmpty()) {
                Text("No tienes notificaciones", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onNotificationClick = {
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
            .padding(vertical = 4.dp)
            .clickable { onNotificationClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = notification.senderProfileImageUrl,
                    error = painterResource(id = android.R.drawable.ic_menu_myplaces)
                ),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = notification.senderName,
                    fontWeight = FontWeight.Bold
                )
                Text(text = notification.message)
            }
        }
    }
}
