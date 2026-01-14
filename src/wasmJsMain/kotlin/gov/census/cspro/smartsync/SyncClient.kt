package gov.census.cspro.smartsync

import gov.census.cspro.smartsync.ftp.FtpConnection
import gov.census.cspro.smartsync.http.HttpConnection
import kotlin.js.JsAny

// ============================================================================
// Top-level @JsFun external functions for JSON parsing and URL encoding
// ============================================================================

@JsFun("(jsonStr) => JSON.parse(jsonStr)")
private external fun jsonParse(jsonStr: String): JsAny

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun getStringProperty(obj: JsAny, key: String): String?

@JsFun("(obj, key) => obj[key]")
private external fun getProperty(obj: JsAny, key: String): JsAny?

@JsFun("(arr) => arr.length")
private external fun getArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun getArrayItem(arr: JsAny, idx: Int): JsAny

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : null")
private external fun getNumberProperty(obj: JsAny, key: String): Double?

@JsFun("(value) => encodeURIComponent(value)")
private external fun jsEncodeURIComponent(value: String): String

@JsFun("(msg) => console.log('[SyncClient] ' + msg)")
private external fun logInfo(msg: String)

@JsFun("(msg) => console.error('[SyncClient] ' + msg)")
private external fun logError(msg: String)

/**
 * WASM/Web implementation of SyncClient
 * Uses HTTP/HTTPS for web-based sync operations
 * 
 * Mirrors: CSPro C++ SyncClient class
 */
