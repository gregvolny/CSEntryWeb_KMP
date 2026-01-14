package gov.census.cspro.engine.dialogs

import gov.census.cspro.engine.CSProEngineService
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * Dialog Manager for CSPro HTML Dialogs
 * Uses the native CSPro HTML dialog templates via iframes
 * 
 * WASM-compatible implementation using JsFun annotations for JS interop
 */

// JsFun declarations for WASM-compatible JS interop
@JsFun("(data) => JSON.stringify(data)")
private external fun jsonStringify(data: JsAny): String

@JsFun("(jsonStr) => JSON.parse(jsonStr)")
private external fun jsonParse(jsonStr: String): JsAny

@JsFun("(obj, key) => obj[key]")
private external fun getJsProperty(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun getJsStringProperty(obj: JsAny, key: String): String?

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : null")  
private external fun getJsIntProperty(obj: JsAny, key: String): Int?

@JsFun("(win, msg, origin) => win.postMessage(msg, origin)")
private external fun postMessageToWindow(win: JsAny, msg: JsAny, origin: String)

@JsFun("(type, input) => ({ type: type, input: input })")
private external fun createInputMessage(type: String, input: JsAny): JsAny

@JsFun("(type, requestId, result) => ({ type: type, requestId: requestId, result: result })")
private external fun createResponseMessage(type: String, requestId: Int, result: JsAny?): JsAny

@JsFun("(type, requestId, error) => ({ type: type, requestId: requestId, error: error })")
private external fun createErrorMessage(type: String, requestId: Int, error: String): JsAny

@JsFun("(dialogId) => { try { window.history.pushState({ csproDialog: dialogId }, '', window.location.href); return true; } catch(e) { return false; } }")
private external fun pushDialogHistory(dialogId: String): Boolean

@JsFun("(dialogId) => { try { const s = window.history.state; return !!(s && s.csproDialog === dialogId); } catch(e) { return false; } }")
private external fun isCurrentDialogHistory(dialogId: String): Boolean

@JsFun("() => { try { window.history.back(); } catch(e) { } }")
private external fun popDialogHistory(): Unit

object CSProDialogManager {
    
    // Base path to dialogs (configurable)
    var dialogBasePath = "/dialogs"
    
    // Track active dialogs
    private val activeDialogs = mutableMapOf<String, DialogInstance>()
    private var dialogCounter = 0
    
    /**
     * Show an error message dialog
     */
    suspend fun showErrmsg(
        title: String,
        message: String,
        buttons: List<String>? = null
    ): Int {
        val input = buildJsonObject {
            put("title", title)
            put("message", message)
            if (buttons != null && buttons.isNotEmpty()) {
                put("buttons", buildJsonArray {
                    buttons.forEachIndexed { index, caption ->
                        add(buildJsonObject {
                            put("caption", caption)
                            put("index", index)
                        })
                    }
                })
            }
        }
        
        val result = showDialog("errmsg.html", input)
        return result?.jsonObject?.get("index")?.jsonPrimitive?.intOrNull ?: 0
    }
    
    /**
     * Show a choice/select dialog
     */
    suspend fun showChoice(
        title: String,
        choices: List<String>,
        allowDirectInput: Boolean = false
    ): ChoiceResult {
        val input = buildJsonObject {
            put("title", title)
            put("allowDirectInput", allowDirectInput)
            put("choices", buildJsonArray {
                choices.forEachIndexed { index, caption ->
                    add(buildJsonObject {
                        put("caption", caption)
                        put("index", index)
                    })
                }
            })
        }
        
        val result = showDialog("select.html", input)
        
        return if (result != null) {
            val obj = result.jsonObject
            ChoiceResult(
                selectedIndex = obj["index"]?.jsonPrimitive?.intOrNull ?: -1,
                directInput = obj["directInput"]?.jsonPrimitive?.contentOrNull
            )
        } else {
            ChoiceResult(selectedIndex = -1, directInput = null)
        }
    }
    
    /**
     * Show text input dialog
     */
    suspend fun showTextInput(
        title: String,
        initialValue: String = "",
        multiline: Boolean = false
    ): String? {
        val input = buildJsonObject {
            put("title", title)
            put("initialValue", initialValue)
            put("multiline", multiline)
        }
        
        val result = showDialog("text-input.html", input)
        return result?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
    }
    
    /**
     * Show a note editing dialog
     */
    suspend fun showNoteEdit(
        title: String,
        note: String,
        operatorId: String
    ): String? {
        val input = buildJsonObject {
            put("title", title)
            put("note", note)
            put("operatorId", operatorId)
        }
        
        val result = showDialog("note-edit.html", input)
        return result?.jsonObject?.get("note")?.jsonPrimitive?.contentOrNull
    }
    
    /**
     * Show note review dialog
     */
    suspend fun showNoteReview(
        notes: List<NoteItem>
    ): NoteReviewResult {
        val input = buildJsonObject {
            put("notes", buildJsonArray {
                notes.forEach { note ->
                    add(buildJsonObject {
                        put("index", note.index)
                        put("text", note.text)
                        put("label", note.label)
                        put("isFieldNote", note.isFieldNote)
                    })
                }
            })
        }
        
        val result = showDialog("note-review.html", input)
        
        return if (result != null) {
            val obj = result.jsonObject
            NoteReviewResult(
                action = obj["action"]?.jsonPrimitive?.contentOrNull ?: "cancel",
                selectedNoteIndex = obj["selectedIndex"]?.jsonPrimitive?.intOrNull ?: -1
            )
        } else {
            NoteReviewResult(action = "cancel", selectedNoteIndex = -1)
        }
    }
    
    /**
     * Show file selection dialog
     */
    suspend fun showFileDialog(
        title: String,
        rootDir: String,
        filter: String? = null,
        selectDirectory: Boolean = false
    ): String? {
        val input = buildJsonObject {
            put("title", title)
            put("rootDir", rootDir)
            if (filter != null) put("filter", filter)
            put("selectDirectory", selectDirectory)
        }
        
        val result = showDialog("Path-selectFile.html", input)
        return result?.jsonObject?.get("path")?.jsonPrimitive?.contentOrNull
    }
    
    /**
     * Show image viewer dialog
     */
    suspend fun showImageViewer(
        imageUrl: String,
        title: String? = null
    ) {
        val input = buildJsonObject {
            put("imageUrl", imageUrl)
            if (title != null) put("title", title)
        }
        
        showDialog("Image-view.html", input)
    }
    
    /**
     * Show a progress dialog (non-blocking, returns handle)
     */
    fun showProgressDialog(
        title: String,
        message: String,
        total: Int = 100,
        cancellable: Boolean = false
    ): ProgressDialogHandle {
        val dialogId = "cspro-progress-${++dialogCounter}"
        
        // Create overlay
        val overlay = (document.createElement("div") as HTMLDivElement).apply {
            id = "$dialogId-overlay"
            className = "cspro-dialog-overlay"
            style.apply {
                position = "fixed"
                top = "0"
                left = "0"
                width = "100vw"
                height = "100vh"
                backgroundColor = "rgba(0,0,0,0.5)"
                display = "flex"
                justifyContent = "center"
                alignItems = "center"
                zIndex = "9999"
            }
        }
        
        // Create dialog container
        val container = (document.createElement("div") as HTMLDivElement).apply {
            id = dialogId
            className = "cspro-progress-dialog"
            style.apply {
                backgroundColor = "white"
                borderRadius = "8px"
                padding = "24px"
                minWidth = "300px"
                maxWidth = "400px"
                boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
            }
        }
        
        // Title
        val titleElement = (document.createElement("div") as HTMLDivElement).apply {
            textContent = title
            style.apply {
                fontWeight = "bold"
                fontSize = "16px"
                marginBottom = "12px"
            }
        }
        
        // Message
        val messageElement = (document.createElement("div") as HTMLDivElement).apply {
            id = "$dialogId-message"
            textContent = message
            style.apply {
                fontSize = "14px"
                color = "#333"
            }
        }
        
        // Progress bar container
        val progressContainer = (document.createElement("div") as HTMLDivElement).apply {
            className = "progress-bar-container"
            style.apply {
                backgroundColor = "#e0e0e0"
                borderRadius = "4px"
                height = "8px"
                this.width = "100%"
                marginBottom = "16px"
                setProperty("overflow", "hidden")
            }
        }
        
        // Progress bar fill
        val progressBar = (document.createElement("div") as HTMLDivElement).apply {
            id = "$dialogId-bar"
            className = "progress-bar-fill"
            style.apply {
                backgroundColor = "#2196F3"
                height = "100%"
                this.width = "0%"
                transition = "width 0.2s ease"
            }
        }
        progressContainer.appendChild(progressBar)
        
        container.appendChild(titleElement)
        container.appendChild(messageElement)
        container.appendChild(progressContainer)
        
        // Cancel button (if cancellable)
        var cancelled = false
        if (cancellable) {
            val cancelButton = (document.createElement("button") as HTMLButtonElement).apply {
                textContent = "Cancel"
                className = "btn btn-secondary"
                setAttribute("style", "display: block; margin: 0 auto; padding: 8px 24px; cursor: pointer;")
            }
            cancelButton.addEventListener("click", { cancelled = true })
            container.appendChild(cancelButton)
        }
        
        overlay.appendChild(container)
        document.body?.appendChild(overlay)
        
        return ProgressDialogHandle(
            dialogId = dialogId,
            overlay = overlay,
            messageElement = messageElement,
            progressBar = progressBar,
            total = total,
            isCancelledFn = { cancelled }
        )
    }
    
    /**
     * Handle for controlling a progress dialog
     */
    class ProgressDialogHandle(
        private val dialogId: String,
        private val overlay: HTMLDivElement,
        private val messageElement: HTMLDivElement,
        private val progressBar: HTMLDivElement,
        private var total: Int,
        private val isCancelledFn: () -> Boolean
    ) {
        private var currentProgress = 0
        
        fun update(message: String? = null, progress: Int? = null) {
            if (message != null) {
                messageElement.textContent = message
            }
            if (progress != null) {
                currentProgress = progress
                val percentage = if (total > 0) (progress * 100 / total) else 0
                progressBar.style.width = "$percentage%"
            }
        }
        
        fun isCancelled(): Boolean = isCancelledFn()
        
        fun close() {
            document.body?.removeChild(overlay)
        }
    }
    
    /**
     * Show a dialog and wait for result
     * Core method that displays an HTML dialog via iframe
     */
    suspend fun showDialog(
        dialogPath: String,
        input: JsonElement? = null,
        width: Int? = null,
        height: Int? = null
    ): JsonElement? {
        val dialogId = "cspro-dialog-${++dialogCounter}"
        val url = "$dialogBasePath/$dialogPath"
        val deferred = CompletableDeferred<JsonElement?>()

        // Allow Android hardware back / browser back to close the dialog by pushing a synthetic history entry.
        // Also helps desktop browsers: Back closes the overlay instead of navigating away.
        val historyPushed = pushDialogHistory(dialogId)
        
        // Create overlay
        val overlay = (document.createElement("div") as HTMLDivElement).apply {
            id = "$dialogId-overlay"
            className = "cspro-dialog-overlay"
            style.apply {
                position = "fixed"
                top = "0"
                left = "0"
                this.width = "100vw"
                this.height = "100vh"
                backgroundColor = "rgba(0,0,0,0.5)"
                display = "flex"
                justifyContent = "center"
                alignItems = "center"
                zIndex = "10000"
            }
        }
        
        // Create dialog container
        val dialogWidth = width ?: (window.innerWidth * 0.9).toInt().coerceIn(320, 800)
        val dialogHeight = height ?: (window.innerHeight * 0.85).toInt().coerceIn(200, 600)
        
        val container = (document.createElement("div") as HTMLDivElement).apply {
            id = dialogId
            className = "cspro-dialog-container"
            style.apply {
                backgroundColor = "white"
                borderRadius = "8px"
                this.width = "${dialogWidth}px"
                this.height = "${dialogHeight}px"
                boxShadow = "0 4px 20px rgba(0,0,0,0.4)"
                setProperty("overflow", "hidden")
            }
        }
        
        // Create iframe
        val iframe = (document.createElement("iframe") as HTMLIFrameElement).apply {
            id = "$dialogId-iframe"
            src = url
            style.apply {
                this.width = "100%"
                this.height = "100%"
                border = "none"
            }
        }
        
        // Set up message listener for dialog communication
        val messageHandler = MessageHandler(dialogId, input, iframe, deferred, this::processActionRequest)
        val messageListener: (Event) -> Unit = messageHandler::handleMessage
        window.addEventListener("message", messageListener)

        // Close on Escape (web requirement)
        val keyListener: (Event) -> Unit = { event ->
            try {
                val keyEvent = event.unsafeCast<KeyboardEvent>()
                if (keyEvent.key == "Escape") {
                    closeDialog(dialogId)
                    if (!deferred.isCompleted) deferred.complete(null)
                }
            } catch (_: Exception) {
            }
        }
        window.addEventListener("keydown", keyListener)

        // Close on back button (Android hardware back triggers this in browsers)
        val popStateListener: (Event) -> Unit = {
            // If the dialog is still open and the user navigated back, dismiss it.
            if (activeDialogs.containsKey(dialogId)) {
                closeDialog(dialogId)
                if (!deferred.isCompleted) deferred.complete(null)
            }
        }
        window.addEventListener("popstate", popStateListener)
        
        // Store dialog instance
        activeDialogs[dialogId] = DialogInstance(
            id = dialogId,
            overlay = overlay,
            iframe = iframe,
            messageHandler = messageHandler::handleMessage,
            deferred = deferred
        )
        
        // Inject input data when iframe loads
        iframe.onload = {
            // Send input via postMessage when ready
            if (input != null) {
                try {
                    val contentWindow = iframe.contentWindow
                    if (contentWindow != null) {
                        val jsInput = jsonParse(input.toString())
                        val message = createInputMessage("cspro-dialog-input", jsInput)
                        postMessageToWindow(contentWindow.unsafeCast<JsAny>(), message, "*")
                    }
                } catch (e: Exception) {
                    println("[CSProDialogManager] Could not inject input: ${e.message}")
                }
            }
            Unit
        }
        
        container.appendChild(iframe)
        overlay.appendChild(container)
        document.body?.appendChild(overlay)
        
        // Wait for result
        val result = deferred.await()
        
        // Clean up
        window.removeEventListener("message", messageListener)
        window.removeEventListener("keydown", keyListener)
        window.removeEventListener("popstate", popStateListener)
        activeDialogs.remove(dialogId)

        // If the dialog was closed via ESC/X (not via back), remove the synthetic history entry.
        // If it was closed by back, the entry is already gone.
        if (historyPushed && isCurrentDialogHistory(dialogId)) {
            popDialogHistory()
        }
        
        return result
    }
    
    private fun closeDialog(dialogId: String) {
        activeDialogs[dialogId]?.let { instance ->
            try {
                document.body?.removeChild(instance.overlay)
            } catch (e: Exception) {
                // Already removed
            }
        }
    }
    
    private suspend fun processActionRequest(method: String, args: JsAny?, accessToken: String): JsAny? {
        // Use CSProEngineService to process action
        return try {
            val service = CSProEngineService.getInstance()
            val argsString = if (args != null) jsonStringify(args) else "{}"
            val message = "{\"action\":\"$method\",\"arguments\":$argsString}"
            // Use default web controller key 0 for dialog actions
            val result = service?.actionInvokerProcessMessage(
                webControllerKey = 0,
                listener = null,
                message = message,
                async = false,
                calledByOldCSProObject = false
            )
            if (result != null && result.isNotEmpty()) {
                jsonParse(result)
            } else {
                null
            }
        } catch (e: Exception) {
            println("[CSProDialogManager] Error processing action: ${e.message}")
            null
        }
    }
    
    /**
     * Message handler class for dialog communication
     */
    private class MessageHandler(
        private val dialogId: String,
        private val input: JsonElement?,
        private val iframe: HTMLIFrameElement,
        private val deferred: CompletableDeferred<JsonElement?>,
        private val actionProcessor: suspend (String, JsAny?, String) -> JsAny?
    ) {
        fun handleMessage(event: Event) {
            try {
                val messageEvent = event.unsafeCast<org.w3c.dom.MessageEvent>()
                val messageData = messageEvent.data ?: return
                val data = messageData.unsafeCast<JsAny>()
                
                val messageType = getJsStringProperty(data, "type")
                val messageDialogId = getJsStringProperty(data, "dialogId")
                
                when (messageType) {
                    "cspro-dialog-ready" -> {
                        // Dialog is ready, send input data
                        if (input != null) {
                            val contentWindow = iframe.contentWindow
                            if (contentWindow != null) {
                                val jsInput = jsonParse(input.toString())
                                val message = createInputMessage("cspro-dialog-input", jsInput)
                                postMessageToWindow(contentWindow.unsafeCast<JsAny>(), message, "*")
                            }
                        }
                    }
                    "cspro-dialog-result" -> {
                        // Dialog returned a result
                        val resultData = getJsProperty(data, "result")
                        val jsonResult = if (resultData != null) {
                            try {
                                val resultString = jsonStringify(resultData)
                                Json.parseToJsonElement(resultString)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                        
                        closeDialogById(dialogId)
                        deferred.complete(jsonResult)
                    }
                    "cspro-dialog-resize" -> {
                        // Dialog wants to resize based on content
                        val newWidth = getJsIntProperty(data, "width")
                        val newHeight = getJsIntProperty(data, "height")
                        
                        if (newWidth != null && newHeight != null) {
                            // Find the dialog container and resize it
                            val container = document.getElementById(dialogId) as? HTMLDivElement
                            if (container != null) {
                                // Cap the size to reasonable limits
                                val maxW = (window.innerWidth * 0.9).toInt()
                                val maxH = (window.innerHeight * 0.9).toInt()
                                val cappedWidth = newWidth.coerceIn(280, maxW)
                                val cappedHeight = newHeight.coerceIn(100, maxH)
                                
                                container.style.width = "${cappedWidth}px"
                                container.style.height = "${cappedHeight}px"
                            }
                        }
                    }
                    "cspro-dialog-close" -> {
                        // Dialog was closed/cancelled
                        closeDialogById(dialogId)
                        deferred.complete(null)
                    }
                    "cspro-action-request" -> {
                        // Dialog is requesting an action be processed by the WASM engine
                        val requestId = getJsIntProperty(data, "requestId")
                        val method = getJsStringProperty(data, "method")
                        val args = getJsProperty(data, "args")
                        val accessToken = getJsStringProperty(data, "accessToken") ?: ""
                        
                        if (requestId != null && method != null) {
                            // Process the action asynchronously and send response back
                            CoroutineScope(Dispatchers.Default).launch {
                                try {
                                    val result = actionProcessor(method, args, accessToken)
                                    // Send response back to iframe
                                    val contentWindow = iframe.contentWindow
                                    if (contentWindow != null) {
                                        val response = createResponseMessage("cspro-action-response", requestId, result)
                                        postMessageToWindow(contentWindow.unsafeCast<JsAny>(), response, "*")
                                    }
                                } catch (e: Exception) {
                                    println("[CSProDialogManager] Error processing action request: ${e.message}")
                                    val contentWindow = iframe.contentWindow
                                    if (contentWindow != null) {
                                        val response = createErrorMessage("cspro-action-response", requestId, e.message ?: "Unknown error")
                                        postMessageToWindow(contentWindow.unsafeCast<JsAny>(), response, "*")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore non-CSPro messages
            }
        }
        
        private fun closeDialogById(id: String) {
            CSProDialogManager.closeDialog(id)
        }
    }
    
    /**
     * Dialog instance tracking
     */
    private class DialogInstance(
        val id: String,
        val overlay: HTMLDivElement,
        val iframe: HTMLIFrameElement,
        val messageHandler: (Event) -> Unit,
        val deferred: CompletableDeferred<JsonElement?>
    )
}

/**
 * Choice dialog result
 */
data class ChoiceResult(
    val selectedIndex: Int,
    val directInput: String?
)

/**
 * Note item for review dialog
 */
data class NoteItem(
    val index: Int,
    val text: String,
    val label: String,
    val isFieldNote: Boolean
)

/**
 * Note review dialog result
 */
data class NoteReviewResult(
    val action: String,  // "select", "delete", "goTo", "cancel"
    val selectedNoteIndex: Int
)
