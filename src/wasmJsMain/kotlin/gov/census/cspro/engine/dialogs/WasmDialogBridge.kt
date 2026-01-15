package gov.census.cspro.engine.dialogs

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise

/**
 * Top-level JsFun declarations for registering dialog handlers
 * These are evaluated at module load time to set up the global handlers
 */

@JsFun("() => { window.CSProDialogHandler = window.CSProDialogHandler || {}; }")
private external fun initDialogHandler()

/**
 * Register all the async dialog functions that C++ expects
 * This sets up showDialogAsync, showHtmlDialogAsync, showModalDialogAsync, viewFileAsync
 * Using native HTML dialogs rendered via iframe overlay
 */
@JsFun("""
(showDialogFn, showHtmlDialogFn, showModalDialogFn) => {
    window.CSProDialogHandler = window.CSProDialogHandler || {};
    
    // Helper: Create and show an HTML dialog in an iframe overlay
    window.CSProDialogHandler._showHtmlDialogInIframe = function(dialogPath, inputData) {
        return new Promise((resolve, reject) => {
            console.log("[WasmDialogBridge JS] Opening HTML dialog:", dialogPath, "with input:", inputData);
            
            // Create overlay container
            const overlay = document.createElement('div');
            overlay.id = 'cspro-dialog-overlay';
            overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:10000;display:flex;align-items:center;justify-content:center;';
            
            // Create dialog container - set explicit initial height
            const dialogContainer = document.createElement('div');
            dialogContainer.id = 'cspro-dialog-container';
            dialogContainer.style.cssText = 'background:white;border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.3);max-width:90vw;max-height:90vh;overflow:hidden;min-width:300px;min-height:180px;width:500px;height:200px;';
            
            // Create iframe for dialog - explicit dimensions
            const iframe = document.createElement('iframe');
            iframe.id = 'cspro-dialog-iframe';
            iframe.style.cssText = 'width:100%;height:100%;border:none;';
            iframe.src = dialogPath;
            
            // Handle iframe load errors
            iframe.onerror = function(e) {
                console.error('[WasmDialogBridge JS] iframe load error:', e);
            };
            
            dialogContainer.appendChild(iframe);
            overlay.appendChild(dialogContainer);
            document.body.appendChild(overlay);
            
            // Store input data for the dialog to access
            window.CSProDialogHandler._currentDialogInput = inputData;
            window.CSProDialogHandler._currentDialogResolve = resolve;
            
            // Listen for dialog close messages
            const messageHandler = function(event) {
                if (event.data && event.data.type === 'cspro-dialog-close') {
                    console.log("[WasmDialogBridge JS] Dialog closed with result:", event.data.result);
                    window.removeEventListener('message', messageHandler);
                    document.body.removeChild(overlay);
                    window.CSProDialogHandler._currentDialogInput = null;
                    window.CSProDialogHandler._currentDialogResolve = null;
                    resolve(event.data.result);
                } else if (event.data && event.data.type === 'cspro-dialog-ready') {
                    // Dialog is ready, send input data
                    console.log("[WasmDialogBridge JS] Dialog ready, sending input data");
                    iframe.contentWindow.postMessage({
                        type: 'cspro-dialog-input',
                        input: inputData
                    }, '*');
                } else if (event.data && event.data.type === 'cspro-dialog-resize') {
                    // Dialog wants to resize - update iframe dimensions
                    console.log("[WasmDialogBridge JS] Dialog resize request:", event.data.width, "x", event.data.height);
                    iframe.style.width = event.data.width + 'px';
                    iframe.style.height = event.data.height + 'px';
                    iframe.style.minWidth = event.data.width + 'px';
                    iframe.style.minHeight = event.data.height + 'px';
                }
            };
            window.addEventListener('message', messageHandler);
            
            // Handle ESC key to close dialog
            const keyHandler = function(event) {
                if (event.key === 'Escape') {
                    window.removeEventListener('keydown', keyHandler);
                    window.removeEventListener('message', messageHandler);
                    document.body.removeChild(overlay);
                    window.CSProDialogHandler._currentDialogInput = null;
                    window.CSProDialogHandler._currentDialogResolve = null;
                    resolve(null);
                }
            };
            window.addEventListener('keydown', keyHandler);
        });
    };
    
    // viewFileAsync - view a file (HTML, text, etc.) in an overlay
    // contentType: 0 = Filename, 1 = HtmlUrl  
    // Returns true on success, false on failure
    window.CSProDialogHandler.viewFileAsync = async function(content, contentType, title, accessToken) {
        console.log("[WasmDialogBridge JS] viewFileAsync:", content, "type:", contentType, "title:", title);
        
        try {
            // Create overlay container
            const overlay = document.createElement('div');
            overlay.id = 'cspro-view-overlay';
            overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:10000;display:flex;align-items:center;justify-content:center;';
            
            // Create viewer container
            const viewContainer = document.createElement('div');
            viewContainer.id = 'cspro-view-container';
            viewContainer.style.cssText = 'background:white;border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.3);width:90vw;height:90vh;display:flex;flex-direction:column;overflow:hidden;';
            
            // Create header with title and close button
            const header = document.createElement('div');
            header.style.cssText = 'display:flex;justify-content:space-between;align-items:center;padding:12px 16px;background:#f5f5f5;border-bottom:1px solid #ddd;flex-shrink:0;';
            
            const titleEl = document.createElement('span');
            titleEl.style.cssText = 'font-weight:600;font-size:16px;color:#333;';
            titleEl.textContent = title || 'View';
            
            const closeBtn = document.createElement('button');
            closeBtn.style.cssText = 'background:none;border:none;font-size:24px;cursor:pointer;color:#666;padding:4px 8px;';
            closeBtn.innerHTML = '&times;';
            closeBtn.title = 'Close (ESC)';
            
            header.appendChild(titleEl);
            header.appendChild(closeBtn);
            viewContainer.appendChild(header);
            
            // Create content area (iframe for HTML, pre for text)
            let contentEl;
            let isHtml = contentType === 1 || content.toLowerCase().endsWith('.html') || content.toLowerCase().endsWith('.htm');
            
            if (isHtml) {
                // For HTML content, use iframe
                contentEl = document.createElement('iframe');
                contentEl.style.cssText = 'flex:1;width:100%;border:none;';
                // Note: Using allow-scripts without allow-same-origin for security
                // (prevents script from removing sandbox). allow-forms for form submission.
                contentEl.sandbox = 'allow-scripts allow-forms';
                
                // Determine the URL to load
                let url = content;
                if (contentType === 0) {
                    // It's a filename - convert to URL
                    // Files in WASM FS are at /opfs/ prefix
                    if (content.startsWith('/opfs/') || content.startsWith('/')) {
                        // Try to read the file content via the WASM module
                        try {
                            // Use window.CSProModule which is set by csentryKMP-loader.js
                            const wasmModule = window.CSProModule || window.csentryKMPModule?.module;
                            if (wasmModule && wasmModule.FS) {
                                console.log("[WasmDialogBridge JS] Reading HTML file from WASM FS:", content);
                                const fileContent = wasmModule.FS.readFile(content, { encoding: 'utf8' });
                                console.log("[WasmDialogBridge JS] File content length:", fileContent.length);
                                // Create a blob URL
                                const blob = new Blob([fileContent], { type: 'text/html' });
                                url = URL.createObjectURL(blob);
                            } else {
                                console.warn("[WasmDialogBridge JS] Cannot read WASM FS file - module not found:", content);
                                url = content;
                            }
                        } catch (e) {
                            console.error("[WasmDialogBridge JS] Error reading HTML file:", e);
                            url = content;
                        }
                    } else {
                        url = content;
                    }
                }
                
                contentEl.src = url;
            } else {
                // For text files, read content and display in pre
                const scrollContainer = document.createElement('div');
                scrollContainer.style.cssText = 'flex:1;overflow:auto;padding:16px;';
                
                contentEl = document.createElement('pre');
                contentEl.style.cssText = 'margin:0;white-space:pre-wrap;word-wrap:break-word;font-family:monospace;font-size:14px;';
                
                // Try to read the file
                try {
                    // Use window.CSProModule which is set by csentryKMP-loader.js
                    const wasmModule = window.CSProModule || window.csentryKMPModule?.module;
                    if (wasmModule && wasmModule.FS) {
                        console.log("[WasmDialogBridge JS] Reading text file from WASM FS:", content);
                        const fileContent = wasmModule.FS.readFile(content, { encoding: 'utf8' });
                        console.log("[WasmDialogBridge JS] File content length:", fileContent.length);
                        contentEl.textContent = fileContent;
                    } else {
                        console.error("[WasmDialogBridge JS] Cannot read WASM FS file - module not found");
                        contentEl.textContent = 'Unable to read file: ' + content + '\n\nWASM module not available.';
                    }
                } catch (e) {
                    console.error("[WasmDialogBridge JS] Error reading text file:", e);
                    contentEl.textContent = 'Error reading file: ' + e.message;
                }
                
                scrollContainer.appendChild(contentEl);
                contentEl = scrollContainer;
            }
            
            viewContainer.appendChild(contentEl);
            overlay.appendChild(viewContainer);
            document.body.appendChild(overlay);
            
            // Return a promise that resolves when closed
            return new Promise((resolve) => {
                const closeViewer = () => {
                    document.body.removeChild(overlay);
                    resolve(true);
                };
                
                closeBtn.onclick = closeViewer;
                
                // Also close on click outside
                overlay.onclick = (e) => {
                    if (e.target === overlay) closeViewer();
                };
                
                // Handle ESC key
                const keyHandler = (e) => {
                    if (e.key === 'Escape') {
                        window.removeEventListener('keydown', keyHandler);
                        closeViewer();
                    }
                };
                window.addEventListener('keydown', keyHandler);
            });
        } catch (e) {
            console.error("[WasmDialogBridge JS] viewFileAsync error:", e);
            return false;
        }
    };
    
    // showDialogAsync - called by jspi_showDialog in C++
    // C++ expects 1-based indices in the result!
    window.CSProDialogHandler.showDialogAsync = async function(dialogName, inputDataJson) {
        console.log("[WasmDialogBridge JS] showDialogAsync:", dialogName);
        try {
            const data = inputDataJson ? JSON.parse(inputDataJson) : {};
            
            if (dialogName === 'errmsg') {
                // Try to use native HTML dialog
                const dialogPath = '/dialogs/errmsg.html';
                try {
                    const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, data);
                    if (result && result.result && typeof result.result.index === 'number') {
                        console.log("[WasmDialogBridge JS] HTML dialog returned index:", result.result.index);
                        return JSON.stringify({ index: result.result.index });
                    }
                } catch (e) {
                    console.warn("[WasmDialogBridge JS] HTML dialog failed, using fallback:", e);
                }
                
                // Fallback to browser alert if HTML dialog fails
                const message = data.message || data.title || "Error";
                const title = data.title || "";
                window.alert(title ? (title + "\n\n" + message) : message);
                return JSON.stringify({ index: 1 }); // 1-based index!
            }
            
            if (dialogName === 'choice' || dialogName === 'select') {
                const dialogPath = '/dialogs/' + dialogName + '.html';
                try {
                    const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, data);
                    if (result && result.result) {
                        return JSON.stringify(result.result);
                    }
                } catch (e) {
                    console.warn("[WasmDialogBridge JS] HTML dialog failed:", e);
                }
                return JSON.stringify({ index: 1 });
            }
            
            // Default: try HTML dialog, fallback to alert
            const dialogPath = '/dialogs/' + dialogName + '.html';
            try {
                const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, data);
                if (result && result.result) {
                    return JSON.stringify(result.result);
                }
            } catch (e) {
                console.warn("[WasmDialogBridge JS] HTML dialog failed:", e);
            }
            
            const message = data.message || data.title || "Dialog: " + dialogName;
            window.alert(message);
            return JSON.stringify({ index: 1 }); // 1-based index!
        } catch (e) {
            console.error("[WasmDialogBridge JS] showDialogAsync error:", e);
            return JSON.stringify({ index: 1 }); // 1-based index!
        }
    };
    
    // showHtmlDialogAsync - for HTML-based dialogs
    window.CSProDialogHandler.showHtmlDialogAsync = async function(dialogPath, inputDataJson, optionsJson) {
        console.log("[WasmDialogBridge JS] showHtmlDialogAsync:", dialogPath);
        try {
            const inputData = inputDataJson ? JSON.parse(inputDataJson) : {};
            const options = optionsJson ? JSON.parse(optionsJson) : {};
            
            const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, inputData);
            if (result && result.result) {
                return JSON.stringify(result.result);
            }
            return JSON.stringify({});
        } catch (e) {
            console.error("[WasmDialogBridge JS] showHtmlDialogAsync error:", e);
            return null;
        }
    };
    
    // showModalDialogAsync - for modal message boxes
    window.CSProDialogHandler.showModalDialogAsync = async function(title, message, mbType) {
        console.log("[WasmDialogBridge JS] showModalDialogAsync:", title, mbType);
        try {
            // mbType determines button layout (OK, OK/Cancel, Yes/No, etc.)
            // Use HTML dialog for better UI
            let buttons = [{ caption: 'OK', index: 1 }];
            if (mbType === 4 || mbType === 36) { // MB_YESNO
                buttons = [{ caption: 'Yes', index: 1 }, { caption: 'No', index: 2 }];
            } else if (mbType === 1 || mbType === 33) { // MB_OKCANCEL
                buttons = [{ caption: 'OK', index: 1 }, { caption: 'Cancel', index: 2 }];
            } else if (mbType === 3 || mbType === 35) { // MB_YESNOCANCEL
                buttons = [{ caption: 'Yes', index: 1 }, { caption: 'No', index: 2 }, { caption: 'Cancel', index: 3 }];
            }
            
            const inputData = { title: title, message: message, buttons: buttons };
            
            try {
                const result = await window.CSProDialogHandler._showHtmlDialogInIframe('/dialogs/errmsg.html', inputData);
                if (result && result.result && typeof result.result.index === 'number') {
                    // Convert dialog index to Windows button ID
                    const idx = result.result.index;
                    if (mbType === 4 || mbType === 36) { // MB_YESNO
                        return idx === 1 ? 6 : 7; // 6=IDYES, 7=IDNO
                    } else if (mbType === 1 || mbType === 33) { // MB_OKCANCEL
                        return idx === 1 ? 1 : 2; // 1=IDOK, 2=IDCANCEL
                    } else if (mbType === 3 || mbType === 35) { // MB_YESNOCANCEL
                        if (idx === 1) return 6; // IDYES
                        if (idx === 2) return 7; // IDNO
                        return 2; // IDCANCEL
                    }
                    return 1; // IDOK
                }
            } catch (e) {
                console.warn("[WasmDialogBridge JS] HTML modal failed, using fallback:", e);
            }
            
            // Fallback to browser dialogs
            if (mbType === 4 || mbType === 36) { // MB_YESNO types
                const result = window.confirm(title + "\n\n" + message);
                return result ? 6 : 7; // 6=IDYES, 7=IDNO
            } else if (mbType === 1 || mbType === 33) { // MB_OKCANCEL types
                const result = window.confirm(title + "\n\n" + message);
                return result ? 1 : 2; // 1=IDOK, 2=IDCANCEL
            } else {
                window.alert(title + "\n\n" + message);
                return 1; // IDOK
            }
        } catch (e) {
            console.error("[WasmDialogBridge JS] showModalDialogAsync error:", e);
            return 1;
        }
    };
    
    console.log("[WasmDialogBridge JS] All dialog handlers registered with HTML dialog support");
}
""")
private external fun registerDialogFunctions(showDialogFn: JsAny?, showHtmlDialogFn: JsAny?, showModalDialogFn: JsAny?)

