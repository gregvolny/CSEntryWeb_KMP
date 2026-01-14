package gov.census.cspro.platform

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * System Settings Manager - WASM implementation with SQLite OPFS persistence
 * 
 * Provides system settings storage using:
 * - SQLite database in OPFS (Origin Private File System) for persistent storage
 * - Matches native CSPro CommonStore.db behavior
 * - Compatible with CSPro loadsetting/savesetting and ActionInvoker Settings namespace
 * 
 * Mirrors: Android's Settings.Secure / SharedPreferences / Native CommonStore
 */

// Top-level JS interop functions for WASM compatibility
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

@JsFun("() => new Date().toLocaleDateString()")
private external fun jsGetDateString(): String

@JsFun("() => new Date().toLocaleTimeString()")
private external fun jsGetTimeString(): String

// OPFS File System API types - using CSPro prefix to avoid conflicts
external interface CSProFileSystemDirectoryHandle : JsAny
external interface CSProFileSystemFileHandle : JsAny
external interface CSProFile : JsAny
external interface CSProFileSystemWritableFileStream : JsAny

@JsFun("() => navigator.storage.getDirectory()")
private external fun jsGetStorageDirectory(): Promise<CSProFileSystemDirectoryHandle>

@JsFun("(dir, name, create) => dir.getDirectoryHandle(name, { create: create })")
private external fun jsGetDirectoryHandle(dir: CSProFileSystemDirectoryHandle, name: String, create: Boolean): Promise<CSProFileSystemDirectoryHandle>

@JsFun("(dir, name, create) => dir.getFileHandle(name, { create: create })")
private external fun jsGetFileHandle(dir: CSProFileSystemDirectoryHandle, name: String, create: Boolean): Promise<CSProFileSystemFileHandle>

@JsFun("(handle) => handle.getFile()")
private external fun jsGetFile(handle: CSProFileSystemFileHandle): Promise<CSProFile>

@JsFun("(file) => file.text()")
private external fun jsFileText(file: CSProFile): Promise<JsString>

@JsFun("(handle) => handle.createWritable()")
private external fun jsCreateWritable(handle: CSProFileSystemFileHandle): Promise<CSProFileSystemWritableFileStream>

@JsFun("(writable, content) => writable.write(content)")
private external fun jsWritableWrite(writable: CSProFileSystemWritableFileStream, content: String): Promise<JsAny?>

@JsFun("(writable) => writable.close()")
private external fun jsWritableClose(writable: CSProFileSystemWritableFileStream): Promise<JsAny?>

@JsFun("(dir, name) => dir.removeEntry(name)")
private external fun jsRemoveEntry(dir: CSProFileSystemDirectoryHandle, name: String): Promise<JsAny?>

@JsFun("(dir) => dir.entries()")
private external fun jsGetEntries(dir: CSProFileSystemDirectoryHandle): JsAny

@JsFun("(iterator) => iterator.next()")
private external fun jsIteratorNext(iterator: JsAny): Promise<JsAny>

@JsFun("(result) => result.done")
private external fun jsGetIteratorDone(result: JsAny): Boolean

@JsFun("(result) => result.value")
private external fun jsGetIteratorValue(result: JsAny): JsAny?

@JsFun("(entry) => entry[0]")
private external fun jsGetEntryName(entry: JsAny): String

@JsFun("(entry) => entry[1].kind")
private external fun jsGetEntryKind(entry: JsAny): String

@JsFun("(url, options) => fetch(url, options)")
private external fun jsFetch(url: String, options: JsAny): Promise<JsAny>

@JsFun("() => ({ mode: 'no-cors', cache: 'no-cache' })")
private external fun jsCreateFetchOptions(): JsAny

@JsFun("(response) => response.text()")
private external fun jsResponseText(response: JsAny): Promise<JsString>

object SystemSettings {
    
    // Source table name matching native behavior
    private const val TABLE_NAME = "SystemSettings"
    
    // Cache for fast synchronous reads
    private var settingsCache: MutableMap<String, String>? = null
    private var initialized = false
    
    /**
     * Initialize the settings system with SQLite backing
     */
    suspend fun initialize() {
        if (initialized) return
        
        try {
            // Initialize the SQLite persistent settings store
            PersistentSettingsStore.initialize()
            
            // Load settings into cache for fast access
            loadSettingsFromSqlite()
            
            initialized = true
            println("[SystemSettings] Initialized with SQLite backend, ${settingsCache?.size ?: 0} settings loaded")
        } catch (e: Exception) {
            println("[SystemSettings] Error initializing: ${e.message}")
            settingsCache = mutableMapOf()
            initialized = true
        }
    }
    
