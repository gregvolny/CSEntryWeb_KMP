package gov.census.cspro.export

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import kotlin.js.JsAny

/**
 * Top-level @JsFun declarations for WASM compatibility
 * These replace inline js() calls which are not allowed in WASM
 */

// JSON operations
@JsFun("(dataPath) => { return JSON.stringify({ exportDate: new Date().toISOString(), source: dataPath, cases: [] }, null, 2); }")
private external fun createExportJson(dataPath: String): String

// Blob operations
@JsFun("(content, mimeType) => { return new Blob([content], { type: mimeType }); }")
private external fun createTextBlob(content: String, mimeType: String): JsAny

@JsFun("(size) => { return new Uint8Array(size); }")
private external fun createUint8Array(size: Int): JsAny

@JsFun("(arr, index, value) => { arr[index] = value; }")
private external fun setArrayValue(arr: JsAny, index: Int, value: Byte)

@JsFun("(uint8Array, mimeType) => { return new Blob([uint8Array], { type: mimeType }); }")
private external fun createBlobFromUint8Array(uint8Array: JsAny, mimeType: String): JsAny

// URL operations
@JsFun("(blob) => { return URL.createObjectURL(blob); }")
private external fun createObjectURL(blob: JsAny): String

@JsFun("(url) => { URL.revokeObjectURL(url); }")
private external fun revokeObjectURL(url: String)

// Element property setters
@JsFun("(element, value) => { element.href = value; }")
private external fun setHref(element: JsAny, value: String)

@JsFun("(element, value) => { element.download = value; }")
private external fun setDownload(element: JsAny, value: String)

@JsFun("(element, value) => { element.style.display = value; }")
private external fun setStyleDisplay(element: JsAny, value: String)

@JsFun("(element) => { element.click(); }")
private external fun clickElement(element: JsAny)

// Feature detection
@JsFun("() => { return typeof Blob !== 'undefined' && typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function'; }")
private external fun isBlobSupported(): Boolean

// Logging
@JsFun("(message) => { console.log('[FileDownloader] ' + message); }")
private external fun logDownload(message: String)

/**
 * WASM/Web implementation of DataExporter
 * Uses JavaScript libraries for format conversion when available
 * 
 * Mirrors export functionality from CSPro engine
 */

