package com.santiago.sindesparches.presentation.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.zIndex
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.presentation.flash_plan.obtenerNombreUsuario
import com.santiago.sindesparches.presentation.plan_detail.UserProfile
import com.santiago.sindesparches.presentation.notifications.sendNotification
import com.santiago.sindesparches.presentation.publicaciones.Plan
import com.santiago.sindesparches.ui.theme.Purple
import com.santiago.sindesparches.ui.theme.azul
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.gris
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun homeScreen(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    navigateToInicial: () -> Unit = {},
    navigateToFlashPlan: () -> Unit = {},
    navigateToPublicaciones: () -> Unit = {},
    navigateToPlanDetail: (String) -> Unit,
    navigateToUserProfile: (String) -> Unit ={} ,
    navigateToMiPerfil: () -> Unit = {},
    navigateToEditPlan: (String) -> Unit = {},
    navigateToMegusta: () -> Unit = {},
    navigateToParticipar: () -> Unit = {},
    navigateToNotifications: () -> Unit = {},
    navigateToComments: (String) -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showRightMenu by remember { mutableStateOf(false) }
    var showLeftMenu by remember { mutableStateOf(false) }
    var showStory by remember { mutableStateOf(false) }
    var currentStory by remember { mutableStateOf<Story?>(null) }
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }


    //colors
    val vibrantPink = Color(0xFFEC4899)
    val electricBlue = Color(0xFF06B6D4)

    // Estado del botón flotante mejorado con verificación de nulos
    var isButtonVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                try {
                    val deltaY = available.y
                    when {
                        deltaY < -10 -> isButtonVisible = false // scroll hacia arriba
                        deltaY > 10 -> isButtonVisible = true // scroll hacia abajo
                    }
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error en onPreScroll", e)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                try {
                    val deltaY = available.y
                    when {
                        deltaY < -5 -> isButtonVisible = false
                        deltaY > 5 -> isButtonVisible = true
                    }
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error en onPostScroll", e)
                }
                return Offset.Zero
            }
        }
    }

    //variables para las publicaciones
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var planes by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var allPlanes by remember { mutableStateOf<List<Plan>>(emptyList()) } // ✅ Lista completa sin filtrar
    var isLoadingpublicacion by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    //variables cerrar secion

    // Estado para controlar la visibilidad del diálogo
    var showLogoutDialog by remember { mutableStateOf(false) }

