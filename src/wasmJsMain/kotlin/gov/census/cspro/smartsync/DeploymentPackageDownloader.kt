package gov.census.cspro.smartsync

import gov.census.cspro.smartsync.http.HttpConnection
import kotlin.js.JsAny

// ============================================================================
// Top-level @JsFun external functions for JSON parsing
// ============================================================================

@JsFun("(jsonStr) => JSON.parse(jsonStr)")
private external fun jsonParseDeployment(jsonStr: String): JsAny

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun getDeploymentStringProperty(obj: JsAny, key: String): String?

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : null")
private external fun getDeploymentNumberProperty(obj: JsAny, key: String): Double?

@JsFun("(arr) => arr.length")
private external fun getDeploymentArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun getDeploymentArrayItem(arr: JsAny, idx: Int): JsAny

/**
 * WASM/Web implementation of DeploymentPackageDownloader
 * Uses HTTP APIs to communicate with the deployment server
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/addapp/DeploymentPackageDownloader.kt
 */

actual class DeploymentPackageDownloader {
    private val httpConnection = HttpConnection()
    private var serverUrl: String? = null
    private var credentials: String? = null
    private var connected = false
    
    actual companion object {
        actual val resultOk: Int = 1
        actual val resultError: Int = 0
        actual val resultCanceled: Int = -1
    }
    
    actual suspend fun connectToServer(serverUrl: String): Int {
        return connectToServerCredentials(serverUrl, null, null)
    }
    
    actual suspend fun connectToServerCredentials(
        serverUrl: String,
        applicationName: String?,
        credentials: String?
    ): Int {
        return try {
            println("[DeploymentPackageDownloader] Connecting to $serverUrl")
            
            this.serverUrl = serverUrl
            this.credentials = credentials
            
            // Validate connection by fetching server info
            val url = buildUrl("/api/deployments/info")
            val headers = mutableMapOf<String, String>()
            if (credentials != null) {
                headers["Authorization"] = "Bearer $credentials"
            }
            if (applicationName != null) {
                headers["X-Application-Name"] = applicationName
            }
            
            val response = httpConnection.request("GET", url, null, headers)
            
            if (response.isSuccessful()) {
                connected = true
                println("[DeploymentPackageDownloader] Connected successfully")
                resultOk
            } else if (response.statusCode == 401 || response.statusCode == 403) {
                println("[DeploymentPackageDownloader] Authentication failed")
                resultError
            } else {
                println("[DeploymentPackageDownloader] Connection failed: ${response.statusCode}")
                resultError
            }
        } catch (e: SyncCancelException) {
            println("[DeploymentPackageDownloader] Cancelled")
            resultCanceled
        } catch (e: Exception) {
            println("[DeploymentPackageDownloader] Error: ${e.message}")
            resultError
        }
    }
    
    actual suspend fun listPackages(): List<DeploymentPackage> {
        if (!connected || serverUrl == null) {
            println("[DeploymentPackageDownloader] Not connected")
            return emptyList()
        }
        
        return try {
            val url = buildUrl("/api/deployments/packages")
            val headers = buildAuthHeaders()
            
            val response = httpConnection.request("GET", url, null, headers)
            
            if (response.isSuccessful()) {
                parsePackagesJson(response.bodyAsString() ?: "[]")
            } else {
                println("[DeploymentPackageDownloader] List packages failed: ${response.statusCode}")
                emptyList()
            }
        } catch (e: Exception) {
            println("[DeploymentPackageDownloader] Error listing packages: ${e.message}")
            emptyList()
        }
    }
    
    actual suspend fun installPackage(packageName: String, forceFullUpdate: Boolean): Int {
        if (!connected || serverUrl == null) {
            println("[DeploymentPackageDownloader] Not connected")
            return resultError
        }
        
        return try {
            println("[DeploymentPackageDownloader] Installing package: $packageName")
            
            val url = buildUrl("/api/deployments/packages/$packageName/install")
            val headers = buildAuthHeaders().toMutableMap()
            headers["Content-Type"] = "application/json"
            
            val body = """{"forceFullUpdate": $forceFullUpdate}"""
            val response = httpConnection.request("POST", url, body.encodeToByteArray(), headers)
            
            if (response.isSuccessful()) {
                println("[DeploymentPackageDownloader] Package installed successfully")
                resultOk
            } else {
                println("[DeploymentPackageDownloader] Install failed: ${response.statusCode}")
                resultError
            }
        } catch (e: SyncCancelException) {
            resultCanceled
        } catch (e: Exception) {
            println("[DeploymentPackageDownloader] Error installing package: ${e.message}")
            resultError
        }
    }
    
    actual suspend fun listUpdatablePackages(): List<DeploymentPackage> {
        if (!connected || serverUrl == null) {
            println("[DeploymentPackageDownloader] Not connected")
            return emptyList()
        }
        
        return try {
            val url = buildUrl("/api/deployments/packages/updates")
            val headers = buildAuthHeaders()
            
            val response = httpConnection.request("GET", url, null, headers)
            
            if (response.isSuccessful()) {
                parsePackagesJson(response.bodyAsString() ?: "[]")
            } else {
                println("[DeploymentPackageDownloader] List updatable packages failed: ${response.statusCode}")
                emptyList()
            }
        } catch (e: Exception) {
            println("[DeploymentPackageDownloader] Error listing updatable packages: ${e.message}")
            emptyList()
        }
    }
    
    actual suspend fun updatePackage(pkg: DeploymentPackage): Int {
        if (!connected && pkg.serverUrl != null) {
            // Reconnect to package's server
            val result = connectToServer(pkg.serverUrl!!)
            if (result != resultOk) {
                return result
            }
        }
        
        return try {
            println("[DeploymentPackageDownloader] Updating package: ${pkg.name}")
            
            val url = buildUrl("/api/deployments/packages/${pkg.name}/update")
            val headers = buildAuthHeaders()
            
            val response = httpConnection.request("POST", url, null, headers)
            
            if (response.isSuccessful()) {
                println("[DeploymentPackageDownloader] Package updated successfully")
                resultOk
            } else {
                println("[DeploymentPackageDownloader] Update failed: ${response.statusCode}")
                resultError
            }
        } catch (e: SyncCancelException) {
            resultCanceled
        } catch (e: Exception) {
            println("[DeploymentPackageDownloader] Error updating package: ${e.message}")
            resultError
        }
    }
    
    actual fun disconnect() {
        connected = false
        serverUrl = null
        credentials = null
        println("[DeploymentPackageDownloader] Disconnected")
    }
    
    // Helper methods
    
    private fun buildUrl(path: String): String {
        val base = serverUrl?.trimEnd('/') ?: ""
        return "$base$path"
    }
    
    private fun buildAuthHeaders(): Map<String, String> {
        return if (credentials != null) {
            mapOf("Authorization" to "Bearer $credentials")
        } else {
            emptyMap()
        }
    }
    
    private fun parsePackagesJson(json: String): List<DeploymentPackage> {
        // Simple JSON parsing for deployment packages
        // In production, would use kotlinx.serialization
        val packages = mutableListOf<DeploymentPackage>()
        
        try {
            val parsed = jsonParseDeployment(json)
            val length = getDeploymentArrayLength(parsed)
            
            for (i in 0 until length) {
                val item = getDeploymentArrayItem(parsed, i)
                val pkg = DeploymentPackage(
                    name = getDeploymentStringProperty(item, "name"),
                    description = getDeploymentStringProperty(item, "description"),
                    buildTime = getDeploymentNumberProperty(item, "buildTime")?.toLong(),
                    currentInstalledBuildTime = getDeploymentNumberProperty(item, "currentInstalledBuildTime")?.toLong(),
                    serverUrl = getDeploymentStringProperty(item, "serverUrl") ?: serverUrl,
                    deploymentType = getDeploymentNumberProperty(item, "deploymentType")?.toInt() ?: DeploymentPackage.DeploymentType_CSWeb
                )
                packages.add(pkg)
            }
        } catch (e: Exception) {
            println("[DeploymentPackageDownloader] Error parsing JSON: ${e.message}")
        }
        
        return packages
    }
}
