package gov.census.cspro.engine

import gov.census.cspro.platform.CSProWasmModule
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/**
 * CSPro JavaScript Interface for Web
 * Ported from Android CSProJavaScriptInterface.kt
 * 
 * This provides the legacy CSPro JavaScript API that HTML content can use:
 * - CSPro.getMaxDisplayWidth()
 * - CSPro.getMaxDisplayHeight()
 * - CSPro.getInputData()
 * - CSPro.setDisplayOptions()
 * - CSPro.returnData()
 * - CSPro.getAsyncResult()
 * - CSPro.do(action)
 * - CSPro.runLogic(logic)
 * - CSPro.runLogicAsync(logic)
 * - CSPro.invoke(functionName)
 * - CSPro.invokeAsync(functionName)
 * 
 * WASM-compatible implementation using JsFun annotations for JS interop
 */

// Top-level JsFun declarations for WASM interop
@JsFun("(code) => { try { eval(code); } catch(e) { console.error('JS eval error:', e); } }")
private external fun evalJavaScript(code: String)

@JsFun("() => window.innerWidth || 1024")
private external fun getWindowWidth(): Int

@JsFun("() => window.innerHeight || 768")
private external fun getWindowHeight(): Int

@JsFun("() => window._csproInputData || ''")
private external fun getStoredInputData(): String

@JsFun("(data) => { window._csproLastResult = data; }")
private external fun storeAsyncResult(data: String)

@JsFun("() => window._csproLastResult || null")
private external fun getStoredAsyncResult(): String?

@JsFun("(json) => { window._csproDisplayOptions = json; }")
private external fun storeDisplayOptions(json: String)

@JsFun("(json) => { if (window._csproDialogCallback) window._csproDialogCallback(json); }")
private external fun sendDialogResult(json: String)

/**
 * CSPro JavaScript Interface
 * Provides API for HTML dialogs to communicate with CSPro engine
 */
