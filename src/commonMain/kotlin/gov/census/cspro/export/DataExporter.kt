package gov.census.cspro.export

/**
 * Export functionality interfaces
 * Mirrors export capabilities from CSPro engine
 */

/**
 * Export format types
 */
enum class ExportFormat {
    CSPRO_DATA,      // Native CSPro data format
    COMMA_DELIMITED, // CSV
    TAB_DELIMITED,   // TSV
    SEMICOLON_DELIMITED,
    JSON,
    EXCEL,           // XLSX
    STATA,           // DTA
    SPSS,            // SAV
    SAS,
    R                // RData
}

/**
 * Export options
 */
data class ExportOptions(
    val format: ExportFormat = ExportFormat.COMMA_DELIMITED,
    val includeHeader: Boolean = true,
    val useLabels: Boolean = false,
    val exportNotes: Boolean = false,
    val recordsToExport: Set<String> = emptySet(), // Empty = all records
    val itemsToExport: Set<String> = emptySet(),   // Empty = all items
    val outputPath: String = ""
)

/**
 * Export result
 */
data class ExportResult(
    val success: Boolean,
    val outputPath: String = "",
    val recordsExported: Int = 0,
    val casesExported: Int = 0,
    val errorMessage: String = ""
)

/**
 * Export progress listener
 */
interface ExportProgressListener {
    fun onProgress(current: Int, total: Int, message: String)
    fun onComplete(result: ExportResult)
    fun onError(error: String)
}

/**
 * Data exporter interface - platform specific implementations
 */
expect class DataExporter() {
    /**
     * Export data from a CSPro data source
     * 
     * @param dataPath Path to the CSPro data file or repository
     * @param dictionaryPath Path to the dictionary file
     * @param options Export options
     * @param listener Progress listener
     * @return Export result
     */
    suspend fun export(
        dataPath: String,
        dictionaryPath: String,
        options: ExportOptions,
        listener: ExportProgressListener? = null
    ): ExportResult
    
    /**
     * Get available export formats for current platform
     */
    fun getAvailableFormats(): List<ExportFormat>
    
    /**
     * Validate export options
     */
    fun validateOptions(options: ExportOptions): Boolean
    
    /**
     * Cancel ongoing export
     */
    fun cancel()
}

/**
 * File download helper for web platforms
 */
expect class FileDownloader() {
    /**
     * Trigger a file download in the browser
     * @param data The file content
     * @param filename The suggested filename
     * @param mimeType The MIME type
     */
    fun downloadFile(data: ByteArray, filename: String, mimeType: String)
    
    /**
     * Download a text file
     */
    fun downloadTextFile(content: String, filename: String, mimeType: String = "text/plain")
    
    /**
     * Check if download is supported
     */
    fun isSupported(): Boolean
}
