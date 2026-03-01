package org.ttproject.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.ttproject.SERVER_IP
import org.ttproject.data.LoginRequest
import org.ttproject.data.TokenResponse
import org.ttproject.data.TokenStorage

// 1. The Interface
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
}

// 2. The Implementation (where Ktor lives)
class AuthRepositoryImpl(
    private val httpClient: HttpClient, // Injected by Koin
    private val tokenStorage: TokenStorage // Injected by Koin
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val cleanEmail = email.trim()
            // Point this to your Google Cloud IP!
            val response = httpClient.post("${SERVER_IP}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(cleanEmail, password))
            }

            if (response.status.isSuccess()) {
                // Parse the JSON token from your Ktor backend
                val tokenResponse = response.body<TokenResponse>()
                tokenStorage.saveToken(tokenResponse.token)
                Result.success("Logged in perfectly!")
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}