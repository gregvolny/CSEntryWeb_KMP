package gov.census.cspro.platform

import gov.census.cspro.camera.BarcodeResult
import gov.census.cspro.export.ExportFormat
import gov.census.cspro.maps.MapCameraPosition

/**
 * Platform-specific services that need different implementations
 * on Android vs Web
 * 
 * Mirrors capabilities from:
 * - CSEntryDroid/app/src/main/java/gov/census/cspro/location/
 * - CSEntryDroid/app/src/main/java/gov/census/cspro/camera/
 * - CSEntryDroid/app/src/main/java/gov/census/cspro/maps/
 * - CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/
 */
interface IPlatformServices {
    // ============================================================
    // GPS/Geolocation
    // ============================================================
    
    /**
     * Get current location
     */
    suspend fun getLocation(): LocationData?
    
    /**
     * Check if GPS is available
     */
    fun isGpsAvailable(): Boolean
    
    /**
     * Start continuous location updates
     */
    suspend fun startLocationUpdates(listener: LocationListener)
    
    /**
     * Stop location updates
     */
    fun stopLocationUpdates()
    
    // ============================================================
    // Camera/Barcode
    // ============================================================
    
    /**
     * Check if camera is available
     */
    fun isCameraAvailable(): Boolean
    
    /**
     * Check if barcode scanning is supported
     */
    fun isBarcodeSupported(): Boolean
    
    /**
     * Scan for barcode
     * @param formats Barcode formats to scan for
     * @param message Optional message to display
     * @return Scanned barcode, or null if cancelled
     */
    suspend fun scanBarcode(formats: Int = 0, message: String? = null): BarcodeResult?
    
    /**
     * Take a photo
     * @param outputPath Path to save the photo
     * @return Path to saved photo, or null on failure
     */
    suspend fun capturePhoto(outputPath: String? = null): String?
    
    // ============================================================
    // Maps
    // ============================================================
    
    /**
     * Check if mapping is available
     */
    fun isMappingAvailable(): Boolean
    
    /**
     * Get current map camera position
     */
    fun getMapCameraPosition(): MapCameraPosition?
    
    // ============================================================
    // File system operations
    // ============================================================
    
    suspend fun readFile(path: String): String?
    suspend fun writeFile(path: String, content: String): Boolean
    suspend fun fileExists(path: String): Boolean
    suspend fun deleteFile(path: String): Boolean
    suspend fun listFiles(path: String): List<String>
    
    // ============================================================
    // Network connectivity
    // ============================================================
    
    fun isNetworkConnected(): Boolean
    
    /**
     * Get network type (wifi, cellular, etc.)
     */
    fun getNetworkType(): NetworkType
    
    // ============================================================
    // Bluetooth
    // ============================================================
    
    /**
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean
    
    /**
     * Scan for Bluetooth devices
     */
    suspend fun scanBluetoothDevices(): List<BluetoothDevice>?
    
    /**
     * Connect to a Bluetooth device
     */
    suspend fun connectBluetoothDevice(deviceId: String): Boolean
    
    /**
     * Disconnect from current Bluetooth device
     */
    fun disconnectBluetooth()
    
    // ============================================================
    // Export
    // ============================================================
    
    /**
     * Get available export formats
     */
    fun getAvailableExportFormats(): List<ExportFormat>
    
    /**
     * Download/save a file to user's device
     */
    suspend fun downloadFile(data: ByteArray, filename: String, mimeType: String): Boolean
    
    // ============================================================
    // Storage/Credentials
    // ============================================================
    
    suspend fun storeCredential(key: String, value: String): Boolean
    suspend fun retrieveCredential(key: String): String?
    suspend fun deleteCredential(key: String): Boolean
    
    // ============================================================
    // Device Info
    // ============================================================
    
    /**
     * Get device unique identifier
     */
    fun getDeviceId(): String
    
    /**
     * Get device name
     */
    fun getDeviceName(): String
    
    /**
     * Get platform name (Android, Web, etc.)
     */
    fun getPlatformName(): String
}

// ============================================================
// Data Classes
// ============================================================

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val satellites: Int = 0,
    val timestamp: Long
) {
    /**
     * Convert to CSPro GPS string format
     */
    fun toGpsString(): String {
        val timeStr = formatTimeFromTimestamp(timestamp)
        return "${formatDouble(latitude, 9)};${formatDouble(longitude, 9)};${formatDouble(altitude, 9)};$satellites;${formatDouble(accuracy.toDouble(), 9)};$timeStr"
    }
    
    private fun formatDouble(value: Double, decimals: Int): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        val rounded = kotlin.math.round(value * factor) / factor
        return rounded.toString()
    }
    
    private fun formatTimeFromTimestamp(timestamp: Long): String {
        // Simple time formatting - would use kotlinx-datetime in real implementation
        return "000000"
    }
}

data class BluetoothDevice(
    val id: String,
    val name: String,
    val address: String,
    val isPaired: Boolean = false,
    val rssi: Int = 0 // Signal strength
)

enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN
}

/**
 * Location update listener
 */
interface LocationListener {
    fun onLocationChanged(location: LocationData)
    fun onLocationError(error: String)
}