actual class DataExporter {
    private var isCancelled = false
    
    actual suspend fun export(
        dataPath: String,
        dictionaryPath: String,
        options: ExportOptions,
        listener: ExportProgressListener?
    ): ExportResult {
        isCancelled = false
        
        return try {
            listener?.onProgress(0, 100, "Starting export...")
            
            // For web, we primarily support text-based formats
            when (options.format) {
                ExportFormat.COMMA_DELIMITED,
                ExportFormat.TAB_DELIMITED,
                ExportFormat.SEMICOLON_DELIMITED -> {
                    exportToDelimited(dataPath, dictionaryPath, options, listener)
                }
                ExportFormat.JSON -> {
                    exportToJson(dataPath, dictionaryPath, options, listener)
                }
                else -> {
                    ExportResult(
                        success = false,
                        errorMessage = "Format ${options.format} not supported on web platform"
                    )
                }
            }
        } catch (e: Exception) {
            listener?.onError("Export failed: ${e.message}")
            ExportResult(
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }
    
    private suspend fun exportToDelimited(
        dataPath: String,
        dictionaryPath: String,
        options: ExportOptions,
        listener: ExportProgressListener?
    ): ExportResult {
        listener?.onProgress(10, 100, "Reading data...")
        
        val delimiter = when (options.format) {
            ExportFormat.TAB_DELIMITED -> "\t"
            ExportFormat.SEMICOLON_DELIMITED -> ";"
            else -> ","
        }
        
        // This would integrate with the CSPro engine to read data
        // For now, return a placeholder result
        listener?.onProgress(50, 100, "Converting to ${options.format}...")
        
        if (isCancelled) {
            return ExportResult(
                success = false,
                errorMessage = "Export cancelled"
            )
        }
        
        // Build output content
        val content = StringBuilder()
        
        // Add header if requested
        if (options.includeHeader) {
            content.appendLine("# CSPro Export - ${options.format}")
        }
        
        listener?.onProgress(90, 100, "Preparing download...")
        
        // Trigger download
        val fileName = options.outputPath.ifBlank { "export.${getExtension(options.format)}" }
        val fileDownloader = FileDownloader()
        fileDownloader.downloadTextFile(
            content.toString(),
            fileName,
            getMimeType(options.format)
        )
        
        val result = ExportResult(
            success = true,
            outputPath = fileName,
            recordsExported = 0,
            casesExported = 0
        )
        
        listener?.onProgress(100, 100, "Export complete")
        listener?.onComplete(result)
        
        return result
    }
    
    private suspend fun exportToJson(
        dataPath: String,
        dictionaryPath: String,
        options: ExportOptions,
        listener: ExportProgressListener?
    ): ExportResult {
        listener?.onProgress(10, 100, "Reading data...")
        
        // Create JSON using top-level @JsFun
        val jsonContent = createExportJson(dataPath)
        
        listener?.onProgress(90, 100, "Preparing download...")
        
        val fileName = options.outputPath.ifBlank { "export.json" }
        val fileDownloader = FileDownloader()
        fileDownloader.downloadTextFile(
            jsonContent,
            fileName,
            "application/json"
        )
        
        val result = ExportResult(
            success = true,
            outputPath = fileName,
            recordsExported = 0,
            casesExported = 0
        )
        
        listener?.onProgress(100, 100, "Export complete")
        listener?.onComplete(result)
        
        return result
    }
    
    actual fun getAvailableFormats(): List<ExportFormat> {
        // Web platform supports text-based formats
        return listOf(
            ExportFormat.COMMA_DELIMITED,
            ExportFormat.TAB_DELIMITED,
            ExportFormat.SEMICOLON_DELIMITED,
            ExportFormat.JSON
        )
    }
    
    actual fun validateOptions(options: ExportOptions): Boolean {
        return options.format in getAvailableFormats()
    }
    
    actual fun cancel() {
        isCancelled = true
    }
    
    private fun getExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.COMMA_DELIMITED -> "csv"
            ExportFormat.TAB_DELIMITED -> "tsv"
            ExportFormat.SEMICOLON_DELIMITED -> "csv"
            ExportFormat.JSON -> "json"
            ExportFormat.EXCEL -> "xlsx"
            ExportFormat.STATA -> "dta"
            ExportFormat.SPSS -> "sav"
            ExportFormat.SAS -> "sas7bdat"
            ExportFormat.R -> "RData"
            ExportFormat.CSPRO_DATA -> "csdb"
        }
    }
    
    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.COMMA_DELIMITED,
            ExportFormat.TAB_DELIMITED,
            ExportFormat.SEMICOLON_DELIMITED -> "text/csv"
            ExportFormat.JSON -> "application/json"
            ExportFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}

/**
 * WASM/Web implementation of FileDownloader
 * Uses browser download capabilities with proper WASM-compatible typing
 */
actual class FileDownloader {
    
    actual fun downloadFile(data: ByteArray, filename: String, mimeType: String) {
        try {
            // Convert ByteArray to JavaScript Uint8Array using @JsFun
            val uint8Array = createUint8Array(data.size)
            for (i in data.indices) {
                setArrayValue(uint8Array, i, data[i])
            }
            
            // Create Blob from Uint8Array
            val blob = createBlobFromUint8Array(uint8Array, mimeType)
            
            triggerDownload(blob, filename)
        } catch (e: Exception) {
            println("[FileDownloader] Error downloading file: ${e.message}")
        }
    }
    
    actual fun downloadTextFile(content: String, filename: String, mimeType: String) {
        try {
            // Create Blob from text using @JsFun
            val blob = createTextBlob(content, mimeType)
            
            triggerDownload(blob, filename)
        } catch (e: Exception) {
            println("[FileDownloader] Error downloading text file: ${e.message}")
        }
    }
    
    private fun triggerDownload(blob: JsAny, filename: String) {
        // Create download URL
        val url = createObjectURL(blob)
        
        // Create anchor element with proper typing
        val link = document.createElement("a") as HTMLAnchorElement
        
        // Use @JsFun to set properties (WASM compatible)
        @Suppress("UNCHECKED_CAST")
        val linkAsJsAny = link as JsAny
        setHref(linkAsJsAny, url)
        setDownload(linkAsJsAny, filename)
        setStyleDisplay(linkAsJsAny, "none")
        
        document.body?.appendChild(link)
        clickElement(linkAsJsAny)
        
        // Cleanup after delay - return null to satisfy JsAny? requirement
        window.setTimeout({
            document.body?.removeChild(link)
            revokeObjectURL(url)
            null
        }, 100)
        
        logDownload("Download triggered: $filename")
    }
    
    actual fun isSupported(): Boolean {
        return try {
            isBlobSupported()
        } catch (e: Exception) {
            false
        }
    }
}
