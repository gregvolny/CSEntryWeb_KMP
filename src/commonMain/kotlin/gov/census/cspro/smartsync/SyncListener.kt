package gov.census.cspro.smartsync

/**
 * Sync progress listener interface
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/SyncListenerWrapper.java
 */
interface SyncListener {
    /**
     * Get the total size of the transfer
     */
    var total: Long
    
    /**
     * Report progress
     * @param progress Number of bytes transferred so far
     */
    fun onProgress(progress: Long)
    
    /**
     * Check if the operation has been cancelled
     */
    fun isCancelled(): Boolean
}

/**
 * Default implementation of SyncListener
 */
open class DefaultSyncListener : SyncListener {
    override var total: Long = 0L
    private var cancelled = false
    private var progress: Long = 0L
    
    override fun onProgress(progress: Long) {
        this.progress = progress
    }
    
    override fun isCancelled(): Boolean = cancelled
    
    /**
     * Cancel the operation
     */
    fun cancel() {
        cancelled = true
    }
    
    /**
     * Get current progress
     */
    fun getProgress(): Long = progress
}
