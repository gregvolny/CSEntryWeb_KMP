package gov.census.cspro.util

import kotlinx.browser.window

/**
 * WASM implementation using virtual file system paths
 */
actual class CSEntryDirectory {
    
    private val basePath = "/csentry"
    
    actual fun getPath(): String {
        return basePath
    }
    
    actual fun getApplicationsPath(): String {
        return "$basePath/applications"
    }
    
    actual fun getDataPath(): String {
        return "$basePath/data"
    }
    
    actual fun getTempPath(): String {
        return "$basePath/temp"
    }
    
    init {
        println("[CSEntryDirectory] Base path: $basePath")
    }
}


