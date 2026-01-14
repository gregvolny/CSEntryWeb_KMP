package gov.census.cspro.util

import kotlinx.browser.localStorage

/**
 * WASM implementation using localStorage for persistent credential storage
 * 
 * SQLite integration is handled at the JavaScript layer via sqlite-settings-store.js.
 * This Kotlin class uses localStorage as the synchronous storage mechanism,
 * which is then synchronized to SQLite by the JavaScript layer.
 * 
 * Note: The ActionInvoker Settings namespace handles the actual SQLite persistence.
 */
actual class CredentialStore {
    
    private val keyPrefix = "cspro_cred_"
    
    actual fun getString(key: String, defaultValue: String): String {
        return try {
            localStorage.getItem("$keyPrefix$key") ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    actual fun putString(key: String, value: String) {
        try {
            localStorage.setItem("$keyPrefix$key", value)
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }
    
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val value = getString(key, "")
        return if (value.isEmpty()) defaultValue else value.toBoolean()
    }
    
    actual fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }
    
    actual fun getInt(key: String, defaultValue: Int): Int {
        val value = getString(key, "")
        return if (value.isEmpty()) defaultValue else value.toIntOrNull() ?: defaultValue
    }
    
    actual fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }
    
    actual fun remove(key: String) {
        try {
            localStorage.removeItem("$keyPrefix$key")
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }
    
    actual fun clear() {
        try {
            // Get all keys with our prefix and remove them
            val keysToRemove = mutableListOf<String>()
            for (i in 0 until localStorage.length) {
                val key = localStorage.key(i)
                if (key != null && key.startsWith(keyPrefix)) {
                    keysToRemove.add(key)
                }
            }
            keysToRemove.forEach { key ->
                localStorage.removeItem(key)
            }
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }
    
    actual fun contains(key: String): Boolean {
        return try {
            localStorage.getItem("$keyPrefix$key") != null
        } catch (e: Exception) {
            false
        }
    }
}
