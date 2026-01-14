package gov.census.cspro.smartsync.http

import gov.census.cspro.smartsync.SyncCancelException
import gov.census.cspro.smartsync.SyncListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.Promise

/**
 * WASM/Web implementation of HttpConnection
 * Uses the Fetch API with WASM-compatible JsFun external declarations
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/smartsync/http/AndroidHttpConnection.kt
 */

// ============================================================================
// Top-level @JsFun declarations for Fetch API and JS interop
// All JS interop must be at top level for WASM compatibility
// ============================================================================

// Fetch API
@JsFun("(url, options) => fetch(url, options)")
private external fun jsFetch(url: String, options: JsAny): Promise<JsAny?>

// Object creation and manipulation
@JsFun("() => ({})")
private external fun createJsObject(): JsAny

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setObjectProperty(obj: JsAny, key: String, value: JsAny?)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setObjectStringProperty(obj: JsAny, key: String, value: String)

@JsFun("(obj, key) => obj[key]")
private external fun getObjectProperty(obj: JsAny, key: String): JsAny?

// Response status and properties
@JsFun("(response) => response.status")
private external fun getResponseStatus(response: JsAny): Int

@JsFun("(response) => response.ok")
private external fun getResponseOk(response: JsAny): Boolean

// Headers iteration
@JsFun("(response) => { const headers = {}; response.headers.forEach((v, k) => { headers[k.toLowerCase()] = v; }); return headers; }")
private external fun getResponseHeaders(response: JsAny): JsAny

@JsFun("(obj) => Object.keys(obj)")
private external fun getObjectKeys(obj: JsAny): JsAny

@JsFun("(arr) => arr.length")
private external fun getArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun getArrayElement(arr: JsAny, idx: Int): String

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : ''")
private external fun getStringProperty(obj: JsAny, key: String): String

// Response body
@JsFun("(response) => response.arrayBuffer()")
private external fun getResponseArrayBuffer(response: JsAny): Promise<JsAny?>

// ArrayBuffer to Uint8Array
@JsFun("(buffer) => new Uint8Array(buffer)")
private external fun createUint8Array(buffer: JsAny): JsAny

@JsFun("(arr) => arr.length")
private external fun getUint8ArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun getUint8ArrayElement(arr: JsAny, idx: Int): Int

// ByteArray to Uint8Array
@JsFun("(size) => new Uint8Array(size)")
private external fun createUint8ArrayOfSize(size: Int): JsAny

@JsFun("(arr, idx, value) => { arr[idx] = value; }")
private external fun setUint8ArrayElement(arr: JsAny, idx: Int, value: Int)

// Promise handling
@JsFun("(promise, resolve, reject) => promise.then(resolve).catch(reject)")
private external fun handlePromise(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?)

// Error handling
@JsFun("(error) => error.message || String(error)")
private external fun getErrorMessage(error: JsAny): String

actual class HttpConnection {
    private var listener: SyncListener? = null
    
    actual fun setListener(listener: SyncListener?) {
        this.listener = listener
    }
    
    actual suspend fun request(
        method: String,
        url: String,
        body: ByteArray?,
        headers: Map<String, String>
    ): HttpResponse {
        if (listener?.isCancelled() == true) {
            throw SyncCancelException()
        }
        
        return try {
            // Build fetch options
            val options = createJsObject()
            setObjectStringProperty(options, "method", method.uppercase())
            
            // Build headers object
            val fetchHeaders = createJsObject()
            headers.forEach { (key, value) ->
                setObjectStringProperty(fetchHeaders, key, value)
            }
            setObjectProperty(options, "headers", fetchHeaders)
            
            // Add body if present
            if (body != null) {
                val uint8Array = byteArrayToUint8Array(body)
                setObjectProperty(options, "body", uint8Array)
                listener?.let {
                    if (it.total <= 0) it.total = body.size.toLong()
                }
            }
            
            println("[HttpConnection] $method: $url")
            val response = awaitPromise(jsFetch(url, options))
                ?: return createErrorResponse("Fetch returned null")
            
            // Check for cancellation
            if (listener?.isCancelled() == true) {
                throw SyncCancelException()
            }
            
            val statusCode = getResponseStatus(response)
            val isOk = getResponseOk(response)
            println("[HttpConnection] Response status: $statusCode")
            
            // Extract headers
            val responseHeaders = mutableMapOf<String, String>()
            val headersObj = getResponseHeaders(response)
            val keys = getObjectKeys(headersObj)
            val keyCount = getArrayLength(keys)
            for (i in 0 until keyCount) {
                val key = getArrayElement(keys, i)
                val value = getStringProperty(headersObj, key)
                responseHeaders[key] = value
            }
            
            // Get body as ArrayBuffer then convert to ByteArray
            val arrayBufferPromise = getResponseArrayBuffer(response)
            val arrayBuffer = awaitPromise(arrayBufferPromise)
            val bodyBytes = if (arrayBuffer != null) {
                arrayBufferToByteArray(arrayBuffer)
            } else {
                ByteArray(0)
            }
            
            listener?.onProgress(bodyBytes.size.toLong())
            
            HttpResponse(
                statusCode = statusCode,
                headers = responseHeaders,
                body = if (isOk) bodyBytes else null,
                errorBody = if (isOk) null else bodyBytes
            )
        } catch (e: SyncCancelException) {
            throw e
        } catch (e: Exception) {
            println("[HttpConnection] Error: ${e.message}")
            createErrorResponse(e.message)
        }
    }
    
    actual suspend fun download(url: String, headers: Map<String, String>): ByteArray {
        val response = request("GET", url, null, headers)
        if (!response.isSuccessful()) {
            throw Exception("Download failed with status ${response.statusCode}: ${response.errorAsString()}")
        }
        return response.body ?: ByteArray(0)
    }
    
    actual suspend fun upload(url: String, data: ByteArray, headers: Map<String, String>): HttpResponse {
        return request("POST", url, data, headers)
    }
    
    private fun byteArrayToUint8Array(bytes: ByteArray): JsAny {
        val array = createUint8ArrayOfSize(bytes.size)
        for (i in bytes.indices) {
            setUint8ArrayElement(array, i, bytes[i].toInt() and 0xFF)
        }
        return array
    }
    
    private fun arrayBufferToByteArray(buffer: JsAny): ByteArray {
        val uint8Array = createUint8Array(buffer)
        val length = getUint8ArrayLength(uint8Array)
        return ByteArray(length) { i ->
            getUint8ArrayElement(uint8Array, i).toByte()
        }
    }
    
    private fun createErrorResponse(message: String?): HttpResponse {
        return HttpResponse(
            statusCode = 0,
            headers = emptyMap(),
            body = null,
            errorBody = message?.encodeToByteArray()
        )
    }
    
    /**
     * Await a JS Promise
     */
    private suspend fun awaitPromise(promise: Promise<JsAny?>): JsAny? = suspendCancellableCoroutine { cont ->
        handlePromise(
            promise.unsafeCast<JsAny>(),
            { result -> 
                cont.resume(result)
                null
            },
            { error -> 
                cont.resumeWithException(Exception("Promise rejected: ${getErrorMessage(error)}"))
                null
            }
        )
    }
}
