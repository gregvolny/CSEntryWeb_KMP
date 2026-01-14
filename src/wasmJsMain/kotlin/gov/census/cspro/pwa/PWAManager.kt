package gov.census.cspro.pwa

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ============================================================
// TOP-LEVEL @JsFun EXTERNAL FUNCTIONS FOR WASM INTEROP
// ============================================================

// Service Worker Support Check
@JsFun("() => 'serviceWorker' in navigator")
private external fun jsIsServiceWorkerSupported(): Boolean

// Navigator access
@JsFun("() => navigator.serviceWorker")
private external fun jsGetServiceWorkerContainer(): JsAny?

// Online status
@JsFun("() => navigator.onLine")
private external fun jsIsOnline(): Boolean

// Display mode checks
@JsFun("() => window.matchMedia('(display-mode: standalone)').matches")
private external fun jsIsStandalonePWA(): Boolean

@JsFun("() => window.navigator.standalone === true")
private external fun jsIsIOSStandalone(): Boolean

// Service Worker Registration
@JsFun("(swPath, scope) => navigator.serviceWorker.register(swPath, { scope: scope })")
private external fun jsRegisterServiceWorker(swPath: String, scope: String): JsAny

@JsFun("(registration) => registration.update()")
private external fun jsUpdateServiceWorker(registration: JsAny): JsAny

// Service Worker messaging
@JsFun("(sw, message) => { if (sw && sw.controller) sw.controller.postMessage(message); }")
private external fun jsPostMessageToSW(sw: JsAny, message: JsAny)

@JsFun("(waiting, type) => { if (waiting) waiting.postMessage({ type: type }); }")
private external fun jsPostMessageToWaiting(waiting: JsAny?, messageType: String)

// Create message objects
@JsFun("(type) => ({ type: type })")
private external fun jsCreateMessageObject(type: String): JsAny

@JsFun("(type, tag) => ({ type: type, tag: tag })")
private external fun jsCreateTagMessage(type: String, tag: String): JsAny

// Promise handling
@JsFun("(promise, resolve, reject) => promise.then(resolve).catch(reject)")
private external fun jsHandlePromise(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?)

