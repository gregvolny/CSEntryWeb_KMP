package gov.census.cspro.camera

import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

/**
 * WASM/Web implementation of BarcodeScanner
 * 
 * This is a simplified stub implementation for WASM compatibility.
 * The actual barcode scanning is delegated to JavaScript for better compatibility.
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/camera/BarcodeCaptureActivity.kt
 */

// External JS functions for barcode scanning
@JsFun("() => typeof BarcodeDetector !== 'undefined'")
private external fun isBarcodeDetectorAvailable(): Boolean

@JsFun("() => navigator.mediaDevices != null")
private external fun hasMediaDevices(): Boolean

@JsFun("(message) => { console.log('[BarcodeScanner] ' + message); }")
private external fun logMessage(message: String)

/**
 * Barcode Scanner for Web/WASM
 * 
 * Note: Full implementation requires JavaScript bridge due to WASM limitations
 * with dynamic types and complex async callbacks
 */
actual class BarcodeScanner {
    private var isScanning = false
    private var overlayElement: HTMLDivElement? = null
    
    /**
     * Start scanning for barcodes using the camera
     * 
     * Due to WASM limitations with complex async callbacks and dynamic types,
     * this implementation shows a message and falls back to manual entry.
     */
    actual suspend fun startScanning(
        formats: Int,
        message: String?,
        callback: BarcodeScannerCallback
    ) {
        if (isScanning) {
            stopScanning()
        }
        
        // Check if BarcodeDetector API is available
        if (!isBarcodeDetectorAvailable()) {
            callback.onScanError("Barcode scanning is not supported in this browser. Please use Chrome 83+ or Edge 83+.")
            return
        }
        
        // Check if camera is available
        if (!hasMediaDevices()) {
            callback.onScanError("Camera access is not available.")
            return
        }
        
        isScanning = true
        
        // For full barcode scanning, delegate to JavaScript implementation
        // This Kotlin code provides the interface, actual scanning done in JS
        logMessage("Barcode scanning initiated. Message: ${message ?: "none"}")
        
        // Show a message to the user
        showScannerUnavailableMessage(message, callback)
    }
    
    private fun showScannerUnavailableMessage(message: String?, callback: BarcodeScannerCallback) {
        // Create overlay to show message
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "barcode-scanner-overlay"
        overlay.innerHTML = """
            <div style="
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.9);
                z-index: 10000;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                color: white;
                font-family: sans-serif;
            ">
                <div style="font-size: 24px; margin-bottom: 20px;">Barcode Scanner</div>
                <div style="font-size: 16px; margin-bottom: 30px; text-align: center; padding: 0 20px;">
                    ${message ?: "Point your camera at a barcode"}
                </div>
                <div style="font-size: 14px; color: #aaa; margin-bottom: 30px; text-align: center; padding: 0 20px;">
                    Note: Full barcode scanning requires browser support.<br>
                    Please manually enter the barcode value.
                </div>
                <input type="text" id="barcode-manual-input" placeholder="Enter barcode value" style="
                    padding: 15px 20px;
                    font-size: 18px;
                    border: 2px solid white;
                    border-radius: 8px;
                    background: transparent;
                    color: white;
                    width: 80%;
                    max-width: 300px;
                    text-align: center;
                    margin-bottom: 20px;
                ">
                <div style="display: flex; gap: 20px;">
                    <button id="barcode-submit-btn" style="
                        padding: 12px 30px;
                        font-size: 16px;
                        border: none;
                        border-radius: 5px;
                        background: #4CAF50;
                        color: white;
                        cursor: pointer;
                    ">Submit</button>
                    <button id="barcode-cancel-btn" style="
                        padding: 12px 30px;
                        font-size: 16px;
                        border: none;
                        border-radius: 5px;
                        background: #ff4444;
                        color: white;
                        cursor: pointer;
                    ">Cancel</button>
                </div>
            </div>
        """.trimIndent()
        
        document.body?.appendChild(overlay)
        overlayElement = overlay
        
        // Set up button handlers via JavaScript
        setupButtonHandlers(callback)
    }
    
    private fun setupButtonHandlers(callback: BarcodeScannerCallback) {
        val submitBtn = document.getElementById("barcode-submit-btn")
        val cancelBtn = document.getElementById("barcode-cancel-btn")
        val input = document.getElementById("barcode-manual-input")
        
        submitBtn?.addEventListener("click", { _ ->
            val inputElement = document.getElementById("barcode-manual-input")
            val value = inputElement?.getAttribute("value") ?: 
                        (inputElement as? org.w3c.dom.HTMLInputElement)?.value ?: ""
            
            if (value.isNotBlank()) {
                stopScanning()
                callback.onBarcodeDetected(BarcodeResult(
                    value = value,
                    format = BarcodeFormat.ALL_FORMATS,
                    displayValue = value,
                    rawValue = value
                ))
            }
            null
        })
        
        cancelBtn?.addEventListener("click", { _ ->
            stopScanning()
            callback.onScanError("Cancelled by user")
            null
        })
        
        // Focus the input field
        (input as? org.w3c.dom.HTMLInputElement)?.focus()
    }
    
    actual fun stopScanning() {
        isScanning = false
        
        // Remove overlay
        overlayElement?.remove()
        overlayElement = null
        
        // Also try to remove by ID in case reference was lost
        document.getElementById("barcode-scanner-overlay")?.remove()
    }
    
    actual fun isCameraAvailable(): Boolean {
        return try {
            hasMediaDevices()
        } catch (e: Exception) {
            false
        }
    }
    
    actual fun isSupported(): Boolean {
        return isCameraAvailable() && isBarcodeDetectorAvailable()
    }
}
