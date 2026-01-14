package gov.census.cspro.smartsync

/**
 * Deployment package downloader interface
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/addapp/DeploymentPackageDownloader.kt
 */

/**
 * Interface for downloading and managing deployment packages
 */
expect class DeploymentPackageDownloader() {
    /**
     * Connect to a deployment server
     * @param serverUrl Server URL
     * @return Result code (resultOk, resultError, or resultCanceled)
     */
    suspend fun connectToServer(serverUrl: String): Int
    
    /**
     * Connect to a deployment server with credentials
     * @param serverUrl Server URL
     * @param applicationName Optional application name
     * @param credentials Optional credentials
     * @return Result code
     */
    suspend fun connectToServerCredentials(
        serverUrl: String,
        applicationName: String?,
        credentials: String?
    ): Int
    
    /**
     * List available deployment packages
     * @return List of available packages
     */
    suspend fun listPackages(): List<DeploymentPackage>
    
    /**
     * Install a deployment package
     * @param packageName Name of the package to install
     * @param forceFullUpdate Force a full update even if partial update is possible
     * @return Result code
     */
    suspend fun installPackage(packageName: String, forceFullUpdate: Boolean): Int
    
    /**
     * List packages that have updates available
     * @return List of updatable packages
     */
    suspend fun listUpdatablePackages(): List<DeploymentPackage>
    
    /**
     * Update a deployment package
     * @param pkg Package to update
     * @return Result code
     */
    suspend fun updatePackage(pkg: DeploymentPackage): Int
    
    /**
     * Disconnect from the server
     */
    fun disconnect()
    
    companion object {
        val resultOk: Int
        val resultError: Int
        val resultCanceled: Int
    }
}
