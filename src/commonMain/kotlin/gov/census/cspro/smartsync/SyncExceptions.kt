package gov.census.cspro.smartsync

/**
 * Sync exception classes
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/SyncError.java
 *          CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/SyncCancelException.java
 *          CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/SyncLoginDeniedError.java
 */

/**
 * Base sync error exception
 */
open class SyncError : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

/**
 * Exception thrown when sync operation is cancelled
 */
class SyncCancelException : SyncError {
    constructor() : super("Sync operation cancelled")
    constructor(message: String?) : super(message)
}

/**
 * Exception thrown when login is denied
 */
class SyncLoginDeniedError : SyncError {
    constructor() : super("Login denied")
    constructor(message: String?) : super(message)
}
