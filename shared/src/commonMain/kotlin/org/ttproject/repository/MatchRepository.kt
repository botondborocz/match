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
import io.ktor.client.statement.bodyAsText

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
        // 👇 1. Check for token FIRST. If null, throw it so the ViewModel catches the Error state!
        val token = tokenStorage.getToken() ?: throw Exception("No token found")

        return try {
            val response = httpClient.get("${SERVER_IP}/api/users/nearby"){
                bearerAuth(token) // 👈 Safe to use now
            }
            response.body<List<Player>>()
        } catch (e: Exception) {
            println("Network Error fetching players: ${e.message}")
            emptyList()
        }
    }

    override suspend fun recordSwipeAction(playerId: String, isLiked: Boolean): Boolean {
        return try {
            // Ask the backend if this was a match
            val response = httpClient.post("${SERVER_IP}/api/users/$playerId/swipe") {
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
