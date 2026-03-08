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
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.ttproject.data.PlayerResponse
import org.ttproject.data.SwipeRequest
import org.ttproject.data.SwipeResponse
import org.ttproject.data.UpdateLanguageRequest
import org.ttproject.data.UserProfile
import org.ttproject.database.tables.Swipes
import org.ttproject.services.MatchService
import java.util.UUID

fun Route.userRoutes() {
    // We put this inside the JWT block so only logged-in users can see other players
    authenticate("auth-jwt") {

        get("/users/nearby") {
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asString()

            if (currentUserId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@get
            }
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
                }.filter { it.id != currentUserId }
            }

            val alreadySwipedPlayers = transaction {
                Swipes.selectAll().where {
                    (Swipes.swiperId eq UUID.fromString(currentUserId)) and
                            (Swipes.swipedId inList allPlayers.map { UUID.fromString(it.id) })
                }.map {
                    it[Swipes.swipedId].toString()
                }.toSet()
            }

            val freshPlayers = allPlayers.filterNot { player ->
                alreadySwipedPlayers.contains(player.id)
            }

            if (userLat != null && userLng != null) {
                // 📍 SORT BY DISTANCE
                val sortedPlayers = freshPlayers.sortedBy { player ->
                    if (player.lat != null && player.lng != null) {
                        calculateDistanceKm(userLat, userLng, player.lat!!, player.lng!!)
                    } else {
                        Double.MAX_VALUE // Put users with no location at the very bottom
                    }
                }
                call.respond(sortedPlayers)
            } else {
                // 🎲 RANDOM SHUFFLE
                call.respond(freshPlayers.shuffled())
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
        get("/users/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            val userProfile = transaction {
                Users.selectAll().where { Users.id eq UUID.fromString(userId) }
                    .singleOrNull()?.let { row ->
                        UserProfile(
                            id = row[Users.id].toString(),
                            name = row[Users.username],
                            email = row[Users.email],
                            elo = row[Users.eloRating],
                            winRate = "50%", // Placeholder, calculate based on your matches table
                            preferredLanguage = row[Users.preferred_language]
                        )
                }
            }

            if (userProfile != null) {
                call.respond(userProfile)
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }
        put("/users/language") {
            // 1. Grab the user's ID from their secure JWT token
            val principal = call.principal<JWTPrincipal>()
            val userIdClaim = principal?.payload?.getClaim("userId")?.asString() // Note: check your login function to see what you named this claim! It might just be "id".

            if (userIdClaim == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@put
            }

            // 2. Parse the JSON body from the mobile app
            val request = try {
                call.receive<UpdateLanguageRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid JSON format")
                return@put
            }

            // 3. Update the database!
            try {
                val userUuid = UUID.fromString(userIdClaim)

                val updatedRows = transaction {
                    Users.update({ Users.id eq userUuid }) {
                        it[Users.preferred_language] = request.language
                    }
                }

                if (updatedRows > 0) {
                    call.respond(HttpStatusCode.OK, "Language updated successfully!")
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found in database.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Database error: ${e.message}")
            }
        }
    }
}
