package gov.census.cspro.util

/**
 * Application directory management
 * Ported from Android CSEntryDirectory
 */
expect class CSEntryDirectory {
    fun getPath(): String
    fun getApplicationsPath(): String
    fun getDataPath(): String
    fun getTempPath(): String
}
