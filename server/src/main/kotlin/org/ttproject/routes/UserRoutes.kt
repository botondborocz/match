package org.ttproject.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.ttproject.database.tables.Users
import org.ttproject.utils.calculateDistanceKm
import kotlinx.serialization.Serializable
import org.ttproject.data.SwipeRequest
import org.ttproject.data.SwipeResponse
import org.ttproject.server.services.MatchService

fun Route.userRoutes() {
    // We put this inside the JWT block so only logged-in users can see other players
    authenticate("auth-jwt") {

        get("/users/nearby") {
            // Read the coordinates from the URL (e.g., /users/nearby?lat=47.5&lng=19.1)
            val userLat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val userLng = call.request.queryParameters["lng"]?.toDoubleOrNull()

            val allPlayers = transaction {
                Users.selectAll().map { row ->
                    // Map your DB row to a simple data class to send as JSON
                    PlayerResponse(
                        id = row[Users.id].toString(),
                        username = row[Users.username],
                        skillLevel = row[Users.skillLevel].toString(),
                        lat = row[Users.lastLat],
                        lng = row[Users.lastLng]
                    )
                }
            }

            if (userLat != null && userLng != null) {
                // 📍 SORT BY DISTANCE
                val sortedPlayers = allPlayers.sortedBy { player ->
                    if (player.lat != null && player.lng != null) {
                        calculateDistanceKm(userLat, userLng, player.lat, player.lng)
                    } else {
                        Double.MAX_VALUE // Put users with no location at the very bottom
                    }
                }
                call.respond(sortedPlayers)
            } else {
                // 🎲 RANDOM SHUFFLE
                call.respond(allPlayers.shuffled())
            }
        }

        post("/users/{playerId}/swipe") {
            // 1. Get the ID of the user who is swiping (from the JWT token)
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asString()

            if (currentUserId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@post
            }

            // 2. Get the ID of the player they are swiping ON (from the URL)
            val targetPlayerId = call.parameters["playerId"]
            if (targetPlayerId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing playerId in URL")
                return@post
            }

            // 3. Get the swipe action (Like or Pass) from the JSON body
            val swipeRequest = call.receive<SwipeRequest>()

            val matchService = MatchService()

            // 4. Send this to your database service to process
            val isMatch = matchService.processSwipe(
                swiperId = currentUserId,
                targetId = targetPlayerId,
                isLiked = swipeRequest.isLiked
            )

            // 5. Reply to the mobile app!
            call.respond(HttpStatusCode.OK, SwipeResponse(isMatch = isMatch))
        }
    }
}

// The JSON model to send back
@Serializable
data class PlayerResponse(
    val id: String,
    val username: String,
    val skillLevel: String,
    val lat: Double?,
    val lng: Double?
)