package com.santiago.sindesparches.presentation.map_picker

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    navController: androidx.navigation.NavController
) {
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val bogota = LatLng(4.60971, -74.08175)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 15f)
    }

    fun searchLocation() {
        focusManager.clearFocus()
        coroutineScope.launch {
            try {
                val geocoder = Geocoder(context)
                val addressList = geocoder.getFromLocationName(searchText, 1)
                if (addressList != null && addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                } else {
                    Toast.makeText(context, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error en la búsqueda. Revisa tu conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Ubicación", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F23)
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // El marcador se actualiza automáticamente con la posición de la cámara
            }
            // Marcador fijo en el centro de la pantalla
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Marcador",
                tint = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar dirección...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchLocation() })
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val target = cameraPositionState.position.target
                        val geocoder = Geocoder(context)
                        var addressText = "Ubicación sin nombre"
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(target.latitude, target.longitude, 1) { addresses ->
                                    if (addresses.isNotEmpty()) {
                                        addressText = addresses[0].getAddressLine(0)
                                    }
                                    // Set result and navigate back on the main thread
                                    val previousBackStackEntry = navController.previousBackStackEntry
                                    previousBackStackEntry?.savedStateHandle?.set("location_address", addressText)
                                    previousBackStackEntry?.savedStateHandle?.set("location_lat", target.latitude)
                                    previousBackStackEntry?.savedStateHandle?.set("location_lng", target.longitude)
                                    navController.popBackStack()
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1)
                                if (addresses != null && addresses.isNotEmpty()) {
                                    addressText = addresses[0].getAddressLine(0)
                                }
                                val previousBackStackEntry = navController.previousBackStackEntry
                                previousBackStackEntry?.savedStateHandle?.set("location_address", addressText)
                                previousBackStackEntry?.savedStateHandle?.set("location_lat", target.latitude)
                                previousBackStackEntry?.savedStateHandle?.set("location_lng", target.longitude)
                                navController.popBackStack()
                            }
                        } catch (e: IOException) {
                             val previousBackStackEntry = navController.previousBackStackEntry
                            previousBackStackEntry?.savedStateHandle?.set("location_address", addressText)
                            previousBackStackEntry?.savedStateHandle?.set("location_lat", target.latitude)
                            previousBackStackEntry?.savedStateHandle?.set("location_lng", target.longitude)
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Confirmar Ubicación")
            }
        }
    }
}
