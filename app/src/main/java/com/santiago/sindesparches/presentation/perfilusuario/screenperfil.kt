package com.santiago.sindesparches.presentation.perfilusuario

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FieldValue
import com.santiago.sindesparches.data.models.Plan
import com.santiago.sindesparches.data.models.UserProfile
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    navigateToPlanDetail: (String) -> Unit = {},
    navigateToProfile: (String) -> Unit = {},
    navigateToMiPerfil: () -> Unit = {},
    navigateToComments: (String) -> Unit = {},
    currentUserId: String
) {
    Log.d("UserProfileScreen", "Recibido userId: '$userId'")

    val coroutineScope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userPlans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Colores del tema oscuro
    val backgroundColor = Color(0xFF0A0A0A)
    val surfaceColor = Color(0xFF1A1A1A)
    val primaryColor = Color(0xFF6C63FF)
    val accentColor = Color(0xFF00D4AA)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)

    // Cargar información del usuario y sus planes
    LaunchedEffect(userId) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                Log.d("UserProfileScreen", "Iniciando carga de datos para userId: $userId")

                // 🔧 LLAMADA DE DEBUG (remover después de solucionar)
                debugFirestoreStructure(db, userId)

                userProfile = getUserProfileById(db, userId)
                Log.d("UserProfileScreen", "Perfil cargado: ${userProfile?.nombre}")

                profileImageUrl = getUserProfileImageUrl(FirebaseStorage.getInstance(), userId)
                Log.d("UserProfileScreen", "Imagen de perfil: $profileImageUrl")

                userPlans = getUserPlans(db, userId)
                Log.d("UserProfileScreen", "Planes cargados: ${userPlans.size}")

                isLoading = false
            } catch (e: Exception) {
                Log.e("UserProfileScreen", "Error al cargar el perfil: ${e.message}", e)
                errorMessage = "Error al cargar el perfil: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Perfil de Usuario",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        modifier = Modifier
                            .background(
                                primaryColor.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        primaryColor = primaryColor
                    )
                }
                errorMessage != null -> {
                    ErrorScreen(
                        errorMessage = errorMessage,
                        primaryColor = primaryColor,
                        textPrimary = textPrimary,
                        onRetry = {
                            coroutineScope.launch {
                                try {
                                    errorMessage = null
                                    isLoading = true
                                    userProfile = getUserProfileById(db, userId)
                                    profileImageUrl = getUserProfileImageUrl(FirebaseStorage.getInstance(), userId)
                                    userPlans = getUserPlans(db, userId)
                                    isLoading = false
                                } catch (e: Exception) {
                                    Log.e("UserProfileScreen", "Error al reintentar: ${e.message}", e)
                                    errorMessage = "Error al cargar el perfil: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
                userProfile == null -> {
                    NotFoundScreen(
                        textPrimary = textPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Header del perfil con diseño mejorado
                        item {
                            ModernProfileHeader(
                                userProfile = userProfile!!,
                                profileImageUrl = profileImageUrl,
                                planCount = userPlans.size,
                                primaryColor = primaryColor,
                                accentColor = accentColor,
                                surfaceColor = surfaceColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        // Sección de publicaciones
                        item {
                            PublicationsHeader(
                                planCount = userPlans.size,
                                textPrimary = textPrimary,
                                accentColor = accentColor
                            )
                        }

                        // Lista de planes o estado vacío
                        if (userPlans.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    surfaceColor = surfaceColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        } else {
                            items(userPlans) { plan ->
                                com.santiago.sindesparches.presentation.home.PlanCard(
                                    plan = plan,
                                    onPlanClick = { navigateToPlanDetail(plan.id) },
                                    currentUserId = currentUserId,
                                    db = db,
                                    coroutineScope = coroutineScope,
                                    context = LocalContext.current,
                                    navigateToUserProfile = navigateToProfile,
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
}

@Composable
private fun ModernProfileHeader(
    userProfile: UserProfile,
    profileImageUrl: String?,
    planCount: Int,
    primaryColor: Color,
    accentColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Fondo con gradiente sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.1f),
                                accentColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Imagen de perfil con anillo de gradiente
                Box(
                    modifier = Modifier.size(140.dp)
                ) {
                    // Anillo de gradiente
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        primaryColor,
                                        accentColor,
                                        primaryColor
                                    )
                                ),
                                CircleShape
                            )
                    )

                    // Imagen de perfil
                    AsyncImage(
                        model = profileImageUrl ?: R.drawable.bxs_user,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(132.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(surfaceColor),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.bxs_user)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Nombre del usuario
                Text(
                    text = userProfile.nombre.takeIf { it.isNotBlank() } ?: "Usuario",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ciudad con icono
                userProfile.ciudad?.takeIf { it.isNotBlank() }?.let { ciudad ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                primaryColor.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ciudad,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Email
                userProfile.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Estadísticas mejoradas
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModernStatisticItem(
                        value = planCount.toString(),
                        label = "Eventos",
                        icon = Icons.Default.DateRange,
                        primaryColor = primaryColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    userProfile.edad?.let { edad ->
                        ModernStatisticItem(
                            value = edad.toString(),
                            label = "Años",
                            icon = Icons.Default.Person,
                            primaryColor = accentColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernStatisticItem(
    value: String,
    label: String,
    icon: ImageVector,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                primaryColor.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PublicationsHeader(
    planCount: Int,
    textPrimary: Color,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Publicaciones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(
                    accentColor.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = planCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(0xFF6C63FF).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF6C63FF),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sin eventos publicados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Este usuario aún no ha compartido ningún evento",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier,
    primaryColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = primaryColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cargando perfil...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorScreen(
    errorMessage: String?,
    primaryColor: Color,
    textPrimary: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Clear,
            contentDescription = null,
            tint = Color(0xFFFF6B6B),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Oops! Algo salió mal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage ?: "Error desconocido",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Reintentar",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NotFoundScreen(
    textPrimary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Clear,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Usuario no encontrado",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No se pudo encontrar el perfil del usuario solicitado",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ✅ FUNCIONES AUXILIARES MEJORADAS CON CONVERSIÓN MANUAL ROBUSTA
private suspend fun getUserProfileById(db: FirebaseFirestore, userId: String): UserProfile? = withContext(Dispatchers.IO) {
    try {
        Log.d("getUserProfileById", "Buscando perfil para userId: '$userId'")

        if (userId.isBlank()) {
            Log.e("getUserProfileById", "UserId está vacío")
            return@withContext null
        }

        val document = db.collection("perfil")
            .document(userId.trim())
            .get()
            .await()

        if (document.exists()) {
            val profile = document.toObject(UserProfile::class.java)?.copy(id = document.id)
            Log.d("getUserProfileById", "Perfil encontrado: ${profile?.nombre}")
            return@withContext profile
        } else {
            Log.w("getUserProfileById", "Documento no existe para userId: '$userId'")
            return@withContext null
        }
    } catch (e: Exception) {
        Log.e("getUserProfileById", "Error obteniendo perfil: ${e.message}", e)
        return@withContext null
    }
}

private suspend fun getUserProfileImageUrl(storage: FirebaseStorage, userId: String): String? = withContext(Dispatchers.IO) {
    try {
        if (userId.isBlank()) {
            Log.e("getUserProfileImageUrl", "UserId está vacío")
            return@withContext null
        }

        Log.d("getUserProfileImageUrl", "Buscando imagen para userId: $userId")

        // Intentar diferentes formatos de imagen
        val possiblePaths = listOf(
            "profile_pictures/$userId",
            "profile_pictures/$userId.jpg",
            "profile_pictures/$userId.png",
            "profile_pictures/$userId.jpeg"
        )

        for (path in possiblePaths) {
            try {
                val imageRef = storage.reference.child(path)
                val url = imageRef.downloadUrl.await().toString()
                Log.d("getUserProfileImageUrl", "Imagen encontrada en: $path")
                return@withContext url
            } catch (e: Exception) {
                // Continuar con el siguiente path
                continue
            }
        }

        Log.w("getUserProfileImageUrl", "No se encontró imagen de perfil para userId: $userId")
        return@withContext null
    } catch (e: Exception) {
        Log.w("getUserProfileImageUrl", "Error general al cargar imagen: ${e.message}")
        return@withContext null
    }
}

// ✅ FUNCIÓN getUserPlans COMPLETAMENTE CORREGIDA CON CONVERSIÓN MANUAL
private suspend fun getUserPlans(db: FirebaseFirestore, userId: String): List<Plan> = withContext(Dispatchers.IO) {
    try {
        if (userId.isBlank()) {
            Log.e("getUserPlans", "UserId está vacío")
            return@withContext emptyList()
        }

        val cleanUserId = userId.trim()
        Log.d("getUserPlans", "🔍 Buscando planes para userId: '$cleanUserId'")

        val documents = db.collection("planes")
            .whereEqualTo("userId", cleanUserId)
            .get()
            .await()

        Log.d("getUserPlans", "🎯 Documentos encontrados para '$cleanUserId': ${documents.size()}")

        // ✅ CONVERSIÓN MANUAL ROBUSTA - MANEJA TANTO TIMESTAMP COMO LONG
        val plans = documents.documents.mapNotNull { document ->
            try {
                val plan = Plan(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    description = document.getString("description") ?: "",
                    location = document.getString("location") ?: "",
                    latitude = document.getDouble("latitude"),
                    longitude = document.getDouble("longitude"),
                    date = document.getLong("date") ?: 0L,
                    timeString = document.getString("timeString") ?: "",
                    userId = document.getString("userId") ?: "",
                    createdAt = when (val created = document.get("createdAt")) {
                        is Long -> created
                        is com.google.firebase.Timestamp -> created.seconds * 1000
                        else -> System.currentTimeMillis()
                    },
                    updatedAt = when (val updated = document.get("updatedAt")) {
                        is Long -> updated
                        is com.google.firebase.Timestamp -> updated.seconds * 1000
                        null -> null
                        else -> null
                    },
                    imageUrls = (document.get("imageUrls") as? List<String>) ?: emptyList(),
                    enableWhatsapp = document.getBoolean("enableWhatsapp") ?: false,
                    phoneNumber = document.getString("phoneNumber") ?: "",
                    likes = (document.get("likes") as? List<String>) ?: emptyList(),
                    participants = (document.get("participants") as? List<String>) ?: emptyList(),
                    shares = document.getLong("shares")?.toInt() ?: 0,
                    commentCount = document.getLong("commentCount")?.toInt() ?: 0
                )

                Log.d("getUserPlans", "✅ Plan cargado: '${plan.title}' (ID: ${plan.id})")
                plan

            } catch (e: Exception) {
                Log.w("getUserPlans", "❌ Error convirtiendo documento ${document.id}: ${e.message}")
                // Log adicional para debug
                Log.d("getUserPlans", "   └─ Datos del documento problemático:")
                document.data?.forEach { (key, value) ->
                    Log.d("getUserPlans", "       $key: $value (${value?.javaClass?.simpleName})")
                }
                null
            }
        }

        val sortedPlans = plans.sortedByDescending { it.createdAt }
        Log.d("getUserPlans", "🎉 Total planes cargados y ordenados: ${sortedPlans.size}")

        return@withContext sortedPlans

    } catch (e: Exception) {
        Log.e("getUserPlans", "💥 Error obteniendo planes: ${e.message}", e)
        return@withContext emptyList()
    }
}

// ✅ FUNCIÓN DEBUG MEJORADA PARA IDENTIFICAR PROBLEMAS DE TIPO
private suspend fun debugFirestoreStructure(db: FirebaseFirestore, targetUserId: String) = withContext(Dispatchers.IO) {
    try {
        Log.d("DEBUG_FIRESTORE", "🔍 === INICIANDO DEBUG DE FIRESTORE ===")
        Log.d("DEBUG_FIRESTORE", "🎯 Usuario objetivo: '$targetUserId'")

        val allPlanes = db.collection("planes").get().await()
        Log.d("DEBUG_FIRESTORE", "📊 Total documentos en colección 'planes': ${allPlanes.size()}")

        allPlanes.documents.forEach { doc ->
            try {
                val userId = doc.getString("userId")
                val title = doc.getString("title")
                val id = doc.id

                // ✅ VERIFICAR TIPOS DE DATOS CRÍTICOS
                val createdAt = doc.get("createdAt")
                val updatedAt = doc.get("updatedAt")

                Log.d("DEBUG_FIRESTORE", "📄 Doc ID: $id")
                Log.d("DEBUG_FIRESTORE", "   └─ userId: '$userId'")
                Log.d("DEBUG_FIRESTORE", "   └─ title: '$title'")
                Log.d("DEBUG_FIRESTORE", "   └─ createdAt tipo: ${createdAt?.javaClass?.simpleName} valor: $createdAt")
                Log.d("DEBUG_FIRESTORE", "   └─ updatedAt tipo: ${updatedAt?.javaClass?.simpleName} valor: $updatedAt")
                Log.d("DEBUG_FIRESTORE", "   └─ coincide userId?: ${userId?.trim() == targetUserId.trim()}")

                // ✅ INTENTAR CONVERSIÓN PARA DETECTAR ERRORES
                try {
                    val testPlan = doc.toObject(Plan::class.java)
                    Log.d("DEBUG_FIRESTORE", "   └─ Conversión automática: ✅ EXITOSA")
                } catch (e: Exception) {
                    Log.e("DEBUG_FIRESTORE", "   └─ Conversión automática: ❌ ERROR - ${e.message}")

                    // Intentar conversión manual para este documento problemático
                    try {
                        val manualPlan = Plan(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            location = doc.getString("location") ?: "",
                            date = doc.getLong("date") ?: 0L,
                            timeString = doc.getString("timeString") ?: "",
                            userId = doc.getString("userId") ?: "",
                            createdAt = when (val created = doc.get("createdAt")) {
                                is Long -> created
                                is com.google.firebase.Timestamp -> created.seconds * 1000
                                else -> 0L
                            },
                            updatedAt = when (val updated = doc.get("updatedAt")) {
                                is Long -> updated
                                is com.google.firebase.Timestamp -> updated.seconds * 1000
                                else -> null
                            },
                            imageUrls = (doc.get("imageUrls") as? List<String>) ?: emptyList(),
                            enableWhatsapp = doc.getBoolean("enableWhatsapp") ?: false,
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            likes = (doc.get("likes") as? List<String>) ?: emptyList(),
                            participants = (doc.get("participants") as? List<String>) ?: emptyList(),
                            shares = doc.getLong("shares")?.toInt() ?: 0,
                            commentCount = doc.getLong("commentCount")?.toInt() ?: 0
                        )
                        Log.d("DEBUG_FIRESTORE", "   └─ Conversión manual: ✅ EXITOSA")
                    } catch (e2: Exception) {
                        Log.e("DEBUG_FIRESTORE", "   └─ Conversión manual: ❌ FALLÓ - ${e2.message}")
                    }
                }

                Log.d("DEBUG_FIRESTORE", "   └─ campos disponibles: ${doc.data?.keys}")

            } catch (e: Exception) {
                Log.e("DEBUG_FIRESTORE", "❌ Error procesando doc ${doc.id}: ${e.message}")
            }
        }

        // ✅ CONSULTA ESPECÍFICA DE VERIFICACIÓN
        val specificQuery = db.collection("planes")
            .whereEqualTo("userId", targetUserId.trim())
            .get()
            .await()

        Log.d("DEBUG_FIRESTORE", "🎯 Consulta específica encontró: ${specificQuery.size()} documentos")

        // ✅ VERIFICAR PERMISOS
        try {
            val testRead = db.collection("planes").limit(1).get().await()
            Log.d("DEBUG_FIRESTORE", "✅ Permisos de lectura: OK")
        } catch (e: Exception) {
            Log.e("DEBUG_FIRESTORE", "❌ Problema de permisos: ${e.message}")
        }

        Log.d("DEBUG_FIRESTORE", "🏁 === FIN DEBUG DE FIRESTORE ===")

    } catch (e: Exception) {
        Log.e("DEBUG_FIRESTORE", "💥 Error en debug: ${e.message}", e)
    }
}

// ✅ DATA CLASSES CORREGIDAS - USANDO LONG CONSISTENTEMENTE