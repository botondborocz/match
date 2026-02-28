package org.ttproject.routes

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.ttproject.database.tables.SkillLevel
import org.ttproject.database.tables.Users
import org.ttproject.security.JwtConfig
import java.util.UUID

// Data classes for Ktor to parse incoming JSON
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val username: String, val fullName: String)
data class GoogleAuthRequest(val idToken: String)

fun Route.authRoutes() {

    // 1. BASIC REGISTER
    post("/auth/register") {
        val req = call.receive<RegisterRequest>()

        // Securely hash the password (NEVER save plain text!)
        val hashedPw = BCrypt.hashpw(req.password, BCrypt.gensalt())

        val newUserId = transaction {
            Users.insert {
                it[email] = req.email
                it[passwordHash] = hashedPw
                it[username] = req.username
                it[fullName] = req.fullName
                it[skillLevel] = SkillLevel.Beginner
                it[createdAt] = java.time.Instant.now()
            } get Users.id
        }

        // Note: In reality, you'd generate and return a JWT here.
        call.respondText("Registered successfully! User ID: $newUserId")
    }

    // 2. BASIC LOGIN
    post("/auth/login") {
        val req = call.receive<LoginRequest>()

        val userRow = transaction {
            Users.select { Users.email eq req.email }.singleOrNull()
        }

        if (userRow == null) {
            call.respondText("User not found", status = io.ktor.http.HttpStatusCode.Unauthorized)
            return@post
        }

        val savedHash = userRow[Users.passwordHash]
        if (savedHash != null && BCrypt.checkpw(req.password, savedHash)) {
            val userId = userRow[Users.id]
            // Generate the token using the user's UUID
            val token = JwtConfig.generateToken(userId.toString())

            // Send it back to Android as a JSON response!
            call.respondText(
                """{"token": "$token"}""",
                io.ktor.http.ContentType.Application.Json
            )
        } else {
            call.respondText("Invalid password", status = io.ktor.http.HttpStatusCode.Unauthorized)
        }
    }

    // 3. GOOGLE LOGIN
    post("/auth/google") {
        val req = call.receive<GoogleAuthRequest>()

        // Initialize Google Verifier (Replace with your actual Google Cloud Web Client ID)
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
            .setAudience(listOf("YOUR_GOOGLE_WEB_CLIENT_ID"))
            .build()

        val idToken = verifier.verify(req.idToken)

        if (idToken != null) {
            val payload = idToken.payload
            val googleUserId = payload.subject // The unique Google ID
            val googleEmail = payload.email
            val googleName = payload["name"] as String

            val userId = transaction {
                // Check if user exists
                val existingUser = Users.select { Users.googleId eq googleUserId }.singleOrNull()

                if (existingUser != null) {
                    existingUser[Users.id]
                } else {
                    // Auto-register the new Google user!
                    Users.insert {
                        it[email] = googleEmail
                        it[googleId] = googleUserId
                        it[fullName] = googleName
                        // Create a random username for them to change later
                        it[username] = "user_${UUID.randomUUID().toString().take(8)}"
                        it[skillLevel] = SkillLevel.Beginner
                        it[createdAt] = java.time.Instant.now()
                    } get Users.id
                }
            }
            // Success! Generate JWT here.
            call.respondText("Google Login successful! User ID: $userId")
        } else {
            call.respondText("Invalid Google Token", status = io.ktor.http.HttpStatusCode.Unauthorized)
        }
    }
}