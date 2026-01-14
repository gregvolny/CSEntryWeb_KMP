package gov.census.cspro.engine.functions

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import kotlin.js.JsAny

/**
 * Top-level JS interop functions for HTML dialogs
 */
@JsFun("(element, name, value) => { element[name] = value; }")
private external fun setElementProperty(element: JsAny, name: String, value: String)

@JsFun("(element, name) => { return element[name]; }")
private external fun getElementProperty(element: JsAny, name: String): String?

@JsFun("(element) => { element.focus(); }")
private external fun focusElement(element: JsAny)

@JsFun("(event) => { return event.data; }")
private external fun getEventData(event: JsAny): JsAny?

@JsFun("(data, key) => { return data[key]; }")
private external fun getDataProperty(data: JsAny, key: String): JsAny?

@JsFun("(obj) => { return typeof obj === 'string' ? obj : null; }")
private external fun asString(obj: JsAny?): String?

@JsFun("(iframe, token) => { try { if (iframe.contentWindow) { iframe.contentWindow.CSProAccessToken = token; } } catch(e) {} }")
private external fun injectAccessToken(iframe: JsAny, token: String)

/**
 * Display CS HTML Dialog Function - displays an HTML page in a dialog
 * Ported from Android DisplayCSHtmlDlgFunction.kt
 */
class DisplayCSHtmlDlgFunction(
    private val url: String,
    private val actionInvokerAccessTokenOverride: String? = null,
    private val title: String? = null,
    private val width: Int? = null,
    private val height: Int? = null
) : EngineFunction {
    
    private var result: String? = null
    private var cancelled = false
    
    fun getResult(): String? = result
    fun isCancelled(): Boolean = cancelled
    
    override suspend fun run() {
        val deferred = CompletableDeferred<String?>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-html-dialog-overlay"
        overlay.style.position = "fixed"
        overlay.style.top = "0"
        overlay.style.left = "0"
        overlay.style.width = "100%"
        overlay.style.height = "100%"
        overlay.style.backgroundColor = "rgba(0, 0, 0, 0.6)"
        overlay.style.display = "flex"
        overlay.style.justifyContent = "center"
        overlay.style.alignItems = "center"
        overlay.style.zIndex = "10000"
        
        // Create dialog container
        val dialog = document.createElement("div") as HTMLDivElement
        val dialogWidth = width ?: (window.innerWidth * 0.8).toInt()
        val dialogHeight = height ?: (window.innerHeight * 0.8).toInt()
        
        dialog.style.backgroundColor = "white"
        dialog.style.borderRadius = "8px"
        dialog.style.width = "${dialogWidth}px"
        dialog.style.height = "${dialogHeight}px"
        dialog.style.display = "flex"
        dialog.style.flexDirection = "column"
        dialog.style.boxShadow = "0 4px 20px rgba(0,0,0,0.4)"
        dialog.style.setProperty("overflow", "hidden")
        
        // Header with title and close button
        val header = document.createElement("div") as HTMLDivElement
        header.style.display = "flex"
        header.style.justifyContent = "space-between"
        header.style.alignItems = "center"
        header.style.padding = "10px 15px"
        header.style.backgroundColor = "#f8f9fa"
        header.style.borderBottom = "1px solid #dee2e6"
        
        val titleEl = document.createElement("span") as HTMLElement
        titleEl.textContent = title ?: "Dialog"
        titleEl.style.fontWeight = "bold"
        titleEl.style.fontSize = "16px"
        titleEl.style.color = "#333"
        header.appendChild(titleEl)
        
        val closeBtn = document.createElement("button") as HTMLButtonElement
        closeBtn.innerHTML = "&times;"
        closeBtn.style.background = "none"
        closeBtn.style.border = "none"
        closeBtn.style.fontSize = "24px"
        closeBtn.style.cursor = "pointer"
        closeBtn.style.color = "#666"
        closeBtn.style.padding = "0 5px"
        closeBtn.onclick = {
            document.body?.removeChild(overlay)
            cancelled = true
            deferred.complete(null)
        }
        header.appendChild(closeBtn)
        
        dialog.appendChild(header)
        
        // IFrame for HTML content
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        iframe.src = url
        iframe.style.flex = "1"
        iframe.style.border = "none"
        iframe.style.width = "100%"
        
        // Set up message listener for dialog results
        val messageHandler: (Event) -> Unit = { event ->
            val eventJs = event.unsafeCast<JsAny>()
            val data = getEventData(eventJs)
            if (data != null) {
                try {
                    val typeObj = getDataProperty(data, "type")
                    val messageType = asString(typeObj)
                    when (messageType) {
                        "cspro-dialog-result" -> {
                            val resultObj = getDataProperty(data, "result")
                            result = asString(resultObj)
                            document.body?.removeChild(overlay)
                            deferred.complete(result)
                        }
                        "cspro-dialog-close" -> {
                            document.body?.removeChild(overlay)
                            cancelled = true
                            deferred.complete(null)
                        }
                    }
                } catch (e: Exception) {
                    // Not a CSPro message, ignore
                }
            }
        }
        
        window.addEventListener("message", messageHandler)
        
        // Inject access token if provided
        iframe.onload = {
            if (actionInvokerAccessTokenOverride != null) {
                val iframeJs = iframe.unsafeCast<JsAny>()
                injectAccessToken(iframeJs, actionInvokerAccessTokenOverride)
            }
            null
        }
        
        dialog.appendChild(iframe)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        // Wait for result
        result = deferred.await()
        
        // Clean up message listener
        window.removeEventListener("message", messageHandler)
    }
}

