package gov.census.cspro.exception

/**
 * Exception thrown for path-related errors
 * Ported from Android PathException.java
 */
class PathException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
