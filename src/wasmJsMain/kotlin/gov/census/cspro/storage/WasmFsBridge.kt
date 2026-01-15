package gov.census.cspro.storage

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridge between OPFS (browser storage) and WASM virtual filesystem (Emscripten FS)
 * 
 * The WASM engine can only read files from its virtual filesystem (Emscripten FS).
 * Files uploaded by users are stored in OPFS (Origin Private File System).
 * This bridge copies files from OPFS to the WASM FS so the engine can access them.
 * 
 * Path mapping:
 * - OPFS: applications/{appFolder}/{file} -> WASM FS: /opfs/{appFolder}/{file}
 * - Embedded assets are already at: /Assets/examples/{file}
 */
object WasmFsBridge {
    
    private const val WASM_OPFS_ROOT = "/opfs"
    
    // Track which application folders have already been copied to avoid duplicate copies
    private val copiedApplications = mutableSetOf<String>()
    
    /**
     * Clear the cache of copied application folders.
     * Call this when an application is fully closed/ended to allow re-copying if needed.
     */
    fun clearCopiedApplicationCache() {
        jsLogInfo("Clearing copied applications cache (was ${copiedApplications.size} entries)")
        copiedApplications.clear()
    }
    
    /**
     * Clear cache entry for a specific application folder
     */
    fun clearCopiedApplicationCache(appFolder: String) {
        if (copiedApplications.remove(appFolder)) {
            jsLogInfo("Cleared cache for: $appFolder")
        }
    }

    /**
     * Convert a WASM FS path under /opfs back to an OPFS path under applications/.
     * Example: /opfs/myapp/data.csdb -> applications/myapp/data.csdb
     */
    fun getOpfsPathForWasmPath(wasmPath: String): String? {
        if (!wasmPath.startsWith("$WASM_OPFS_ROOT/")) return null
        return "applications/" + wasmPath.removePrefix("$WASM_OPFS_ROOT/")
    }
    
    /**
     * Check if a path refers to an embedded asset (already in WASM FS)
     */
    fun isEmbeddedAsset(path: String): Boolean {
        return path.startsWith("/Assets/") || path.startsWith("Assets/")
    }
    
    /**
     * Get the WASM filesystem path for a given application path
     * 
     * For OPFS files: applications/myapp/file.pff -> /opfs/myapp/file.pff
     * For embedded assets: /Assets/... -> /Assets/... (unchanged)
     */
    fun getWasmPath(opfsPath: String): String {
        return when {
            isEmbeddedAsset(opfsPath) -> {
                // Ensure it starts with /
                if (opfsPath.startsWith("/")) opfsPath else "/$opfsPath"
            }
            opfsPath.startsWith("applications/") -> {
                // Convert OPFS path to WASM FS path
                "$WASM_OPFS_ROOT/${opfsPath.removePrefix("applications/")}"
            }
            else -> {
                // Assume it's a direct path, put it under /opfs
                "$WASM_OPFS_ROOT/$opfsPath"
            }
        }
    }
    
    /**
     * Copy a single file from OPFS to WASM virtual filesystem
     */
    suspend fun copyFileToWasmFs(opfsPath: String): Boolean {
        if (isEmbeddedAsset(opfsPath)) {
            jsLogInfo("Path $opfsPath is an embedded asset, no copy needed")
            return true
        }
        
        try {
            jsLogInfo("Copying file from OPFS to WASM FS: $opfsPath")
            
            // Read file from OPFS
            val content = OpfsService.readFile(opfsPath)
            if (content == null) {
                jsLogError("Failed to read file from OPFS: $opfsPath")
                return false
            }
            
            // Get the target path in WASM FS
            val wasmPath = getWasmPath(opfsPath)
            jsLogInfo("Target WASM path: $wasmPath")
            
            // Ensure parent directories exist in WASM FS
            val parentDir = wasmPath.substringBeforeLast("/")
            if (parentDir.isNotEmpty() && parentDir != wasmPath) {
                createWasmDirectory(parentDir)
            }
            
            // Write file to WASM FS
            writeFileToWasmFs(wasmPath, content)
            jsLogInfo("File copied successfully: $opfsPath -> $wasmPath")
            
            return true
        } catch (e: Exception) {
            jsLogError("Error copying file to WASM FS: ${e.message}")
            return false
        }
    }
    