    /**
     * Get a string setting
     */
    suspend fun getString(key: String, defaultValue: String = ""): String {
        if (!initialized) initialize()
        
        // Try cache first
        settingsCache?.get(key)?.let { return it }
        
        // Fall back to SQLite
        return PersistentSettingsStore.getString(key, defaultValue, TABLE_NAME)
    }
    
    /**
     * Get an integer setting
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        if (!initialized) initialize()
        return settingsCache?.get(key)?.toIntOrNull() 
            ?: PersistentSettingsStore.getInt(key, defaultValue, TABLE_NAME)
    }
    
    /**
     * Get a boolean setting
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        if (!initialized) initialize()
        return settingsCache?.get(key)?.toBooleanStrictOrNull() 
            ?: PersistentSettingsStore.getBoolean(key, defaultValue, TABLE_NAME)
    }
    
    /**
     * Get a float setting
     */
    suspend fun getFloat(key: String, defaultValue: Float = 0f): Float {
        if (!initialized) initialize()
        return settingsCache?.get(key)?.toFloatOrNull() 
            ?: PersistentSettingsStore.getFloat(key, defaultValue, TABLE_NAME)
    }
    
    /**
     * Set a string setting (persisted to SQLite)
     */
    suspend fun setString(key: String, value: String): Boolean {
        if (!initialized) initialize()
        settingsCache?.set(key, value)
        return PersistentSettingsStore.setString(key, value, TABLE_NAME)
    }
    
    /**
     * Set an integer setting
     */
    suspend fun setInt(key: String, value: Int): Boolean {
        return setString(key, value.toString())
    }
    
    /**
     * Set a boolean setting
     */
    suspend fun setBoolean(key: String, value: Boolean): Boolean {
        return setString(key, value.toString())
    }
    
    /**
     * Set a float setting
     */
    suspend fun setFloat(key: String, value: Float): Boolean {
        return setString(key, value.toString())
    }
    
    /**
     * Remove a setting
     */
    suspend fun remove(key: String): Boolean {
        if (!initialized) initialize()
        settingsCache?.remove(key)
        return PersistentSettingsStore.delete(key, TABLE_NAME)
    }
    
    /**
     * Check if a setting exists
     */
    suspend fun contains(key: String): Boolean {
        if (!initialized) initialize()
        return settingsCache?.containsKey(key) == true || PersistentSettingsStore.contains(key, TABLE_NAME)
    }
    
    /**
     * Get all settings keys
     */
    suspend fun getAllKeys(): Set<String> {
        if (!initialized) initialize()
        return PersistentSettingsStore.getAllKeys(TABLE_NAME).toSet()
    }
    
    /**
     * Clear all settings
     */
    suspend fun clear(): Boolean {
        if (!initialized) initialize()
        settingsCache?.clear()
        return PersistentSettingsStore.clear(TABLE_NAME)
    }
    
    // ============================================================
    // CSPro-specific settings
    // ============================================================
    
    object CSPro {
        // Operator settings
        const val OPERATOR_ID = "cspro.operator.id"
        const val OPERATOR_NAME = "cspro.operator.name"
        
        // Application settings
        const val LAST_APPLICATION = "cspro.app.last"
        const val AUTO_ADVANCE = "cspro.app.autoAdvance"
        const val SHOW_CODES = "cspro.app.showCodes"
        const val SHOW_CASE_TREE = "cspro.app.showCaseTree"
        
        // Sync settings
        const val SYNC_SERVER_URL = "cspro.sync.serverUrl"
        const val SYNC_DEVICE_ID = "cspro.sync.deviceId"
        const val SYNC_LAST_SYNC = "cspro.sync.lastSync"
        
        // UI settings
        const val THEME = "cspro.ui.theme"
        const val LANGUAGE = "cspro.ui.language"
        const val FONT_SIZE = "cspro.ui.fontSize"
        
        // GPS settings
        const val GPS_ACCURACY = "cspro.gps.accuracy"
        const val GPS_TIMEOUT = "cspro.gps.timeout"
        
