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
import platform.MapKit.MKUserLocation
import platform.MapKit.MKMarkerAnnotationView
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIImageSymbolWeightBold
import platform.darwin.NSObject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    userLocationTrigger: Int,
    onMarkerClick: (TTClub) -> Unit
) {
    val currentLocations by rememberUpdatedState(locations)
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)

    var lastHandledTrigger by remember { mutableStateOf(0) }

    // 1. Request iOS Location Permission
    val locationManager = remember { CLLocationManager() }
    LaunchedEffect(Unit) {
        locationManager.requestWhenInUseAuthorization()
    }

    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {

            // 1. Handle the Click
            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val tappedTitle = didSelectAnnotationView.annotation?.title
                val clickedClub = currentLocations.find { it.id == tappedTitle }
                if (clickedClub != null) currentOnMarkerClick(clickedClub)
            }

            // 2. CUSTOMIZE THE PIN APPEARANCE
            override fun mapView(
                mapView: MKMapView,
                viewForAnnotation: MKAnnotationProtocol
            ): MKAnnotationView? {

                // 👇 A. CUSTOM USER LOCATION MARKER
                if (viewForAnnotation is MKUserLocation) {
                    val userIdentifier = "CustomUserLocation"
                    var userView = mapView.dequeueReusableAnnotationViewWithIdentifier(userIdentifier)

                    if (userView == null) {
                        userView = MKAnnotationView(
                            annotation = viewForAnnotation,
                            reuseIdentifier = userIdentifier
                        )
                    } else {
                        userView.annotation = viewForAnnotation
                    }

                    // Use the built-in Apple Table Tennis SF Symbol!
                    val config = UIImageSymbolConfiguration.configurationWithPointSize(28.0, UIImageSymbolWeightBold)
                    userView.image = UIImage.systemImageNamed("figure.table.tennis", withConfiguration = config)

                    // Tint it to match your AccentCyan (Color(0xFF00D2FF))
                    userView.tintColor = UIColor.colorWithRed(0.0, 0.824, 1.0, 1.0)

                    return userView
                }

                // 👇 B. CUSTOM CLUB PINS
                val identifier = "CustomOrangePin"
                var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier)
                        as? MKMarkerAnnotationView

                if (annotationView == null) {
                    annotationView = MKMarkerAnnotationView(
                        annotation = viewForAnnotation,
                        reuseIdentifier = identifier
                    )
                    annotationView.canShowCallout = false
                } else {
                    annotationView.annotation = viewForAnnotation
                }

                // Apply styling to the club pins (AccentOrange)
                annotationView.markerTintColor = UIColor.colorWithRed(1.0, 0.482, 0.259, 1.0)
                annotationView.glyphText = "🏓"

                return annotationView
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = MKMapView()
            mapView.delegate = mapDelegate

            // Turn on the iOS Blue Dot (so our custom delegate can intercept and restyle it!)
            mapView.showsUserLocation = true

            val budapestCoord = CLLocationCoordinate2DMake(47.4979, 19.0402)
            val region = MKCoordinateRegionMakeWithDistance(budapestCoord, 8000.0, 8000.0)
            mapView.setRegion(region, animated = false)

            mapView
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        ),
        update = { mapView ->
            // 1. PLOT MARKERS (Filter out the MKUserLocation so we don't accidentally delete our ping pong player!)
            val existingCustomPins = mapView.annotations.filter { it !is MKUserLocation }

            if (existingCustomPins.size != locations.size) {
                mapView.removeAnnotations(existingCustomPins)

                locations.forEach { club ->
                    val annotation = MKPointAnnotation().apply {
                        setCoordinate(CLLocationCoordinate2DMake(club.lat, club.lng))
                        setTitle(club.id)
                    }
                    mapView.addAnnotation(annotation)
                }
            }

            // 2. ZOOM TO SELECTED CLUB
            if (selectedClub != null) {
                val annotationToSelect = mapView.annotations.firstOrNull {
                    (it as? MKAnnotationProtocol)?.title == selectedClub.id
                } as? MKAnnotationProtocol

                if (annotationToSelect != null) {
                    mapView.selectAnnotation(annotationToSelect, animated = true)

                    // Pan and zoom beautifully to 1000m altitude
                    val centerCoord = CLLocationCoordinate2DMake(selectedClub.lat, selectedClub.lng)
                    val region = MKCoordinateRegionMakeWithDistance(centerCoord, 1000.0, 1000.0)
                    mapView.setRegion(region, animated = true)
                }
            } else {
                mapView.selectedAnnotations.forEach {
                    mapView.deselectAnnotation(it as MKAnnotationProtocol, animated = true)
                }
            }

            // 3. ZOOM TO USER LOCATION
            if (userLocationTrigger > lastHandledTrigger) {
                lastHandledTrigger = userLocationTrigger

                val userLoc = mapView.userLocation.location
                if (userLoc != null) {
                    // Pan and zoom beautifully to the user's location at 1000m altitude
                    val region = MKCoordinateRegionMakeWithDistance(userLoc.coordinate, 1000.0, 1000.0)
                    mapView.setRegion(region, animated = true)
                }
            }
        }
    )
}