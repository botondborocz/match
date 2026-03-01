package org.ttproject.data

import com.liftric.kvault.KVault

class IosTokenStorage : TokenStorage {
    // KVault wraps the iOS Keychain automatically!
    private val vault = KVault()
    private val key = "jwt_token"

    override fun saveToken(token: String) { vault.set(key, token) }
    override fun getToken(): String? = vault.string(key)
    override fun clearToken() { vault.deleteObject(key) }
}