package gov.census.cspro.engine

/**
 * Listener interface for engine messages/events
 * Web equivalent of Android's IEngineMessageListener
 */
interface IEngineMessageListener {
    /**
     * Called when the engine requests a page refresh
     */
    fun onRefreshPage()
    
    /**
     * Called when the engine wants to show a dialog
     */
    fun onShowDialog(title: String, message: String)
    
    /**
     * Called when an error occurs
     */
    fun onError(error: String)
    
    /**
     * Called to update progress
     */
    fun onProgress(current: Int, total: Int)
}
