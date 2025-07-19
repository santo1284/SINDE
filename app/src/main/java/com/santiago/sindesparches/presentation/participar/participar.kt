package com.santiago.sindesparches.presentation.participar

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    // Colores vibrantes para tema nocturno
    val nightColors = listOf(
        Color(0xFF001312), // Azul marino profundo
        Color(0xFF01312B), // Azul oscuro
        Color(0xFF02574D), // Azul medianoche
        Color(0xFF01312B), // Azul oscuro
        Color(0xFF001312), // Púrpura profundo
    )

    val accentColors = listOf(
        Color(0xFF3498DB), // Azul brillante - principal para participación
        Color(0xFF1ABC9C), // Verde esmeralda
        Color(0xFF9B59B6), // Púrpura vibrante
        Color(0xFFF39C12), // Naranja dorado
        Color(0xFFE94560), // Rojo vibrante
        Color(0xFFE74C3C), // Rojo coral
    )

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

    val gradientOffsetfondo by rememberInfiniteTransition().animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Fondo animado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = nightColors,
                    startY = gradientOffsetfondo * 2000f,
                    endY = (gradientOffsetfondo + 2f) * 2000f
                )
            )
    ) {
        // Canvas con partículas animadas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Partículas flotantes
            val particleCount = 80
            repeat(particleCount) { i ->
                val x = (i * 137.5f + gradientOffset * 200f) % size.width
                val y = (i * 73.2f + gradientOffset * 150f) % size.height
                val radius = (2f + sin(gradientOffset * 2f + i) * 1.5f).coerceAtLeast(0.5f)

                drawCircle(
                    color = accentColors[i % accentColors.size].copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Icono con glow effect - tema participación
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF3498DB), // Azul brillante
                                                Color(0xFF1ABC9C)  // Verde esmeralda
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Face, // Icono de grupo para participación
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                "Mis Participaciones",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = navigatehome,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> {
                        ParticipationLoadingSection(accentColors)
                    }

                    errorMessage != null -> {
                        ParticipationErrorSection(
                            errorMessage = errorMessage!!,
                            onRetry = { reloadData() },
                            accentColors = accentColors
                        )
                    }

                    participatingPlans.isEmpty() -> {
                        ParticipationEmptySection(accentColors)
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(participatingPlans, key = { it.id }) { plan ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it / 2 },
                                        animationSpec = tween(600)
                                    ) + fadeIn(animationSpec = tween(600)),
                                    modifier = Modifier.animateItemPlacement()
                                ) {
                                    PlanCardWithParticipationTheme(
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
                                            commentCount = plan.commentCount ?: 0
                                        ),
                                        onPlanClick = { navigateToDetail_Plan(plan.id) },
                                        currentUserId = currentUserId,
                                        db = db,
                                        coroutineScope = coroutineScope,
                                        context = context,
                                        navigateToUserProfile = navigateToUserProfile,
                                        navigateToEditPlan = { /* Empty lambda */ },
                                        navigateToMiPerfil = navigateToMiPerfil,
                                        navigateToComments = navigateToComments,
                                        accentColors = accentColors
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
fun ParticipationLoadingSection(accentColors: List<Color>) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Loading indicator personalizado con tema de participación
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .rotate(rotationAngle)
            ) {
                repeat(8) { i ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .offset(
                                x = (28f * cos(i * 45f * PI / 180f)).dp,
                                y = (28f * sin(i * 45f * PI / 180f)).dp
                            )
                            .background(
                                accentColors[i % accentColors.size].copy(
                                    alpha = 0.3f + 0.7f * ((rotationAngle / 45f + i) % 8f) / 8f
                                ),
                                CircleShape
                            )
                    )
                }
            }

            Text(
                "Cargando tus participaciones...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ParticipationErrorSection(
    errorMessage: String,
    onRetry: () -> Unit,
    accentColors: List<Color>
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE74660),
                                Color(0xFFF39C12)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                errorMessage,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF3498DB),
                                Color(0xFF1ABC9C)
                            )
                        ),
                        shape = RoundedCornerShape(25.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    "Reintentar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ParticipationEmptySection(accentColors: List<Color>) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val groupScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "groupScale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(groupScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3498DB).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF3498DB),
                    modifier = Modifier.size(60.dp)
                )
            }

            Text(
                "¡Únete a eventos increíbles!",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Participa en eventos que te interesen y aparecerán aquí para que no los olvides",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Extensión para aplicar tema nocturno al PlanCard con enfoque en participación
@Composable
fun PlanCardWithParticipationTheme(
    plan: com.santiago.sindesparches.presentation.publicaciones.Plan,
    onPlanClick: () -> Unit,
    currentUserId: String,
    db: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    context: Context,
    navigateToUserProfile: (String) -> Unit,
    navigateToEditPlan: () -> Unit,
    navigateToMiPerfil: () -> Unit,
    navigateToComments: (String) -> Unit,
    accentColors: List<Color>
) {
    // Crear un CompositionLocalProvider para personalizar los colores
    CompositionLocalProvider(
        LocalContentColor provides Color.White
    ) {
        // Wrapper con efectos visuales enfocados en participación
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(1.dp) // Para el borde
        ) {
            // Borde con gradiente de participación (azul-verde)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF3498DB), // Azul brillante
                                Color(0xFF1ABC9C), // Verde esmeralda
                                Color(0xFF9B59B6)  // Púrpura vibrante
                            )
                        )
                    )
            )

            // PlanCard original
            com.santiago.sindesparches.presentation.home.PlanCard(
                plan = plan,
                onPlanClick = onPlanClick,
                currentUserId = currentUserId,
                db = db,
                coroutineScope = coroutineScope,
                context = context,
                navigateToUserProfile = navigateToUserProfile,
                navigateToEditPlan = { _ -> navigateToEditPlan() },
                navigateToMiPerfil = navigateToMiPerfil,
                navigateToComments = navigateToComments
            )
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

