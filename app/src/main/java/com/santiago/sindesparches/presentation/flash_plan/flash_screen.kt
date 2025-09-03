package com.santiago.sindesparches.presentation.flash_plan

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.unit.Dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

// Colores del tema nocturno
object NightTheme {
    val Background = Color(0xFF0A0A0F)
    val Surface = Color(0xFF1A1A2E)
    val SurfaceVariant = Color(0xFF16213E)
    val Primary = Color(0xFFE91E63) // Fucsia
    val PrimaryVariant = Color(0xFF8E24AA) // Fucsia más oscuro
    val Secondary = Color(0xFFFFC107) // Amarillo
    val Tertiary = Color(0xFF2196F3) // Azul
    val OnSurface = Color(0xFFE0E0E0)
    val OnSurfaceVariant = Color(0xFFB0B0B0)
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFFF5252)
}

//funcion traer nombre de firebase
fun obtenerNombreUsuario(onNombreObtenido: (String?) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    if (uid != null) {
        db.collection("perfil").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre")
                    onNombreObtenido(nombre)
                } else {
                    onNombreObtenido(null)
                }
            }
            .addOnFailureListener {
                onNombreObtenido(null)
            }
    } else {
        onNombreObtenido(null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun flash_plan(auth: FirebaseAuth,
               db: FirebaseFirestore,
               navigateToHome: () -> Unit) {

    // Estados
    var showExitDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Configuración responsiva
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenHeight < 700.dp
    val isVerySmallScreen = screenHeight < 600.dp

    // Tamaños adaptativos
    val cardSize = when {
        isVerySmallScreen -> (screenWidth * 0.6f).coerceAtMost(200.dp)
        isSmallScreen -> (screenWidth * 0.7f).coerceAtMost(240.dp)
        else -> (screenWidth * 0.75f).coerceAtMost(280.dp)
    }

    val imageSize = cardSize * 0.7f
    val topPadding = when {
        isVerySmallScreen -> 80.dp
        isSmallScreen -> 90.dp
        else -> 110.dp
    }

    val titleSize = when {
        isVerySmallScreen -> 22.sp
        isSmallScreen -> 24.sp
        else -> 28.sp
    }

    val subtitleSize = when {
        isVerySmallScreen -> 14.sp
        isSmallScreen -> 15.sp
        else -> 16.sp
    }

    val buttonHeight = if (isSmallScreen) 48.dp else 56.dp
    val iconSize = if (isSmallScreen) 20.dp else 24.dp
    val circleSize = if (isSmallScreen) 60.dp else 80.dp
    val circleIconSize = if (isSmallScreen) 30.dp else 40.dp

    // Traer nombre de firebase
    var nombre by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        obtenerNombreUsuario { resultado ->
            nombre = resultado
        }
    }

    // Launcher para seleccionar imagen
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            imageUri = uri
            imageUrl = null
            uploadStatus = null
        }
    )

    // Manejador del botón atrás
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    // Función para comprimir imagen
    suspend fun compressImage(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // Redimensionar manteniendo aspect ratio
            val maxSize = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                maxSize.toFloat() / width
            } else {
                maxSize.toFloat() / height
            }

            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )

            // Comprimir en formato WEBP
            ByteArrayOutputStream().use { outputStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 80, outputStream)
                scaledBitmap.recycle()
                originalBitmap.recycle()
                outputStream.toByteArray()
            }
        } ?: throw Exception("No se pudo leer la imagen")
    }

    // Función para subir imagen
    suspend fun uploadFlashPlanImage(uri: Uri): String? {
        return try {
            val user = auth.currentUser ?: throw Exception("Usuario no autenticado")
            val userId = user.uid
            val userName = user.displayName ?: nombre
            val userPhoto = user.photoUrl?.toString() ?: ""

            val storageRef = FirebaseStorage.getInstance().reference
            val imageName = "${UUID.randomUUID()}.webp"
            val imagePath = "flashPlans/$userId/$imageName"

            // 1. Comprimir imagen
            val compressedImage = compressImage(uri)

            // 2. Subir a Storage
            val imageRef = storageRef.child(imagePath)
            imageRef.putBytes(compressedImage).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            // 3. Crear documento en flashPlans
            val flashPlanData = mapOf(
                "userId" to userId,
                "userName" to userName,
                "userPhoto" to userPhoto,
                "imageUrl" to downloadUrl,
                "timestamp" to FieldValue.serverTimestamp(),
                "viewers" to listOf<String>()
            )

            db.collection("flashPlans").add(flashPlanData).await()

            withContext(Dispatchers.Main) {
                showSuccessDialog = true
            }

            downloadUrl // Retorna directamente, sin 'return'
        } catch (e: Exception) {
            errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de red. Verifica tu conexión a internet."
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "No tienes permisos para guardar el FlashPlan."
                e is StorageException -> {
                    // Removido el cast innecesario
                    val code = e.errorCode
                    val httpCode = e.httpResultCode
                    val detailedMessage = e.message ?: "Sin detalles"
                    Log.e("StorageException", "Código: $code, HTTP: $httpCode, Mensaje: $detailedMessage")
                    "Error en Firebase Storage ($httpCode): $detailedMessage"
                }
                else -> "Error inesperado: ${e.localizedMessage ?: "Error desconocido"}"
            }

            withContext(Dispatchers.Main) {
                showErrorDialog = true
            }
            null // Retorna null en caso de error
        }
    }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // UI Principal con scroll
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NightTheme.Background,
                        NightTheme.Surface.copy(alpha = 0.6f),
                        NightTheme.Background
                    )
                )
            )
    ) {
        // Fondo con efectos adaptativos
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NightTheme.Primary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = if (isSmallScreen) 400f else 800f
                    )
                )
        )

        // Header responsivo
        TopAppBar(
            title = {
                Text(
                    text = "Crear FlashPlan",
                    color = NightTheme.OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isSmallScreen) 18.sp else 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { showExitDialog = true }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = NightTheme.OnSurface,
                        modifier = Modifier.size(iconSize)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.zIndex(1f)
        )

        // Contenido principal scrolleable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
                .padding(top = topPadding, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título y subtítulo adaptativos
            Text(
                text = "Comparte tu momento",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = NightTheme.OnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 6.dp else 8.dp))

            Text(
                text = "Selecciona una imagen para tu FlashPlan",
                fontSize = subtitleSize,
                color = NightTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = if (isSmallScreen) 20.sp else 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 40.dp))

            // Área de imagen responsiva
            Card(
                modifier = Modifier
                    .size(cardSize)
                    .clip(RoundedCornerShape(if (isSmallScreen) 16.dp else 20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = NightTheme.Surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSmallScreen) 6.dp else 8.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    NightTheme.Primary.copy(alpha = 0.1f),
                                    NightTheme.Tertiary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            LoadingContent(isSmallScreen = isSmallScreen)
                        }
                        imageUrl != null -> {
                            SuccessContent(imageUrl = imageUrl!!, imageSize = imageSize, isSmallScreen = isSmallScreen)
                        }
                        imageUri != null -> {
                            SelectedImageContent(imageUri = imageUri!!, context = context, imageSize = imageSize, isSmallScreen = isSmallScreen)
                        }
                        else -> {
                            EmptyStateContent(
                                glowAlpha = glowAlpha,
                                onClick = { galleryLauncher.launch("image/*") },
                                circleSize = circleSize,
                                iconSize = circleIconSize,
                                isSmallScreen = isSmallScreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 40.dp))

            // Botones adaptativos
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isSmallScreen) 8.dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botón seleccionar imagen
                    ElevatedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = NightTheme.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
                        elevation = ButtonDefaults.elevatedButtonColors().let {
                            ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
                        }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 12.dp))
                        Text(
                            text = "Seleccionar Imagen",
                            fontSize = if (isSmallScreen) 14.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Botón subir imagen
                    AnimatedVisibility(
                        visible = imageUri != null && imageUrl == null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))

                            ElevatedButton(
                                onClick = {
                                    isLoading = true
                                    scope.launch {
                                        imageUrl = uploadFlashPlanImage(imageUri!!)
                                        isLoading = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(buttonHeight),
                                enabled = !isLoading,
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = NightTheme.Secondary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
                                elevation = ButtonDefaults.elevatedButtonColors().let {
                                    ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
                                }
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(if (isSmallScreen) 16.dp else 20.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 12.dp))
                                    Text(
                                        text = "Subiendo...",
                                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize)
                                    )
                                    Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 12.dp))
                                    Text(
                                        text = "Subir FlashPlan",
                                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogos
        if (showSuccessDialog) {
            ModernDialog(
                title = "¡Éxito!",
                message = "Tu FlashPlan se ha creado correctamente",
                confirmText = "Continuar",
                onConfirm = {
                    showSuccessDialog = false
                    navigateToHome()
                },
                type = DialogType.Success,
                isSmallScreen = isSmallScreen
            )
        }

        if (showErrorDialog) {
            ModernDialog(
                title = "Error",
                message = errorMessage,
                confirmText = "Entendido",
                onConfirm = { showErrorDialog = false },
                type = DialogType.Error,
                isSmallScreen = isSmallScreen
            )
        }

        if (showExitDialog) {
            ModernDialog(
                title = "Confirmar salida",
                message = "¿Estás seguro de que quieres salir? Se perderán los cambios no guardados.",
                confirmText = "Salir",
                dismissText = "Cancelar",
                onConfirm = {
                    showExitDialog = false
                    navigateToHome()
                },
                onDismiss = { showExitDialog = false },
                type = DialogType.Warning,
                isSmallScreen = isSmallScreen
            )
        }
    }
}

