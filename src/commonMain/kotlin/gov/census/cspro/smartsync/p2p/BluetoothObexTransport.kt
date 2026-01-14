package gov.census.cspro.smartsync.p2p

/**
 * Bluetooth OBEX Transport interface
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/p2p/BluetoothObexTransport.java
 */

/**
 * Transport layer for Bluetooth OBEX communication
 * Used for peer-to-peer sync over Bluetooth
 */
expect class BluetoothObexTransport(socket: BluetoothSocket) {
    /**
     * Close the transport connection
     */
    fun close()
    
    /**
     * Write data to the transport
     * @param data Data to write
     */
    suspend fun write(data: ByteArray)
    
    /**
     * Read data from the transport
     * @param buffer Buffer to read into
     * @param byteCount Maximum number of bytes to read
     * @param timeoutMs Timeout in milliseconds
     * @return Number of bytes read
     */
    suspend fun read(buffer: ByteArray, byteCount: Int, timeoutMs: Int): Int
}

/**
 * Bluetooth socket abstraction
 */
expect class BluetoothSocket {
    /**
     * Check if the socket is connected
     */
    fun isConnected(): Boolean
    
    /**
     * Close the socket
     */
    fun close()
    
    /**
     * Get the remote device address
     */
    fun getRemoteAddress(): String
}
