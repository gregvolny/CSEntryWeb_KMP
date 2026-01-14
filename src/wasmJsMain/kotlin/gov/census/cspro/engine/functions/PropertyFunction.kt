package gov.census.cspro.engine.functions

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.js.JsAny

/**
 * Top-level JS interop functions for property access
 */
@JsFun("() => { return Intl.DateTimeFormat().resolvedOptions().timeZone || ''; }")
private external fun getTimeZoneJs(): String

@JsFun("() => { return navigator.onLine; }")
private external fun isOnlineJs(): Boolean

@JsFun("() => { return 'geolocation' in navigator; }")
private external fun isGpsAvailableJs(): Boolean

@JsFun("() => { return 'mediaDevices' in navigator && 'getUserMedia' in navigator.mediaDevices; }")
private external fun isCameraAvailableJs(): Boolean

@JsFun("() => { try { return crypto.randomUUID(); } catch(e) { return null; } }")
private external fun randomUUID(): String?

@JsFun("() => { var conn = navigator.connection || navigator.mozConnection || navigator.webkitConnection; return conn ? (conn.effectiveType || 'unknown') : 'unknown'; }")
private external fun getEffectiveNetworkType(): String

@JsFun("(url, target) => { window.open(url, target); }")
private external fun windowOpen(url: String, target: String)

@JsFun("(title, text, url) => { try { if (navigator.share) { navigator.share({ title: title, text: text, url: url }); return true; } return false; } catch(e) { return false; } }")
private external fun navigatorShare(title: String, text: String, url: String?): Boolean

@JsFun("() => { return navigator.share !== undefined; }")
private external fun canShare(): Boolean

/**
 * Property Function - handles system and application properties
 * Ported from Android PropertyFunction.java
 */
class PropertyFunction {
    
