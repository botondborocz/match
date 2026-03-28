package org.ttproject.services

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager {

    /**
     * Maps a Connection ID (the chat room) to a thread-safe Set of active WebSocket sessions.
     * We use a ConcurrentHashMap so multiple threads can read/write simultaneously safely.
     */
    private val activeConnections = ConcurrentHashMap<UUID, MutableSet<WebSocketServerSession>>()

    /**
     * Called when a user connects to the /chat route.
     */
    fun addSession(connectionId: UUID, session: WebSocketServerSession) {
        // computeIfAbsent creates a new synchronized set ONLY if the connectionId doesn't exist yet
        val sessions = activeConnections.computeIfAbsent(connectionId) {
            Collections.synchronizedSet(LinkedHashSet())
        }
        sessions.add(session)
    }

    /**
     * Called in the `finally` block of your route when a user disconnects or crashes.
     */
    fun removeSession(connectionId: UUID, session: WebSocketServerSession) {
        val sessions = activeConnections[connectionId]
        if (sessions != null) {
            sessions.remove(session)

            // Cleanup: If both users leave the chat room, remove the room from memory
            // to prevent memory leaks over time.
            if (sessions.isEmpty()) {
                activeConnections.remove(connectionId)
            }
        }
    }

    /**
     * Iterates through everyone in the room and pushes the message to them.
     */
    suspend fun broadcast(connectionId: UUID, messagePayload: String) {
        // Get all active sessions for this specific chat room
        val sessions = activeConnections[connectionId] ?: return

        // We convert the synchronized set to a static List before iterating.
        // This is crucial! Since `session.send()` is a suspending function,
        // the list of users could change while we are waiting to send the message.
        val activeSessionsSnapshot = sessions.toList()

        activeSessionsSnapshot.forEach { session ->
            // Only send if the connection is still alive
            if (session.isActive) {
                try {
                    session.send(Frame.Text(messagePayload))
                } catch (e: Exception) {
                    // If the send fails (e.g., dropped internet), forcefully remove them
                    removeSession(connectionId, session)
                }
            }
        }
    }
}