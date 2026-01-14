package gov.census.cspro.camera

/**
 * Common interface for barcode scanning
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/camera/BarcodeCaptureActivity.kt
 */

/**
 * Barcode formats supported (mirrors com.google.android.gms.vision.barcode.Barcode)
 */
object BarcodeFormat {
    const val ALL_FORMATS = 0
    const val CODE_128 = 1
    const val CODE_39 = 2
    const val CODE_93 = 4
    const val CODABAR = 8
    const val DATA_MATRIX = 16
    const val EAN_13 = 32
    const val EAN_8 = 64
    const val ITF = 128
    const val QR_CODE = 256
    const val UPC_A = 512
    const val UPC_E = 1024
    const val PDF417 = 2048
    const val AZTEC = 4096
}

/**
 * Result of a barcode scan
 */
data class BarcodeResult(
    val value: String,
    val format: Int,
    val displayValue: String,
    val rawValue: String? = null
)

/**
 * Barcode scanner callback interface
 */
interface BarcodeScannerCallback {
    fun onBarcodeDetected(barcode: BarcodeResult)
    fun onScanCancelled()
    fun onScanError(error: String)
}

/**
 * Platform-independent barcode scanner interface
 */
expect class BarcodeScanner() {
    /**
     * Start scanning for barcodes
     * @param formats Barcode formats to scan for (default: QR_CODE)
     * @param message Optional message to display during scanning
     * @param callback Callback for scan results
     */
    suspend fun startScanning(
        formats: Int = BarcodeFormat.QR_CODE,
        message: String? = null,
        callback: BarcodeScannerCallback
    )
    
    /**
     * Stop scanning
     */
    fun stopScanning()
    
    /**
     * Check if camera is available
     */
    fun isCameraAvailable(): Boolean
    
    /**
     * Check if barcode scanning is supported on this platform
     */
    fun isSupported(): Boolean
}
