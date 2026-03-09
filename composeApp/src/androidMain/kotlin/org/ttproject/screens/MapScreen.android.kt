package org.ttproject.screens

import android.Manifest
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    userLocationTrigger: Int,
    onMarkerClick: (TTClub) -> Unit
) {
    val context = LocalContext.current

    // --- STATE ---
    var hasLocationPermission by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val budapest = LatLng(47.4979, 19.0402)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(budapest, 12f)
    }

    // --- PERMISSIONS ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // --- LIVE LOCATION TRACKING FOR CUSTOM MARKER ---
    DisposableEffect(hasLocationPermission) {
        val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)

        // Create the listener safely
        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                userLocation = LatLng(loc.latitude, loc.longitude)
            }
        }

        if (hasLocationPermission && locationManager != null) {
            try {
                // Request location updates every 2 seconds or 2 meters
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, locationListener)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 2f, locationListener)
            } catch (e: SecurityException) {
                // Ignore if location is restricted
            }
        }

        onDispose {
            // Stop tracking when the map screen is closed to save battery
            locationManager?.removeUpdates(locationListener)
        }
    }

    // --- ZOOM TO SELECTED CLUB ---
    LaunchedEffect(selectedClub) {
        if (selectedClub != null) {
            val targetLocation = LatLng(selectedClub.lat, selectedClub.lng)
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(targetLocation, 15f),
                durationMs = 800
            )
        }
    }

    // --- ZOOM TO ME ("Center Me" Button) ---
    LaunchedEffect(userLocationTrigger) {
        if (userLocationTrigger > 0 && hasLocationPermission) {
            try {
                val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)
                val lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastLocation?.let { loc ->
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f),
                        durationMs = 800
                    )
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    // --- THE MAP ---
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = PaddingValues(top = 72.dp, bottom = 90.dp),
        // Force the native Google blue dot OFF so we can use our custom one
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        )
    ) {

        // 1. DRAW CUSTOM USER LOCATION MARKER
        userLocation?.let { loc ->
            MarkerComposable(
                keys = arrayOf(loc), // Update when location changes
                state = MarkerState(position = loc),
                zIndex = 2f, // Ensure user stays on top of unselected clubs
                title = "Me"
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = Color(0xFF00D2FF), shape = CircleShape)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧍", fontSize = 18.sp)
                }
            }
        }

        // 2. DRAW CLUB MARKERS
        locations.forEach { club ->
            val isSelected = club.id == selectedClub?.id

            // Animations
            val animatedPinSize by animateDpAsState(
                targetValue = if (isSelected) 48.dp else 36.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "PinSize"
            )

            val animatedEmojiSize by animateFloatAsState(
                targetValue = if (isSelected) 22f else 16f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "EmojiSize"
            )

            MarkerComposable(
                keys = arrayOf(isSelected, animatedPinSize),
                state = MarkerState(position = LatLng(club.lat, club.lng)),
                title = club.id,
                // Push selected marker to the absolute front (zIndex 3 outranks the user's 2)
                zIndex = if (isSelected) 3f else 1f,
                onClick = {
                    onMarkerClick(club)
                    true
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(animatedPinSize)
                        .background(color = Color(0xFFFF7B42), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏓", fontSize = animatedEmojiSize.sp)
                }
            }
        }
    }
}