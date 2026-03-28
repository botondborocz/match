package org.ttproject.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import kotlinx.coroutines.channels.consumeEach
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.ttproject.data.ChatThreadDto
import org.ttproject.data.MessageDto
import org.ttproject.database.tables.Connections
import org.ttproject.database.tables.Messages
import org.ttproject.database.tables.Users
import org.ttproject.services.ConnectionManager
import java.time.Instant
import java.util.UUID

// You will need a simple class to manage your active WebSocket connections
val connectionManager = ConnectionManager()

fun Route.messageRoutes() {

    authenticate("auth-jwt") {
        get("/api/connections") {
            val principal = call.principal<JWTPrincipal>()
            val currentUserIdStr = principal?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val currentUserId = UUID.fromString(currentUserIdStr)

            val connections = transaction {
                // Find all connections where the current user is involved
                val userConnections = Connections.selectAll().where {
                    (Connections.user1Id eq currentUserId) or (Connections.user2Id eq currentUserId)
                }.toList()

                userConnections.map { row ->
                    val connectionId = row[Connections.id]
                    val user1 = row[Connections.user1Id]
                    val user2 = row[Connections.user2Id]

                    // Determine who the *other* person is
                    val otherUserId = if (user1 == currentUserId) user2 else user1

                    // Fetch the other user's details
                    val otherUser = Users.selectAll().where { Users.id eq otherUserId }.singleOrNull()
                    val otherUserName = otherUser?.get(Users.username) ?: "Unknown Player"

                    // Optional: Fetch the actual last message (Keep it simple for MVP)
                    val lastMessageRow = Messages.selectAll()
                        .where { Messages.connectionId eq connectionId }
                        .orderBy(Messages.createdAt to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()

                    val lastMessageText = lastMessageRow?.get(Messages.content) ?: "No messages yet"
                    val timestamp = lastMessageRow?.get(Messages.createdAt)?.toString() ?: ""

                    ChatThreadDto(
                        id = connectionId.toString(),
                        otherUserName = otherUserName,
                        lastMessage = lastMessageText,
                        timestamp = timestamp,
                        unreadCount = 0, // Implement unread logic later
                        isOnline = false // Implement presence logic later
                    )
                }
            }
            val sortedConnections = connections.sortedByDescending { it.timestamp }

            call.respond(HttpStatusCode.OK, sortedConnections)
        }

        route("/api/connections/{connectionId}") {

            // --------------------------------------------------------
            // 1. GET HISTORY: Fetch old messages via standard HTTP
            // --------------------------------------------------------
            get("/messages") {
                val connectionIdStr = call.parameters["connectionId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing connection ID"
                )
                val connectionId = UUID.fromString(connectionIdStr)

                // Query the database using Exposed
                val messages = transaction {
                    Messages.selectAll().where { Messages.connectionId eq connectionId }
                        .orderBy(Messages.createdAt to SortOrder.ASC) // Oldest first, so UI scrolls down
                        .map {
                            // Map your DB row to a simple Data Transfer Object (DTO)
                            MessageDto(
                                id = it[Messages.id].toString(),
                                senderId = it[Messages.senderId].toString(),
                                content = it[Messages.content],
                                createdAt = it[Messages.createdAt].toString()
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, messages)
            }

            // --------------------------------------------------------
            // 2. LIVE CHAT: Connect via WebSockets
            // --------------------------------------------------------
            webSocket("/chat") {
                val connectionIdStr = call.parameters["connectionId"] ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "No connection ID"
                    )
                )
                val connectionId = UUID.fromString(connectionIdStr)

                val principal = call.principal<JWTPrincipal>()
                val senderIdStr = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                val senderId = UUID.fromString(senderIdStr)

                // Add this active user to our thread-safe manager
                connectionManager.addSession(connectionId, this)

                try {
                    // Keep the pipe open and listen for incoming messages from this user
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val textContent = frame.readText()
                            val created_at = Instant.now()

                            // A. Save the new message to the database
                            val newMessageId = transaction {
                                Messages.insert {
                                    it[Messages.connectionId] = connectionId
                                    it[Messages.senderId] = senderId
                                    it[content] = textContent
                                    it[createdAt] = created_at
                                    // Exposed handles the timestamp automatically based on your schema setup,
                                    // or you can explicitly set it here using java.time.Instant.now()
                                } get Messages.id
                            }

                            // B. Broadcast the message to the other user in the connection
                            // (You would format this as JSON in a real app)
                            val payload =
                                """{"id": "$newMessageId", "senderId": "$senderId", "content": "$textContent", "createdAt": "$created_at"}"""
                            connectionManager.broadcast(connectionId, payload)
                        }
                    }
                } catch (e: Exception) {
                    // Handle disconnects or errors gracefully
                    call.application.environment.log.error("WebSocket error", e)
                } finally {
                    // Clean up the session when the user closes the app or drops connection
                    connectionManager.removeSession(connectionId, this)
                }
            }
        }
    }
}