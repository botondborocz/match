package org.ttproject

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.ttproject.database.initDatabase
import org.ttproject.database.tables.Locations.name
import org.ttproject.database.tables.Users
import kotlin.collections.map

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    initDatabase()
    routing {
        get("/") {
            call.respondText("Ktor: hoppá")
        }
        get("/ping") {
            call.respondText("pong")
        }
        post("/users") {
            val newName = call.receiveText()

            transaction {
                Users.insert {
                    it[username] = newName
                }
            }
            call.respondText("User '$newName' saved to the database!")
        }

        get("/users") {
            val allUsers = transaction {
                Users.selectAll().map { it[Users.username] }
            }
            call.respondText("Users in DB: $allUsers")
        }
    }
}