package org.ttproject.data

import com.liftric.kvault.KVault

class IosTokenStorage : TokenStorage {
    // KVault wraps the iOS Keychain automatically!
    private val vault = KVault()
    private val key = "jwt_token"

    override fun saveToken(token: String) { vault.set(key, token) }
    override fun getToken(): String? = vault.string(key)
    override fun clearToken() { vault.deleteObject(key) }
    override fun saveLanguage(languageCode: String) { vault.set("language", languageCode) }
    override fun getLanguage(): String? = vault.string("language")
    override fun clearLanguage() { vault.deleteObject("language") }
    override fun saveThemeMode(mode: String) { vault.set("theme_mode", mode) }
    override fun getThemeMode(): String = vault.string("theme_mode") ?: "system"
}