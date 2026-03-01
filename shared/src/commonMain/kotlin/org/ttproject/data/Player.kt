package org.ttproject.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val username: String,
    val skillLevel: String,
    val lat: Double,
    val lng: Double,
    // We'll calculate mock values for the UI display based on your design
    val age: Int = (18..45).random(),
    val elo: Int = (1000..2000).random(),
    val distanceKm: Int = (1..10).random()
)