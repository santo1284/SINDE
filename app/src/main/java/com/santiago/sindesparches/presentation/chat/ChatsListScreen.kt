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
    val userId = auth.currentUser?.uid
    val chats = remember { mutableStateOf<List<Chat>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading.value = true
            val chatsQuery = db.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            val chatList = chatsQuery.documents.mapNotNull { doc ->
                doc.toObject(Chat::class.java)?.copy(id = doc.id)
            }
            chats.value = chatList
            isLoading.value = false
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (chats.value.isEmpty()) {
                Text(text = "No tienes chats", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn {
                    items(chats.value) { chat ->
                        ChatItem(chat = chat, db = db, onChatClick = {
                            navigateToChat(chat.planId)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, db: FirebaseFirestore, onChatClick: () -> Unit) {
    var plan by remember { mutableStateOf<Plan?>(null) }

    LaunchedEffect(chat.planId) {
        val planDoc = db.collection("planes").document(chat.planId).get().await()
        plan = planDoc.toObject(Plan::class.java)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = plan?.imageUrls?.firstOrNull()),
            contentDescription = "Imagen del plan",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = plan?.title ?: "Cargando...", style = MaterialTheme.typography.titleMedium)
            // Aquí podrías mostrar el último mensaje del chat
            Text(text = "Último mensaje...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
