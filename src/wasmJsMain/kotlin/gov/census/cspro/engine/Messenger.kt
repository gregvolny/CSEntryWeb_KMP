/**
 * Messenger.kt
 * 
 * Web port of Android's Messenger.java (409 lines)
 * 
 * Purpose: Engine ? UI communication system
 * 
 * Android Architecture:
 * - Main Thread (UI) ? Worker Thread (Engine) communication via Handler/Looper
 * - Messages queued in LinkedList, processed FIFO
 * - Synchronous engine functions block worker thread until UI completes
 * 
 * Web Architecture:
 * - Main Thread (Kotlin/Wasm + JS) ? C++ Engine (via Emscripten ASYNCIFY)
 * - Uses Kotlin Coroutines + Channels instead of Handler/Looper
 * - async/await pattern replaces blocking waits
 * - No separate worker thread needed (WASM is single-threaded with ASYNCIFY)
 */

package gov.census.cspro.engine

import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Base class for messages sent to the engine thread
 * Replaces: Android's EngineMessage (Runnable)
 */
abstract class EngineMessage {
    var result: Long = 0
    
    /**
     * Execute the message in the engine context
     * This is called from a coroutine scope
     */
    abstract suspend fun run()
    
    /**
     * Called after message completes (on main thread)
     */
    open suspend fun onCompleted() {
        // Override in subclasses if needed
    }
}

/**
 * Interface for message completion listeners
 * Replaces: Android's IEngineMessageCompletedListener
 */
interface IEngineMessageCompletedListener {
    suspend fun onMessageCompleted(message: EngineMessage)
}

/**
 * Message with completion listener
 */
abstract class CallbackEngineMessage(
    private val listener: IEngineMessageCompletedListener?
) : EngineMessage() {
    
    override suspend fun onCompleted() {
        listener?.onMessageCompleted(this)
    }
}

/**
 * Engine function - runs synchronously on UI thread, returns value to engine
 * Replaces: Android's EngineFunction
 */
abstract class EngineFunction<T> {
    private val completionDeferred = CompletableDeferred<T>()
    
    /**
     * Execute the function on UI thread
     */
    abstract suspend fun run(): T
    
    /**
     * Complete the function and unblock engine thread
     */
    fun complete(value: T) {
        completionDeferred.complete(value)
    }
    
    /**
     * Wait for function completion
     */
    suspend fun await(): T {
        return completionDeferred.await()
    }
}

/**
 * Web Messenger - Engine ? UI Communication
 * 
 * Replaces: Android's Messenger.java (409 lines)
 * 
 * Key differences from Android:
 * - Uses Kotlin Channels instead of LinkedList<EngineMessage>
 * - Uses Coroutines instead of Thread + Handler
 * - No Activity lifecycle tracking (web has single page)
 * - No ActivityResultLauncher (web uses different patterns)
 */
object Messenger {
    
    private val messageChannel = Channel<EngineMessage>(Channel.UNLIMITED)
    private var currentMessage: EngineMessage? = null
    private var isRunning = false
    
    // Coroutine scope for the engine message loop
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // UI scope for callbacks
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // State flow for observing current activity
    private val _currentActivity = MutableStateFlow<String?>(null)
    val currentActivity: StateFlow<String?> = _currentActivity
    
    /**
     * Initialize the messenger and start the message loop
     * Replaces: CreateMessengerInstance() + Thread.start()
     */
    fun initialize() {
        if (isRunning) return
        
        isRunning = true
        println("[Messenger] Starting message loop")
        
        engineScope.launch {
            processMessages()
        }
    }
    
    /**
     * Main message processing loop
     * Replaces: Android's run() method with while(!m_stop) loop
     */
    private suspend fun processMessages() {
        println("[Messenger] Message loop started")
        
        while (isRunning) {
            try {
                // Wait for next message (suspends until available)
                currentMessage = messageChannel.receive()
                
                currentMessage?.let { msg ->
                    println("[Messenger] Processing message: ${msg::class.simpleName}")
                    
                    try {
                        // Execute the message
                        msg.run()
                        
                        // Notify completion on UI thread
                        uiScope.launch {
                            msg.onCompleted()
                        }
                        
                        println("[Messenger] Message completed: ${msg::class.simpleName}")
                    } catch (e: Exception) {
                        println("[Messenger] Error processing message: $e")
                    }
                }
                
            } catch (e: CancellationException) {
                println("[Messenger] Message loop cancelled")
                break
            } catch (e: Exception) {
                println("[Messenger] Error in message loop: $e")
            }
        }
        
        println("[Messenger] Message loop stopped")
    }
    
    /**
     * Send a message to the engine thread
     * Replaces: Android's sendMessage(EngineMessage)
     * 
     * @param message The message to process
     */
    suspend fun sendMessage(message: EngineMessage) {
        println("[Messenger] Queuing message: ${message::class.simpleName}")
        messageChannel.send(message)
    }
    
    /**
     * Send a message without suspending (fire and forget)
     */
    fun postMessage(message: EngineMessage) {
        engineScope.launch {
            sendMessage(message)
        }
    }
    
