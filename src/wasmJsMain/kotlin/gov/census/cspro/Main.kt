package gov.census.cspro

import gov.census.cspro.ui.ActivityRouter
import gov.census.cspro.storage.OpfsService
import gov.census.cspro.engine.dialogs.WasmDialogBridge
import gov.census.cspro.engine.events.WasmEventBridge
import gov.census.cspro.engine.QuestionTextJsBridge
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Main entry point for CSEntry Web Application (Kotlin/Wasm)
 * Initializes OPFS and launches the ActivityRouter to handle navigation
 */

private val mainScope = MainScope()

fun main() {
    println("=".repeat(60))
    println("CSEntry Web - Kotlin/Wasm Application")
    println("=".repeat(60))
    
    // Start initialization immediately - don't wait for window.load
    // since the module is loaded dynamically after page is ready
    println("[CSEntry] Starting initialization...")
    initializeApplication()
}

private fun initializeApplication() {
    println("[CSEntry] Launching coroutine for async initialization...")
    
    mainScope.launch {
        try {
            println("[CSEntry] Coroutine started...")
            
            // Show loading indicator in app container
            val appContainer = document.getElementById("app")
            println("[CSEntry] App container found: ${appContainer != null}")
            
            if (appContainer != null) {
                appContainer.innerHTML = "<div class='loading-state'><p>Initializing CSEntry...</p></div>"
            }
            
            // Register WASM bridges EARLY - before any engine interaction
            // This connects C++ WASM engine callbacks to Kotlin UI
            println("[CSEntry] Registering WASM Dialog Bridge...")
            WasmDialogBridge.register()
            
            println("[CSEntry] Registering WASM Event Bridge...")
            WasmEventBridge.register()
            
            // Register Question Text JavaScript Bridge for QSF function calls
            println("[CSEntry] Registering Question Text JS Bridge...")
            QuestionTextJsBridge.register()
            
            // Initialize OPFS storage
            println("[CSEntry] Initializing OPFS storage...")
            val opfsAvailable = OpfsService.isAvailable()
            println("[CSEntry] OPFS available: $opfsAvailable")
            
            if (opfsAvailable) {
                val initialized = OpfsService.initialize()
                println("[CSEntry] OPFS initialized: $initialized")
            }
            
            // Initialize the Activity Router for navigation
            println("[CSEntry] Initializing ActivityRouter...")
            ActivityRouter.initialize()
            
            println("[CSEntry] Application initialized successfully!")
            
        } catch (e: Throwable) {
            println("[CSEntry] EXCEPTION during initialization: $e")
            println("[CSEntry] Exception message: ${e.message}")
            
            // Try to show error in UI
            try {
                document.getElementById("app")?.innerHTML = """
                    <div class='error-state'>
                        <h2>Initialization Error</h2>
                        <p>${e.message ?: e.toString()}</p>
                    </div>
                """.trimIndent()
            } catch (e2: Throwable) {
                println("[CSEntry] Failed to update UI: $e2")
            }
        }
    }
    
    println("[CSEntry] Coroutine launched (async)")
}