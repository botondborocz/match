package org.ttproject.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    onMarkerClick: (TTClub) -> Unit
) {
    // 1. Set up Permission Request
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // 2. Ask for permission as soon as the map loads
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val budapest = LatLng(47.4979, 19.0402)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(budapest, 12f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = hasLocationPermission,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        locations.forEach { club ->
            // Use MarkerComposable instead of Marker!
            MarkerComposable(
                state = MarkerState(position = LatLng(club.lat, club.lng)),
                title = club.id, // Keep this as ID for logic purposes
                onClick = {
                    onMarkerClick(club)
                    true
                }
            ) {
                // Draw your completely custom Compose pin here!
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF7B42), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    // You can put an Icon or Text in here
                    androidx.compose.material3.Text("🏓", fontSize = 16.sp)
                }
            }
        }
    }
}