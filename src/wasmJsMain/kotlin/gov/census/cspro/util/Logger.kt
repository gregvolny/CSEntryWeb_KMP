package gov.census.cspro.util

/**
 * Logger utility for CSEntry Web
 * Sends logs to the server for persistent file storage via the JavaScript CSEntryLogger
 */
@JsFun("""(level, message, data) => {
    if (typeof window !== 'undefined' && window.logger) {
        const dataStr = data ? data : null;
        switch(level) {
            case 'DEBUG': window.logger.debug(message, dataStr); break;
            case 'WARN': window.logger.warn(message, dataStr); break;
            case 'ERROR': window.logger.error(message, dataStr); break;
            default: window.logger.info(message, dataStr);
        }
    } else {
        console.log('[' + level + ']', message, data || '');
    }
}""")
private external fun jsLog(level: String, message: String, data: String?)

@JsFun("""(category, action, details) => {
    if (typeof window !== 'undefined' && window.logger) {
        window.logger.log('INFO', '[' + category + '] ' + action, details ? JSON.parse(details) : null);
    } else {
        console.log('[' + category + ']', action, details || '');
    }
}""")
private external fun jsLogCategory(category: String, action: String, details: String?)

/**
 * CSEntry Logger singleton
 * All logs are sent to the server and written to log files in the logs/ folder
 */
object Logger {
    
    fun debug(message: String, data: Map<String, Any?>? = null) {
        jsLog("DEBUG", message, data?.toJsonString())
    }
    
    fun info(message: String, data: Map<String, Any?>? = null) {
        jsLog("INFO", message, data?.toJsonString())
    }
    
    fun warn(message: String, data: Map<String, Any?>? = null) {
        jsLog("WARN", message, data?.toJsonString())
    }
    
    fun error(message: String, data: Map<String, Any?>? = null) {
        jsLog("ERROR", message, data?.toJsonString())
    }
    
    /**
     * Log QSF-related events
     */
    fun qsf(action: String, details: Map<String, Any?>? = null) {
        jsLogCategory("QSF", action, details?.toJsonString())
    }
    
    /**
     * Log field-related events
     */
    fun field(fieldName: String, action: String, details: Map<String, Any?>? = null) {
        jsLogCategory("FIELD:$fieldName", action, details?.toJsonString())
    }
    
    /**
     * Log page-related events
     */
    fun page(action: String, details: Map<String, Any?>? = null) {
        jsLogCategory("PAGE", action, details?.toJsonString())
    }
    
    /**
     * Log engine-related events
     */
    fun engine(action: String, details: Map<String, Any?>? = null) {
        jsLogCategory("ENGINE", action, details?.toJsonString())
    }
    
    /**
     * Convert a map to JSON string for passing to JavaScript
     */
    private fun Map<String, Any?>.toJsonString(): String {
        val sb = StringBuilder("{")
        var first = true
        for ((key, value) in this) {
            if (!first) sb.append(",")
            first = false
            sb.append("\"").append(key.escapeJson()).append("\":")
            sb.append(value.toJsonValue())
        }
        sb.append("}")
        return sb.toString()
    }
    
    private fun Any?.toJsonValue(): String {
        return when (this) {
            null -> "null"
            is String -> "\"${this.escapeJson()}\""
            is Number -> this.toString()
            is Boolean -> this.toString()
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (this as Map<String, Any?>).toJsonString()
            }
            is List<*> -> {
                "[" + this.joinToString(",") { it.toJsonValue() } + "]"
            }
            else -> "\"${this.toString().escapeJson()}\""
        }
    }
    
    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
