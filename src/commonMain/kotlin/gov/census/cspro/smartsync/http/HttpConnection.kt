package gov.census.cspro.smartsync.http

/**
 * HTTP connection interfaces for SmartSync
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/http/AndroidHttpConnection.kt
 */
import gov.census.cspro.smartsync.SyncListener

/**
 * HTTP Response data
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/http/HttpResponse.kt
 */
data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: ByteArray?,
    val errorBody: ByteArray? = null
) {
    /**
     * Check if the response was successful (2xx status code)
     */
    fun isSuccessful(): Boolean = statusCode in 200..299
    
    /**
     * Get the body as a string
     */
    fun bodyAsString(): String? = body?.decodeToString()
    
    /**
     * Get the error body as a string
     */
    fun errorAsString(): String? = errorBody?.decodeToString()
    
    /**
     * Get a specific header value
     */
    fun getHeader(name: String): String? = headers[name.lowercase()]
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as HttpResponse
        
        if (statusCode != other.statusCode) return false
        if (headers != other.headers) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * HTTP Connection interface for sync operations
 */
expect class HttpConnection() {
    /**
     * Set the sync listener for progress tracking
     */
    fun setListener(listener: SyncListener?)
    
    /**
     * Send an HTTP request
     * @param method HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param url Full URL to request
     * @param body Request body data (or null)
     * @param headers Map of request headers
     * @return HTTP response
     */
    suspend fun request(
        method: String,
        url: String,
        body: ByteArray? = null,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse
    
    /**
     * Download a file with progress tracking
     * @param url URL to download from
     * @param headers Request headers
     * @return Downloaded content as ByteArray
     */
    suspend fun download(url: String, headers: Map<String, String> = emptyMap()): ByteArray
    
    /**
     * Upload data with progress tracking
     * @param url URL to upload to
     * @param data Data to upload
     * @param headers Request headers
     * @return HTTP response
     */
    suspend fun upload(url: String, data: ByteArray, headers: Map<String, String> = emptyMap()): HttpResponse
}
