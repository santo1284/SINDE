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

    // Colors
    val vibrantPink = Color(0xFFEC4899)
    val electricBlue = Color(0xFF06B6D4)
    val nightBackground = Color(0xFF0A0E27)
    val cardBackground = Color(0xFF1A1D3A)
    // Function to detect current location
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
                                                city = address.locality ?: address.subAdminArea
                                                ?: "",
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
                                    Log.e(
                                        "LocationSettings",
                                        "Error obteniendo ubicación actual",
                                        e
                                    )
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
                        Toast.makeText(context, "Error obteniendo ubicación", Toast.LENGTH_SHORT)
                            .show()
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

    // Function to search locations
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

    // Function to save location to Firestore
    fun saveLocationToFirestore(location: LocationSuggestion) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
                Toast.makeText(context, "Ubicación guardada exitosamente", Toast.LENGTH_SHORT)
                    .show()
                onLocationSelected(location)
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                Log.e("LocationSettings", "Error guardando ubicación", e)
                Toast.makeText(context, "Error guardando ubicación", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
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
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Search field
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        if (it.isNotEmpty()) {
                            suggestions = emptyList() // Clear suggestions when typing
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar ciudad (ej. Bogotá, Medellín...)",
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = electricBlue
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (searchText.isNotEmpty()) {
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchLocations() }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = electricBlue,
                        focusedIndicatorColor = electricBlue,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search button
                Button(
                    onClick = { searchLocations() },
                    modifier = Modifier.weight(1f),
                    enabled = searchText.isNotEmpty() && !isSearching,
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
                    enabled = !isDetectingLocation,
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

            // Current location display
            currentUserLocation?.let { location ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveLocationToFirestore(location) },
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
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
                            tint = vibrantPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tu ubicación actual",
                                color = vibrantPink,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = location.displayName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = location.fullAddress,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Seleccionar",
                            color = electricBlue,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Suggestions list
            if (suggestions.isNotEmpty()) {
                Text(
                    text = "Resultados de búsqueda:",
                    color = Color.White,
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
                                .clickable { saveLocationToFirestore(suggestion) },
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
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
                                    tint = electricBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.displayName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = suggestion.fullAddress,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty state
            if (searchText.isNotEmpty() && suggestions.isEmpty() && !isSearching) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground)
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
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No se encontraron ciudades",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Intenta con otro término de búsqueda",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


