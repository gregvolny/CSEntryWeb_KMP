package gov.census.cspro.smartsync

/**
 * Application deployment package
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/addapp/DeploymentPackage.kt
 */
data class DeploymentPackage(
    var name: String? = null,
    var description: String? = null,
    var buildTime: Long? = null,          // Unix timestamp in milliseconds
    var currentInstalledBuildTime: Long? = null, // Unix timestamp in milliseconds
    var serverUrl: String? = null,
    var deploymentType: Int = DeploymentType_None
) {
    /**
     * Get the installation status of this package
     */
    val installStatus: Int
        get() = when {
            currentInstalledBuildTime == null -> NEW_INSTALL
            (buildTime ?: 0L) > (currentInstalledBuildTime ?: 0L) -> UPDATE_AVAILABLE
            else -> UP_TO_DATE
        }
    
    companion object {
        // Deployment types
        const val DeploymentType_None = 0
        const val DeploymentType_CSWeb = 1
        const val DeploymentType_Dropbox = 2
        const val DeploymentType_FTP = 3
        const val DeploymentType_LocalFile = 4
        const val DeploymentType_LocalFolder = 5
        
        // Install status
        const val NEW_INSTALL = 1
        const val UPDATE_AVAILABLE = 2
        const val UP_TO_DATE = 3
    }
}
