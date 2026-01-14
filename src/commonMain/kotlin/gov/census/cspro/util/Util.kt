package gov.census.cspro.util

/**
 * Utility functions for file path manipulation and application discovery
 * Ported from Android Util.java
 */
object Util {
    
    /**
     * Combine two path segments with proper separator
     */
    fun combinePath(p1: String, p2: String): String {
        if (p1.isEmpty()) return p2
        if (p2.isEmpty()) return p1
        
        val separator = "/"
        val path1 = p1.trimEnd('/', '\\')
        val path2 = p2.trimStart('/', '\\')
        
        return "$path1$separator$path2"
    }
    
    /**
     * Strip filename from path leaving only directory.
     * @param file Path from which to remove filename
     * @return Path with filename removed
     */
    fun removeFilename(file: String): String {
        var slashPos = file.lastIndexOf('/')
        
        // Handle path with trailing /
        if (slashPos == file.length - 1) {
            if (slashPos != 0) {
                slashPos = file.substring(0, file.length - 1).lastIndexOf('/')
            }
        }
        
        return if (slashPos != -1) {
            file.substring(0, slashPos + 1)
        } else {
            file
        }
    }
    
    /**
     * Remove directory from path leaving only filename
     */
    fun removeDirectory(path: String): String {
        val slashPos = path.lastIndexOf('/')
        
        return if (slashPos != -1) {
            path.substring(slashPos + 1)
        } else {
            path
        }
    }
    
    /**
     * Check if string is null or empty
     */
    fun stringIsNullOrEmpty(t: String?): Boolean {
        return t == null || t.isEmpty()
    }
    
    /**
     * Check if string is null, empty, or whitespace
     */
    fun stringIsNullOrEmptyTrim(t: String?): Boolean {
        return t == null || t.trim().isEmpty()
    }
    
    /**
     * Pad string on left with specified character
     */
    fun padLeft(stringWidth: Int, padChar: Char, inputString: String): String {
        if (inputString.length >= stringWidth) {
            return inputString
        }
        
        val padLength = stringWidth - inputString.length
        return padChar.toString().repeat(padLength) + inputString
    }
}
