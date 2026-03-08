package org.ttproject.data

import kotlinx.serialization.Serializable

// 👇 @Serializable is required so Ktor knows how to convert the JSON string into this Kotlin object
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val elo: Int,
    val winRate: String,
    val preferredLanguage: String? = "en" // Optional fallback
)

@Serializable
data class PlayerResponse(
    val id: String,
    val username: String,
    val skillLevel: String,
    val lat: Double?,
    val lng: Double?,
    val preferredLanguage: String? = null
)