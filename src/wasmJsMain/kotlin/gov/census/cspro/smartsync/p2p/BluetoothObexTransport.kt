package gov.census.cspro.smartsync.p2p

/**
 * WASM/Web implementation of BluetoothObexTransport
 * 
 * Note: Web Bluetooth has significant limitations compared to Android Bluetooth.
 * OBEX protocol is not directly supported in browsers, and Web Bluetooth API
 * requires user gesture and HTTPS, with limited device/service support.
 * 
 * This is a stub implementation that logs appropriate messages when Bluetooth
 * operations are attempted. Full Bluetooth OBEX support would require:
 * - Web Bluetooth API (Chrome only, limited support)
 * - Custom GATT service implementation
 * - User gesture for device pairing
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/p2p/BluetoothObexTransport.java
 */
actual class BluetoothObexTransport actual constructor(socket: BluetoothSocket) {
    private val socket = socket
    private var closed = false
    
    // Buffer for received data (stub - not actually used)
    private val receiveBuffer = mutableListOf<Byte>()
    
    actual fun close() {
        if (!closed) {
            closed = true
            socket.close()
            receiveBuffer.clear()
            println("[BluetoothObexTransport] Closed")
        }
    }
    
    actual suspend fun write(data: ByteArray) {
        if (closed) {
            throw Exception("Transport is closed")
        }
        
        println("[BluetoothObexTransport] Write requested for ${data.size} bytes")
        println("[BluetoothObexTransport] WARNING: Bluetooth OBEX not supported in browser environment")
        
        // In a real implementation, this would use Web Bluetooth GATT
        // For now, throw an exception indicating lack of support
        throw Exception("Bluetooth OBEX transport is not supported in the browser. Use network sync instead.")
    }
    
    actual suspend fun read(buffer: ByteArray, byteCount: Int, timeoutMs: Int): Int {
        if (closed) {
            throw Exception("Transport is closed")
        }
        
        println("[BluetoothObexTransport] Read requested for up to $byteCount bytes with ${timeoutMs}ms timeout")
        println("[BluetoothObexTransport] WARNING: Bluetooth OBEX not supported in browser environment")
        
        // In a real implementation, this would use Web Bluetooth GATT
        // For now, throw an exception indicating lack of support
        throw Exception("Bluetooth OBEX transport is not supported in the browser. Use network sync instead.")
    }
    
    companion object {
        /**
         * Check if Bluetooth is available in the current browser environment
         */
        fun isSupported(): Boolean {
            // Web Bluetooth is only available in secure contexts (HTTPS) and 
            // only in certain browsers (primarily Chrome)
            println("[BluetoothObexTransport] Checking Bluetooth support...")
            println("[BluetoothObexTransport] Web Bluetooth has limited browser support")
            return false // Stub: report as not supported
        }
    }
}

/**
 * WASM/Web implementation of BluetoothSocket
 * 
 * This is a stub implementation. Web Bluetooth sockets would need to wrap
 * a BluetoothDevice and BluetoothRemoteGATTServer from the Web Bluetooth API.
 */
actual class BluetoothSocket(
    private val deviceId: String = "unknown",
    private val deviceName: String = "Unknown Device"
) {
    private var closed = false
    private var connected = false
    
    actual fun isConnected(): Boolean {
        return !closed && connected
    }
    
    actual fun close() {
        if (!closed) {
            closed = true
            connected = false
            println("[BluetoothSocket] Closed - device: $deviceId")
        }
    }
    
    actual fun getRemoteAddress(): String {
        return deviceId
    }
    
    /**
     * Get the device name
     */
    fun getDeviceName(): String {
        return deviceName
    }
    
    companion object {
        /**
         * Create a stub socket for testing or when Bluetooth is not available
         */
        fun createStub(deviceId: String = "stub-device", deviceName: String = "Stub Device"): BluetoothSocket {
            println("[BluetoothSocket] Creating stub socket - Bluetooth not supported in browser")
            return BluetoothSocket(deviceId, deviceName)
        }
    }
}
