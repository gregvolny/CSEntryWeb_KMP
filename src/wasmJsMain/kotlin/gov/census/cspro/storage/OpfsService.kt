package gov.census.cspro.storage

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ============================================================
// TOP-LEVEL @JsFun EXTERNAL FUNCTIONS FOR WASM INTEROP
// ============================================================

// OPFS Availability Check
@JsFun("() => typeof navigator !== 'undefined' && navigator.storage && typeof navigator.storage.getDirectory === 'function'")
private external fun jsIsOpfsAvailable(): Boolean

// Get OPFS storage root - returns a Promise
@JsFun("() => navigator.storage.getDirectory()")
private external fun jsGetStorageRoot(): JsAny

// Promise handling - with error trapping for debugging
@JsFun("""(promise, resolve, reject) => {
    try {
        promise.then(
            (result) => { try { resolve(result); } catch(e) { console.error('[jsHandlePromise] Error in resolve callback:', e); } },
            (error) => { try { reject(error); } catch(e) { console.error('[jsHandlePromise] Error in reject callback:', e); } }
        );
    } catch(e) {
        console.error('[jsHandlePromise] Error setting up promise:', e);
        try { reject(e); } catch(e2) { console.error('[jsHandlePromise] Error calling reject:', e2); }
    }
}""")
private external fun jsHandlePromise(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?)

// Directory operations
@JsFun("(dir, name, create) => dir.getDirectoryHandle(name, { create: create })")
private external fun jsGetDirectoryHandle(dir: JsAny, name: String, create: Boolean): JsAny

@JsFun("(dir, name, create) => dir.getFileHandle(name, { create: create })")
private external fun jsGetFileHandle(dir: JsAny, name: String, create: Boolean): JsAny

@JsFun("(dir, name, recursive) => dir.removeEntry(name, { recursive: recursive })")
private external fun jsRemoveEntry(dir: JsAny, name: String, recursive: Boolean): JsAny

// File operations
@JsFun("(handle) => handle.createWritable()")
private external fun jsCreateWritable(handle: JsAny): JsAny

@JsFun("(writable, data) => writable.write(data)")
private external fun jsWriteToStream(writable: JsAny, data: JsAny): JsAny

@JsFun("(writable) => writable.close()")
private external fun jsCloseStream(writable: JsAny): JsAny

@JsFun("(handle) => handle.getFile()")
private external fun jsGetFile(handle: JsAny): JsAny

@JsFun("(file) => file.arrayBuffer()")
private external fun jsFileToArrayBuffer(file: JsAny): JsAny

// Iterator/entries handling
@JsFun("(dir) => dir.entries()")
private external fun jsGetEntries(dir: JsAny): JsAny

// Convert async iterator to array - uses an IIFE pattern
@JsFun("""(iterator) => {
    return (async function() {
        const arr = [];
        for await (const entry of iterator) {
            arr.push(entry[1]);
        }
        return arr;
    })();
}""")
private external fun jsIteratorToArray(iterator: JsAny): JsAny

// Array helpers
@JsFun("(arr) => arr.length")
private external fun jsArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun jsArrayGet(arr: JsAny, idx: Int): JsAny?

// Property access
@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun jsGetStringProperty(obj: JsAny, key: String): String?

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : 0")
private external fun jsGetNumberProperty(obj: JsAny, key: String): Int

@JsFun("(obj, key) => typeof obj[key] === 'number' ? Number(obj[key]) : 0")
private external fun jsGetLongProperty(obj: JsAny, key: String): Double

// ByteArray/Uint8Array conversion
@JsFun("(length) => new Uint8Array(length)")
private external fun jsCreateUint8Array(length: Int): JsAny

@JsFun("(arr, idx, value) => { arr[idx] = value; }")
private external fun jsSetUint8ArrayElement(arr: JsAny, idx: Int, value: Int)

@JsFun("(arr) => arr.length")
private external fun jsUint8ArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun jsGetUint8ArrayElement(arr: JsAny, idx: Int): Int

@JsFun("(buffer) => new Uint8Array(buffer)")
private external fun jsUint8ArrayFromBuffer(buffer: JsAny): JsAny

// Logging
@JsFun("(msg) => console.log('[OpfsService] ' + msg)")
private external fun jsLogInfo(msg: String)

