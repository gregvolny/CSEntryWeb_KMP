package gov.census.cspro.smartsync.ftp

/**
 * FTP connection interfaces for SmartSync
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/ftp/AndroidFtpConnection.java
 */
import gov.census.cspro.smartsync.SyncListener

/**
 * File information for directory listings
 */
data class FileInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedTime: Long?  // Unix timestamp in milliseconds
)

/**
 * FTP Connection interface for sync operations
 * Note: Web browsers do not support FTP directly, so this provides a limited
 * implementation using HTTP-to-FTP bridges or returns unsupported errors
 */
expect class FtpConnection() {
    /**
     * Set the sync listener for progress tracking
     */
    fun setListener(listener: SyncListener?)
    
    /**
     * Connect to FTP server
     * @param serverUrl FTP server URL (ftp://, ftps://, or ftpes://)
     * @param username Username for authentication
     * @param password Password for authentication
     */
    suspend fun connect(serverUrl: String, username: String, password: String)
    
    /**
     * Disconnect from FTP server
     */
    suspend fun disconnect()
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean
    
    /**
     * Download a file from the server
     * @param remotePath Remote file path
     * @param localPath Local file path to save to
     */
    suspend fun download(remotePath: String, localPath: String)
    
    /**
     * Upload a file to the server
     * @param localPath Local file path
     * @param remotePath Remote file path
     */
    suspend fun upload(localPath: String, remotePath: String)
    
    /**
     * Upload data to the server
     * @param data Data to upload
     * @param remotePath Remote file path
     */
    suspend fun uploadData(data: ByteArray, remotePath: String)
    
    /**
     * Get directory listing
     * @param remotePath Remote directory path
     * @return List of files and directories
     */
    suspend fun listDirectory(remotePath: String): List<FileInfo>
    
    /**
     * Get last modified time of a file
     * @param remotePath Remote file path
     * @return Modified time as Unix timestamp in seconds
     */
    suspend fun getLastModifiedTime(remotePath: String): Long
}