// Colores del tema oscuro para eventos
    val nightBackground = Color(0xFF0A0E27)
    val deepPurple = Color(0xFF6366F1)
    val neonGreen = Color(0xFF10B981)
    val cardBackground = Color(0xFF1A1D3A)
    val surfaceVariant = Color(0xFF2D2F4F)
    // Animación para el efecto glow
    val infiniteTransition = rememberInfiniteTransition()
    val glowAnimation by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )


    //funcion cerrar sesion confirmacion
    // Diálogo personalizado de confirmación
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = cardBackground,
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            vibrantPink.copy(alpha = 0.5f),
                            electricBlue.copy(alpha = 0.5f),
                            deepPurple.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(25.dp)
                ),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icono con efecto glow
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        vibrantPink.copy(alpha = glowAnimation * 0.4f),
                                        Color.Transparent
                                    ),
                                    radius = 60f
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = vibrantPink,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    shadowElevation = 8.dp.toPx()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "¿Cerrar Sesión?",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = electricBlue.copy(alpha = 0.5f),
                                blurRadius = 8f
                            )
                        ),
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    // Línea decorativa
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        electricBlue.copy(alpha = 0.6f),
                                        vibrantPink.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Estás a punto de salir de tu cuenta. ¿Estás seguro de que quieres continuar?",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Podrás volver a iniciar sesión cuando quieras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                // Botón de confirmar con efecto glassmorphism
                Button(
                    onClick = {
                        showLogoutDialog = false
                        auth.signOut()
                        navigateToInicial()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    vibrantPink.copy(alpha = 0.8f),
                                    Color(0xFFDC2626).copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
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
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sí, Salir",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            },
            dismissButton = {
                // Botón de cancelar con efecto neón
                TextButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier
                        .height(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    neonGreen.copy(alpha = 0.6f),
                                    electricBlue.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = neonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cancelar",
                            color = neonGreen,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = neonGreen.copy(alpha = 0.5f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
            }
        )
    }

    // ✅ Función para filtrar planes por búsqueda
    fun filterPlanes(searchQuery: String): List<Plan> {
        return if (searchQuery.isBlank()) {
            allPlanes
        } else {
            allPlanes.filter { plan ->
                plan.title.contains(searchQuery, ignoreCase = true) ||
                        plan.description.contains(searchQuery, ignoreCase = true) ||
                        plan.location.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // ✅ Actualizar planes filtrados cuando cambie el texto de búsqueda
    LaunchedEffect(searchText) {
        planes = filterPlanes(searchText)
    }

    // Animaciones para los menús laterales
    val leftMenuOffset by animateDpAsState(
        targetValue = if (showLeftMenu) 0.dp else (-150).dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "leftMenuAnimation"
    )

    val rightMenuOffset by animateDpAsState(
        targetValue = if (showRightMenu) 0.dp else 250.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "rightMenuAnimation"
    )

    val currentUserId = auth.currentUser?.uid.orEmpty()
    val context = LocalContext.current

    var userProfileImages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var imagenUrl by remember { mutableStateOf<String?>(null) }
    fun obtenerFotoPerfilUrl(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_pictures/$uid")

        storageRef.downloadUrl
            .addOnSuccessListener { uri -> onResult(uri.toString()) }
            .addOnFailureListener { onResult(null) }
    }
    var nombreuser by remember { mutableStateOf<String?>(null) }
    val nombreCorto = nombreuser?.split(" ")?.firstOrNull() ?: ""

    fun NombreUsuario(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        FirebaseFirestore.getInstance().collection("perfil").document(uid)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.getString("nombre"))
            }
            .addOnFailureListener { onResult(null) }
    }

    LaunchedEffect(Unit) {
        NombreUsuario { nombreuser = it }
        obtenerFotoPerfilUrl { imagenUrl = it }
    }

    //nombre de quien creo el flashplan
    var nombre_flashplan by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        obtenerNombreUsuario { resultado ->
            nombre_flashplan = resultado
        }
    }

    // Reemplaza tu LaunchedEffect actual con esta versión mejorada:

    LaunchedEffect(Unit) {
        try {
            val twentyFourHoursAgo = System.currentTimeMillis() - 86400000

            db.collection("flashPlans")
                .whereGreaterThan("timestamp", Timestamp(twentyFourHoursAgo / 1000, 0))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("HomeScreen", "Error al cargar FlashPlans", e)
                        Toast.makeText(context, "Error al cargar historias", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val flashPlanList = mutableListOf<Story>()
                    val userIds = mutableSetOf<String>()

                    snapshot?.documents?.forEach { doc ->
                        val userId = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: nombre_flashplan.orEmpty()
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val timestamp = (doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis())

                        // ✅ Obtener viewers correctamente de Firestore
                        val viewers = doc.get("viewers") as? List<String> ?: emptyList()

                        if (userId.isNotEmpty()) {
                            userIds.add(userId)
                        }

                        flashPlanList.add(
                            Story(
                                id = doc.id,
                                imageUrl = imageUrl,
                                userId = userId,
                                username = userName,
                                timestamp = timestamp,
                                viewers = viewers // ✅ Esto mantendrá el estado actualizado
                            )
                        )
                    }

                    // ✅ Actualizar stories manteniendo el estado de viewers
                    stories = flashPlanList

                    // Función para eliminar FlashPlan (mantener como estaba)
                    fun deleteFlashPlan(flashPlanId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
                        db.collection("flashPlans").document(flashPlanId)
                            .delete()
                            .addOnSuccessListener {
                                stories = stories.filter { it.id != flashPlanId }
                                onSuccess()
                                Log.d("HomeScreen", "FlashPlan eliminado exitosamente")
                            }
                            .addOnFailureListener { e ->
                                Log.e("HomeScreen", "Error al eliminar FlashPlan", e)
                                onFailure(e)
                            }
                    }

                    // Cargar imágenes de perfil
                    if (userIds.isNotEmpty()) {
                        val profileImagesMap = mutableMapOf<String, String>()
                        userIds.forEach { userId ->
                            val storageRef = FirebaseStorage.getInstance().reference
                                .child("profile_pictures/$userId")

                            storageRef.downloadUrl
                                .addOnSuccessListener { uri ->
                                    profileImagesMap[userId] = uri.toString()
                                    userProfileImages = profileImagesMap.toMap()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HomeScreen", "Error al cargar imagen de perfil para usuario $userId", e)
                                    // Respaldo con Firestore
                                    db.collection("perfil").document(userId)
                                        .get()
                                        .addOnSuccessListener { userDoc ->
                                            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                                            if (profileImageUrl.isNotEmpty()) {
                                                profileImagesMap[userId] = profileImageUrl
                                                userProfileImages = profileImagesMap.toMap()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Log.e("HomeScreen", "Error al cargar desde Firestore para usuario $userId", it)
                                        }
                                }
                        }
                    }

                    isLoading = false
                }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error al configurar listener de FlashPlans", e)
            isLoading = false
        }
    }

    // ✅ Cargar publicaciones desde Firestore
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoadingpublicacion = true
                val loadedPlanes = getPlanes(db)
                allPlanes = loadedPlanes // ✅ Guardar lista completa
                planes = loadedPlanes     // ✅ Mostrar lista completa inicialmente
                isLoadingpublicacion = false
            } catch (e: Exception) {
                errorMessage = "Error al cargar los planes: ${e.message}"
                isLoadingpublicacion = false
                Log.e("HomeScreen", "Error al cargar planes", e)
            }
        }
    }

    BackHandler {
        when {
            showStory -> showStory = false
            showRightMenu -> {
                showRightMenu = false
                return@BackHandler
            }

            showLeftMenu -> {
                showLeftMenu = false
                return@BackHandler
            }

            else -> showDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(black)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .then(
                        if (!showLeftMenu && !showRightMenu) {
                            Modifier.pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    when {
                                        dragAmount < -10 -> {
                                            showRightMenu = true
                                            showLeftMenu = false
                                        }
                                        dragAmount > 10 -> {
                                            showLeftMenu = true
                                            showRightMenu = false
                                        }
                                    }
                                    change.consume()
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(black)
                        // ✅ Aplicar alpha cuando hay menús activos
                        .alpha(if (showLeftMenu || showRightMenu) 0.3f else 1f)
                ) {
                    // Header con título, búsqueda, menú y botón flotante
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SINDE",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // ✅ TextField de búsqueda mejorado
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = {
                                Text(
                                    "Buscar..",
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Gray.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Gray.copy(alpha = 0.2f),
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(25.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchText = "" }
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón flotante reposicionado en la parte superior derecha
                            AnimatedVisibility(
                                visible = isButtonVisible,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                FloatingActionButton(
                                    onClick = navigateToPublicaciones,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    vibrantPink.copy(alpha = 0.9f),
                                                    electricBlue.copy(alpha = 0.9f)
                                                ),
                                                start = Offset(0f, 0f),
                                                end = Offset(100f, 100f)
                                            ),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.5f),
                                                    Color.White.copy(alpha = 0.1f)
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 12.dp,
                                        pressedElevation = 16.dp
                                    )
                                ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Crear publicación",
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.White
                                        )

                                }
                            }

                            IconButton(onClick = {
                                showRightMenu = !showRightMenu
                                if (showRightMenu) showLeftMenu = false
                            }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menú",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // ✅ Mostrar contador de resultados de búsqueda
                    if (searchText.isNotEmpty()) {
                        Text(
                            text = "Encontrados: ${planes.size} plan${if (planes.size != 1) "es" else ""}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    fun removePlanFromList(planId: String) {
                        planes = planes.filter { it.id != planId }
                        allPlanes = allPlanes.filter { it.id != planId }
                    }

                    // Contenido principal de publicaciones
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            isLoadingpublicacion -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.White
                                )
                            }

                            errorMessage != null -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = errorMessage ?: "Error desconocido",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    errorMessage = null
                                                    isLoadingpublicacion = true
                                                    val loadedPlanes = getPlanes(db)
                                                    allPlanes = loadedPlanes
                                                    planes = filterPlanes(searchText) // ✅ Aplicar filtro actual
                                                    isLoadingpublicacion = false
                                                } catch (e: Exception) {
                                                    errorMessage = "Error al cargar los planes: ${e.message}"
                                                    isLoadingpublicacion = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Reintentar")
                                    }
                                }
                            }

                            planes.isEmpty() -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // ✅ Diferentes mensajes según si hay búsqueda activa o no
                                    if (searchText.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No se encontraron planes para \"$searchText\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Intenta con otros términos de búsqueda",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { searchText = "" }
                                        ) {
                                            Text("Limpiar búsqueda")
                                        }
                                    } else {
                                        Text(
                                            text = "No hay planes disponibles",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = navigateToPublicaciones) {
                                            Text("Crear Plan")
                                        }
                                    }
                                }
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(nestedScrollConnection),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(planes) { plan ->
                                        PlanCard(
                                            plan = plan,
                                            onPlanClick = { navigateToPlanDetail(plan.id) },
                                            currentUserId = auth.currentUser?.uid ?: "",
                                            db = db,
                                            coroutineScope = coroutineScope,
                                            context = context,
                                            searchText = searchText, // ✅ Pasar texto de búsqueda para destacar coincidencias
                                            navigateToUserProfile = navigateToUserProfile,
                                            navigateToEditPlan = navigateToEditPlan,
                                            navigateToMiPerfil = navigateToMiPerfil,
                                            navigateToComments = navigateToComments,
                                            onPlanDeleted = { planId -> removePlanFromList(planId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // OVERLAY TRANSPARENTE para bloquear interacciones cuando hay menús activos
        if (showLeftMenu || showRightMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Cerrar menús al tocar el overlay
                            showLeftMenu = false
                            showRightMenu = false
                        }
                    }
                    .zIndex(0.5f) // Por encima del contenido, pero debajo de los menús
            )
        }

       // OVERLAY TRANSPARENTE para bloquear interacciones cuando hay menús activos
        if (showLeftMenu || showRightMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Cerrar menús al tocar el overlay
                            showLeftMenu = false
                            showRightMenu = false
                        }
                    }
                    .zIndex(0.5f) // Por encima del contenido, pero debajo de los menús
            )
        }

        // Menú izquierdo - CON ANIMACIÓN
        Box(
            modifier = Modifier
                .offset(x = leftMenuOffset)
                .width(150.dp)
                .fillMaxHeight()
                .background(boton)
                .padding(top = 60.dp)
                .padding(start = 16.dp)
                .zIndex(2f) // ✅ Más alto que el overlay
        ) {
            Column {
                Spacer(Modifier.height(50.dp))

                Button(
                    modifier = Modifier
                        .height(75.dp)
                        .width(75.dp)
                        .clip(CircleShape),
                    onClick = navigateToFlashPlan
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (stories.isEmpty()) {
                    Text(
                        "No hay FlashPlans disponibles",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "FlashPlans",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Reemplaza la sección de LazyColumn de los FlashPlans con este código mejorado:

                    LazyColumn {
                        items(stories.size) { index ->
                            val story = stories[index]
                            val seen = story.viewers.contains(currentUserId)

                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 3.dp,
                                        color = if (seen) Color.Gray else Color.Magenta,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        showStory = true
                                        currentStory = story

                                        // ✅ Marcar como visto SOLO si no ha sido visto antes
                                        if (!seen) {
                                            // Primero actualizar el estado local inmediatamente
                                            stories = stories.map { s ->
                                                if (s.id == story.id) {
                                                    s.copy(viewers = s.viewers + currentUserId)
                                                } else {
                                                    s
                                                }
                                            }

                                            // Luego actualizar en Firestore
                                            db.collection("flashPlans").document(story.id)
                                                .update("viewers", FieldValue.arrayUnion(currentUserId))
                                                .addOnFailureListener { e ->
                                                    Log.e("HomeScreen", "Error al marcar FlashPlan como visto", e)
                                                    // Revertir el cambio local si falla la actualización
                                                    stories = stories.map { s ->
                                                        if (s.id == story.id) {
                                                            s.copy(viewers = s.viewers - currentUserId)
                                                        } else {
                                                            s
                                                        }
                                                    }
                                                }
                                        }
                                    }
                            ) {
                                // Contenido del FlashPlan (imagen de perfil)
                                val profileImageUrl = userProfileImages[story.userId]

                                if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = profileImageUrl,
                                        contentDescription = "Perfil de ${story.username}",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Imagen por defecto si no hay imagen de perfil
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Gray.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = "Perfil por defecto",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Nombre del usuario debajo del FlashPlan
                            Text(
                                text = story.username.take(8),
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp, start = 20.dp),

                            )
                        }
                    }
                }
            }
        }

        //  Menú derecho
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = rightMenuOffset)
                .width(220.dp)
                .fillMaxHeight()
                .background(
                    Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .padding(16.dp)
                .zIndex(4f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 25.dp)
            ) {
                // Perfil del usuario
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigateToMiPerfil() }
                        .padding(vertical = 12.dp)
                ) {
                    if (imagenUrl != null) {
                        AsyncImage(
                            model = imagenUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Usuario",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = nombreCorto ?: "Cargando...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de navegación
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuItem(
                        text = "Me Gusta",
                        icon = Icons.Default.Favorite,
                        onClick = navigateToMegusta
                    )
                    MenuItem(
                        text = "Participar",
                        icon = Icons.Default.Check,
                        onClick = navigateToParticipar
                    )
                    MenuItem(
                        text = "Notificaciones",
                        icon = Icons.Default.Notifications,
                        onClick = navigateToNotifications
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón de cerrar sesión
                MenuItem(
                    text = "Cerrar Sesión",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = {
                        showLogoutDialog = true
                    }
                )
            }
        }

        // Primero, agrega la función de eliminar dentro de tu homeScreen composable:

        fun deleteFlashPlan(flashPlanId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
            db.collection("flashPlans").document(flashPlanId)
                .delete()
                .addOnSuccessListener {
                    // Actualizar la lista local inmediatamente
                    stories = stories.filter { it.id != flashPlanId }
                    onSuccess()
                    Log.d("HomeScreen", "FlashPlan eliminado exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e("HomeScreen", "Error al eliminar FlashPlan", e)
                    onFailure(e)
                }
        }
        // Historia en pantalla completa por 3s
        if (showStory && currentStory != null) {
            val progress = remember { Animatable(0f) }

            LaunchedEffect(currentStory) {
                progress.snapTo(0f)

                // Marcar como visto al abrir la historia (si no estaba visto)
                val seen = currentStory!!.viewers.contains(currentUserId)
                if (!seen) {
                    db.collection("flashPlans").document(currentStory!!.id)
                        .update("viewers", FieldValue.arrayUnion(currentUserId))
                        .addOnSuccessListener {
                            val updatedStories = stories.map { s ->
                                if (s.id == currentStory!!.id) {
                                    s.copy(viewers = s.viewers + currentUserId)
                                } else {
                                    s
                                }
                            }
                            stories = updatedStories
                            Log.d("HomeScreen", "FlashPlan marcado como visto desde el visor")
                        }
                }

                progress.animateTo(1f, animationSpec = tween(durationMillis = 30000, easing = LinearEasing))
                showStory = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(10f)
                    .clickable { showStory = false }
                    .padding(top = 24.dp)
            ) {
                AsyncImage(
                    model = currentStory!!.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Barra de progreso y detalles del usuario
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color.White,
                        trackColor = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigateToUserProfile(currentStory!!.userId) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val userImageUrl = userProfileImages[currentStory!!.userId]
                        if (userImageUrl != null && userImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = userImageUrl,
                                contentDescription = "Imagen de perfil",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Usuario",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = currentStory!!.username,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Botones en la parte superior derecha
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón de eliminar - Solo visible para el creador del FlashPlan
                    if (currentStory!!.userId == currentUserId) {
                        IconButton(
                            onClick = {
                                deleteFlashPlan(currentStory!!.id) {
                                    showStory = false
                                    currentStory = null
                                    // Opcional: mostrar mensaje de confirmación
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("FlashPlan eliminado")
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(
                                    Color.Red.copy(alpha = 0.7f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar FlashPlan",
                                tint = Color.White
                            )
                        }
                    }

                    // Botón para cerrar
                    IconButton(
                        onClick = { showStory = false },
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }
            }
        }


        // Diálogo salir
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("¿Cerrar sesión?") },
                text = { Text("¿Estás seguro de que quieres salir?") },
                confirmButton = {
                    Button(onClick = {
                        auth.signOut()
                        navigateToInicial()
                    }) {
                        Text("Sí, salir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

    }

}



@Composable
fun PlanCard(
    plan: Plan,
    onPlanClick: () -> Unit,
    currentUserId: String,
    db: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    context: Context,
    searchText: String = "",
    navigateToUserProfile: (String) -> Unit,
    navigateToEditPlan: (String) -> Unit,
    navigateToMiPerfil: () -> Unit,
    navigateToComments: (String) -> Unit,
    onPlanDeleted: (String) -> Unit = {}
) {
    val isOwner = plan.userId == currentUserId

    // Estados para eliminar plan
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Estados para las interacciones
    var likes by remember { mutableIntStateOf(plan.likes?.size ?: 0) }
    var isLiked by remember { mutableStateOf(plan.likes?.contains(currentUserId) == true) }
    var participants by remember { mutableIntStateOf(plan.participants?.size ?: 0) }
    var isParticipating by remember { mutableStateOf(plan.participants?.contains(currentUserId) == true) }
    var shares by remember { mutableIntStateOf(plan.shares ?: 0) }
    var commentCount by remember { mutableIntStateOf(plan.commentCount) }

    LaunchedEffect(plan.id) {
        val planRef = db.collection("planes").document(plan.id)
        val listener = planRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("PlanCard", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val updatedPlan = snapshot.toObject(Plan::class.java)
                updatedPlan?.let {
                    likes = it.likes?.size ?: 0
                    isLiked = it.likes?.contains(currentUserId) == true
                    participants = it.participants?.size ?: 0
                    isParticipating = it.participants?.contains(currentUserId) == true
                    shares = it.shares ?: 0
                    commentCount = it.commentCount
                }
            } else {
                Log.d("PlanCard", "Current data: null")
            }
        }
    }

    // Estados para mostrar diálogos de información
    var showLikesDialog by remember { mutableStateOf(false) }
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Estados para usuarios con imágenes de perfil
    var likesUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var participantsUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingLikes by remember { mutableStateOf(false) }
    var isLoadingParticipants by remember { mutableStateOf(false) }

    // Estados para el perfil del usuario que publicó
    var imagenUsuario by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }

    // Función para eliminar el plan
    suspend fun deletePlan() {
        try {
            isDeleting = true
            Log.d("PlanCard", "Iniciando eliminación del plan: ${plan.id}")

            // 1. Eliminar las imágenes del plan de Firebase Storage
            if (plan.imageUrls.isNotEmpty()) {
                Log.d("PlanCard", "Eliminando ${plan.imageUrls.size} imágenes...")
                val storage = FirebaseStorage.getInstance()
                for (imageUrl in plan.imageUrls) {
                    try {
                        val imageRef = storage.getReferenceFromUrl(imageUrl)
                        imageRef.delete().await()
                        Log.d("PlanCard", "Imagen eliminada: $imageUrl")
                    } catch (e: Exception) {
                        Log.w("PlanCard", "Error al eliminar imagen: $imageUrl", e)
                    }
                }
            }

            // 2. Eliminar comentarios
            Log.d("PlanCard", "Eliminando comentarios del plan...")
            val commentsQuery = db.collection("comentarios")
                .whereEqualTo("planId", plan.id)
                .get()
                .await()

            if (commentsQuery.documents.isNotEmpty()) {
                Log.d("PlanCard", "Encontrados ${commentsQuery.documents.size} comentarios")
                val batch = db.batch()
                for (comment in commentsQuery.documents) {
                    batch.delete(comment.reference)
                    Log.d("PlanCard", "Programando eliminación de comentario: ${comment.id}")
                }
                batch.commit().await()
                Log.d("PlanCard", "Comentarios eliminados exitosamente")
            }

            // 3. Eliminar el documento del plan
            Log.d("PlanCard", "Eliminando documento del plan...")
            db.collection("planes").document(plan.id).delete().await()
            Log.d("PlanCard", "Plan eliminado exitosamente: ${plan.id}")

            // ✅ 4. NUEVO: Actualizar la lista local inmediatamente
            onPlanDeleted(plan.id)

            // 5. Mostrar mensaje de éxito
            Toast.makeText(
                context,
                "Plan eliminado exitosamente",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e("PlanCard", "Error al eliminar el plan", e)

            val errorMessage = when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    "Error: No tienes permisos para eliminar este plan"
                }
                e.message?.contains("not-found") == true -> {
                    "Error: El plan ya no existe"
                }
                e.message?.contains("unavailable") == true -> {
                    "Error: Servicio temporalmente no disponible. Intenta de nuevo"
                }
                else -> {
                    "Error inesperado al eliminar el plan"
                }
            }

            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        } finally {
            isDeleting = false
        }
    }

    // Función para cargar usuarios con imágenes de perfil
    suspend fun getUserProfiles(userIds: List<String>): List<UserProfile> {
        return try {
            val userProfiles = mutableListOf<UserProfile>()
            val storage = FirebaseStorage.getInstance().reference

            for (userId in userIds) {
                try {
                    val userDoc = db.collection("perfil").document(userId).get().await()
                    val name = userDoc.getString("nombre") ?: "Usuario"

                    val storageRef = storage.child("profile_pictures/$userId")
                    var profileImageUrl: String? = null

                    try {
                        profileImageUrl = storageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        profileImageUrl = userDoc.getString("profileImageUrl")
                    }

                    userProfiles.add(
                        UserProfile(
                            userId = userId,
                            nombre = name,
                            profileImageUrl = profileImageUrl
                        )
                    )
                } catch (e: Exception) {
                    userProfiles.add(
                        UserProfile(
                            userId = userId,
                            nombre = "Usuario",
                            profileImageUrl = null
                        )
                    )
                }
            }
            userProfiles
        } catch (e: Exception) {
            Log.e("PlanCard", "Error al cargar perfiles de usuarios", e)
            emptyList()
        }
    }

    // Cargar información del usuario que publicó
    LaunchedEffect(plan.userId) {
        try {
            isLoadingUser = true
            val userDoc = db.collection("perfil").document(plan.userId).get().await()
            userName = userDoc.getString("nombre") ?: "Usuario"

            val storage = FirebaseStorage.getInstance().reference
            val storageRef = storage.child("profile_pictures/${plan.userId}")

            try {
                val uri = storageRef.downloadUrl.await()
                imagenUsuario = uri.toString()
            } catch (e: Exception) {
                imagenUsuario = userDoc.getString("profileImageUrl")
            }

            isLoadingUser = false
        } catch (e: Exception) {
            Log.e("PlanCard", "Error al cargar perfil del usuario", e)
            userName = "Usuario"
            imagenUsuario = null
            isLoadingUser = false
        }
    }

    // Función para destacar texto de búsqueda
    @Composable
    fun HighlightedText(
        text: String,
        searchText: String,
        style: androidx.compose.ui.text.TextStyle,
        color: Color = Color.White,
        highlightColor: Color = Color.Yellow
    ) {
        if (searchText.isBlank()) {
            Text(text = text, style = style, color = color)
        } else {
            val annotatedString = buildAnnotatedString {
                val startIndex = text.indexOf(searchText, ignoreCase = true)
                if (startIndex >= 0) {
                    append(text.substring(0, startIndex))
                    withStyle(
                        style = SpanStyle(
                            background = highlightColor,
                            color = Color.Black
                        )
                    ) {
                        append(text.substring(startIndex, startIndex + searchText.length))
                    }
                    append(text.substring(startIndex + searchText.length))
                } else {
                    append(text)
                }
            }
            Text(text = annotatedString, style = style, color = color)
        }
    }

    // Componente para mostrar usuario con imagen de perfil
    @Composable
    fun UserListItem(
        userProfile: UserProfile,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = userProfile.profileImageUrl,
                contentDescription = "Foto de perfil ${userProfile.nombre}",
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

            userProfile.nombre?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver perfil",
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlanClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color.Gray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // Header con información del usuario que publicó
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen de perfil del usuario con estado de carga
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isLoadingUser) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        AsyncImage(
                            model = imagenUsuario,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isOwner) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (!isOwner) navigateToUserProfile(plan.userId)
                                },
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.ic_menu_myplaces),
                            placeholder = painterResource(id = android.R.drawable.ic_menu_myplaces)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre del usuario y etiqueta "Mi Plan" si es el dueño
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userName ?: "Cargando...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isOwner) {
                        Text(
                            text = "Mi Plan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Timestamp del plan
                Text(
                    text = formatTimeAgo(plan.createdAt ?: System.currentTimeMillis()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                // Botón de opciones (solo para el propietario)
                if (isOwner) {
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = {
                                    showOptionsMenu = false
                                    navigateToEditPlan(plan.id)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    if (isDeleting) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Eliminando...", color = Color.Red)
                                        }
                                    } else {
                                        Text("Eliminar", color = Color.Red)
                                    }
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    if (!isDeleting) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                },
                                enabled = !isDeleting
                            )
                        }
                    }
                }
            }

            // Imágenes del plan con indicadores
            if (plan.imageUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (plan.imageUrls.size == 1) {
                        AsyncImage(
                            model = plan.imageUrls.first(),
                            contentDescription = "Imagen del plan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { plan.imageUrls.size })

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = plan.imageUrls[page],
                                contentDescription = "Imagen del plan ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Indicadores de página
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(plan.imageUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) Color.White
                                            else Color.White.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Información del plan
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Título con destacado de búsqueda
                HighlightedText(
                    text = plan.title,
                    searchText = searchText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha y hora
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(plan.date),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.bx_time),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plan.timeString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ubicación con destacado de búsqueda
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    HighlightedText(
                        text = plan.location,
                        searchText = searchText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción con destacado de búsqueda
                HighlightedText(
                    text = plan.description,
                    searchText = searchText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de interacción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón Me Gusta
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    handleLikeAction(
                                        planId = plan.id,
                                        currentUserId = currentUserId,
                                        isLiked = isLiked,
                                        planOwnerId = plan.userId,
                                        db = db,
                                        context = context,
                                        onSuccess = { newIsLiked, newLikesCount ->
                                            isLiked = newIsLiked
                                            likes = newLikesCount
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Me gusta",
                                tint = if (isLiked) Color.Red else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(likes),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                if (likes > 0) {
                                    coroutineScope.launch {
                                        isLoadingLikes = true
                                        likesUsers = getUserProfiles(plan.likes ?: emptyList())
                                        isLoadingLikes = false
                                        showLikesDialog = true
                                    }
                                }
                            }
                        )
                    }

                    // Botón Participar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    handleParticipateAction(
                                        planId = plan.id,
                                        currentUserId = currentUserId,
                                        isParticipating = isParticipating,
                                        planOwnerId = plan.userId,
                                        db = db,
                                        context = context,
                                        onSuccess = { newIsParticipating, newParticipantsCount ->
                                            isParticipating = newIsParticipating
                                            participants = newParticipantsCount
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                if (isParticipating) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Participar",
                                tint = if (isParticipating) Color.Green else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(participants),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isParticipating) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                if (participants > 0) {
                                    coroutineScope.launch {
                                        isLoadingParticipants = true
                                        participantsUsers = getUserProfiles(plan.participants ?: emptyList())
                                        isLoadingParticipants = false
                                        showParticipantsDialog = true
                                    }
                                }
                            }
                        )
                    }

                    // Botón Compartir
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    handleShareAction(
                                        planId = plan.id,
                                        currentUserId = currentUserId,
                                        planOwnerId = plan.userId,
                                        planTitle = plan.title,
                                        db = db,
                                        context = context,
                                        onSuccess = { newSharesCount ->
                                            shares = newSharesCount
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(shares),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Botón Comentarios
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { navigateToComments(plan.id) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_comment),
                                contentDescription = "Comentarios",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = formatCount(commentCount),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar (FUERA del Card, al mismo nivel)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) showDeleteDialog = false
            },
            containerColor = Color.Black,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Eliminar Plan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    // Preview del plan que se va a eliminar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Mostrar imagen miniatura si existe
                            if (plan.imageUrls.isNotEmpty()) {
                                AsyncImage(
                                    model = plan.imageUrls.first(),
                                    contentDescription = "Vista previa",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                text = plan.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDate(plan.date),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = plan.location,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "¿Estás seguro de que quieres eliminar este plan?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Esta acción eliminará permanentemente:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = Color.Red)
                            Text("El plan y toda su información", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = Color.Red)
                            Text("Todas las imágenes asociadas (${plan.imageUrls.size})", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = Color.Red)
                            Text("Todos los comentarios ($commentCount)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = Color.Red)
                            Text("$likes me gusta y $participants participantes", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )

                    if (isDeleting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Eliminando plan...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Por favor espera",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            deletePlan()
                            showDeleteDialog = false // Cerrar diálogo después de eliminar
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = Color.Red.copy(alpha = 0.3f)
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogos de Me gusta y Participantes (mantener igual)
    if (showLikesDialog) {
        AlertDialog(
            onDismissRequest = { showLikesDialog = false },
            containerColor = Color.Black,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Me gusta ($likes)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                if (isLoadingLikes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cargando perfiles...", color = Color.White)
                        }
                    }
                } else if (likesUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay me gusta aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(likesUsers) { userProfile ->
                            UserListItem(
                                userProfile = userProfile,
                                onClick = {
                                    showLikesDialog = false
                                    if (userProfile.userId == currentUserId) {
                                        navigateToMiPerfil()
                                    } else {
                                        navigateToUserProfile(userProfile.userId)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLikesDialog = false }) {
                    Text("Cerrar", color = Color.White)
                }
            }
        )
    }

    if (showParticipantsDialog) {
        AlertDialog(
            onDismissRequest = { showParticipantsDialog = false },
            containerColor = Color.Black,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Participantes ($participants)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                if (isLoadingParticipants) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cargando perfiles...", color = Color.White)
                        }
                    }
                } else if (participantsUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay participantes aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(participantsUsers) { userProfile ->
                            UserListItem(
                                userProfile = userProfile,
                                onClick = {
                                    showParticipantsDialog = false
                                    if (userProfile.userId == currentUserId) {
                                        navigateToMiPerfil()
                                    } else {
                                        navigateToUserProfile(userProfile.userId)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParticipantsDialog = false }) {
                    Text("Cerrar", color = Color.White)
                }
            }
        )
    }
}

// Función helper para formatear números
private fun formatCount(count: Int): String {
    return if (count > 999) String.format("%.1fk", count / 1000.0) else {
        count.toString()
    }
}


// ✅ Función de extensión para formatear números
private fun Double.format(digits: Int) = "%.${digits}f".format(this)

// Función para formatear el tiempo transcurrido
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Ahora"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> "${diff / 604800000}sem"
    }
}

// Funciones quien dio me gusta
suspend fun handleLikeAction(
    planId: String,
    currentUserId: String,
    isLiked: Boolean,
    planOwnerId: String,
    db: FirebaseFirestore,
    context: Context,
    onSuccess: (Boolean, Int) -> Unit
) {
    try {
        val planRef = db.collection("planes").document(planId)

        if (isLiked) {
            // Quitar like
            planRef.update("likes", FieldValue.arrayRemove(currentUserId)).await()

            val document = planRef.get().await()
            val newLikes = document.get("likes") as? List<String> ?: emptyList()
            onSuccess(false, newLikes.size)

        } else {
            // Agregar like
            planRef.update("likes", FieldValue.arrayUnion(currentUserId)).await()

            val document = planRef.get().await()
            val newLikes = document.get("likes") as? List<String> ?: emptyList()
            onSuccess(true, newLikes.size)

            // Enviar notificación al dueño del plan
            if (planOwnerId != currentUserId) {
                sendNotification(
                    db = db,
                    recipientId = planOwnerId,
                    senderId = currentUserId,
                    type = "like",
                    planId = planId,
                    context = context
                )
            }
        }

    } catch (e: Exception) {
        Log.e("PlanCard", "Error al manejar like", e)
        Toast.makeText(context, "Error al procesar like", Toast.LENGTH_SHORT).show()
    }
}

// funcion de quien va  aparticiapr
suspend fun handleParticipateAction(
    planId: String,
    currentUserId: String,
    isParticipating: Boolean,
    planOwnerId: String,
    db: FirebaseFirestore,
    context: Context,
    onSuccess: (Boolean, Int) -> Unit
) {
    try {
        val planRef = db.collection("planes").document(planId)

        if (isParticipating) {
            // Dejar de participar
            planRef.update("participants", FieldValue.arrayRemove(currentUserId)).await()

            val document = planRef.get().await()
            val newParticipants = document.get("participants") as? List<String> ?: emptyList()
            onSuccess(false, newParticipants.size)

        } else {
            // Participar
            planRef.update("participants", FieldValue.arrayUnion(currentUserId)).await()

            val document = planRef.get().await()
            val newParticipants = document.get("participants") as? List<String> ?: emptyList()
            onSuccess(true, newParticipants.size)

            // Enviar notificación al dueño del plan
            if (planOwnerId != currentUserId) {
                sendNotification(
                    db = db,
                    recipientId = planOwnerId,
                    senderId = currentUserId,
                    type = "participate",
                    planId = planId,
                    context = context
                )
            }
        }
    } catch (e: Exception) {
        Log.e("PlanCard", "Error al manejar participación", e)
        Toast.makeText(context, "Error al procesar participación", Toast.LENGTH_SHORT).show()
    }
}

//funcion para ver quien compartio
suspend fun handleShareAction(
    planId: String,
    currentUserId: String,
    planOwnerId: String,
    planTitle: String,
    db: FirebaseFirestore,
    context: Context,
    onSuccess: (Int) -> Unit
) {
    try {
        val planRef = db.collection("planes").document(planId)

        // Obtener el documento del plan
        val document = planRef.get().await()

        val currentShares = document.getLong("shares")?.toInt() ?: 0
        val newShares = currentShares + 1

        // Actualizar el contador de shares
        planRef.update("shares", newShares).await()

        // Callback de éxito
        onSuccess(newShares)

        // Enviar notificación al dueño del plan
        if (planOwnerId != currentUserId) {
            sendNotification(
                db = db,
                recipientId = planOwnerId,
                senderId = currentUserId,
                type = "share",
                planId = planId,
                context = context
            )
        }

        // Mostrar opciones de compartir nativas
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Mira este plan increíble: $planTitle!")
            type = "text/plain"
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Compartir plan"))
        } catch (e: Exception) {
            Log.e("Share", "Error al compartir", e)
        }

    } catch (e: Exception) {
        Log.e("PlanCard", "Error al compartir", e)
        Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show()
    }
}


suspend fun getUserNames(userIds: List<String>, db: FirebaseFirestore): List<String> {
    return try {
        val userNames = mutableListOf<String>()

        for (userId in userIds) {
            val userDoc = db.collection("perfil").document(userId).get().await()
            val userName = userDoc.getString("nombre") ?: "Usuario desconocido"
            userNames.add(userName)
        }

        userNames
    } catch (e: Exception) {
        Log.e("getUserNames", "Error al obtener nombres", e)
        emptyList()
    }
}


// funcion para enviar notificaciones

// Función para obtener los planes desde Firestore
private suspend fun getPlanes(db: FirebaseFirestore): List<Plan> = withContext(Dispatchers.IO) {
    try {
        val documents = db.collection("planes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return@withContext documents.documents.mapNotNull { document ->
            try {
                val plan = document.toObject(Plan::class.java)
                plan?.copy(id = document.id) // ✅ Siempre asignar el ID
            } catch (e: Exception) {
                Log.w("getAllPlans", "Error convirtiendo documento ${document.id}: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("getAllPlans", "Error obteniendo planes: ${e.message}", e)
        emptyList()
    }
}

// Función para formatear fecha de timestamp a formato legible
private fun formatDate(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

// data class
data class Story(
    val id: String,
    val imageUrl: String,
    val userId: String,
    val username: String,
    val timestamp: Long,
    val viewers: List<String> = emptyList()
)



@Composable
fun MenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}



