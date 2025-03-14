package com.santiago.sindesparches.presentation.perfil

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.placeholder
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.white

@Composable

fun PerfilScreen(auth: FirebaseAuth, db: FirebaseFirestore, navigate_registro_completo: (nombre: String) -> Unit = {}) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var nombre by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var selectedCiudad by remember { mutableStateOf("") }
    val userId = auth.currentUser?.uid

    // Selector de imagen
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
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


        Spacer(modifier = Modifier.height(16.dp))

        imageUrl?.let {
            Text("Imagen subida con éxito")
        }
        // Campo de texto para ingresar el nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = { Text("nombre de usuario" , color = Color.White) },
            modifier = Modifier
                .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = boton,
                unfocusedContainerColor = boton,
                unfocusedIndicatorColor = Color.White,
                focusedIndicatorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = black,
                cursorColor = white,
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo para poner la edad
        var edad by remember { mutableStateOf<Int?>(null) }

        EdadDropdown { nuevaEdad ->
            edad = nuevaEdad
        }

// Muestra la edad seleccionada
        edad?.let {
            Text(text = "Edad seleccionada: $it años", fontSize = 16.sp, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        //muestra el sexo a elegir
        var selectedSexo by remember { mutableStateOf("Masculino") }

        SexoDropDown(selectedSexo) { nuevoSexo ->
            selectedSexo = nuevoSexo
        }

        Spacer(modifier = Modifier.height(16.dp))
        //seleccionar ciudad donde se encuentra

        CiudadDropdown { nuevaCiudad ->
            selectedCiudad = nuevaCiudad
        }


        // Botón para guardar en Firestore y la imagen
        Button(onClick = {

            if (nombre.isNotBlank() && edad != null && imageUri != null && selectedSexo.isNotBlank() && selectedCiudad.isNotBlank()) {
                imageUri?.let { uri ->
                    userId.let { uid ->
                        uploadImageToFirebase(uri, uid) { url ->
                            imageUrl = url
                        }
                    } ?: Log.e("Upload", "Usuario no autenticado")
                } ?: Log.e("Upload", "No se ha seleccionado una imagen")

                guardarPerfilEnFirestore(userId, nombre, edad!!, selectedSexo, selectedCiudad) { success ->
                     if(success) {
                        mensaje= "Perfil guardado correctamente"
                         navigate_registro_completo(nombre)

                    }else mensaje= "Error al guardar el perfil"
                }
            } else {
                mensaje = "Por favor ingresa todos los datos"
            }
        },enabled = imageUri != null){ // Deshabilita el botón si no hay imagen seleccionada
            Text("Guardar Perfil")
        }

        // Mensaje de confirmación
        if (mensaje.isNotEmpty()) {
            Text(text = mensaje, fontSize = 16.sp, color = Color.Black)
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

fun guardarPerfilEnFirestore(userId: String?,
                             nombre: String,
                             edad: Int,
                             sexo: String,
                             ciudad:String,
                             onResult: (Boolean) -> Unit) {
    if (userId == null) {
        Log.e("Firestore", "Usuario no autenticado")
        onResult(false)
        return
    }

    val db = FirebaseFirestore.getInstance()
    val perfilData = hashMapOf(
        "nombre" to nombre,
        "edad" to edad,
        "sexo" to sexo,
        "ciudad" to ciudad
    )

    db.collection("perfil").document(userId)
        .set(perfilData)
        .addOnSuccessListener {
            Log.d("Firestore", "Perfil guardado correctamente")
            onResult(true)
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error al guardar perfil", e)
            onResult(false)
        }
}

@Composable
fun EdadDropdown(onEdadSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedEdad by remember { mutableStateOf<Int?>(null) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(text = "Selecciona tu edad: ", color = Color.White)

        Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(boton)
        ) {
            Text(text = selectedEdad?.toString() ?: "15",
                modifier = Modifier
                    .background(boton_texto, shape = RoundedCornerShape(10.dp))
                    .height(30.dp)
                    .width(60.dp),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp)
                .height(250.dp)
                .background(boton_texto)) {
            for (edad in 15..60) { // Rango de edades permitidas
                DropdownMenuItem(
                    text = { Text(text = edad.toString()+ " años" ) },
                    onClick = {
                        selectedEdad = edad
                        onEdadSelected(edad) // Devuelve la edad seleccionada
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SexoDropDown(selectedSexo: String, onSexoSelected: (String) -> Unit) {
    val sexos = listOf("Masculino", "Femenino", "Otro")
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedSexo) }

    Box(modifier = Modifier
        .clickable { expanded = true }) {

        Text(text = selected, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .background(boton_texto, shape = RoundedCornerShape(10.dp))
                .height(40.dp)
                .width(100.dp)
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sexos.forEach { sexo ->
                DropdownMenuItem(text = { Text(sexo) }, onClick = {
                    selected = sexo
                    onSexoSelected(sexo)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun CiudadDropdown(onCiudadSelected: (String) -> Unit) {
    val ciudades = listOf("Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena") // Agrega más si quieres
    var expanded by remember { mutableStateOf(false) }
    var selectedCiudad by remember { mutableStateOf(ciudades[0]) }

    Box (modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .clickable { expanded = true }
        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
        .padding(12.dp)
    ) {
        if(selectedCiudad == ciudades[0]) Text(text = "seleccionar ciudad", color = Color.Gray)
        else Text(text = selectedCiudad, color = Color.White)

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ciudades.forEach { ciudad ->
                DropdownMenuItem(text = { Text(ciudad) }, onClick = {
                    selectedCiudad = ciudad
                    onCiudadSelected(ciudad)
                    expanded = false
                })
            }
        }
    }
}






