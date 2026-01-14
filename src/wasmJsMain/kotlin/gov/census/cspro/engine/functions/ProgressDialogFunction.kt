package gov.census.cspro.engine.functions

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

/**
 * Progress Dialog Function - displays a progress indicator
 * Ported from Android ProgressDialogFunction.java
 */
class ProgressDialogFunction(
    private val config: ProgressDialogConfig
) {
    private var overlay: HTMLDivElement? = null
    private var progressBar: HTMLDivElement? = null
    private var progressText: HTMLElement? = null
    private var messageText: HTMLElement? = null
    private var onCancel: (() -> Unit)? = null
    private var currentProgress = 0
    
    /**
     * Show the progress dialog
     */
    fun show(cancelCallback: (() -> Unit)? = null) {
        onCancel = cancelCallback
        
        // Create overlay
        overlay = (document.createElement("div") as HTMLDivElement).apply {
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
        
        // Create dialog
        val dialog = (document.createElement("div") as HTMLDivElement).apply {
            style.apply {
                backgroundColor = "white"
                borderRadius = "8px"
                padding = "25px"
                minWidth = "350px"
                maxWidth = "450px"
                boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
                textAlign = "center"
            }
        }
        
        // Title
        val titleEl = (document.createElement("h3") as HTMLElement).apply {
            textContent = config.title
            style.apply {
                margin = "0 0 15px 0"
                color = "#333"
            }
        }
        dialog.appendChild(titleEl)
        
        // Message
        messageText = (document.createElement("p") as HTMLElement).apply {
            textContent = config.message
            style.apply {
                margin = "0 0 20px 0"
                color = "#666"
            }
        }
        dialog.appendChild(messageText!!)
        
        if (config.isIndeterminate) {
            // Indeterminate spinner
            val spinner = (document.createElement("div") as HTMLDivElement).apply {
                style.apply {
                    width = "40px"
                    height = "40px"
                    margin = "20px auto"
                    border = "4px solid #f3f3f3"
                    borderTop = "4px solid #007bff"
                    borderRadius = "50%"
                    animation = "cspro-spin 1s linear infinite"
                }
            }
            
            // Add keyframe animation
            val style = document.createElement("style") as HTMLElement
            style.textContent = """
                @keyframes cspro-spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            """.trimIndent()
            document.head?.appendChild(style)
            
            dialog.appendChild(spinner)
        } else {
            // Progress bar container
            val progressContainer = (document.createElement("div") as HTMLDivElement).apply {
                style.apply {
                    width = "100%"
                    height = "20px"
                    backgroundColor = "#e9ecef"
                    borderRadius = "10px"
                    setProperty("overflow", "hidden")
                    marginBottom = "10px"
                }
            }
            
            // Progress bar fill
            progressBar = (document.createElement("div") as HTMLDivElement).apply {
                style.apply {
                    width = "0%"
                    height = "100%"
                    backgroundColor = "#007bff"
                    transition = "width 0.3s ease"
                }
            }
            progressContainer.appendChild(progressBar!!)
            dialog.appendChild(progressContainer)
            
            // Progress text
            progressText = (document.createElement("p") as HTMLElement).apply {
                textContent = "0%"
                style.apply {
                    margin = "5px 0 15px 0"
                    fontSize = "14px"
                    color = "#666"
                }
            }
            dialog.appendChild(progressText!!)
        }
        
        // Cancel button (if cancellable)
        if (config.cancellable) {
            val cancelBtn = (document.createElement("button") as HTMLButtonElement).apply {
                textContent = "Cancel"
                style.apply {
                    padding = "8px 25px"
                    border = "1px solid #ccc"
                    borderRadius = "4px"
                    cursor = "pointer"
                    backgroundColor = "#fff"
                    marginTop = "10px"
                }
                onclick = {
                    onCancel?.invoke()
                    dismiss()
                    Unit
                }
            }
            dialog.appendChild(cancelBtn)
        }
        
        overlay?.appendChild(dialog)
        document.body?.appendChild(overlay!!)
    }
    
    /**
     * Update progress (0-100)
     */
    fun updateProgress(progress: Int, message: String? = null) {
        currentProgress = progress.coerceIn(0, config.maxProgress)
        val percentage = (currentProgress * 100) / config.maxProgress
        
        progressBar?.style?.width = "$percentage%"
        progressText?.textContent = "$percentage%"
        
        if (message != null) {
            messageText?.textContent = message
        }
    }
    
    /**
     * Update message only
     */
    fun updateMessage(message: String) {
        messageText?.textContent = message
    }
    
    /**
     * Dismiss the dialog
     */
    fun dismiss() {
        overlay?.let { 
            document.body?.removeChild(it)
        }
        overlay = null
        progressBar = null
        progressText = null
        messageText = null
    }
    
    /**
     * Check if dialog is showing
     */
    fun isShowing(): Boolean = overlay != null
}

/**
 * Singleton progress dialog manager
 */
object ProgressDialogManager {
    private var currentDialog: ProgressDialogFunction? = null
    
    fun show(title: String, message: String, indeterminate: Boolean = true, cancellable: Boolean = true, onCancel: (() -> Unit)? = null) {
        dismiss() // Dismiss any existing dialog
        
        currentDialog = ProgressDialogFunction(
            ProgressDialogConfig(
                title = title,
                message = message,
                isIndeterminate = indeterminate,
                cancellable = cancellable
            )
        )
        currentDialog?.show(onCancel)
    }
    
    fun updateProgress(progress: Int, message: String? = null) {
        currentDialog?.updateProgress(progress, message)
    }
    
    fun updateMessage(message: String) {
        currentDialog?.updateMessage(message)
    }
    
    fun dismiss() {
        currentDialog?.dismiss()
        currentDialog = null
    }
    
    fun isShowing(): Boolean = currentDialog?.isShowing() == true
}