actual class SyncClient actual constructor(private val deviceId: String) {
    private val httpConnection = HttpConnection()
    private var ftpConnection: FtpConnection? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    private var listener: SyncListener? = null
    private var serverUrl: String? = null
    private var serverDeviceId: String? = null
    private var connected = false
    private var serverType = SyncServerType.NONE
    
    private var authToken: String? = null
    
    actual suspend fun connect(
        hostUrl: String,
        username: String?,
        password: String?
    ): SyncResult {
        return try {
            // Determine server type from URL
            val url = hostUrl.lowercase()
            when {
                url.startsWith("bluetooth:") -> {
                    connectBluetooth(hostUrl.removePrefix("bluetooth:"))
                }
                url.startsWith("ftp://") || url.startsWith("ftps://") -> {
                    connectFtp(hostUrl, username ?: "", password ?: "")
                }
                url.startsWith("dropbox:") -> {
                    connectDropbox(password ?: "") // password contains access token
                }
                url.startsWith("file://") -> {
                    connectLocalFileSystem(hostUrl.removePrefix("file://"))
                }
                else -> {
                    connectWeb(hostUrl, username ?: "", password ?: "")
                }
            }
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] Connect error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun connectWeb(hostUrl: String, username: String, password: String): SyncResult {
        return try {
            println("[SyncClient] Connecting to CSWeb: $hostUrl")
            
            serverUrl = hostUrl.trimEnd('/')
            serverType = SyncServerType.CSWEB
            
            // Authenticate with CSWeb server
            val loginUrl = "$serverUrl/api/login"
            val loginBody = """{"username": "$username", "password": "$password", "deviceId": "$deviceId"}"""
            
            val response = httpConnection.request(
                method = "POST",
                url = loginUrl,
                body = loginBody.encodeToByteArray(),
                headers = mapOf("Content-Type" to "application/json")
            )
            
            if (response.isSuccessful()) {
                // Parse token from response
                val responseJson = response.bodyAsString() ?: "{}"
                authToken = parseJsonString(responseJson, "token")
                serverDeviceId = parseJsonString(responseJson, "deviceId")
                connected = true
                println("[SyncClient] Connected to CSWeb")
                SyncResult.SYNC_OK
            } else if (response.statusCode == 401) {
                println("[SyncClient] Authentication failed")
                SyncResult.SYNC_ERROR
            } else {
                println("[SyncClient] Connection failed: ${response.statusCode}")
                SyncResult.SYNC_ERROR
            }
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] Error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun connectDropbox(accessToken: String): SyncResult {
        return try {
            println("[SyncClient] Connecting to Dropbox")
            
            serverType = SyncServerType.DROPBOX
            authToken = accessToken
            
            // Verify access token by making an API call
            val response = httpConnection.request(
                method = "POST",
                url = "https://api.dropboxapi.com/2/users/get_current_account",
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            )
            
            if (response.isSuccessful()) {
                connected = true
                println("[SyncClient] Connected to Dropbox")
                SyncResult.SYNC_OK
            } else {
                println("[SyncClient] Dropbox auth failed: ${response.statusCode}")
                SyncResult.SYNC_ERROR
            }
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] Error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun connectFtp(hostUrl: String, username: String, password: String): SyncResult {
        return try {
            println("[SyncClient] Connecting to FTP: $hostUrl")
            
            serverType = SyncServerType.FTP
            serverUrl = hostUrl
            
            ftpConnection = FtpConnection()
            ftpConnection?.setListener(listener)
            ftpConnection?.connect(hostUrl, username, password)
            
            connected = true
            println("[SyncClient] Connected to FTP")
            SyncResult.SYNC_OK
        } catch (e: SyncLoginDeniedError) {
            println("[SyncClient] FTP login denied")
            SyncResult.SYNC_ERROR
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] FTP error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun connectBluetooth(deviceAddress: String): SyncResult {
        return try {
            println("[SyncClient] Connecting to Bluetooth: $deviceAddress")
            
            serverType = SyncServerType.BLUETOOTH
            
            bluetoothAdapter = BluetoothAdapter.create()
            if (bluetoothAdapter == null) {
                println("[SyncClient] Bluetooth not available")
                return SyncResult.SYNC_ERROR
            }
            
            val connectResult = bluetoothAdapter?.connectToDevice(
                deviceAddress,
                object : BluetoothSyncListener {
                    override fun onDeviceFound(device: BluetoothDeviceInfo) {}
                    override fun onDeviceConnected(device: BluetoothDeviceInfo) {
                        serverDeviceId = device.id
                        connected = true
                    }
                    override fun onDeviceDisconnected(device: BluetoothDeviceInfo) {
                        connected = false
                    }
                    override fun onDataReceived(data: ByteArray) {}
                    override fun onError(error: String) {}
                    override fun onScanComplete() {}
                }
            )
            
            if (connectResult == true) {
                println("[SyncClient] Connected to Bluetooth device")
                SyncResult.SYNC_OK
            } else {
                println("[SyncClient] Bluetooth connection failed")
                SyncResult.SYNC_ERROR
            }
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] Bluetooth error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun connectLocalFileSystem(rootDirectory: String): SyncResult {
        // In web environment, we use OPFS
        println("[SyncClient] Connecting to local file system (OPFS): $rootDirectory")
        serverType = SyncServerType.LOCAL_FILE_SYSTEM
        serverUrl = rootDirectory
        connected = true
        return SyncResult.SYNC_OK
    }
    
    actual suspend fun disconnect(): SyncResult {
        return try {
            when (serverType) {
                SyncServerType.FTP -> {
                    ftpConnection?.disconnect()
                    ftpConnection = null
                }
                SyncServerType.BLUETOOTH -> {
                    bluetoothAdapter?.disconnect()
                    bluetoothAdapter = null
                }
                else -> {
                    // HTTP connections are stateless
                }
            }
            
            connected = false
            serverUrl = null
            serverDeviceId = null
            authToken = null
            serverType = SyncServerType.NONE
            
            println("[SyncClient] Disconnected")
            SyncResult.SYNC_OK
        } catch (e: Exception) {
            println("[SyncClient] Disconnect error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual fun isConnected(): Boolean = connected
    
    actual fun getServerDeviceId(): String? = serverDeviceId
    
    actual suspend fun syncData(
        direction: SyncDirection,
        dictionaryName: String,
        universe: String?
    ): SyncResult {
        if (!connected) {
            println("[SyncClient] Not connected")
            return SyncResult.SYNC_ERROR
        }
        
        return try {
            println("[SyncClient] Syncing data: $dictionaryName, direction: $direction")
            
            val url = "$serverUrl/api/sync/data"
            val body = buildString {
                append("{")
                append("\"direction\": \"${direction.name}\",")
                append("\"dictionary\": \"$dictionaryName\"")
                if (universe != null) {
                    append(",\"universe\": \"$universe\"")
                }
                append("}")
            }
            
            val response = httpConnection.request(
                method = "POST",
                url = url,
                body = body.encodeToByteArray(),
                headers = buildAuthHeaders()
            )
            
            if (response.isSuccessful()) {
                println("[SyncClient] Sync data complete")
                SyncResult.SYNC_OK
            } else {
                println("[SyncClient] Sync data failed: ${response.statusCode}")
                SyncResult.SYNC_ERROR
            }
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] Sync data error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun syncFile(
        direction: SyncDirection,
        pathFrom: String,
        pathTo: String
    ): SyncResult {
        if (!connected) {
            println("[SyncClient] Not connected")
            return SyncResult.SYNC_ERROR
        }
        
        return try {
            println("[SyncClient] Syncing file: $pathFrom -> $pathTo, direction: $direction")
            
            when (direction) {
                SyncDirection.GET -> {
                    val url = "$serverUrl/api/files/${encodeURIComponent(pathFrom)}"
                    val data = httpConnection.download(url, buildAuthHeaders())
                    // Would save to OPFS here
                    println("[SyncClient] Downloaded ${data.size} bytes")
                }
                SyncDirection.PUT -> {
                    // Would read from OPFS here
                    val url = "$serverUrl/api/files/${encodeURIComponent(pathTo)}"
                    httpConnection.upload(url, ByteArray(0), buildAuthHeaders())
                }
                SyncDirection.BOTH -> {
                    // Bidirectional not implemented for files
                    return SyncResult.SYNC_ERROR
                }
            }
            
            SyncResult.SYNC_OK
        } catch (e: SyncCancelException) {
            SyncResult.SYNC_CANCELED
        } catch (e: Exception) {
            println("[SyncClient] Sync file error: ${e.message}")
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun getDictionaries(): List<DictionaryInfo> {
        if (!connected) return emptyList()
        
        return try {
            val url = "$serverUrl/api/dictionaries"
            val response = httpConnection.request("GET", url, null, buildAuthHeaders())
            
            if (response.isSuccessful()) {
                parseDictionariesJson(response.bodyAsString() ?: "[]")
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("[SyncClient] Get dictionaries error: ${e.message}")
            emptyList()
        }
    }
    
    actual suspend fun downloadDictionary(dictionaryName: String): String? {
        if (!connected) return null
        
        return try {
            val url = "$serverUrl/api/dictionaries/${encodeURIComponent(dictionaryName)}"
            val response = httpConnection.request("GET", url, null, buildAuthHeaders())
            
            if (response.isSuccessful()) {
                response.bodyAsString()
            } else {
                null
            }
        } catch (e: Exception) {
            println("[SyncClient] Download dictionary error: ${e.message}")
            null
        }
    }
    
    actual suspend fun uploadDictionary(dictPath: String): SyncResult {
        if (!connected) return SyncResult.SYNC_ERROR
        
        return try {
            // Would read from OPFS here
            val url = "$serverUrl/api/dictionaries"
            val response = httpConnection.upload(url, ByteArray(0), buildAuthHeaders())
            
            if (response.isSuccessful()) SyncResult.SYNC_OK else SyncResult.SYNC_ERROR
        } catch (e: Exception) {
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun deleteDictionary(dictName: String): SyncResult {
        if (!connected) return SyncResult.SYNC_ERROR
        
        return try {
            val url = "$serverUrl/api/dictionaries/${encodeURIComponent(dictName)}"
            val response = httpConnection.request("DELETE", url, null, buildAuthHeaders())
            
            if (response.isSuccessful()) SyncResult.SYNC_OK else SyncResult.SYNC_ERROR
        } catch (e: Exception) {
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun listApplicationPackages(): List<ApplicationPackage> {
        if (!connected) return emptyList()
        
        return try {
            val url = "$serverUrl/api/applications"
            val response = httpConnection.request("GET", url, null, buildAuthHeaders())
            
            if (response.isSuccessful()) {
                parseApplicationsJson(response.bodyAsString() ?: "[]")
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("[SyncClient] List packages error: ${e.message}")
            emptyList()
        }
    }
    
    actual suspend fun downloadApplicationPackage(packageName: String, forceFullInstall: Boolean): SyncResult {
        if (!connected) return SyncResult.SYNC_ERROR
        
        return try {
            val url = "$serverUrl/api/applications/${encodeURIComponent(packageName)}/download"
            val body = """{"forceFullInstall": $forceFullInstall}"""
            val response = httpConnection.request("POST", url, body.encodeToByteArray(), buildAuthHeaders())
            
            if (response.isSuccessful()) SyncResult.SYNC_OK else SyncResult.SYNC_ERROR
        } catch (e: Exception) {
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun uploadApplicationPackage(
        localPath: String,
        packageName: String,
        packageSpecJson: String
    ): SyncResult {
        if (!connected) return SyncResult.SYNC_ERROR
        
        return try {
            val url = "$serverUrl/api/applications/${encodeURIComponent(packageName)}/upload"
            val response = httpConnection.upload(url, packageSpecJson.encodeToByteArray(), buildAuthHeaders())
            
            if (response.isSuccessful()) SyncResult.SYNC_OK else SyncResult.SYNC_ERROR
        } catch (e: Exception) {
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun syncParadata(direction: SyncDirection): SyncResult {
        if (!connected) return SyncResult.SYNC_ERROR
        
        return try {
            val url = "$serverUrl/api/sync/paradata"
            val body = """{"direction": "${direction.name}"}"""
            val response = httpConnection.request("POST", url, body.encodeToByteArray(), buildAuthHeaders())
            
            if (response.isSuccessful()) SyncResult.SYNC_OK else SyncResult.SYNC_ERROR
        } catch (e: Exception) {
            SyncResult.SYNC_ERROR
        }
    }
    
    actual suspend fun syncMessage(messageKey: String, messageValue: String): String? {
        if (!connected) return null
        
        return try {
            val url = "$serverUrl/api/sync/message"
            val body = """{"key": "$messageKey", "value": "$messageValue"}"""
            val response = httpConnection.request("POST", url, body.encodeToByteArray(), buildAuthHeaders())
            
            if (response.isSuccessful()) {
                parseJsonString(response.bodyAsString() ?: "{}", "value")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun setListener(listener: SyncListener?) {
        this.listener = listener
        httpConnection.setListener(listener)
        ftpConnection?.setListener(listener)
    }
    
    // Helper methods
    
    private fun buildAuthHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "X-Device-Id" to deviceId
        )
        if (authToken != null) {
            headers["Authorization"] = "Bearer $authToken"
        }
        return headers
    }
    
    private fun parseJsonString(json: String, key: String): String? {
        return try {
            val parsed = jsonParse(json)
            getStringProperty(parsed, key)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseDictionariesJson(json: String): List<DictionaryInfo> {
        return try {
            val result = mutableListOf<DictionaryInfo>()
            val parsed = jsonParse(json)
            val length = getArrayLength(parsed)
            
            for (i in 0 until length) {
                val item = getArrayItem(parsed, i)
                result.add(DictionaryInfo(
                    name = getStringProperty(item, "name") ?: "",
                    label = getStringProperty(item, "label") ?: ""
                ))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseApplicationsJson(json: String): List<ApplicationPackage> {
        return try {
            val result = mutableListOf<ApplicationPackage>()
            val parsed = jsonParse(json)
            val length = getArrayLength(parsed)
            
            for (i in 0 until length) {
                val item = getArrayItem(parsed, i)
                result.add(ApplicationPackage(
                    name = getStringProperty(item, "name") ?: "",
                    description = getStringProperty(item, "description"),
                    buildTime = getNumberProperty(item, "buildTime")?.toLong(),
                    signature = getStringProperty(item, "signature")
                ))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun encodeURIComponent(value: String): String {
        return jsEncodeURIComponent(value)
    }
}
