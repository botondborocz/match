package org.ttproject.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKCoordinateRegionMakeWithDistance

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    onMarkerClick: (TTClub) -> Unit
) {
    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = MKMapView()

            // Set initial camera position to Budapest
            val budapestCoord = CLLocationCoordinate2DMake(47.4979, 19.0402)
            val region = MKCoordinateRegionMakeWithDistance(budapestCoord, 8000.0, 8000.0)
            mapView.setRegion(region, animated = false)

            mapView
        },
        properties = UIKitInteropProperties(
            // CRITICAL: This allows the user to pan and zoom the Apple Map.
            // Without this, Compose steals all the touch events!
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        ),
        update = { mapView ->
            // Clear old markers safely
            val existingAnnotations = mapView.annotations
            if (existingAnnotations.isNotEmpty()) {
                mapView.removeAnnotations(existingAnnotations)
            }

            // Plot all the clubs
            locations.forEach { club ->
                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(club.lat, club.lng))
                    setTitle(club.name)
                }
                mapView.addAnnotation(annotation)
            }
        }
    )
}