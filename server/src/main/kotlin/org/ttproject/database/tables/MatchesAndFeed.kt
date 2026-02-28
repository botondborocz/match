package org.ttproject.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

enum class MatchStatus { Pending, Confirmed }

object Matches : Table("matches") {
    val id = uuid("id").autoGenerate()
    val player1Id = reference("player1_id", Users.id)
    val player2Id = reference("player2_id", Users.id)
    val locationId = reference("location_id", Locations.id).nullable()

    val player1Score = integer("player1_score")
    val player2Score = integer("player2_score")
    val status = enumerationByName("status", 20, MatchStatus::class).default(MatchStatus.Pending)

    val playedAt = timestamp("played_at")

    override val primaryKey = PrimaryKey(id)
}