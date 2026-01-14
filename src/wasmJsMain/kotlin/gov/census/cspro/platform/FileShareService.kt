package gov.census.cspro.platform

import kotlin.js.JsString
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WiFi P2P File Sharing Service for PWA
 * 
 * Implements file sharing between nearby devices using:
 * - WebRTC for peer-to-peer data channels
 * - BroadcastChannel for same-browser communication
 * - Optional: WebSocket signaling server for discovery
 * 
 * Mirrors: Android's FileShareActivity.kt and WiFi Direct functionality
 */

// External declarations for WebRTC and BroadcastChannel
@JsFun("() => ({ urls: ['stun:stun.l.google.com:19302', 'stun:stun1.l.google.com:19302', 'stun:stun2.l.google.com:19302'] })")
private external fun getIceServerConfig(): JsAny

@JsFun("(name) => new BroadcastChannel(name)")
private external fun createBroadcastChannel(name: JsString): JsAny

@JsFun("(channel, callback) => { channel.onmessage = (e) => callback(e.data); }")
private external fun setBroadcastChannelOnMessage(channel: JsAny, callback: (JsAny) -> JsAny?): Unit

@JsFun("(channel, msg) => channel.postMessage(msg)")
private external fun broadcastChannelPostMessage(channel: JsAny, msg: JsAny): Unit

@JsFun("(config) => new RTCPeerConnection(config)")
private external fun createRTCPeerConnection(config: JsAny): JsAny

@JsFun("(pc, name, options) => pc.createDataChannel(name, options)")
private external fun pcCreateDataChannel(pc: JsAny, name: JsString, options: JsAny): JsAny

@JsFun("(pc) => pc.createOffer()")
private external fun pcCreateOffer(pc: JsAny): JsAny // Returns Promise

@JsFun("(pc) => pc.createAnswer()")
private external fun pcCreateAnswer(pc: JsAny): JsAny // Returns Promise

@JsFun("(pc, desc) => pc.setLocalDescription(desc)")
private external fun pcSetLocalDescription(pc: JsAny, desc: JsAny): JsAny // Returns Promise

@JsFun("(pc, desc) => pc.setRemoteDescription(desc)")
private external fun pcSetRemoteDescription(pc: JsAny, desc: JsAny): JsAny // Returns Promise

@JsFun("(pc, candidate) => pc.addIceCandidate(candidate)")
private external fun pcAddIceCandidate(pc: JsAny, candidate: JsAny): JsAny // Returns Promise

@JsFun("(pc) => pc.connectionState || 'new'")
private external fun pcGetConnectionState(pc: JsAny): JsString

@JsFun("(pc, callback) => { pc.onicecandidate = (e) => { if (e.candidate) callback(e.candidate); }; }")
private external fun pcSetOnIceCandidate(pc: JsAny, callback: (JsAny) -> JsAny?): Unit

@JsFun("(pc, callback) => { pc.onconnectionstatechange = () => callback(pc.connectionState); }")
private external fun pcSetOnConnectionStateChange(pc: JsAny, callback: (JsString) -> JsAny?): Unit

@JsFun("(pc, callback) => { pc.ondatachannel = (e) => callback(e.channel); }")
private external fun pcSetOnDataChannel(pc: JsAny, callback: (JsAny) -> JsAny?): Unit

@JsFun("(dc, callback) => { dc.onopen = () => callback(); }")
private external fun dcSetOnOpen(dc: JsAny, callback: () -> JsAny?): Unit

@JsFun("(dc, callback) => { dc.onclose = () => callback(); }")
private external fun dcSetOnClose(dc: JsAny, callback: () -> JsAny?): Unit

@JsFun("(dc, callback) => { dc.onmessage = (e) => callback(e.data); }")
private external fun dcSetOnMessage(dc: JsAny, callback: (JsString) -> JsAny?): Unit

@JsFun("(dc, data) => dc.send(data)")
private external fun dcSend(dc: JsAny, data: JsString): Unit

@JsFun("(dc) => dc.close()")
private external fun dcClose(dc: JsAny): Unit

@JsFun("(pc) => pc.close()")
private external fun pcClose(pc: JsAny): Unit

@JsFun("() => navigator.storage.getDirectory()")
private external fun getStorageDirectory(): JsAny // Returns Promise

