package org.ttproject.database

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.ttproject.database.tables.Connections
import org.ttproject.database.tables.LocationType
import org.ttproject.database.tables.Locations
import org.ttproject.database.tables.Messages
import org.ttproject.database.tables.PlayStyle
import org.ttproject.database.tables.SkillLevel
import org.ttproject.database.tables.Users
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

fun insertDummyData() {
    transaction {
        // Only insert if the database is empty
        if (Users.selectAll().empty()) {

            // 1. Insert Users
            println("🌱 Seeding database with 100 dummy players around Budapest...")

            // Hash the password ONCE for speed
            val defaultPassword = BCrypt.hashpw("password123", BCrypt.gensalt())
            val skills = listOf(SkillLevel.Beginner, SkillLevel.Intermediate, SkillLevel.Advanced, SkillLevel.Pro)

            // Budapest Center Coordinates
            val centerLat = 47.4979
            val centerLng = 19.0402

            for (i in 1..10) {
                Users.insert {
                    it[id] = UUID.randomUUID()
                    it[email] = "player$i@match.com"
                    it[passwordHash] = defaultPassword
                    it[username] = "player_$i"
                    it[fullName] = "Table Tennis Player $i"
                    it[skillLevel] = skills.random()
                    it[eloRating] = Random.nextInt(800, 2000)

                    // Scatter them within roughly ~20km of Budapest center
                    // 0.1 degree is roughly 11km
                    it[lastLat] = centerLat + Random.nextDouble(-0.2, 0.2)
                    it[lastLng] = centerLng + Random.nextDouble(-0.2, 0.2)
                    it[createdAt] = java.time.Instant.now()
                }
            }
            println("✅ 100 Dummy users successfully injected!")

            // 2. Insert a Location
            Locations.insert {
                it[name] = "Városligeti asztalok"
                it[type] = LocationType.Outdoor
                it[latitude] = 47.5133
                it[longitude] = 19.0835
                it[address] = "Budapest, Városliget"
                it[isFree] = true
                it[tableCount] = 4
            }

//            // 3. Create a Connection (Match)
//            val connectionId = Connections.insert {
//                it[this.user1Id] = user1Id
//                it[this.user2Id] = user2Id
//                it[createdAt] = Instant.now()
//            } get Connections.id
//
//            // 4. Send a Message
//            Messages.insert {
//                it[this.connectionId] = connectionId
//                it[senderId] = user1Id
//                it[content] = "Szia Anna! Tolunk egy meccset a Ligetben?"
//                it[createdAt] = Instant.now()
//            }

            println("✅ Dummy data successfully injected!")
        }
    }
}