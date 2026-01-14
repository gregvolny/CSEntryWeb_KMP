package gov.census.cspro.engine.functions

import gov.census.cspro.camera.BarcodeFormat
import gov.census.cspro.camera.BarcodeResult
import gov.census.cspro.camera.BarcodeScanner
import gov.census.cspro.camera.BarcodeScannerCallback
import kotlinx.coroutines.CompletableDeferred
import kotlin.js.JsAny

/**
 * Top-level JsFun declarations for QR code functionality
 */

@JsFun("() => { return typeof QRCode !== 'undefined'; }")
private external fun isQRCodeLibraryAvailable(): Boolean

@JsFun("() => { return document.createElement('canvas'); }")
private external fun createCanvas(): JsAny

@JsFun("(canvas, format) => { return canvas.toDataURL(format); }")
private external fun canvasToDataURL(canvas: JsAny, format: String): String

/**
 * Barcode Read Engine Function
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/engine/functions/BarcodeReadFunction.java
 */
class BarcodeReadFunction(
    private val message: String = "",
    private val formats: Int = BarcodeFormat.ALL_FORMATS
) {
    private val scanner = BarcodeScanner()
    
    /**
     * Execute barcode scanning
     * @return The scanned barcode value, or empty string if cancelled/failed
     */
    suspend fun execute(): String {
        if (!scanner.isSupported()) {
            println("[BarcodeReadFunction] Barcode scanning not supported")
            return ""
        }
        
        val result = CompletableDeferred<String>()
        
        scanner.startScanning(
            formats = formats,
            message = message,
            callback = object : BarcodeScannerCallback {
                override fun onBarcodeDetected(barcode: BarcodeResult) {
                    println("[BarcodeReadFunction] Barcode detected: ${barcode.value}")
                    result.complete(barcode.value)
                }
                
                override fun onScanCancelled() {
                    println("[BarcodeReadFunction] Scan cancelled")
                    result.complete("")
                }
                
                override fun onScanError(error: String) {
                    println("[BarcodeReadFunction] Scan error: $error")
                    result.complete("")
                }
            }
        )
        
        return result.await()
    }
    
    /**
     * Check if barcode scanning is available
     */
    fun isAvailable(): Boolean {
        return scanner.isSupported()
    }
    
    /**
     * Cancel ongoing scan
     */
    fun cancel() {
        scanner.stopScanning()
    }
}

/**
 * QR Code Creator Function
 * Mirrors QR code creation from CSPro engine
 */
class QRCodeFunction(
    private val value: String,
    private val outputPath: String? = null,
    private val options: QRCodeOptions = QRCodeOptions()
) {
    data class QRCodeOptions(
        val errorCorrection: Int = 1, // L=0, M=1, Q=2, H=3
        val scale: Int = 4,
        val quietZone: Int = 4,
        val darkColor: String = "#000000",
        val lightColor: String = "#FFFFFF"
    )
    
    /**
     * Create a QR code
     * @return Path to the created QR code image, or data URL on failure/no path
     */
    suspend fun execute(): String {
        return try {
            // Use qrcode library if available
            if (!isQRLibraryAvailable()) {
                println("[QRCodeFunction] QR code library not available")
                return ""
            }
            
            val canvas = createQRCodeCanvas(value, options)
            
            if (outputPath != null) {
                // Save to file system (OPFS) - currently not implemented
                // For now, return data URL
                canvasToDataURL(canvas, "image/png")
            } else {
                // Return data URL
                canvasToDataURL(canvas, "image/png")
            }
        } catch (e: Exception) {
            println("[QRCodeFunction] Error creating QR code: ${e.message}")
            ""
        }
    }
    
    private fun isQRLibraryAvailable(): Boolean {
        return try {
            isQRCodeLibraryAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createQRCodeCanvas(text: String, options: QRCodeOptions): JsAny {
        // Create a simple canvas - actual QR code generation would require
        // integrating a QR code library
        return createCanvas()
    }
}