@JsFun("(dir, name, create) => dir.getDirectoryHandle(name, { create: create })")
private external fun dirGetDirectoryHandle(dir: JsAny, name: JsString, create: Boolean): JsAny // Returns Promise

@JsFun("(dir, name, create) => dir.getFileHandle(name, { create: create })")
private external fun dirGetFileHandle(dir: JsAny, name: JsString, create: Boolean): JsAny // Returns Promise

@JsFun("(dir, name, recursive) => dir.removeEntry(name, { recursive: recursive })")
private external fun dirRemoveEntry(dir: JsAny, name: JsString, recursive: Boolean): JsAny // Returns Promise

@JsFun("(dir) => dir.entries()")
private external fun dirGetEntries(dir: JsAny): JsAny // Returns iterator

@JsFun("(iter) => iter.next()")
private external fun iteratorNext(iter: JsAny): JsAny // Returns Promise

@JsFun("(result) => result.done")
private external fun iteratorResultDone(result: JsAny): Boolean

@JsFun("(result) => result.value")
private external fun iteratorResultValue(result: JsAny): JsAny

@JsFun("(entry) => entry[0]")
private external fun entryGetName(entry: JsAny): JsString

@JsFun("(entry) => entry[1]")
private external fun entryGetHandle(entry: JsAny): JsAny

@JsFun("(handle) => handle.kind")
private external fun handleGetKind(handle: JsAny): JsString

@JsFun("(handle) => handle.getFile()")
private external fun fileHandleGetFile(handle: JsAny): JsAny // Returns Promise

@JsFun("(file) => file.size")
private external fun fileGetSize(file: JsAny): Int

@JsFun("(file) => file.arrayBuffer()")
private external fun fileGetArrayBuffer(file: JsAny): JsAny // Returns Promise

@JsFun("(buffer) => new Uint8Array(buffer)")
private external fun createUint8ArrayFromBuffer(buffer: JsAny): JsAny

@JsFun("(arr) => arr.length")
private external fun uint8ArrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun uint8ArrayGet(arr: JsAny, i: Int): Int

@JsFun("(data) => btoa(String.fromCharCode.apply(null, data))")
private external fun encodeBase64Js(data: JsAny): JsString

@JsFun("(str) => { const binary = atob(str); const arr = new Uint8Array(binary.length); for (let i = 0; i < binary.length; i++) arr[i] = binary.charCodeAt(i); return arr; }")
private external fun decodeBase64Js(str: JsString): JsAny

@JsFun("(key) => localStorage.getItem(key)")
private external fun localStorageGetItem(key: JsString): JsString?

@JsFun("(key, value) => localStorage.setItem(key, value)")
private external fun localStorageSetItem(key: JsString, value: JsString): Unit

@JsFun("() => Date.now()")
private external fun dateNow(): Double

@JsFun("() => ({ ordered: true })")
private external fun createDataChannelOptions(): JsAny

@JsFun("() => ({ iceServers: [{ urls: ['stun:stun.l.google.com:19302', 'stun:stun1.l.google.com:19302'] }] })")
private external fun createPeerConnectionConfig(): JsAny

@JsFun("(obj) => JSON.stringify(obj)")
private external fun jsonStringify(obj: JsAny): JsString

@JsFun("(str) => JSON.parse(str)")
private external fun jsonParse(str: JsString): JsAny

@JsFun("(obj, key) => obj[key]")
private external fun objGet(obj: JsAny, key: JsString): JsAny?

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun objSet(obj: JsAny, key: JsString, value: JsAny): Unit

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun objSetString(obj: JsAny, key: JsString, value: JsString): Unit

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun objSetInt(obj: JsAny, key: JsString, value: Int): Unit

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun objSetBoolean(obj: JsAny, key: JsString, value: Boolean): Unit

@JsFun("() => ({})")
private external fun createEmptyObject(): JsAny

@JsFun("(arr) => arr")
private external fun listToJsArray(arr: JsAny): JsAny

@JsFun("(promise, resolve, reject) => promise.then(resolve).catch(reject)")
private external fun promiseThenCatch(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?): Unit

@JsFun("(s) => s")
private external fun toJsString(s: String): JsString

@JsFun("(s) => s")
private external fun jsStringToKotlin(s: JsString): String