    /**
     * Copy an entire application folder from OPFS to WASM FS
     * This includes the PFF file and all related files (PEN, DCF, CSDB, etc.)
     * Also copies parent folder files which may contain additional resources.
     * 
     * Caches which applications have been copied to avoid duplicate copies within
     * the same session (e.g., when CaseListActivity opens app, then EntryActivity uses it).
     */
    suspend fun copyApplicationToWasmFs(pffPath: String): String? {
        if (isEmbeddedAsset(pffPath)) {
            jsLogInfo("Path $pffPath is an embedded asset, returning as-is")
            return if (pffPath.startsWith("/")) pffPath else "/$pffPath"
        }
        
        try {
            // Determine the application root folder early to check cache
            val pathParts = pffPath.removePrefix("applications/").split("/")
            val appRootFolder = if (pathParts.isNotEmpty()) {
                "applications/${pathParts[0]}"
            } else {
                pffPath.substringBeforeLast("/")
            }
            
            // Check if already copied to WASM FS (avoid duplicate copies)
            if (appRootFolder in copiedApplications) {
                jsLogInfo("Application folder already copied to WASM FS: $appRootFolder")
                val wasmPath = getWasmPath(pffPath)
                jsLogInfo("Returning cached WASM path for PFF: $wasmPath")
                return wasmPath
            }
            
            jsLogInfo("Copying application folder to WASM FS for: $pffPath")
            
            // Determine the folder containing the PFF file
            val pffFolder = if (pffPath.contains("/")) {
                pffPath.substringBeforeLast("/")
            } else {
                ""
            }
            
            if (pffFolder.isEmpty()) {
                jsLogError("Invalid PFF path: $pffPath")
                return null
            }
            
            jsLogInfo("PFF folder: $pffFolder")
            jsLogInfo("Application root folder: $appRootFolder")
            
            // Recursively list ALL files in the application folder
            val allFiles = OpfsService.listAllFiles(appRootFolder)
            jsLogInfo("Found ${allFiles.size} total files in application folder")
            
            // Copy each file
            var successCount = 0
            var failCount = 0
            for (file in allFiles) {
                if (!file.isDirectory) {
                    val filePath = file.path
                    jsLogInfo("Copying: $filePath")
                    val copied = copyFileToWasmFs(filePath)
                    if (copied) {
                        successCount++
                    } else {
                        failCount++
                        jsLogError("Failed to copy: $filePath")
                    }
                }
            }
            
            jsLogInfo("Copied $successCount files to WASM FS ($failCount failed)")
            
            if (successCount == 0 && allFiles.isNotEmpty()) {
                jsLogError("No files were successfully copied!")
                return null
            }
            
            // Mark this application folder as copied to avoid duplicate copies
            copiedApplications.add(appRootFolder)
            
            // Return the WASM FS path for the PFF file
            val wasmPath = getWasmPath(pffPath)
            jsLogInfo("Returning WASM path for PFF: $wasmPath")
            return wasmPath
        } catch (e: Exception) {
            jsLogError("Error copying application to WASM FS: ${e.message}")
            return null
        }
    }
    
    /**
     * List files in a WASM FS directory
     */
    fun listWasmDirectory(path: String): List<String> {
        return try {
            jsListWasmDirectory(path)
        } catch (e: Exception) {
            jsLogError("Error listing WASM directory $path: ${e.message}")
            emptyList()
        }
    }

    /**
     * List all files (recursive) under a WASM FS directory.
     */
    fun listWasmFilesRecursive(path: String): List<String> {
        return try {
            val result = jsListWasmFilesRecursiveInternal(path)
            val length = jsGetArrayLength(result)
            val list = mutableListOf<String>()
            for (i in 0 until length) {
                val item = jsGetArrayStringElement(result, i)
                if (item != null) list.add(item)
            }
            list
        } catch (e: Exception) {
            jsLogError("Error listing WASM files recursively under $path: ${e.message}")
            emptyList()
        }
    }

