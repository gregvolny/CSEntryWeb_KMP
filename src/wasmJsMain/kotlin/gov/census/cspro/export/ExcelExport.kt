package gov.census.cspro.export

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLScriptElement
import kotlin.js.JsAny

/**
 * Top-level JsFun declarations for Excel/CSV export operations
 * All JS interop must be at top level for WASM compatibility
 */

// XLSX Library availability
@JsFun("() => typeof XLSX !== 'undefined'")
private external fun isXlsxAvailable(): Boolean

@JsFun("() => XLSX")
private external fun getXlsx(): JsAny

// Object/Array creation and manipulation
@JsFun("() => ({})")
private external fun createJsObject(): JsAny

@JsFun("() => []")
private external fun createJsArray(): JsAny

@JsFun("(arr, item) => { arr.push(item); }")
private external fun pushToArray(arr: JsAny, item: JsAny)

@JsFun("(arr, item) => { arr.push(item); }")
private external fun pushStringToArray(arr: JsAny, item: String)

@JsFun("(arr, item) => { arr.push(item); }")
private external fun pushNumberToArray(arr: JsAny, item: Double)

@JsFun("(arr) => arr.length")
private external fun getArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun getArrayElement(arr: JsAny, idx: Int): JsAny?

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setObjectProperty(obj: JsAny, key: String, value: JsAny)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setObjectStringProperty(obj: JsAny, key: String, value: String)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setObjectIntProperty(obj: JsAny, key: String, value: Int)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setObjectBoolProperty(obj: JsAny, key: String, value: Boolean)