// Helper suspend function to await a JS Promise
private suspend fun <T : JsAny?> awaitPromise(promise: JsAny): T {
    return suspendCancellableCoroutine { continuation ->
        promiseThenCatch(
            promise,
            { result: JsAny? ->
                @Suppress("UNCHECKED_CAST")
                continuation.resume(result as T)
                null
            },
            { error: JsAny ->
                continuation.resumeWithException(Exception("JS Promise rejected: $error"))
                null
            }
        )
    }
}

object FileShareService {
    
    private var peerConnection: JsAny? = null
    private var dataChannel: JsAny? = null
    private var isHost = false
    private var onCommandReceived: ((FileShareCommand) -> Unit)? = null
    private var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
    
    // BroadcastChannel for same-origin device discovery
    private var discoveryChannel: JsAny? = null
    private val discoveredPeers = mutableListOf<PeerDevice>()
    
    // Current session info
    private var sessionId: String? = null
    private var deviceName: String = "CSPro Device"
    
    /**
     * Initialize the file share service
     */
    fun initialize(name: String = "CSPro Device") {
        deviceName = name
        setupDiscoveryChannel()
        println("[FileShare] Service initialized: $name")
    }
    
    /**
     * Setup BroadcastChannel for device discovery
     */
    private fun setupDiscoveryChannel() {
        try {
            discoveryChannel = createBroadcastChannel(toJsString("cspro-file-share"))
            
            val channel = discoveryChannel
            if (channel != null) {
                setBroadcastChannelOnMessage(channel) { data: JsAny ->
                    handleBroadcastMessage(data)
                    null
                }
            }
            
            println("[FileShare] Discovery channel setup")
        } catch (e: Exception) {
            println("[FileShare] BroadcastChannel not available: ${e.message}")
        }
    }
    
    private fun handleBroadcastMessage(data: JsAny) {
        val typeVal = objGet(data, toJsString("type"))
        if (typeVal != null) {
            val typeStr = jsStringToKotlin(typeVal.unsafeCast<JsString>())
            when (typeStr) {
                "discovery" -> handleDiscovery(data)
                "offer" -> handleOffer(data)
                "answer" -> handleAnswer(data)
                "ice" -> handleIceCandidate(data)
            }
        }
    }
    
    /**
     * Start hosting a file share session
     */
    suspend fun startHost(sessionName: String = "CSPro Session"): String {
        isHost = true
        sessionId = generateSessionId()
        
        createPeerConnectionInternal()
        
        val pc = peerConnection ?: throw Exception("Failed to create peer connection")
        
        // Create data channel for file transfer
        val options = createDataChannelOptions()
        dataChannel = pcCreateDataChannel(pc, toJsString("fileShare"), options)
        setupDataChannelInternal()
        
        // Create offer
        val offer = awaitPromise<JsAny>(pcCreateOffer(pc))
        awaitPromise<JsAny?>(pcSetLocalDescription(pc, offer))
        
        // Broadcast availability
        broadcastDiscovery()
        
        println("[FileShare] Hosting session: $sessionId")
        return sessionId!!
    }
    
    /**
     * Join a file share session
     */
    suspend fun joinSession(hostSessionId: String): Boolean {
        isHost = false
        sessionId = hostSessionId
        
        createPeerConnectionInternal()
        
        // Request connection from host
        val message = createEmptyObject()
        objSetString(message, toJsString("type"), toJsString("join"))
        objSetString(message, toJsString("sessionId"), toJsString(hostSessionId))
        objSetString(message, toJsString("deviceName"), toJsString(deviceName))
        discoveryChannel?.let { broadcastChannelPostMessage(it, message) }
        
        return true
    }
    
    /**
     * Discover available peers
     */
    fun discoverPeers(): List<PeerDevice> {
        // Broadcast discovery request
        val message = createEmptyObject()
        objSetString(message, toJsString("type"), toJsString("discovery"))
        objSetString(message, toJsString("deviceName"), toJsString(deviceName))
        objSetInt(message, toJsString("timestamp"), dateNow().toInt())
        discoveryChannel?.let { broadcastChannelPostMessage(it, message) }
        
        return discoveredPeers.toList()
    }
    
    /**
     * Get list of discovered peers
     */
    fun getDiscoveredPeers(): List<PeerDevice> = discoveredPeers.toList()
    
