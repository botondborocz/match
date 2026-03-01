package org.ttproject.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.ttproject.SERVER_IP
import org.ttproject.data.Player
import org.ttproject.data.SwipeRequest
import org.ttproject.data.SwipeResponse
import org.ttproject.data.TokenStorage

// The interface remains completely unchanged!
interface MatchRepository {
    suspend fun getNearbyPlayers(): List<Player>
    suspend fun recordSwipeAction(playerId: String, isLiked: Boolean): Boolean
}

// 👇 The new Ktor implementation
class MatchRepositoryImpl(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : MatchRepository {

    override suspend fun getNearbyPlayers(): List<Player> {
        // Make the GET request and automatically parse the JSON array into a List<Player>
        return try {
            httpClient.get("${SERVER_IP}/users/nearby"){
                bearerAuth(tokenStorage.getToken()!!)
            }.body<List<Player>>()
        } catch (e: Exception) {
            // In a real app, you might want to throw a custom domain exception here
            println("Network Error fetching players: ${e.message}")
            emptyList()
        }
    }

    override suspend fun recordSwipeAction(playerId: String, isLiked: Boolean): Boolean {
        return try {
            // Ask the backend if this was a match
            val response = httpClient.post("${SERVER_IP}/users/$playerId/swipe") {
                contentType(ContentType.Application.Json)
                bearerAuth(tokenStorage.getToken()!!)
                setBody(SwipeRequest(isLiked = isLiked))
            }.body<SwipeResponse>()

            response.isMatch // Return the backend's answer
        } catch (e: Exception) {
            println("Network Error recording swipe: ${e.message}")
            false // Default to no match if the network fails
        }
    }
}