@JsFun("(handler) => { window.CSProDialogHandler.kotlinHandler = handler; }")
private external fun setKotlinHandler(handler: JsAny)

@JsFun("() => { return window.CSProDialogHandler.kotlinHandler; }")
private external fun getKotlinHandler(): JsAny?

@JsFun("(result) => { return JSON.stringify(result); }")
private external fun stringifyResult(result: JsAny): JsString

@JsFun("(json) => { return JSON.parse(json); }")
private external fun parseJson(json: String): JsAny

@JsFun("(obj, key) => { return obj[key]; }")
private external fun getProperty(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key) => { return typeof obj[key] === 'string' ? obj[key] : null; }")
private external fun getStringProperty(obj: JsAny, key: String): JsString?

@JsFun("(obj, key) => { return typeof obj[key] === 'number' ? obj[key] : null; }")
private external fun getNumberProperty(obj: JsAny, key: String): JsAny?

@JsFun("(type, data) => { return { type: type, data: data }; }")
private external fun createResultObject(type: String, data: JsAny?): JsAny

@JsFun("(index) => { return { index: index }; }")
private external fun createIndexResult(index: Int): JsAny

@JsFun("(note) => { return { note: note }; }")
private external fun createNoteResult(note: String?): JsAny

@JsFun("(text) => { return { textInput: text }; }")
private external fun createTextInputResult(text: String?): JsAny

