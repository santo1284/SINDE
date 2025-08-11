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
import com.santiago.sindesparches.data.models.Plan
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.presentation.notifications.sendNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    planId: String,
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    context: Context,
    navigateBack: () -> Unit,
    navigateToUserProfile: (String) -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isSendingComment by remember { mutableStateOf(false) } // Estado para el botón de envío
    var planOwnerId by remember { mutableStateOf<String?>(null) }
    var lastVisibleComment by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var hasMoreComments by remember { mutableStateOf(true) }
    val currentUserId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Constantes para paginación
    val INITIAL_LOAD_SIZE = 10
    val PAGE_SIZE = 5

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

    // Colores modernos para modo oscuro
    val primaryColor = Color(0xFF6C63FF)
    val secondaryColor = Color(0xFF00D4AA)
    val accentColor = Color(0xFFFF6B9D)
    val surfaceColor = Color(0xFF1A1A2E)
    val backgroundDark = Color(0xFF0F0F1E)
    val cardColor = Color(0xFF252543)

    // Función para obtener imagen de perfil del usuario
    suspend fun getUserWithProfileImage(comment: Comment): Comment {
        val userDoc = db.collection("perfil").document(comment.userId).get().await()
        var profileImageUrl = userDoc.getString("profileImageUrl")
        try {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/${comment.userId}")
            profileImageUrl = storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            // Fallback
        }
        return comment.copy(userProfileImageUrl = profileImageUrl)
    }

    // Función para cargar comentarios iniciales (primeros 10)
    suspend fun loadInitialComments() {
        val planDoc = db.collection("planes").document(planId).get().await()
        val plan = planDoc.toObject<Plan>()
        planOwnerId = plan?.userId

        val commentsQuery = db.collection("planes").document(planId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(INITIAL_LOAD_SIZE.toLong())
            .get()
            .await()

        val commentsList = commentsQuery.documents.mapNotNull { doc ->
            val comment = doc.toObject<Comment>()?.copy(id = doc.id)
            comment
        }

        val commentsWithImages = commentsList.map { comment ->
            getUserWithProfileImage(comment)
        }

        comments = commentsWithImages

        // Actualizar el último documento visible para paginación
        if (commentsQuery.documents.isNotEmpty()) {
            lastVisibleComment = commentsQuery.documents.last()
            hasMoreComments = commentsQuery.documents.size == INITIAL_LOAD_SIZE
        } else {
            hasMoreComments = false
        }
    }

    // Función para cargar más comentarios (paginación)
    suspend fun loadMoreComments() {
        if (!hasMoreComments || isLoadingMore || lastVisibleComment == null) return

        isLoadingMore = true

        val moreCommentsQuery = db.collection("planes").document(planId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisibleComment!!)
            .limit(PAGE_SIZE.toLong())
            .get()
            .await()

        val moreCommentsList = moreCommentsQuery.documents.mapNotNull { doc ->
            val comment = doc.toObject<Comment>()?.copy(id = doc.id)
            comment
        }

        if (moreCommentsList.isNotEmpty()) {
            val moreCommentsWithImages = moreCommentsList.map { comment ->
                getUserWithProfileImage(comment)
            }

            // Agregar los comentarios más antiguos al final de la lista
            comments = comments + moreCommentsWithImages
            lastVisibleComment = moreCommentsQuery.documents.lastOrNull()
            hasMoreComments = moreCommentsQuery.documents.size == PAGE_SIZE
        } else {
            hasMoreComments = false
        }

        isLoadingMore = false
    }

    // Función para cargar comentarios nuevos (más recientes que el primero actual)
    suspend fun loadNewComments() {
        if (comments.isEmpty()) return

        val firstComment = comments.first()
        val firstCommentTimestamp = firstComment.timestamp as? com.google.firebase.Timestamp
            ?: return

        val newCommentsQuery = db.collection("planes").document(planId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .endBefore(firstCommentTimestamp)
            .get()
            .await()

        val newCommentsList = newCommentsQuery.documents.mapNotNull { doc ->
            val comment = doc.toObject<Comment>()?.copy(id = doc.id)
            comment
        }

        if (newCommentsList.isNotEmpty()) {
            val newCommentsWithImages = newCommentsList.map { comment ->
                getUserWithProfileImage(comment)
            }

            // Agregar los nuevos comentarios al principio
            comments = newCommentsWithImages + comments
        }
    }

    // Detector de scroll para cargar más comentarios
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: -1
                val totalItems = comments.size

                // Si estamos cerca del final (últimos 3 elementos), cargar más
                if (lastVisibleItemIndex >= totalItems - 3 && hasMoreComments && !isLoadingMore) {
                    loadMoreComments()
                }
            }
    }

    LaunchedEffect(planId) {
        isLoading = true
        loadInitialComments()
        isLoading = false
    }

    fun addComment() {
        if (newCommentText.isNotBlank() && currentUserId != null && !isSendingComment) {
            isSendingComment = true // Activar estado de carga

            coroutineScope.launch {
                try {
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

                    // Agregar comentario a Firebase
                    val docRef = db.collection("planes").document(planId).collection("comments")
                        .add(comment)
                        .await()

                    val planRef = db.collection("planes").document(planId)
                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(planRef)
                        val newCommentCount = (snapshot.getLong("commentCount") ?: 0) + 1
                        transaction.update(planRef, "commentCount", newCommentCount)
                    }.await()

                    if (planOwnerId != null && planOwnerId != currentUserId) {
                        sendNotification(
                            db = db,
                            recipientId = planOwnerId!!,
                            senderId = currentUserId,
                            type = "comment",
                            planId = planId,
                            context = context
                        )
                    }

                    // Crear comentario optimista (aparece inmediatamente)
                    val optimisticComment = comment.copy(
                        id = docRef.id,
                        timestamp = com.google.firebase.Timestamp.now(),
                        userProfileImageUrl = userProfileImageUrl
                    )

                    // Agregar el comentario al principio de la lista inmediatamente
                    comments = listOf(optimisticComment) + comments
                    newCommentText = ""

                    // Scroll al inicio para mostrar el nuevo comentario
                    listState.animateScrollToItem(0)

                    // Actualizar la imagen de perfil del comentario recién agregado
                    try {
                        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$currentUserId")
                        val imageUrl = storageRef.downloadUrl.await().toString()

                        val updatedComments = comments.toMutableList()
                        if (updatedComments.isNotEmpty() && updatedComments[0].id == docRef.id) {
                            updatedComments[0] = updatedComments[0].copy(userProfileImageUrl = imageUrl)
                            comments = updatedComments
                        }
                    } catch (e: Exception) {
                        // La imagen de perfil se mantiene como estaba
                    }
                } catch (e: Exception) {
                    // Manejar error (puedes mostrar un mensaje al usuario)
                    // Por ejemplo, usando un SnackBar o Toast
                } finally {
                    isSendingComment = false // Desactivar estado de carga
                }
            }
        }
    }

    // Función para actualizar comentarios (pull to refresh)
    fun refreshComments() {
        coroutineScope.launch {
            loadNewComments()
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
                            "Comentarios (${comments.size})",
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
                        // Botón de refresh
                        IconButton(
                            onClick = { refreshComments() }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = primaryColor
                            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                                "Cargando comentarios...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = comments.size,
                            key = { index -> comments[index].id }
                        ) { index ->
                            val comment = comments[index]
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { -it },
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = if (index == 0) 0 else 50
                                    )
                                ) + fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = if (index == 0) 0 else 50
                                    )
                                )
                            ) {
                                ModernCommentItem(
                                    comment = comment,
                                    isPlanOwner = comment.userId == planOwnerId,
                                    onProfileClick = { navigateToUserProfile(comment.userId) },
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    cardColor = cardColor
                                )
                            }
                        }

                        // Indicador de carga de más comentarios
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = primaryColor,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Cargando más comentarios...",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Mensaje cuando no hay más comentarios
                        if (!hasMoreComments && comments.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "✨ Has visto todos los comentarios",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }

                // Barra de comentario moderna con botón mejorado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                cardColor,
                                RoundedCornerShape(25.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Escribe algo increíble...",
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = primaryColor,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3,
                            enabled = !isSendingComment // Desabilitar mientras se envía
                        )

                        AnimatedVisibility(
                            visible = newCommentText.isNotBlank(),
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            IconButton(
                                onClick = { addComment() },
                                enabled = !isSendingComment, // Desabilitar botón mientras se envía
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = if (isSendingComment) {
                                            // Color más opaco cuando está enviando
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0.5f),
                                                    secondaryColor.copy(alpha = 0.5f)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(primaryColor, secondaryColor)
                                            )
                                        },
                                        CircleShape
                                    )
                            ) {
                                if (isSendingComment) {
                                    // Mostrar indicador de carga
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    // Mostrar ícono de envío
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Enviar",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernCommentItem(
    comment: Comment,
    isPlanOwner: Boolean,
    onProfileClick: () -> Unit,
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
            .clickable { },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlanOwner)
                primaryColor.copy(alpha = 0.15f)
            else
                cardColor.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Efecto de brillo sutil
            if (isPlanOwner) {
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
                // Avatar con efecto glow
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = comment.userProfileImageUrl,
                            error = painterResource(id = android.R.drawable.ic_menu_myplaces)
                        ),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (isPlanOwner) primaryColor else secondaryColor,
                                CircleShape
                            )
                            .clickable { onProfileClick() },
                        contentScale = ContentScale.Crop
                    )

                    // Indicador de estado online (simulado)
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(secondaryColor, CircleShape)
                            .border(2.dp, cardColor, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = comment.userName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        if (isPlanOwner) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(primaryColor, accentColor)
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✨ Autor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Tiempo (puedes agregar esta funcionalidad)
                        Text(
                            text = formatCommentTime(comment.timestamp),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = comment.text,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


// Función para formatear el tiempo transcurrido desde la creación del comentario
fun formatCommentTime(timestamp: Any?): String {
    return try {
        val firebaseTimestamp = timestamp as? com.google.firebase.Timestamp
        if (firebaseTimestamp != null) {
            val commentTime = firebaseTimestamp.toDate()
            val now = Date()
            val diffInMillis = now.time - commentTime.time

            when {
                diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "ahora"
                diffInMillis < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                    "${minutes}m"
                }
                diffInMillis < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                    "${hours}h"
                }
                diffInMillis < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                    "${days}d"
                }
                diffInMillis < TimeUnit.DAYS.toMillis(365) -> {
                    val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                    formatter.format(commentTime)
                }
                else -> {
                    val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    formatter.format(commentTime)
                }
            }
        } else {
            "ahora"
        }
    } catch (e: Exception) {
        "ahora"
    }
}