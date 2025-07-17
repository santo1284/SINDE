package com.santiago.sindesparches.presentation.notifications

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "",
    val planId: String = "",
    val planTitle: String = "",
    val message: String = "",
    val read: Boolean = false,
    val senderProfileImageUrl: String? = null
)

suspend fun getNotifications(db: FirebaseFirestore, userId: String): List<Notification> {
    val notifications = mutableListOf<Notification>()
    try {
        val snapshot = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        for (document in snapshot.documents) {
            val notification = document.toObject<Notification>()
            if (notification != null) {
                val senderProfileImageUrl = getSenderProfileImageUrl(db, notification.senderId)
                notifications.add(notification.copy(id = document.id, senderProfileImageUrl = senderProfileImageUrl))
            }
        }
    } catch (e: Exception) {
        Log.e("NotificationScreen", "Error getting notifications", e)
    }
    return notifications
}

suspend fun getSenderProfileImageUrl(db: FirebaseFirestore, senderId: String): String? {
    return try {
        val userDoc = db.collection("perfil").document(senderId).get().await()
        userDoc.getString("profileImageUrl")
    } catch (e: Exception) {
        null
    }
}
@Composable
fun NotificationScreen(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    navigateToUserProfile: (String) -> Unit,
    navigateToPlanDetail: (String) -> Unit
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = auth.currentUser?.uid

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            notifications = getNotifications(db, currentUserId)
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (notifications.isEmpty()) {
            Text(
                text = "No tienes notificaciones",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onNotificationClick = {
                            if (notification.planId.isNotEmpty()) {
                                navigateToPlanDetail(notification.planId)
                            }
                        },
                        onProfileClick = {
                            if (notification.senderId.isNotEmpty()) {
                                navigateToUserProfile(notification.senderId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = notification.senderProfileImageUrl),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = notification.senderName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = notification.message,
                    color = Color.White
                )
                Text(
                    text = "Plan: ${notification.planTitle}",
                    color = Color.Gray
                )
            }
        }
    }
}