@JsFun("(value) => { return { value: value }; }")
private external fun createValueResult(value: String?): JsAny

@JsFun("() => { return null; }")
private external fun jsNull(): JsAny?

/**
 * Bridge between C++ WASM Engine and Kotlin Dialog Manager
 * 
 * The C++ WASM engine (csentryKMP) calls JavaScript functions via:
 * - window.CSProDialogHandler.showDialogAsync(dialogName, inputDataJson)
 * - window.CSProDialogHandler.showHtmlDialogAsync(dialogPath, inputDataJson, optionsJson)
 * - window.CSProDialogHandler.showModalDialogAsync(title, message, mbType)
 * - window.CSProDialogHandler.getInputDataAsync(dialogId)
 * 
 * This bridge routes those calls to CSProDialogManager which renders
 * native CSPro HTML dialogs via iframes.
 */
object WasmDialogBridge {
    
    private val scope = MainScope()
    private var isRegistered = false
    
    // Pending dialog results for getInputDataAsync
    private val pendingDialogResults = mutableMapOf<String, String?>()
    
    /**
     * Register the dialog handler with the WASM engine
     * Call this during application initialization
     */
    fun register() {
        if (isRegistered) {
            println("[WasmDialogBridge] Already registered")
            return
        }
        
        println("[WasmDialogBridge] Registering window.CSProDialogHandler...")
        
        // Initialize the handler object and register all async functions
        initDialogHandler()
        registerDialogFunctions(null, null, null)
        
        isRegistered = true
        println("[WasmDialogBridge] Dialog handler registered successfully")
    }
    