    /**
     * Execute a file share command
     */
    suspend fun executeCommand(command: FileShareCommand): FileShareResult {
        return when (command) {
            is FileShareCommand.MakeDir -> executeMakeDir(command)
            is FileShareCommand.ListDir -> executeListDir(command)
            is FileShareCommand.PushFiles -> executePushFiles(command)
            is FileShareCommand.PullFiles -> executePullFiles(command)
            is FileShareCommand.Delete -> executeDelete(command)
        }
    }
    
    /**
     * Send command to connected peer
     */
    private suspend fun sendCommand(command: FileShareCommand): Boolean {
        val dc = dataChannel ?: return false
        
        return try {
            val json = command.toJson()
            dcSend(dc, toJsString(json))
            true
        } catch (e: Exception) {
            println("[FileShare] Error sending command: ${e.message}")
            false
        }
    }
    
    /**
     * Create WebRTC peer connection
     */
    private fun createPeerConnectionInternal() {
        val config = createPeerConnectionConfig()
        peerConnection = createRTCPeerConnection(config)
        
        val pc = peerConnection ?: return
        
        pcSetOnIceCandidate(pc) { candidate: JsAny ->
            val message = createEmptyObject()
            objSetString(message, toJsString("type"), toJsString("ice"))
            sessionId?.let { objSetString(message, toJsString("sessionId"), toJsString(it)) }
            objSet(message, toJsString("candidate"), candidate)
            discoveryChannel?.let { broadcastChannelPostMessage(it, message) }
            null
        }
        
        pcSetOnConnectionStateChange(pc) { state: JsString ->
            val stateStr = jsStringToKotlin(state)
            val connState = when (stateStr) {
                "connected" -> ConnectionState.CONNECTED
                "connecting" -> ConnectionState.CONNECTING
                "disconnected" -> ConnectionState.DISCONNECTED
                "failed" -> ConnectionState.FAILED
                else -> ConnectionState.NEW
            }
            onConnectionStateChanged?.invoke(connState)
            println("[FileShare] Connection state: $stateStr")
            null
        }
        
        pcSetOnDataChannel(pc) { channel: JsAny ->
            dataChannel = channel
            setupDataChannelInternal()
            null
        }
    }
    