    /**
     * Read a WASM FS file as an ArrayBuffer (for efficient write back to OPFS).
     */
    fun readWasmFileAsArrayBuffer(path: String): JsAny? {
        return try {
            jsReadWasmFileAsArrayBufferInternal(path)
        } catch (e: Exception) {
            jsLogError("Error reading WASM file as ArrayBuffer $path: ${e.message}")
            null
        }
    }

    /**
     * Persist all files under a WASM /opfs/{appFolder} directory back to OPFS.
     * This is required because we currently copy OPFS -> WASM FS on open, but without this
     * step the engine's updates (e.g., *.csdb) are lost or overwritten on the next open.
     */
    suspend fun persistApplicationFolderToOpfs(wasmAppFolder: String): Boolean {
        if (!wasmAppFolder.startsWith("$WASM_OPFS_ROOT/")) {
            jsLogError("persistApplicationFolderToOpfs called with non-/opfs path: $wasmAppFolder")
            return false
        }

        if (!OpfsService.isAvailable()) {
            jsLogError("OPFS is not available; cannot persist application data")
            return false
        }

        if (!OpfsService.ensureInitialized()) {
            jsLogError("OPFS failed to initialize; cannot persist application data")
            return false
        }

        val files = listWasmFilesRecursive(wasmAppFolder)
        if (files.isEmpty()) {
            jsLogInfo("No files found under $wasmAppFolder to persist")
            return true
        }

        var allOk = true
        for (wasmPath in files) {
            val opfsPath = getOpfsPathForWasmPath(wasmPath)
            if (opfsPath == null) {
                jsLogError("Skipping non-/opfs file during persist: $wasmPath")
                continue
            }

            val buffer = readWasmFileAsArrayBuffer(wasmPath)
            if (buffer == null) {
                jsLogError("Failed to read WASM file for persist: $wasmPath")
                allOk = false
                continue
            }

            val ok = OpfsService.writeFileFromArrayBuffer(opfsPath, buffer)
            if (!ok) {
                jsLogError("Failed to write file back to OPFS: $opfsPath")
                allOk = false
            }
        }

        if (allOk) {
            jsLogInfo("Persisted ${files.size} file(s) from $wasmAppFolder to OPFS")
        } else {
            jsLogError("Persist completed with errors for $wasmAppFolder")
        }

        return allOk
    }
    