    companion object {
        private const val STORAGE_PREFIX = "cspro:property:"
        
        // Property keys matching Android
        const val PROPERTY_DEVICE_ID = "DeviceId"
        const val PROPERTY_DEVICE_NAME = "DeviceName"
        const val PROPERTY_OS_NAME = "OperatingSystem"
        const val PROPERTY_OS_VERSION = "OperatingSystemVersion"
        const val PROPERTY_CSPRO_VERSION = "CSProVersion"
        const val PROPERTY_SCREEN_WIDTH = "ScreenWidth"
        const val PROPERTY_SCREEN_HEIGHT = "ScreenHeight"
        const val PROPERTY_LANGUAGE = "Language"
        const val PROPERTY_COUNTRY = "Country"
        const val PROPERTY_TIMEZONE = "TimeZone"
        const val PROPERTY_NETWORK_CONNECTED = "NetworkConnected"
        const val PROPERTY_NETWORK_TYPE = "NetworkType"
        const val PROPERTY_BATTERY_LEVEL = "BatteryLevel"
        const val PROPERTY_BATTERY_CHARGING = "BatteryCharging"
        const val PROPERTY_GPS_AVAILABLE = "GpsAvailable"
        const val PROPERTY_CAMERA_AVAILABLE = "CameraAvailable"
        
        /**
         * Get a system property value
         */
        fun getProperty(propertyName: String): String {
            return when (propertyName.lowercase()) {
                PROPERTY_DEVICE_ID.lowercase() -> getDeviceId()
                PROPERTY_DEVICE_NAME.lowercase() -> getDeviceName()
                PROPERTY_OS_NAME.lowercase() -> "Web"
                PROPERTY_OS_VERSION.lowercase() -> getBrowserVersion()
                PROPERTY_CSPRO_VERSION.lowercase() -> "8.0.0"
                PROPERTY_SCREEN_WIDTH.lowercase() -> window.screen.width.toString()
                PROPERTY_SCREEN_HEIGHT.lowercase() -> window.screen.height.toString()
                PROPERTY_LANGUAGE.lowercase() -> getLanguage()
                PROPERTY_COUNTRY.lowercase() -> getCountry()
                PROPERTY_TIMEZONE.lowercase() -> getTimeZone()
                PROPERTY_NETWORK_CONNECTED.lowercase() -> if (isOnline()) "1" else "0"
                PROPERTY_NETWORK_TYPE.lowercase() -> getNetworkType()
                PROPERTY_BATTERY_LEVEL.lowercase() -> "-1" // Not reliably available
                PROPERTY_BATTERY_CHARGING.lowercase() -> "" // Not reliably available
                PROPERTY_GPS_AVAILABLE.lowercase() -> if (isGpsAvailable()) "1" else "0"
                PROPERTY_CAMERA_AVAILABLE.lowercase() -> if (isCameraAvailable()) "1" else "0"
                else -> getStoredProperty(propertyName) ?: ""
            }
        }
        
        /**
         * Set a custom property value
         */
        fun setProperty(propertyName: String, value: String): Boolean {
            return try {
                localStorage.setItem("$STORAGE_PREFIX$propertyName", value)
                true
            } catch (e: Exception) {
                println("[PropertyFunction] Error setting property: ${e.message}")
                false
            }
        }
        
        /**
         * Get a stored custom property
         */
        private fun getStoredProperty(propertyName: String): String? {
            return try {
                localStorage.getItem("$STORAGE_PREFIX$propertyName")
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Get or generate device ID
         */
        private fun getDeviceId(): String {
            val storedId = getStoredProperty(PROPERTY_DEVICE_ID)
            if (storedId != null) return storedId
            
            val newId = generateUUID()
            setProperty(PROPERTY_DEVICE_ID, newId)
            return newId
        }
        
        /**
         * Get device name from user agent
         */
        private fun getDeviceName(): String {
            val userAgent = window.navigator.userAgent
            return when {
                userAgent.contains("Chrome") && !userAgent.contains("Edge") -> "Chrome Browser"
                userAgent.contains("Firefox") -> "Firefox Browser"
                userAgent.contains("Safari") && !userAgent.contains("Chrome") -> "Safari Browser"
                userAgent.contains("Edge") -> "Edge Browser"
                userAgent.contains("Opera") || userAgent.contains("OPR") -> "Opera Browser"
                else -> "Web Browser"
            }
        }
        
        /**
         * Get browser version
         */
        private fun getBrowserVersion(): String {
            val userAgent = window.navigator.userAgent
            
            // Try to extract version number
            val patterns = listOf(
                Regex("Chrome/([\\d.]+)"),
                Regex("Firefox/([\\d.]+)"),
                Regex("Safari/([\\d.]+)"),
                Regex("Edge/([\\d.]+)"),
                Regex("Edg/([\\d.]+)")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(userAgent)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
            
            return "Unknown"
        }
        
        /**
         * Get user language
         */
        private fun getLanguage(): String {
            val lang = window.navigator.language
            return lang.split("-").firstOrNull() ?: "en"
        }
        
        /**
         * Get user country
         */
        private fun getCountry(): String {
            val lang = window.navigator.language
            val parts = lang.split("-")
            return if (parts.size > 1) parts[1].uppercase() else ""
        }
        
        /**
         * Get timezone
         */
        private fun getTimeZone(): String {
            return try {
                getTimeZoneJs()
            } catch (e: Exception) {
                ""
            }
        }
        
        /**
         * Check if online
         */
        private fun isOnline(): Boolean {
            return try {
                isOnlineJs()
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Get network type
         */
        private fun getNetworkType(): String {
            return try {
                if (isOnline()) getEffectiveNetworkType() else "none"
            } catch (e: Exception) {
                "unknown"
            }
        }
        
        /**
         * Check if GPS is available
         */
        private fun isGpsAvailable(): Boolean {
            return try {
                isGpsAvailableJs()
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Check if camera is available
         */
        private fun isCameraAvailable(): Boolean {
            return try {
                isCameraAvailableJs()
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Generate UUID
         */
        private fun generateUUID(): String {
            return try {
                randomUUID() ?: generateFallbackUUID()
            } catch (e: Exception) {
                generateFallbackUUID()
            }
        }
        
        private fun generateFallbackUUID(): String {
            return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".map { c ->
                when (c) {
                    'x' -> "0123456789abcdef".random()
                    'y' -> "89ab".random()
                    else -> c
                }
            }.joinToString("")
        }
    }
}

/**
 * ExecSystem Function - executes system commands (limited on web)
 * Ported from Android ExecSystemFunction.kt
 */
class ExecSystemFunction {
    
    companion object {
        /**
         * Execute a "system" command
         * On web, this is very limited - mainly for opening URLs
         */
        fun exec(command: String): Int {
            return when {
                command.startsWith("http://") || command.startsWith("https://") -> {
                    openUrl(command)
                    0 // Success
                }
                command.startsWith("mailto:") -> {
                    openUrl(command)
                    0
                }
                command.startsWith("tel:") -> {
                    openUrl(command)
                    0
                }
                else -> {
                    println("[ExecSystem] Command not supported on web: $command")
                    -1 // Not supported
                }
            }
        }
        
        private fun openUrl(url: String) {
            windowOpen(url, "_blank")
        }
    }
}

/**
 * System App Engine Function - handles system app interactions
 * Ported from Android SystemAppEngineFunction.kt
 */
class SystemAppEngineFunction {
    
    companion object {
        /**
         * Open a file with the system handler
         * On web, this triggers a download
         */
        fun openFile(path: String, mimeType: String?): Boolean {
            return try {
                // On web, we can only really handle URLs
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    windowOpen(path, "_blank")
                    true
                } else {
                    println("[SystemApp] Cannot open local file: $path")
                    false
                }
            } catch (e: Exception) {
                println("[SystemApp] Error opening file: ${e.message}")
                false
            }
        }
        
        /**
         * Share content
         * Uses Web Share API if available
         */
        suspend fun share(title: String, text: String, url: String?): Boolean {
            return try {
                if (canShare()) {
                    navigatorShare(title, text, url)
                } else {
                    println("[SystemApp] Web Share API not available")
                    false
                }
            } catch (e: Exception) {
                println("[SystemApp] Error sharing: ${e.message}")
                false
            }
        }
    }
}
