package gov.census.cspro.platform

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * External declaration for the WASM module factory function
 * This is created by Emscripten and loaded via script tag
 */
@JsName("createCSEntryKMPModule")
external fun createCSProModule(config: JsAny?): Promise<CSProWasmModule>

/**
 * Factory to create and initialize the CSPro WASM module
 */
object CSProModuleFactory {
    private var moduleInstance: CSProWasmModule? = null
    
    suspend fun getInstance(): CSProWasmModule {
        if (moduleInstance == null) {
            println("[CSProModule] Creating CSPro WASM module...")
            
            // Create module without config (will use defaults)
            moduleInstance = createCSProModule(null).await<CSProWasmModule>()
            println("[CSProModule] CSPro WASM module created successfully")
        }
        
        return moduleInstance!!
    }
    
    fun isInitialized(): Boolean = moduleInstance != null
}


