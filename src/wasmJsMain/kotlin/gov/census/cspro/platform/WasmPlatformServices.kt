package gov.census.cspro.platform

import gov.census.cspro.camera.BarcodeFormat
import gov.census.cspro.camera.BarcodeResult
import gov.census.cspro.camera.BarcodeScanner
import gov.census.cspro.camera.BarcodeScannerCallback
import gov.census.cspro.export.ExportFormat
import gov.census.cspro.maps.MapCameraPosition
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import kotlin.js.Promise
import kotlin.js.unsafeCast

/**
 * WASM implementation of platform services using Web APIs
 * Uses OPFS (Origin Private File System) for file operations
 * 
 * Comprehensive implementation of platform services for:
 * - GPS/Geolocation (Web Geolocation API)
 * - Camera/Barcode (MediaDevices API + BarcodeDetector)
 * - Maps (Leaflet.js integration)
 * - Bluetooth (Web Bluetooth API)
 * - File System (OPFS)
 * - Export (Blob download)
 */

// External declarations for global objects
@JsName("localStorage")
external val windowLocalStorage: Storage

@JsName("navigator.storage")
external val navigatorStorage: StorageManager

@JsName("navigator")
external val windowNavigator: JsNavigator

// Local Storage API (for credentials only)
external interface Storage : kotlin.js.JsAny {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
    fun clear()
}

// Top-level @JsFun helpers for creating options objects (Kotlin/Wasm compatible)
@JsFun("(create) => ({ create: create })")
private external fun jsCreateFileSystemGetFileOptions(create: Boolean): FileSystemGetFileOptions

@JsFun("() => ({ enableHighAccuracy: true, timeout: 10000, maximumAge: 0 })")
private external fun jsCreateGeolocationOptions(): GeolocationOptions

@JsFun("() => ({ acceptAllDevices: true })")
private external fun jsCreateBluetoothRequestOptions(): BluetoothRequestDeviceOptions

@JsFun("() => navigator.onLine")
private external fun jsGetOnlineStatus(): Boolean

@JsFun("(str) => str")
private external fun stringToJsAny(str: String): kotlin.js.JsAny

@JsFun("() => typeof L !== 'undefined'")
private external fun jsIsLeafletAvailable(): Boolean

@JsFun("() => crypto.randomUUID()")
private external fun jsGenerateUUID(): String

@JsFun("(data, mimeType) => new Blob([data], { type: mimeType })")
private external fun jsCreateBlob(data: kotlin.js.JsAny, mimeType: String): kotlin.js.JsAny

@JsFun("(blob) => URL.createObjectURL(blob)")
private external fun jsCreateObjectURL(blob: kotlin.js.JsAny): String

@JsFun("(url) => URL.revokeObjectURL(url)")
private external fun jsRevokeObjectURL(url: String)

@JsFun("(length) => new Uint8Array(length)")
private external fun jsCreateUint8Array(length: Int): kotlin.js.JsAny

@JsFun("(arr, index, value) => { arr[index] = value; }")
private external fun jsSetArrayValue(arr: kotlin.js.JsAny, index: Int, value: Byte)

@JsFun("() => navigator.mediaDevices != null")
private external fun jsHasMediaDevices(): Boolean

@JsFun("(navigator) => navigator.connection")
private external fun jsGetConnection(navigator: kotlin.js.JsAny): kotlin.js.JsAny?

@JsFun("(connection) => connection.type")
private external fun jsGetConnectionType(connection: kotlin.js.JsAny): String?

@JsFun("(navigator) => navigator.userAgent")
private external fun jsGetUserAgent(navigator: kotlin.js.JsAny): String

@JsFun("(dir) => dir.entries()")
private external fun jsGetDirectoryEntries(dir: kotlin.js.JsAny): kotlin.js.JsAny

@JsFun("(entries) => entries.next()")
private external fun jsIteratorNext(entries: kotlin.js.JsAny): Promise<kotlin.js.JsAny>

@JsFun("(result) => result.done")
private external fun jsIteratorDone(result: kotlin.js.JsAny): Boolean

@JsFun("(result) => result.value[0]")
private external fun jsIteratorValue(result: kotlin.js.JsAny): String

@JsFun("(dir, name) => dir.removeEntry(name)")
private external fun jsRemoveEntry(dir: kotlin.js.JsAny, name: String): Promise<kotlin.js.JsAny?>

@JsFun("(link, url) => setTimeout(() => { link.remove(); URL.revokeObjectURL(url); }, 100)")
private external fun jsScheduleCleanup(link: org.w3c.dom.HTMLAnchorElement, url: String)

