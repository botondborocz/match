package org.ttproject.data

import kotlinx.serialization.Serializable

// 1. What we send TO the server
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

// 2. What we get BACK from the server
@Serializable
data class TokenResponse(
    val token: String
)