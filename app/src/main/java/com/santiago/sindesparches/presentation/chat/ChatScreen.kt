package com.santiago.sindesparches.presentation.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    planId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var newMessage by remember { mutableStateOf("") }
    val userId = auth.currentUser?.uid

    LaunchedEffect(planId) {
        val participants = listOfNotNull(userId, getPlanOwnerId(db, planId)).sorted()
        val chatQuery = db.collection("chats")
            .whereEqualTo("planId", planId)
            .whereEqualTo("participants", participants)
            .limit(1)
            .get()
            .await()

        if (chatQuery.isEmpty) {
            val newChat = Chat(planId = planId, participants = participants)
            db.collection("chats").add(newChat).await()
        }
    }

    suspend fun getPlanOwnerId(db: FirebaseFirestore, planId: String): String? {
        return try {
            val planDoc = db.collection("planes").document(planId).get().await()
            planDoc.getString("userId")
        } catch (e: Exception) {
            null
        }

        val chatId = chatQuery.documents.firstOrNull()?.id
        if (chatId != null) {
            db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        val messageList = it.documents.mapNotNull { doc ->
                            doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                        }
                        messages.clear()
                        messages.addAll(messageList)
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat del Plan") },
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(messages) { message ->
                    MessageItem(message = message, isCurrentUser = message.senderId == userId)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") }
                )
                IconButton(onClick = {
                    if (newMessage.isNotBlank() && userId != null) {
                        coroutineScope.launch {
                            val chatQuery = db.collection("chats").whereEqualTo("planId", planId).limit(1).get().await()
                            val chatId = chatQuery.documents.firstOrNull()?.id
                            if (chatId != null) {
                                val userDoc = db.collection("perfil").document(userId).get().await()
                                val userName = userDoc.getString("nombre") ?: "Usuario"
                                val profilePictureUrl = userDoc.getString("profileImageUrl")
                                val tempMessage = ChatMessage(
                                    id = "temp_${System.currentTimeMillis()}",
                                    senderId = userId,
                                    senderName = userName,
                                    message = newMessage,
                                    timestamp = com.google.firebase.Timestamp.now(),
                                    senderProfilePictureUrl = profilePictureUrl,
                                    status = MessageStatus.SENDING
                                )
                                messages.add(tempMessage)
                                val messageToSend = tempMessage.copy(id = "", status = MessageStatus.SENT)
                                try {
                                    db.collection("chats").document(chatId).collection("messages").add(messageToSend).await()
                                    messages.remove(tempMessage)
                                } catch (e: Exception) {
                                    val index = messages.indexOf(tempMessage)
                                    if (index != -1) {
                                        messages[index] = tempMessage.copy(status = MessageStatus.FAILED)
                                    }
                                }
                                newMessage = ""
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage, isCurrentUser: Boolean) {
    val messageColor = when (message.status) {
        MessageStatus.SENDING -> Color.Gray
        MessageStatus.SENT -> if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        MessageStatus.FAILED -> Color.Red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            Image(
                painter = rememberAsyncImagePainter(model = message.senderProfilePictureUrl),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column {
            Text(text = message.senderName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = messageColor
            ) {
                Text(
                    text = message.message,
                    modifier = Modifier.padding(12.dp),
                    color = if (isCurrentUser) Color.White else Color.Black
                )
            }
        }
        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = rememberAsyncImagePainter(model = message.senderProfilePictureUrl),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