    /**
     * Setup data channel event handlers
     */
    private fun setupDataChannelInternal() {
        val dc = dataChannel ?: return
        
        dcSetOnOpen(dc) {
            println("[FileShare] Data channel opened")
            onConnectionStateChanged?.invoke(ConnectionState.CONNECTED)
            null
        }
        
        dcSetOnClose(dc) {
            println("[FileShare] Data channel closed")
            onConnectionStateChanged?.invoke(ConnectionState.DISCONNECTED)
            null
        }
        
        dcSetOnMessage(dc) { data: JsString ->
            val dataStr = jsStringToKotlin(data)
            try {
                val command = FileShareCommand.fromJson(dataStr)
                onCommandReceived?.invoke(command)
            } catch (e: Exception) {
                println("[FileShare] Error parsing command: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Handle discovery message
     */
    private fun handleDiscovery(data: JsAny) {
        val deviceIdVal = objGet(data, toJsString("deviceId"))
        val deviceId = if (deviceIdVal != null) jsStringToKotlin(deviceIdVal.unsafeCast<JsString>()) else return
        val nameVal = objGet(data, toJsString("deviceName"))
        val name = if (nameVal != null) jsStringToKotlin(nameVal.unsafeCast<JsString>()) else return
        val sessionIdVal = objGet(data, toJsString("sessionId"))
        val peerSessionId = if (sessionIdVal != null) jsStringToKotlin(sessionIdVal.unsafeCast<JsString>()) else null
        
        val peer = PeerDevice(
            id = deviceId,
            name = name,
            sessionId = peerSessionId
        )
        
        if (!discoveredPeers.any { it.id == deviceId }) {
            discoveredPeers.add(peer)
            println("[FileShare] Discovered peer: $name")
        }
        
        // If we're hosting, respond with our session info
        if (isHost && sessionId != null) {
            val response = createEmptyObject()
            objSetString(response, toJsString("type"), toJsString("discovery"))
            objSetString(response, toJsString("deviceId"), toJsString(getDeviceId()))
            objSetString(response, toJsString("deviceName"), toJsString(deviceName))
            sessionId?.let { objSetString(response, toJsString("sessionId"), toJsString(it)) }
            objSetBoolean(response, toJsString("isHost"), true)
            discoveryChannel?.let { broadcastChannelPostMessage(it, response) }
        }
    }
    
    /**
     * Handle WebRTC offer
     */
    private fun handleOffer(data: JsAny) {
        val dataSessionIdVal = objGet(data, toJsString("sessionId"))
        val dataSessionId = if (dataSessionIdVal != null) jsStringToKotlin(dataSessionIdVal.unsafeCast<JsString>()) else null
        if (isHost || sessionId != dataSessionId) return
        
        val pc = peerConnection ?: return
        val offerVal = objGet(data, toJsString("offer")) ?: return
        
        MainScope().launch {
            try {
                awaitPromise<JsAny?>(pcSetRemoteDescription(pc, offerVal))
                val answer = awaitPromise<JsAny>(pcCreateAnswer(pc))
                awaitPromise<JsAny?>(pcSetLocalDescription(pc, answer))
                
                val message = createEmptyObject()
                objSetString(message, toJsString("type"), toJsString("answer"))
                sessionId?.let { objSetString(message, toJsString("sessionId"), toJsString(it)) }
                objSet(message, toJsString("answer"), answer)
                discoveryChannel?.let { broadcastChannelPostMessage(it, message) }
            } catch (e: Exception) {
                println("[FileShare] Error handling offer: ${e.message}")
            }
        }
    }
    
    /**
     * Handle WebRTC answer
     */
    private fun handleAnswer(data: JsAny) {
        val dataSessionIdVal = objGet(data, toJsString("sessionId"))
        val dataSessionId = if (dataSessionIdVal != null) jsStringToKotlin(dataSessionIdVal.unsafeCast<JsString>()) else null
        if (!isHost || sessionId != dataSessionId) return
        
        val pc = peerConnection ?: return
        val answerVal = objGet(data, toJsString("answer")) ?: return
        
        MainScope().launch {
            try {
                awaitPromise<JsAny?>(pcSetRemoteDescription(pc, answerVal))
            } catch (e: Exception) {
                println("[FileShare] Error handling answer: ${e.message}")
            }
        }
    }
    
    /**
     * Handle ICE candidate
     */
    private fun handleIceCandidate(data: JsAny) {
        val dataSessionIdVal = objGet(data, toJsString("sessionId"))
        val dataSessionId = if (dataSessionIdVal != null) jsStringToKotlin(dataSessionIdVal.unsafeCast<JsString>()) else null
        if (sessionId != dataSessionId) return
        
        val pc = peerConnection ?: return
        val candidateVal = objGet(data, toJsString("candidate")) ?: return
        
        MainScope().launch {
            try {
                awaitPromise<JsAny?>(pcAddIceCandidate(pc, candidateVal))
            } catch (e: Exception) {
                println("[FileShare] Error adding ICE candidate: ${e.message}")
            }
        }
    }
    
    /**
     * Broadcast discovery message
     */
    private fun broadcastDiscovery() {
        val message = createEmptyObject()
        objSetString(message, toJsString("type"), toJsString("discovery"))
        objSetString(message, toJsString("deviceId"), toJsString(getDeviceId()))
        objSetString(message, toJsString("deviceName"), toJsString(deviceName))
        sessionId?.let { objSetString(message, toJsString("sessionId"), toJsString(it)) }
        objSetBoolean(message, toJsString("isHost"), isHost)
        discoveryChannel?.let { broadcastChannelPostMessage(it, message) }
    }
    
    /**
     * Execute MakeDir command
     */
    private suspend fun executeMakeDir(command: FileShareCommand.MakeDir): FileShareResult {
        return try {
            val root = awaitPromise<JsAny>(getStorageDirectory())
            
            var dir = root
            for (part in command.path.split("/").filter { it.isNotEmpty() }) {
                dir = awaitPromise<JsAny>(dirGetDirectoryHandle(dir, toJsString(part), true))
            }
            
            FileShareResult.Success("Directory created: ${command.path}")
        } catch (e: Exception) {
            FileShareResult.Error("Failed to create directory: ${e.message}")
        }
    }
    
    /**
     * Execute ListDir command
     */
    private suspend fun executeListDir(command: FileShareCommand.ListDir): FileShareResult {
        return try {
            val root = awaitPromise<JsAny>(getStorageDirectory())
            
            val dir = if (command.path.isEmpty() || command.path == "/") {
                root
            } else {
                var d = root
                for (part in command.path.split("/").filter { it.isNotEmpty() }) {
                    d = awaitPromise<JsAny>(dirGetDirectoryHandle(d, toJsString(part), false))
                }
                d
            }
            
            val entries = mutableListOf<DirectoryEntry>()
            val iterator = dirGetEntries(dir)
            
            while (true) {
                val next = awaitPromise<JsAny>(iteratorNext(iterator))
                if (iteratorResultDone(next)) break
                
                val entry = iteratorResultValue(next)
                val name = jsStringToKotlin(entryGetName(entry))
                val handle = entryGetHandle(entry)
                val kind = jsStringToKotlin(handleGetKind(handle))
                val isDir = kind == "directory"
                
                var size = 0L
                if (!isDir) {
                    val file = awaitPromise<JsAny>(fileHandleGetFile(handle))
                    size = fileGetSize(file).toLong()
                }
                
                entries.add(DirectoryEntry(name, isDir, size))
            }
            
            FileShareResult.DirectoryListing(entries)
        } catch (e: Exception) {
            FileShareResult.Error("Failed to list directory: ${e.message}")
        }
    }
    
    /**
     * Execute PushFiles command - send files to peer
     */
    private suspend fun executePushFiles(command: FileShareCommand.PushFiles): FileShareResult {
        val dc = dataChannel
        if (dc == null) {
            return FileShareResult.Error("Not connected to peer")
        }
        
        return try {
            var totalSize = 0L
            val files = mutableListOf<Pair<String, ByteArray>>()
            
            for (path in command.paths) {
                val data = readFileFromOPFS(path)
                if (data != null) {
                    files.add(path to data)
                    totalSize += data.size
                }
            }
            
            // Send files over data channel
            for ((path, data) in files) {
                // Send file header
                val header = createEmptyObject()
                objSetString(header, toJsString("type"), toJsString("file"))
                objSetString(header, toJsString("command"), toJsString("push"))
                objSetString(header, toJsString("path"), toJsString(path))
                objSetInt(header, toJsString("size"), data.size)
                objSetString(header, toJsString("destinationPath"), toJsString(command.destinationPath))
                dcSend(dc, jsonStringify(header))
                
                // Send file data in chunks
                val chunkSize = 16384 // 16KB chunks
                var offset = 0
                while (offset < data.size) {
                    val end = minOf(offset + chunkSize, data.size)
                    val chunk = data.sliceArray(offset until end)
                    
                    // Convert to base64 for transmission
                    val base64 = encodeBase64(chunk)
                    val chunkMsg = createEmptyObject()
                    objSetString(chunkMsg, toJsString("type"), toJsString("chunk"))
                    objSetString(chunkMsg, toJsString("path"), toJsString(path))
                    objSetInt(chunkMsg, toJsString("offset"), offset)
                    objSetString(chunkMsg, toJsString("data"), toJsString(base64))
                    dcSend(dc, jsonStringify(chunkMsg))
                    
                    offset = end
                }
                
                // Send file complete
                val complete = createEmptyObject()
                objSetString(complete, toJsString("type"), toJsString("fileComplete"))
                objSetString(complete, toJsString("path"), toJsString(path))
                dcSend(dc, jsonStringify(complete))
            }
            
            FileShareResult.Success("Pushed ${files.size} files ($totalSize bytes)")
        } catch (e: Exception) {
            FileShareResult.Error("Failed to push files: ${e.message}")
        }
    }
    
    /**
     * Execute PullFiles command - request files from peer
     */
    private suspend fun executePullFiles(command: FileShareCommand.PullFiles): FileShareResult {
        val dc = dataChannel
        if (dc == null) {
            return FileShareResult.Error("Not connected to peer")
        }
        
        return try {
            // Send pull request to peer
            val request = createEmptyObject()
            objSetString(request, toJsString("type"), toJsString("pullRequest"))
            // Convert paths to JsArray - simplified approach
            objSetString(request, toJsString("paths"), toJsString(command.paths.joinToString("|")))
            objSetString(request, toJsString("destinationPath"), toJsString(command.destinationPath))
            dcSend(dc, jsonStringify(request))
            
            FileShareResult.Success("Pull request sent for ${command.paths.size} files")
        } catch (e: Exception) {
            FileShareResult.Error("Failed to request files: ${e.message}")
        }
    }
    
    /**
     * Execute Delete command
     */
    private suspend fun executeDelete(command: FileShareCommand.Delete): FileShareResult {
        return try {
            val root = awaitPromise<JsAny>(getStorageDirectory())
            
            val parts = command.path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                return FileShareResult.Error("Invalid path")
            }
            
            var parentDir = root
            for (i in 0 until parts.size - 1) {
                parentDir = awaitPromise<JsAny>(dirGetDirectoryHandle(parentDir, toJsString(parts[i]), false))
            }
            
            val entryName = parts.last()
            
            // Try to delete with recursive option
            try {
                awaitPromise<JsAny?>(dirRemoveEntry(parentDir, toJsString(entryName), true))
            } catch (e: Exception) {
                awaitPromise<JsAny?>(dirRemoveEntry(parentDir, toJsString(entryName), false))
            }
            
            FileShareResult.Success("Deleted: ${command.path}")
        } catch (e: Exception) {
            FileShareResult.Error("Failed to delete: ${e.message}")
        }
    }
    
    /**
     * Read file from OPFS
     */
    private suspend fun readFileFromOPFS(path: String): ByteArray? {
        return try {
            val root = awaitPromise<JsAny>(getStorageDirectory())
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                dir = awaitPromise<JsAny>(dirGetDirectoryHandle(dir, toJsString(parts[i]), false))
            }
            
            val fileHandle = awaitPromise<JsAny>(dirGetFileHandle(dir, toJsString(parts.last()), false))
            val file = awaitPromise<JsAny>(fileHandleGetFile(fileHandle))
            val arrayBuffer = awaitPromise<JsAny>(fileGetArrayBuffer(file))
            
            val uint8Array = createUint8ArrayFromBuffer(arrayBuffer)
            val length = uint8ArrayLength(uint8Array)
            val bytes = ByteArray(length)
            for (i in 0 until length) {
                bytes[i] = uint8ArrayGet(uint8Array, i).toByte()
            }
            bytes
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Encode bytes to base64
     */
    private fun encodeBase64(data: ByteArray): String {
        val binary = StringBuilder()
        for (byte in data) {
            binary.append((byte.toInt() and 0xFF).toChar())
        }
        return jsStringToKotlin(encodeBase64Js(toJsString(binary.toString()).unsafeCast<JsAny>()))
    }
    
    /**
     * Decode base64 to bytes
     */
    private fun decodeBase64(base64: String): ByteArray {
        val uint8Array = decodeBase64Js(toJsString(base64))
        val length = uint8ArrayLength(uint8Array)
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            bytes[i] = uint8ArrayGet(uint8Array, i).toByte()
        }
        return bytes
    }
    
    /**
     * Generate unique session ID
     */
    private fun generateSessionId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
    
    /**
     * Get unique device ID
     */
    private fun getDeviceId(): String {
        // Try to get from localStorage, or generate new
        val stored = localStorageGetItem(toJsString("cspro_device_id"))
        if (stored != null) return jsStringToKotlin(stored)
        
        val newId = "device_${generateSessionId()}"
        localStorageSetItem(toJsString("cspro_device_id"), toJsString(newId))
        return newId
    }
    
    /**
     * Set connection state listener
     */
    fun setConnectionStateListener(listener: (ConnectionState) -> Unit) {
        onConnectionStateChanged = listener
    }
    
    /**
     * Set command received listener
     */
    fun setCommandReceivedListener(listener: (FileShareCommand) -> Unit) {
        onCommandReceived = listener
    }
    
    /**
     * Disconnect and cleanup
     */
    fun disconnect() {
        dataChannel?.let { dcClose(it) }
        peerConnection?.let { pcClose(it) }
        dataChannel = null
        peerConnection = null
        sessionId = null
        isHost = false
        discoveredPeers.clear()
        println("[FileShare] Disconnected")
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return try {
            val pc = peerConnection ?: return false
            val state = jsStringToKotlin(pcGetConnectionState(pc))
            state == "connected"
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * File share commands matching Android implementation
 */
sealed class FileShareCommand {
    abstract fun toJson(): String
    
    data class MakeDir(val path: String) : FileShareCommand() {
        override fun toJson(): String {
            val obj = createEmptyObject()
            objSetString(obj, toJsString("command"), toJsString("mkdir"))
            objSetString(obj, toJsString("path"), toJsString(path))
            return jsStringToKotlin(jsonStringify(obj))
        }
    }
    
    data class ListDir(val path: String) : FileShareCommand() {
        override fun toJson(): String {
            val obj = createEmptyObject()
            objSetString(obj, toJsString("command"), toJsString("listdir"))
            objSetString(obj, toJsString("path"), toJsString(path))
            return jsStringToKotlin(jsonStringify(obj))
        }
    }
    
    data class PushFiles(
        val paths: List<String>,
        val destinationPath: String
    ) : FileShareCommand() {
        override fun toJson(): String {
            val obj = createEmptyObject()
            objSetString(obj, toJsString("command"), toJsString("push"))
            // Serialize paths as pipe-separated string for simplicity
            objSetString(obj, toJsString("paths"), toJsString(paths.joinToString("|")))
            objSetString(obj, toJsString("destinationPath"), toJsString(destinationPath))
            return jsStringToKotlin(jsonStringify(obj))
        }
    }
    
    data class PullFiles(
        val paths: List<String>,
        val destinationPath: String
    ) : FileShareCommand() {
        override fun toJson(): String {
            val obj = createEmptyObject()
            objSetString(obj, toJsString("command"), toJsString("pull"))
            // Serialize paths as pipe-separated string for simplicity
            objSetString(obj, toJsString("paths"), toJsString(paths.joinToString("|")))
            objSetString(obj, toJsString("destinationPath"), toJsString(destinationPath))
            return jsStringToKotlin(jsonStringify(obj))
        }
    }
    
    data class Delete(val path: String) : FileShareCommand() {
        override fun toJson(): String {
            val obj = createEmptyObject()
            objSetString(obj, toJsString("command"), toJsString("delete"))
            objSetString(obj, toJsString("path"), toJsString(path))
            return jsStringToKotlin(jsonStringify(obj))
        }
    }
    
    companion object {
        fun fromJson(json: String): FileShareCommand {
            val obj = jsonParse(toJsString(json))
            val commandVal = objGet(obj, toJsString("command"))
            val command = if (commandVal != null) jsStringToKotlin(commandVal.unsafeCast<JsString>()) else throw IllegalArgumentException("Missing command")
            
            return when (command) {
                "mkdir" -> {
                    val pathVal = objGet(obj, toJsString("path"))
                    val path = if (pathVal != null) jsStringToKotlin(pathVal.unsafeCast<JsString>()) else ""
                    MakeDir(path)
                }
                "listdir" -> {
                    val pathVal = objGet(obj, toJsString("path"))
                    val path = if (pathVal != null) jsStringToKotlin(pathVal.unsafeCast<JsString>()) else ""
                    ListDir(path)
                }
                "push" -> {
                    val pathsVal = objGet(obj, toJsString("paths"))
                    val pathsStr = if (pathsVal != null) jsStringToKotlin(pathsVal.unsafeCast<JsString>()) else ""
                    val paths = pathsStr.split("|").filter { it.isNotEmpty() }
                    val destVal = objGet(obj, toJsString("destinationPath"))
                    val dest = if (destVal != null) jsStringToKotlin(destVal.unsafeCast<JsString>()) else ""
                    PushFiles(paths, dest)
                }
                "pull" -> {
                    val pathsVal = objGet(obj, toJsString("paths"))
                    val pathsStr = if (pathsVal != null) jsStringToKotlin(pathsVal.unsafeCast<JsString>()) else ""
                    val paths = pathsStr.split("|").filter { it.isNotEmpty() }
                    val destVal = objGet(obj, toJsString("destinationPath"))
                    val dest = if (destVal != null) jsStringToKotlin(destVal.unsafeCast<JsString>()) else ""
                    PullFiles(paths, dest)
                }
                "delete" -> {
                    val pathVal = objGet(obj, toJsString("path"))
                    val path = if (pathVal != null) jsStringToKotlin(pathVal.unsafeCast<JsString>()) else ""
                    Delete(path)
                }
                else -> throw IllegalArgumentException("Unknown command: $command")
            }
        }
    }
}

/**
 * File share result
 */
sealed class FileShareResult {
    data class Success(val message: String) : FileShareResult()
    data class Error(val message: String) : FileShareResult()
    data class DirectoryListing(val entries: List<DirectoryEntry>) : FileShareResult()
}

/**
 * Directory entry
 */
data class DirectoryEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long
)

/**
 * Peer device info
 */
data class PeerDevice(
    val id: String,
    val name: String,
    val sessionId: String?
)

/**
 * Connection state
 */
enum class ConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED
}
