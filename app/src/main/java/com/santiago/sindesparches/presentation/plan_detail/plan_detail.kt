package com.santiago.sindesparches.presentation.plan_detail

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.santiago.sindesparches.presentation.publicaciones.Plan
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
import com.santiago.sindesparches.ui.theme.boton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToUserProfile: (String) -> Unit,
    navigateToChat: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var plan by remember { mutableStateOf<Plan?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.title ?: "Detalles del Plan", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (plan?.userId == auth.currentUser?.uid) {
                        IconButton(onClick = { navigateToEdit(planId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Plan", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Plan", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                // Error handling UI
            } else if (plan != null) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (plan!!.imageUrls.isNotEmpty()) {
                        LazyRow(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                            items(plan!!.imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Imagen del plan",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillParentMaxWidth()
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = plan!!.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        InfoRow(icon = Icons.Default.DateRange, text = formatDate(plan!!.date))
                        InfoRow(icon = Icons.Default.Star, text = plan!!.timeString)
                        InfoRow(icon = Icons.Default.LocationOn, text = plan!!.location)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = plan!!.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        userProfile?.let { profile ->
                            Row(
                                modifier = Modifier.clickable { navigateToUserProfile(plan!!.userId) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = profile.profileImageUrl,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.size(48.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Creado por", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(
                                        profile.nombre ?: "Usuario",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        if (plan!!.enableWhatsapp == true && plan!!.phoneNumber != null) {
                            Button(
                                onClick = {
                                    val message = "Hola, estoy interesado en el plan: ${plan!!.title}"
                                    val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
                                    val whatsappUrl = "https://api.whatsapp.com/send?phone=${plan!!.phoneNumber}&text=$encodedMessage"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("WhatsApp no instalado") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Text("Contactar por WhatsApp", color = Color.White)
                            }
                        }
                        Button(
                            onClick = { navigateToChat(planId) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text("Abrir chat del plan")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar plan") },
            text = { Text("¿Estás seguro de que quieres eliminar este plan?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                deletePlan(db, planId)
                                snackbarHostState.showSnackbar("Plan eliminado")
                                navigateBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al eliminar el plan")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
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
data class UserProfile(
    val userId: String,
    val nombre: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val createdAt: Long? = null
    // Añade otros campos que tengas en tu colección perfil
)