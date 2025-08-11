package com.santiago.sindesparches.presentation.plan_detail

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.data.models.Plan
import com.santiago.sindesparches.data.models.UserProfile
import com.santiago.sindesparches.ui.theme.Purple
import com.santiago.sindesparches.ui.theme.azul
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.santiago.sindesparches.ui.theme.boton
import kotlin.math.PI
import kotlin.math.sin


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToUserProfile: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var plan by remember { mutableStateOf<Plan?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userProfileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition()
    val gradientAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Estados de animación
    val scrollState = rememberScrollState()
    val cardAnimation by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )

    val context = LocalContext.current
    var imagen_usuario by remember { mutableStateOf<String?>(null) }
    val storage = FirebaseStorage.getInstance().reference
    val storageRef = storage.child("profile_pictures/${plan?.userId}")

    storageRef.downloadUrl.addOnSuccessListener { uri ->
        imagen_usuario = uri.toString()
    }.addOnFailureListener { }

    // Colores del tema nocturno vibrante
    val nightBackground = Color(0xFF0A0E27)
    val deepPurple = Color(0xFF6366F1)
    val vibrantPink = Color(0xFFEC4899)
    val electricBlue = Color(0xFF06B6D4)
    val neonGreen = Color(0xFF10B981)
    val goldAccent = Color(0xFFF59E0B)
    val cardBackground = Color(0xFF1A1D3A)
    val surfaceVariant = Color(0xFF2D2F4F)

    // Cargar los detalles del plan al iniciar
    LaunchedEffect(planId) {
        coroutineScope.launch {
            try {
                isLoading = true
                plan = getPlanById(db, planId)
                plan?.userId?.let { userId ->
                    userProfile = getUserProfileById(db, userId)
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error al cargar el plan: ${e.message}"
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        nightBackground,
                        nightBackground.copy(alpha = 0.9f),
                        Color(0xFF1A1D3A)
                    )
                )
            )
    ) {
        // Fondo animado con partículas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            // Estrellas animadas
            for (i in 0..20) {
                val x = (width * (i * 0.123f + gradientAnimation * 0.1f)) % width
                val y = (height * (i * 0.456f + gradientAnimation * 0.05f)) % height
                val alpha = (sin(gradientAnimation * PI * 2 + i) * 0.5f + 0.5f).toFloat()

                drawCircle(
                    color = electricBlue.copy(alpha = alpha * 0.6f),
                    radius = 2f + sin(gradientAnimation * PI * 2 + i).toFloat() * 1f,
                    center = Offset(x, y)
                )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = plan?.title ?: "Detalles del Plan",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = vibrantPink.copy(alpha = 0.5f),
                                    blurRadius = 8f
                                )
                            ),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = navigateBack,
                            modifier = Modifier
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            deepPurple.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (plan?.userId == auth.currentUser?.uid) {
                            IconButton(
                                onClick = { navigateToEdit(planId) },
                                modifier = Modifier
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                neonGreen.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar Plan",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.Red.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar Plan",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.background(Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        deepPurple.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(pulseAnimation),
                                color = vibrantPink,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando evento...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    shadow = Shadow(
                                        color = electricBlue.copy(alpha = 0.5f),
                                        blurRadius = 8f
                                    )
                                ),
                                color = Color.White
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .scale(cardAnimation),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "Error desconocido",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            errorMessage = null
                                            isLoading = true
                                            plan = getPlanById(db, planId)
                                            plan?.userId?.let { userId ->
                                                userProfile = getUserProfileById(db, userId)
                                            }
                                            isLoading = false
                                        } catch (e: Exception) {
                                            errorMessage = "Error al cargar el plan: ${e.message}"
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = vibrantPink
                                ),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Text("Reintentar", color = Color.White)
                            }
                        }
                    }
                } else if (plan == null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .scale(cardAnimation),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "No se encontró el evento",
                            modifier = Modifier.padding(24.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .graphicsLayer {
                                alpha = cardAnimation
                                translationY = (1f - cardAnimation) * 100f
                            }
                    ) {
                        // Información del usuario creador con diseño mejorado
                        userProfile?.let { profile ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        plan?.userId?.let { userId ->
                                            if (userId.isNotBlank()) {
                                                navigateToUserProfile(userId)
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBackground
                                ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 8.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    deepPurple.copy(alpha = 0.3f),
                                                    vibrantPink.copy(alpha = 0.3f),
                                                    electricBlue.copy(alpha = 0.3f)
                                                )
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Imagen de perfil con efecto glow
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            vibrantPink.copy(alpha = 0.5f),
                                                            Color.Transparent
                                                        )
                                                    ),
                                                    shape = CircleShape
                                                )
                                                .padding(2.dp)
                                        ) {
                                            AsyncImage(
                                                model = imagen_usuario ?: R.drawable.bxs_user,
                                                contentDescription = "Foto de perfil",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .border(
                                                        2.dp,
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                vibrantPink,
                                                                electricBlue
                                                            )
                                                        ),
                                                        CircleShape
                                                    ),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Text(
                                                text = "Creado por",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    shadow = Shadow(
                                                        color = electricBlue.copy(alpha = 0.5f),
                                                        blurRadius = 4f
                                                    )
                                                ),
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = profile.nombre ?: "Usuario",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = Shadow(
                                                        color = vibrantPink.copy(alpha = 0.5f),
                                                        blurRadius = 8f
                                                    )
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        Icon(
                                            Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Ver perfil",
                                            tint = vibrantPink,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Imágenes del plan con efecto paralax
                        if (plan!!.imageUrls.isNotEmpty()) {
                            val parallaxOffset = scrollState.value * 0.5f

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(350.dp),
                                shape = RoundedCornerShape(25.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 12.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (plan!!.imageUrls.size == 1) {
                                        AsyncImage(
                                            model = plan!!.imageUrls.first(),
                                            contentDescription = "Imagen del plan",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    translationY = parallaxOffset
                                                }
                                        )
                                    } else {
                                        // Para múltiples imágenes, usar HorizontalPager para que cada imagen ocupe todo el contenedor
                                        val pagerState = rememberPagerState(pageCount = { plan!!.imageUrls.size })

                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            AsyncImage(
                                                model = plan!!.imageUrls[page],
                                                contentDescription = "Imagen del plan ${page + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        translationY = parallaxOffset
                                                    }
                                            )
                                        }

                                        // Indicadores de página (puntos)
                                        if (plan!!.imageUrls.size > 1) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(16.dp)
                                                    .background(
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                repeat(plan!!.imageUrls.size) { iteration ->
                                                    val color = if (pagerState.currentPage == iteration) {
                                                        vibrantPink
                                                    } else {
                                                        Color.White.copy(alpha = 0.5f)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .size(8.dp)
                                                            .background(color, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Overlay gradient
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.3f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Información detallada del plan
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = cardBackground
                            ),
                            shape = RoundedCornerShape(25.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                // Título con efecto glow
                                Text(
                                    text = plan!!.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = vibrantPink.copy(alpha = 0.7f),
                                            blurRadius = 12f
                                        )
                                    ),
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Fecha y hora con iconos animados
                                InfoRow(
                                    iconRes = R.drawable.bx_calendar,
                                    text = formatDate(plan!!.date),
                                    color = neonGreen
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                InfoRow(
                                    iconRes = R.drawable.bx_time,
                                    text = plan!!.timeString,
                                    color = goldAccent
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (plan!!.latitude != null && plan!!.longitude != null) {
                                    val location = LatLng(plan!!.latitude!!, plan!!.longitude!!)
                                    val cameraPositionState = rememberCameraPositionState {
                                        position = CameraPosition.fromLatLngZoom(location, 15f)
                                    }

                                    Text(
                                        text = "Ubicación",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(
                                                color = deepPurple.copy(alpha = 0.7f),
                                                blurRadius = 8f
                                            )
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                val gmmIntentUri = Uri.parse("google.navigation:q=${plan!!.latitude},${plan!!.longitude}")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(mapIntent)
                                            },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        GoogleMap(
                                            modifier = Modifier.fillMaxSize(),
                                            cameraPositionState = cameraPositionState,
                                            uiSettings = MapUiSettings(
                                                zoomControlsEnabled = false,
                                                scrollGesturesEnabled = false,
                                                zoomGesturesEnabled = false,
                                                tiltGesturesEnabled = false,
                                                rotationGesturesEnabled = false
                                            )
                                        ) {
                                            Marker(
                                                state = MarkerState(position = location),
                                                title = plan!!.title
                                            )
                                        }
                                    }
                                } else {
                                    InfoRow(
                                        icon = Icons.Filled.Place,
                                        text = plan!!.location,
                                        color = electricBlue
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Descripción con fondo gradiente
                                Text(
                                    text = "Descripción",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = deepPurple.copy(alpha = 0.7f),
                                            blurRadius = 8f
                                        )
                                    ),
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        surfaceVariant,
                                                        surfaceVariant.copy(alpha = 0.8f)
                                                    )
                                                )
                                            )
                                    ) {
                                        Text(
                                            text = plan!!.description,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                lineHeight = 24.sp
                                            ),
                                            modifier = Modifier.padding(20.dp),
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Fecha de creación
                                Text(
                                    text = "Publicado el ${formatFullDate(plan!!.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        shadow = Shadow(
                                            color = electricBlue.copy(alpha = 0.5f),
                                            blurRadius = 4f
                                        )
                                    ),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Botón de WhatsApp con animación
                        if (plan!!.enableWhatsapp == true && plan!!.phoneNumber != null) {
                            val message = "Hola, estoy interesado en el plan: ${plan!!.title}"
                            val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
                            val whatsappUrl = "https://api.whatsapp.com/send?phone=${plan!!.phoneNumber}&text=$encodedMessage"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(30.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("WhatsApp no instalado")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    neonGreen,
                                                    Color(0xFF25D366)
                                                )
                                            ),
                                            shape = RoundedCornerShape(30.dp)
                                        ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(30.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 8.dp,
                                        pressedElevation = 12.dp
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.bxl_whatsapp),
                                            contentDescription = "WhatsApp",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Contactar por WhatsApp",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar con diseño mejorado
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    "Eliminar evento",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Red.copy(alpha = 0.5f),
                            blurRadius = 8f
                        )
                    ),
                    color = Color.White
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que quieres eliminar este evento? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                deletePlan(db, planId)
                                snackbarHostState.showSnackbar("Evento eliminado correctamente")
                                navigateBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al eliminar el evento: ${e.message}")
                            }
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = cardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun InfoRow(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                icon != null -> {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                iconRes != null -> {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                shadow = Shadow(
                    color = color.copy(alpha = 0.3f),
                    blurRadius = 4f
                )
            ),
            color = Color.White
        )
    }
}
// Función para obtener un plan por su ID
private suspend fun getPlanById(db: FirebaseFirestore, planId: String): Plan = withContext(Dispatchers.IO) {
    try {
        Log.d("getPlanById", "Buscando plan con ID: $planId")

        val document = db.collection("planes").document(planId).get().await()

        if (document.exists()) {
            val plan = document.toObject(Plan::class.java)
                ?: throw Exception("No se pudo convertir el documento a Plan")

            // ✅ IMPORTANTE: Asignar el ID del documento al plan
            val planWithId = plan.copy(id = document.id)

            Log.d("getPlanById", "Plan encontrado: ${planWithId.title} con ID: ${planWithId.id}")
            return@withContext planWithId
        } else {
            throw Exception("El plan no existe")
        }
    } catch (e: Exception) {
        Log.e("getPlanById", "Error obteniendo plan: ${e.message}", e)
        throw e
    }
}

// Función para obtener el perfil del usuario por su ID
private suspend fun getUserProfileById(db: FirebaseFirestore, userId: String): UserProfile? = withContext(Dispatchers.IO) {
    try {
        val document = db.collection("perfil").document(userId).get().await()
        return@withContext document.toObject(UserProfile::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

// Función para obtener la URL de la imagen de perfil del usuario
private suspend fun getUserProfileImageUrl(storage: FirebaseStorage, userId: String): String? = withContext(Dispatchers.IO) {
    try {
        val imageRef = storage.reference.child("profile_pictures/$userId.jpg")
        return@withContext imageRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        // Si no encuentra la imagen, intentar con diferentes extensiones
        try {
            val imageRef = storage.reference.child("profile_pictures/$userId.png")
            return@withContext imageRef.downloadUrl.await().toString()
        } catch (e2: Exception) {
            try {
                val imageRef = storage.reference.child("profile_pictures/$userId.jpeg")
                return@withContext imageRef.downloadUrl.await().toString()
            } catch (e3: Exception) {
                e3.printStackTrace()
                return@withContext null
            }
        }
    }
}

// Función para eliminar un plan
private suspend fun deletePlan(db: FirebaseFirestore, planId: String) = withContext(Dispatchers.IO) {
    try {
        db.collection("planes").document(planId).delete().await()
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

// Función para formatear fecha de timestamp a formato legible
private fun formatDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

// Función para formatear fecha completa con hora
private fun formatFullDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

// Data class para el perfil del usuario (ajusta según tu estructura)