package org.ttproject.database.places_import

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class GeoJsonCollection(
    val features: List<GeoJsonFeature>
)

@Serializable
data class GeoJsonFeature(
    val geometry: GeoJsonGeometry,
    val properties: JsonObject // Handles the dynamic map of OSM tags
)

@Serializable
data class GeoJsonGeometry(
    val type: String,
    val coordinates: List<Double> // Index 0 is Longitude, Index 1 is Latitude
)