    /**
     * Handle showDialog request from WASM engine
     * Routes to appropriate CSProDialogManager method based on dialog name
     */
    suspend fun handleShowDialog(dialogName: String, inputDataJson: String): String? {
        println("[WasmDialogBridge] handleShowDialog: $dialogName")
        
        return try {
            val input = if (inputDataJson.isNotEmpty()) {
                Json.parseToJsonElement(inputDataJson)
            } else {
                JsonObject(emptyMap())
            }
            
            when (dialogName.lowercase()) {
                "errmsg" -> {
                    // Error message dialog
                    val title = input.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Error"
                    val message = input.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: ""
                    val buttons = input.jsonObject["buttons"]?.jsonArray?.map { 
                        it.jsonObject["caption"]?.jsonPrimitive?.contentOrNull ?: "OK"
                    }
                    
                    val resultIndex = CSProDialogManager.showErrmsg(title, message, buttons)
                    buildJsonObject {
                        put("index", resultIndex)
                    }.toString()
                }
                
                "choice", "select" -> {
                    // Choice/select dialog
                    val title = input.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Select"
                    val choices = input.jsonObject["choices"]?.jsonArray?.map {
                        it.jsonObject["caption"]?.jsonPrimitive?.contentOrNull ?: ""
                    } ?: emptyList()
                    
                    val result = CSProDialogManager.showChoice(title, choices)
                    if (result.selectedIndex < 0) {
                        null
                    } else {
                        buildJsonObject {
                            put("index", result.selectedIndex)
                            put("value", result.directInput ?: choices.getOrNull(result.selectedIndex))
                        }.toString()
                    }
                }
                
                "textinput", "text-input" -> {
                    // Text input dialog
                    val title = input.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Enter Value"
                    val initialValue = input.jsonObject["initialValue"]?.jsonPrimitive?.contentOrNull ?: ""
                    val multiline = input.jsonObject["multiline"]?.jsonPrimitive?.booleanOrNull ?: false
                    
                    val result = CSProDialogManager.showTextInput(title, initialValue, multiline)
                    if (result == null) {
                        null
                    } else {
                        buildJsonObject {
                            put("textInput", result)
                        }.toString()
                    }
                }
                
                "note", "note-edit" -> {
                    // Note edit dialog
                    val title = input.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Edit Note"
                    val note = input.jsonObject["note"]?.jsonPrimitive?.contentOrNull ?: ""
                    val operatorId = input.jsonObject["operatorId"]?.jsonPrimitive?.contentOrNull ?: ""
                    
                    val result = CSProDialogManager.showNoteEdit(title, note, operatorId)
                    if (result == null) {
                        null
                    } else {
                        buildJsonObject {
                            put("note", result)
                        }.toString()
                    }
                }
                
                "show" -> {
                    // Show dialog - display data in a table
                    val title = input.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Data"
                    // For show dialog, just display it and return 0
                    CSProDialogManager.showErrmsg(title, "Data display", listOf("OK"))
                    buildJsonObject {
                        put("index", 0)
                    }.toString()
                }
                
                "selcase" -> {
                    // Selcase dialog - select a case from a list
                    val title = input.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Select Case"
                    val choices = input.jsonObject["choices"]?.jsonArray?.map {
                        it.jsonPrimitive.contentOrNull ?: ""
                    } ?: emptyList()
                    
                    val result = CSProDialogManager.showChoice(title, choices)
                    buildJsonObject {
                        put("index", if (result.selectedIndex < 0) -1 else result.selectedIndex)
                    }.toString()
                }
                
                else -> {
                    // Unknown dialog - try to show generic HTML dialog
                    println("[WasmDialogBridge] Unknown dialog type: $dialogName, using generic handler")
                    handleShowHtmlDialog("/dialogs/$dialogName.html", inputDataJson, "{}")
                }
            }
        } catch (e: Exception) {
            println("[WasmDialogBridge] handleShowDialog error: ${e.message}")
            null
        }
    }
    
