package gov.census.cspro.location

import gov.census.cspro.platform.LocationData
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * WASM/Web implementation of GpsReader
 * Uses the Web Geolocation API
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/location/GpsReader.java
 */

// Top-level JS interop functions for WASM compatibility
@JsFun("() => navigator.geolocation !== null && navigator.geolocation !== undefined")
private external fun jsIsGeolocationAvailable(): Boolean

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

// External type declarations for Geolocation API
external interface JsGeolocationPosition : JsAny {
    val coords: JsGeolocationCoordinates
    val timestamp: Double
}

external interface JsGeolocationCoordinates : JsAny {
    val latitude: Double
    val longitude: Double
    val altitude: Double?
    val accuracy: Double
}

external interface JsGeolocationError : JsAny {
    val code: Int
    val message: String
}

external interface JsGeolocation : JsAny

external interface JsNavigator : JsAny {
    val geolocation: JsGeolocation?
}

@JsFun("() => navigator")
private external fun jsGetNavigator(): JsNavigator

@JsFun("(success, error) => navigator.geolocation.getCurrentPosition(success, error, { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 })")
private external fun jsGetCurrentPosition(
    success: (JsGeolocationPosition) -> Unit,
    error: (JsGeolocationError) -> Unit
)

@JsFun("(success, error) => navigator.geolocation.watchPosition(success, error, { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 })")
private external fun jsWatchPosition(
    success: (JsGeolocationPosition) -> Unit,
    error: (JsGeolocationError) -> Unit
): Int

@JsFun("(id) => navigator.geolocation.clearWatch(id)")
private external fun jsClearWatch(id: Int)

actual class GpsReader {
    private var isRunningState = false
    private var watchId: Int? = null
    private var lastLocation: GpsReading? = null
    private var mostAccurateLocation: GpsReading? = null
    private var hasReceivedValue = false
    private var hasReceivedNewValue = false
    
    companion object {
        private const val UPDATE_INTERVAL_MS = 200
        private const val HIGH_ACCURACY_TIMEOUT_MS = 10000
    }
    
    actual fun isRunning(): Boolean = isRunningState
    
    actual suspend fun start(enableListener: GpsEnableListener) {
        if (isRunningState) {
            enableListener.onSuccess()
            return
        }
        
        hasReceivedValue = false
        hasReceivedNewValue = false
        mostAccurateLocation = null
        
        try {
            // Check if geolocation is available
            if (!isGeolocationAvailable()) {
                println("[GpsReader] Geolocation not available")
                enableListener.onFailure()
                return
            }
            
            // Check permissions
            val permissionGranted = checkPermission()
            if (!permissionGranted) {
                println("[GpsReader] Geolocation permission denied")
                enableListener.onFailure()
                return
            }
            
            // Start watching position
            startWatchingPosition()
            
            isRunningState = true
            println("[GpsReader] GPS started successfully")
            enableListener.onSuccess()
            
        } catch (e: Exception) {
            println("[GpsReader] Error starting GPS: ${e.message}")
            enableListener.onFailure()
        }
    }
    
    private fun isGeolocationAvailable(): Boolean {
        return try {
            jsIsGeolocationAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun checkPermission(): Boolean {
        return try {
            // Try to get a single position to trigger permission request
            suspendCancellableCoroutine { cont ->
                jsGetCurrentPosition(
                    success = { position ->
                        // Permission granted, process initial position
                        processPosition(position)
                        cont.resume(true)
                    },
                    error = { error ->
                        val errorCode = error.code
                        if (errorCode == 1) { // PERMISSION_DENIED
                            cont.resume(false)
                        } else {
                            // Other errors (timeout, etc.) - permission may still be granted
                            cont.resume(true)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            println("[GpsReader] Permission check error: ${e.message}")
            false
        }
    }
    
    private fun startWatchingPosition() {
        watchId = jsWatchPosition(
            success = { position -> processPosition(position) },
            error = { error -> handleError(error) }
        )
        
        println("[GpsReader] Watch started with ID: $watchId")
    }
    
    private fun processPosition(position: JsGeolocationPosition) {
        try {
            val coords = position.coords
            val timestamp = position.timestamp.toLong()
            
            val reading = GpsReading(
                latitude = coords.latitude,
                longitude = coords.longitude,
                altitude = coords.altitude ?: -1.0,
                satellites = 0, // Not available in Web API
                accuracy = coords.accuracy.toFloat(),
                readTime = timestamp
            )
            
            hasReceivedValue = true
            hasReceivedNewValue = true
            lastLocation = reading
            
            // Update most accurate if better
            if (isMoreAccurate(reading, mostAccurateLocation)) {
                mostAccurateLocation = reading
                println("[GpsReader] New most accurate: ${reading.accuracy}m")
            }
            
            println("[GpsReader] Position update: ${reading.latitude}, ${reading.longitude}, accuracy: ${reading.accuracy}m")
            
        } catch (e: Exception) {
            println("[GpsReader] Error processing position: ${e.message}")
        }
    }
    
    private fun handleError(error: JsGeolocationError) {
        val code = error.code
        val message = error.message
        println("[GpsReader] Geolocation error $code: $message")
    }
    
    private fun isMoreAccurate(a: GpsReading?, b: GpsReading?): Boolean {
        if (a == null) return false
        if (b == null) return true
        if (a.accuracy < 0) return false
        if (b.accuracy < 0) return true
        return a.accuracy < b.accuracy
    }
    
    actual fun stop() {
        println("[GpsReader] Stopping GPS...")
        
        watchId?.let { id ->
            try {
                jsClearWatch(id)
            } catch (e: Exception) {
                println("[GpsReader] Error clearing watch: ${e.message}")
            }
        }
        
        watchId = null
        isRunningState = false
        println("[GpsReader] GPS stopped")
    }
    
    actual fun readLast(): String {
        return lastLocation?.toGpsString() ?: ""
    }
    
    actual fun hasNewGPSReading(desiredAccuracy: Int): Boolean {
        if (!hasReceivedNewValue) return false
        
        if (desiredAccuracy == 0) {
            return true
        }
        
        val location = mostAccurateLocation ?: return false
        return location.accuracy >= 0 && location.accuracy <= desiredAccuracy
    }
    
    actual fun readMostAccurateGPS(): String {
        if (!hasReceivedValue) return ""
        
        // Reset the "new" flag when reading
        hasReceivedNewValue = false
        
        return mostAccurateLocation?.toGpsString() ?: ""
    }
    
    actual fun onSettingsResult() {
        // On web, there's no settings activity to return from
        // This is primarily for Android compatibility
        println("[GpsReader] Settings result received (no-op on web)")
    }
    
    private fun currentTimeMillis(): Long = jsDateNow().toLong()
}