        // Paradata settings
        const val PARADATA_ENABLED = "cspro.paradata.enabled"
        const val PARADATA_GPS_INTERVAL = "cspro.paradata.gpsInterval"
    }
    
    // ============================================================
    // Private implementation - SQLite-backed
    // ============================================================
    
    /**
     * Load settings from SQLite into cache
     */
    private suspend fun loadSettingsFromSqlite() {
        settingsCache = mutableMapOf()
        
        try {
            val keys = PersistentSettingsStore.getAllKeys(TABLE_NAME)
            for (key in keys) {
                val value = PersistentSettingsStore.getString(key, "", TABLE_NAME)
                if (value.isNotEmpty()) {
                    settingsCache?.set(key, value)
                }
            }
            println("[SystemSettings] Loaded ${settingsCache?.size} settings from SQLite")
        } catch (e: Exception) {
            println("[SystemSettings] Error loading from SQLite: ${e.message}")
        }
    }
}

/**
 * Exec System Function - WASM implementation
 * 
 * Provides exec() functionality with OPFS virtual file system
 * Supports safe commands that work in browser context
 * 
 * Mirrors: Android's Runtime.exec() / ProcessBuilder
 */
object ExecSystem {
    
    /**
     * Execute a command
     * Web browsers cannot execute arbitrary system commands for security
     * This provides safe alternatives for common operations
     */
    suspend fun exec(command: String, wait: Boolean = true): ExecResult {
        println("[ExecSystem] Executing: $command (wait=$wait)")
        
        return try {
            val parts = command.trim().split(" ", limit = 2)
            val cmd = parts[0].lowercase()
            val args = if (parts.size > 1) parts[1] else ""
            
            when (cmd) {
                // File system commands (via OPFS)
                "ls", "dir" -> listDirectory(args)
                "cat", "type" -> readFile(args)
                "echo" -> echoCommand(args)
                "mkdir", "md" -> makeDirectory(args)
                "rm", "del" -> deleteFile(args)
                "cp", "copy" -> copyFile(args)
                "mv", "move" -> moveFile(args)
                "touch" -> touchFile(args)
                "pwd" -> getCurrentDirectory()
                
                // Environment commands
                "env", "set" -> getEnvironment(args)
                "date" -> getDate()
                "time" -> getTime()
                "hostname" -> getHostname()
                
                // CSPro-specific commands
                "cspro" -> csproCommand(args)
                "sync" -> syncCommand(args)
                "export" -> exportCommand(args)
                
                // Network commands
                "ping" -> pingCommand(args)
                "curl", "wget" -> fetchUrl(args)
                
                // Browser commands
                "open", "start" -> openUrl(args)
                "print" -> printPage()
                "screenshot" -> takeScreenshot()
                
                else -> ExecResult(
                    exitCode = 127,
                    stdout = "",
                    stderr = "Command not found: $cmd\nUse 'cspro help' for available commands"
                )
            }
        } catch (e: Exception) {
            ExecResult(
                exitCode = 1,
                stdout = "",
                stderr = "Error executing command: ${e.message}"
            )
        }
    }
    
    // ============================================================
    // File system commands
    // ============================================================
    
    private suspend fun listDirectory(path: String): ExecResult {
        return try {
            val root = jsGetStorageDirectory().await<CSProFileSystemDirectoryHandle>()
            
            val targetDir = if (path.isBlank()) {
                root
            } else {
                var dir = root
                for (part in path.split("/").filter { it.isNotEmpty() }) {
                    dir = jsGetDirectoryHandle(dir, part, false).await<CSProFileSystemDirectoryHandle>()
                }
                dir
            }
            
            val entries = StringBuilder()
            val iterator = jsGetEntries(targetDir)
            
            while (true) {
                val next = jsIteratorNext(iterator).await<JsAny>()
                if (jsGetIteratorDone(next)) break
                val entry = jsGetIteratorValue(next) ?: continue
                val name = jsGetEntryName(entry)
                val kind = jsGetEntryKind(entry)
                entries.appendLine(if (kind == "directory") "$name/" else name)
            }
            
            ExecResult(0, entries.toString(), "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error listing directory: ${e.message}")
        }
    }
    
    private suspend fun readFile(path: String): ExecResult {
        return try {
            val root = jsGetStorageDirectory().await<CSProFileSystemDirectoryHandle>()
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                return ExecResult(1, "", "No file specified")
            }
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                dir = jsGetDirectoryHandle(dir, parts[i], false).await<CSProFileSystemDirectoryHandle>()
            }
            
