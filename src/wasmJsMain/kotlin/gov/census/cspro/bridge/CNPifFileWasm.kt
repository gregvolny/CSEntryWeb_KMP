package gov.census.cspro.bridge

import gov.census.cspro.storage.OpfsService

/**
 * Wasm/JS implementation of CNPifFile loader
 * Parses PFF files directly from text content (INI-style format)
 * Matches Android CNPifFile.java behavior exactly
 */
actual object CNPifFileLoader {
    
    // Cache for already loaded PFF data to avoid repeated parsing
    private val pffCache = mutableMapOf<String, CNPifFileData>()
    
    /**
     * Synchronous load - uses cached data only
     */
    actual fun load(filename: String): CNPifFileData {
        println("[CNPifFileLoader] Loading PFF file (sync): $filename")
        
        // Check cache first
        pffCache[filename]?.let { return it }
        
        // Cannot read from OPFS synchronously - return mock data
        // For proper loading, use loadAsync()
        println("[CNPifFileLoader] Cache miss - returning default data for: $filename")
        
        // Extract description from filename as fallback
        val description = filename.substringAfterLast('/').substringBeforeLast(".pff")
        
        return CNPifFileData(
            isValid = true,
            description = description,
            entryAppType = true,
            showInApplicationListing = 0 // Always
        )
    }
    
    /**
     * Asynchronous load - reads from OPFS
     */
    actual suspend fun loadAsync(filename: String): CNPifFileData {
        println("[CNPifFileLoader] Loading PFF file (async): $filename")
        
        // Check cache first
        pffCache[filename]?.let { return it }
        
        // Read file content from OPFS
        val content = try {
            OpfsService.readFile(filename)?.decodeToString()
        } catch (e: Exception) {
            println("[CNPifFileLoader] Error reading file: $e")
            null
        }
        
        if (content == null) {
            println("[CNPifFileLoader] Could not read file: $filename")
            return CNPifFileData(isValid = false)
        }
        
        // Parse PFF content
        val data = parsePffContent(filename, content)
        pffCache[filename] = data
        return data
    }
    
    /**
     * Parse PFF file content (INI-style format)
     * Example:
     * [Run Information]
     * Version=CSPro 8.0
     * AppType=Entry
     * Label=Simple CAPI
     * ShowInApplicationListing=Always
     * ...
     */
    private fun parsePffContent(filename: String, content: String): CNPifFileData {
        val lines = content.lines()
        
        var description = ""
        var appType = ""
        var showInApplicationListing = 0 // Default to Always
        var inputFilename = ""
        var appFilename = ""
        var writeFilename = ""
        var onExitFilename = ""
        val externalFilenames = mutableListOf<String>()
        val userFilenames = mutableListOf<String>()
        
        var currentSection = ""
        
        for (rawLine in lines) {
            val line = rawLine.trim()
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue
            
            // Section header
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).lowercase()
                println("[PFF] Section: [$currentSection]")
                continue
            }
            
            // Key=Value pairs
            val eqIndex = line.indexOf('=')
            if (eqIndex <= 0) continue
            
            val key = line.substring(0, eqIndex).trim().lowercase()
            val value = line.substring(eqIndex + 1).trim()
            
            // Log parsing for debugging
            if (key == "showinapplicationlisting" || key == "label" || key == "description" || key == "apptype") {
                println("[PFF] [$currentSection] '$key' = '$value'")
            }
            
            // Handle AppType and Description in ANY section (CSPro uses different formats)
            when (key) {
                "apptype" -> {
                    if (appType.isEmpty()) appType = value
                }
                "label", "description" -> {
                    if (description.isEmpty()) description = value
                }
                "showinapplicationlisting" -> {
                    showInApplicationListing = when (value.lowercase()) {
                        "always" -> 0
                        "hidden" -> 1
                        "never" -> 2
                        else -> 0
                    }
                }
            }
            
            // Section-specific handling for file paths
            when (currentSection) {
                "run information", "cspro", "dataentry" -> {
                    when (key) {
                        "application" -> if (appFilename.isEmpty()) appFilename = value
                        "inputdata" -> if (inputFilename.isEmpty()) inputFilename = value
                    }
                }
                "files" -> {
                    when (key) {
                        "application" -> appFilename = value
                        "inputdata" -> inputFilename = value
                        "writefile" -> writeFilename = value
                        "onexit" -> onExitFilename = value
                    }
                }
                "externalfiles" -> {
                    externalFilenames.add(value)
                }
                "userfiles" -> {
                    userFilenames.add(value)
                }
            }
        }
        
        // Check if it's an entry application
        val isEntryApp = appType.lowercase() in listOf("entry", "dataentry", "capi")
        println("[PFF] AppType='$appType', isEntryApp=$isEntryApp")
        
        // If no description found, use filename
        if (description.isEmpty()) {
            description = filename.substringAfterLast('/').substringBeforeLast(".pff")
        }
        
        return CNPifFileData(
            isValid = true,
            description = description,
            entryAppType = isEntryApp,
            showInApplicationListing = showInApplicationListing,
            inputFilename = inputFilename,
            appFilename = appFilename,
            externalFilenames = externalFilenames.toTypedArray(),
            userFilenames = userFilenames.toTypedArray(),
            writeFilename = writeFilename,
            onExitFilename = onExitFilename
        )
    }
    
    /**
     * Clear the cache (useful when files are updated)
     */
    fun clearCache() {
        pffCache.clear()
    }
}



