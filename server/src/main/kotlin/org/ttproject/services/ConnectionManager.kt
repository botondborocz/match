package org.ttproject.services

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager {

    // Map: Connection ID -> (Map: User ID -> WebSocket Session)
    // Using MutableMap for the inner value makes Kotlin's standard library extensions happier
    private val activeConnections = ConcurrentHashMap<UUID, MutableMap<UUID, WebSocketServerSession>>()

    fun addSession(connectionId: UUID, userId: UUID, session: WebSocketServerSession) {
        // 👇 FIX 1: Use Kotlin's getOrPut, and explicitly declare the types inside the braces!
        val room = activeConnections.getOrPut(connectionId) {
            ConcurrentHashMap<UUID, WebSocketServerSession>()
        }
        room[userId] = session
    }

    fun removeSession(connectionId: UUID, userId: UUID) {
        val room = activeConnections[connectionId]
        if (room != null) {
            room.remove(userId)
            if (room.isEmpty()) {
                activeConnections.remove(connectionId)
            }
        }
    }

    fun isUserConnected(connectionId: UUID, userId: UUID): Boolean {
        val room = activeConnections[connectionId] ?: return false
        // 👇 FIX 2: Now that the compiler knows 'room' is a MutableMap<UUID, WebSocketSession>,
        // containsKey will compile perfectly.
        return room.containsKey(userId) && room[userId]?.isActive == true
    }

    suspend fun broadcast(connectionId: UUID, messagePayload: String) {
        val room = activeConnections[connectionId] ?: return
        val activeSessionsSnapshot = room.values.toList()

        activeSessionsSnapshot.forEach { session ->
            if (session.isActive) {
                try {
                    session.send(Frame.Text(messagePayload))
                } catch (e: Exception) {
                    // Ignored here, the disconnect block in the route handles cleanup
                }
            }
        }
    }
}