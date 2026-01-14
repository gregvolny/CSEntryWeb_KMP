package gov.census.cspro.engine.functions

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileList
import kotlin.js.JsAny
import kotlin.js.JsString

/**
 * Top-level JsFun declarations for file operations
 */

@JsFun("(fileList, index) => { return fileList.item(index); }")
private external fun getFileFromList(fileList: JsAny, index: Int): JsAny?

@JsFun("(file) => { return file.name; }")
private external fun getFileName(file: JsAny): String

@JsFun("(file) => { return file.size; }")
private external fun getFileSize(file: JsAny): Double

@JsFun("(file) => { return file.type; }")
private external fun getFileType(file: JsAny): String

@JsFun("(fileList) => { return fileList.length; }")
private external fun getFileListLength(fileList: JsAny): Int

@JsFun("(content, mimeType) => { return new Blob([content], { type: mimeType }); }")
private external fun createTextBlob(content: String, mimeType: String): JsAny

@JsFun("(blob) => { return URL.createObjectURL(blob); }")
private external fun createObjectURL(blob: JsAny): String

@JsFun("(url) => { URL.revokeObjectURL(url); }")
private external fun revokeObjectURL(url: String)

@JsFun("(element, name, value) => { element[name] = value; }")
private external fun setElementProperty(element: JsAny, name: String, value: String)

@JsFun("(element) => { element.click(); }")
private external fun clickElement(element: JsAny)

/**
 * File selected from dialog
 */
data class SelectedFile(
    val name: String,
    val path: String, // Virtual path in OPFS
    val size: Long,
    val type: String,
    val content: ByteArray? = null
)

/**
 * Select Document Dialog Function
 * Ported from Android SelectDocumentDialog.kt
 * Uses HTML5 File API for file selection
 */
class SelectDocumentDialogFunction(
    private val mimeTypes: List<String> = listOf("*/*"),
    private val multiple: Boolean = false,
    private val title: String = "Select File"
) : EngineFunction {
    
    private var selectedFiles: List<SelectedFile> = emptyList()
    private var cancelled = true
    
    fun getSelectedFiles(): List<SelectedFile> = selectedFiles
    fun isCancelled(): Boolean = cancelled
    
    override suspend fun run() {
        val deferred = CompletableDeferred<List<SelectedFile>>()
        
        // Create hidden file input
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.multiple = multiple
        
        // Set accept types
        val acceptTypes = mimeTypes.map { mimeType ->
            when {
                mimeType == "*/*" -> "*"
                mimeType.endsWith("/*") -> mimeType.replace("/*", "")
                else -> mimeType
            }
        }
        input.accept = acceptTypes.joinToString(",")
        
        input.style.display = "none"
        
        input.onchange = {
            val files = input.files
            if (files != null) {
                val filesJs = files.unsafeCast<JsAny>()
                val length = getFileListLength(filesJs)
                if (length > 0) {
                    processFilesJs(filesJs, length, deferred)
                } else {
                    cancelled = true
                    deferred.complete(emptyList())
                }
            } else {
                cancelled = true
                deferred.complete(emptyList())
            }
            Unit
        }
        
        // Handle cancel (no files selected)
        input.oncancel = {
            cancelled = true
            deferred.complete(emptyList())
            Unit
        }
        
        document.body?.appendChild(input)
        input.click()
        
        // Clean up after selection
        selectedFiles = deferred.await()
        document.body?.removeChild(input)
    }
    
    private fun processFilesJs(fileList: JsAny, totalCount: Int, deferred: CompletableDeferred<List<SelectedFile>>) {
        val files = mutableListOf<SelectedFile>()
        
        for (i in 0 until totalCount) {
            val file = getFileFromList(fileList, i)
            if (file != null) {
                files.add(
                    SelectedFile(
                        name = getFileName(file),
                        path = "/selected/${getFileName(file)}", // Virtual path
                        size = getFileSize(file).toLong(),
                        type = getFileType(file)
                    )
                )
            }
        }
        
        cancelled = files.isEmpty()
        deferred.complete(files)
    }
}

/**
 * Save file dialog (triggers download)
 */
class SaveFileDialogFunction(
    private val suggestedName: String,
    private val content: String,
    private val mimeType: String = "text/plain"
) : EngineFunction {
    
    private var saved = false
    
    fun isSaved(): Boolean = saved
    
    override suspend fun run() {
        try {
            // Create blob
            val blob = createTextBlob(content, mimeType)
            val url = createObjectURL(blob)
            
            // Create download link
            val link = document.createElement("a") as HTMLElement
            val linkJs = link.unsafeCast<JsAny>()
            setElementProperty(linkJs, "href", url)
            setElementProperty(linkJs, "download", suggestedName)
            link.style.display = "none"
            
            document.body?.appendChild(link)
            clickElement(linkJs)
            
            // Cleanup after a short delay
            window.setTimeout({
                document.body?.removeChild(link)
                revokeObjectURL(url)
                null
            }, 100)
            
            saved = true
        } catch (e: Exception) {
            println("[SaveFileDialog] Error: ${e.message}")
            saved = false
        }
    }
}

/**
 * Save file dialog with bytes
 * Note: For WASM, byte array operations are more complex
 * This is a simplified version that converts to base64
 */
class SaveBytesDialogFunction(
    private val suggestedName: String,
    private val content: ByteArray,
    private val mimeType: String = "application/octet-stream"
) : EngineFunction {
    
    private var saved = false
    
    fun isSaved(): Boolean = saved
    
    override suspend fun run() {
        try {
            // Convert bytes to base64 string as a workaround
            // In production, you'd use proper ArrayBuffer handling
            val base64 = content.encodeToBase64()
            val dataUrl = "data:$mimeType;base64,$base64"
            
            // Create download link
            val link = document.createElement("a") as HTMLElement
            val linkJs = link.unsafeCast<JsAny>()
            setElementProperty(linkJs, "href", dataUrl)
            setElementProperty(linkJs, "download", suggestedName)
            link.style.display = "none"
            
            document.body?.appendChild(link)
            clickElement(linkJs)
            
            // Cleanup after a short delay
            window.setTimeout({
                document.body?.removeChild(link)
                null
            }, 100)
            
            saved = true
        } catch (e: Exception) {
            println("[SaveBytesDialog] Error: ${e.message}")
            saved = false
        }
    }
    
    private fun ByteArray.encodeToBase64(): String {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val sb = StringBuilder()
        
        var i = 0
        while (i < size) {
            val b0 = this[i].toInt() and 0xFF
            val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0
            
            sb.append(base64Chars[(b0 shr 2)])
            sb.append(base64Chars[((b0 and 0x03) shl 4) or (b1 shr 4)])
            
            if (i + 1 < size) {
                sb.append(base64Chars[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            } else {
                sb.append('=')
            }
            
            if (i + 2 < size) {
                sb.append(base64Chars[b2 and 0x3F])
            } else {
                sb.append('=')
            }
            
            i += 3
        }
        
        return sb.toString()
    }
}
