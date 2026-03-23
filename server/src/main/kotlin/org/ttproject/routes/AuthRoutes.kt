package org.ttproject.routes

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpStatusCode
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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Data classes for Ktor to parse incoming JSON
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val username: String? = null, val fullName: String? = null)
data class GoogleAuthRequest(val idToken: String)

fun Route.authRoutes() {
    route("/api/auth") {
        // 1. BASIC REGISTER
        post("/register") {
            val req = call.receive<RegisterRequest>()

            // 1. Check if the user already exists!
            val existingUser = transaction {
                Users.select { Users.email eq req.email }.singleOrNull()
            }

            if (existingUser != null) {
                call.respondText("Email already in use", status = HttpStatusCode.Conflict)
                return@post
            }

            // 2. Hash the password
            val hashedPw = BCrypt.hashpw(req.password, BCrypt.gensalt())

            // 3. Insert the user safely
            val newUserId = transaction {
                Users.insert {
                    it[email] = req.email
                    it[passwordHash] = hashedPw

                    // If they didn't send a username, generate a random one just like we did for Google!
                    it[username] = req.username ?: "user_${java.util.UUID.randomUUID().toString().take(8)}"
                    it[fullName] = req.fullName ?: "test" // This will safely insert null if they didn't send it
                    it[skillLevel] = SkillLevel.Beginner
                    it[createdAt] = java.time.Instant.now()
                } get Users.id
            }

            // Success!
            // Generate the token using the user's UUID
            val token = JwtConfig.generateToken(newUserId.toString())

            // Send it back to Android as a JSON response!
            call.respondText(
                """{"token": "$token"}""",
                io.ktor.http.ContentType.Application.Json
            )
        }

        // 2. BASIC LOGIN
        post("/login") {
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

        // 1. Load the .env file
        // (ignoreIfMissing = true prevents crashes in production where Docker handles envs instead)
        val dotenv = dotenv {
            ignoreIfMissing = true
        }
        println(dotenv["GOOGLE_CLIENT_ID"])
        println(System.getenv("GOOGLE_CLIENT_ID"))

        // 2. Grab your Google ID! (Falls back to System Env if .env file is missing)
        val googleClientId = dotenv["GOOGLE_CLIENT_ID"]
            ?: System.getenv("GOOGLE_CLIENT_ID")
            ?: throw IllegalStateException("Google Client ID is missing!")

        // 3. Build your verifier
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
            .setAudience(listOf(googleClientId))
            .build()

        // 3. GOOGLE LOGIN
                post("/google") {
                    val req = call.receive<GoogleAuthRequest>() // Ktor catches the JSON from React

                    // 1. Call Google's UserInfo API using the token React sent us
                    val client = HttpClient.newHttpClient()
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                        .header("Authorization", "Bearer ${req.idToken}") // (Or req.token, whatever your data class uses)
                        .GET()
                        .build()

                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                    // 2. Check if Google approved the token
                    if (response.statusCode() == 200) {
                        // Parse the JSON Google sent back
                        val json = Json.parseToJsonElement(response.body()).jsonObject
                        val googleUserId = json["sub"]?.jsonPrimitive?.content ?: return@post call.respondText("Missing Google ID", status = HttpStatusCode.BadRequest)
                        val googleEmail = json["email"]?.jsonPrimitive?.content ?: ""
                        val googleName = json["name"]?.jsonPrimitive?.content ?: ""

                        // 3. Save or find the user in your database (Exactly the same as before!)
                        val userId = transaction {
                            val existingUser = Users.select { Users.googleId eq googleUserId }.singleOrNull()

                            if (existingUser != null) {
                                existingUser[Users.id]
                            } else {
                                Users.insert {
                                    it[email] = googleEmail
                                    it[googleId] = googleUserId
                                    it[fullName] = googleName
                                    it[username] = "user_${java.util.UUID.randomUUID().toString().take(8)}"
                                    it[skillLevel] = SkillLevel.Beginner
                                    it[createdAt] = java.time.Instant.now()
                                } get Users.id
                            }
                        }
                        // Generate the token using the user's UUID
                        val token = JwtConfig.generateToken(userId.toString())

                        // Send it back to Android as a JSON response!
                        call.respondText(
                            """{"token": "$token"}""",
                            io.ktor.http.ContentType.Application.Json
                        )
                    } else {
                        call.respondText("Invalid or Expired Google Token", status = HttpStatusCode.Unauthorized)
                    }
                }
    }
}