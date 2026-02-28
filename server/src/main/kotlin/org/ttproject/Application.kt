package org.ttproject

import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
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
import org.ttproject.routes.authRoutes
import org.ttproject.security.JwtConfig
import kotlin.collections.map
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    initDatabase()

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    // 1. Install the Authentication plugin
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Match App Backend"
            verifier(JwtConfig.verifier)
            validate { credential ->
                // If the token has a userId claim, let them in!
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    routing {
        authRoutes()

        get("/") {
            call.respondText("Ktor Server is Online!")
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

        // 2. The Security Checkpoint! Everything inside here requires a valid token.
        authenticate("auth-jwt") {

            get("/users") {
                // If they made it here, their token is 100% valid.
                // We can even extract their ID from the token like this:
                val principal = call.principal<JWTPrincipal>()
                val myUserId = principal!!.payload.getClaim("userId").asString()

                call.respondText("Welcome to the VIP area! Your ID is: $myUserId")
            }
        }
    }
}