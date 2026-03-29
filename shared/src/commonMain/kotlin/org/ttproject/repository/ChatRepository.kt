package org.ttproject.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.ttproject.ORACLE_IP
import org.ttproject.SERVER_DNS
import org.ttproject.SERVER_IP
import org.ttproject.data.ChatThreadDto
import org.ttproject.data.Location
import org.ttproject.data.MessageDto
import org.ttproject.data.TokenResponse
import org.ttproject.data.TokenStorage

interface ChatRepository {
    suspend fun getMessageHistory(connectionId: String): List<MessageDto>
    fun observeLiveMessages(connectionId: String): Flow<MessageDto>
    suspend fun sendMessage(text: String)
    fun disconnect()
    suspend fun getConnections(): List<ChatThreadDto>
    suspend fun savePushToken(fcmToken: String)
}

class ChatRepositoryImpl (
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) : ChatRepository {
    // We hold onto the active session so we can send messages through it later
    private var webSocketSession: DefaultClientWebSocketSession? = null

    // 1. Fetch History via REST
    override suspend fun getMessageHistory(connectionId: String): List<MessageDto> {
        val token = tokenStorage.getToken() ?: return emptyList()
        return try {
            client.get("${SERVER_IP}/api/connections/$connectionId/messages"){
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Open WebSocket and return a stream (Flow) of incoming messages
    override fun observeLiveMessages(connectionId: String): Flow<MessageDto> = flow {
        val token = tokenStorage.getToken() ?: throw Exception("No auth token found")
        try {
            client.webSocket(
                urlString = "wss://${SERVER_DNS}/api/connections/$connectionId/chat",
                request = {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            ) {
                webSocketSession = this // Save the session

                // Keep reading incoming frames as long as the connection is open
                while (true) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // Parse the JSON string into our Kotlin object
                        val message = Json.decodeFromString<MessageDto>(text)
                        emit(message) // Push to the UI
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Clean up when the connection drops or the user leaves the screen
            webSocketSession = null
        }
    }

    // 3. Send a message through the active WebSocket
    override suspend fun sendMessage(text: String) {
        webSocketSession?.send(Frame.Text(text))
    }

    override fun disconnect() {
        // Handled automatically when the coroutine observing the Flow is cancelled,
        // but you can add explicit close logic here if needed.
    }

    override suspend fun getConnections(): List<ChatThreadDto> {
        val token = tokenStorage.getToken() ?: return emptyList()

        return try {
            client.get("${SERVER_IP}/api/connections") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun savePushToken(fcmToken: String) {
        val token = tokenStorage.getToken() ?: return

        try {
            client.post("${SERVER_IP}/api/users/fcm-token") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(TokenResponse(token = fcmToken))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}