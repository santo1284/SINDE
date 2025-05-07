package com.santiago.sindesparches.presentation.home

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.boton


@Composable
fun homeScreen(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    navigateToInicial: () -> Unit = {},
    navigateToFlashPlan: () -> Unit = {}

) {
    var showDialog by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showRightMenu by remember { mutableStateOf(false) }
    var showLeftMenu by remember { mutableStateOf(false) }
    var showStory by remember { mutableStateOf(false) }
    var currentStory by remember { mutableStateOf<Story?>(null) }
    var publicaciones by remember { mutableStateOf<List<Publicacion>>(emptyList()) }
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = auth.currentUser?.uid.orEmpty()
    val context = LocalContext.current

    var nombre by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        obtenerNombreUsuario { resultado ->
            nombre = resultado
        }
    }


    // Cargar los FlashPlans reales desde Firestore
    LaunchedEffect(Unit) {
        try {
            // Obtenemos las últimas 24 horas en timestamp
            val twentyFourHoursAgo = System.currentTimeMillis() - 86400000

            // Consulta a Firestore para obtener los FlashPlans de las últimas 24 horas
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

                    snapshot?.documents?.forEach { doc ->
                        val userId = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: nombre.orEmpty()
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val timestamp = (doc.getTimestamp("timestamp")?.toDate()?.time
                            ?: System.currentTimeMillis())
                        val viewers = doc.get("viewers") as? List<String> ?: emptyList()

                        flashPlanList.add(
                            Story(
                                id = doc.id,
                                imageUrl = imageUrl,
                                userId = userId,
                                username = userName,
                                timestamp = timestamp,
                                viewers = viewers
                            )
                        )
                    }

                    stories = flashPlanList
                    isLoading = false
                }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error al configurar listener de FlashPlans", e)
            isLoading = false
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

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final)))
                .pointerInput(showLeftMenu, showRightMenu) {
                    detectTapGestures {
                        if (showLeftMenu || showRightMenu) {
                            showLeftMenu = false
                            showRightMenu = false
                        }
                    }
                }
                .pointerInput(Unit) {
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
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SINDE", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Buscar...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        showRightMenu = !showRightMenu
                        if (showRightMenu) showLeftMenu = false
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                }

                LazyColumn(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                ) {
                    items(publicaciones.size) { index ->
                        val publicacion = publicaciones[index]
                        PublicacionItem(publicacion = publicacion)
                    }
                }
            }

            // Botón para navegar a crear historia y mostrar FlashPlans
            if (showLeftMenu) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(150.dp)
                        .fillMaxHeight()
                        .background(boton)
                        .padding(start = 16.dp)



                ) {

                    Spacer(Modifier.height(50.dp))

                    Button(modifier = Modifier.clip(CircleShape), onClick = navigateToFlashPlan) {
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
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            "FlashPlans",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn {
                            items(stories.size) { index ->
                                val story = stories[index]
                                val seen = story.viewers.contains(currentUserId)
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            showStory = true
                                            currentStory = story

                                            // Marcar como visto cuando se abre
                                            if (!seen && story.userId != currentUserId) {
                                                // Actualizar la lista de viewers en Firestore
                                                db.collection("flashPlans").document(story.id)
                                                    .update("viewers", FieldValue.arrayUnion(currentUserId))
                                            }
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Imagen con borde de color
                                        AsyncImage(
                                            model = story.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(75.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (seen) Color.Gray else Color.Magenta,
                                                    shape = CircleShape
                                                ),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(id = android.R.drawable.ic_menu_gallery),
                                            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Nombre de usuario abreviado
                                        Text(
                                            text = if (story.username.length > 8)
                                                story.username.take(6) + "..."
                                            else
                                                story.username,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Historia en pantalla completa por 30s
            if (showStory && currentStory != null) {
                LaunchedEffect(currentStory) {
                    delay(3000)
                    showStory = false
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { showStory = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Mostrar el nombre del autor arriba de la historia
                        Text(
                            text = currentStory!!.username,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        // Imagen de la historia
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

            // Menú derecho
            if (showRightMenu) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(250.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray)
                        .padding(16.dp)
                ) {
                    Text("Menú derecho", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Opción 1")
                    Text("Opción 2")
                    Button(onClick = { showRightMenu = false }) {
                        Text("Cerrar")
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
}

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

data class Story(
    val id: String,
    val imageUrl: String,
    val userId: String,
    val username: String,
    val timestamp: Long,
    val viewers: List<String> = emptyList()
)

data class Publicacion(
    val id: String,
    val titulo: String,
    val contenido: String,
    val imageUrl: String? = null,
    val autorId: String,
    val autorNombre: String,
    val timestamp: Long
)

@Composable
fun PublicacionItem(publicacion: Publicacion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),

    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar o imagen de perfil (se podría reemplazar por la imagen real del usuario)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = publicacion.autorNombre,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTimeAgo(publicacion.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = publicacion.titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = publicacion.contenido)

            // Si hay una imagen, mostrarla
            publicacion.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = "Imagen de la publicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_gallery)
                )
            }

            // Botones de interacción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { /* Implementar Me gusta */ }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = "Me gusta",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Me gusta")
                    }
                }

                TextButton(onClick = { /* Implementar Comentar */ }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Comentar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Comentar")
                    }
                }

                TextButton(onClick = { /* Implementar Compartir */ }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir")
                    }
                }
            }
        }
    }
}

// Función auxiliar para formatear el tiempo transcurrido
fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Hace un momento"
        diff < 3600_000 -> "${diff / 60_000} min"
        diff < 86400_000 -> "${diff / 3600_000} h"
        else -> "${diff / 86400_000} d"
    }
}