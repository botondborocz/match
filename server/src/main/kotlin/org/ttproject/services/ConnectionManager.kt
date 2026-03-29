package org.ttproject.services

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager {

    // Map: Connection ID -> (Map: User ID -> WebSocket Session)
    private val activeConnections = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, WebSocketServerSession>>()

    fun addSession(connectionId: UUID, userId: UUID, session: WebSocketServerSession) {
        // computeIfAbsent is fully atomic!
        val room = activeConnections.computeIfAbsent(connectionId) {
            ConcurrentHashMap()
        }
        room[userId] = session
    }

    fun removeSession(connectionId: UUID, userId: UUID) {
        // 👇 FIX: computeIfPresent locks this specific room for a microsecond.
        // If the block returns 'null', the map safely deletes the room.
        // If it returns the room, it keeps it. No race conditions!
        activeConnections.computeIfPresent(connectionId) { _, room ->
            room.remove(userId)
            if (room.isEmpty()) null else room
        }
    }

    fun isUserConnected(connectionId: UUID, userId: UUID): Boolean {
        val room = activeConnections[connectionId] ?: return false
        // 👇 CLEANUP: You don't actually need containsKey.
        // The safe call `?.` handles the null check automatically!
        return room[userId]?.isActive == true
    }

    suspend fun broadcast(connectionId: UUID, messagePayload: String) {
        val room = activeConnections[connectionId] ?: return
        // Taking a snapshot prevents ConcurrentModificationExceptions
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