// Property accessors
@JsFun("(obj, key) => obj[key]")
private external fun jsGetProperty(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun jsGetStringProperty(obj: JsAny, key: String): String?

@JsFun("(obj, key) => typeof obj[key] === 'boolean' ? obj[key] : false")
private external fun jsGetBoolProperty(obj: JsAny, key: String): Boolean

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : 0")
private external fun jsGetNumberProperty(obj: JsAny, key: String): Int

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun jsSetProperty(obj: JsAny, key: String, value: JsAny?)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun jsSetStringProperty(obj: JsAny, key: String, value: String)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun jsSetIntProperty(obj: JsAny, key: String, value: Int)

// Object/Array creation
@JsFun("() => ({})")
private external fun jsCreateObject(): JsAny

@JsFun("() => []")
private external fun jsCreateArray(): JsAny

@JsFun("(arr) => arr.length")
private external fun jsArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun jsArrayGet(arr: JsAny, idx: Int): JsAny?

@JsFun("(obj) => Object.keys(obj)")
private external fun jsObjectKeys(obj: JsAny): JsAny

// Notification API
@JsFun("() => Notification.requestPermission()")
private external fun jsRequestNotificationPermission(): JsAny

@JsFun("(registration, title, options) => registration.showNotification(title, options)")
private external fun jsShowNotification(registration: JsAny, title: String, options: JsAny): JsAny

// Permissions API
@JsFun("(name) => navigator.permissions.query({ name: name })")
private external fun jsQueryPermission(name: String): JsAny

// Push Manager
@JsFun("(registration) => registration.pushManager")
private external fun jsGetPushManager(registration: JsAny): JsAny?

@JsFun("(pushManager, options) => pushManager.subscribe(options)")
private external fun jsPushSubscribe(pushManager: JsAny, options: JsAny): JsAny

@JsFun("(pushManager) => pushManager.getSubscription()")
private external fun jsGetPushSubscription(pushManager: JsAny): JsAny

// Push subscription access
@JsFun("(subscription, keyType) => subscription.getKey(keyType)")
private external fun jsGetSubscriptionKey(subscription: JsAny, keyType: String): JsAny?

// Sync API
@JsFun("(registration) => registration.sync")
private external fun jsGetSync(registration: JsAny): JsAny?

@JsFun("(sync, tag) => sync.register(tag)")
private external fun jsSyncRegister(sync: JsAny, tag: String): JsAny

// Periodic Sync API
@JsFun("(registration) => registration.periodicSync")
private external fun jsGetPeriodicSync(registration: JsAny): JsAny?

@JsFun("(periodicSync, tag, minInterval) => periodicSync.register(tag, { minInterval: minInterval })")
private external fun jsPeriodicSyncRegister(periodicSync: JsAny, tag: String, minInterval: Double): JsAny

@JsFun("(periodicSync, tag) => periodicSync.unregister(tag)")
private external fun jsPeriodicSyncUnregister(periodicSync: JsAny, tag: String): JsAny

// Base64 utilities
@JsFun("(str) => atob(str)")
private external fun jsAtob(str: String): String

@JsFun("(str) => btoa(str)")
private external fun jsBtoa(str: String): String

// Uint8Array operations
@JsFun("(length) => new Uint8Array(length)")
private external fun jsCreateUint8Array(length: Int): JsAny

@JsFun("(arr, idx, value) => { arr[idx] = value; }")
private external fun jsSetUint8ArrayElement(arr: JsAny, idx: Int, value: Int)

@JsFun("(arr) => arr.length")
private external fun jsUint8ArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun jsGetUint8ArrayElement(arr: JsAny, idx: Int): Int

@JsFun("(buffer) => new Uint8Array(buffer)")
private external fun jsUint8ArrayFromBuffer(buffer: JsAny): JsAny

// JSON
@JsFun("(data) => JSON.stringify(data)")
private external fun jsJsonStringify(data: JsAny): String

@JsFun("(str) => JSON.parse(str)")
private external fun jsJsonParse(str: String): JsAny

// Message Channel
@JsFun("() => new MessageChannel()")
private external fun jsCreateMessageChannel(): JsAny

@JsFun("(channel) => channel.port1")
private external fun jsGetPort1(channel: JsAny): JsAny

@JsFun("(channel) => channel.port2")
private external fun jsGetPort2(channel: JsAny): JsAny

@JsFun("(port, handler) => { port.onmessage = handler; }")
private external fun jsSetOnMessage(port: JsAny, handler: (JsAny) -> Unit)

@JsFun("(sw, message, ports) => { if (sw && sw.controller) sw.controller.postMessage(message, ports); }")
private external fun jsPostMessageWithPorts(sw: JsAny, message: JsAny, ports: JsAny)

// Event handling
@JsFun("(event) => event.data")
private external fun jsGetEventData(event: JsAny): JsAny?

@JsFun("(event) => event.preventDefault()")
private external fun jsPreventDefault(event: JsAny)

// Install prompt
@JsFun("(prompt) => prompt.prompt()")
private external fun jsShowInstallPrompt(prompt: JsAny)

@JsFun("(prompt) => prompt.userChoice")
private external fun jsGetUserChoice(prompt: JsAny): JsAny

// Logging
@JsFun("(msg) => console.log('[PWA] ' + msg)")
private external fun jsLogInfo(msg: String)

@JsFun("(msg) => console.error('[PWA] ' + msg)")
private external fun jsLogError(msg: String)

// OPFS Storage access
@JsFun("() => typeof navigator !== 'undefined' && navigator.storage && typeof navigator.storage.getDirectory === 'function'")
private external fun jsIsOpfsAvailable(): Boolean

@JsFun("() => navigator.storage.getDirectory()")
private external fun jsGetStorageRoot(): JsAny

@JsFun("(dir, name, create) => dir.getDirectoryHandle(name, { create: create })")
private external fun jsGetDirectoryHandle(dir: JsAny, name: String, create: Boolean): JsAny

@JsFun("(dir, name, create) => dir.getFileHandle(name, { create: create })")
private external fun jsGetFileHandle(dir: JsAny, name: String, create: Boolean): JsAny

@JsFun("(dir, name) => dir.removeEntry(name)")
private external fun jsRemoveEntry(dir: JsAny, name: String): JsAny

@JsFun("(handle) => handle.createWritable()")
private external fun jsCreateWritable(handle: JsAny): JsAny

@JsFun("(writable, data) => writable.write(data)")
private external fun jsWriteToStream(writable: JsAny, data: JsAny): JsAny

@JsFun("(writable) => writable.close()")
private external fun jsCloseStream(writable: JsAny): JsAny

@JsFun("(dir) => dir.entries()")
private external fun jsGetEntries(dir: JsAny): JsAny

// Async iterator to array
@JsFun("""(iterator) => {
    return (async function() {
        const arr = [];
        for await (const entry of iterator) {
            arr.push({ name: entry[0], handle: entry[1] });
        }
        return arr;
    })();
}""")
private external fun jsIteratorToArray(iterator: JsAny): JsAny

// Check if done (for async iterator)
@JsFun("(iterResult) => iterResult.done === true")
private external fun jsIteratorDone(iterResult: JsAny): Boolean

@JsFun("(iterator) => iterator.next()")
private external fun jsIteratorNext(iterator: JsAny): JsAny

// Handle kind check
@JsFun("(handle) => handle.kind === 'file'")
private external fun jsIsFileHandle(handle: JsAny): Boolean

/**
 * PWA Manager - Progressive Web App functionality
 * 
 * Provides:
 * - Service Worker registration and management
 * - Offline detection and handling
 * - Install prompt handling
 * - Background sync registration
 * - Push notification management
 * - Cache management
 */
object PWAManager {
    
    private var serviceWorkerRegistration: JsAny? = null
    private var deferredInstallPrompt: JsAny? = null
    private var isOnline: Boolean = true
    
    // Callbacks
    var onOnlineStatusChanged: ((Boolean) -> Unit)? = null
    var onInstallPromptAvailable: (() -> Unit)? = null
    var onServiceWorkerUpdated: (() -> Unit)? = null
    var onSyncComplete: ((Boolean) -> Unit)? = null
    var onNotificationReceived: ((NotificationData) -> Unit)? = null
    
    /**
     * Initialize PWA functionality
     */
    suspend fun initialize(): Boolean {
        return try {
            // Check if service workers are supported
            if (!isServiceWorkerSupported()) {
                jsLogInfo("Service Workers not supported")
                return false
            }
            
            // Register service worker
            val registered = registerServiceWorker()
            
            // Set up online/offline detection
            setupNetworkDetection()
            
            // Set up install prompt handling
            setupInstallPrompt()
            
            // Set up message handling from service worker
            setupServiceWorkerMessaging()
            
            jsLogInfo("Initialized successfully")
            registered
        } catch (e: Exception) {
            jsLogError("Initialization failed: ${e.message}")
            false
        }
    }
    
    /**
     * Check if Service Workers are supported
     */
    fun isServiceWorkerSupported(): Boolean {
        return try {
            jsIsServiceWorkerSupported()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Register the service worker
     */
    suspend fun registerServiceWorker(): Boolean {
        return try {
            val registrationPromise = jsRegisterServiceWorker("/sw.js", "/")
            val registration = awaitPromise(registrationPromise)
            
            if (registration == null) {
                jsLogError("Service Worker registration returned null")
                return false
            }
            
            serviceWorkerRegistration = registration
            
            val scope = jsGetStringProperty(registration, "scope") ?: "/"
            jsLogInfo("Service Worker registered: $scope")
            
            // Check for waiting worker
            val waiting = jsGetProperty(registration, "waiting")
            if (waiting != null) {
                onServiceWorkerUpdated?.invoke()
            }
            
            true
        } catch (e: Exception) {
            jsLogError("Service Worker registration failed: ${e.message}")
            false
        }
    }
    
    /**
     * Update the service worker
     */
    suspend fun updateServiceWorker() {
        try {
            val registration = serviceWorkerRegistration ?: return
            val updatePromise = jsUpdateServiceWorker(registration)
            awaitPromise(updatePromise)
            jsLogInfo("Service Worker update requested")
        } catch (e: Exception) {
            jsLogError("Service Worker update failed: ${e.message}")
        }
    }
    
    /**
     * Skip waiting and activate new service worker
     */
    fun activateNewServiceWorker() {
        val registration = serviceWorkerRegistration ?: return
        val waiting = jsGetProperty(registration, "waiting")
        if (waiting != null) {
            jsPostMessageToWaiting(waiting, "SKIP_WAITING")
        }
    }
    
    /**
     * Set up online/offline detection
     */
    private fun setupNetworkDetection() {
        isOnline = jsIsOnline()
        
        window.addEventListener("online", {
            isOnline = true
            jsLogInfo("Online")
            onOnlineStatusChanged?.invoke(true)
            // Trigger sync when back online
            requestBackgroundSync()
        })
        
        window.addEventListener("offline", {
            isOnline = false
            jsLogInfo("Offline")
            onOnlineStatusChanged?.invoke(false)
        })
    }
    
    /**
     * Check if currently online
     */
    fun isNetworkOnline(): Boolean = isOnline
    
    /**
     * Set up install prompt handling
     */
    private fun setupInstallPrompt() {
        window.addEventListener("beforeinstallprompt", { event ->
            // Prevent Chrome 67+ from automatically showing the prompt
            jsPreventDefault(event.unsafeCast<JsAny>())
            // Store the event for later use
            deferredInstallPrompt = event.unsafeCast<JsAny>()
            jsLogInfo("Install prompt available")
            onInstallPromptAvailable?.invoke()
        })
        
        window.addEventListener("appinstalled", {
            jsLogInfo("App installed")
            deferredInstallPrompt = null
        })
    }
    
    /**
     * Check if app can be installed
     */
    fun canInstall(): Boolean = deferredInstallPrompt != null
    
    /**
     * Show install prompt
     */
    suspend fun showInstallPrompt(): Boolean {
        val prompt = deferredInstallPrompt ?: return false
        
        return try {
            jsShowInstallPrompt(prompt)
            val userChoicePromise = jsGetUserChoice(prompt)
            val result = awaitPromise(userChoicePromise) ?: return false
            
            val outcome = jsGetStringProperty(result, "outcome")
            jsLogInfo("Install prompt result: $outcome")
            
            if (outcome == "accepted") {
                deferredInstallPrompt = null
                true
            } else {
                false
            }
        } catch (e: Exception) {
            jsLogError("Install prompt failed: ${e.message}")
            false
        }
    }
    
    /**
     * Check if app is running as PWA
     */
    fun isRunningAsPWA(): Boolean {
        return try {
            val displayMode = jsIsStandalonePWA()
            val iosStandalone = jsIsIOSStandalone()
            displayMode || iosStandalone
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Set up messaging with service worker
     */
    private fun setupServiceWorkerMessaging() {
        val swContainer = jsGetServiceWorkerContainer() ?: return
        
        window.addEventListener("message", { event ->
            try {
                val jsEvent = event.unsafeCast<JsAny>()
                val data = jsGetEventData(jsEvent) ?: return@addEventListener
                val type = jsGetStringProperty(data, "type")
                
                when (type) {
                    "SYNC_COMPLETE" -> {
                        val success = jsGetBoolProperty(data, "success")
                        jsLogInfo("Sync complete: $success")
                        onSyncComplete?.invoke(success)
                    }
                    "UPLOAD_COMPLETE" -> {
                        val id = jsGetStringProperty(data, "id")
                        jsLogInfo("Upload complete: $id")
                    }
                    "NOTIFICATION_CLICK" -> {
                        val notifData = jsGetProperty(data, "data")
                        val action = jsGetStringProperty(data, "action")
                        val title = if (notifData != null) jsGetStringProperty(notifData, "title") else null
                        val body = if (notifData != null) jsGetStringProperty(notifData, "body") else null
                        val url = if (notifData != null) jsGetStringProperty(notifData, "url") else null
                        
                        onNotificationReceived?.invoke(NotificationData(
                            title = title ?: "",
                            body = body ?: "",
                            action = action,
                            url = url
                        ))
                    }
                    "CACHES_CLEARED" -> {
                        jsLogInfo("Caches cleared")
                    }
                }
            } catch (e: Exception) {
                jsLogError("Error processing SW message: ${e.message}")
            }
        })
    }
    
    /**
     * Request background sync
     */
    fun requestBackgroundSync(tag: String = "csentry-data-sync"): Boolean {
        return try {
            val registration = serviceWorkerRegistration ?: return false
            val sync = jsGetSync(registration)
            if (sync != null) {
                jsSyncRegister(sync, tag)
                jsLogInfo("Background sync registered: $tag")
                true
            } else {
                jsLogInfo("Background sync not available")
                false
            }
        } catch (e: Exception) {
            jsLogError("Background sync not supported: ${e.message}")
            false
        }
    }
    
    /**
     * Queue item for sync when online
     */
    fun queueForSync(item: SyncQueueItem) {
        try {
            val swContainer = jsGetServiceWorkerContainer() ?: return
            
            val itemObj = jsCreateObject()
            jsSetStringProperty(itemObj, "url", item.url)
            jsSetStringProperty(itemObj, "method", item.method)
            jsSetIntProperty(itemObj, "priority", item.priority)
            if (item.data != null) {
                val dataJson = item.data.toString()
                jsSetStringProperty(itemObj, "data", dataJson)
            }
            
            val message = jsCreateObject()
            jsSetStringProperty(message, "type", "QUEUE_SYNC")
            jsSetProperty(message, "item", itemObj)
            
            jsPostMessageToSW(swContainer, message)
            jsLogInfo("Queued for sync: ${item.url}")
        } catch (e: Exception) {
            jsLogError("Failed to queue for sync: ${e.message}")
        }
    }
    
    /**
     * Queue upload for background processing
     */
    fun queueUpload(url: String, data: ByteArray, headers: Map<String, String> = emptyMap()) {
        try {
            val swContainer = jsGetServiceWorkerContainer() ?: return
            
            val headersObj = jsCreateObject()
            headers.forEach { (key, value) ->
                jsSetStringProperty(headersObj, key, value)
            }
            
            val uint8Array = byteArrayToJsUint8Array(data)
            
            val uploadObj = jsCreateObject()
            jsSetStringProperty(uploadObj, "url", url)
            jsSetStringProperty(uploadObj, "method", "POST")
            jsSetProperty(uploadObj, "headers", headersObj)
            jsSetProperty(uploadObj, "body", uint8Array)
            
            val message = jsCreateObject()
            jsSetStringProperty(message, "type", "QUEUE_UPLOAD")
            jsSetProperty(message, "upload", uploadObj)
            
            jsPostMessageToSW(swContainer, message)
            jsLogInfo("Queued upload: $url")
        } catch (e: Exception) {
            jsLogError("Failed to queue upload: ${e.message}")
        }
    }
    
    /**
     * Register periodic background sync
     */
    suspend fun registerPeriodicSync(intervalMs: Long = 24 * 60 * 60 * 1000): Boolean {
        return try {
            val registration = serviceWorkerRegistration ?: return false
            
            // Check permission
            val statusPromise = jsQueryPermission("periodic-background-sync")
            val status = awaitPromise(statusPromise)
            
            if (status == null || jsGetStringProperty(status, "state") != "granted") {
                jsLogInfo("Periodic sync permission not granted")
                return false
            }
            
            val periodicSync = jsGetPeriodicSync(registration)
            if (periodicSync == null) {
                jsLogInfo("Periodic sync not available")
                return false
            }
            
            val registerPromise = jsPeriodicSyncRegister(periodicSync, "csentry-periodic-sync", intervalMs.toDouble())
            awaitPromise(registerPromise)
            
            jsLogInfo("Periodic sync registered: ${intervalMs}ms")
            true
        } catch (e: Exception) {
            jsLogError("Periodic sync not supported: ${e.message}")
            false
        }
    }
    
    /**
     * Unregister periodic sync
     */
    suspend fun unregisterPeriodicSync(): Boolean {
        return try {
            val registration = serviceWorkerRegistration ?: return false
            val periodicSync = jsGetPeriodicSync(registration) ?: return false
            
            val unregisterPromise = jsPeriodicSyncUnregister(periodicSync, "csentry-periodic-sync")
            awaitPromise(unregisterPromise)
            
            jsLogInfo("Periodic sync unregistered")
            true
        } catch (e: Exception) {
            jsLogError("Failed to unregister periodic sync: ${e.message}")
            false
        }
    }
    
    /**
     * Request push notification permission
     */
    suspend fun requestNotificationPermission(): String {
        return try {
            val resultPromise = jsRequestNotificationPermission()
            val result = awaitPromise(resultPromise)
            result?.toString() ?: "denied"
        } catch (e: Exception) {
            "denied"
        }
    }
    
    /**
     * Subscribe to push notifications
     */
    suspend fun subscribeToPush(applicationServerKey: String): PushSubscription? {
        return try {
            val registration = serviceWorkerRegistration ?: return null
            val pushManager = jsGetPushManager(registration) ?: return null
            
            val options = jsCreateObject()
            jsSetProperty(options, "userVisibleOnly", jsBooleanToJs(true))
            jsSetProperty(options, "applicationServerKey", urlBase64ToUint8Array(applicationServerKey))
            
            val subscribePromise = jsPushSubscribe(pushManager, options)
            val subscription = awaitPromise(subscribePromise) ?: return null
            
            val endpoint = jsGetStringProperty(subscription, "endpoint") ?: return null
            val p256dhKey = jsGetSubscriptionKey(subscription, "p256dh")
            val authKey = jsGetSubscriptionKey(subscription, "auth")
            
            PushSubscription(
                endpoint = endpoint,
                p256dh = if (p256dhKey != null) arrayBufferToBase64(p256dhKey) else "",
                auth = if (authKey != null) arrayBufferToBase64(authKey) else ""
            )
        } catch (e: Exception) {
            jsLogError("Push subscription failed: ${e.message}")
            null
        }
    }
    
    /**
     * Get push subscription
     */
    suspend fun getPushSubscription(): PushSubscription? {
        return try {
            val registration = serviceWorkerRegistration ?: return null
            val pushManager = jsGetPushManager(registration) ?: return null
            
            val subscriptionPromise = jsGetPushSubscription(pushManager)
            val subscription = awaitPromise(subscriptionPromise) ?: return null
            
            val endpoint = jsGetStringProperty(subscription, "endpoint") ?: return null
            val p256dhKey = jsGetSubscriptionKey(subscription, "p256dh")
            val authKey = jsGetSubscriptionKey(subscription, "auth")
            
            PushSubscription(
                endpoint = endpoint,
                p256dh = if (p256dhKey != null) arrayBufferToBase64(p256dhKey) else "",
                auth = if (authKey != null) arrayBufferToBase64(authKey) else ""
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Show local notification
     */
    suspend fun showNotification(title: String, body: String, icon: String? = null, data: Map<String, Any>? = null): Boolean {
        return try {
            val registration = serviceWorkerRegistration ?: return false
            
            val options = jsCreateObject()
            jsSetStringProperty(options, "body", body)
            jsSetStringProperty(options, "icon", icon ?: "/icons/icon-192x192.png")
            jsSetStringProperty(options, "badge", "/icons/badge-72x72.png")
            
            // Vibration pattern
            val vibrateArray = jsCreateArray()
            jsArrayPushInt(vibrateArray, 100)
            jsArrayPushInt(vibrateArray, 50)
            jsArrayPushInt(vibrateArray, 100)
            jsSetProperty(options, "vibrate", vibrateArray)
            
            if (data != null) {
                val dataObj = jsCreateObject()
                data.forEach { (key, value) ->
                    when (value) {
                        is String -> jsSetStringProperty(dataObj, key, value)
                        is Int -> jsSetIntProperty(dataObj, key, value)
                        else -> jsSetStringProperty(dataObj, key, value.toString())
                    }
                }
                jsSetProperty(options, "data", dataObj)
            }
            
            val notifPromise = jsShowNotification(registration, title, options)
            awaitPromise(notifPromise)
            true
        } catch (e: Exception) {
            jsLogError("Show notification failed: ${e.message}")
            false
        }
    }
    
    /**
     * Clear all caches
     */
    fun clearCaches() {
        try {
            val swContainer = jsGetServiceWorkerContainer() ?: return
            val message = jsCreateMessageObject("CLEAR_CACHE")
            jsPostMessageToSW(swContainer, message)
        } catch (e: Exception) {
            jsLogError("Failed to clear caches: ${e.message}")
        }
    }
    
    /**
     * Get cache status
     */
    suspend fun getCacheStatus(): Map<String, Int> {
        return try {
            val deferred = CompletableDeferred<Map<String, Int>>()
            
            val channel = jsCreateMessageChannel()
            val port1 = jsGetPort1(channel)
            val port2 = jsGetPort2(channel)
            
            jsSetOnMessage(port1) { event ->
                val status = mutableMapOf<String, Int>()
                val data = jsGetEventData(event)
                if (data != null) {
                    val keys = jsObjectKeys(data)
                    val keysLength = jsArrayLength(keys)
                    for (i in 0 until keysLength) {
                        val key = jsArrayGet(keys, i)?.toString() ?: continue
                        val value = jsGetNumberProperty(data, key)
                        status[key] = value
                    }
                }
                deferred.complete(status)
            }
            
            val swContainer = jsGetServiceWorkerContainer() ?: return emptyMap()
            val message = jsCreateMessageObject("GET_CACHE_STATUS")
            val portsArray = jsCreateArray()
            jsArrayPush(portsArray, port2)
            jsPostMessageWithPorts(swContainer, message, portsArray)
            
            deferred.await()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Helper: Convert URL-safe base64 to Uint8Array
     */
    private fun urlBase64ToUint8Array(base64String: String): JsAny {
        val padding = "=".repeat((4 - base64String.length % 4) % 4)
        val base64 = (base64String + padding)
            .replace("-", "+")
            .replace("_", "/")
        
        val rawData = jsAtob(base64)
        val outputArray = jsCreateUint8Array(rawData.length)
        
        for (i in rawData.indices) {
            jsSetUint8ArrayElement(outputArray, i, rawData[i].code)
        }
        
        return outputArray
    }
    
    /**
     * Helper: Convert ArrayBuffer to base64
     */
    private fun arrayBufferToBase64(buffer: JsAny): String {
        return try {
            val bytes = jsUint8ArrayFromBuffer(buffer)
            val len = jsUint8ArrayLength(bytes)
            val binary = StringBuilder()
            for (i in 0 until len) {
                binary.append(jsGetUint8ArrayElement(bytes, i).toChar())
            }
            jsBtoa(binary.toString())
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Helper: Convert ByteArray to JavaScript Uint8Array
     */
    private fun byteArrayToJsUint8Array(data: ByteArray): JsAny {
        val uint8Array = jsCreateUint8Array(data.size)
        for (i in data.indices) {
            jsSetUint8ArrayElement(uint8Array, i, data[i].toInt() and 0xFF)
        }
        return uint8Array
    }
    
    /**
     * Await a JS Promise using suspendCancellableCoroutine
     */
    private suspend fun awaitPromise(promise: JsAny): JsAny? = suspendCancellableCoroutine { cont ->
        jsHandlePromise(
            promise,
            { result ->
                cont.resume(result)
                null
            },
            { error ->
                cont.resumeWithException(Exception("Promise rejected: $error"))
                null
            }
        )
    }
}

// Array helpers
@JsFun("(arr, item) => arr.push(item)")
private external fun jsArrayPush(arr: JsAny, item: JsAny)

@JsFun("(arr, item) => arr.push(item)")
private external fun jsArrayPushInt(arr: JsAny, item: Int)

// Boolean conversion helper
@JsFun("(b) => b")
private external fun jsBooleanToJs(b: Boolean): JsAny

/**
 * Sync queue item
 */
data class SyncQueueItem(
    val url: String,
    val method: String = "POST",
    val data: Any? = null,
    val priority: Int = 0
)

/**
 * Push subscription info
 */
data class PushSubscription(
    val endpoint: String,
    val p256dh: String,
    val auth: String
)

/**
 * Notification data
 */
data class NotificationData(
    val title: String,
    val body: String,
    val action: String? = null,
    val url: String? = null
)

/**
 * Offline Storage Manager - OPFS-based offline data storage
 */
object OfflineStorage {
    
    /**
     * Check if there's pending offline data
     */
    suspend fun hasPendingData(): Boolean {
        return try {
            if (!jsIsOpfsAvailable()) return false
            
            val rootPromise = jsGetStorageRoot()
            val root = awaitPromise(rootPromise) ?: return false
            
            // Try to get pending_sync directory
            val pendingDirPromise = jsGetDirectoryHandle(root, "pending_sync", false)
            val pendingDir = awaitPromise(pendingDirPromise) ?: return false
            
            // Check if there are any entries
            val entriesIterator = jsGetEntries(pendingDir)
            val nextPromise = jsIteratorNext(entriesIterator)
            val entry = awaitPromise(nextPromise) ?: return false
            
            !jsIteratorDone(entry)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get pending sync count
     */
    suspend fun getPendingSyncCount(): Int {
        return try {
            if (!jsIsOpfsAvailable()) return 0
            
            val rootPromise = jsGetStorageRoot()
            val root = awaitPromise(rootPromise) ?: return 0
            
            val pendingDirPromise = jsGetDirectoryHandle(root, "pending_sync", false)
            val pendingDir = awaitPromise(pendingDirPromise) ?: return 0
            
            // Convert iterator to array and count
            val entriesIterator = jsGetEntries(pendingDir)
            val arrayPromise = jsIteratorToArray(entriesIterator)
            val entriesArray = awaitPromise(arrayPromise) ?: return 0
            
            jsArrayLength(entriesArray)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Queue data for sync
     */
    suspend fun queueForSync(id: String, data: ByteArray): Boolean {
        return try {
            if (!jsIsOpfsAvailable()) return false
            
            val rootPromise = jsGetStorageRoot()
            val root = awaitPromise(rootPromise) ?: return false
            
            val pendingDirPromise = jsGetDirectoryHandle(root, "pending_sync", true)
            val pendingDir = awaitPromise(pendingDirPromise) ?: return false
            
            val fileHandlePromise = jsGetFileHandle(pendingDir, "$id.json", true)
            val fileHandle = awaitPromise(fileHandlePromise) ?: return false
            
            val writablePromise = jsCreateWritable(fileHandle)
            val writable = awaitPromise(writablePromise) ?: return false
            
            val uint8Array = byteArrayToJsUint8Array(data)
            
            val writePromise = jsWriteToStream(writable, uint8Array)
            awaitPromise(writePromise)
            
            val closePromise = jsCloseStream(writable)
            awaitPromise(closePromise)
            
            // Request background sync
            PWAManager.requestBackgroundSync()
            
            true
        } catch (e: Exception) {
            jsLogError("Failed to queue for sync: ${e.message}")
            false
        }
    }
    
    /**
     * Clear synced data
     */
    suspend fun clearSyncedData(id: String): Boolean {
        return try {
            if (!jsIsOpfsAvailable()) return false
            
            val rootPromise = jsGetStorageRoot()
            val root = awaitPromise(rootPromise) ?: return false
            
            val pendingDirPromise = jsGetDirectoryHandle(root, "pending_sync", false)
            val pendingDir = awaitPromise(pendingDirPromise) ?: return false
            
            val removePromise = jsRemoveEntry(pendingDir, "$id.json")
            awaitPromise(removePromise)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Helper: Convert ByteArray to JavaScript Uint8Array
     */
    private fun byteArrayToJsUint8Array(data: ByteArray): JsAny {
        val uint8Array = jsCreateUint8Array(data.size)
        for (i in data.indices) {
            jsSetUint8ArrayElement(uint8Array, i, data[i].toInt() and 0xFF)
        }
        return uint8Array
    }
    
    /**
     * Await a JS Promise using suspendCancellableCoroutine
     */
    private suspend fun awaitPromise(promise: JsAny): JsAny? = suspendCancellableCoroutine { cont ->
        jsHandlePromise(
            promise,
            { result ->
                cont.resume(result)
                null
            },
            { error ->
                cont.resumeWithException(Exception("Promise rejected: $error"))
                null
            }
        )
    }
}