/**
 * WASM implementation of platform services using Web APIs
 * Uses OPFS (Origin Private File System) for file operations
 */
class WasmPlatformServices : IPlatformServices {
    
    private val localStorage: Storage get() = windowLocalStorage
    private var opfsRoot: FileSystemDirectoryHandle? = null
    
    /**
     * Initialize OPFS root directory
     */
    private suspend fun getOPFSRoot(): FileSystemDirectoryHandle {
        if (opfsRoot == null) {
            opfsRoot = navigatorStorage.getDirectory().await()
            println("[WasmPlatform] OPFS initialized")
        }
        return opfsRoot!!
    }
    
    /**
     * Get or create directory path in OPFS
     */
    private suspend fun getDirectoryHandle(path: String, create: Boolean = true): FileSystemDirectoryHandle {
        var currentDir = getOPFSRoot()
        val parts = path.split("/").filter { it.isNotEmpty() }
        
        for (i in 0 until parts.size - 1) {
            val options = jsCreateFileSystemGetFileOptions(create)
            currentDir = currentDir.getDirectoryHandle(parts[i], options).await<FileSystemDirectoryHandle>()
        }
        
        return currentDir
    }
    
    override suspend fun getLocation(): LocationData? {
        return try {
            val navigator = windowNavigator
            val position = suspendCoroutinePromise<GeolocationPosition> { resolve, reject ->
                val options = jsCreateGeolocationOptions()
                navigator.geolocation.getCurrentPosition(
                    successCallback = { position -> resolve(position) },
                    errorCallback = { error -> reject(Exception(error.message)) },
                    options = options
                )
            }
            
            LocationData(
                latitude = position.coords.latitude,
                longitude = position.coords.longitude,
                accuracy = position.coords.accuracy.toFloat(),
                timestamp = position.timestamp.toLong()
            )
        } catch (e: Exception) {
            println("[WasmPlatform] Error getting location:" + e)
            null
        }
    }
    
    override suspend fun readFile(path: String): String? {
        return try {
            println("[WasmPlatform] Reading file from OPFS: $path")
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            
            val fileName = parts.last()
            val dir = getDirectoryHandle(path, create = false)
            
            // Get file handle
            val options = jsCreateFileSystemGetFileOptions(false)
            val fileHandle = dir.getFileHandle(fileName, options).await<FileSystemFileHandle>()
            val file: JsFile = fileHandle.getFile().await<JsFile>()
            val content: JsString = file.text().await<kotlin.js.JsString>()
            val result = content.toString()
            
            println("[WasmPlatform] File read successfully: ${result.length} bytes")
            result
        } catch (e: Exception) {
            println("[WasmPlatform] File not found in OPFS: $path")
            null
        }
    }
    
    override suspend fun writeFile(path: String, content: String): Boolean {
        return try {
            println("[WasmPlatform] Writing file to OPFS: $path (${content.length} bytes)")
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            val fileName = parts.last()
            val dir = getDirectoryHandle(path, create = true)
            
            // Get or create file handle
            val options = jsCreateFileSystemGetFileOptions(true)
            val fileHandle = dir.getFileHandle(fileName, options).await<FileSystemFileHandle>()
            
            // Create writable stream and write content
            val writable = fileHandle.createWritable().await<FileSystemWritableFileStream>()
            val jsContent = stringToJsAny(content)
            writable.write(jsContent).await<kotlin.js.JsAny>()
            writable.close().await<kotlin.js.JsAny>()
            
            println("[WasmPlatform] File written successfully")
            true
        } catch (e: Exception) {
            println("[WasmPlatform] Error writing file:" + e)
            false
        }
    }
    
    override suspend fun fileExists(path: String): Boolean {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            val fileName = parts.last()
            val dir = getDirectoryHandle(path, create = false)
            
            // Try to get file handle without creating
            val options = jsCreateFileSystemGetFileOptions(false)
            dir.getFileHandle(fileName, options).await<FileSystemFileHandle>()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isNetworkConnected(): Boolean {
        return try {
            jsGetOnlineStatus()
        } catch (e: Exception) {
            println("[WasmPlatform] Error checking network:" + e)
            false
        }
    }
    
    override suspend fun scanBluetoothDevices(): List<BluetoothDevice>? {
        return try {
            println("[WasmPlatform] Scanning Bluetooth devices...")
            
            // Check if Web Bluetooth is available
            val bluetooth = windowNavigator.bluetooth ?: run {
                println("[WasmPlatform] Web Bluetooth not available")
                return null
            }
            
            // Request device with basic services
            val options = jsCreateBluetoothRequestOptions()
            val device = (bluetooth.requestDevice(options).await() as? BluetoothRemoteGATTDevice) ?: run {
                println("[WasmPlatform] No device selected")
                return null
            }
            
            listOf(
                BluetoothDevice(
                    id = device.id,
                    name = device.name ?: "Unknown Device",
                    address = device.id
                )
            )
        } catch (e: Exception) {
            println("[WasmPlatform] Error scanning Bluetooth:" + e)
            null
        }
    }
    
    override suspend fun connectBluetoothDevice(deviceId: String): Boolean {
        return try {
            println("[WasmPlatform] Connecting to Bluetooth device: $deviceId")
            // Implementation would involve GATT server connection
            false
        } catch (e: Exception) {
            println("[WasmPlatform] Error connecting Bluetooth:" + e)
            false
        }
    }
    
    override suspend fun storeCredential(key: String, value: String): Boolean {
        return try {
            localStorage.setItem("credential:$key", value)
            true
        } catch (e: Exception) {
            println("[WasmPlatform] Error storing credential:" + e)
            false
        }
    }
    
    override suspend fun retrieveCredential(key: String): String? {
        return try {
            localStorage.getItem("credential:$key")
        } catch (e: Exception) {
            println("[WasmPlatform] Error retrieving credential:" + e)
            null
        }
    }
    
    // ============================================================
    // New IPlatformServices implementations
    // ============================================================
    
    private var locationWatchId: Int? = null
    private var locationListener: LocationListener? = null
    
    override fun isGpsAvailable(): Boolean {
        return try {
            windowNavigator.geolocation != null
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun startLocationUpdates(listener: LocationListener) {
        stopLocationUpdates()
        locationListener = listener
        
        try {
            val options = jsCreateGeolocationOptions()
            locationWatchId = windowNavigator.geolocation.watchPosition(
                successCallback = { position ->
                    val data = LocationData(
                        latitude = position.coords.latitude,
                        longitude = position.coords.longitude,
                        altitude = position.coords.altitude ?: 0.0,
                        accuracy = position.coords.accuracy.toFloat(),
                        timestamp = position.timestamp.toLong()
                    )
                    locationListener?.onLocationChanged(data)
                },
                errorCallback = { error ->
                    locationListener?.onLocationError(error.message ?: "Unknown error")
                },
                options = options
            ) as? Int
        } catch (e: Exception) {
            listener.onLocationError("Failed to start location updates: ${e.message}")
        }
    }
    
    override fun stopLocationUpdates() {
        locationWatchId?.let { id ->
            try {
                windowNavigator.geolocation.clearWatch(id)
            } catch (e: Exception) {
                println("[WasmPlatform] Error stopping location updates: ${e.message}")
            }
        }
        locationWatchId = null
        locationListener = null
    }
    
    override fun isCameraAvailable(): Boolean {
        return try {
            jsHasMediaDevices()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isBarcodeSupported(): Boolean {
        return BarcodeScanner().isSupported()
    }
    
    override suspend fun scanBarcode(formats: Int, message: String?): BarcodeResult? {
        val scanner = BarcodeScanner()
        if (!scanner.isSupported()) {
            return null
        }
        
        val result = CompletableDeferred<BarcodeResult?>()
        
        scanner.startScanning(
            formats = if (formats == 0) BarcodeFormat.ALL_FORMATS else formats,
            message = message,
            callback = object : BarcodeScannerCallback {
                override fun onBarcodeDetected(barcode: BarcodeResult) {
                    result.complete(barcode)
                }
                
                override fun onScanCancelled() {
                    result.complete(null)
                }
                
                override fun onScanError(error: String) {
                    println("[WasmPlatform] Barcode scan error: $error")
                    result.complete(null)
                }
            }
        )
        
        return result.await()
    }
    
    override suspend fun capturePhoto(outputPath: String?): String? {
        // Photo capture would use MediaDevices API
        // Simplified implementation
        return try {
            println("[WasmPlatform] Photo capture not fully implemented")
            null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isMappingAvailable(): Boolean {
        // Check if Leaflet is loaded
        return try {
            jsIsLeafletAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getMapCameraPosition(): MapCameraPosition? {
        // Would need to get from current map instance
        return null
    }
    
    override suspend fun deleteFile(path: String): Boolean {
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            val fileName = parts.last()
            val dir = getDirectoryHandle(path, create = false)
            
            jsRemoveEntry(dir.unsafeCast<kotlin.js.JsAny>(), fileName).await<kotlin.js.JsAny?>()
            true
        } catch (e: Exception) {
            println("[WasmPlatform] Error deleting file: ${e.message}")
            false
        }
    }
    
    override suspend fun listFiles(path: String): List<String> {
        return try {
            val dir = if (path.isEmpty()) {
                getOPFSRoot()
            } else {
                getDirectoryHandle("$path/dummy", create = false)
            }
            
            val files = mutableListOf<String>()
            val entries = jsGetDirectoryEntries(dir.unsafeCast<kotlin.js.JsAny>())
            
            // Iterate over directory entries
            while (true) {
                val next = jsIteratorNext(entries).await<kotlin.js.JsAny>()
                if (jsIteratorDone(next)) break
                val entryName = jsIteratorValue(next)
                files.add(entryName)
            }
            
            files
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun getNetworkType(): NetworkType {
        return try {
            val connection = jsGetConnection(windowNavigator.unsafeCast<kotlin.js.JsAny>())
            if (connection == null) {
                if (isNetworkConnected()) NetworkType.UNKNOWN else NetworkType.NONE
            } else {
                when (jsGetConnectionType(connection)) {
                    "wifi" -> NetworkType.WIFI
                    "cellular" -> NetworkType.CELLULAR
                    "ethernet" -> NetworkType.ETHERNET
                    "none" -> NetworkType.NONE
                    else -> NetworkType.UNKNOWN
                }
            }
        } catch (e: Exception) {
            NetworkType.UNKNOWN
        }
    }
    
    override fun isBluetoothAvailable(): Boolean {
        return try {
            windowNavigator.bluetooth != null
        } catch (e: Exception) {
            false
        }
    }
    
    override fun disconnectBluetooth() {
        // Would need to track connected device
        println("[WasmPlatform] Bluetooth disconnect (no-op)")
    }
    
    override fun getAvailableExportFormats(): List<ExportFormat> {
        return listOf(
            ExportFormat.COMMA_DELIMITED,
            ExportFormat.TAB_DELIMITED,
            ExportFormat.SEMICOLON_DELIMITED,
            ExportFormat.JSON
        )
    }
    
    override suspend fun downloadFile(data: ByteArray, filename: String, mimeType: String): Boolean {
        return try {
            // Create Uint8Array and copy data
            val uint8Array = jsCreateUint8Array(data.size)
            for (i in data.indices) {
                jsSetArrayValue(uint8Array, i, data[i])
            }
            
            val blob = jsCreateBlob(uint8Array, mimeType)
            val url = jsCreateObjectURL(blob)
            
            val link = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
            link.href = url
            link.download = filename
            link.style.display = "none"
            
            document.body?.appendChild(link)
            link.click()
            
            // Schedule cleanup after download
            jsScheduleCleanup(link, url)
            
            true
        } catch (e: Exception) {
            println("[WasmPlatform] Error downloading file: ${e.message}")
            false
        }
    }
    
    override suspend fun deleteCredential(key: String): Boolean {
        return try {
            localStorage.removeItem("credential:$key")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getDeviceId(): String {
        // Generate or retrieve a persistent device ID
        return try {
            var deviceId = localStorage.getItem("cspro:deviceId")
            if (deviceId == null) {
                deviceId = generateUUID()
                localStorage.setItem("cspro:deviceId", deviceId)
            }
            deviceId
        } catch (e: Exception) {
            generateUUID()
        }
    }
    
    override fun getDeviceName(): String {
        return try {
            val userAgent = jsGetUserAgent(windowNavigator.unsafeCast<kotlin.js.JsAny>())
            when {
                userAgent.contains("Chrome") -> "Chrome Browser"
                userAgent.contains("Firefox") -> "Firefox Browser"
                userAgent.contains("Safari") -> "Safari Browser"
                userAgent.contains("Edge") -> "Edge Browser"
                else -> "Web Browser"
            }
        } catch (e: Exception) {
            "Web Browser"
        }
    }
    
    override fun getPlatformName(): String = "Web"
    
    private fun generateUUID(): String {
        return jsGenerateUUID()
    }
}

// Helper for promise-based coroutines
private suspend fun <T> suspendCoroutinePromise(
    block: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit
): T = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
    block(
        { value -> cont.resumeWith(Result.success(value)) },
        { error -> cont.resumeWith(Result.failure(error)) }
    )
}



