package gov.census.cspro.smartsync

/**
 * Sync direction enumeration
 * Mirrors: CSPro C++ SyncDirection enum
 */
enum class SyncDirection {
    PUT,    // Upload data to server
    GET,    // Download data from server
    BOTH    // Bidirectional sync
}

/**
 * Sync result enumeration
 * Mirrors: CSPro C++ SyncClient::SyncResult enum
 */
enum class SyncResult(val value: Int) {
    SYNC_OK(1),
    SYNC_ERROR(0),
    SYNC_CANCELED(-1)
}

/**
 * Sync server type enumeration
 */
enum class SyncServerType {
    NONE,
    CSWEB,
    DROPBOX,
    FTP,
    BLUETOOTH,
    LOCAL_FILE_SYSTEM
}

/**
 * Dictionary information
 */
data class DictionaryInfo(
    val name: String,
    val label: String
)

/**
 * Application package information
 */
data class ApplicationPackage(
    val name: String,
    val description: String?,
    val buildTime: Long?,
    val signature: String?
)

/**
 * Sync client interface for smart sync operations
 * Mirrors: CSPro C++ SyncClient class
 */
expect class SyncClient(deviceId: String) {
    /**
     * Connect to a sync server
     * @param hostUrl Server URL
     * @param username Optional username
     * @param password Optional password
     * @return Sync result
     */
    suspend fun connect(
        hostUrl: String,
        username: String? = null,
        password: String? = null
    ): SyncResult
    
    /**
     * Connect to a web (CSWeb) server
     * @param hostUrl Server URL
     * @param username Username
     * @param password Password
     * @return Sync result
     */
    suspend fun connectWeb(hostUrl: String, username: String, password: String): SyncResult
    
    /**
     * Connect to a Dropbox server
     * @param accessToken Dropbox access token
     * @return Sync result
     */
    suspend fun connectDropbox(accessToken: String): SyncResult
    
    /**
     * Connect to an FTP server
     * @param hostUrl FTP server URL
     * @param username Username
     * @param password Password
     * @return Sync result
     */
    suspend fun connectFtp(hostUrl: String, username: String, password: String): SyncResult
    
    /**
     * Connect to a Bluetooth device for P2P sync
     * @param deviceAddress Bluetooth device address
     * @return Sync result
     */
    suspend fun connectBluetooth(deviceAddress: String): SyncResult
    
    /**
     * Connect to local file system
     * @param rootDirectory Root directory for sync
     * @return Sync result
     */
    suspend fun connectLocalFileSystem(rootDirectory: String): SyncResult
    
    /**
     * Disconnect from the server
     * @return Sync result
     */
    suspend fun disconnect(): SyncResult
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get the server device ID
     */
    fun getServerDeviceId(): String?
    
    /**
     * Sync data file using smart sync
     * @param direction Direction of sync
     * @param dictionaryName Dictionary name
     * @param universe Optional universe filter
     * @return Sync result
     */
    suspend fun syncData(
        direction: SyncDirection,
        dictionaryName: String,
        universe: String? = null
    ): SyncResult
    
    /**
     * Sync a file
     * @param direction Direction of sync (PUT or GET)
     * @param pathFrom Source path
     * @param pathTo Destination path
     * @return Sync result
     */
    suspend fun syncFile(
        direction: SyncDirection,
        pathFrom: String,
        pathTo: String
    ): SyncResult
    
    /**
     * Get list of dictionaries on the server
     * @return List of dictionary info
     */
    suspend fun getDictionaries(): List<DictionaryInfo>
    
    /**
     * Download a dictionary from the server
     * @param dictionaryName Name of dictionary
     * @return Dictionary content as text
     */
    suspend fun downloadDictionary(dictionaryName: String): String?
    
    /**
     * Upload a dictionary to the server
     * @param dictPath Path to dictionary file
     * @return Sync result
     */
    suspend fun uploadDictionary(dictPath: String): SyncResult
    
    /**
     * Delete a dictionary on the server
     * @param dictName Dictionary name
     * @return Sync result
     */
    suspend fun deleteDictionary(dictName: String): SyncResult
    
    /**
     * List application packages on the server
     * @return List of packages
     */
    suspend fun listApplicationPackages(): List<ApplicationPackage>
    
    /**
     * Download and install an application package
     * @param packageName Package name
     * @param forceFullInstall Force full installation
     * @return Sync result
     */
    suspend fun downloadApplicationPackage(packageName: String, forceFullInstall: Boolean): SyncResult
    
    /**
     * Upload an application package
     * @param localPath Path to local package file
     * @param packageName Package name
     * @param packageSpecJson Package specification JSON
     * @return Sync result
     */
    suspend fun uploadApplicationPackage(
        localPath: String,
        packageName: String,
        packageSpecJson: String
    ): SyncResult
    
    /**
     * Sync paradata
     * @param direction Sync direction
     * @return Sync result
     */
    suspend fun syncParadata(direction: SyncDirection): SyncResult
    
    /**
     * Send/receive a sync message
     * @param messageKey Message key
     * @param messageValue Message value
     * @return Response message
     */
    suspend fun syncMessage(messageKey: String, messageValue: String): String?
    
    /**
     * Set the sync listener for progress tracking
     */
    fun setListener(listener: SyncListener?)
}
