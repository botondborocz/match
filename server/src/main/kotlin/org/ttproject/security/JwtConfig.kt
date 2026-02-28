package org.ttproject.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "local-development-secret-123"
    const val issuer = "http://0.0.0.0:8080"
    const val audience = "match-app-users"
    private const val validityInMs = 36_000_00 * 24 * 30L // 30 days

    private val algorithm = Algorithm.HMAC256(secret)

    // The verifier is used by Ktor to check if incoming tokens are fake
    val verifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    // This function generates the actual token string!
    fun generateToken(userId: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId) // We hide their user ID inside the token!
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }
}