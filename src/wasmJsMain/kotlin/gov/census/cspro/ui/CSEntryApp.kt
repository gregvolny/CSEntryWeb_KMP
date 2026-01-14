package gov.census.cspro.ui

import gov.census.cspro.engine.IEngineInterface
import gov.census.cspro.platform.IPlatformServices
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

// Top-level @JsFun helper for timestamp (Kotlin/Wasm requires @JsFun at top-level)
@JsFun("() => new Date().toLocaleTimeString()")
private external fun jsGetCurrentTime(): String

/**
 * Main UI Application using HTML/DOM manipulation
 * This is a lightweight alternative to Compose Multiplatform
 */
class CSEntryApp(
    private val engineInterface: IEngineInterface,
    private val platformServices: IPlatformServices
) {
    private val scope = MainScope()
    private var appContainer: HTMLElement? = null
    
    fun mount() {
        appContainer = document.getElementById("app") as? HTMLElement
        
        if (appContainer == null) {
            println("[CSEntryApp] App container not found")
            return
        }
        
        render()
    }
    
    private fun render() {
        val container = appContainer ?: return
        
        container.innerHTML = """
            <div class="csentry-container">
                <header class="csentry-header">
                    <h1>CSEntry Web</h1>
                    <p class="subtitle">Kotlin/Wasm + Emscripten Hybrid Application</p>
                </header>
                
                <div class="csentry-main">
                    <section class="status-section">
                        <h2>Engine Status</h2>
                        <div id="engine-status" class="status-card">
                            <div class="status-item">
                                <span class="status-label">Engine:</span>
                                <span class="status-value status-ready">Ready</span>
                            </div>
                            <div class="status-item">
                                <span class="status-label">Application:</span>
                                <span id="app-status" class="status-value">Not loaded</span>
                            </div>
                        </div>
                    </section>
                    
                    <section class="control-section">
                        <h2>Controls</h2>
                        <div class="button-group">
                            <button id="btn-load-app" class="btn btn-primary">Load Application</button>
                            <button id="btn-start-entry" class="btn btn-success" disabled>Start Data Entry</button>
                            <button id="btn-new-case" class="btn btn-secondary" disabled>New Case</button>
                            <button id="btn-save-case" class="btn btn-secondary" disabled>Save Case</button>
                        </div>
                    </section>
                    
                    <section class="form-section" id="form-section" style="display: none;">
                        <h2>Data Entry Form</h2>
                        <div id="entry-form" class="entry-form"></div>
                        <div class="navigation-buttons">
                            <button id="btn-prev-field" class="btn btn-secondary">← Previous</button>
                            <button id="btn-next-field" class="btn btn-primary">Next →</button>
                        </div>
                    </section>
                    
                    <section class="console-section">
                        <h2>Console Log</h2>
                        <div id="console-log" class="console"></div>
                    </section>
                </div>
            </div>
        """.trimIndent()
        
        // Attach event listeners
        attachEventListeners()
    }
    
    private fun attachEventListeners() {
        val btnLoadApp = document.getElementById("btn-load-app") as? HTMLButtonElement
        val btnStartEntry = document.getElementById("btn-start-entry") as? HTMLButtonElement
        val btnNewCase = document.getElementById("btn-new-case") as? HTMLButtonElement
        val btnSaveCase = document.getElementById("btn-save-case") as? HTMLButtonElement
        val btnPrevField = document.getElementById("btn-prev-field") as? HTMLButtonElement
        val btnNextField = document.getElementById("btn-next-field") as? HTMLButtonElement
        
        btnLoadApp?.addEventListener("click", {
            scope.launch { loadApplication() }
        })
        
        btnStartEntry?.addEventListener("click", {
            scope.launch { startEntry() }
        })
        
        btnNewCase?.addEventListener("click", {
            scope.launch { createNewCase() }
        })
        
        btnSaveCase?.addEventListener("click", {
            scope.launch { saveCurrentCase() }
        })
        
        btnPrevField?.addEventListener("click", {
            scope.launch { goToPreviousField() }
        })
        
        btnNextField?.addEventListener("click", {
            scope.launch { goToNextField() }
        })
    }
    
    private suspend fun loadApplication() {
        logToConsole("Loading application...")
        
        try {
            // For demo, use the example PFF from embedded assets
            val pffPath = "/Assets/examples/Simple CAPI.pff"
            
            val success = engineInterface.openApplication(pffPath)
            
            if (success) {
                logToConsole("Application loaded successfully", "success")
                updateAppStatus("Loaded: ${engineInterface.getWindowTitle() ?: "Unknown"}")
                
                // Enable start button
                val btnStartEntry = document.getElementById("btn-start-entry") as? HTMLButtonElement
                btnStartEntry?.disabled = false
            } else {
                logToConsole("Failed to load application", "error")
            }
        } catch (e: Exception) {
            logToConsole("Error loading application: ${e.message}", "error")
        }
    }
    
    private suspend fun startEntry() {
        logToConsole("Starting data entry...")
        
        try {
            val success = engineInterface.start()
            
            if (success) {
                logToConsole("Data entry started", "success")
                
                // Show form section
                val formSection = document.getElementById("form-section") as? HTMLDivElement
                formSection?.style?.display = "block"
                
                // Enable controls
                val btnNewCase = document.getElementById("btn-new-case") as? HTMLButtonElement
                val btnSaveCase = document.getElementById("btn-save-case") as? HTMLButtonElement
                btnNewCase?.disabled = false
                btnSaveCase?.disabled = false
                
                // Render current form
                renderForm()
            } else {
                logToConsole("Failed to start data entry", "error")
            }
        } catch (e: Exception) {
            logToConsole("Error starting data entry: ${e.message}", "error")
        }
    }
    
    private suspend fun createNewCase() {
        logToConsole("Creating new case...")
        // Insert case at position 0 (beginning)
        val success = engineInterface.insertCase(0.0)
        if (success) {
            logToConsole("New case created", "success")
            renderForm()
        }
    }
    
    private suspend fun saveCurrentCase() {
        logToConsole("Saving case...")
        try {
            engineInterface.savePartial()
            logToConsole("Case saved successfully", "success")
        } catch (e: Exception) {
            logToConsole("Failed to save case: ${e.message}", "error")
        }
    }
    
    private suspend fun goToPreviousField() {
        engineInterface.previousField()
        renderForm()
    }
    
    private suspend fun goToNextField() {
        engineInterface.nextField()
        renderForm()
    }
    
    private fun renderForm() {
        val formDiv = document.getElementById("entry-form") as? HTMLDivElement ?: return
        
        formDiv.innerHTML = """
            <div class="form-placeholder">
                <p>Form rendering in progress...</p>
                <p class="info-text">
                    The C++ engine is ready. Form fields will be rendered here by 
                    querying the engine's CoreEntryPageInterface.
                </p>
            </div>
        """.trimIndent()
    }
    
    private fun updateAppStatus(status: String) {
        val statusSpan = document.getElementById("app-status")
        if (statusSpan != null) {
            statusSpan.textContent = status
        }
    }
    
    private fun logToConsole(message: String, type: String = "info") {
        val consoleDiv = document.getElementById("console-log") as? HTMLDivElement ?: return
        
        // Use top-level @JsFun helper for timestamp
        val timestamp: String = jsGetCurrentTime()
        val logEntry = document.createElement("div") as HTMLDivElement
        logEntry.className = "console-entry console-$type"
        logEntry.innerHTML = """
            <span class="console-timestamp">[$timestamp]</span>
            <span class="console-message">$message</span>
        """.trimIndent()
        
        consoleDiv.appendChild(logEntry)
        consoleDiv.scrollTop = consoleDiv.scrollHeight.toDouble()
        
        // Also log to browser console
        when (type) {
            "error" -> println("[CSEntry] ERROR: $message")
            "warn" -> println("[CSEntry] WARN: $message")
            "success" -> println("[CSEntry] ✓ $message")
            else -> println("[CSEntry] $message")
        }
    }
}
