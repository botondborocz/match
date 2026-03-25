package org.ttproject.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import org.ttproject.SERVER_IP
import org.ttproject.data.Location
import org.ttproject.data.Player
import org.ttproject.data.TokenStorage

interface LocationRepository {
    suspend fun getNearbyLocations(): List<Location>
}

class LocationRepositoryImpl(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : LocationRepository {

    override suspend fun getNearbyLocations(): List<Location> {
        return try {
            httpClient.get("${SERVER_IP}/api/locations/nearby") {
            }.body<List<Location>>()
        } catch (e: Exception) {
            println("Network Error fetching locations: ${e.message}")
            emptyList()
        }
    }
}