@Composable
private fun LoadingContent(isSmallScreen: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(if (isSmallScreen) 36.dp else 48.dp),
            color = NightTheme.Primary,
            strokeWidth = if (isSmallScreen) 3.dp else 4.dp
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text(
            text = "Procesando imagen...",
            color = NightTheme.OnSurfaceVariant,
            fontSize = if (isSmallScreen) 12.sp else 14.sp
        )
    }
}

@Composable
private fun SuccessContent(imageUrl: String, imageSize: Dp, isSmallScreen: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "FlashPlan creado",
            modifier = Modifier
                .size(imageSize)
                .clip(RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text(
            text = "FlashPlan creado",
            color = NightTheme.Success,
            fontSize = if (isSmallScreen) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SelectedImageContent(imageUri: Uri, context: android.content.Context, imageSize: Dp, isSmallScreen: Boolean) {
    val bitmap = remember(imageUri) {
        context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = "Imagen seleccionada",
            modifier = Modifier
                .size(imageSize)
                .clip(RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(
            text = "Error al cargar imagen",
            color = NightTheme.Error,
            fontSize = if (isSmallScreen) 12.sp else 14.sp
        )
    }
}

@Composable
private fun EmptyStateContent(
    glowAlpha: Float,
    onClick: () -> Unit,
    circleSize: Dp,
    iconSize: Dp,
    isSmallScreen: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(if (isSmallScreen) 16.dp else 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(
                    color = NightTheme.Primary.copy(alpha = glowAlpha),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Agregar imagen",
                modifier = Modifier.size(iconSize),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text(
            text = "Toca para agregar\nuna imagen",
            color = NightTheme.OnSurfaceVariant,
            fontSize = if (isSmallScreen) 12.sp else 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = if (isSmallScreen) 16.sp else 20.sp
        )
    }
}

enum class DialogType {
    Success, Error, Warning
}

@Composable
private fun ModernDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    type: DialogType,
    isSmallScreen: Boolean
) {
    val iconColor = when (type) {
        DialogType.Success -> NightTheme.Success
        DialogType.Error -> NightTheme.Error
        DialogType.Warning -> NightTheme.Secondary
    }

    AlertDialog(
        onDismissRequest = onDismiss ?: {},
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (type) {
                        DialogType.Success -> Icons.Default.Check
                        DialogType.Error -> Icons.Default.Close
                        DialogType.Warning -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = NightTheme.OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isSmallScreen) 16.sp else 18.sp
                )
            }
        },
        text = {
            Text(
                text = message,
                color = NightTheme.OnSurfaceVariant,
                lineHeight = if (isSmallScreen) 18.sp else 22.sp,
                fontSize = if (isSmallScreen) 14.sp else 16.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = iconColor
                )
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isSmallScreen) 14.sp else 16.sp
                )
            }
        },
        dismissButton = dismissText?.let { text ->
            {
                TextButton(
                    onClick = onDismiss ?: {},
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = NightTheme.OnSurfaceVariant
                    )
                ) {
                    Text(
                        text = text,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    )
                }
            }
        },
        containerColor = NightTheme.Surface,
        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp)
    )
}