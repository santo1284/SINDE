package com.santiago.sindesparches.presentation.chat

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.santiago.sindesparches.presentation.publicaciones.Plan
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateToChat: (String) -> Unit,
    navigateBack: () -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Mis Planes", "Mis Mensajes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Chats") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }
            when (tabIndex) {
                0 -> MyPlansChats(auth, db, navigateToChat)
                1 -> MyMessagesChats(auth, db, navigateToChat)
            }
        }
    }
}

@Composable
fun MyPlansChats(auth: FirebaseAuth, db: FirebaseFirestore, navigateToChat: (String) -> Unit) {
    val userId = auth.currentUser?.uid
    val privateChats = remember { mutableStateOf<List<PrivateChat>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading.value = true
            val privateChatsQuery = db.collection("private_chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            val privateChatList = privateChatsQuery.documents.mapNotNull { doc ->
                val privateChat = doc.toObject(PrivateChat::class.java)?.copy(id = doc.id)
                val planId = privateChat?.planId ?: ""
                val planDoc = db.collection("planes").document(planId).get().await()
                val plan = planDoc.toObject(Plan::class.java)
                if (plan?.userId == userId) {
                    privateChat
                } else {
                    null
                }
            }
            privateChats.value = privateChatList
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        CircularProgressIndicator()
    } else if (privateChats.value.isEmpty()) {
        Text("Nadie te ha escrito aún.")
    } else {
        LazyColumn {
            items(privateChats.value) { privateChat ->
                PrivateChatItem(privateChat = privateChat, db = db, currentUserId = userId, onPrivateChatClick = {
                    navigateToChat(privateChat.id)
                })
            }
        }
    }
}

@Composable
fun MyMessagesChats(auth: FirebaseAuth, db: FirebaseFirestore, navigateToChat: (String) -> Unit) {
    val userId = auth.currentUser?.uid
    val privateChats = remember { mutableStateOf<List<PrivateChat>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading.value = true
            val privateChatsQuery = db.collection("private_chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            val privateChatList = privateChatsQuery.documents.mapNotNull { doc ->
                val privateChat = doc.toObject(PrivateChat::class.java)?.copy(id = doc.id)
                val planId = privateChat?.planId ?: ""
                val planDoc = db.collection("planes").document(planId).get().await()
                val plan = planDoc.toObject(Plan::class.java)
                if (plan?.userId != userId) {
                    privateChat
                } else {
                    null
                }
            }
            privateChats.value = privateChatList
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        CircularProgressIndicator()
    } else if (privateChats.value.isEmpty()) {
        Text("No has iniciado ninguna conversación.")
    } else {
        LazyColumn {
            items(privateChats.value) { privateChat ->
                PrivateChatItem(privateChat = privateChat, db = db, currentUserId = userId, onPrivateChatClick = {
                    navigateToChat(privateChat.id)
                })
            }
        }
    }
}

@Composable
fun PrivateChatItem(privateChat: PrivateChat, db: FirebaseFirestore, currentUserId: String?, onPrivateChatClick: () -> Unit) {
    var otherUser by remember { mutableStateOf<com.santiago.sindesparches.presentation.plan_detail.UserProfile?>(null) }
    var plan by remember { mutableStateOf<Plan?>(null) }

    LaunchedEffect(privateChat) {
        val otherUserId = privateChat.participants.find { it != currentUserId }
        if (otherUserId != null) {
            val userDoc = db.collection("perfil").document(otherUserId).get().await()
            otherUser = userDoc.toObject(com.santiago.sindesparches.presentation.plan_detail.UserProfile::class.java)
        }
        val planDoc = db.collection("planes").document(privateChat.planId).get().await()
        plan = planDoc.toObject(Plan::class.java)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPrivateChatClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = if (plan?.userId == currentUserId) otherUser?.profileImageUrl else plan?.imageUrls?.firstOrNull()
        Image(
            painter = rememberAsyncImagePainter(model = imageUrl),
            contentDescription = "Imagen",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            if (plan?.userId == currentUserId) {
                Text(text = plan?.title ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(text = otherUser?.nombre ?: "Cargando...", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(text = plan?.title ?: "Cargando...", style = MaterialTheme.typography.titleMedium)
                Text(text = otherUser?.nombre ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Text(text = privateChat.lastMessage?.message ?: "No hay mensajes", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