/**
 * Edit Note Function - displays a note editor dialog
 * Ported from Android EditNoteFunction.java
 */
class EditNoteFunction(
    private val title: String = "Edit Note",
    private val initialNote: String = "",
    private val fieldName: String = ""
) : EngineFunction {
    
    private var noteText: String? = null
    private var cancelled = true
    
    fun getNoteText(): String? = noteText
    fun isCancelled(): Boolean = cancelled
    
    override suspend fun run() {
        val deferred = CompletableDeferred<String?>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-note-overlay"
        overlay.style.position = "fixed"
        overlay.style.top = "0"
        overlay.style.left = "0"
        overlay.style.width = "100%"
        overlay.style.height = "100%"
        overlay.style.backgroundColor = "rgba(0, 0, 0, 0.5)"
        overlay.style.display = "flex"
        overlay.style.justifyContent = "center"
        overlay.style.alignItems = "center"
        overlay.style.zIndex = "10000"
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.backgroundColor = "white"
        dialog.style.borderRadius = "8px"
        dialog.style.padding = "20px"
        dialog.style.width = "450px"
        dialog.style.maxWidth = "90vw"
        dialog.style.maxHeight = "80vh"
        dialog.style.display = "flex"
        dialog.style.flexDirection = "column"
        dialog.style.boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
        
        // Title
        val titleEl = document.createElement("h3") as HTMLElement
        titleEl.textContent = if (fieldName.isNotEmpty()) "$title - $fieldName" else title
        titleEl.style.margin = "0 0 15px 0"
        titleEl.style.color = "#333"
        dialog.appendChild(titleEl)
        
        // Textarea for note
        val textarea = document.createElement("textarea") as HTMLTextAreaElement
        textarea.value = initialNote
        textarea.style.width = "100%"
        textarea.style.height = "200px"
        textarea.style.padding = "10px"
        textarea.style.fontSize = "14px"
        textarea.style.border = "1px solid #ccc"
        textarea.style.borderRadius = "4px"
        textarea.style.boxSizing = "border-box"
        textarea.style.setProperty("resize", "vertical")
        textarea.style.marginBottom = "15px"
        textarea.style.fontFamily = "inherit"
        dialog.appendChild(textarea)
        
        // Buttons
        val buttonContainer = document.createElement("div") as HTMLDivElement
        buttonContainer.style.display = "flex"
        buttonContainer.style.justifyContent = "space-between"
        
        // Delete button (left side)
        val deleteBtn = document.createElement("button") as HTMLButtonElement
        deleteBtn.textContent = "Delete Note"
        deleteBtn.style.padding = "8px 20px"
        deleteBtn.style.border = "1px solid #dc3545"
        deleteBtn.style.borderRadius = "4px"
        deleteBtn.style.cursor = "pointer"
        deleteBtn.style.backgroundColor = "#fff"
        deleteBtn.style.color = "#dc3545"
        deleteBtn.onclick = {
            document.body?.removeChild(overlay)
            noteText = "" // Empty string signals deletion
            cancelled = false
            deferred.complete("")
        }
        buttonContainer.appendChild(deleteBtn)
        
        // Right side buttons
        val rightButtons = document.createElement("div") as HTMLDivElement
        rightButtons.style.display = "flex"
        rightButtons.style.setProperty("gap", "10px")
        
        val cancelBtn = document.createElement("button") as HTMLButtonElement
        cancelBtn.textContent = "Cancel"
        cancelBtn.style.padding = "8px 20px"
        cancelBtn.style.border = "1px solid #ccc"
        cancelBtn.style.borderRadius = "4px"
        cancelBtn.style.cursor = "pointer"
        cancelBtn.style.backgroundColor = "#fff"
        cancelBtn.onclick = {
            document.body?.removeChild(overlay)
            cancelled = true
            deferred.complete(null)
        }
        rightButtons.appendChild(cancelBtn)
        
        val saveBtn = document.createElement("button") as HTMLButtonElement
        saveBtn.textContent = "Save"
        saveBtn.style.padding = "8px 20px"
        saveBtn.style.border = "none"
        saveBtn.style.borderRadius = "4px"
        saveBtn.style.cursor = "pointer"
        saveBtn.style.backgroundColor = "#007bff"
        saveBtn.style.color = "#fff"
        saveBtn.onclick = {
            val note = textarea.value
            document.body?.removeChild(overlay)
            noteText = note
            cancelled = false
            deferred.complete(note)
        }
        rightButtons.appendChild(saveBtn)
        
        buttonContainer.appendChild(rightButtons)
        dialog.appendChild(buttonContainer)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        // Focus textarea
        textarea.focus()
        
        deferred.await()
    }
}
