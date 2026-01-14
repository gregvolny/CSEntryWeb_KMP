package gov.census.cspro.engine

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ActionInvoker Listener for Web
 * Ported from Android ActionInvokerListener
 * NOTE: Renamed to avoid conflict with external interface ActionInvokerListener
 */
open class WebActionInvokerListener {
    
    /**
     * Called to get display options for a web controller
     */
    open fun onGetDisplayOptions(webControllerKey: Int): String? {
        return null
    }
    
    /**
     * Called to set display options
     */
    open fun onSetDisplayOptions(displayOptionsJson: String, webControllerKey: Int): Boolean? {
        return null
    }
    
    /**
     * Called when a dialog should be closed
     */
    open fun onCloseDialog(resultsText: String?, webControllerKey: Int): Boolean? {
        return null
    }
    
    /**
     * Called when engine program control is executed
     */
    open fun onEngineProgramControlExecuted(): Boolean {
        return false
    }
    
    /**
     * Post a web message to the target
     */
    open fun onPostWebMessage(message: String, targetOrigin: String?) {
        // Override in subclass to post messages
    }
}

/**
 * ActionInvoker for Web
 * Ported from Android ActionInvoker.kt
 * 
 * Provides the bridge between JavaScript and the CSPro engine
 */
open class ActionInvoker(
    private val accessTokenOverride: String? = null,
    protected val listener: WebActionInvokerListener = WebActionInvokerListener()
) {
    private var webControllerKey: Int? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Get or create web controller key
     */
    suspend fun getWebControllerKey(): Int {
        if (webControllerKey == null) {
            val engine = CSProEngineService.getInstance()
            webControllerKey = engine?.actionInvokerCreateWebController(accessTokenOverride ?: "") ?: -1
        }
        return webControllerKey!!
    }
    
    /**
     * Cancel and wait on actions in progress
     */
    suspend fun cancelAndWaitOnActionsInProgress() {
        if (webControllerKey != null) {
            val engine = CSProEngineService.getInstance()
            engine?.actionInvokerCancelAndWaitOnActionsInProgress(webControllerKey!!)
        }
    }
    
    /**
     * Run an action synchronously
     */
    suspend fun run(message: String): String {
        return runSync(message)
    }
    
    /**
     * Internal sync run
     */
    protected open suspend fun runSync(message: String): String {
        val engine = CSProEngineService.getInstance() ?: return ""
        val key = getWebControllerKey()
        
        return engine.actionInvokerProcessMessage(
            webControllerKey = key,
            listener = null, // External listener not supported in WASM
            message = message,
            async = false,
            calledByOldCSProObject = false
        )
    }
    
    /**
     * Run an action asynchronously
     */
    fun runAsync(message: String, callback: ((String) -> Unit)? = null) {
        scope.launch {
            val result = runAsyncInternal(message, false)
            callback?.invoke(result)
        }
    }
    
    /**
     * Run async for old CSPro object compatibility
     */
    fun runAsyncOldCSPro(message: String, callback: (String) -> Unit) {
        scope.launch {
            val result = runAsyncInternal(message, true)
            callback(result)
        }
    }
    
    /**
     * Internal async run
     */
    protected suspend fun runAsyncInternal(message: String, calledByOldCSProObject: Boolean): String {
        val engine = CSProEngineService.getInstance() ?: return ""
        val key = getWebControllerKey()
        
        return engine.actionInvokerProcessMessage(
            webControllerKey = key,
            listener = null, // External listener not supported in WASM, use polling instead
            message = message,
            async = true,
            calledByOldCSProObject = calledByOldCSProObject
        )
    }
}

// ActionInvokerActivityResult is defined in WasmEngineInterface.kt
// Use that definition to avoid redeclaration

// External JS function for reading properties from JsAny
@JsFun("(obj, key) => obj[key]")
private external fun getJsProperty(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key) => obj[key] === true")
private external fun getJsBooleanProperty(obj: JsAny, key: String): Boolean

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun getJsStringProperty(obj: JsAny, key: String): String?

/**
 * Extension to convert JS result to ActionInvokerActivityResult
 */
fun JsAny.toActionInvokerActivityResult(): ActionInvokerActivityResult {
    return ActionInvokerActivityResult(
        success = getJsBooleanProperty(this, "success"),
        error = getJsStringProperty(this, "error"),
        accessToken = getJsStringProperty(this, "accessToken"),
        refreshToken = getJsStringProperty(this, "refreshToken")
    )
}

/**
 * Singleton for managing action invoker instances
 */
object ActionInvokerManager {
    private val invokers = mutableMapOf<Int, ActionInvoker>()
    private var nextId = 0
    
    /**
     * Create a new action invoker
     */
    fun create(accessToken: String? = null, listener: WebActionInvokerListener? = null): Int {
        val id = nextId++
        invokers[id] = ActionInvoker(accessToken, listener ?: WebActionInvokerListener())
        return id
    }
    
    /**
     * Get an action invoker by ID
     */
    fun get(id: Int): ActionInvoker? = invokers[id]
    
    /**
     * Remove an action invoker
     */
    suspend fun remove(id: Int) {
        invokers[id]?.cancelAndWaitOnActionsInProgress()
        invokers.remove(id)
    }
    
    /**
     * Clear all invokers
     */
    suspend fun clear() {
        invokers.values.forEach { it.cancelAndWaitOnActionsInProgress() }
        invokers.clear()
    }
}
