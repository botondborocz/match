package org.ttproject.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Messages : Table("messages") {
    val id = uuid("id").autoGenerate()
    val connectionId = reference("connection_id", Connections.id)
    val senderId = reference("sender_id", Users.id)
    val content = text("content")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}