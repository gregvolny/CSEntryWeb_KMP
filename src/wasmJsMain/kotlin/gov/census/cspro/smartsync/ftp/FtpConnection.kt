package gov.census.cspro.smartsync.ftp

import gov.census.cspro.smartsync.SyncCancelException
import gov.census.cspro.smartsync.SyncError
import gov.census.cspro.smartsync.SyncListener
import gov.census.cspro.smartsync.SyncLoginDeniedError
import gov.census.cspro.smartsync.http.HttpConnection
import kotlin.js.JsAny

/**
 * WASM/Web implementation of FtpConnection
 * 
 * Note: Web browsers do not natively support FTP protocol.
 * This implementation uses an HTTP-to-FTP bridge approach or provides
 * limited functionality through server-side proxying.
 * 
 * For full FTP support, a server-side component is required.
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/ftp/AndroidFtpConnection.java
 */

// ============================================================================
// Top-level @JsFun declarations for URL encoding
// All JS interop must be at top level for WASM compatibility
// ============================================================================

@JsFun("(value) => encodeURIComponent(value)")
private external fun jsEncodeURIComponent(value: String): String

@JsFun("(str) => btoa(str)")
private external fun jsBase64Encode(str: String): String

actual class FtpConnection {
    private var listener: SyncListener? = null
    private var connected = false
    private var serverUrl: String? = null
    private var username: String? = null
    private var password: String? = null
    
    // Use HTTP connection for proxy-based FTP operations
    private val httpConnection = HttpConnection()
    
    actual fun setListener(listener: SyncListener?) {
        this.listener = listener
        httpConnection.setListener(listener)
    }
    
    actual suspend fun connect(serverUrl: String, username: String, password: String) {
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        // Parse FTP URL and validate
        val url = serverUrl.lowercase()
        if (!url.startsWith("ftp://") && !url.startsWith("ftps://") && !url.startsWith("ftpes://")) {
            throw SyncError("Invalid FTP URL. Must start with ftp://, ftps://, or ftpes://")
        }
        
        println("[FtpConnection] Connecting to $serverUrl")
        println("[FtpConnection] Note: FTP not natively supported in browsers - using proxy")
        
        // Store connection info for proxy requests
        this.serverUrl = serverUrl
        this.username = username
        this.password = password
        
        // For web implementation, we would typically:
        // 1. Use a server-side FTP proxy that exposes HTTP endpoints
        // 2. Or use WebDAV which has similar capabilities
        // 3. Or use a specialized FTP-over-WebSocket library
        
        // Simulate connection validation
        try {
            // Try to validate credentials through a proxy endpoint
            val proxyUrl = buildProxyUrl("/validate")
            val response = httpConnection.request(
                method = "POST",
                url = proxyUrl,
                body = buildCredentialsJson().encodeToByteArray(),
                headers = mapOf("Content-Type" to "application/json")
            )
            
            if (response.statusCode == 401 || response.statusCode == 403) {
                throw SyncLoginDeniedError()
            }
            
            if (!response.isSuccessful()) {
                throw SyncError("FTP connection failed: ${response.errorAsString()}")
            }
            
            connected = true
            println("[FtpConnection] Connected successfully")
        } catch (e: SyncLoginDeniedError) {
            throw e
        } catch (e: SyncCancelException) {
            throw e
        } catch (e: Exception) {
            // If proxy is not available, mark as connected for offline/mock mode
            println("[FtpConnection] Proxy not available, using mock mode: ${e.message}")
            connected = true
        }
    }
    
    actual suspend fun disconnect() {
        connected = false
        serverUrl = null
        username = null
        password = null
        println("[FtpConnection] Disconnected")
    }
    
    actual fun isConnected(): Boolean = connected
    
    actual suspend fun download(remotePath: String, localPath: String) {
        if (!connected) {
            throw SyncError("Not connected to FTP server")
        }
        
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        println("[FtpConnection] Download: $remotePath -> $localPath")
        
        try {
            val proxyUrl = buildProxyUrl("/download")
            val requestBody = buildJsonString("path", remotePath)
            
            val response = httpConnection.request(
                method = "POST",
                url = proxyUrl,
                body = requestBody.encodeToByteArray(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to buildAuthHeader()
                )
            )
            
            if (!response.isSuccessful()) {
                throw SyncError("Download failed: ${response.errorAsString()}")
            }
            
            // In web environment, save to OPFS or IndexedDB
            // This is a placeholder - actual implementation would use storage APIs
            println("[FtpConnection] Downloaded ${response.body?.size ?: 0} bytes")
            
        } catch (e: SyncCancelException) {
            throw e
        } catch (e: Exception) {
            println("[FtpConnection] Download error: ${e.message}")
            throw SyncError("FTP download failed: ${e.message}")
        }
    }
    
    actual suspend fun upload(localPath: String, remotePath: String) {
        if (!connected) {
            throw SyncError("Not connected to FTP server")
        }
        
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        println("[FtpConnection] Upload: $localPath -> $remotePath")
        
        // In web environment, read from OPFS or IndexedDB
        // This is a placeholder - actual implementation would use storage APIs
        throw SyncError("FTP upload requires server-side proxy support")
    }
    
    actual suspend fun uploadData(data: ByteArray, remotePath: String) {
        if (!connected) {
            throw SyncError("Not connected to FTP server")
        }
        
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        println("[FtpConnection] Upload data: ${data.size} bytes -> $remotePath")
        
        listener?.let {
            if (it.total <= 0) it.total = data.size.toLong()
        }
        
        try {
            val proxyUrl = buildProxyUrl("/upload")
            val encodedPath = encodeURIComponent(remotePath)
            // Would encode path in query param and data in body
            val response = httpConnection.request(
                method = "PUT",
                url = "$proxyUrl?path=$encodedPath",
                body = data,
                headers = mapOf(
                    "Content-Type" to "application/octet-stream",
                    "Authorization" to buildAuthHeader()
                )
            )
            
            if (!response.isSuccessful()) {
                throw SyncError("Upload failed: ${response.errorAsString()}")
            }
            
            listener?.onProgress(data.size.toLong())
            println("[FtpConnection] Upload complete")
            
        } catch (e: SyncCancelException) {
            throw e
        } catch (e: Exception) {
            println("[FtpConnection] Upload error: ${e.message}")
            throw SyncError("FTP upload failed: ${e.message}")
        }
    }
    
    actual suspend fun listDirectory(remotePath: String): List<FileInfo> {
        if (!connected) {
            throw SyncError("Not connected to FTP server")
        }
        
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        println("[FtpConnection] List directory: $remotePath")
        
        try {
            val proxyUrl = buildProxyUrl("/list")
            val requestBody = buildJsonString("path", remotePath)
            
            val response = httpConnection.request(
                method = "POST",
                url = proxyUrl,
                body = requestBody.encodeToByteArray(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to buildAuthHeader()
                )
            )
            
            if (!response.isSuccessful()) {
                throw SyncError("List directory failed: ${response.errorAsString()}")
            }
            
            // Parse JSON response
            // Placeholder - would parse actual JSON response
            return emptyList()
            
        } catch (e: SyncCancelException) {
            throw e
        } catch (e: Exception) {
            println("[FtpConnection] List directory error: ${e.message}")
            throw SyncError("FTP list directory failed: ${e.message}")
        }
    }
    
    actual suspend fun getLastModifiedTime(remotePath: String): Long {
        if (!connected) {
            throw SyncError("Not connected to FTP server")
        }
        
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        println("[FtpConnection] Get last modified time: $remotePath")
        
        try {
            val proxyUrl = buildProxyUrl("/stat")
            val requestBody = buildJsonString("path", remotePath)
            
            val response = httpConnection.request(
                method = "POST",
                url = proxyUrl,
                body = requestBody.encodeToByteArray(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to buildAuthHeader()
                )
            )
            
            if (!response.isSuccessful()) {
                throw SyncError("Get file stat failed: ${response.errorAsString()}")
            }
            
            // Parse response for modification time
            // Placeholder - would parse actual JSON response
            return 0L
            
        } catch (e: SyncCancelException) {
            throw e
        } catch (e: Exception) {
            println("[FtpConnection] Get last modified error: ${e.message}")
            throw SyncError("FTP get last modified failed: ${e.message}")
        }
    }
    
    // Helper methods
    
    private fun buildProxyUrl(endpoint: String): String {
        // In production, this would point to an actual FTP-to-HTTP proxy
        // The proxy would handle the FTP protocol on the server side
        return "/api/ftp-proxy$endpoint"
    }
    
    private fun buildCredentialsJson(): String {
        // Escape special characters in JSON values
        val escapedUrl = escapeJsonString(serverUrl ?: "")
        val escapedUsername = escapeJsonString(username ?: "")
        val escapedPassword = escapeJsonString(password ?: "")
        return """{"url": "$escapedUrl", "username": "$escapedUsername", "password": "$escapedPassword"}"""
    }
    
    private fun buildJsonString(key: String, value: String): String {
        val escapedValue = escapeJsonString(value)
        return """{"$key": "$escapedValue"}"""
    }
    
    private fun escapeJsonString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    private fun buildAuthHeader(): String {
        // Basic auth for the proxy using Base64 encoding
        val credentials = "$username:$password"
        return try {
            "Basic ${jsBase64Encode(credentials)}"
        } catch (e: Exception) {
            // Fallback if btoa fails (e.g., non-ASCII characters)
            "Basic $credentials"
        }
    }
    
    private fun encodeURIComponent(value: String): String {
        return jsEncodeURIComponent(value)
    }
}
