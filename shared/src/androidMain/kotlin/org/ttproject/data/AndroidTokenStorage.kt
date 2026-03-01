package org.ttproject.data

import android.content.Context
import com.liftric.kvault.KVault

class AndroidTokenStorage(context: Context) : TokenStorage {
    // KVault wraps Android's EncryptedSharedPreferences automatically!
    private val vault = KVault(context, "match_app_secure_prefs")
    private val key = "jwt_token"

    override fun saveToken(token: String) { vault.set(key, token) }
    override fun getToken(): String? = vault.string(key)
    override fun clearToken() { vault.deleteObject(key) }
}