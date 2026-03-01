package org.ttproject.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

enum class SwipeAction { Like, Pass, SuperLike }

object Swipes : Table("swipes") {
    val id = uuid("id").autoGenerate()
    val swiperId = reference("swiper_id", Users.id)
    val swipedId = reference("swiped_id", Users.id)
    val isLiked = bool("is_liked")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Connections : Table("connections") {
    val id = uuid("id").autoGenerate()
    val user1Id = reference("user1_id", Users.id)
    val user2Id = reference("user2_id", Users.id)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}