    /**
     * Run an engine function on the UI thread (synchronous from engine's perspective)
     * 
     * Replaces: Android's runLongEngineFunction() / runStringEngineFunction()
     * 
     * In Android: Engine thread blocks, UI thread runs function, signals complete
     * In Web: Uses async/await with CompletableDeferred for synchronization
     * 
     * @param function The function to execute on UI thread
     * @return The function's return value
     */
    suspend fun <T> runEngineFunctionOnUIThread(function: EngineFunction<T>): T {
        println("[Messenger] Running engine function on UI thread: ${function::class.simpleName}")
        
        // Switch to UI dispatcher
        return withContext(Dispatchers.Main) {
            try {
                val result = function.run()
                function.complete(result)
                println("[Messenger] Engine function completed: ${function::class.simpleName}")
                result
            } catch (e: Exception) {
                println("[Messenger] Error in engine function: $e")
                throw e
            }
        }
    }
    
    /**
     * Run a function that returns a Long
     * Replaces: Android's runLongEngineFunction()
     */
    suspend fun runLongEngineFunction(function: EngineFunction<Long>): Long {
        return runEngineFunctionOnUIThread(function)
    }
    
    /**
     * Run a function that returns a String
     * Replaces: Android's runStringEngineFunction()
     */
    suspend fun runStringEngineFunction(function: EngineFunction<String>): String {
        return runEngineFunctionOnUIThread(function)
    }
    
    /**
     * Run a function that returns a Boolean
     */
    suspend fun runBooleanEngineFunction(function: EngineFunction<Boolean>): Boolean {
        return runEngineFunctionOnUIThread(function)
    }
    
    /**
     * Get the current message being processed
     * Replaces: Android's getCurrentMessage()
     */
    fun getCurrentMessage(): EngineMessage? = currentMessage
    
    /**
     * Stop the messenger
     * Replaces: Android's stop()
     */
    fun stop() {
        println("[Messenger] Stopping messenger")
        isRunning = false
        messageChannel.close()
        engineScope.cancel()
        uiScope.cancel()
    }
    
    /**
     * Set the current activity context
     * In web, this is just a string identifier (page name)
     */
    fun setCurrentActivity(activityName: String?) {
        _currentActivity.value = activityName
    }
}

/**
 * Specific message types (examples to replace Android versions)
 */

/**
 * Message to initialize application
 */
class InitApplicationMessage(
    private val pffFilename: String,
    listener: IEngineMessageCompletedListener? = null
) : CallbackEngineMessage(listener) {
    
    var success = false
    
    override suspend fun run() {
        val engineService = CSProEngineService.getInstance()
        success = engineService?.openApplication(pffFilename) ?: false
        result = if (success) 1 else 0
    }
}

/**
 * Message to move to next field
 */
class NextFieldMessage(
    listener: IEngineMessageCompletedListener? = null
) : CallbackEngineMessage(listener) {
    
    override suspend fun run() {
        val engineService = CSProEngineService.getInstance()
        engineService?.nextField()
        // nextField is a Unit function, so we just mark success
        result = 1
    }
}

/**
 * Message to save case
 */
class SaveCaseMessage(
    listener: IEngineMessageCompletedListener? = null
) : CallbackEngineMessage(listener) {
    
    override suspend fun run() {
        val engineService = CSProEngineService.getInstance()
        engineService?.savePartial()
        // savePartial is a Unit function, so we just mark success
        result = 1
    }
}

/**
 * Message to start data entry
 */
class StartDataEntryMessage(
    listener: IEngineMessageCompletedListener? = null
) : CallbackEngineMessage(listener) {
    
    var success = false
    
    override suspend fun run() {
        val engineService = CSProEngineService.getInstance()
        success = engineService?.start() ?: false
        result = if (success) 1 else 0
    }
}

/**
 * Engine function to show a dialog
 * Returns: User's choice (button index)
 */
class ShowDialogFunction(
    private val title: String,
    private val message: String,
    private val buttons: List<String>
) : EngineFunction<Long>() {
    
    override suspend fun run(): Long {
        // This would be implemented by the UI layer
        // For now, return default choice
        println("[ShowDialog] $title: $message")
        
        // In real implementation, this would:
        // 1. Display HTML dialog
        // 2. Wait for user interaction
        // 3. Return button index
        
        return 0 // OK button
    }
}

/**
 * Engine function to show a prompt
 * Returns: User's input string
 */
class ShowPromptFunction(
    private val title: String,
    private val message: String,
    private val defaultValue: String = ""
) : EngineFunction<String>() {
    
    override suspend fun run(): String {
        println("[ShowPrompt] $title: $message")
        
        // In real implementation, this would:
        // 1. Display HTML input dialog
        // 2. Wait for user input
        // 3. Return input value
        
        return defaultValue
    }
}

/**
 * Engine function to get GPS coordinates
 * Returns: Coordinate string "lat,lon,accuracy"
 */
class GetGPSFunction(
    private val accuracy: Int,
    private val waitTime: Int
) : EngineFunction<String>() {
    
    override suspend fun run(): String {
        println("[GetGPS] Requesting GPS (accuracy: $accuracy, wait: $waitTime)")
        
        // This would use Web Geolocation API via WasmPlatformServices
        // For now, return empty string
        
        return ""
    }
}

/**
 * Helper extension to run a suspend function as an EngineMessage
 */
fun CoroutineScope.launchEngineMessage(
    listener: IEngineMessageCompletedListener? = null,
    block: suspend () -> Unit
) {
    val message = object : CallbackEngineMessage(listener) {
        override suspend fun run() {
            block()
        }
    }
    
    launch {
        Messenger.sendMessage(message)
    }
}


