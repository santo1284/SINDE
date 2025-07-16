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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    navigateToUserProfile: (String) -> Unit = {},
    navigateToMiPerfil: () -> Unit = {},
    navigateToEditPlan: (String) -> Unit = {},
    navigateToMegusta: () -> Unit = {},
    navigateToParticipar: () -> Unit = {},
    navigateToNotificaciones: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showRightMenu by remember { mutableStateOf(false) }
    var showLeftMenu by remember { mutableStateOf(false) }
    var showStory by remember { mutableStateOf(false) }
    var currentStory by remember { mutableStateOf<Story?>(null) }
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isButtonVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (deltaY < -10) {
                    isButtonVisible = false
                } else if (deltaY > 10) {
                    isButtonVisible = true
                }
                return Offset.Zero
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var planes by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var allPlanes by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var isLoadingpublicacion by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun filterPlanes(searchQuery: String) {
        planes = if (searchQuery.isBlank()) {
            allPlanes
        } else {
            allPlanes.filter { plan ->
                plan.title.contains(searchQuery, ignoreCase = true) ||
                        plan.description.contains(searchQuery, ignoreCase = true) ||
                        plan.location.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(searchText) {
        filterPlanes(searchText)
    }

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
    var nombreuser by remember { mutableStateOf<String?>(null) }
    val nombreCorto = nombreuser?.split(" ")?.firstOrNull() ?: ""

    fun obtenerFotoPerfilUrl(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$uid")
        storageRef.downloadUrl.addOnSuccessListener { uri -> onResult(uri.toString()) }.addOnFailureListener { onResult(null) }
    }

    fun NombreUsuario(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        FirebaseFirestore.getInstance().collection("perfil").document(uid).get().addOnSuccessListener { document ->
            onResult(document.getString("nombre"))
        }.addOnFailureListener { onResult(null) }
    }

    LaunchedEffect(Unit) {
        NombreUsuario { nombreuser = it }
        obtenerFotoPerfilUrl { imagenUrl = it }
    }

    var nombre_flashplan by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        obtenerNombreUsuario { resultado ->
            nombre_flashplan = resultado
        }
    }

    LaunchedEffect(Unit) {
        try {
            val twentyFourHoursAgo = System.currentTimeMillis() - 86400000
            db.collection("flashPlans")
                .whereGreaterThan("timestamp", Timestamp(twentyFourHoursAgo / 1000, 0))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    val flashPlanList = mutableListOf<Story>()
                    val userIds = mutableSetOf<String>()
                    snapshot?.documents?.forEach { doc ->
                        val userId = doc.getString("userId") ?: ""
                        if (userId.isNotEmpty()) {
                            userIds.add(userId)
                        }
                        flashPlanList.add(
                            Story(
                                id = doc.id,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                userId = userId,
                                username = doc.getString("userName") ?: nombre_flashplan.orEmpty(),
                                timestamp = (doc.getTimestamp("timestamp")?.toDate()?.time
                                    ?: System.currentTimeMillis()),
                                viewers = doc.get("viewers") as? List<String> ?: emptyList()
                            )
                        )
                    }
                    stories = flashPlanList
                    if (userIds.isNotEmpty()) {
                        val profileImagesMap = mutableMapOf<String, String>()
                        userIds.forEach { userId ->
                            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$userId")
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                profileImagesMap[userId] = uri.toString()
                                userProfileImages = profileImagesMap.toMap()
                            }.addOnFailureListener {
                                db.collection("perfil").document(userId).get().addOnSuccessListener { userDoc ->
                                    val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                                    if (profileImageUrl.isNotEmpty()) {
                                        profileImagesMap[userId] = profileImageUrl
                                        userProfileImages = profileImagesMap.toMap()
                                    }
                                }
                            }
                        }
                    }
                    isLoading = false
                }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoadingpublicacion = true
                val loadedPlanes = getPlanes(db)
                allPlanes = loadedPlanes
                planes = loadedPlanes
                isLoadingpublicacion = false
            } catch (e: Exception) {
                errorMessage = "Error al cargar los planes: ${e.message}"
                isLoadingpublicacion = false
            }
        }
    }

    BackHandler {
        when {
            showStory -> showStory = false
            showRightMenu -> showRightMenu = false
            showLeftMenu -> showLeftMenu = false
            else -> showDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(black)) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        if (!showLeftMenu && !showRightMenu) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (dragAmount < -10) {
                                    showRightMenu = true
                                    showLeftMenu = false
                                } else if (dragAmount > 10) {
                                    showLeftMenu = true
                                    showRightMenu = false
                                }
                                change.consume()
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(black)
                        .alpha(if (showLeftMenu || showRightMenu) 0.3f else 1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SINDE", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Buscar..", color = Color.Gray) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                                    }
                                }
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isButtonVisible,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                FloatingActionButton(
                                    onClick = navigateToPublicaciones,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Crear publicación", modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(onClick = {
                                showRightMenu = !showRightMenu
                                if (showRightMenu) showLeftMenu = false
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                            }
                        }
                    }
                    if (searchText.isNotEmpty()) {
                        Text(
                            text = "Encontrados: ${planes.size} plan${if (planes.size != 1) "es" else ""}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            isLoadingpublicacion -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                            errorMessage != null -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = {
                                        coroutineScope.launch {
                                            errorMessage = null
                                            isLoadingpublicacion = true
                                            val loadedPlanes = getPlanes(db)
                                            allPlanes = loadedPlanes
                                            filterPlanes(searchText)
                                            isLoadingpublicacion = false
                                        }
                                    }) {
                                        Text("Reintentar")
                                    }
                                }
                            }
                            planes.isEmpty() -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (searchText.isNotEmpty()) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
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
                                        Button(onClick = { searchText = "" }) { Text("Limpiar búsqueda") }
                                    } else {
                                        Text(
                                            text = "No hay planes disponibles",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = navigateToPublicaciones) { Text("Crear Plan") }
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
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
                                            searchText = searchText,
                                            navigateToUserProfile = navigateToUserProfile,
                                            navigateToEditPlan = navigateToEditPlan,
                                            navigateToMiPerfil = navigateToMiPerfil,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showLeftMenu || showRightMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) { detectTapGestures { showLeftMenu = false; showRightMenu = false } }
                    .zIndex(0.5f)
            )
        }
        Box(
            modifier = Modifier
                .offset(x = leftMenuOffset)
                .width(150.dp)
                .fillMaxHeight()
                .background(boton)
                .padding(top = 60.dp, start = 16.dp)
                .zIndex(2f)
        ) {
            Column {
                Spacer(Modifier.height(50.dp))
                Button(
                    modifier = Modifier.height(75.dp).width(75.dp).clip(CircleShape),
                    onClick = navigateToFlashPlan
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                Spacer(Modifier.height(12.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (stories.isEmpty()) {
                    Text("No hay FlashPlans disponibles", modifier = Modifier.padding(vertical = 16.dp), color = Color.White)
                } else {
                    Text("FlashPlans", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn {
                        items(stories.size) { index ->
                            val story = stories[index]
                            val seen = story.viewers.contains(currentUserId)
                            Box(
                                modifier = Modifier.clickable {
                                    showStory = true
                                    currentStory = story
                                    if (!seen && story.userId != currentUserId) {
                                        db.collection("flashPlans").document(story.id)
                                            .update("viewers", FieldValue.arrayUnion(currentUserId))
                                            .addOnSuccessListener {
                                                stories = stories.map { s ->
                                                    if (s.id == story.id) s.copy(viewers = s.viewers + currentUserId) else s
                                                }
                                            }
                                    }
                                }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = story.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(75.dp)
                                            .clip(CircleShape)
                                            .border(width = 2.dp, color = if (seen) Color.Gray else Color.Magenta, shape = CircleShape),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = android.R.drawable.ic_menu_gallery),
                                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (story.username.length > 8) story.username.take(6) + "..." else story.username,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = rightMenuOffset)
                .width(200.dp)
                .height(910.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .padding(10.dp)
                .zIndex(2f)
        ) {
            Column {
                Text("Menú derecho", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Button(
                    onClick = navigateToMiPerfil,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (imagenUrl != null) {
                            AsyncImage(
                                model = imagenUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Box(modifier = Modifier.size(55.dp).clip(CircleShape).background(Color.Gray))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = nombreCorto ?: "Cargando...", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = navigateToMegusta) { Text("Me Gusta") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = navigateToParticipar) { Text("Participar") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = navigateToNotificaciones) { Text("Notificaciones") }
                Button(onClick = { showRightMenu = false }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
        if (showStory && currentStory != null) {
            LaunchedEffect(currentStory) {
                delay(30000)
                showStory = false
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).clickable { showStory = false }.zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { navigateToUserProfile(currentStory!!.userId) },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val userImageUrl = userProfileImages[currentStory!!.userId]
                        if (userImageUrl != null && userImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = userImageUrl,
                                contentDescription = "Imagen de perfil",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = android.R.drawable.ic_menu_gallery),
                                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                            )
                        } else {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Usuario", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = currentStory!!.username, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    AsyncImage(
                        model = currentStory!!.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        contentScale = ContentScale.Fit,
                        error = painterResource(id = android.R.drawable.ic_menu_gallery)
                    )
                }
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("¿Cerrar sesión?") },
                text = { Text("¿Estás seguro de que quieres salir?") },
                confirmButton = { Button(onClick = { auth.signOut(); navigateToInicial() }) { Text("Sí, salir") } },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
            )
        }
    }
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




