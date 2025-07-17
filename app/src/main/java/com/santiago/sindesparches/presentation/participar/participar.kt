package com.santiago.sindesparches.presentation.participar

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.white
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

// Data class para los planes
data class Plan(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val timeString: String = "",
    val userId: String = "",
    val imageUrls: List<String> = emptyList(),
    val likes: List<String>? = null,
    val participants: List<String>? = null,
    val shares: Int = 0,
    val createdAt: Long? = null,
    val commentCount: Int = 0
)

// Data class para perfiles de usuario
data class UserProfile(
    val id: String = "",
    val displayName: String? = null,
    val profileImageUrl: String? = null,
    val email: String? = null
)

// Nueva data class para mostrar información de usuario en los diálogos
data class UserInfo(
    val id: String,
    val name: String,
    val profileImageUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun planesParticipoScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigatehome: () -> Unit,
    navigateToUserProfile: (String) -> Unit = {},
    navigateToDetail_Plan: (String) -> Unit = {},
    navigateToMiPerfil: () -> Unit = {},
    navigateToComments: (String) -> Unit = {}
) {
    val currentUserId = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var participatingPlans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Función para recargar los datos
    fun reloadData() {
        coroutineScope.launch {
            if (currentUserId.isNotEmpty()) {
                try {
                    isLoading = true
                    errorMessage = null
                    Log.d("PlanesParticipoScreen", "Recargando planes para usuario: $currentUserId")

                    val query = db.collection("planes")
                        .whereArrayContains("participants", currentUserId)

                    val snapshot = query.get().await()
                    Log.d("PlanesParticipoScreen", "Planes encontrados: ${snapshot.documents.size}")

                    val plans = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dateValue = doc.get("date")
                            val dateString = when (dateValue) {
                                is String -> dateValue
                                is Long -> {
                                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                    dateFormat.format(java.util.Date(dateValue))
                                }
                                else -> ""
                            }

                            Plan(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                location = doc.getString("location") ?: "",
                                date = dateString,
                                timeString = doc.getString("timeString") ?: "",
                                userId = doc.getString("userId") ?: "",
                                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                                likes = doc.get("likes") as? List<String> ?: emptyList(),
                                participants = doc.get("participants") as? List<String> ?: emptyList(),
                                shares = (doc.getLong("shares") ?: 0).toInt(),
                                createdAt = doc.getLong("createdAt")
                            )
                        } catch (e: Exception) {
                            Log.e("PlanesParticipoScreen", "Error parsing plan: ${doc.id}", e)
                            null
                        }
                    }

                    participatingPlans = plans.sortedByDescending { it.createdAt ?: 0 }
                    isLoading = false
                } catch (e: Exception) {
                    Log.e("PlanesParticipoScreen", "Error loading participating plans", e)
                    errorMessage = "Error al cargar los planes: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    // Cargar planes inicialmente
    LaunchedEffect(currentUserId) {
        reloadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Planes donde participo",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigatehome) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cargando planes...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                errorMessage!!,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { reloadData() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                participatingPlans.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No participas en ningún plan aún",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Explora planes y únete a los que te interesen",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(participatingPlans, key = { it.id }) { plan ->
                            com.santiago.sindesparches.presentation.home.PlanCard(
                                plan = com.santiago.sindesparches.presentation.publicaciones.Plan(
                                    id = plan.id,
                                    userId = plan.userId,
                                    title = plan.title,
                                    description = plan.description,
                                    date = 0,
                                    timeString = plan.timeString,
                                    location = plan.location,
                                    imageUrls = plan.imageUrls,
                                    likes = plan.likes,
                                    participants = plan.participants,
                                    shares = plan.shares,
                                    createdAt = System.currentTimeMillis(),
                                    commentCount = plan.commentCount
                                ),
                                onPlanClick = { navigateToDetail_Plan(plan.id) },
                                currentUserId = currentUserId,
                                db = db,
                                coroutineScope = coroutineScope,
                                context = context,
                                navigateToUserProfile = navigateToUserProfile,
                                navigateToEditPlan = {},
                                navigateToMiPerfil = navigateToMiPerfil,
                                navigateToComments = navigateToComments
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListDialog(
    title: String,
    userIds: List<String>,
    db: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    var users by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cargar información de los usuarios
    LaunchedEffect(userIds) {
        if (userIds.isNotEmpty()) {
            try {
                isLoading = true
                errorMessage = null

                val userInfoList = mutableListOf<UserInfo>()
                val storage = FirebaseStorage.getInstance().reference

                for (userId in userIds) {
                    try {
                        val userDoc = db.collection("perfil").document(userId).get().await()
                        val userName = userDoc.getString("nombre") ?: "Usuario"

                        // Intentar obtener la imagen del storage primero
                        var profileImageUrl: String? = null
                        try {
                            val storageRef = storage.child("profile_pictures/$userId")
                            val uri = storageRef.downloadUrl.await()
                            profileImageUrl = uri.toString()
                        } catch (e: Exception) {
                            // Si falla, usar la URL del documento
                            profileImageUrl = userDoc.getString("profileImageUrl")
                        }

                        userInfoList.add(
                            UserInfo(
                                id = userId,
                                name = userName,
                                profileImageUrl = profileImageUrl
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("UserListDialog", "Error al cargar usuario $userId", e)
                        // Añadir usuario con información básica si hay error
                        userInfoList.add(
                            UserInfo(
                                id = userId,
                                name = "Usuario",
                                profileImageUrl = null
                            )
                        )
                    }
                }

                users = userInfoList
                isLoading = false
            } catch (e: Exception) {
                Log.e("UserListDialog", "Error general al cargar usuarios", e)
                errorMessage = "Error al cargar usuarios"
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    users.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay usuarios para mostrar",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(users) { user ->
                                UserItem(
                                    user = user,
                                    onClick = { onUserClick(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun UserItem(
    user: UserInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen de perfil
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = "Foto de perfil de ${user.name}",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = CircleShape
                ),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_myplaces),
            placeholder = painterResource(id = android.R.drawable.ic_menu_myplaces)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Nombre del usuario
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Icono de flecha para indicar que es clickeable
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ir al perfil",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}
fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> "${count / 1000}k"
        else -> "${count / 1000000}m"
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Hace un momento"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 2592000000 -> "${diff / 86400000}d"
        else -> "${diff / 2592000000}m"
    }
}

fun formatDate(dateString: String): String {
    return dateString
}