@JsFun("(msg) => console.error('[OpfsService] ' + msg)")
private external fun jsLogError(msg: String)

/**
 * Origin Private File System (OPFS) Service
 * Provides persistent file storage for CSPro applications in the browser
 * 
 * OPFS is a browser API that provides a private, persistent file system
 * that is not accessible to users or other websites.
 * 
 * Directory structure:
 *   /csentry/
 *     /applications/           - PFF files and application folders
 *       /{appFolder}/          - Each application in its own folder
 *         app.pff              - The PFF file
 *         app.pen              - The PEN file
 *         *.dcf                - Dictionary files
 *         *.csdb               - Data files
 *     /data/                   - User data files
 *     /temp/                   - Temporary files
 */
object OpfsService {
    
    private const val ROOT_DIR = "csentry"
    private const val APPLICATIONS_DIR = "applications"
    private const val DATA_DIR = "data"
    private const val TEMP_DIR = "temp"
    
    private var rootHandle: JsAny? = null
    private var applicationsHandle: JsAny? = null

    /**
     * True after a successful [initialize] call for this session.
     */
    fun isInitialized(): Boolean = rootHandle != null

    /**
     * Initialize OPFS if needed.
     */
    suspend fun ensureInitialized(): Boolean {
        return if (isInitialized()) true else initialize()
    }
    
    /**
     * Initialize OPFS and create directory structure
     */
    suspend fun initialize(): Boolean {
        return try {
            jsLogInfo("Initializing OPFS...")
            
            // Get the OPFS root
            val storageRoot = getStorageRoot()
            if (storageRoot == null) {
                jsLogInfo("OPFS not available in this browser")
                return false
            }
            
            // Create directory structure
            rootHandle = getOrCreateDirectory(storageRoot, ROOT_DIR)
            if (rootHandle == null) {
                jsLogInfo("Failed to create root directory")
                return false
            }
            
            applicationsHandle = getOrCreateDirectory(rootHandle!!, APPLICATIONS_DIR)
            getOrCreateDirectory(rootHandle!!, DATA_DIR)
            getOrCreateDirectory(rootHandle!!, TEMP_DIR)
            
            jsLogInfo("OPFS initialized successfully")
            true
        } catch (e: Exception) {
            jsLogError("Failed to initialize OPFS: ${e.message}")
            false
        }
    }
    
