package gov.census.cspro.platform

import kotlin.js.JsString
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SQLite-based Persistent Settings Store for CSPro WASM
 * 
 * This provides persistent settings storage using SQLite in OPFS (Origin Private File System),
 * matching the native CSPro behavior with CommonStore.db database.
 * 
 * Tables:
 * - UserSettings: For loadsetting/savesetting functions
 * - Configurations: For config variables
 * - PersistentVariables: For persistent variables
 * - CS_*: Custom ActionInvoker settings tables
 * 
 * Mirrors: Native CSPro CommonStore class (zUtilO/CommonStore.cpp)
 */

// Top-level external JS interop functions for WASM compatibility
@JsFun("() => window.sqliteSettingsStore")
private external fun getSettingsStore(): JsAny?

@JsFun("(store) => store.initialize()")
private external fun storeInitialize(store: JsAny): JsAny // Returns Promise

@JsFun("(store, key, source) => store.getValue(key, source)")
private external fun storeGetValue(store: JsAny, key: JsString, source: JsString): JsAny // Returns Promise

@JsFun("(store, key, value, source) => store.putValue(key, value, source)")
private external fun storePutValue(store: JsAny, key: JsString, value: JsString, source: JsString): JsAny // Returns Promise

@JsFun("(store, key, source) => store.deleteValue(key, source)")
private external fun storeDeleteValue(store: JsAny, key: JsString, source: JsString): JsAny // Returns Promise

@JsFun("(store, key, source) => store.contains(key, source)")
private external fun storeContains(store: JsAny, key: JsString, source: JsString): JsAny // Returns Promise

@JsFun("(store, source) => store.getAllKeys(source)")
private external fun storeGetAllKeys(store: JsAny, source: JsString): JsAny // Returns Promise

@JsFun("(store, source) => store.clear(source)")
private external fun storeClear(store: JsAny, source: JsString): JsAny // Returns Promise

@JsFun("(store, tableName) => store.createTable(tableName)")
private external fun storeCreateTable(store: JsAny, tableName: JsString): JsAny // Returns Promise

@JsFun("(store, sql) => store.exec(sql)")
private external fun storeExec(store: JsAny, sql: JsString): JsAny // Returns Promise

@JsFun("(store) => store.close()")
private external fun storeClose(store: JsAny): Unit

@JsFun("(promise, resolve, reject) => promise.then(resolve).catch(reject)")
private external fun promiseThenCatch(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?): Unit

@JsFun("(arr) => arr ? arr.length : 0")
private external fun jsArrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun jsArrayGet(arr: JsAny, i: Int): JsAny?

@JsFun("(s) => s")
private external fun toJsString(s: String): JsString

@JsFun("(s) => s")
private external fun jsStringToKotlin(s: JsString): String

// Helper suspend function to await a JS Promise
private suspend fun <T : JsAny?> awaitPromise(promise: JsAny): T {
    return suspendCancellableCoroutine { continuation ->
        promiseThenCatch(
            promise,
            { result: JsAny? ->
                @Suppress("UNCHECKED_CAST")
                continuation.resume(result as T)
                null
            },
            { error: JsAny ->
                continuation.resumeWithException(Exception("JS Promise rejected: $error"))
                null
            }
        )
    }
}

object PersistentSettingsStore {
    
    private var initialized = false
    
    /**
     * Table types matching native CSPro CommonStore
     */
    enum class TableType(val tableName: String) {
        UserSettings("UserSettings"),
        ConfigVariables("Configurations"),
        PersistentVariables("PersistentVariables")
    }
    
    /**
     * Initialize the SQLite settings store
     */
    suspend fun initialize(): Boolean {
        if (initialized) return true
        
        return try {
            val store = getSettingsStore()
            if (store != null) {
                val result = awaitPromise<JsAny?>(storeInitialize(store))
                initialized = true
                println("[PersistentSettingsStore] SQLite settings store initialized")
                true
            } else {
                println("[PersistentSettingsStore] SQLite store not available")
                false
            }
        } catch (e: Exception) {
            println("[PersistentSettingsStore] Initialization failed: ${e.message}")
            false
        }
    }
    
    /**
     * Check if store is available
     */
    fun isAvailable(): Boolean {
        return getSettingsStore() != null
    }
    
