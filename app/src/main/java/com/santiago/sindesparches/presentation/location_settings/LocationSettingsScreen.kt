package com.santiago.sindesparches.presentation.location_settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
// Extension function for suspendCancellableCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine

data class LocationSuggestion(
    val displayName: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val state: String,
    val country: String
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingsScreen(
    navController: NavController,
    onLocationSelected: (LocationSuggestion) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var currentUserLocation by remember { mutableStateOf<LocationSuggestion?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // NUEVO: Estado para controlar la pantalla de carga al seleccionar ubicación
    var isSavingLocation by remember { mutableStateOf(false) }
    var selectedLocationName by remember { mutableStateOf("") }

    // Colors
    val vibrantPink = Color(0xFFEC4899)
    val electricBlue = Color(0xFF06B6D4)
    val nightBackground = Color(0xFF0A0E27)
    val cardBackground = Color(0xFF1A1D3A)

    // Function to save location to Firestore - MODIFICADA
    fun saveLocationToFirestore(location: LocationSuggestion) {
        if (isSavingLocation) return // Prevenir múltiples clics

        isSavingLocation = true
        selectedLocationName = location.displayName

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            isSavingLocation = false
            return
        }

        val db = FirebaseFirestore.getInstance()

        val locationData = mapOf(
            "city" to location.city,
            "state" to location.state,
            "country" to location.country,
            "displayName" to location.displayName,
            "fullAddress" to location.fullAddress,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("perfil").document(userId)
            .update("location", locationData)
            .addOnSuccessListener {
                // Simular un pequeño delay para mostrar la animación
                coroutineScope.launch {
                    kotlinx.coroutines.delay(1500) // 1.5 segundos de animación
                    Toast.makeText(context, "¡Ubicación guardada exitosamente!", Toast.LENGTH_SHORT).show()
                    onLocationSelected(location)
                    navController.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationSettings", "Error guardando ubicación", e)
                Toast.makeText(context, "Error guardando ubicación", Toast.LENGTH_SHORT).show()
                isSavingLocation = false
            }
    }

    // Function to detect current location - MODIFICADA para manejar el estado de carga
    fun detectCurrentLocation() {
        isDetectingLocation = true
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val geocoder = Geocoder(context)
                                        val addresses =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                suspendCancellableCoroutine { continuation ->
                                                    geocoder.getFromLocation(
                                                        location.latitude,
                                                        location.longitude,
                                                        1
                                                    ) { addressList ->
                                                        continuation.resume(addressList) {}
                                                    }
                                                }
                                            } else {
                                                @Suppress("DEPRECATION")
                                                geocoder.getFromLocation(
                                                    location.latitude,
                                                    location.longitude,
                                                    1
                                                )
                                            }

                                        if (addresses?.isNotEmpty() == true) {
                                            val address = addresses[0]
                                            val locationSuggestion = LocationSuggestion(
                                                displayName = address.locality
                                                    ?: address.subAdminArea ?: "Ubicación Actual",
                                                fullAddress = address.getAddressLine(0) ?: "",
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                city = address.locality ?: address.subAdminArea ?: "",
                                                state = address.adminArea ?: "",
                                                country = address.countryName ?: ""
                                            )

                                            withContext(Dispatchers.Main) {
                                                currentUserLocation = locationSuggestion
                                                searchText = locationSuggestion.displayName
                                                isDetectingLocation = false
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo obtener la dirección",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                isDetectingLocation = false
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("LocationSettings", "Error obteniendo ubicación actual", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Error obteniendo ubicación",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isDetectingLocation = false
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "No se pudo obtener la ubicación",
                                Toast.LENGTH_SHORT
                            ).show()
                            isDetectingLocation = false
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationSettings", "Error obteniendo ubicación", e)
                        Toast.makeText(context, "Error obteniendo ubicación", Toast.LENGTH_SHORT).show()
                        isDetectingLocation = false
                    }
            }
        } catch (e: Exception) {
            Log.e("LocationSettings", "Error en detectCurrentLocation", e)
            isDetectingLocation = false
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            detectCurrentLocation()
        } else {
            Toast.makeText(context, "Se necesitan permisos de ubicación", Toast.LENGTH_LONG).show()
        }
    }

    // Function to search locations - Sin cambios significativos
    fun searchLocations() {
        if (searchText.isBlank()) return

        isSearching = true
        focusManager.clearFocus()

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context)
                    val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(
                                "$searchText, Colombia",
                                5
                            ) { addressList ->
                                continuation.resume(addressList) {}
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName("$searchText, Colombia", 5)
                    }

                    val locationSuggestions = addresses?.mapNotNull { address ->
                        try {
                            LocationSuggestion(
                                displayName = address.locality ?: address.subAdminArea
                                ?: address.adminArea ?: "Ubicación",
                                fullAddress = address.getAddressLine(0) ?: "",
                                latitude = address.latitude,
                                longitude = address.longitude,
                                city = address.locality ?: address.subAdminArea ?: "",
                                state = address.adminArea ?: "",
                                country = address.countryName ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }?.distinctBy { it.city } ?: emptyList()

                    withContext(Dispatchers.Main) {
                        suggestions = locationSuggestions
                        isSearching = false
                    }
                }
            } catch (e: IOException) {
                Log.e("LocationSettings", "Error en búsqueda de ubicación", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error de conexión. Verifica tu internet.",
                        Toast.LENGTH_SHORT
                    ).show()
                    isSearching = false
                }
            } catch (e: Exception) {
                Log.e("LocationSettings", "Error general en búsqueda", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                    isSearching = false
                }
            }
        }
    }

    // NUEVO: Overlay de carga cuando se está guardando la ubicación
    if (isSavingLocation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(enabled = false) { }, // Bloquea interacciones
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Círculo con gradiente para la animación
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(vibrantPink, electricBlue),
                                    radius = 120f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¡Configurando tu ubicación!",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Guardando $selectedLocationName...",
                        color = electricBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Por favor espera un momento",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configurar Ubicación",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isSavingLocation) { // Solo permitir navegación si no se está guardando
                                navController.popBackStack()
                            }
                        },
                        enabled = !isSavingLocation // Deshabilitar botón durante la carga
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isSavingLocation) Color.Gray else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = nightBackground
                )
            )
        },
        containerColor = nightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Instruction text
            Text(
                text = "Selecciona tu ciudad para ver eventos cercanos a ti",
                color = Color.White.copy(alpha = if (isSavingLocation) 0.5f else 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Search field - MODIFICADO para deshabilitar durante la carga
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSavingLocation) cardBackground.copy(alpha = 0.5f) else cardBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        if (!isSavingLocation) { // Solo permitir cambios si no se está guardando
                            searchText = it
                            if (it.isNotEmpty()) {
                                suggestions = emptyList()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar ciudad (ej. Bogotá, Medellín...)",
                            color = if (isSavingLocation) Color.Gray.copy(alpha = 0.5f) else Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = if (isSavingLocation) electricBlue.copy(alpha = 0.5f) else electricBlue
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (searchText.isNotEmpty() && !isSavingLocation) {
                                IconButton(onClick = {
                                    searchText = ""
                                    suggestions = emptyList()
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isSavingLocation, // Deshabilitar durante la carga
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (!isSavingLocation) searchLocations()
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = if (isSavingLocation) Color.White.copy(alpha = 0.5f) else Color.White,
                        unfocusedTextColor = if (isSavingLocation) Color.White.copy(alpha = 0.5f) else Color.White,
                        cursorColor = electricBlue,
                        focusedIndicatorColor = electricBlue,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledTextColor = Color.White.copy(alpha = 0.5f),
                        disabledIndicatorColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons - MODIFICADOS para deshabilitar durante la carga
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search button
                Button(
                    onClick = { searchLocations() },
                    modifier = Modifier.weight(1f),
                    enabled = searchText.isNotEmpty() && !isSearching && !isSavingLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = electricBlue,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSearching) "Buscando..." else "Buscar")
                }

                // Current location button
                Button(
                    onClick = {
                        val hasLocationPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasLocationPermission) {
                            detectCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isDetectingLocation && !isSavingLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = vibrantPink,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    if (isDetectingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDetectingLocation) "Detectando..." else "Mi Ubicación")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current location display - MODIFICADO para manejar el estado de carga
            currentUserLocation?.let { location ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSavingLocation) {
                            saveLocationToFirestore(location)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSavingLocation) cardBackground.copy(alpha = 0.5f) else cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSavingLocation) vibrantPink.copy(alpha = 0.5f) else vibrantPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tu ubicación actual",
                                color = if (isSavingLocation) vibrantPink.copy(alpha = 0.5f) else vibrantPink,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = location.displayName,
                                color = if (isSavingLocation) Color.White.copy(alpha = 0.5f) else Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = location.fullAddress,
                                color = if (isSavingLocation) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = if (isSavingLocation) "Guardando..." else "Seleccionar",
                            color = if (isSavingLocation) electricBlue.copy(alpha = 0.5f) else electricBlue,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Suggestions list - MODIFICADO para manejar el estado de carga
            if (suggestions.isNotEmpty()) {
                Text(
                    text = "Resultados de búsqueda:",
                    color = if (isSavingLocation) Color.White.copy(alpha = 0.5f) else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSavingLocation) {
                                    saveLocationToFirestore(suggestion)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSavingLocation) cardBackground.copy(alpha = 0.5f) else cardBackground
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSavingLocation) electricBlue.copy(alpha = 0.5f) else electricBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.displayName,
                                        color = if (isSavingLocation) Color.White.copy(alpha = 0.5f) else Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = suggestion.fullAddress,
                                        color = if (isSavingLocation) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty state - Sin cambios significativos en funcionalidad
            if (searchText.isNotEmpty() && suggestions.isEmpty() && !isSearching) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSavingLocation) cardBackground.copy(alpha = 0.5f) else cardBackground
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSavingLocation) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No se encontraron ciudades",
                            color = if (isSavingLocation) Color.White.copy(alpha = 0.5f) else Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Intenta con otro término de búsqueda",
                            color = if (isSavingLocation) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
