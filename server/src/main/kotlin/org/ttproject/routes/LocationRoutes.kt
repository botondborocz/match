package org.ttproject.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.ttproject.data.Location
import org.ttproject.database.tables.Locations
import org.ttproject.utils.calculateDistanceKm

fun Route.locationRoutes() {
    authenticate("auth-jwt") {

        get("/locations/nearby") {
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asString()

            if (currentUserId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@get
            }

            // Read the coordinates from the URL (e.g., /locations/nearby?lat=47.5&lng=19.1)
            val userLat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val userLng = call.request.queryParameters["lng"]?.toDoubleOrNull()

            // 1. Fetch all locations from the DB
            val allLocations = transaction {
                Locations.selectAll().map { row ->
                    Location(
                        id = row[Locations.id].toString(),
                        name = row[Locations.name],
                        latitude = row[Locations.latitude],
                        longitude = row[Locations.longitude],
                        type = org.ttproject.data.LocationType.valueOf(row[Locations.type].name),                        isFree = row[Locations.isFree],
                        tableCount = row[Locations.tableCount],
                        address = row[Locations.address],
                        createdBy = row[Locations.createdBy]?.toString()
                    )
                }
            }

            // 2. Sort or Filter
            if (userLat != null && userLng != null) {
                // 📍 Sort by distance to user
                val sortedLocations = allLocations.sortedBy { loc ->
                    calculateDistanceKm(userLat, userLng, loc.latitude, loc.longitude)
                }
                call.respond(sortedLocations)
            } else {
                // Return unsorted if no coordinates provided
                call.respond(allLocations)
            }
        }
    }
}