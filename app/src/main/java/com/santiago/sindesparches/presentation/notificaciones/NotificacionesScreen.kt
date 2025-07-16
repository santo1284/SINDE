package com.santiago.sindesparches.presentation.notificaciones

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Composable
fun NotificacionesScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateToPlanDetail: (String) -> Unit
) {
    val userId = auth.currentUser?.uid
    val notifications = remember { mutableStateOf<List<Notification>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading.value = true
            val notificationsQuery = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val notificationList = notificationsQuery.documents.mapNotNull { doc ->
                val notification = doc.toObject(Notification::class.java)?.copy(id = doc.id)
                notification
            }

            // Cargar las fotos de perfil de los remitentes
            val notificationsWithProfilePictures = notificationList.map { notification ->
                val senderDoc = db.collection("perfil").document(notification.senderId).get().await()
                val profilePictureUrl = senderDoc.getString("profileImageUrl")
                notification.copy(senderProfilePictureUrl = profilePictureUrl)
            }


            notifications.value = notificationsWithProfilePictures
            isLoading.value = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading.value) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (notifications.value.isEmpty()) {
            Text(text = "No tienes notificaciones", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(notifications.value) { notification ->
                    NotificationItem(notification = notification, onNotificationClick = {
                        navigateToPlanDetail(notification.planId)
                    })
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onNotificationClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = notification.senderProfilePictureUrl),
            contentDescription = "Foto de perfil",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = notification.message)
        }
    }
}