    /**
     * Check if OPFS is available
     */
    fun isAvailable(): Boolean {
        return try {
            jsIsOpfsAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the OPFS storage root
     */
    private suspend fun getStorageRoot(): JsAny? {
        return try {
            val promise = jsGetStorageRoot()
            awaitPromise(promise)
        } catch (e: Exception) {
            jsLogError("Error getting storage root: ${e.message}")
            null
        }
    }
    
    /**
     * Get or create a directory
     */
    private suspend fun getOrCreateDirectory(parent: JsAny, name: String): JsAny? {
        return try {
            val promise = jsGetDirectoryHandle(parent, name, true)
            awaitPromise(promise)
        } catch (e: Exception) {
            jsLogError("Error creating directory $name: ${e.message}")
            null
        }
    }
    
    /**
     * Get a directory without creating it
     */
    private suspend fun getDirectory(parent: JsAny, name: String): JsAny? {
        return try {
            val promise = jsGetDirectoryHandle(parent, name, false)
            awaitPromise(promise)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Write a file to OPFS
     */
    suspend fun writeFile(path: String, content: ByteArray): Boolean {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var currentDir = rootHandle ?: return false
            
            // Navigate/create directories
            for (i in 0 until parts.size - 1) {
                currentDir = getOrCreateDirectory(currentDir, parts[i]) ?: return false
            }
            
            val fileName = parts.last()
            
            // Create file handle
            val fileHandlePromise = jsGetFileHandle(currentDir, fileName, true)
            val handle = awaitPromise(fileHandlePromise) ?: return false
            
            // Get writable stream
            val writablePromise = jsCreateWritable(handle)
            val writable = awaitPromise(writablePromise) ?: return false
            
            // Convert ByteArray to Uint8Array
            val uint8Array = byteArrayToJsUint8Array(content)
            
            // Write content
            val writePromise = jsWriteToStream(writable, uint8Array)
            awaitPromise(writePromise)
            
            // Close stream
            val closePromise = jsCloseStream(writable)
            awaitPromise(closePromise)
            
            jsLogInfo("Wrote file: $path (${content.size} bytes)")
            true
        } catch (e: Exception) {
            jsLogError("Error writing file $path: ${e.message}")
            false
        }
    }
    
    /**
     * Write file from ArrayBuffer (JS object)
     */
    suspend fun writeFileFromArrayBuffer(path: String, arrayBuffer: JsAny): Boolean {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var currentDir = rootHandle ?: return false
            
            // Navigate/create directories
            for (i in 0 until parts.size - 1) {
                currentDir = getOrCreateDirectory(currentDir, parts[i]) ?: return false
            }
            
            val fileName = parts.last()
            
            // Create file handle
            val fileHandlePromise = jsGetFileHandle(currentDir, fileName, true)
            val handle = awaitPromise(fileHandlePromise) ?: return false
            
            // Get writable stream
            val writablePromise = jsCreateWritable(handle)
            val writable = awaitPromise(writablePromise) ?: return false
            
            // Write content (arrayBuffer is a JS ArrayBuffer)
            val writePromise = jsWriteToStream(writable, arrayBuffer)
            awaitPromise(writePromise)
            
            // Close stream
            val closePromise = jsCloseStream(writable)
            awaitPromise(closePromise)
            
            jsLogInfo("Wrote file: $path")
            true
        } catch (e: Exception) {
            jsLogError("Error writing file $path: ${e.message}")
            false
        }
    }
    
    /**
     * Read a file from OPFS
     */
    suspend fun readFile(path: String): ByteArray? {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            
            var currentDir = rootHandle ?: return null
            
            // Navigate directories
            for (i in 0 until parts.size - 1) {
                currentDir = getDirectory(currentDir, parts[i]) ?: return null
            }
            
            val fileName = parts.last()
            
            // Get file handle
            val fileHandlePromise = jsGetFileHandle(currentDir, fileName, false)
            val handle = awaitPromise(fileHandlePromise) ?: return null
            
            // Get file
            val filePromise = jsGetFile(handle)
            val file = awaitPromise(filePromise) ?: return null
            
            // Read as array buffer
            val bufferPromise = jsFileToArrayBuffer(file)
            val buffer = awaitPromise(bufferPromise) ?: return null
            
            // Convert to ByteArray
            jsUint8ArrayToByteArray(jsUint8ArrayFromBuffer(buffer))
        } catch (e: Exception) {
            jsLogError("Error reading file $path: ${e.message}")
            null
        }
    }
    
    /**
     * List files in a directory
     */
    suspend fun listFiles(dirPath: String): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        
        try {
            var currentDir = rootHandle ?: return files
            
            val parts = dirPath.split("/").filter { it.isNotEmpty() }
            for (part in parts) {
                currentDir = getDirectory(currentDir, part) ?: return files
            }
            
            // Iterate entries
            val entries = jsGetEntries(currentDir)
            
            // Process entries using async iteration
            val entriesArrayPromise = jsIteratorToArray(entries)
            val entriesArray = awaitPromise(entriesArrayPromise) ?: return files
            val length = jsArrayLength(entriesArray)
            
            for (i in 0 until length) {
                val entry = jsArrayGet(entriesArray, i) ?: continue
                val name = jsGetStringProperty(entry, "name") ?: continue
                val kind = jsGetStringProperty(entry, "kind") ?: continue
                
                files.add(FileInfo(
                    name = name,
                    isDirectory = kind == "directory",
                    path = if (dirPath.isEmpty()) name else "$dirPath/$name"
                ))
            }
        } catch (e: Exception) {
            jsLogError("Error listing files in $dirPath: ${e.message}")
        }
        
        return files
    }
    
    /**
     * List application folders
     */
    suspend fun listApplications(): List<String> {
        val apps = mutableListOf<String>()
        
        try {
            val appDir = applicationsHandle ?: return apps
            
            val entries = jsGetEntries(appDir)
            val entriesArrayPromise = jsIteratorToArray(entries)
            val entriesArray = awaitPromise(entriesArrayPromise) ?: return apps
            val length = jsArrayLength(entriesArray)
            
            for (i in 0 until length) {
                val entry = jsArrayGet(entriesArray, i) ?: continue
                val name = jsGetStringProperty(entry, "name") ?: continue
                val kind = jsGetStringProperty(entry, "kind") ?: continue
                
                if (kind == "directory") {
                    apps.add(name)
                }
            }
        } catch (e: Exception) {
            jsLogError("Error listing applications: ${e.message}")
        }
        
        return apps
    }
    
