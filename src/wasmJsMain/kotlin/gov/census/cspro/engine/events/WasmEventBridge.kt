package gov.census.cspro.engine.events

import gov.census.cspro.ui.ActivityRouter
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLButtonElement
import kotlin.js.JsAny

/**
 * Top-level JsFun declarations for registering event handlers
 */

@JsFun("() => { window.CSProEventHandler = window.CSProEventHandler || {}; }")
private external fun initEventHandler()

@JsFun("(style, value) => { style.setProperty('overflow', value); }")
private external fun setOverflow(style: JsAny, value: String)

/**
 * Register the execPffAsync handler that stores the pending PFF and signals success
 */
@JsFun("""
() => {
    window.CSProEventHandler = window.CSProEventHandler || {};
    window.CSProEventHandler._pendingPffPath = null;
    
    // execPffAsync - called by jspi_execPff in C++ 
    // Stores the PFF path for the Kotlin layer to pick up after the current app stops
    window.CSProEventHandler.execPffAsync = async function(pffPath) {
        console.log("[WasmEventBridge JS] execPffAsync called:", pffPath);
        
        // Store the pending PFF path for the activity to pick up when it finishes
        window.CSProEventHandler._pendingPffPath = pffPath;
        
        // Return true to indicate we've accepted the execPff request
        // The actual navigation happens after the current activity finishes
        return true;
    };
    
    // Helper to get and clear the pending PFF path
    window.CSProEventHandler.getPendingPff = function() {
        const path = window.CSProEventHandler._pendingPffPath;
        window.CSProEventHandler._pendingPffPath = null;
        return path;
    };
    
    // Check if there's a pending PFF
    window.CSProEventHandler.hasPendingPff = function() {
        return window.CSProEventHandler._pendingPffPath !== null;
    };
    
    console.log("[WasmEventBridge JS] execPffAsync handler registered");
}
""")
private external fun registerExecPffHandler()

/**
 * Get the pending PFF path from JavaScript (returns null if none)
 */
@JsFun("""
() => {
    if (typeof window.CSProEventHandler !== 'undefined' && 
        typeof window.CSProEventHandler.getPendingPff === 'function') {
        return window.CSProEventHandler.getPendingPff();
    }
    return null;
}
""")
private external fun jsGetPendingPff(): String?

/**
 * Check if there's a pending PFF to launch
 */
@JsFun("""
() => {
    if (typeof window.CSProEventHandler !== 'undefined' && 
        typeof window.CSProEventHandler.hasPendingPff === 'function') {
        return window.CSProEventHandler.hasPendingPff();
    }
    return false;
}
""")
private external fun jsHasPendingPff(): Boolean

/**
 * Bridge between C++ WASM Engine and Kotlin for non-dialog events
 * 
 * The C++ WASM engine (csentryKMP) calls JavaScript functions via:
 * - window.CSProEventHandler.showProgress(message)
 * - window.CSProEventHandler.hideProgress()
 * - window.CSProEventHandler.updateProgress(percent, message) -> boolean
 * - window.CSProEventHandler.refreshPage(contents)
 * - window.CSProEventHandler.execPffAsync(pffPath) -> boolean (async)
 * 
 * This bridge routes those calls to the Kotlin UI system.
 */
object WasmEventBridge {
    
    private val scope = MainScope()
    private var isRegistered = false
    
    // Progress dialog state
    private var progressOverlay: HTMLDivElement? = null
    private var progressMessage: HTMLDivElement? = null
    private var progressBar: HTMLDivElement? = null
    private var progressCancelled = false
    
    // Event listeners
    private val refreshListeners = mutableListOf<(Int) -> Unit>()
    private val progressListeners = mutableListOf<ProgressListener>()
    
    interface ProgressListener {
        fun onShowProgress(message: String)
        fun onHideProgress()
        fun onUpdateProgress(percent: Int, message: String?): Boolean // Return true if cancelled
    }
    
    /**
     * Register the event handler with the WASM engine
     * Call this during application initialization
     */
    fun register() {
        if (isRegistered) {
            println("[WasmEventBridge] Already registered")
            return
        }
        
        println("[WasmEventBridge] Registering window.CSProEventHandler...")
        
        // Initialize the handler object
        initEventHandler()
        
        // Register the execPff handler
        registerExecPffHandler()
        
        isRegistered = true
        println("[WasmEventBridge] Event handler registered successfully")
    }
    
    /**
     * Check if there's a pending PFF to launch
     */
    fun hasPendingPff(): Boolean {
        return jsHasPendingPff()
    }
    
    /**
     * Get and clear the pending PFF path
     * Returns null if no pending PFF
     */
    fun getPendingPff(): String? {
        val path = jsGetPendingPff()
        if (path != null) {
            println("[WasmEventBridge] Got pending PFF: $path")
        }
        return path
    }
    
    /**
     * Handle showProgress call from WASM engine
     */
    fun handleShowProgress(message: String) {
        println("[WasmEventBridge] handleShowProgress: $message")
        progressCancelled = false
        
        // Notify listeners first
        progressListeners.forEach { it.onShowProgress(message) }
        
        // Create/show progress overlay if no listeners handled it
        if (progressListeners.isEmpty()) {
            showDefaultProgressDialog(message)
        }
    }
    
