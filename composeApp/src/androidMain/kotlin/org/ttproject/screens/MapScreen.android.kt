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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    userLocationTrigger: Int,
    bottomPadding: Dp, // 👈 Added Bottom Padding
    isDark: Boolean,   // 👈 Added Theme State
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

    // --- LIVE LOCATION TRACKING ---
    DisposableEffect(hasLocationPermission) {
        val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                userLocation = LatLng(loc.latitude, loc.longitude)
            }
        }

        if (hasLocationPermission && locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, locationListener)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 2f, locationListener)
            } catch (e: SecurityException) {
                // Ignore if location is restricted
            }
        }

        onDispose {
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

    // --- GOOGLE MAPS DARK MODE JSON ---
    val darkMapStyle = """
        [
          {"elementType": "geometry", "stylers": [{"color": "#242f3e"}]},
          {"elementType": "labels.text.fill", "stylers": [{"color": "#746855"}]},
          {"elementType": "labels.text.stroke", "stylers": [{"color": "#242f3e"}]},
          {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
          {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
          {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#263c3f"}]},
          {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#6b9a76"}]},
          {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#38414e"}]},
          {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#212a37"}]},
          {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b3"}]},
          {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#746855"}]},
          {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#1f2835"}]},
          {"featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{"color": "#f3d19c"}]},
          {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#17263c"}]},
          {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#515c6d"}]},
          {"featureType": "water", "elementType": "labels.text.stroke", "stylers": [{"color": "#17263c"}]}
        ]
    """.trimIndent()

    // --- THE MAP ---
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // 👇 Dynamic Bottom Padding applied here!
        contentPadding = PaddingValues(top = 72.dp, bottom = bottomPadding),
        properties = MapProperties(
            isMyLocationEnabled = false,
            // 👇 Apply Dark Mode JSON if app is in dark mode
            mapStyleOptions = if (isDark) MapStyleOptions(darkMapStyle) else null
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        )
    ) {

        // 1. DRAW CUSTOM USER LOCATION MARKER
        userLocation?.let { loc ->
            MarkerComposable(
                keys = arrayOf(loc),
                state = MarkerState(position = loc),
                zIndex = 2f,
                title = "Me"
            ) {
                // Outer Box acts as the touch target and the soft translucent "halo"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = Color(0xFF00D2FF).copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Box is the crisp, physical dot
                    Box(
                        modifier = Modifier
                            .size(22.dp) // The size of the actual dot
                            .shadow(elevation = 4.dp, shape = CircleShape) // Drop shadow so it pops over roads
                            .background(color = Color(0xFF00D2FF), shape = CircleShape)
                            .border(width = 3.dp, color = Color.White, shape = CircleShape)
                    )
                }
            }
        }

        // 2. DRAW CLUB MARKERS
        locations.forEach { club ->
            val isSelected = club.id == selectedClub?.id

            val animatedPinSize by animateDpAsState(
                targetValue = if (isSelected) 48.dp else 36.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "PinSize"
            )

            val animatedEmojiSize by animateFloatAsState(
                targetValue = if (isSelected) 22f else 16f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "EmojiSize"
            )

            MarkerComposable(
                keys = arrayOf(isSelected, animatedPinSize),
                state = MarkerState(position = LatLng(club.lat, club.lng)),
                title = club.id,
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