    /**
     * Get a string value from settings
     * @param key The setting key
     * @param defaultValue Default value if not found
     * @param source Table name (source), defaults to UserSettings
     */
    suspend fun getString(key: String, defaultValue: String = "", source: String = TableType.UserSettings.tableName): String {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return defaultValue
        
        return try {
            val result = awaitPromise<JsAny?>(storeGetValue(store, toJsString(key), toJsString(source)))
            if (result != null) {
                jsStringToKotlin(result.unsafeCast<JsString>())
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            println("[PersistentSettingsStore] getString error: ${e.message}")
            defaultValue
        }
    }
    
    /**
     * Get an integer value from settings
     */
    suspend fun getInt(key: String, defaultValue: Int = 0, source: String = TableType.UserSettings.tableName): Int {
        val str = getString(key, "", source)
        return str.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Get a boolean value from settings
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false, source: String = TableType.UserSettings.tableName): Boolean {
        val str = getString(key, "", source)
        return when (str.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> defaultValue
        }
    }
    
    /**
     * Get a float value from settings
     */
    suspend fun getFloat(key: String, defaultValue: Float = 0f, source: String = TableType.UserSettings.tableName): Float {
        val str = getString(key, "", source)
        return str.toFloatOrNull() ?: defaultValue
    }
    
    /**
     * Get a double value from settings
     */
    suspend fun getDouble(key: String, defaultValue: Double = 0.0, source: String = TableType.UserSettings.tableName): Double {
        val str = getString(key, "", source)
        return str.toDoubleOrNull() ?: defaultValue
    }
    
    /**
     * Set a string value in settings
     * @param key The setting key
     * @param value The value to store
     * @param source Table name (source), defaults to UserSettings
     */
    suspend fun setString(key: String, value: String, source: String = TableType.UserSettings.tableName): Boolean {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return false
        
        return try {
            awaitPromise<JsAny?>(storePutValue(store, toJsString(key), toJsString(value), toJsString(source)))
            true
        } catch (e: Exception) {
            println("[PersistentSettingsStore] setString error: ${e.message}")
            false
        }
    }
    
    /**
     * Set an integer value in settings
     */
    suspend fun setInt(key: String, value: Int, source: String = TableType.UserSettings.tableName): Boolean {
        return setString(key, value.toString(), source)
    }
    
    /**
     * Set a boolean value in settings
     */
    suspend fun setBoolean(key: String, value: Boolean, source: String = TableType.UserSettings.tableName): Boolean {
        return setString(key, value.toString(), source)
    }
    
    /**
     * Set a float value in settings
     */
    suspend fun setFloat(key: String, value: Float, source: String = TableType.UserSettings.tableName): Boolean {
        return setString(key, value.toString(), source)
    }
    
    /**
     * Set a double value in settings
     */
    suspend fun setDouble(key: String, value: Double, source: String = TableType.UserSettings.tableName): Boolean {
        return setString(key, value.toString(), source)
    }
    
    /**
     * Delete a setting
     */
    suspend fun delete(key: String, source: String = TableType.UserSettings.tableName): Boolean {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return false
        
        return try {
            awaitPromise<JsAny?>(storeDeleteValue(store, toJsString(key), toJsString(source)))
            true
        } catch (e: Exception) {
            println("[PersistentSettingsStore] delete error: ${e.message}")
            false
        }
    }
    
    /**
     * Check if a setting exists
     */
    suspend fun contains(key: String, source: String = TableType.UserSettings.tableName): Boolean {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return false
        
        return try {
            val result = awaitPromise<JsAny?>(storeContains(store, toJsString(key), toJsString(source)))
            result != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all keys from a table
     */
    suspend fun getAllKeys(source: String = TableType.UserSettings.tableName): List<String> {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return emptyList()
        
        return try {
            val keys = awaitPromise<JsAny>(storeGetAllKeys(store, toJsString(source)))
            val result = mutableListOf<String>()
            val length = jsArrayLength(keys)
            for (i in 0 until length) {
                val key = jsArrayGet(keys, i)
                if (key != null) {
                    result.add(jsStringToKotlin(key.unsafeCast<JsString>()))
                }
            }
            result
        } catch (e: Exception) {
            println("[PersistentSettingsStore] getAllKeys error: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Clear all settings in a table
     */
    suspend fun clear(source: String = TableType.UserSettings.tableName): Boolean {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return false
        
        return try {
            awaitPromise<JsAny?>(storeClear(store, toJsString(source)))
            true
        } catch (e: Exception) {
            println("[PersistentSettingsStore] clear error: ${e.message}")
            false
        }
    }
    
    /**
     * Create a custom table (for CS_ prefixed ActionInvoker tables)
     */
    suspend fun createTable(tableName: String): Boolean {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return false
        
        return try {
            awaitPromise<JsAny?>(storeCreateTable(store, toJsString(tableName)))
            true
        } catch (e: Exception) {
            println("[PersistentSettingsStore] createTable error: ${e.message}")
            false
        }
    }
    
    /**
     * Execute raw SQL (for advanced use)
     */
    suspend fun execSql(sql: String): Boolean {
        if (!initialized) initialize()
        
        val store = getSettingsStore() ?: return false
        
        return try {
            awaitPromise<JsAny?>(storeExec(store, toJsString(sql)))
            true
        } catch (e: Exception) {
            println("[PersistentSettingsStore] execSql error: ${e.message}")
            false
        }
    }
    
    /**
     * Close the database
     */
    fun close() {
        try {
            val store = getSettingsStore()
            if (store != null) {
                storeClose(store)
            }
            initialized = false
        } catch (e: Exception) {
            println("[PersistentSettingsStore] close error: ${e.message}")
        }
    }
}
