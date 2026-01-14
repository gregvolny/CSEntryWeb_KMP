package gov.census.cspro.engine.functions

import gov.census.cspro.location.GpsEnableListener
import gov.census.cspro.location.GpsReader
import gov.census.cspro.location.GpsReading
import gov.census.cspro.maps.BaseMapSelection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.js.JsAny

/**
 * Top-level JS functions for GPS
 */
@JsFun("() => { return Date.now(); }")
private external fun dateNow(): Double

/**
 * GPS Engine Function
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/engine/functions/GPSFunction.java
 */
class GPSFunction(
    private val command: Int,
    private val waitTime: Int = 0,
    private val desiredAccuracy: Int = 0,
    private val dialogText: String = "",
    private val baseMapSelection: BaseMapSelection = BaseMapSelection()
) {
    companion object {
        const val GPS_OPEN = 1
        const val GPS_CLOSE = 2
        const val GPS_READ = 3
        const val GPS_READLAST = 4
        const val GPS_READINTERACTIVE = 5
        const val GPS_SELECT = 6
        
        const val DIALOG_TYPE_INTERACTIVE = 0
        const val DIALOG_TYPE_SELECT = 1
        
        private val gpsReader = GpsReader()
        
        /**
         * Get the shared GPS reader instance
         */
        fun getReader(): GpsReader = gpsReader
    }
    
    /**
     * Execute the GPS function
     * @return The result string (GPS reading, "1" for success, "0" for failure, or "" for no reading)
     */
    suspend fun execute(): String {
        return when (command) {
            GPS_OPEN -> open()
            GPS_CLOSE -> close()
            GPS_READ -> read()
            GPS_READLAST -> readLast()
            GPS_READINTERACTIVE -> readInteractive()
            GPS_SELECT -> select()
            else -> ""
        }
    }
    
    private suspend fun open(): String {
        if (gpsReader.isRunning()) {
            return "1"
        }
        
        val result = CompletableDeferred<Boolean>()
        
        gpsReader.start(object : GpsEnableListener {
            override fun onSuccess() {
                result.complete(true)
            }
            
            override fun onFailure() {
                result.complete(false)
            }
        })
        
        return if (result.await()) "1" else "0"
    }
    
    private fun close(): String {
        gpsReader.stop()
        return "1"
    }
    
    private fun readLast(): String {
        return gpsReader.readLast()
    }
    
    private suspend fun read(): String {
        if (!gpsReader.isRunning()) {
            return ""
        }
        
        // Wait for GPS reading with timeout
        val startTime = currentTimeMillis()
        val timeoutMs = if (waitTime > 0) waitTime * 1000L else 60000L
        
        while (currentTimeMillis() - startTime < timeoutMs) {
            if (gpsReader.hasNewGPSReading(desiredAccuracy)) {
                return gpsReader.readMostAccurateGPS()
            }
            delay(200)
        }
        
        // Timeout - return best reading we have
        return gpsReader.readMostAccurateGPS()
    }
    
    private suspend fun readInteractive(): String {
        // On web, interactive mode shows a dialog with map (if available)
        // For now, fall back to regular read
        return read()
    }
    
    private suspend fun select(): String {
        // Select mode allows user to pick location on map
        // For now, fall back to regular read
        return read()
    }
    
    private fun currentTimeMillis(): Long {
        return dateNow().toLong()
    }
}

/**
 * GPS utilities for location string parsing
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/location/LocationUtils.java
 */
object LocationUtils {
    /**
     * Parse GPS string to individual components
     */
    fun parseGpsString(gpsString: String): GpsReading? {
        return GpsReading.fromGpsString(gpsString)
    }
    
    /**
     * Get latitude from GPS string
     */
    fun getLatitude(gpsString: String): Double {
        return parseGpsString(gpsString)?.latitude ?: Double.NaN
    }
    
    /**
     * Get longitude from GPS string
     */
    fun getLongitude(gpsString: String): Double {
        return parseGpsString(gpsString)?.longitude ?: Double.NaN
    }
    
    /**
     * Get altitude from GPS string
     */
    fun getAltitude(gpsString: String): Double {
        return parseGpsString(gpsString)?.altitude ?: Double.NaN
    }
    
    /**
     * Get accuracy from GPS string
     */
    fun getAccuracy(gpsString: String): Float {
        return parseGpsString(gpsString)?.accuracy ?: Float.NaN
    }
    
    /**
     * Get satellites from GPS string
     */
    fun getSatellites(gpsString: String): Int {
        return parseGpsString(gpsString)?.satellites ?: 0
    }
    
    /**
     * Calculate distance between two points (Haversine formula)
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
}

// Math utility for web
private object Math {
    fun toRadians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0
}
