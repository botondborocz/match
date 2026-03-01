package org.ttproject.data

// We expect the Android and iOS folders to provide the actual code for this later!
interface TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}