    /**
     * Handle showHtmlDialog request from WASM engine
     * Shows the dialog via CSProDialogManager.showDialog
     */
    suspend fun handleShowHtmlDialog(
        dialogPath: String,
        inputDataJson: String,
        optionsJson: String
    ): String? {
        println("[WasmDialogBridge] handleShowHtmlDialog: $dialogPath")
        
        return try {
            val input = if (inputDataJson.isNotEmpty()) {
                Json.parseToJsonElement(inputDataJson)
            } else {
                null
            }
            
            val options = if (optionsJson.isNotEmpty()) {
                Json.parseToJsonElement(optionsJson).jsonObject
            } else {
                null
            }
            
            val width = options?.get("width")?.jsonPrimitive?.intOrNull
            val height = options?.get("height")?.jsonPrimitive?.intOrNull
            
            // Use the dialog method
            val result = CSProDialogManager.showDialog(dialogPath, input, width, height)
            result?.toString()
        } catch (e: Exception) {
            println("[WasmDialogBridge] handleShowHtmlDialog error: ${e.message}")
            null
        }
    }
    
    /**
     * Handle showModalDialog request from WASM engine
     * Shows a modal message box with buttons
     * 
     * @param mbType Windows message box types:
     *   MB_OK = 0
     *   MB_OKCANCEL = 1
     *   MB_YESNO = 4
     *   MB_YESNOCANCEL = 3
     *   
     * @return Button ID:
     *   IDOK = 1
     *   IDCANCEL = 2
     *   IDYES = 6
     *   IDNO = 7
     */
    suspend fun handleShowModalDialog(title: String, message: String, mbType: Int): Int {
        println("[WasmDialogBridge] handleShowModalDialog: $title (type=$mbType)")
        
        return try {
            val buttons = when (mbType) {
                0 -> listOf("OK")                           // MB_OK
                1 -> listOf("OK", "Cancel")                 // MB_OKCANCEL
                4 -> listOf("Yes", "No")                    // MB_YESNO
                3 -> listOf("Yes", "No", "Cancel")          // MB_YESNOCANCEL
                else -> listOf("OK")
            }
            
            val resultIndex = CSProDialogManager.showErrmsg(title, message, buttons)
            
            // Convert index to Windows button ID
            when (mbType) {
                0 -> 1  // IDOK
                1 -> if (resultIndex == 0) 1 else 2  // IDOK or IDCANCEL
                4 -> if (resultIndex == 0) 6 else 7  // IDYES or IDNO
                3 -> when (resultIndex) {
                    0 -> 6   // IDYES
                    1 -> 7   // IDNO
                    else -> 2 // IDCANCEL
                }
                else -> 1  // IDOK
            }
        } catch (e: Exception) {
            println("[WasmDialogBridge] handleShowModalDialog error: ${e.message}")
            1 // Return IDOK on error
        }
    }
    
    /**
     * Handle getInputData request from WASM engine
     * Returns stored input data for a dialog
     */
    fun handleGetInputData(dialogId: String): String? {
        println("[WasmDialogBridge] handleGetInputData: $dialogId")
        
        // Check if we have stored data for this dialog
        return pendingDialogResults.remove(dialogId)
    }
    
    /**
     * Store dialog result for later retrieval via getInputData
     */
    fun storeDialogResult(dialogId: String, result: String?) {
        if (result != null) {
            pendingDialogResults[dialogId] = result
        }
    }
    
    /**
     * Unregister the dialog handler
     */
    fun unregister() {
        if (!isRegistered) return
        isRegistered = false
        println("[WasmDialogBridge] Dialog handler unregistered")
    }
}