class CSProJavaScriptInterface(
    private val csproModule: CSProWasmModule?,
    private val accessToken: String = ""
) {
    private var lastAsyncResult: String? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Action code for "execute" matches Android constant (11276)
    private val ACTION_EXECUTE = 11276
    
    /**
     * Get maximum display width
     */
    fun getMaxDisplayWidth(): Int {
        return try {
            getWindowWidth()
        } catch (e: Exception) {
            1024
        }
    }
    
    /**
     * Get maximum display height
     */
    fun getMaxDisplayHeight(): Int {
        return try {
            getWindowHeight()
        } catch (e: Exception) {
            768
        }
    }
    
    /**
     * Get input data passed to the dialog
     */
    fun getInputData(): String {
        return try {
            getStoredInputData()
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Set display options for the dialog
     */
    fun setDisplayOptions(jsonText: String) {
        try {
            storeDisplayOptions(jsonText)
        } catch (e: Exception) {
            println("[CSProJS] Error setting display options: ${e.message}")
        }
    }
    
    /**
     * Return data from the dialog
     */
    fun returnData(jsonText: String) {
        try {
            sendDialogResult(jsonText)
        } catch (e: Exception) {
            println("[CSProJS] Error returning data: ${e.message}")
        }
    }
    
    /**
     * Get the last async result
     */
    fun getAsyncResult(): String? {
        return lastAsyncResult ?: getStoredAsyncResult()
    }
    
    /**
     * Execute a simple action
     */
    fun doAction(action: String): String? {
        return when (action) {
            "close" -> {
                sendDialogResult("{\"action\":\"close\"}")
                null
            }
            else -> null
        }
    }
    
    /**
     * Execute an action with input
     */
    fun doAction(action: String, input: String): String? {
        // Not commonly used, but available
        return null
    }
    
    /**
     * Run CSPro logic synchronously
     */
    fun runLogic(logic: String): String? {
        return try {
            // In web context, true sync calls are not possible
            // Return null and log warning
            println("[CSProJS] Sync logic execution not supported in web - use runLogicAsync")
            null
        } catch (e: Exception) {
            println("[CSProJS] Error running logic: ${e.message}")
            null
        }
    }
    
    /**
     * Run CSPro logic asynchronously
     */
    fun runLogicAsync(logic: String, callback: String? = null) {
        scope.launch {
            try {
                // Execute logic through engine
                val result = executeLogicAsync(logic)
                lastAsyncResult = result
                storeAsyncResult(result ?: "")
                
                if (callback != null) {
                    // Execute callback in the web context
                    evalJavaScript(callback)
                }
            } catch (e: Exception) {
                println("[CSProJS] Error in runLogicAsync: ${e.message}")
                lastAsyncResult = null
            }
        }
    }
    
    /**
     * Invoke a CSPro function synchronously
     */
    fun invoke(functionName: String, arguments: String? = null): String? {
        return try {
            // In web context, true sync calls are not possible
            println("[CSProJS] Sync function invocation not supported in web - use invokeAsync")
            null
        } catch (e: Exception) {
            println("[CSProJS] Error invoking function: ${e.message}")
            null
        }
    }
    
    /**
     * Invoke a CSPro function asynchronously
     */
    fun invokeAsync(functionName: String, arguments: String? = null, callback: String? = null) {
        scope.launch {
            try {
                val result = invokeFunctionAsync(functionName, arguments)
                lastAsyncResult = result
                storeAsyncResult(result ?: "")
                
                if (callback != null) {
                    evalJavaScript(callback)
                }
            } catch (e: Exception) {
                println("[CSProJS] Error in invokeAsync: ${e.message}")
                lastAsyncResult = null
            }
        }
    }
    
    // ========================================================================
    // Internal helpers
    // ========================================================================
    
    private fun createActionInvokerMessage(action: String, argumentsJson: JsonObject?): String {
        val messageJson = buildJsonObject {
            put("action", ACTION_EXECUTE)
            put("accessToken", accessToken)
            put("arguments", buildJsonObject {
                put("action", action)
                if (argumentsJson != null) {
                    put("arguments", argumentsJson)
                }
            })
        }
        return messageJson.toString()
    }
    
    private fun getStringResultFromResponse(response: String?): String? {
        if (response == null) return null
        
        return try {
            val responseJson = Json.parseToJsonElement(response).jsonObject
            if (responseJson["type"]?.jsonPrimitive?.contentOrNull == "exception") {
                null
            } else {
                responseJson["value"]?.jsonPrimitive?.contentOrNull
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun executeLogicAsync(logic: String): String? {
        // Use CSProEngineService to execute logic
        return try {
            val service = CSProEngineService.getInstance()
            val result = service?.runLogic(logic)
            result
        } catch (e: Exception) {
            println("[CSProJS] Error executing logic: ${e.message}")
            null
        }
    }
    
    private suspend fun invokeFunctionAsync(functionName: String, arguments: String?): String? {
        // Use CSProEngineService to invoke function
        return try {
            val service = CSProEngineService.getInstance()
            val result = service?.invokeFunction(functionName, arguments)
            result
        } catch (e: Exception) {
            println("[CSProJS] Error invoking function: ${e.message}")
            null
        }
    }
    
    companion object {
        /**
         * Install CSPro API into global window object
         * This is called during application initialization
         */
        fun installGlobalApi(module: CSProWasmModule?, accessToken: String = "") {
            val instance = CSProJavaScriptInterface(module, accessToken)
            GlobalCSProApi.install(instance)
        }
    }
}

/**
 * Global CSPro API installer
 * Uses JsFun to install the API into the window object
 */
object GlobalCSProApi {
    private var instance: CSProJavaScriptInterface? = null
    
    fun install(jsInterface: CSProJavaScriptInterface) {
        instance = jsInterface
        installApiToWindow()
    }
    
    fun getInstance(): CSProJavaScriptInterface? = instance
}

// Install functions using JsFun
@JsFun("""() => {
    window.CSPro = window.CSPro || {};
    
    // These will be connected via Kotlin callbacks
    window.CSPro._ready = false;
    window.CSPro._pendingCalls = [];
    
    console.log('[CSProJS] CSPro API placeholder installed');
}""")
private external fun installApiToWindow()
