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
    val myPlans = remember { mutableStateOf<List<Plan>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading.value = true
            val plansQuery = db.collection("planes")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val planList = plansQuery.documents.mapNotNull { doc ->
                doc.toObject(Plan::class.java)?.copy(id = doc.id)
            }
            myPlans.value = planList
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        CircularProgressIndicator()
    } else if (myPlans.value.isEmpty()) {
        Text("No has creado ningún plan.")
    } else {
        LazyColumn {
            items(myPlans.value) { plan ->
                // Aquí deberías mostrar una lista de chats por cada plan
                Text(text = plan.title, modifier = Modifier.clickable { navigateToChat(plan.id) })
            }
        }
    }
}

@Composable
fun MyMessagesChats(auth: FirebaseAuth, db: FirebaseFirestore, navigateToChat: (String) -> Unit) {
    val userId = auth.currentUser?.uid
    val myMessages = remember { mutableStateOf<List<Chat>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading.value = true
            val chatsQuery = db.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            val chatList = chatsQuery.documents.mapNotNull { doc ->
                val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                // Filtrar los chats donde el usuario no es el creador del plan
                val planId = chat?.planId ?: ""
                val planDoc = db.collection("planes").document(planId).get().await()
                val plan = planDoc.toObject(Plan::class.java)
                if (plan?.userId != userId) {
                    chat
                } else {
                    null
                }
            }
            myMessages.value = chatList
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        CircularProgressIndicator()
    } else if (myMessages.value.isEmpty()) {
        Text("No has iniciado ninguna conversación.")
    } else {
        LazyColumn {
            items(myMessages.value) { chat ->
                ChatItem(chat = chat, db = db, onChatClick = {
                    navigateToChat(chat.planId)
                })
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
