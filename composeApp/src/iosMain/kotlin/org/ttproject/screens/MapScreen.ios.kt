package org.ttproject.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
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
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIImageSymbolWeightBold
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject
import kotlinx.cinterop.useContents

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    userLocationTrigger: Int,
    bottomPadding: Dp, // 👈 Added Bottom Padding
    isDark: Boolean,   // 👈 Added Theme State
    onMarkerClick: (TTClub) -> Unit,
    onBoundsChanged: (MapBounds) -> Unit
) {
    val currentLocations by rememberUpdatedState(locations)
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)

    val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)

    var lastHandledTrigger by remember { mutableStateOf(0) }

    // 1. Request iOS Location Permission
    val locationManager = remember { CLLocationManager() }
    LaunchedEffect(Unit) {
        locationManager.requestWhenInUseAuthorization()
    }

    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {

            // 👇 3. ADD THIS: Fires when the map stops moving!
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                mapView.region.useContents {
                    // Calculate the 4 edges of the screen using the center point and the "span" (zoom level)
                    val north = center.latitude + (span.latitudeDelta / 2.0)
                    val south = center.latitude - (span.latitudeDelta / 2.0)
                    val east = center.longitude + (span.longitudeDelta / 2.0)
                    val west = center.longitude - (span.longitudeDelta / 2.0)

                    currentOnBoundsChanged(MapBounds(north = north, south = south, east = east, west = west))
                }
            }

            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val tappedTitle = didSelectAnnotationView.annotation?.title
                val clickedClub = currentLocations.find { it.id == tappedTitle }
                if (clickedClub != null) currentOnMarkerClick(clickedClub)
            }

            override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {

                // A. CUSTOM USER LOCATION MARKER
                if (viewForAnnotation is MKUserLocation) {
                    val userIdentifier = "CustomUserLocation"
                    var userView = mapView.dequeueReusableAnnotationViewWithIdentifier(userIdentifier)

                    if (userView == null) {
                        userView = MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = userIdentifier)
                    } else {
                        userView.annotation = viewForAnnotation
                    }

                    // Use "circle.circle.fill" - it looks like a solid dot with an outer ring
                    val config = UIImageSymbolConfiguration.configurationWithPointSize(22.0, UIImageSymbolWeightBold)
                    userView.image = UIImage.systemImageNamed("circle.circle.fill", withConfiguration = config)

                    // Match the exact Android Cyan color!
                    userView.tintColor = UIColor.colorWithRed(0.0, 0.824, 1.0, 1.0)

                    // Add a native shadow so it pops off the map
//                    userView.layer.shadowColor = UIColor.blackColor.CGColor
//                    userView.layer.shadowOpacity = 0.3f
//                    userView.layer.shadowRadius = 4.0
//                    userView.layer.shadowOffset = CGSizeMake(0.0, 2.0)

                    return userView
                }
                if (viewForAnnotation is MKUserLocation) {
                    val userIdentifier = "CustomUserLocation"
                    var userView = mapView.dequeueReusableAnnotationViewWithIdentifier(userIdentifier)

                    if (userView == null) {
                        userView = MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = userIdentifier)
                    } else {
                        userView.annotation = viewForAnnotation
                    }

                    val config = UIImageSymbolConfiguration.configurationWithPointSize(28.0, UIImageSymbolWeightBold)
                    userView.image = UIImage.systemImageNamed("figure.table.tennis", withConfiguration = config)
                    userView.tintColor = UIColor.colorWithRed(0.0, 0.824, 1.0, 1.0)

                    return userView
                }

                // B. CUSTOM CLUB PINS
                val identifier = "CustomOrangePin"
                var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier) as? MKMarkerAnnotationView

                if (annotationView == null) {
                    annotationView = MKMarkerAnnotationView(annotation = viewForAnnotation, reuseIdentifier = identifier)
                    annotationView.canShowCallout = false
                } else {
                    annotationView.annotation = viewForAnnotation
                }

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

            // 👇 1. Apply Dynamic Bottom Padding to shift the map center
            mapView.layoutMargins = UIEdgeInsetsMake(0.0, 0.0, bottomPadding.value.toDouble(), 0.0)

            // 👇 2. Apply Dynamic Theme to iOS Map
            mapView.overrideUserInterfaceStyle = if (isDark) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark
            } else {
                UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }

            // 3. PLOT MARKERS
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

            // 4. ZOOM TO SELECTED CLUB
            if (selectedClub != null) {
                val annotationToSelect = mapView.annotations.firstOrNull {
                    (it as? MKAnnotationProtocol)?.title == selectedClub.id
                } as? MKAnnotationProtocol

                if (annotationToSelect != null) {
                    mapView.selectAnnotation(annotationToSelect, animated = true)

                    val centerCoord = CLLocationCoordinate2DMake(selectedClub.lat, selectedClub.lng)
                    val region = MKCoordinateRegionMakeWithDistance(centerCoord, 1000.0, 1000.0)
                    mapView.setRegion(region, animated = true)
                }
            } else {
                mapView.selectedAnnotations.forEach {
                    mapView.deselectAnnotation(it as MKAnnotationProtocol, animated = true)
                }
            }

            // 5. ZOOM TO USER LOCATION
            if (userLocationTrigger > lastHandledTrigger) {
                lastHandledTrigger = userLocationTrigger

                val userLoc = mapView.userLocation.location
                if (userLoc != null) {
                    val region = MKCoordinateRegionMakeWithDistance(userLoc.coordinate, 1000.0, 1000.0)
                    mapView.setRegion(region, animated = true)
                }
            }
        }
    )
}