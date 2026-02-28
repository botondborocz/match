package org.ttproject.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    onMarkerClick: (TTClub) -> Unit
) {
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val budapest = LatLng(47.4979, 19.0402)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(budapest, 12f)
    }

    // NEW: When a club is selected, smoothly animate the camera to center it!
    LaunchedEffect(selectedClub) {
        if (selectedClub != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(LatLng(selectedClub.lat, selectedClub.lng)),
                durationMs = 400 // Smooth 400ms pan
            )
        }
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
            val isSelected = club.id == selectedClub?.id

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

            // Instantly swap properties (No continuous animation states)
            val pinSize = if (isSelected) 48.dp else 36.dp
            val pinColor = Color(0xFFFF7B42)
            val emojiSize = if (isSelected) 22.sp else 16.sp

            MarkerComposable(
                keys = arrayOf(isSelected, animatedPinSize),
                state = MarkerState(position = LatLng(club.lat, club.lng)),
                title = club.id,
                // NEW: Push the selected marker to the front so it overlaps others
                zIndex = if (isSelected) 1f else 0f,
                onClick = {
                    onMarkerClick(club)
                    true
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(animatedPinSize)
                        .background(color = pinColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏓", fontSize = animatedEmojiSize.sp)
                }
            }
        }
    }
}