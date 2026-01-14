package gov.census.cspro.util

/**
 * Credential storage (web equivalent of Android SharedPreferences)
 * Ported from Android CredentialStore
 */
expect class CredentialStore {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
    fun clear()
    fun contains(key: String): Boolean
}
