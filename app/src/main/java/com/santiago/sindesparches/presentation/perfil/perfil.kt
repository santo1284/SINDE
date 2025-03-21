package com.santiago.sindesparches.presentation.perfil

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.placeholder
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.black
import com.santiago.sindesparches.ui.theme.boton
import com.santiago.sindesparches.ui.theme.boton_iniciar
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.white




@Composable

fun PerfilScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onLogout: () -> Unit = {},
    navigate_registro_completo: (nombre: String) -> Unit = {}
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var nombre by remember { mutableStateOf("") }
    var celular by remember { mutableStateOf("") }

    var mensaje by remember { mutableStateOf("") }

    var imageUrl by remember { mutableStateOf<String?>(null) }

    var selectedSexo by remember { mutableStateOf("Genero") }
    var edad by remember { mutableStateOf<Int?>(null) }

    var selectedCiudad by remember { mutableStateOf("seleccionar ciudad") }
    val userId = auth.currentUser?.uid

    // Selector de imagen                                                          funcion lambda
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }
    var showDialog by remember { mutableStateOf(false) }

    // Intercepta el botón de retroceso
    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("¿Estás seguro de que quieres salir? se eliminara tu usuario") },
            confirmButton = {
                Button(onClick = {
                    val user = FirebaseAuth.getInstance().currentUser

                    user?.delete()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FirebaseAuth", "Usuario eliminado correctamente")
                            } else {
                                Log.e("FirebaseAuth", "Error al eliminar usuario", task.exception)
                            }
                        }
                    auth.signOut() // Cierra la sesión
                    onLogout() // Navega a la pantalla de login
                }) {
                    Text("Sí, salir")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(color = boton_texto)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier
                        .size(250.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.bxs_camera_plus),
                    contentDescription = "Placeholder",
                    modifier = Modifier.size(100.dp),
                    tint = boton_iniciar
                )
            }
            Button(onClick = { launcher.launch("image/*") },
                modifier = Modifier .size(150.dp),colors = ButtonDefaults.buttonColors(boton)) {
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("REPORTE", "Usuario no autenticado")
            return
        }


        Spacer(modifier = Modifier.height(16.dp))

        imageUrl?.let {
            Text("Imagen subida con éxito")
        }
        // Campo de texto para ingresar el nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            singleLine = true,
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            placeholder = { Text("nombre de usuario", color = Color.Black) },
               colors = TextFieldDefaults.colors(
                focusedContainerColor = boton_texto,
                unfocusedContainerColor = boton,
                unfocusedIndicatorColor = Color.White,
                focusedIndicatorColor = boton,
                focusedTextColor = Color.White,
                unfocusedTextColor = white,
                cursorColor = white,
            ), shape = RoundedCornerShape(40.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        //campo para numero celular

        OutlinedTextField(
            value= celular,
            onValueChange = { celular = it },
            singleLine = true,
            textStyle = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                textAlign = if (celular.isEmpty()) TextAlign.Start else TextAlign.Center
            ),
            placeholder = {Text("numero celular",color = Color.Black )},
            colors = TextFieldDefaults.colors(
                focusedContainerColor = boton_texto,
                unfocusedContainerColor = boton,
                unfocusedIndicatorColor = Color.White,
                focusedIndicatorColor = boton,
                focusedTextColor = Color.White,
                unfocusedTextColor = white,
                cursorColor = white,
            ), shape = RoundedCornerShape(40.dp)
        )




        Spacer(modifier = Modifier.height(16.dp))

        //ingresa la edad
        EdadDropdown { nuevaEdad ->
            edad = nuevaEdad
        }

        Spacer(modifier = Modifier.height(16.dp))

        //seleccionar ciudad donde se encuentra

        CiudadDropdown(selectedCiudad) { nuevaCiudad ->
            selectedCiudad = nuevaCiudad
        }

        // Botón para guardar en Firestore y la imagen
        Button(onClick = {

            if (imageUri == null) {
                mensaje = "Por favor selecciona una imagen"
                return@Button
            }

            if (nombre.isEmpty()) {
                mensaje = "Por favor ingresa un nombre"
                return@Button
            }
            if (celular.isEmpty()){
                mensaje = "Por favor ingresa un numero de celular"
                return@Button
            }
            if (celular.length != 10){
                mensaje = "Por favor ingresa un numero de celular valido"
                return@Button
            }

            if (edad == null) {
                mensaje = "porfavor selecciona una edad"
                return@Button
            }

            if (selectedCiudad == "seleccionar ciudad") {
                mensaje = "Por favor selecciona una ciudad"
                return@Button
            }

            imageUri?.let { uri ->
                userId.let { uid ->
                    uploadImageToFirebase(uri, uid) { url ->
                        imageUrl = url
                    }
                } ?: Log.e("Upload", "Usuario no autenticado")
            } ?: Log.e("Upload", "No se ha seleccionado una imagen")

            guardarPerfilEnFirestore(
                userId,
                nombre,
                celular,
                edad!!,
                selectedCiudad
            ) { success ->
                if (success) {
                    mensaje = "Perfil guardado correctamente"
                    navigate_registro_completo(nombre)

                } else mensaje = "Error al guardar el perfil"
            }

        }, enabled = nombre.isNotEmpty()) { // Deshabilita el botón si no se registra un nombre
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

fun guardarPerfilEnFirestore(
    userId: String?,
    nombre: String,
    celular: String,
    edad: Int,
    ciudad: String,
    onResult: (Boolean) -> Unit
) {
    if (userId == null) {
        Log.e("Firestore", "Usuario no autenticado")
        onResult(false)
        return
    }

    val db = FirebaseFirestore.getInstance()
    val perfilData = hashMapOf(
        "nombre" to nombre,
        "celular" to celular,
        "edad" to edad,
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

//funcion para seleccionar la edad
fun EdadDropdown(onEdadSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedEdad by remember { mutableStateOf<Int?>(null) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(text = "Selecciona tu edad: ", color = Color.White)

        Button(
            onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(boton),
            modifier = Modifier
                .border(1.dp, if (selectedEdad != null) Color.White else Color.Gray, RoundedCornerShape(20.dp))
                .height(40.dp)
                .width(100.dp)
        ) {
            Text(
                text = selectedEdad?.toString() ?: "edad",
                color = if (selectedEdad != null) Color.White else Color.Gray,
                modifier = Modifier
                    .background(boton),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp)
                .height(250.dp)
                .background(boton_texto),
        ) {
            for (edad in 15..60) { // Rango de edades permitidas
                DropdownMenuItem(
                    text = { Text(text = edad.toString()) },
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

//funcion para seleccionar la ciudad
fun CiudadDropdown(selectedCiudad: String, onCiudadSelected: (String) -> Unit) {
    val ciudades = listOf("Garzon", "Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena")
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedCiudad) }

    Box(modifier = Modifier
        .padding(16.dp)
        .clickable { expanded = true }
        .border(1.dp, if (selectedCiudad == "seleccionar ciudad") Color.Gray else Color.White, RoundedCornerShape(8.dp))
        .height(40.dp)
        .width(200.dp)
        .padding(start = 8.dp, top = 8.dp)
    ) {
        Text(
            text = selectedCiudad,
            color = if (selectedCiudad == "seleccionar ciudad") Color.Gray else Color.White,
            fontSize = 20.sp,
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ciudades.forEach { ciudad ->
                DropdownMenuItem(text = { Text(ciudad) }, onClick = {
                    selected = ciudad
                    onCiudadSelected(ciudad)
                    expanded = false
                })
            }
        }
    }
}






