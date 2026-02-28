package org.ttproject.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKUserTrackingButton
import platform.UIKit.NSLayoutConstraint
import platform.darwin.NSObject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    onMarkerClick: (TTClub) -> Unit
) {
    val currentLocations by rememberUpdatedState(locations)
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)

    // 1. Request iOS Location Permission
    val locationManager = remember { CLLocationManager() }
    LaunchedEffect(Unit) {
        locationManager.requestWhenInUseAuthorization()
    }

    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {

            // 1. Handle the Click (You already have this)
            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val tappedTitle = didSelectAnnotationView.annotation?.title
                val clickedClub = currentLocations.find { it.id == tappedTitle } // Using ID now!
                if (clickedClub != null) currentOnMarkerClick(clickedClub)
            }

            // 2. CUSTOMIZE THE PIN APPEARANCE
            override fun mapView(
                mapView: MKMapView,
                viewForAnnotation: platform.MapKit.MKAnnotationProtocol
            ): MKAnnotationView? {
                // If this is the blue "User Location" dot, leave it alone!
                if (viewForAnnotation is platform.MapKit.MKUserLocation) return null

                val identifier = "CustomOrangePin"
                var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier)
                        as? platform.MapKit.MKMarkerAnnotationView

                if (annotationView == null) {
                    // Create a new pin
                    annotationView = platform.MapKit.MKMarkerAnnotationView(
                        annotation = viewForAnnotation,
                        reuseIdentifier = identifier
                    )

                    // Hide the default Apple tooltip bubble
                    annotationView.canShowCallout = false

                    // Change the pin color to your Brand Orange (RGB: 255, 123, 66)
                    annotationView.markerTintColor = platform.UIKit.UIColor.colorWithRed(
                        red = 1.0, green = 0.482, blue = 0.259, alpha = 1.0
                    )

                    // Set a custom emoji or text inside the pin
                    annotationView.glyphText = "🏓"
                } else {
                    // Recycle an old pin as the user pans the map
                    annotationView.annotation = viewForAnnotation
                }

                return annotationView
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = MKMapView()
            mapView.delegate = mapDelegate

            // 2. Turn on the iOS Blue Dot
            mapView.showsUserLocation = true

            // 3. Add the native Apple "Center on Me" button
            val trackingButton = MKUserTrackingButton.userTrackingButtonWithMapView(mapView)
            trackingButton.translatesAutoresizingMaskIntoConstraints = false
            mapView.addSubview(trackingButton)

            // 4. Anchor the button to the bottom right (Offset by -100 to sit above our 76dp bottom sheet!)
            NSLayoutConstraint.activateConstraints(listOf(
                trackingButton.trailingAnchor.constraintEqualToAnchor(mapView.trailingAnchor, constant = -16.0),
                trackingButton.bottomAnchor.constraintEqualToAnchor(mapView.bottomAnchor, constant = -100.0)
            ))

            val budapestCoord = CLLocationCoordinate2DMake(47.4979, 19.0402)
            val region = MKCoordinateRegionMakeWithDistance(budapestCoord, 8000.0, 8000.0)
            mapView.setRegion(region, animated = false)

            mapView
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        ),
        update = { mapView ->
            val existingAnnotations = mapView.annotations
            if (existingAnnotations.isNotEmpty()) mapView.removeAnnotations(existingAnnotations)

            locations.forEach { club ->
                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(club.lat, club.lng))

                    // We removed setTitle() so Apple's native popup doesn't appear!
                    // We use the club's ID as the title silently so our Delegate can still find it
                    setTitle(club.id)
                }
                mapView.addAnnotation(annotation)
            }

            if (selectedClub != null) {
                // Find the Apple pin that matches the selected Compose club
                val annotationToSelect = mapView.annotations.firstOrNull {
                    (it as? MKAnnotationProtocol)?.title == selectedClub.id
                } as? MKAnnotationProtocol

                if (annotationToSelect != null) {
                    mapView.selectAnnotation(annotationToSelect, animated = true)
                    val centerCoord = CLLocationCoordinate2DMake(selectedClub.lat, selectedClub.lng)
                    mapView.setCenterCoordinate(centerCoord, animated = true)
                }
            } else {
                // If selectedClub is null (user closed the card), deselect everything natively
                mapView.selectedAnnotations.forEach {
                    mapView.deselectAnnotation(it as MKAnnotationProtocol, animated = true)
                }
            }
        }
    )
}