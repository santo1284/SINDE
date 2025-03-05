package com.santiago.sindesparches.presentation.perfil

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

@Composable
fun PerfilScreen(auth: FirebaseAuth) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val userId = auth.currentUser?.uid

    // Selector de imagen
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Imagen seleccionada",
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Seleccionar imagen")
        }
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("REPORTE", "Usuario no autenticado")
            return
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                imageUri?.let { uri ->
                    userId?.let { uid ->
                        uploadImageToFirebase(uri, uid) { url ->
                            imageUrl = url
                        }
                    } ?: Log.e("Upload", "Usuario no autenticado")
                } ?: Log.e("Upload", "No se ha seleccionado una imagen")
            },
            enabled = imageUri != null // Deshabilita el botón si no hay imagen seleccionada
        ) {
            Text("Subir imagen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageUrl?.let {
            Text("Imagen subida con éxito")
        }
    }
}

// Función para subir imagen a Firebase Storage
fun uploadImageToFirebase(uri: Uri, userId: String, onSuccess: (String) -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$userId")

    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { url ->
                onSuccess(url.toString()) // Devuelve la URL de la imagen
                Log.d("Upload", "Imagen subida exitosamente: $url")
            }
        }
        .addOnFailureListener { e ->
            Log.e("Upload", "Error al subir imagen", e)
        }
}
