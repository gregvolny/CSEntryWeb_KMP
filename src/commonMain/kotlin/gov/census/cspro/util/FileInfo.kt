package gov.census.cspro.util

/**
 * File metadata wrapper
 * Ported from Android FileInfo.java (minus Parcelable)
 */
data class FileInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long = -1,
    val lastModified: Long = 0 // milliseconds since epoch
) {
    
    val lastModifiedTimeSeconds: Long
        get() = lastModified / 1000
    
    companion object {
        /**
         * Find a file by name in a collection
         */
        fun find(filename: String, infos: Iterable<FileInfo>): FileInfo? {
            return infos.firstOrNull { it.name == filename }
        }
    }
}
