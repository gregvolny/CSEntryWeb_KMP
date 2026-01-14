package gov.census.cspro.location

import gov.census.cspro.platform.LocationData
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Common GPS Reader interface
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/location/GpsReader.java
 */

/**
 * GPS reading result with parsed components
 */
data class GpsReading(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = -1.0,
    val satellites: Int = 0,
    val accuracy: Float = -1f,
    val readTime: Long = 0L
) {
    /**
     * Convert to CSPro GPS string format
     * Format: "latitude;longitude;altitude;satellites;accuracy;HHMMSS"
     */
    fun toGpsString(): String {
        val timeStr = if (readTime > 0) {
            val date = Instant.fromEpochMilliseconds(readTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.hour.toString().padStart(2, '0')}${date.minute.toString().padStart(2, '0')}${date.second.toString().padStart(2, '0')}"
        } else {
            "000000"
        }
        
        return "${formatDouble(latitude, 9)};${formatDouble(longitude, 9)};${formatDouble(altitude, 9)};$satellites;${formatDouble(accuracy.toDouble(), 9)};$timeStr"
    }
    
    private fun formatDouble(value: Double, decimals: Int): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        val rounded = kotlin.math.round(value * factor) / factor
        return rounded.toString()
    }
    
    companion object {
        /**
         * Parse a GPS string into a GpsReading
         */
        fun fromGpsString(gpsString: String): GpsReading? {
            if (gpsString.isBlank()) return null
            
            val parts = gpsString.split(";")
            if (parts.size < 5) return null
            
            return try {
                GpsReading(
                    latitude = parts[0].toDouble(),
                    longitude = parts[1].toDouble(),
                    altitude = parts.getOrNull(2)?.toDoubleOrNull() ?: -1.0,
                    satellites = parts.getOrNull(3)?.toIntOrNull() ?: 0,
                    accuracy = parts.getOrNull(4)?.toFloatOrNull() ?: -1f,
                    readTime = 0L // Time parsing would require more logic
                )
            } catch (e: Exception) {
                null
            }
        }
        
        fun fromLocationData(location: LocationData): GpsReading {
            return GpsReading(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = -1.0, // Not available in basic LocationData
                satellites = 0,
                accuracy = location.accuracy,
                readTime = location.timestamp
            )
        }
    }
}

/**
 * GPS Reader enable/disable listener
 */
interface GpsEnableListener {
    fun onSuccess()
    fun onFailure()
}

/**
 * GPS Reader interface - platform specific implementations
 */
expect class GpsReader() {
    /**
     * Whether GPS is currently running
     */
    fun isRunning(): Boolean
    
    /**
     * Start GPS reading
     */
    suspend fun start(enableListener: GpsEnableListener)
    
    /**
     * Stop GPS reading
     */
    fun stop()
    
    /**
     * Read last known location (sync)
     */
    fun readLast(): String
    
    /**
     * Check if we have received a new reading since last check
     * @param desiredAccuracy If > 0, only returns true if accuracy is better than this
     */
    fun hasNewGPSReading(desiredAccuracy: Int = 0): Boolean
    
    /**
     * Get the most accurate reading received
     */
    fun readMostAccurateGPS(): String
    
    /**
     * Handle settings result (for enabling GPS in settings)
     */
    fun onSettingsResult()
}
