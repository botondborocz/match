package org.ttproject.repository

import org.ttproject.data.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.ttproject.SERVER_IP
import org.ttproject.data.PlayerResponse
import org.ttproject.data.TokenStorage
import org.ttproject.data.UpdateLanguageRequest

// 1. The Interface (The Blueprint)
interface UserRepository {
    suspend fun getMyProfile(): UserProfile
    suspend fun updateLanguage(language: String): Result<Boolean>
}

// 2. The Implementation (The actual Ktor network calls)
class UserRepositoryImpl(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : UserRepository {

    override suspend fun getMyProfile(): UserProfile {
        // Step 1: Grab the saved token
        val token = tokenStorage.getToken()
            ?: throw Exception("No auth token found! User should be logged out.")

        // Step 2: Make the GET request to your /me endpoint
        val response = httpClient.get("${SERVER_IP}/users/me") {
            // 👇 THIS IS THE CRITICAL LINE! It attaches your token to the request.
            bearerAuth(tokenStorage.getToken()!!)
        }

        // Step 3: Check if the server accepted it
        if (response.status.value in 200..299) {
            // Success! Ktor automatically parses the JSON into your UserProfile class
            return response.body()
        } else if (response.status.value == 401) {
            // 401 means the token expired or is invalid
            throw Exception("Session expired. Please log in again.")
        } else {
            throw Exception("Server error: ${response.status.description}")
        }
    }

    override suspend fun updateLanguage(language: String): Result<Boolean> {
        return try {
            val response = httpClient.put("${SERVER_IP}/users/language") {
                contentType(ContentType.Application.Json)
                setBody(UpdateLanguageRequest(language))
                bearerAuth(tokenStorage.getToken()!!)
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update language. Server returned: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}