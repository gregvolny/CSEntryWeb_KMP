package gov.census.cspro.bridge

import gov.census.cspro.util.Util

/**
 * CSPro .pff (Program Information File) parser
 * Ported from Android CNPifFile.java
 * 
 * Uses expect/actual pattern for native PFF parsing
 */
class CNPifFile(filename: String) {
    
    enum class ShowInApplicationListing {
        Always,
        Hidden,
        Never
    }
    
    var isValid: Boolean = false
        private set
    
    var description: String = ""
        private set
    
    var entryAppType: Boolean = false
        private set
    
    var showInApplicationListing: ShowInApplicationListing = ShowInApplicationListing.Always
        private set
    
    var inputFilename: String = ""
        private set
    
    var appFilename: String = ""
        private set
    
    var externalFilenames: Array<String> = emptyArray()
        private set
    
    var userFilenames: Array<String> = emptyArray()
        private set
    
    var writeFilename: String = ""
        private set
    
    var onExitFilename: String = ""
        private set
    
    var pffDirectory: String = ""
        private set
    
    init {
        // Platform-specific initialization
        loadPifData(filename)
    }
    
    fun shouldShowInApplicationListing(showHiddenApplications: Boolean): Boolean {
        // if the description is empty then we won't display it in the Applications Listing screen
        return description.trim().isNotEmpty() && (
            showInApplicationListing == ShowInApplicationListing.Always ||
            (showHiddenApplications && showInApplicationListing == ShowInApplicationListing.Hidden)
        )
    }
    
    // Platform-specific PFF loading - will be implemented in wasmJsMain
    private fun loadPifData(filename: String) {
        val data = CNPifFileLoader.load(filename)
        
        isValid = data.isValid
        if (isValid) {
            pffDirectory = Util.removeFilename(filename)
            description = data.description
            entryAppType = data.entryAppType
            showInApplicationListing = ShowInApplicationListing.values()[data.showInApplicationListing]
            inputFilename = data.inputFilename
            appFilename = data.appFilename
            externalFilenames = data.externalFilenames
            userFilenames = data.userFilenames
            writeFilename = data.writeFilename
            onExitFilename = data.onExitFilename
        }
    }
}

/**
 * Data class for PFF file contents
 */
data class CNPifFileData(
    val isValid: Boolean = false,
    val description: String = "",
    val entryAppType: Boolean = false,
    val showInApplicationListing: Int = 0,
    val inputFilename: String = "",
    val appFilename: String = "",
    val externalFilenames: Array<String> = emptyArray(),
    val userFilenames: Array<String> = emptyArray(),
    val writeFilename: String = "",
    val onExitFilename: String = ""
)

/**
 * Platform-specific PFF loader
 */
expect object CNPifFileLoader {
    fun load(filename: String): CNPifFileData
}
