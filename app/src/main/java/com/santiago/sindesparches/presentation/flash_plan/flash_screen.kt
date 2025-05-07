package com.santiago.sindesparches.presentation.flash_plan

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.FirebaseDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

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

    @Composable
    fun flash_plan(auth: FirebaseAuth, db: FirebaseFirestore, navigateToHome: () -> Unit) {

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

        //traer nombre de firebase
        var nombre by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            com.santiago.sindesparches.presentation.home.obtenerNombreUsuario { resultado ->
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
                // Ruta que coincide con las reglas de seguridad
                val imagePath = "flashPlans/$userId/$imageName"

                // 1. Comprimir imagen
                val compressedImage = compressImage(uri)

                // 2. Subir a Storage
                val imageRef = storageRef.child(imagePath)
                imageRef.putBytes(compressedImage).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()

                // 3. Crear documento en flashPlans con campos adicionales
                val flashPlanData = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "userPhoto" to userPhoto,
                    "imageUrl" to downloadUrl,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "viewers" to listOf<String>() // Lista vacía inicial de viewers
                )

                db.collection("flashPlans").add(flashPlanData).await()

                withContext(Dispatchers.Main) {
                    showSuccessDialog = true
                }

                return downloadUrl
            } catch (e: Exception) {
                errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Error de red. Verifica tu conexión a internet."
                    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "No tienes permisos para guardar el FlashPlan."
                    e is StorageException -> {
                        val code = (e as StorageException).errorCode
                        val httpCode = (e as StorageException).httpResultCode
                        val detailedMessage = e.message ?: "Sin detalles"
                        Log.e("StorageException", "Código: $code, HTTP: $httpCode, Mensaje: $detailedMessage")
                        "Error en Firebase Storage ($httpCode): $detailedMessage"
                    }
                    else -> "Error inesperado: ${e.localizedMessage ?: "Error desconocido"}"
                }

                withContext(Dispatchers.Main) {
                    showErrorDialog = true
                }
                return null
            }
        }

        // Diálogo de éxito
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("¡Éxito!") },
                text = { Text("La imagen se ha subido correctamente") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            navigateToHome() // Navegar a home después de aceptar
                        }
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }

    // Diálogo de error
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(
                        onClick = { showErrorDialog = false }
                    ) {
                        Text("Entendido")
                    }
                }
            )
        }

        // UI Principal
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mostrar imagen seleccionada/subida
                when {
                    isLoading -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Procesando imagen...")
                        }
                    }
                    imageUrl != null -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Imagen del FlashPlan",
                                modifier = Modifier.size(250.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Imagen cargada exitosamente")

                        }
                    }
                    imageUri != null -> {
                        val bitmap = remember(imageUri) {
                            context.contentResolver.openInputStream(imageUri!!)?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                painter = BitmapPainter(bitmap.asImageBitmap()),
                                contentDescription = "Imagen seleccionada",
                                modifier = Modifier.size(250.dp)
                            )
                        } else {
                            Text("No se pudo cargar la imagen")
                        }
                    }
                    else -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                contentDescription = "Seleccionar imagen",
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Selecciona una imagen para tu FlashPlan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón para seleccionar imagen
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seleccionar Imagen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para subir imagen
                if (imageUri != null && imageUrl == null) {
                    Button(
                        onClick = {
                            scope.launch {
                                imageUrl = uploadFlashPlanImage(imageUri!!)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subiendo...")
                            }
                        } else {
                            Text("Subir Imagen")
                        }
                    }
                }
            }

            // Mostrar estado de la subida
            uploadStatus?.let { status ->
                AlertDialog(
                    onDismissRequest = { uploadStatus = null },
                    title = { Text(if (status.startsWith("¡")) "Éxito" else "Aviso") },
                    text = { Text(status) },
                    confirmButton = {
                        TextButton(
                            onClick = { uploadStatus = null }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }

            // Diálogo de confirmación para salir
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Confirmar salida") },
                    text = { Text("¿Estás seguro de que quieres salir? Se perderán los cambios no guardados.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showExitDialog = false
                                navigateToHome()
                            }
                        ) {
                            Text("Salir")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExitDialog = false }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }