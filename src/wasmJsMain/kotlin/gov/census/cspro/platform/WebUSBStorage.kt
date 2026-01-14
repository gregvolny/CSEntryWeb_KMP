package gov.census.cspro.platform

import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.JsArray
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebUSB API - External storage device access
 * 
 * Provides access to external USB storage devices (SD card readers, USB drives)
 * using the WebUSB API for:
 * - Mass Storage Class (MSC) devices
 * - SD card readers
 * - External drives
 * 
 * Mirrors: Android's MediaScanner and external storage access
 */

// ============================================================================
// Top-level @JsFun external functions for WASM compatibility
// ============================================================================

@JsFun("() => navigator.usb !== undefined")
private external fun jsNavigatorUsbAvailable(): Boolean

@JsFun("(filters) => navigator.usb.requestDevice({ filters: filters })")
private external fun jsUsbRequestDevice(filters: JsAny): JsAny // Returns Promise

@JsFun("() => navigator.usb.getDevices()")
private external fun jsUsbGetDevices(): JsAny // Returns Promise

@JsFun("(device) => device.vendorId")
private external fun jsUsbDeviceVendorId(device: JsAny): Int

@JsFun("(device) => device.productId")
private external fun jsUsbDeviceProductId(device: JsAny): Int

@JsFun("(device) => device.productName || 'USB Device'")
private external fun jsUsbDeviceProductName(device: JsAny): JsString

@JsFun("(device) => device.serialNumber")
private external fun jsUsbDeviceSerialNumber(device: JsAny): JsString?

@JsFun("(device) => device.open()")
private external fun jsUsbDeviceOpen(device: JsAny): JsAny // Returns Promise

@JsFun("(device) => device.close()")
private external fun jsUsbDeviceClose(device: JsAny): JsAny // Returns Promise

@JsFun("(device) => device.configuration")
private external fun jsUsbDeviceConfiguration(device: JsAny): JsAny?

@JsFun("(device, configValue) => device.selectConfiguration(configValue)")
private external fun jsUsbDeviceSelectConfiguration(device: JsAny, configValue: Int): JsAny // Returns Promise

@JsFun("(config) => config.interfaces")
private external fun jsUsbConfigInterfaces(config: JsAny): JsAny

@JsFun("(device, interfaceNumber) => device.claimInterface(interfaceNumber)")
private external fun jsUsbDeviceClaimInterface(device: JsAny, interfaceNumber: Int): JsAny // Returns Promise

@JsFun("(iface) => iface.alternate")
private external fun jsUsbInterfaceAlternate(iface: JsAny): JsAny

@JsFun("(alternate) => alternate.interfaceClass")
private external fun jsUsbAlternateInterfaceClass(alternate: JsAny): Int

@JsFun("(iface) => iface.interfaceNumber")
private external fun jsUsbInterfaceNumber(iface: JsAny): Int

@JsFun("() => [{ classCode: 0x08 }, { vendorId: 0x058f }, { vendorId: 0x0781 }, { vendorId: 0x0951 }, { vendorId: 0x0930 }]")
private external fun jsUsbMassStorageFilters(): JsAny

@JsFun("() => navigator.storage.getDirectory()")
private external fun jsGetStorageDirectory(): JsAny // Returns Promise

@JsFun("(dir, name, options) => dir.getDirectoryHandle(name, options)")
private external fun jsGetDirectoryHandle(dir: JsAny, name: JsString, options: JsAny): JsAny // Returns Promise

@JsFun("(dir, name, options) => dir.getFileHandle(name, options)")
private external fun jsGetFileHandle(dir: JsAny, name: JsString, options: JsAny): JsAny // Returns Promise

@JsFun("(fileHandle) => fileHandle.getFile()")
private external fun jsGetFile(fileHandle: JsAny): JsAny // Returns Promise

@JsFun("(fileHandle) => fileHandle.createWritable()")
private external fun jsCreateWritable(fileHandle: JsAny): JsAny // Returns Promise

@JsFun("(writable, data) => writable.write(data)")
private external fun jsWritableWrite(writable: JsAny, data: JsAny): JsAny // Returns Promise

@JsFun("(writable) => writable.close()")
private external fun jsWritableClose(writable: JsAny): JsAny // Returns Promise

@JsFun("(dir) => dir.entries()")
private external fun jsDirEntries(dir: JsAny): JsAny

@JsFun("(iterator) => iterator.next()")
private external fun jsIteratorNext(iterator: JsAny): JsAny // Returns Promise

@JsFun("(result) => result.done")
private external fun jsIteratorResultDone(result: JsAny): Boolean

@JsFun("(result) => result.value")
private external fun jsIteratorResultValue(result: JsAny): JsAny?

@JsFun("(entry) => entry[0]")
private external fun jsEntryName(entry: JsAny): JsString

@JsFun("(entry) => entry[1]")
private external fun jsEntryHandle(entry: JsAny): JsAny

@JsFun("(handle) => handle.kind")
private external fun jsHandleKind(handle: JsAny): JsString

@JsFun("(file) => file.size")
private external fun jsFileSize(file: JsAny): Double

@JsFun("(file) => file.lastModified")
private external fun jsFileLastModified(file: JsAny): Double

@JsFun("(file) => file.arrayBuffer()")
private external fun jsFileArrayBuffer(file: JsAny): JsAny // Returns Promise

@JsFun("(len) => new Uint8Array(len)")
private external fun jsNewUint8Array(len: Int): JsAny

@JsFun("(arrayBuffer) => new Uint8Array(arrayBuffer)")
private external fun jsUint8ArrayFromBuffer(arrayBuffer: JsAny): JsAny

@JsFun("(arr) => arr.length")
private external fun jsArrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun jsArrayGetInt(arr: JsAny, i: Int): Int

@JsFun("(arr, i) => arr[i]")
private external fun jsArrayGetAny(arr: JsAny, i: Int): JsAny?

@JsFun("(arr, i, val) => arr[i] = val")
private external fun jsArraySetByte(arr: JsAny, i: Int, value: Byte): Unit

@JsFun("() => window.showDirectoryPicker !== undefined")
private external fun jsFileSystemAccessAvailable(): Boolean

@JsFun("() => window.showDirectoryPicker({ mode: 'readwrite' })")
private external fun jsShowDirectoryPicker(): JsAny // Returns Promise

@JsFun("() => ({ create: false })")
private external fun jsCreateFalseOptions(): JsAny

@JsFun("() => ({ create: true })")
private external fun jsCreateTrueOptions(): JsAny

@JsFun("(promise, resolve, reject) => promise.then(resolve).catch(reject)")
private external fun promiseThenCatch(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?): Unit

@JsFun("(s) => s")
private external fun toJsString(s: String): JsString

@JsFun("(s) => s")
private external fun jsStringToKotlin(s: JsString): String

// Helper suspend function to await a JS Promise
private suspend fun <T : JsAny?> awaitPromise(promise: JsAny): T {
    return suspendCancellableCoroutine { continuation ->
        promiseThenCatch(
            promise,
            { result: JsAny? ->
                @Suppress("UNCHECKED_CAST")
                continuation.resume(result as T)
                null
            },
            { error: JsAny ->
                continuation.resumeWithException(Exception("JS Promise rejected: $error"))
                null
            }
        )
    }
}
object WebUSBStorage {
    
    private var connectedDevice: USBDevice? = null
    private val mediaFiles = mutableListOf<MediaFile>()
    
    // USB Mass Storage Class constants
    private const val USB_CLASS_MASS_STORAGE = 0x08
    private const val USB_SUBCLASS_SCSI = 0x06
    private const val USB_PROTOCOL_BBB = 0x50 // Bulk-Only Transport
    
    /**
     * Check if WebUSB is available
     */
    fun isAvailable(): Boolean {
        return try {
            jsNavigatorUsbAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Request access to a USB storage device
     */
    suspend fun requestDevice(): USBDevice? {
        if (!isAvailable()) {
            println("[WebUSB] WebUSB not available")
            return null
        }
        
        return try {
            println("[WebUSB] Requesting USB device...")
            
            // Filter for mass storage devices
            val filters = jsUsbMassStorageFilters()
            val usbDevicePromise = jsUsbRequestDevice(filters)
            val usbDevice: JsAny = awaitPromise(usbDevicePromise)
            
            val device = USBDevice(
                vendorId = jsUsbDeviceVendorId(usbDevice),
                productId = jsUsbDeviceProductId(usbDevice),
                productName = jsStringToKotlin(jsUsbDeviceProductName(usbDevice)),
                serialNumber = jsUsbDeviceSerialNumber(usbDevice)?.let { jsStringToKotlin(it) },
                nativeDevice = usbDevice
            )
            
            println("[WebUSB] Device selected: ${device.productName}")
            connectedDevice = device
            device
        } catch (e: Exception) {
            println("[WebUSB] Error requesting device: ${e.message}")
            null
        }
    }
    
    /**
     * Get previously authorized devices
     */
    suspend fun getDevices(): List<USBDevice> {
        if (!isAvailable()) return emptyList()
        
        return try {
            val devicesPromise = jsUsbGetDevices()
            val devices: JsAny = awaitPromise(devicesPromise)
            val result = mutableListOf<USBDevice>()
            
            val length = jsArrayLength(devices)
            for (i in 0 until length) {
                val usbDevice = jsArrayGetAny(devices, i) ?: continue
                result.add(USBDevice(
                    vendorId = jsUsbDeviceVendorId(usbDevice),
                    productId = jsUsbDeviceProductId(usbDevice),
                    productName = jsStringToKotlin(jsUsbDeviceProductName(usbDevice)),
                    serialNumber = jsUsbDeviceSerialNumber(usbDevice)?.let { jsStringToKotlin(it) },
                    nativeDevice = usbDevice
                ))
            }
            
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Connect to a USB device
     */
    suspend fun connect(device: USBDevice): Boolean {
        return try {
            val usbDevice = device.nativeDevice ?: return false
            
            // Open device
            awaitPromise<JsAny?>(jsUsbDeviceOpen(usbDevice))
            println("[WebUSB] Device opened")
            
            // Select configuration (usually 1)
            if (jsUsbDeviceConfiguration(usbDevice) == null) {
                awaitPromise<JsAny?>(jsUsbDeviceSelectConfiguration(usbDevice, 1))
            }
            
            // Claim interface for mass storage
            val config = jsUsbDeviceConfiguration(usbDevice) ?: return false
            val interfaces = jsUsbConfigInterfaces(config)
            val length = jsArrayLength(interfaces)
            
            for (i in 0 until length) {
                val iface = jsArrayGetAny(interfaces, i) ?: continue
                val alternate = jsUsbInterfaceAlternate(iface)
                if (jsUsbAlternateInterfaceClass(alternate) == USB_CLASS_MASS_STORAGE) {
                    awaitPromise<JsAny?>(jsUsbDeviceClaimInterface(usbDevice, jsUsbInterfaceNumber(iface)))
                    println("[WebUSB] Mass storage interface claimed")
                    connectedDevice = device
                    return true
                }
            }
            
            println("[WebUSB] No mass storage interface found")
            false
        } catch (e: Exception) {
            println("[WebUSB] Error connecting: ${e.message}")
            false
        }
    }
    
    /**
     * Disconnect from device
     */
    suspend fun disconnect() {
        try {
            connectedDevice?.nativeDevice?.let { device ->
                awaitPromise<JsAny?>(jsUsbDeviceClose(device))
            }
            connectedDevice = null
            println("[WebUSB] Device disconnected")
        } catch (e: Exception) {
            println("[WebUSB] Error disconnecting: ${e.message}")
        }
    }
    
    /**
     * Check if a device is connected
     */
    fun isConnected(): Boolean = connectedDevice != null
    
    /**
     * Get the connected device
     */
    fun getConnectedDevice(): USBDevice? = connectedDevice
}

/**
 * USB Device representation
 */
data class USBDevice(
    val vendorId: Int,
    val productId: Int,
    val productName: String,
    val serialNumber: String?,
    val nativeDevice: JsAny? = null
) {
    val vendorIdHex: String get() = "0x${vendorId.toString(16).padStart(4, '0')}"
    val productIdHex: String get() = "0x${productId.toString(16).padStart(4, '0')}"
}

/**
 * Media Scanner - Web implementation using WebUSB
 * 
 * Scans USB devices for media files
 * Mirrors: Android's MediaScannerConnection
 */
object MediaScanner {
    
    // Media file types
    val MEDIA_TYPES = mapOf(
        "audio" to listOf("mp3", "wav", "ogg", "m4a", "flac", "aac"),
        "video" to listOf("mp4", "webm", "mkv", "avi", "mov"),
        "image" to listOf("jpg", "jpeg", "png", "gif", "webp", "bmp"),
        "document" to listOf("pdf", "doc", "docx", "xls", "xlsx", "txt", "csv")
    )
    
    private val scannedFiles = mutableListOf<MediaFile>()
    private var scanListener: MediaScannerListener? = null
    
    /**
     * Scan OPFS for media files
     */
    suspend fun scanOPFS(path: String = "", listener: MediaScannerListener? = null): List<MediaFile> {
        scanListener = listener
        scannedFiles.clear()
        
        return try {
            val rootPromise = jsGetStorageDirectory()
            val root: JsAny = awaitPromise(rootPromise)
            
            val startDir = if (path.isEmpty()) {
                root
            } else {
                var dir = root
                for (part in path.split("/").filter { it.isNotEmpty() }) {
                    val handlePromise = jsGetDirectoryHandle(dir, toJsString(part), jsCreateFalseOptions())
                    dir = awaitPromise(handlePromise)
                }
                dir
            }
            
            scanDirectory(startDir, path)
            
            listener?.onScanComplete(scannedFiles.size)
            scannedFiles.toList()
        } catch (e: Exception) {
            println("[MediaScanner] Scan error: ${e.message}")
            listener?.onError("Scan failed: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun scanDirectory(dirHandle: JsAny, currentPath: String) {
        try {
            val iterator = jsDirEntries(dirHandle)
            
            while (true) {
                val nextPromise = jsIteratorNext(iterator)
                val next: JsAny = awaitPromise(nextPromise)
                if (jsIteratorResultDone(next)) break
                
                val entry = jsIteratorResultValue(next) ?: continue
                val name = jsStringToKotlin(jsEntryName(entry))
                val handle = jsEntryHandle(entry)
                val kind = jsStringToKotlin(jsHandleKind(handle))
                
                val entryPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                
                if (kind == "directory") {
                    // Recursively scan subdirectories
                    scanDirectory(handle, entryPath)
                } else {
                    // Check if it's a media file
                    val extension = name.substringAfterLast('.', "").lowercase()
                    val mediaType = getMediaType(extension)
                    
                    if (mediaType != null) {
                        val filePromise = jsGetFile(handle)
                        val file: JsAny = awaitPromise(filePromise)
                        val mediaFile = MediaFile(
                            path = entryPath,
                            name = name,
                            extension = extension,
                            mediaType = mediaType,
                            size = jsFileSize(file).toLong(),
                            lastModified = jsFileLastModified(file).toLong()
                        )
                        scannedFiles.add(mediaFile)
                        scanListener?.onFileScanned(mediaFile)
                    }
                }
            }
        } catch (e: Exception) {
            println("[MediaScanner] Error scanning directory: ${e.message}")
        }
    }
    
    private fun getMediaType(extension: String): String? {
        for ((type, extensions) in MEDIA_TYPES) {
            if (extension in extensions) return type
        }
        return null
    }
    
    /**
     * Scan files by path patterns
     */
    suspend fun scanFiles(
        paths: Array<String>,
        mimeTypes: Array<String>? = null,
        listener: MediaScannerListener? = null
    ): List<MediaFile> {
        val files = mutableListOf<MediaFile>()
        
        for (path in paths) {
            try {
                val rootPromise = jsGetStorageDirectory()
                val root: JsAny = awaitPromise(rootPromise)
                
                val parts = path.split("/").filter { it.isNotEmpty() }
                if (parts.isEmpty()) continue
                
                var dir = root
                for (i in 0 until parts.size - 1) {
                    val handlePromise = jsGetDirectoryHandle(dir, toJsString(parts[i]), jsCreateFalseOptions())
                    dir = awaitPromise(handlePromise)
                }
                
                val fileHandlePromise = jsGetFileHandle(dir, toJsString(parts.last()), jsCreateFalseOptions())
                val fileHandle: JsAny = awaitPromise(fileHandlePromise)
                val filePromise = jsGetFile(fileHandle)
                val file: JsAny = awaitPromise(filePromise)
                
                val extension = parts.last().substringAfterLast('.', "").lowercase()
                val mediaType = getMediaType(extension) ?: "unknown"
                
                val mediaFile = MediaFile(
                    path = path,
                    name = parts.last(),
                    extension = extension,
                    mediaType = mediaType,
                    size = jsFileSize(file).toLong(),
                    lastModified = jsFileLastModified(file).toLong()
                )
                files.add(mediaFile)
                listener?.onFileScanned(mediaFile)
            } catch (e: Exception) {
                println("[MediaScanner] Could not scan: $path")
            }
        }
        
        listener?.onScanComplete(files.size)
        return files
    }
    
    /**
     * Get file MIME type
     */
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            // Video
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            // Image
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            // Document
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            // CSPro
            "csdb" -> "application/x-cspro-database"
            "cslog" -> "application/x-cspro-log"
            "cspro" -> "application/x-cspro"
            "pff" -> "application/x-cspro-pff"
            "dcf" -> "application/x-cspro-dcf"
            else -> "application/octet-stream"
        }
    }
}

/**
 * Media file information
 */
data class MediaFile(
    val path: String,
    val name: String,
    val extension: String,
    val mediaType: String,
    val size: Long,
    val lastModified: Long
) {
    val sizeFormatted: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    
    val mimeType: String get() = MediaScanner.getMimeType(extension)
}

/**
 * Media scanner listener
 */
interface MediaScannerListener {
    fun onFileScanned(file: MediaFile)
    fun onScanComplete(totalFiles: Int)
    fun onError(message: String)
}

/**
 * External Storage Manager
 * Combines WebUSB and File System Access API for external storage
 */
object ExternalStorage {
    
    private var externalDirectoryHandle: JsAny? = null
    
    /**
     * Check if File System Access API is available
     */
    fun isFileSystemAccessAvailable(): Boolean {
        return try {
            jsFileSystemAccessAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Request access to an external directory (SD card mount point, etc.)
     */
    suspend fun requestExternalDirectory(): Boolean {
        if (!isFileSystemAccessAvailable()) {
            println("[ExternalStorage] File System Access API not available")
            return false
        }
        
        return try {
            val handlePromise = jsShowDirectoryPicker()
            externalDirectoryHandle = awaitPromise(handlePromise)
            println("[ExternalStorage] External directory selected")
            true
        } catch (e: Exception) {
            println("[ExternalStorage] Error selecting directory: ${e.message}")
            false
        }
    }
    
    /**
     * Check if external storage is available
     */
    fun isAvailable(): Boolean = externalDirectoryHandle != null
    
    /**
     * List files in external storage
     */
    suspend fun listFiles(path: String = ""): List<ExternalFile> {
        val handle = externalDirectoryHandle ?: return emptyList()
        
        return try {
            val targetDir = if (path.isEmpty()) {
                handle
            } else {
                var dir = handle
                for (part in path.split("/").filter { it.isNotEmpty() }) {
                    val handlePromise = jsGetDirectoryHandle(dir, toJsString(part), jsCreateFalseOptions())
                    dir = awaitPromise(handlePromise)
                }
                dir
            }
            
            val files = mutableListOf<ExternalFile>()
            val iterator = jsDirEntries(targetDir)
            
            while (true) {
                val nextPromise = jsIteratorNext(iterator)
                val next: JsAny = awaitPromise(nextPromise)
                if (jsIteratorResultDone(next)) break
                
                val entry = jsIteratorResultValue(next) ?: continue
                val name = jsStringToKotlin(jsEntryName(entry))
                val entryHandle = jsEntryHandle(entry)
                val kind = jsStringToKotlin(jsHandleKind(entryHandle))
                
                if (kind == "file") {
                    val filePromise = jsGetFile(entryHandle)
                    val file: JsAny = awaitPromise(filePromise)
                    files.add(ExternalFile(
                        name = name,
                        path = if (path.isEmpty()) name else "$path/$name",
                        isDirectory = false,
                        size = jsFileSize(file).toLong()
                    ))
                } else {
                    files.add(ExternalFile(
                        name = name,
                        path = if (path.isEmpty()) name else "$path/$name",
                        isDirectory = true,
                        size = 0
                    ))
                }
            }
            
            files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (e: Exception) {
            println("[ExternalStorage] Error listing files: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Read file from external storage
     */
    suspend fun readFile(path: String): ByteArray? {
        val handle = externalDirectoryHandle ?: return null
        
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            
            var dir = handle
            for (i in 0 until parts.size - 1) {
                val handlePromise = jsGetDirectoryHandle(dir, toJsString(parts[i]), jsCreateFalseOptions())
                dir = awaitPromise(handlePromise)
            }
            
            val fileHandlePromise = jsGetFileHandle(dir, toJsString(parts.last()), jsCreateFalseOptions())
            val fileHandle: JsAny = awaitPromise(fileHandlePromise)
            val filePromise = jsGetFile(fileHandle)
            val file: JsAny = awaitPromise(filePromise)
            val arrayBufferPromise = jsFileArrayBuffer(file)
            val arrayBuffer: JsAny = awaitPromise(arrayBufferPromise)
            
            // Convert ArrayBuffer to ByteArray
            val uint8Array = jsUint8ArrayFromBuffer(arrayBuffer)
            val length = jsArrayLength(uint8Array)
            val bytes = ByteArray(length)
            for (i in 0 until length) {
                bytes[i] = jsArrayGetInt(uint8Array, i).toByte()
            }
            bytes
        } catch (e: Exception) {
            println("[ExternalStorage] Error reading file: ${e.message}")
            null
        }
    }
    
    /**
     * Write file to external storage
     */
    suspend fun writeFile(path: String, data: ByteArray): Boolean {
        val handle = externalDirectoryHandle ?: return false
        
        return try {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var dir = handle
            for (i in 0 until parts.size - 1) {
                val handlePromise = jsGetDirectoryHandle(dir, toJsString(parts[i]), jsCreateTrueOptions())
                dir = awaitPromise(handlePromise)
            }
            
            val fileHandlePromise = jsGetFileHandle(dir, toJsString(parts.last()), jsCreateTrueOptions())
            val fileHandle: JsAny = awaitPromise(fileHandlePromise)
            val writablePromise = jsCreateWritable(fileHandle)
            val writable: JsAny = awaitPromise(writablePromise)
            
            // Convert ByteArray to Uint8Array
            val uint8Array = jsNewUint8Array(data.size)
            for (i in data.indices) {
                jsArraySetByte(uint8Array, i, data[i])
            }
            
            awaitPromise<JsAny?>(jsWritableWrite(writable, uint8Array))
            awaitPromise<JsAny?>(jsWritableClose(writable))
            true
        } catch (e: Exception) {
            println("[ExternalStorage] Error writing file: ${e.message}")
            false
        }
    }
    
    /**
     * Copy file from external storage to OPFS
     */
    suspend fun copyToOPFS(externalPath: String, opfsPath: String): Boolean {
        val data = readFile(externalPath) ?: return false
        
        return try {
            val rootPromise = jsGetStorageDirectory()
            val root: JsAny = awaitPromise(rootPromise)
            
            val parts = opfsPath.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                val handlePromise = jsGetDirectoryHandle(dir, toJsString(parts[i]), jsCreateTrueOptions())
                dir = awaitPromise(handlePromise)
            }
            
            val fileHandlePromise = jsGetFileHandle(dir, toJsString(parts.last()), jsCreateTrueOptions())
            val fileHandle: JsAny = awaitPromise(fileHandlePromise)
            val writablePromise = jsCreateWritable(fileHandle)
            val writable: JsAny = awaitPromise(writablePromise)
            
            val uint8Array = jsNewUint8Array(data.size)
            for (i in data.indices) {
                jsArraySetByte(uint8Array, i, data[i])
            }
            
            awaitPromise<JsAny?>(jsWritableWrite(writable, uint8Array))
            awaitPromise<JsAny?>(jsWritableClose(writable))
            true
        } catch (e: Exception) {
            println("[ExternalStorage] Error copying to OPFS: ${e.message}")
            false
        }
    }
    
    /**
     * Copy file from OPFS to external storage
     */
    suspend fun copyFromOPFS(opfsPath: String, externalPath: String): Boolean {
        return try {
            val rootPromise = jsGetStorageDirectory()
            val root: JsAny = awaitPromise(rootPromise)
            
            val parts = opfsPath.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                val handlePromise = jsGetDirectoryHandle(dir, toJsString(parts[i]), jsCreateFalseOptions())
                dir = awaitPromise(handlePromise)
            }
            
            val fileHandlePromise = jsGetFileHandle(dir, toJsString(parts.last()), jsCreateFalseOptions())
            val fileHandle: JsAny = awaitPromise(fileHandlePromise)
            val filePromise = jsGetFile(fileHandle)
            val file: JsAny = awaitPromise(filePromise)
            val arrayBufferPromise = jsFileArrayBuffer(file)
            val arrayBuffer: JsAny = awaitPromise(arrayBufferPromise)
            
            val uint8Array = jsUint8ArrayFromBuffer(arrayBuffer)
            val length = jsArrayLength(uint8Array)
            val bytes = ByteArray(length)
            for (i in 0 until length) {
                bytes[i] = jsArrayGetInt(uint8Array, i).toByte()
            }
            
            writeFile(externalPath, bytes)
        } catch (e: Exception) {
            println("[ExternalStorage] Error copying from OPFS: ${e.message}")
            false
        }
    }
}

/**
 * External file information
 */
data class ExternalFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)
