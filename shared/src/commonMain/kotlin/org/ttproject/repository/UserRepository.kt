package org.ttproject.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.ttproject.SERVER_IP
import org.ttproject.data.TokenStorage
import org.ttproject.data.UpdateLanguageRequest
import org.ttproject.data.UpdateProfileRequest
import org.ttproject.data.UserProfile

// 1. The Interface
interface UserRepository {
    suspend fun getMyProfile(): UserProfile
    suspend fun getUserProfile(username: String): Result<UserProfile>
    suspend fun updateProfile(
        name: String, blade: String, forehand: String, backhand: String,
        bio: String?, birthDate: String?, skillLevel: String? // 👈 ADDED
    ): Result<Boolean>
    suspend fun updateLanguage(language: String): Result<Boolean>
    suspend fun uploadProfileImage(imageBytes: ByteArray): Result<Boolean>
}

// 2. The Implementation
class UserRepositoryImpl(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : UserRepository {

    override suspend fun getMyProfile(): UserProfile {
        val token = tokenStorage.getToken()
            ?: throw Exception("No auth token found! User should be logged out.")

        val response = httpClient.get("${SERVER_IP}/api/users/me") {
            bearerAuth(token)
        }

        if (response.status.value in 200..299) {
            val userProfile: UserProfile = response.body()
            tokenStorage.saveUserId(userProfile.id)
            return userProfile
        } else if (response.status.value == 401) {
            throw Exception("Session expired. Please log in again.")
        } else {
            throw Exception("Server error: ${response.status.description}")
        }
    }

    override suspend fun getUserProfile(username: String): Result<UserProfile> {
        return try {
            val token = tokenStorage.getToken() ?: throw Exception("No auth token")

            val response = httpClient.get("${SERVER_IP}/api/users/profile/$username") {
                bearerAuth(token)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch profile. Server returned: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        name: String, blade: String, forehand: String, backhand: String,
        bio: String?, birthDate: String?, skillLevel: String?
    ): Result<Boolean> {
        return try {
            val response = httpClient.put("${SERVER_IP}/api/users/me") {
                contentType(ContentType.Application.Json)
                // 👇 Pass the new variables into the Request object
                setBody(UpdateProfileRequest(name, blade, forehand, backhand, bio, birthDate, skillLevel))
                bearerAuth(tokenStorage.getToken()!!)
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update profile. Server returned: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateLanguage(language: String): Result<Boolean> {
        return try {
            val response = httpClient.put("${SERVER_IP}/api/users/language") {
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

    // 👇 New implementation for sending the image via Multipart Form Data
    override suspend fun uploadProfileImage(imageBytes: ByteArray): Result<Boolean> {
        return try {
            val token = tokenStorage.getToken() ?: throw Exception("No auth token")

            val response = httpClient.post("${SERVER_IP}/api/users/profile-image") {
                bearerAuth(token)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("image", imageBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                // The filename doesn't matter too much here since the server assigns a new one
                                append(HttpHeaders.ContentDisposition, "filename=\"profile_pic.jpg\"")
                            })
                        }
                    )
                )
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Upload failed. Server returned: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}