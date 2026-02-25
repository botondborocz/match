package org.ttproject.database

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.ttproject.database.tables.Connections
import org.ttproject.database.tables.LocationType
import org.ttproject.database.tables.Locations
import org.ttproject.database.tables.Messages
import org.ttproject.database.tables.PlayStyle
import org.ttproject.database.tables.SkillLevel
import org.ttproject.database.tables.Users
import java.time.Instant

fun insertDummyData() {
    transaction {
        // Only insert if the database is empty
        if (Users.selectAll().empty()) {

            // 1. Insert Users
            val user1Id = Users.insert {
                it[username] = "bence_pingpong"
                it[fullName] = "Bence Kovács"
                it[skillLevel] = SkillLevel.Intermediate
                it[eloRating] = 1350
                it[playStyle] = PlayStyle.Offensive
                it[gearBlade] = "Butterfly Viscaria"
                it[lastLat] = 47.4979
                it[lastLng] = 19.0402
                it[createdAt] = Instant.now()
            } get Users.id

            val user2Id = Users.insert {
                it[username] = "anna_spin"
                it[fullName] = "Anna Varga"
                it[skillLevel] = SkillLevel.Advanced
                it[eloRating] = 1500
                it[playStyle] = PlayStyle.AllRound
                it[lastLat] = 47.5000
                it[lastLng] = 19.0500
                it[createdAt] = Instant.now()
            } get Users.id

            // 2. Insert a Location
            Locations.insert {
                it[name] = "Városligeti asztalok"
                it[type] = LocationType.Outdoor
                it[latitude] = 47.5133
                it[longitude] = 19.0835
                it[address] = "Budapest, Városliget"
                it[isFree] = true
                it[tableCount] = 4
                it[createdBy] = user1Id
            }

            // 3. Create a Connection (Match)
            val connectionId = Connections.insert {
                it[this.user1Id] = user1Id
                it[this.user2Id] = user2Id
                it[createdAt] = Instant.now()
            } get Connections.id

            // 4. Send a Message
            Messages.insert {
                it[this.connectionId] = connectionId
                it[senderId] = user1Id
                it[content] = "Szia Anna! Tolunk egy meccset a Ligetben?"
                it[createdAt] = Instant.now()
            }

            println("✅ Dummy data successfully injected!")
        }
    }
}