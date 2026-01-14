package gov.census.cspro.smartsync

/**
 * Bluetooth synchronization interfaces
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/p2p/AndroidBluetoothAdapter.java
 */

/**
 * Bluetooth device information
 */
data class BluetoothDeviceInfo(
    val id: String,
    val name: String,
    val address: String,
    val isPaired: Boolean = false
)

/**
 * Bluetooth sync listener
 */
interface BluetoothSyncListener {
    fun onDeviceFound(device: BluetoothDeviceInfo)
    fun onDeviceConnected(device: BluetoothDeviceInfo)
    fun onDeviceDisconnected(device: BluetoothDeviceInfo)
    fun onDataReceived(data: ByteArray)
    fun onError(error: String)
    fun onScanComplete()
}

/**
 * Bluetooth adapter interface - platform specific implementations
 */
expect class BluetoothAdapter() {
    /**
     * Create a Bluetooth adapter if available
     * @return The adapter, or null if Bluetooth is not supported
     */
    companion object {
        fun create(): BluetoothAdapter?
    }
    
    /**
     * Enable Bluetooth
     */
    suspend fun enable()
    
    /**
     * Disable Bluetooth
     */
    fun disable()
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isEnabled(): Boolean
    
    /**
     * Get the name of this device
     */
    fun getName(): String
    
    /**
     * Set the name of this device
     * @return The new name
     */
    suspend fun setName(bluetoothName: String): String
    
    /**
     * Scan for nearby devices
     * @param listener Listener for scan results
     */
    suspend fun scanForDevices(listener: BluetoothSyncListener)
    
    /**
     * Connect to a remote device
     * @param deviceAddress The address of the device to connect to
     * @param listener Listener for connection events
     * @return true if connection was successful
     */
    suspend fun connectToDevice(deviceAddress: String, listener: BluetoothSyncListener): Boolean
    
    /**
     * Disconnect from current device
     */
    fun disconnect()
    
    /**
     * Send data to connected device
     * @param data The data to send
     * @return true if send was successful
     */
    suspend fun sendData(data: ByteArray): Boolean
    
    /**
     * Check if a device is currently connected
     */
    fun isConnected(): Boolean
    
    /**
     * Cancel ongoing operations
     */
    fun cancel()
}
