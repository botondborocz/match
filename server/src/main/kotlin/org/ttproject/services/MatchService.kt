package org.ttproject.server.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.ttproject.database.tables.Connections
import org.ttproject.database.tables.Matches
import org.ttproject.database.tables.Swipes
import java.time.Instant
import java.util.UUID

class MatchService {

    fun processSwipe(swiperId: String, targetId: String, isLiked: Boolean): Boolean {
        // 1. SAVE THE SWIPE TO THE REAL DATABASE
        transaction {
            // Upsert / Insert ignore to prevent crashes if they somehow swipe twice
            Swipes.insertIgnore {
                it[Swipes.swiperId] = UUID.fromString(swiperId)
                it[swipedId] = UUID.fromString(targetId)
                it[Swipes.isLiked] = isLiked
                it[createdAt] = Instant.now()
            }
        }

        // 2. IF IT WAS A PASS, STOP HERE
        if (!isLiked) return false

        // 3. IF IT WAS A LIKE, CHECK THE DB FOR A MUTUAL MATCH
        val isMutualMatch = transaction {
            // SELECT * FROM swipes WHERE swiper_id = targetId AND target_id = swiperId AND is_liked = true
            Swipes.selectAll().where {
                (Swipes.swiperId eq UUID.fromString(targetId)) and
                        (Swipes.swipedId eq UUID.fromString(swiperId)) and
                        (Swipes.isLiked eq true)
            }.count() > 0
        }

        // 4. IF MATCH, CREATE A PERMANENT CHAT/MATCH RECORD
        if (isMutualMatch) {
            transaction {
                Connections.insertIgnore {
                    // Always sort the IDs alphabetically so User A + User B is the same row as User B + User A
                    val (u1, u2) = listOf(swiperId, targetId).sorted()
                    it[this.user1Id] = UUID.fromString(u1)
                    it[this.user2Id] = UUID.fromString(u2)
                    it[this.createdAt] = Instant.now()
                }
            }
            println("REAL DB MATCH CREATED between $swiperId and $targetId!")
        }

        return isMutualMatch
    }
}