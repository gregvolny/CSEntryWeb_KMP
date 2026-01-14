package gov.census.cspro.smartsync

import kotlin.js.JsAny

/**
 * WASM/Web implementation of BluetoothAdapter
 * Uses the Web Bluetooth API (stub implementation)
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/p2p/AndroidBluetoothAdapter.java
 * 
 * Note: Web Bluetooth API has limited browser support (Chrome, Edge, Opera only)
 * and requires HTTPS. This implementation provides stubs that return sensible defaults
 * while allowing for future full implementation when Web Bluetooth is more widely supported.
 */

// ============================================================================
// Top-level @JsFun external functions for Web Bluetooth API access
// ============================================================================

@JsFun("() => typeof navigator !== 'undefined' && navigator.bluetooth != null")
private external fun isWebBluetoothSupported(): Boolean

@JsFun("() => navigator.bluetooth")
private external fun getBluetoothApi(): JsAny?

@JsFun("(device) => device.id || ''")
private external fun getDeviceId(device: JsAny): String

@JsFun("(device) => device.name || 'Unknown Device'")
private external fun getDeviceName(device: JsAny): String

@JsFun("(device) => device.gatt != null")
private external fun hasGatt(device: JsAny): Boolean

@JsFun("(device) => device.gatt")
private external fun getDeviceGatt(device: JsAny): JsAny?

@JsFun("(gatt) => gatt.connected === true")
private external fun isGattConnected(gatt: JsAny): Boolean

@JsFun("(gatt) => gatt.disconnect()")
private external fun disconnectGatt(gatt: JsAny)

@JsFun("() => console.log('[BluetoothAdapter] Web Bluetooth API not fully supported in WASM mode')")
private external fun logBluetoothWarning()

/**
 * WASM/Web implementation of BluetoothAdapter
 * 
 * Since Web Bluetooth API has limited browser support and complex async/promise
 * handling that's difficult in Kotlin/WASM, this implementation provides
 * stub methods that return sensible defaults.
 * 
 * For production use, consider implementing a JavaScript bridge that handles
 * the actual Bluetooth operations and communicates with Kotlin through callbacks.
 */
actual class BluetoothAdapter {
    private var connectedDeviceId: String? = null
    private var connectedGatt: JsAny? = null
    private var isCancelled = false
    
    actual companion object {
        actual fun create(): BluetoothAdapter? {
            return try {
                if (isWebBluetoothSupported()) {
                    BluetoothAdapter()
                } else {
                    println("[BluetoothAdapter] Web Bluetooth not available")
                    logBluetoothWarning()
                    // Return a stub adapter anyway for testing purposes
                    BluetoothAdapter()
                }
            } catch (e: Exception) {
                println("[BluetoothAdapter] Error creating adapter: ${e.message}")
                // Return a stub adapter for graceful degradation
                BluetoothAdapter()
            }
        }
    }
    
    actual suspend fun enable() {
        // Web Bluetooth doesn't have an enable/disable concept
        // The browser handles Bluetooth state
        println("[BluetoothAdapter] Enable requested (no-op on web)")
    }
    
    actual fun disable() {
        // Web Bluetooth doesn't support disabling
        println("[BluetoothAdapter] Disable requested (no-op on web)")
    }
    
    actual fun isEnabled(): Boolean {
        // Web Bluetooth is "enabled" if the API is available
        return try {
            isWebBluetoothSupported()
        } catch (e: Exception) {
            false
        }
    }
    
    actual fun getName(): String {
        // Web Bluetooth doesn't expose local device name
        return "Web Browser"
    }
    
    actual suspend fun setName(bluetoothName: String): String {
        // Web Bluetooth doesn't support setting device name
        println("[BluetoothAdapter] Set name not supported on web")
        return getName()
    }
    
    actual suspend fun scanForDevices(listener: BluetoothSyncListener) {
        isCancelled = false
        
        // Web Bluetooth scanning requires user gesture and Promise handling
        // which is complex in WASM. For now, we report that no devices were found.
        // A full implementation would require a JavaScript bridge.
        
        println("[BluetoothAdapter] Scan for devices - Web Bluetooth requires user interaction")
        println("[BluetoothAdapter] Use JavaScript bridge for actual Bluetooth device scanning")
        
        if (!isWebBluetoothSupported()) {
            listener.onError("Web Bluetooth not available in this browser")
            return
        }
        
        // Report scan complete with no devices found
        // Real implementation would need JavaScript interop with Promises
        listener.onScanComplete()
    }
    
    actual suspend fun connectToDevice(deviceAddress: String, listener: BluetoothSyncListener): Boolean {
        // Web Bluetooth connection requires a device reference from requestDevice()
        // which must be called from a user gesture context
        
        println("[BluetoothAdapter] Connect to device: $deviceAddress")
        println("[BluetoothAdapter] Web Bluetooth requires user interaction for device pairing")
        
        if (!isWebBluetoothSupported()) {
            listener.onError("Web Bluetooth not available")
            return false
        }
        
        // Cannot connect without a device reference from user interaction
        listener.onError("Bluetooth connection requires user interaction. Use JavaScript bridge for full support.")
        return false
    }
    
    actual fun disconnect() {
        try {
            connectedGatt?.let { gatt ->
                disconnectGatt(gatt)
            }
            connectedGatt = null
            connectedDeviceId = null
            println("[BluetoothAdapter] Disconnected")
        } catch (e: Exception) {
            println("[BluetoothAdapter] Error disconnecting: ${e.message}")
        }
    }
    
    actual suspend fun sendData(data: ByteArray): Boolean {
        if (connectedGatt == null) {
            println("[BluetoothAdapter] Not connected - cannot send data")
            return false
        }
        
        // Web Bluetooth data transfer requires GATT characteristic write
        // which needs service/characteristic discovery
        println("[BluetoothAdapter] Send data not implemented - would send ${data.size} bytes")
        println("[BluetoothAdapter] Use JavaScript bridge for actual data transfer")
        
        return false
    }
    
    actual fun isConnected(): Boolean {
        return try {
            connectedGatt?.let { gatt ->
                isGattConnected(gatt)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    actual fun cancel() {
        isCancelled = true
        disconnect()
    }
}

/**
 * Web Bluetooth GATT Service helper
 */
object WebBluetoothServices {
    // Standard Bluetooth GATT service UUIDs
    const val BATTERY_SERVICE = "battery_service"
    const val DEVICE_INFORMATION = "device_information"
    const val GENERIC_ACCESS = "generic_access"
    const val GENERIC_ATTRIBUTE = "generic_attribute"
    
    // CSPro-specific service UUID (would need to be defined)
    const val CSPRO_SYNC_SERVICE = "cspro-sync"
}