@JsFun("(obj, key) => obj[key]")
private external fun getObjectProperty(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : ''")
private external fun getObjectStringProperty(obj: JsAny, key: String): String

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : 0")
private external fun getObjectIntProperty(obj: JsAny, key: String): Int

// XLSX-specific operations
@JsFun("(xlsx) => xlsx.utils.book_new()")
private external fun xlsxBookNew(xlsx: JsAny): JsAny

@JsFun("(xlsx, data) => xlsx.utils.aoa_to_sheet(data)")
private external fun xlsxAoaToSheet(xlsx: JsAny, data: JsAny): JsAny

@JsFun("(xlsx, workbook, sheet, name) => xlsx.utils.book_append_sheet(workbook, sheet, name)")
private external fun xlsxBookAppendSheet(xlsx: JsAny, workbook: JsAny, sheet: JsAny, name: String)

@JsFun("(xlsx, workbook, options) => xlsx.write(workbook, options)")
private external fun xlsxWrite(xlsx: JsAny, workbook: JsAny, options: JsAny): JsAny

@JsFun("(xlsx, data, options) => xlsx.read(data, options)")
private external fun xlsxRead(xlsx: JsAny, data: JsAny, options: JsAny): JsAny

@JsFun("(xlsx, sheet, options) => xlsx.utils.sheet_to_json(sheet, options)")
private external fun xlsxSheetToJson(xlsx: JsAny, sheet: JsAny, options: JsAny): JsAny

@JsFun("(workbook) => workbook.SheetNames")
private external fun getWorkbookSheetNames(workbook: JsAny): JsAny

@JsFun("(workbook, name) => workbook.Sheets[name]")
private external fun getWorkbookSheet(workbook: JsAny, name: String): JsAny?

// Blob and download operations
@JsFun("(content, mimeType) => new Blob([content], { type: mimeType })")
private external fun createTextBlob(content: String, mimeType: String): JsAny

@JsFun("(data, mimeType) => new Blob([data], { type: mimeType })")
private external fun createArrayBlob(data: JsAny, mimeType: String): JsAny

@JsFun("(blob) => URL.createObjectURL(blob)")
private external fun createBlobUrl(blob: JsAny): String

@JsFun("(url) => URL.revokeObjectURL(url)")
private external fun revokeBlobUrl(url: String)

// Value conversion
@JsFun("(val) => val == null")
private external fun isNullOrUndefined(value: JsAny?): Boolean

@JsFun("(val) => typeof val === 'string' ? val : String(val)")
private external fun jsToString(value: JsAny): String

@JsFun("(str) => str")
private external fun stringToJs(str: String): JsAny

@JsFun("(num) => num")
private external fun numberToJs(num: Double): JsAny

@JsFun("(b) => b")
private external fun boolToJs(b: Boolean): JsAny

@JsFun("() => null")
private external fun jsNull(): JsAny

// Element property operations
@JsFun("(element, name, value) => { element[name] = value; }")
private external fun setElementProperty(element: JsAny, name: String, value: String)

@JsFun("(element) => { element.click(); }")
private external fun clickElement(element: JsAny)

// Date operations
@JsFun("() => new Date().toISOString()")
private external fun getCurrentIsoDate(): String

// Console logging
@JsFun("(msg) => console.log('[ExcelExport] ' + msg)")
private external fun logMessage(msg: String)

@JsFun("(msg) => console.error('[ExcelExport] ' + msg)")
private external fun logError(msg: String)

// Uint8Array operations for binary data
@JsFun("(size) => new Uint8Array(size)")
private external fun createUint8Array(size: Int): JsAny

@JsFun("(arr, idx, value) => { arr[idx] = value; }")
private external fun setUint8ArrayValue(arr: JsAny, idx: Int, value: Int)

@JsFun("(arr, idx) => arr[idx]")
private external fun getUint8ArrayValue(arr: JsAny, idx: Int): Int

@JsFun("(arr) => arr.length")
private external fun getUint8ArrayLength(arr: JsAny): Int

@JsFun("(arr) => arr.buffer")
private external fun getUint8ArrayBuffer(arr: JsAny): JsAny

// Base64 encoding
@JsFun("(str) => btoa(str)")
private external fun btoaJs(str: String): String

@JsFun("(bytes) => { let binary = ''; const len = bytes.length; for (let i = 0; i < len; i++) { binary += String.fromCharCode(bytes[i]); } return btoa(binary); }")
private external fun uint8ArrayToBase64(bytes: JsAny): String

/**
 * Excel Export using SheetJS (xlsx library) or fallback to CSV
 * 
 * Provides Excel file generation and export capabilities for CSEntry data.
 * When SheetJS is available: Supports XLSX, XLS, CSV, ODS formats.
 * Without SheetJS: Falls back to CSV export only.
 * 
 * SheetJS CDN: https://cdn.sheetjs.com/xlsx-latest/package/dist/xlsx.full.min.js
 */
object ExcelExport {
    
    private var xlsxLoaded = false
    private var xlsxLibrary: JsAny? = null
    
    /**
     * Load SheetJS library from CDN
     */
    suspend fun initialize(): Boolean {
        if (xlsxLoaded) return true
        
        return try {
            loadXlsxLibrary()
            if (isXlsxAvailable()) {
                xlsxLibrary = getXlsx()
                xlsxLoaded = true
                logMessage("SheetJS loaded successfully")
            } else {
                logMessage("SheetJS not available, using CSV fallback")
                xlsxLoaded = false
            }
            xlsxLoaded
        } catch (e: Exception) {
            logError("Failed to load SheetJS: ${e.message}")
            xlsxLoaded = false
            false
        }
    }
    
    /**
     * Check if SheetJS is loaded
     */
    fun isSheetJsAvailable(): Boolean = xlsxLoaded && xlsxLibrary != null
    
    /**
     * Load XLSX library from CDN
     */
    private suspend fun loadXlsxLibrary() {
        val existingScript = document.querySelector("script[src*='xlsx']")
        if (existingScript != null) {
            return
        }
        
        val script = document.createElement("script") as HTMLScriptElement
        script.src = "https://cdn.sheetjs.com/xlsx-latest/package/dist/xlsx.full.min.js"
        script.async = true
        
        val deferred = CompletableDeferred<Boolean>()
        
        script.addEventListener("load", { 
            deferred.complete(true)
        })
        
        script.addEventListener("error", { 
            deferred.complete(false)
        })
        
        document.head?.appendChild(script)
        deferred.await()
    }
    
    /**
     * Create a workbook from case data
     */
    fun createWorkbook(): Workbook {
        return if (xlsxLoaded && xlsxLibrary != null) {
            val native = xlsxBookNew(xlsxLibrary!!)
            Workbook(native, useSheetJs = true)
        } else {
            // Create a simple data structure for CSV fallback
            Workbook(createJsObject(), useSheetJs = false)
        }
    }
    
    /**
     * Add a worksheet to a workbook from array of arrays
     */
    fun addWorksheet(
        workbook: Workbook,
        sheetName: String,
        data: List<List<Any?>>,
        options: WorksheetOptions = WorksheetOptions()
    ) {
        if (workbook.useSheetJs && xlsxLibrary != null) {
            addWorksheetSheetJs(workbook, sheetName, data, options)
        } else {
            addWorksheetFallback(workbook, sheetName, data)
        }
    }
    
    private fun addWorksheetSheetJs(
        workbook: Workbook,
        sheetName: String,
        data: List<List<Any?>>,
        options: WorksheetOptions
    ) {
        val xlsx = xlsxLibrary ?: return
        
        // Convert data to JavaScript array of arrays
        val jsData = createJsArray()
        data.forEach { row ->
            val jsRow = createJsArray()
            row.forEach { cell ->
                val jsCell = convertToJsValue(cell)
                pushToArray(jsRow, jsCell)
            }
            pushToArray(jsData, jsRow)
        }
        
        // Create worksheet from array of arrays
        val worksheet = xlsxAoaToSheet(xlsx, jsData)
        
        // Apply column widths if specified
        if (options.columnWidths.isNotEmpty()) {
            val cols = createJsArray()
            options.columnWidths.forEach { width ->
                val colDef = createJsObject()
                setObjectIntProperty(colDef, "wch", width)
                pushToArray(cols, colDef)
            }
            setObjectProperty(worksheet, "!cols", cols)
        }
        
        // Add worksheet to workbook
        xlsxBookAppendSheet(xlsx, workbook.native, worksheet, sheetName)
        workbook.addSheetName(sheetName)
    }
    
    private fun addWorksheetFallback(
        workbook: Workbook,
        sheetName: String,
        data: List<List<Any?>>
    ) {
        // Store data in the workbook for later CSV export
        workbook.sheetsData[sheetName] = data
        workbook.addSheetName(sheetName)
    }
    
    /**
     * Add a worksheet from list of objects
     */
    fun addWorksheetFromObjects(
        workbook: Workbook,
        sheetName: String,
        data: List<Map<String, Any?>>,
        options: WorksheetOptions = WorksheetOptions()
    ) {
        if (data.isEmpty()) {
            addWorksheet(workbook, sheetName, emptyList(), options)
            return
        }
        
        // Get headers from first row
        val headers = data.first().keys.toList()
        
        // Convert to array of arrays
        val arrayData = mutableListOf<List<Any?>>()
        arrayData.add(headers) // Header row
        
        data.forEach { row ->
            arrayData.add(headers.map { header -> row[header] })
        }
        
        addWorksheet(workbook, sheetName, arrayData, options)
    }
    
    /**
     * Export workbook to ByteArray
     */
    fun exportToBytes(workbook: Workbook, format: ExcelExportFormat = ExcelExportFormat.XLSX): ByteArray {
        return if (workbook.useSheetJs && xlsxLibrary != null) {
            exportToBytesSheetJs(workbook, format)
        } else {
            exportToBytesCsv(workbook)
        }
    }
    
    private fun exportToBytesSheetJs(workbook: Workbook, format: ExcelExportFormat): ByteArray {
        val xlsx = xlsxLibrary ?: return ByteArray(0)
        
        val options = createJsObject()
        setObjectStringProperty(options, "bookType", format.bookType)
        setObjectStringProperty(options, "type", "array")
        
        val result = xlsxWrite(xlsx, workbook.native, options)
        val length = getUint8ArrayLength(result)
        
        return ByteArray(length) { idx ->
            getUint8ArrayValue(result, idx).toByte()
        }
    }
    
    private fun exportToBytesCsv(workbook: Workbook): ByteArray {
        val csv = generateCsvContent(workbook)
        return csv.encodeToByteArray()
    }
    
    /**
     * Export workbook and download
     */
    fun downloadWorkbook(workbook: Workbook, filename: String, format: ExcelExportFormat = ExcelExportFormat.XLSX) {
        if (workbook.useSheetJs && xlsxLibrary != null && format != ExcelExportFormat.CSV) {
            downloadWorkbookSheetJs(workbook, filename, format)
        } else {
            downloadWorkbookCsv(workbook, filename)
        }
    }
    
    private fun downloadWorkbookSheetJs(workbook: Workbook, filename: String, format: ExcelExportFormat) {
        val xlsx = xlsxLibrary ?: return
        
        val options = createJsObject()
        setObjectStringProperty(options, "bookType", format.bookType)
        setObjectStringProperty(options, "type", "array")
        
        val result = xlsxWrite(xlsx, workbook.native, options)
        val blob = createArrayBlob(result, format.mimeType)
        val url = createBlobUrl(blob)
        
        val link = document.createElement("a") as HTMLAnchorElement
        setElementProperty(link.unsafeCast<JsAny>(), "href", url)
        setElementProperty(link.unsafeCast<JsAny>(), "download", "$filename.${format.extension}")
        clickElement(link.unsafeCast<JsAny>())
        
        // Revoke URL after a short delay
        window.setTimeout({
            revokeBlobUrl(url)
            null
        }, 100)
    }
    
    private fun downloadWorkbookCsv(workbook: Workbook, filename: String) {
        val csv = generateCsvContent(workbook)
        val blob = createTextBlob(csv, "text/csv;charset=utf-8;")
        val url = createBlobUrl(blob)
        
        val link = document.createElement("a") as HTMLAnchorElement
        setElementProperty(link.unsafeCast<JsAny>(), "href", url)
        setElementProperty(link.unsafeCast<JsAny>(), "download", "$filename.csv")
        clickElement(link.unsafeCast<JsAny>())
        
        window.setTimeout({
            revokeBlobUrl(url)
            null
        }, 100)
    }
    
    /**
     * Generate CSV content from workbook
     */
    private fun generateCsvContent(workbook: Workbook): String {
        val sb = StringBuilder()
        
        workbook.sheetsData.forEach { (sheetName, data) ->
            if (workbook.sheetsData.size > 1) {
                sb.appendLine("# Sheet: $sheetName")
            }
            
            data.forEach { row ->
                val csvRow = row.map { cell ->
                    escapeCsvValue(cell?.toString() ?: "")
                }.joinToString(",")
                sb.appendLine(csvRow)
            }
            
            if (workbook.sheetsData.size > 1) {
                sb.appendLine()
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Escape CSV value
     */
    private fun escapeCsvValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    /**
     * Export workbook to Base64 string
     */
    fun exportToBase64(workbook: Workbook, format: ExcelExportFormat = ExcelExportFormat.XLSX): String {
        val bytes = exportToBytes(workbook, format)
        val uint8 = createUint8Array(bytes.size)
        bytes.forEachIndexed { idx, byte ->
            setUint8ArrayValue(uint8, idx, byte.toInt() and 0xFF)
        }
        return uint8ArrayToBase64(uint8)
    }
    
    /**
     * Read workbook from bytes (requires SheetJS)
     */
    fun readWorkbook(data: ByteArray): Workbook? {
        if (!xlsxLoaded || xlsxLibrary == null) {
            logError("SheetJS not available for reading workbooks")
            return null
        }
        
        val xlsx = xlsxLibrary!!
        
        val uint8Array = createUint8Array(data.size)
        data.forEachIndexed { idx, byte ->
            setUint8ArrayValue(uint8Array, idx, byte.toInt() and 0xFF)
        }
        
        val options = createJsObject()
        setObjectStringProperty(options, "type", "array")
        
        val native = xlsxRead(xlsx, uint8Array, options)
        return Workbook(native, useSheetJs = true)
    }
    
    /**
     * Get sheet names from workbook
     */
    fun getSheetNames(workbook: Workbook): List<String> {
        return if (workbook.useSheetJs) {
            val names = getWorkbookSheetNames(workbook.native)
            val length = getArrayLength(names)
            (0 until length).mapNotNull { idx ->
                getArrayElement(names, idx)?.let { jsToString(it) }
            }
        } else {
            workbook.sheetsData.keys.toList()
        }
    }
    
    /**
     * Read sheet data as array of arrays (requires SheetJS)
     */
    fun readSheetData(workbook: Workbook, sheetName: String): List<List<Any?>> {
        if (!workbook.useSheetJs) {
            return workbook.sheetsData[sheetName] ?: emptyList()
        }
        
        val xlsx = xlsxLibrary ?: return emptyList()
        val sheet = getWorkbookSheet(workbook.native, sheetName) ?: return emptyList()
        
        val options = createJsObject()
        setObjectIntProperty(options, "header", 1) // Return as array of arrays
        
        val data = xlsxSheetToJson(xlsx, sheet, options)
        val length = getArrayLength(data)
        
        return (0 until length).mapNotNull { i ->
            val row = getArrayElement(data, i) ?: return@mapNotNull null
            val rowLength = getArrayLength(row)
            (0 until rowLength).map { j ->
                val cell = getArrayElement(row, j)
                if (isNullOrUndefined(cell)) null else jsToString(cell!!)
            }
        }
    }
    
    /**
     * Convert Kotlin value to JS value
     */
    private fun convertToJsValue(value: Any?): JsAny {
        return when (value) {
            null -> jsNull()
            is String -> stringToJs(value)
            is Number -> numberToJs(value.toDouble())
            is Boolean -> boolToJs(value)
            else -> stringToJs(value.toString())
        }
    }
    
    /**
     * Get cell address from row/column
     */
    private fun getCellAddress(row: Int, col: Int): String {
        val colLetter = getColumnLetter(col)
        return "$colLetter${row + 1}"
    }
    
    /**
     * Get column letter from index (0-based)
     */
    private fun getColumnLetter(col: Int): String {
        var n = col
        var result = ""
        while (n >= 0) {
            result = ('A' + (n % 26)) + result
            n = n / 26 - 1
        }
        return result
    }
}

/**
 * Workbook wrapper
 */
class Workbook(
    internal val native: JsAny,
    internal val useSheetJs: Boolean
) {
    // Store sheet data for CSV fallback
    internal val sheetsData: MutableMap<String, List<List<Any?>>> = mutableMapOf()
    private val sheetNamesList: MutableList<String> = mutableListOf()
    
    internal fun addSheetName(name: String) {
        if (!sheetNamesList.contains(name)) {
            sheetNamesList.add(name)
        }
    }
    
    /**
     * Get sheet names
     */
    val sheetNames: List<String>
        get() = ExcelExport.getSheetNames(this)
    
    /**
     * Add worksheet from array data
     */
    fun addSheet(name: String, data: List<List<Any?>>, options: WorksheetOptions = WorksheetOptions()) {
        ExcelExport.addWorksheet(this, name, data, options)
    }
    
    /**
     * Add worksheet from object data
     */
    fun addSheetFromObjects(name: String, data: List<Map<String, Any?>>, options: WorksheetOptions = WorksheetOptions()) {
        ExcelExport.addWorksheetFromObjects(this, name, data, options)
    }
    
    /**
     * Read sheet data
     */
    fun readSheet(name: String): List<List<Any?>> {
        return ExcelExport.readSheetData(this, name)
    }
    
    /**
     * Export to bytes
     */
    fun toBytes(format: ExcelExportFormat = ExcelExportFormat.XLSX): ByteArray {
        return ExcelExport.exportToBytes(this, format)
    }
    
    /**
     * Export to Base64
     */
    fun toBase64(format: ExcelExportFormat = ExcelExportFormat.XLSX): String {
        return ExcelExport.exportToBase64(this, format)
    }
    
    /**
     * Download workbook
     */
    fun download(filename: String, format: ExcelExportFormat = ExcelExportFormat.XLSX) {
        ExcelExport.downloadWorkbook(this, filename, format)
    }
}

/**
 * Excel export format options
 */
enum class ExcelExportFormat(
    val bookType: String,
    val extension: String,
    val mimeType: String
) {
    XLSX("xlsx", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    XLS("xls", "xls", "application/vnd.ms-excel"),
    CSV("csv", "csv", "text/csv"),
    ODS("ods", "ods", "application/vnd.oasis.opendocument.spreadsheet"),
    HTML("html", "html", "text/html"),
    TXT("txt", "txt", "text/plain")
}

/**
 * Worksheet options
 */
data class WorksheetOptions(
    val columnWidths: List<Int> = emptyList(),
    val rowHeights: List<Int> = emptyList(),
    val freezeRows: Int = 0,
    val freezeCols: Int = 0,
    val autoFilter: Boolean = false,
    val defaultRowHeight: Int? = null,
    val defaultColWidth: Int? = null
)

/**
 * Cell style options
 */
data class CellStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: Int? = null,
    val fontColor: String? = null,
    val backgroundColor: String? = null,
    val horizontalAlign: String? = null, // "left", "center", "right"
    val verticalAlign: String? = null, // "top", "center", "bottom"
    val wrapText: Boolean = false,
    val borderStyle: String? = null, // "thin", "medium", "thick"
    val borderColor: String? = null,
    val numberFormat: String? = null // e.g., "#,##0.00", "0%", "yyyy-mm-dd"
)

/**
 * Case Data Exporter - Exports CSEntry case data to Excel/CSV
 */
object CaseDataExporter {
    
    /**
     * Export case data to Excel/CSV workbook
     */
    suspend fun exportCasesToExcel(
        cases: List<CaseData>,
        dictionaryName: String,
        options: CaseExportOptions = CaseExportOptions()
    ): Workbook {
        // Try to initialize SheetJS, but continue even if it fails
        ExcelExport.initialize()
        
        val workbook = ExcelExport.createWorkbook()
        
        // Main data sheet
        val headers = if (cases.isNotEmpty()) {
            cases.first().fields.keys.toList()
        } else {
            emptyList()
        }
        
        val data = mutableListOf<List<Any?>>()
        
        // Add header row
        if (options.includeHeaders) {
            data.add(headers)
        }
        
        // Add data rows
        cases.forEach { case ->
            val row = headers.map { header -> case.fields[header] }
            data.add(row)
        }
        
        val worksheetOptions = WorksheetOptions(
            columnWidths = headers.map { 15 }, // Default column width
            freezeRows = if (options.includeHeaders) 1 else 0,
            autoFilter = options.includeHeaders
        )
        
        ExcelExport.addWorksheet(workbook, dictionaryName, data, worksheetOptions)
        
        // Add metadata sheet if requested
        if (options.includeMetadata) {
            val metadata = listOf(
                listOf("Dictionary", dictionaryName),
                listOf("Export Date", getCurrentIsoDate()),
                listOf("Total Cases", cases.size),
                listOf("Fields", headers.size)
            )
            ExcelExport.addWorksheet(workbook, "Metadata", metadata)
        }
        
        return workbook
    }
    
    /**
     * Export with multiple record types
     */
    suspend fun exportMultiRecordCases(
        casesByRecord: Map<String, List<CaseData>>,
        dictionaryName: String,
        options: CaseExportOptions = CaseExportOptions()
    ): Workbook {
        ExcelExport.initialize()
        
        val workbook = ExcelExport.createWorkbook()
        
        casesByRecord.forEach { (recordName, cases) ->
            if (cases.isEmpty()) return@forEach
            
            val headers = cases.first().fields.keys.toList()
            val data = mutableListOf<List<Any?>>()
            
            if (options.includeHeaders) {
                data.add(headers)
            }
            
            cases.forEach { case ->
                val row = headers.map { header -> case.fields[header] }
                data.add(row)
            }
            
            val worksheetOptions = WorksheetOptions(
                columnWidths = headers.map { 15 },
                freezeRows = if (options.includeHeaders) 1 else 0
            )
            
            ExcelExport.addWorksheet(workbook, recordName, data, worksheetOptions)
        }
        
        return workbook
    }
    
    /**
     * Quick export to CSV string
     */
    fun exportToCsv(
        cases: List<CaseData>,
        includeHeaders: Boolean = true
    ): String {
        if (cases.isEmpty()) return ""
        
        val headers = cases.first().fields.keys.toList()
        val sb = StringBuilder()
        
        if (includeHeaders) {
            sb.appendLine(headers.joinToString(",") { escapeCsvValue(it) })
        }
        
        cases.forEach { case ->
            val row = headers.map { header ->
                escapeCsvValue(case.fields[header]?.toString() ?: "")
            }
            sb.appendLine(row.joinToString(","))
        }
        
        return sb.toString()
    }
    
    private fun escapeCsvValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}

/**
 * Case data representation
 */
data class CaseData(
    val caseId: String,
    val fields: Map<String, Any?>
)

/**
 * Case export options
 */
data class CaseExportOptions(
    val includeHeaders: Boolean = true,
    val styleHeaders: Boolean = true,
    val includeMetadata: Boolean = true,
    val format: ExcelExportFormat = ExcelExportFormat.XLSX,
    val filename: String = "export"
)
