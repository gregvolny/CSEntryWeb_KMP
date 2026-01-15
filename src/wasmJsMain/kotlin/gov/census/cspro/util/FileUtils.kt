package gov.census.cspro.util

import gov.census.cspro.bridge.CNPifFile
import kotlinx.browser.window
import kotlin.js.Promise

// External reference to Date.now()
@JsName("Date.now")
external fun dateNow(): Double

// Helper function to get current time in milliseconds
private fun currentTimeMillis(): Long = dateNow().toLong()

/**
 * WASM/JS-specific file utilities for discovering CSPro applications
 */
object FileUtils {
    
    /**
     * Get list of .pff files in a directory (WASM version)
     * In the web environment, we'll use the File System Access API or pre-configured paths
     */
    suspend fun getApplicationsInDirectory(searchDir: String): List<String> {
        println("[FileUtils] Scanning directory: $searchDir")
        
        // TODO: Implement actual file system access
        // For now, return mock data
        val mockApps = listOf(
            "$searchDir/PopStan2020.pff",
            "$searchDir/HousingSurvey.pff",
            "$searchDir/AgricultureCensus.pff"
        )
        
        println("[FileUtils] Found ${mockApps.size} applications")
        return mockApps
    }
    
    /**
     * Check if file exists (WASM version)
     */
    fun fileExists(path: String): Boolean {
        // TODO: Implement actual file check via WASM/Emscripten
        println("[FileUtils] Checking if file exists: $path")
        return true
    }
    
    /**
     * Get file info (WASM version)
     */
    fun getFileInfo(path: String): FileInfo? {
        // TODO: Implement actual file stat via WASM/Emscripten
        val filename = Util.removeDirectory(path)
        return FileInfo(
            name = filename,
            isDirectory = false,
            size = 0,
            lastModified = currentTimeMillis()
        )
    }
    
    /**
     * List files in directory (WASM version)
     */
    fun listFiles(directory: String): List<FileInfo> {
        // TODO: Implement actual directory listing via WASM/Emscripten
        println("[FileUtils] Listing files in: $directory")
        return emptyList()
    }
}

/**
 * Application info for display in applications list
 */
data class ApplicationInfo(
    val filename: String,
    val description: String,
    val isEntryApp: Boolean = true,
    val lastModified: Long = 0
)

/**
 * Helper for loading application list
 */
object ApplicationLoader {
    
    suspend fun loadApplications(searchDir: String): List<ApplicationInfo> {
        println("[ApplicationLoader] Loading applications from: $searchDir")
        
        val pffFiles = FileUtils.getApplicationsInDirectory(searchDir)
        val applications = mutableListOf<ApplicationInfo>()
        
        for (pffFile in pffFiles) {
            try {
                val pifFile = CNPifFile.loadAsync(pffFile)
                
                if (pifFile.isValid && pifFile.shouldShowInApplicationListing(false)) {
                    applications.add(
                        ApplicationInfo(
                            filename = pffFile,
                            description = pifFile.description,
                            isEntryApp = pifFile.entryAppType,
                            lastModified = currentTimeMillis()
                        )
                    )
                    println("[ApplicationLoader] Loaded: ${pifFile.description}")
                }
            } catch (e: Exception) {
                println("[ApplicationLoader] Error loading $pffFile:" + e)
            }
        }
        
        println("[ApplicationLoader] Total applications loaded: ${applications.size}")
        return applications
    }
}



