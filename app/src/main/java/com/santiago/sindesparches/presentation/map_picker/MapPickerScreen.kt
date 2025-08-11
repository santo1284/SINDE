package com.santiago.sindesparches.presentation.map_picker

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var isMapLoaded by remember { mutableStateOf(false) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    var currentMarkerPosition by remember { mutableStateOf<LatLng?>(null) }
    var isConfirming by remember { mutableStateOf(false) }

    // Bogotá como ubicación inicial
    val bogota = LatLng(4.60971, -74.08175)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 12f)
    }

    // Función de búsqueda mejorada
    // Función de búsqueda mejorada y corregida
    fun searchLocation() {
        Log.d("MapPickerScreen", "🔍 Iniciando búsqueda para: '$searchText'")

        if (searchText.isBlank()) {
            Toast.makeText(context, "Ingresa una dirección para buscar", Toast.LENGTH_SHORT).show()
            Log.d("MapPickerScreen", "❌ Búsqueda cancelada: texto vacío")
            return
        }

        focusManager.clearFocus()
        Log.d("MapPickerScreen", "🚀 Ejecutando búsqueda geocoding...")

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context)
                    Log.d("MapPickerScreen", "📍 Geocoder creado, buscando: '$searchText'")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Log.d("MapPickerScreen", "📱 Usando API nueva (TIRAMISU+)")

                        // Para Android 13+
                        geocoder.getFromLocationName(searchText, 5) { addresses ->
                            Log.d("MapPickerScreen", "📋 Resultados recibidos: ${addresses.size} direcciones")

                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val latLng = LatLng(address.latitude, address.longitude)

                                Log.d("MapPickerScreen", "✅ Dirección encontrada:")
                                Log.d("MapPickerScreen", "   Lat: ${address.latitude}")
                                Log.d("MapPickerScreen", "   Lng: ${address.longitude}")
                                Log.d("MapPickerScreen", "   Address: ${address.getAddressLine(0)}")

                                coroutineScope.launch {
                                    try {
                                        Log.d("MapPickerScreen", "🎯 Moviendo cámara a la ubicación...")
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                                            durationMs = 1000
                                        )
                                        currentMarkerPosition = latLng
                                        Log.d("MapPickerScreen", "✅ Cámara movida exitosamente")

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Ubicación encontrada", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MapPickerScreen", "❌ Error moviendo cámara: ${e.message}")
                                    }
                                }
                            } else {
                                Log.d("MapPickerScreen", "❌ No se encontraron resultados")
                                coroutineScope.launch {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d("MapPickerScreen", "📱 Usando API legacy (pre-TIRAMISU)")

                        // Para Android 12 y anteriores
                        try {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocationName(searchText, 5)

                            Log.d("MapPickerScreen", "📋 Resultados recibidos: ${addresses?.size ?: 0} direcciones")

                            withContext(Dispatchers.Main) {
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val latLng = LatLng(address.latitude, address.longitude)

                                    Log.d("MapPickerScreen", "✅ Dirección encontrada:")
                                    Log.d("MapPickerScreen", "   Lat: ${address.latitude}")
                                    Log.d("MapPickerScreen", "   Lng: ${address.longitude}")
                                    Log.d("MapPickerScreen", "   Address: ${address.getAddressLine(0)}")

                                    coroutineScope.launch {
                                        try {
                                            Log.d("MapPickerScreen", "🎯 Moviendo cámara a la ubicación...")
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                                                durationMs = 1000
                                            )
                                            currentMarkerPosition = latLng
                                            Log.d("MapPickerScreen", "✅ Cámara movida exitosamente")
                                            Toast.makeText(context, "Ubicación encontrada", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.e("MapPickerScreen", "❌ Error moviendo cámara: ${e.message}")
                                        }
                                    }
                                } else {
                                    Log.d("MapPickerScreen", "❌ No se encontraron resultados")
                                    Toast.makeText(context, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: IOException) {
                            Log.e("MapPickerScreen", "❌ Error de red en geocoding: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error de conexión. Verifica tu internet.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MapPickerScreen", "❌ Error general en búsqueda: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error en la búsqueda: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función mejorada para confirmar ubicación
    // En tu función confirmLocation() en MapPickerScreen, reemplaza con esto:

    fun confirmLocation() {
        if (!isMapLoaded || isConfirming) {
            Toast.makeText(context, "Espera a que cargue el mapa", Toast.LENGTH_SHORT).show()
            return
        }

        isConfirming = true
        val target = cameraPositionState.position.target
        Log.d("MapPickerScreen", "🎯 Confirmando ubicación: $target")

        // Intentar geocoding de forma síncrona y simple
        coroutineScope.launch {
            try {
                var addressText = "Ubicación seleccionada"

                // Intentar obtener dirección, pero no bloquear si falla
                try {
                    withContext(Dispatchers.IO) {
                        val geocoder = Geocoder(context)
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            addressText = addresses[0].getAddressLine(0) ?:
                                    "Lat: ${String.format("%.4f", target.latitude)}, Lng: ${String.format("%.4f", target.longitude)}"
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MapPickerScreen", "Geocoding failed, using coordinates: ${e.message}")
                    addressText = "Lat: ${String.format("%.4f", target.latitude)}, Lng: ${String.format("%.4f", target.longitude)}"
                }

                // Guardar inmediatamente en el hilo principal
                withContext(Dispatchers.Main) {
                    try {
                        val previousBackStackEntry = navController.previousBackStackEntry
                        Log.d("MapPickerScreen", "📱 Previous entry: ${previousBackStackEntry?.destination?.route}")

                        previousBackStackEntry?.savedStateHandle?.apply {
                            Log.d("MapPickerScreen", "💾 Guardando datos:")
                            Log.d("MapPickerScreen", "   Address: $addressText")
                            Log.d("MapPickerScreen", "   Lat: ${target.latitude}")
                            Log.d("MapPickerScreen", "   Lng: ${target.longitude}")

                            set("location_address", addressText)
                            set("location_lat", target.latitude)
                            set("location_lng", target.longitude)

                            // Verificar que se guardaron
                            Log.d("MapPickerScreen", "✅ Verificación guardado:")
                            Log.d("MapPickerScreen", "   Address guardado: ${get<String>("location_address")}")
                            Log.d("MapPickerScreen", "   Lat guardado: ${get<Double>("location_lat")}")
                            Log.d("MapPickerScreen", "   Lng guardado: ${get<Double>("location_lng")}")
                        }

                        Toast.makeText(context, "Ubicación guardada", Toast.LENGTH_SHORT).show()

                        Log.d("MapPickerScreen", "🔙 Navegando de vuelta...")
                        navController.popBackStack()

                    } catch (navError: Exception) {
                        Log.e("MapPickerScreen", "❌ Error navegando: ${navError.message}")
                        isConfirming = false
                        Toast.makeText(context, "Error guardando ubicación", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MapPickerScreen", "❌ Error general: ${e.message}")
                isConfirming = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Detectar cuando se cancela la composición
    DisposableEffect(Unit) {
        onDispose {
            Log.d("MapPickerScreen", "MapPickerScreen disposed")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Seleccionar Ubicación",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isConfirming) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F23)
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Mostrar error si hay problemas con el mapa
            if (mapLoadError != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.9f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error al cargar el mapa",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mapLoadError!!,
                            color = Color.White
                        )
                    }
                }
            }

            // Google Map con mejor manejo de errores
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = false,
                    isTrafficEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    scrollGesturesEnabled = true,
                    zoomGesturesEnabled = true,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                ),
                onMapLoaded = {
                    Log.d("MapPickerScreen", "Mapa cargado exitosamente")
                    isMapLoaded = true
                    currentMarkerPosition = cameraPositionState.position.target
                },
                onMapClick = { latLng ->
                    if (!isConfirming) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                                durationMs = 500
                            )
                            // Actualizar inmediatamente la posición del marcador
                            currentMarkerPosition = latLng
                        }
                    }
                }
            ) {
                // Marcador en la posición actual
                currentMarkerPosition?.let { position ->
                    Marker(
                        state = MarkerState(position = position),
                        title = "Ubicación seleccionada",
                        snippet = "Lat: ${String.format("%.4f", position.latitude)}, Lng: ${String.format("%.4f", position.longitude)}"
                    )
                }
            }
            LaunchedEffect(cameraPositionState.isMoving) {
                if (isMapLoaded && !isConfirming && !cameraPositionState.isMoving) {
                    // Solo actualizar cuando el mapa deje de moverse
                    currentMarkerPosition = cameraPositionState.position.target
                }
            }

            // Marcador fijo en el centro
            if (isMapLoaded) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Marcador central",
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                )
            }

            // Barra de búsqueda
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { if (!isConfirming) searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar dirección en Bogotá...",
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = { if (!isConfirming) searchLocation() },
                            enabled = !isConfirming
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = if (isConfirming) Color.Gray else Color(0xFF6C5CE7)
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty() && !isConfirming) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (!isConfirming) searchLocation() }
                    ),
                    enabled = !isConfirming,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF6C5CE7),
                        focusedIndicatorColor = Color(0xFF6C5CE7),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            // Indicador de carga
            if (!isMapLoaded) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6C5CE7).copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cargando mapa...",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Indicador de confirmación
            if (isConfirming) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Guardando ubicación...",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Botón de confirmación mejorado
            Button(
                onClick = { confirmLocation() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfirming) Color.Gray else Color(0xFF6C5CE7),
                    disabledContainerColor = Color.Gray
                ),
                enabled = isMapLoaded && !isConfirming
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isConfirming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when {
                            isConfirming -> "Guardando..."
                            isMapLoaded -> "Confirmar Ubicación"
                            else -> "Cargando..."
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}