    /**
     * Delete a file or directory
     */
    suspend fun delete(path: String, recursive: Boolean = false): Boolean {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var currentDir = rootHandle ?: return false
            
            // Navigate to parent directory
            for (i in 0 until parts.size - 1) {
                currentDir = getDirectory(currentDir, parts[i]) ?: return false
            }
            
            val name = parts.last()
            
            // Remove entry
            val removePromise = jsRemoveEntry(currentDir, name, recursive)
            awaitPromise(removePromise)
            
            jsLogInfo("Deleted: $path")
            true
        } catch (e: Exception) {
            jsLogError("Error deleting $path: ${e.message}")
            false
        }
    }
    
    /**
     * Get file info
     */
    suspend fun getFileInfo(path: String): FileInfo? {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            
            var currentDir = rootHandle ?: return null
            
            // Navigate directories
            for (i in 0 until parts.size - 1) {
                currentDir = getDirectory(currentDir, parts[i]) ?: return null
            }
            
            val name = parts.last()
            
            // Try to get as file first
            try {
                val fileHandlePromise = jsGetFileHandle(currentDir, name, false)
                val fileHandle = awaitPromise(fileHandlePromise)
                if (fileHandle != null) {
                    val filePromise = jsGetFile(fileHandle)
                    val file = awaitPromise(filePromise)
                    val size = if (file != null) jsGetLongProperty(file, "size").toLong() else 0L
                    return FileInfo(name = name, isDirectory = false, path = path, size = size)
                }
            } catch (e: Exception) {
                // Not a file, try directory
            }
            
            // Try to get as directory
            try {
                val dirHandlePromise = jsGetDirectoryHandle(currentDir, name, false)
                val dirHandle = awaitPromise(dirHandlePromise)
                if (dirHandle != null) {
                    return FileInfo(name = name, isDirectory = true, path = path)
                }
            } catch (e: Exception) {
                // Not found
            }
            
            null
        } catch (e: Exception) {
            jsLogError("Error getting file info for $path: ${e.message}")
            null
        }
    }
    
    /**
     * Check if a file exists
     */
    suspend fun exists(path: String): Boolean {
        return getFileInfo(path) != null
    }
    
    /**
     * Create a directory
     */
    suspend fun createDirectory(path: String): Boolean {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var currentDir = rootHandle ?: return false
            
            for (part in parts) {
                currentDir = getOrCreateDirectory(currentDir, part) ?: return false
            }
            
            true
        } catch (e: Exception) {
            jsLogError("Error creating directory $path: ${e.message}")
            false
        }
    }
    
    // ============================================================
    // UTILITY FUNCTIONS
    // ============================================================
    
    /**
     * Convert Kotlin ByteArray to JS Uint8Array
     */
    private fun byteArrayToJsUint8Array(bytes: ByteArray): JsAny {
        val uint8Array = jsCreateUint8Array(bytes.size)
        for (i in bytes.indices) {
            jsSetUint8ArrayElement(uint8Array, i, bytes[i].toInt() and 0xFF)
        }
        return uint8Array
    }
    
    /**
     * Convert JS Uint8Array to Kotlin ByteArray
     */
    private fun jsUint8ArrayToByteArray(uint8Array: JsAny): ByteArray {
        val length = jsUint8ArrayLength(uint8Array)
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            bytes[i] = jsGetUint8ArrayElement(uint8Array, i).toByte()
        }
        return bytes
    }
    
    /**
     * Await a JS Promise using suspendCancellableCoroutine
     */
    private suspend fun awaitPromise(promise: JsAny): JsAny? = suspendCancellableCoroutine { cont ->
        jsHandlePromise(
            promise,
            { result ->
                cont.resume(result)
                null
            },
            { error ->
                cont.resumeWithException(Exception("Promise rejected: $error"))
                null
            }
        )
    }
}

/**
 * File information
 */
data class FileInfo(
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val size: Long = 0
)