    /**
     * Handle hideProgress call from WASM engine
     */
    fun handleHideProgress() {
        println("[WasmEventBridge] handleHideProgress")
        
        // Notify listeners first
        progressListeners.forEach { it.onHideProgress() }
        
        // Hide default progress dialog
        hideDefaultProgressDialog()
    }
    
    /**
     * Handle updateProgress call from WASM engine
     */
    fun handleUpdateProgress(percent: Int, message: String): Boolean {
        println("[WasmEventBridge] handleUpdateProgress: $percent% - $message")
        
        // Notify listeners and check if any report cancellation
        var cancelled = progressCancelled
        progressListeners.forEach { 
            if (it.onUpdateProgress(percent, message.ifEmpty { null })) {
                cancelled = true
            }
        }
        
        // Update default progress dialog
        updateDefaultProgressDialog(percent, message.ifEmpty { null })
        
        return cancelled
    }
    
    /**
     * Handle refreshPage call from WASM engine
     */
    fun handleRefreshPage(contents: Int) {
        println("[WasmEventBridge] handleRefreshPage: contents=$contents")
        
        // Notify all refresh listeners
        refreshListeners.forEach { it(contents) }
    }
    
    // ========================================================================
    // Default Progress Dialog (used when no custom listener is registered)
    // ========================================================================
    
    private fun showDefaultProgressDialog(message: String) {
        // Create overlay if doesn't exist
        if (progressOverlay == null) {
            progressOverlay = (document.createElement("div") as HTMLDivElement).apply {
                id = "cspro-progress-overlay"
                style.apply {
                    position = "fixed"
                    top = "0"
                    left = "0"
                    width = "100%"
                    height = "100%"
                    backgroundColor = "rgba(0, 0, 0, 0.5)"
                    display = "flex"
                    justifyContent = "center"
                    alignItems = "center"
                    zIndex = "10000"
                }
            }
            
            val dialog = (document.createElement("div") as HTMLDivElement).apply {
                style.apply {
                    backgroundColor = "white"
                    padding = "24px"
                    borderRadius = "8px"
                    minWidth = "300px"
                    maxWidth = "400px"
                    boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
                }
            }
            
            progressMessage = (document.createElement("div") as HTMLDivElement).apply {
                textContent = message
                style.apply {
                    marginBottom = "16px"
                    fontSize = "14px"
                    color = "#333"
                }
            }
            
            val progressContainer = (document.createElement("div") as HTMLDivElement).apply {
                style.apply {
                    width = "100%"
                    height = "8px"
                    backgroundColor = "#e0e0e0"
                    borderRadius = "4px"
                    setProperty("overflow", "hidden")
                }
            }
            
            progressBar = (document.createElement("div") as HTMLDivElement).apply {
                style.apply {
                    width = "0%"
                    height = "100%"
                    backgroundColor = "#007bff"
                    transition = "width 0.3s ease"
                }
            }
            
            val cancelButton = (document.createElement("button") as HTMLButtonElement).apply {
                textContent = "Cancel"
                style.apply {
                    marginTop = "16px"
                    padding = "8px 16px"
                    cursor = "pointer"
                }
                onclick = { 
                    progressCancelled = true
                    null
                }
            }
            
            progressContainer.appendChild(progressBar!!)
            dialog.appendChild(progressMessage!!)
            dialog.appendChild(progressContainer)
            dialog.appendChild(cancelButton)
            progressOverlay!!.appendChild(dialog)
        }
        
        progressMessage?.textContent = message
        progressBar?.style?.width = "0%"
        
        document.body?.appendChild(progressOverlay!!)
    }
    
    private fun hideDefaultProgressDialog() {
        progressOverlay?.let { overlay ->
            overlay.parentElement?.removeChild(overlay)
        }
    }
    
    private fun updateDefaultProgressDialog(percent: Int, message: String?) {
        progressBar?.style?.width = "${percent.coerceIn(0, 100)}%"
        if (message != null) {
            progressMessage?.textContent = message
        }
    }
    
    // ========================================================================
    // Public API for registering listeners
    // ========================================================================
    
    fun addRefreshListener(listener: (Int) -> Unit) {
        refreshListeners.add(listener)
    }
    
    fun removeRefreshListener(listener: (Int) -> Unit) {
        refreshListeners.remove(listener)
    }
    
    fun addProgressListener(listener: ProgressListener) {
        progressListeners.add(listener)
    }
    
    fun removeProgressListener(listener: ProgressListener) {
        progressListeners.remove(listener)
    }
    
    /**
     * Cancel the current progress operation
     */
    fun cancelProgress() {
        progressCancelled = true
    }
    
    /**
     * Check if progress was cancelled
     */
    fun isProgressCancelled(): Boolean = progressCancelled
    
    /**
     * Unregister the event handler
     */
    fun unregister() {
        if (!isRegistered) return
        
        hideDefaultProgressDialog()
        progressOverlay = null
        progressMessage = null
        progressBar = null
        
        isRegistered = false
        println("[WasmEventBridge] Event handler unregistered")
    }
}
