package org.ttproject.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    onMarkerClick: (TTClub) -> Unit
) {
    // Center map on Budapest
    val budapest = LatLng(47.4979, 19.0402)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(budapest, 12f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // Disable default map UI clutter to look cleaner
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        locations.forEach { club ->
            Marker(
                state = MarkerState(position = LatLng(club.lat, club.lng)),
                title = club.name,
                onClick = {
                    onMarkerClick(club)
                    // Return true so the map doesn't auto-center or show default pop-ups.
                    // We want our Bottom Sheet to handle the UI!
                    true
                }
            )
        }
    }
}