            val fileHandle = jsGetFileHandle(dir, parts.last(), false).await<CSProFileSystemFileHandle>()
            val file = jsGetFile(fileHandle).await<CSProFile>()
            val content = jsFileText(file).await<JsString>()
            
            ExecResult(0, content.toString(), "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error reading file: ${e.message}")
        }
    }
    
    private fun echoCommand(args: String): ExecResult {
        return ExecResult(0, args, "")
    }
    
    private suspend fun makeDirectory(path: String): ExecResult {
        return try {
            val root = jsGetStorageDirectory().await<CSProFileSystemDirectoryHandle>()
            
            var dir = root
            for (part in path.split("/").filter { it.isNotEmpty() }) {
                dir = jsGetDirectoryHandle(dir, part, true).await<CSProFileSystemDirectoryHandle>()
            }
            
            ExecResult(0, "Directory created: $path", "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error creating directory: ${e.message}")
        }
    }
    
    private suspend fun deleteFile(path: String): ExecResult {
        return try {
            val root = jsGetStorageDirectory().await<CSProFileSystemDirectoryHandle>()
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                return ExecResult(1, "", "No file specified")
            }
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                dir = jsGetDirectoryHandle(dir, parts[i], false).await<CSProFileSystemDirectoryHandle>()
            }
            
            jsRemoveEntry(dir, parts.last()).await<JsAny?>()
            ExecResult(0, "Deleted: $path", "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error deleting file: ${e.message}")
        }
    }
    
    private suspend fun copyFile(args: String): ExecResult {
        val parts = args.split(" ", limit = 2)
        if (parts.size < 2) {
            return ExecResult(1, "", "Usage: cp <source> <destination>")
        }
        
        // Read source
        val readResult = readFile(parts[0])
        if (readResult.exitCode != 0) {
            return readResult
        }
        
        // Write destination
        return try {
            val root = jsGetStorageDirectory().await<CSProFileSystemDirectoryHandle>()
            
            val destParts = parts[1].split("/").filter { it.isNotEmpty() }
            var dir = root
            for (i in 0 until destParts.size - 1) {
                dir = jsGetDirectoryHandle(dir, destParts[i], true).await<CSProFileSystemDirectoryHandle>()
            }
            
            val fileHandle = jsGetFileHandle(dir, destParts.last(), true).await<CSProFileSystemFileHandle>()
            val writable = jsCreateWritable(fileHandle).await<CSProFileSystemWritableFileStream>()
            jsWritableWrite(writable, readResult.stdout).await<JsAny?>()
            jsWritableClose(writable).await<JsAny?>()
            
            ExecResult(0, "Copied ${parts[0]} to ${parts[1]}", "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error copying file: ${e.message}")
        }
    }
    
    private suspend fun moveFile(args: String): ExecResult {
        val copyResult = copyFile(args)
        if (copyResult.exitCode == 0) {
            val source = args.split(" ")[0]
            deleteFile(source)
        }
        return copyResult
    }
    
    private suspend fun touchFile(path: String): ExecResult {
        return try {
            val root = jsGetStorageDirectory().await<CSProFileSystemDirectoryHandle>()
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                return ExecResult(1, "", "No file specified")
            }
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                dir = jsGetDirectoryHandle(dir, parts[i], true).await<CSProFileSystemDirectoryHandle>()
            }
            
            jsGetFileHandle(dir, parts.last(), true).await<CSProFileSystemFileHandle>()
            ExecResult(0, "Created: $path", "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error creating file: ${e.message}")
        }
    }
    
    private fun getCurrentDirectory(): ExecResult {
        return ExecResult(0, "/", "")
    }
    
    // ============================================================
    // Environment commands
    // ============================================================
    
    private fun getEnvironment(args: String): ExecResult {
        val env = buildString {
            appendLine("PLATFORM=Web")
            appendLine("USER_AGENT=${window.navigator.userAgent}")
            appendLine("LANGUAGE=${window.navigator.language}")
            appendLine("ONLINE=${window.navigator.onLine}")
            appendLine("CSPRO_VERSION=1.0.0")
        }
        return ExecResult(0, env, "")
    }
    
    private fun getDate(): ExecResult {
        val date = jsGetDateString()
        return ExecResult(0, date, "")
    }
    
    private fun getTime(): ExecResult {
        val time = jsGetTimeString()
        return ExecResult(0, time, "")
    }
    
    private fun getHostname(): ExecResult {
        val hostname = window.location.hostname
        return ExecResult(0, hostname, "")
    }
    
    // ============================================================
    // CSPro commands
    // ============================================================
    
    private suspend fun csproCommand(args: String): ExecResult {
        val parts = args.split(" ", limit = 2)
        val subCmd = parts.getOrElse(0) { "help" }
        val subArgs = parts.getOrElse(1) { "" }
        
        return when (subCmd) {
            "help" -> ExecResult(0, """
                CSPro Web Commands:
                  cspro help          - Show this help
                  cspro version       - Show version info
                  cspro settings      - List all settings
                  cspro set KEY VALUE - Set a setting
                  cspro get KEY       - Get a setting
                  cspro clear         - Clear all settings
                  
                File Commands:
                  ls [path]           - List directory
                  cat <file>          - Read file
                  mkdir <path>        - Create directory
                  rm <file>           - Delete file
                  cp <src> <dst>      - Copy file
                  mv <src> <dst>      - Move file
                  
                Network Commands:
                  ping <host>         - Ping host
                  curl <url>          - Fetch URL
                  
                Browser Commands:
                  open <url>          - Open URL
                  print               - Print page
            """.trimIndent(), "")
            
            "version" -> ExecResult(0, "CSPro Web 1.0.0 (WASM)", "")
            
            "settings" -> {
                val keys = SystemSettings.getAllKeys()
                val settings = keys.map { "$it = ${SystemSettings.getString(it)}" }.joinToString("\n")
                ExecResult(0, settings, "")
            }
            
            "set" -> {
                val setParts = subArgs.split(" ", limit = 2)
                if (setParts.size < 2) {
                    ExecResult(1, "", "Usage: cspro set KEY VALUE")
                } else {
                    SystemSettings.setString(setParts[0], setParts[1])
                    ExecResult(0, "Set ${setParts[0]} = ${setParts[1]}", "")
                }
            }
            
            "get" -> {
                val value = SystemSettings.getString(subArgs)
                ExecResult(0, value, "")
            }
            
            "clear" -> {
                SystemSettings.clear()
                ExecResult(0, "Settings cleared", "")
            }
            
            else -> ExecResult(1, "", "Unknown command: cspro $subCmd")
        }
    }
    
    private fun syncCommand(args: String): ExecResult {
        return ExecResult(0, "Sync command: $args (not implemented)", "")
    }
    
    private fun exportCommand(args: String): ExecResult {
        return ExecResult(0, "Export command: $args (not implemented)", "")
    }
    
    // ============================================================
    // Network commands
    // ============================================================
    
    private suspend fun pingCommand(host: String): ExecResult {
        return try {
            val startTime = jsDateNow()
            val url = if (host.startsWith("http")) host else "https://$host"
            
            val options = jsCreateFetchOptions()
            jsFetch(url, options).await<JsAny>()
            
            val endTime = jsDateNow()
            val time = endTime - startTime
            
            ExecResult(0, "Reply from $host: time=${time}ms", "")
        } catch (e: Exception) {
            ExecResult(1, "", "Request timeout for $host")
        }
    }
    
    private suspend fun fetchUrl(url: String): ExecResult {
        return try {
            val options = jsCreateFetchOptions()
            val response = jsFetch(url, options).await<JsAny>()
            val text = jsResponseText(response).await<JsString>()
            ExecResult(0, text.toString(), "")
        } catch (e: Exception) {
            ExecResult(1, "", "Error fetching URL: ${e.message}")
        }
    }
    
    // ============================================================
    // Browser commands
    // ============================================================
    
    private fun openUrl(url: String): ExecResult {
        window.open(url, "_blank")
        return ExecResult(0, "Opened: $url", "")
    }
    
    private fun printPage(): ExecResult {
        window.print()
        return ExecResult(0, "Print dialog opened", "")
    }
    
    private fun takeScreenshot(): ExecResult {
        // Would need html2canvas library
        return ExecResult(0, "Screenshot not yet implemented", "")
    }
}

/**
 * Result of exec() command
 */
data class ExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean get() = exitCode == 0
    val output: String get() = if (success) stdout else stderr
}