    /**
     * Check if a file exists in WASM FS
     */
    fun fileExistsInWasmFs(path: String): Boolean {
        return try {
            jsFileExistsInWasmFs(path)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create a directory in WASM FS (including parent directories)
     */
    private fun createWasmDirectory(path: String) {
        try {
            jsCreateWasmDirectory(path)
        } catch (e: Exception) {
            // Directory might already exist, ignore error
        }
    }
    
    /**
     * Write a file to WASM FS
     */
    private fun writeFileToWasmFs(path: String, content: ByteArray) {
        jsWriteFileToWasmFs(path, content)
    }
}

// ============================================================
// JS INTEROP FUNCTIONS FOR WASM FILESYSTEM ACCESS
// ============================================================

@JsFun("(msg) => console.log('[WasmFsBridge] ' + msg)")
private external fun jsLogInfo(msg: String)

@JsFun("(msg) => console.error('[WasmFsBridge] ' + msg)")
private external fun jsLogError(msg: String)

@JsFun("""(path) => {
    if (!window.CSProModule || !window.CSProModule.FS) {
        console.error('[WasmFsBridge] CSProModule.FS not available');
        return false;
    }
    try {
        const stat = window.CSProModule.FS.stat(path);
        return stat !== null;
    } catch (e) {
        return false;
    }
}""")
private external fun jsFileExistsInWasmFs(path: String): Boolean

@JsFun("""(path) => {
    if (!window.CSProModule || !window.CSProModule.FS) {
        console.error('[WasmFsBridge] CSProModule.FS not available');
        return;
    }
    const FS = window.CSProModule.FS;
    
    // Split path into parts and create each directory
    const parts = path.split('/').filter(p => p.length > 0);
    let currentPath = '';
    
    for (const part of parts) {
        currentPath += '/' + part;
        try {
            FS.stat(currentPath);
        } catch (e) {
            // Directory doesn't exist, create it
            try {
                FS.mkdir(currentPath);
                console.log('[WasmFsBridge] Created directory: ' + currentPath);
            } catch (mkdirError) {
                // Might already exist, ignore
            }
        }
    }
}""")
private external fun jsCreateWasmDirectory(path: String)

@JsFun("""(path, contentJson) => {
    if (!window.CSProModule || !window.CSProModule.FS) {
        throw new Error('CSProModule.FS not available');
    }
    const FS = window.CSProModule.FS;
    
    // Parse the JSON array back to Uint8Array
    const contentArray = JSON.parse(contentJson);
    const uint8Array = new Uint8Array(contentArray);
    
    // Write file to WASM FS
    FS.writeFile(path, uint8Array);
    console.log('[WasmFsBridge] Wrote file: ' + path + ' (' + uint8Array.length + ' bytes)');
}""")
private external fun jsWriteFileToWasmFsInternal(path: String, contentJson: String)

private fun jsWriteFileToWasmFs(path: String, content: ByteArray) {
    // Convert ByteArray to JSON array string for JS interop
    val jsonArray = content.map { (it.toInt() and 0xFF) }.joinToString(",", "[", "]")
    jsWriteFileToWasmFsInternal(path, jsonArray)
}

@JsFun("""(path) => {
    if (!window.CSProModule || !window.CSProModule.FS) {
        console.error('[WasmFsBridge] CSProModule.FS not available');
        return [];
    }
    try {
        const entries = window.CSProModule.FS.readdir(path);
        return entries.filter(e => e !== '.' && e !== '..');
    } catch (e) {
        console.error('[WasmFsBridge] Error reading directory: ' + e.message);
        return [];
    }
}""")
private external fun jsListWasmDirectoryInternal(path: String): JsAny

@JsFun("""(path) => {
    if (!window.CSProModule || !window.CSProModule.FS) {
        console.error('[WasmFsBridge] CSProModule.FS not available');
        return [];
    }
    const FS = window.CSProModule.FS;
    const out = [];
    const stack = [path];
    while (stack.length) {
        const dir = stack.pop();
        let entries = [];
        try {
            entries = FS.readdir(dir);
        } catch (e) {
            continue;
        }
        for (const name of entries) {
            if (name === '.' || name === '..') continue;
            const full = (dir.endsWith('/') ? dir.slice(0, -1) : dir) + '/' + name;
            let st;
            try {
                st = FS.stat(full);
            } catch (e) {
                continue;
            }
            try {
                if (FS.isDir(st.mode)) stack.push(full);
                else out.push(full);
            } catch (e) {
                // Ignore unexpected stat/mode values
            }
        }
    }
    return out;
}""")
private external fun jsListWasmFilesRecursiveInternal(path: String): JsAny

@JsFun("""(path) => {
    if (!window.CSProModule || !window.CSProModule.FS) {
        throw new Error('CSProModule.FS not available');
    }
    const FS = window.CSProModule.FS;
    const data = FS.readFile(path); // Uint8Array
    // Slice to a standalone ArrayBuffer for safe use by OPFS writer.
    const ab = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
    return ab;
}""")
private external fun jsReadWasmFileAsArrayBufferInternal(path: String): JsAny

private fun jsListWasmDirectory(path: String): List<String> {
    return try {
        val result = jsListWasmDirectoryInternal(path)
        val length = jsGetArrayLength(result)
        val list = mutableListOf<String>()
        for (i in 0 until length) {
            val item = jsGetArrayStringElement(result, i)
            if (item != null) {
                list.add(item)
            }
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

@JsFun("(arr) => arr.length")
private external fun jsGetArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun jsGetArrayStringElement(arr: JsAny, idx: Int): String?
