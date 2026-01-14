package gov.census.cspro.engine

/**
 * Shared data classes for CSPro engine
 * These were originally in WasmEngineInterface.kt
 */

/**
 * Application mapping options
 */
data class AppMappingOptions(
    val enabled: Boolean,
    val baseMapUrl: String?,
    val allowOfflineUse: Boolean
)

/**
 * Action invoker result
 */
data class ActionInvokerActivityResult(
    val success: Boolean,
    val error: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

/**
 * Listener interface for action invoker events
 * This is used for receiving callbacks from action invoker operations
 */
interface ActionInvokerListener {
    /**
     * Called when an action invoker operation completes
     */
    fun onActionComplete(result: ActionInvokerActivityResult)
    
    /**
     * Called when an action invoker operation progresses
     */
    fun onProgress(message: String, progress: Int, total: Int) {}
}
