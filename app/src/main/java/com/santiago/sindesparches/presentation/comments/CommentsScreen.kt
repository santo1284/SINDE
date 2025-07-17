package com.santiago.sindesparches.presentation.comments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.santiago.sindesparches.presentation.publicaciones.Plan
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    planId: String,
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    navigateBack: () -> Unit,
    navigateToUserProfile: (String) -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var planOwnerId by remember { mutableStateOf<String?>(null) }
    val currentUserId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(planId) {
        isLoading = true
        // Fetch plan owner ID
        val planDoc = db.collection("planes").document(planId).get().await()
        val plan = planDoc.toObject<Plan>()
        planOwnerId = plan?.userId

        val commentsQuery = db.collection("planes").document(planId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()

        val commentsList = commentsQuery.documents.mapNotNull { doc ->
            val comment = doc.toObject<Comment>()?.copy(id = doc.id)
            comment
        }

        val commentsWithImages = commentsList.map { comment ->
            val userDoc = db.collection("perfil").document(comment.userId).get().await()
            var profileImageUrl = userDoc.getString("profileImageUrl")
            try {
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("profile_pictures/${comment.userId}")
                profileImageUrl = storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                // Use the profileImageUrl from Firestore as a fallback
            }
            comment.copy(userProfileImageUrl = profileImageUrl)
        }

        comments = commentsWithImages
        isLoading = false
    }

    fun addComment() {
        if (newCommentText.isNotBlank() && currentUserId != null) {
            coroutineScope.launch {
                val userDoc = db.collection("perfil").document(currentUserId).get().await()
                val userName = userDoc.getString("nombre") ?: "Usuario"
                val userProfileImageUrl = userDoc.getString("profileImageUrl")

                val comment = Comment(
                    planId = planId,
                    userId = currentUserId,
                    userName = userName,
                    userProfileImageUrl = userProfileImageUrl,
                    text = newCommentText,
                    timestamp = FieldValue.serverTimestamp()
                )

                db.collection("planes").document(planId).collection("comments")
                    .add(comment)
                    .await()

                newCommentText = ""

                // Refresh comments
                val commentsQuery = db.collection("planes").document(planId).collection("comments")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val commentsList = commentsQuery.documents.mapNotNull { doc ->
                    val comment = doc.toObject<Comment>()?.copy(id = doc.id)
                    comment
                }

                val commentsWithImages = commentsList.map { c ->
                    val uDoc = db.collection("perfil").document(c.userId).get().await()
                    var pImageUrl = uDoc.getString("profileImageUrl")
                    try {
                        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("profile_pictures/${c.userId}")
                        pImageUrl = storageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        // Use the profileImageUrl from Firestore as a fallback
                    }
                    c.copy(userProfileImageUrl = pImageUrl)
                }

                comments = commentsWithImages
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comentarios") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            isPlanOwner = comment.userId == planOwnerId,
                            onProfileClick = { navigateToUserProfile(comment.userId) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un comentario...") }
                )
                IconButton(onClick = { addComment() }) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isPlanOwner: Boolean,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlanOwner) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = comment.userProfileImageUrl,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPlanOwner) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Autor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(text = comment.text)
            }
        }
    }
}
