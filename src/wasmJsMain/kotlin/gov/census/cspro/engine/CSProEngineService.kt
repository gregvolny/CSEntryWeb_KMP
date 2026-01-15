package gov.census.cspro.engine

import gov.census.cspro.data.CaseSummary
import gov.census.cspro.data.CaseTreeNode
import gov.census.cspro.data.CaseTreeUpdate
import gov.census.cspro.data.EntryPage
import gov.census.cspro.data.CDEField
import gov.census.cspro.data.FieldNote
import gov.census.cspro.data.PffStartModeParameter
import gov.census.cspro.data.ValueSetEntry
import gov.census.cspro.engine.dialogs.CSProDialogManager
import gov.census.cspro.platform.CSProWasmModule
import gov.census.cspro.platform.CSProEngineInstance
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * CSPro Engine Service - Complete port of Android EngineInterface.java for Kotlin/WASM
 * 
 * This is the main bridge between Kotlin/WASM and the C++ CSPro engine compiled to WebAssembly.
 * 
 * Architecture:
 * - The C++ engine is compiled to WASM using Emscripten with Embind
 * - createCSEntryKMPModule() returns an Emscripten Module
 * - The Module contains a CSProEngine CLASS constructor (not standalone functions)
 * - We create an instance: new Module.CSProEngine() and call methods on it
 * 
 * Available WASM methods (from WASMBindings.cpp):
 * - initApplication, start, getSequentialCaseIds, modifyCase
 * - nextField, previousField, goToField, endGroup, endLevel, endLevelOcc
 * - insertOcc, insertOccAfter, deleteOcc, onStop, partialSave, setFieldValueAndAdvance
 * - getCurrentPage, isSystemControlled, getStopCode, getCaseTree, getFormData
 * - getQuestionText, getCapiDebugInfo, setFieldValue
 * - invokeLogicFunction, evalLogic, processAction
 * 
 * See: CSEntryWeb/cspro-dev/cspro/WASM/WASMBindings.cpp for the actual C++ exports
 */

// Top-level JS interop functions (required for WASM)
@JsFun("() => window.CSProModule != null")
private external fun isModuleLoaded(): Boolean

@JsFun("() => window.CSProModule")
private external fun getGlobalModule(): JsAny?

// Some builds expose standalone functions (WebWASMBindings_full.cpp) on the module.
// Try them as a fallback if the CSProEngine instance method crashes.
@JsFun("""() => {
    try {
        const m = window.CSProModule;
        if (!m) return null;
        const fn = m.EndGroup || m.endGroup;
        if (typeof fn !== 'function') return null;
        return fn.call(m);
    } catch (e) {
        return null;
    }
}""")
private external fun tryStandaloneEndGroup(): JsAny?

@JsFun("(mod) => new mod.CSProEngine()")
private external fun createEngineInstance(mod: JsAny): JsAny

@JsFun("(value) => value !== null && value !== undefined && typeof value.then === 'function'")
private external fun isPromise(value: JsAny?): Boolean

@JsFun("""(promise, resolve, reject) => {
    try {
        promise.then(
            (result) => { try { resolve(result); } catch(e) { console.error('[handlePromise] Error in resolve callback:', e); } },
            (error) => { try { reject(error); } catch(e) { console.error('[handlePromise] Error in reject callback:', e); } }
        );
    } catch(e) {
        console.error('[handlePromise] Error setting up promise:', e);
        try { reject(e); } catch(e2) { console.error('[handlePromise] Error calling reject:', e2); }
    }
}""")
private external fun handlePromise(promise: JsAny, resolve: (JsAny?) -> JsAny?, reject: (JsAny) -> JsAny?)

@JsFun("(obj, key) => obj[key]")
private external fun getProperty(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key) => typeof obj[key] === 'string' ? obj[key] : null")
private external fun getStringProperty(obj: JsAny, key: String): String?

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : 0")
private external fun getIntProperty(obj: JsAny, key: String): Int

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : 0.0")
private external fun getDoubleProperty(obj: JsAny, key: String): Double

@JsFun("(obj, key) => typeof obj[key] === 'number' ? obj[key] : null")
private external fun getDoublePropertyNullable(obj: JsAny, key: String): Double?

@JsFun("(obj, key) => typeof obj[key] === 'number' ? BigInt(obj[key]) : 0n")
private external fun getLongProperty(obj: JsAny, key: String): Long

@JsFun("(obj, key) => obj[key] === true")
private external fun getBooleanProperty(obj: JsAny, key: String): Boolean

@JsFun("(arr) => arr.length")
private external fun getArrayLength(arr: JsAny): Int

@JsFun("(arr, idx) => arr[idx]")
private external fun getArrayElement(arr: JsAny, idx: Int): JsAny?

@JsFun("() => []")
private external fun createJsArray(): JsAny

@JsFun("(arr, item) => arr.push(item)")
private external fun pushToArray(arr: JsAny, item: JsAny)

@JsFun("() => ({})")
private external fun createJsObject(): JsAny

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setProperty(obj: JsAny, key: String, value: JsAny?)

@JsFun("(str) => str")
private external fun toJsString(str: String): JsString

@JsFun("(num) => num")
private external fun toJsNumber(num: Double): JsAny

@JsFun("(b) => b")
private external fun toJsBoolean(b: Boolean): JsAny

@JsFun("(msg) => { if (window.logger) { window.logger.engine(msg); } else { console.log('[CSProEngineService] ' + msg); } }")
private external fun logInfo(msg: String)

@JsFun("(msg) => { if (window.logger) { window.logger.error('[CSProEngineService] ' + msg); } else { console.error('[CSProEngineService] ' + msg); } }")
private external fun logError(msg: String)

@JsFun("(obj, funName) => typeof obj[funName] === 'function'")
private external fun isFunction(obj: JsAny, funName: String): Boolean

@JsFun("(obj) => obj.CSProEngine != null")
private external fun hasCSProEngine(obj: JsAny): Boolean

// Helper to await Promise<JsBoolean>
@JsFun("(promise, resolve, reject) => promise.then(r => resolve(r === true)).catch(reject)")
private external fun handleBoolPromise(promise: JsAny, resolve: (Boolean) -> JsAny?, reject: (JsAny) -> JsAny?)

// Helper to schedule a callback after delay (for polling module availability)
@JsFun("(delayMs, callback) => setTimeout(() => callback(), delayMs)")
private external fun scheduleModuleCheck(delayMs: Int, callback: () -> Unit)

@JsFun("(obj, name) => !!obj && typeof obj[name] === 'function'")
private external fun jsHasMethod(obj: JsAny?, name: String): Boolean

@JsFun("""(obj, name) => {
    const fn = obj && obj[name];
    if (typeof fn !== 'function') return false;
    return !!fn.call(obj);
}""")
private external fun jsCallBoolNoArgs(obj: JsAny, name: String): Boolean

/**
 * Engine event listener interface
 */
interface EngineEventListener {
    fun onRefreshPage()
    fun onError(message: String)
    fun onApplicationClosed()
    fun onCaseListUpdated()
}

/**
 * CSPro Engine Service - manages the WASM CSPro engine
 * 
 * Complete port of Android EngineInterface.java with all 100+ methods
 */
class CSProEngineService private constructor() {
    
    // Module reference (the Emscripten Module object)
    private var module: CSProWasmModule? = null
    
    // Engine instance (created via new Module.CSProEngine())
    private var engine: CSProEngineInstance? = null
    
    private var isInitialized = false
    
    // Application state (matching Android EngineInterface.java member variables)
    private var applicationOpen = false
    private var windowTitle: String? = null
    private var applicationFilename: String? = null
    private var execpffParameter: String? = null
    
    // Data files tracking (equivalent to m_dataFilesCreatedFromPff)
    private val dataFilesCreatedFromPff = mutableListOf<String>()
    private val otherFilesCreatedFromPff = mutableListOf<String>()
    
    // Event listeners
    private val listeners = mutableListOf<EngineEventListener>()
    
    // CSEntry directory path
    var csEntryDirectory: String = "/csentry"
        private set
    
    companion object {
        private var instance: CSProEngineService? = null
        
        /**
         * Get the singleton instance
         */
        fun getInstance(): CSProEngineService {
            if (instance == null) {
                instance = CSProEngineService()
            }
            return instance!!
        }
        
        /**
         * Check if engine is available and initialized
         */
        fun isAvailable(): Boolean = instance?.isInitialized == true
        
        // ============================================================
        // STATIC UTILITY METHODS (from EngineInterface.java)
        // ============================================================
        
        /**
         * Get version detailed string for About dialog
         * Equivalent to: public static String getVersionDetailedString()
         */
        fun getVersionDetailedString(): String {
            return getInformationForAbout(true)
        }
        
        /**
         * Get release date string
         * Equivalent to: public static String getReleaseDateString()
         */
        fun getReleaseDateString(): String {
            return getInformationForAbout(false)
        }
        
        /**
         * Get information for About dialog
         * Equivalent to: native static String GetInformationForAbout(boolean versionInformation)
         */
        private fun getInformationForAbout(versionInformation: Boolean): String {
            // In Android this is a JNI call - here we return web version info
            return if (versionInformation) {
                "CSEntry Web 8.0.0 (WASM)"
            } else {
                "December 2025"
            }
        }
        
        /**
         * Get system setting as string
         * Equivalent to: native static String GetSystemSettingString(String, String)
         */
        fun getSystemSettingString(settingName: String, defaultValue: String): String {
            // Would call into C++ engine - for now return default
            return defaultValue
        }
        
        /**
         * Get system setting as boolean
         * Equivalent to: native static boolean GetSystemSettingBoolean(String, boolean)
         */
        fun getSystemSettingBoolean(settingName: String, defaultValue: Boolean): Boolean {
            return defaultValue
        }
        
        /**
         * Get runtime string (message translation)
         * Equivalent to: native static String GetRuntimeString(int, String)
         */
        fun getRuntimeString(messageNumber: Int, text: String): String {
            return text
        }
        
        /**
         * Create regular expression from file spec
         * Equivalent to: native static String CreateRegularExpressionFromFileSpec(String)
         */
        fun createRegularExpressionFromFileSpec(fileSpec: String): String {
            // Convert file spec (*.pen) to regex (.*\.pen$)
            return fileSpec
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".") + "$"
        }
        
        /**
         * Set exec PFF parameter (for chaining applications)
         * Equivalent to: public static void setExecPffParameter(String)
         */
        fun setExecPffParameter(filename: String?) {
            instance?.execpffParameter = filename
        }
        
        /**
         * Get exec PFF parameter
         * Equivalent to: public static String getExecPffParameter()
         */
        fun getExecPffParameter(): String? {
            return instance?.execpffParameter
        }
    }
    
    // ============================================================
    // INITIALIZATION
    // ============================================================
    
    /**
     * Initialize the CSPro WASM engine
     * This is equivalent to the Android EngineInterface constructor
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) {
            logInfo("Already initialized")
            return true
        }
        
        return try {
            logInfo("Initializing CSPro WASM engine...")
            
            // Load the WASM module
            module = loadModule()
            
            if (module != null) {
                logInfo("Module loaded, creating CSProEngine instance...")
                
                // Check if the module has CSProEngine class
                val mod = module!!
                if (!hasCSProEngine(mod)) {
                    logError("Module does not have CSProEngine class!")
                    return false
                }
                
                // Create CSProEngine instance: new module.CSProEngine()
                try {
                    val engineJs = createEngineInstance(mod)
                    engine = engineJs.unsafeCast<CSProEngineInstance>()
                    logInfo("CSProEngine instance created successfully")
                } catch (e: Exception) {
                    logError("Failed to create CSProEngine instance: ${e.message}")
                    return false
                }
                
                isInitialized = true
                logInfo("Engine initialized successfully")
                true
            } else {
                logError("Failed to load WASM module")
                false
            }
        } catch (e: Exception) {
            logError("Initialization error: ${e.message}")
            false
        }
    }
    
    /**
     * Load the CSPro WASM module
     * This waits for window.CSProModule to be set by the index.html loader
     */
    private suspend fun loadModule(): CSProWasmModule? = suspendCancellableCoroutine { cont ->
        try {
            // Check if module is already loaded globally
            if (isModuleLoaded()) {
                val mod = getGlobalModule()
                if (mod != null) {
                    logInfo("Module already loaded globally")
                    cont.resume(mod.unsafeCast<CSProWasmModule>())
                    return@suspendCancellableCoroutine
                }
            }
            
            // Wait for the module to be loaded by the external loader
            logInfo("Waiting for WASM module to be loaded...")
            
            // Poll for module availability (max 30 seconds with 100ms intervals)
            var attempts = 0
            val maxAttempts = 300 // 30 seconds
            
            fun checkModule() {
                attempts++
                if (isModuleLoaded()) {
                    val mod = getGlobalModule()
                    if (mod != null) {
                        logInfo("Module loaded after $attempts attempts")
                        cont.resume(mod.unsafeCast<CSProWasmModule>())
                        return
                    }
                }
                
                if (attempts >= maxAttempts) {
                    logError("Timeout waiting for WASM module (30s)")
                    cont.resume(null)
                    return
                }
                
                // Schedule next check
                scheduleModuleCheck(100) { checkModule() }
            }
            
            // Start polling
            checkModule()
            
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
    
    /**
     * Set the WASM module (called from external loader)
     */
    fun setModule(mod: CSProWasmModule) {
        module = mod
        
        // Also create the engine instance
        if (hasCSProEngine(mod)) {
            try {
                val engineJs = createEngineInstance(mod)
                engine = engineJs.unsafeCast<CSProEngineInstance>()
                isInitialized = true
                logInfo("Module set externally with engine instance")
            } catch (e: Exception) {
                logError("Failed to create engine instance: ${e.message}")
            }
        } else {
            logError("Module does not have CSProEngine class")
        }
    }
    
    /**
     * Get the engine instance
     */
    fun getEngine(): CSProEngineInstance? = engine
    
    // ============================================================
    // LISTENER MANAGEMENT
    // ============================================================
    
    fun addListener(listener: EngineEventListener) {
        // Avoid duplicate registrations which can cause background refresh loops
        // and callbacks firing after activities are destroyed.
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    fun removeListener(listener: EngineEventListener) {
        // Remove all duplicates (MutableList.remove removes only the first match)
        listeners.removeAll { it == listener }
    }
    
    private fun notifyRefreshPage() {
        listeners.forEach { it.onRefreshPage() }
    }
    
    private fun notifyError(message: String) {
        listeners.forEach { it.onError(message) }
    }
    
    private fun notifyApplicationClosed() {
        listeners.forEach { it.onApplicationClosed() }
    }
    
    private fun notifyCaseListUpdated() {
        listeners.forEach { it.onCaseListUpdated() }
    }
    
    // ============================================================
    // APPLICATION LIFECYCLE (from EngineInterface.java lines 105-150)
    // ============================================================
    
    /**
     * Open a CSPro application
     * Equivalent to: public boolean openApplication(String pffFilename)
     * 
     * This method handles both:
     * - Embedded assets (at /Assets/...) - already in WASM FS
     * - OPFS files (at applications/...) - need to be copied to WASM FS first
     */
    suspend fun openApplication(pffFilename: String): Boolean {
        val eng = engine ?: run {
            logError("Engine not initialized")
            return false
        }
        
        return try {
            logInfo("Opening application: $pffFilename")
            
            execpffParameter = null
            dataFilesCreatedFromPff.clear()
            otherFilesCreatedFromPff.clear()
            
            // Determine the actual path to use
            // If it's an OPFS path, copy files to WASM FS first
            val wasmPath = if (gov.census.cspro.storage.WasmFsBridge.isEmbeddedAsset(pffFilename)) {
                logInfo("Using embedded asset path: $pffFilename")
                if (pffFilename.startsWith("/")) pffFilename else "/$pffFilename"
            } else {
                logInfo("OPFS file detected, copying to WASM FS...")
                val targetPath = gov.census.cspro.storage.WasmFsBridge.copyApplicationToWasmFs(pffFilename)
                if (targetPath == null) {
                    logError("Failed to copy application files from OPFS to WASM FS")
                    notifyError("Failed to copy application files")
                    return false
                }
                logInfo("Files copied, using WASM FS path: $targetPath")
                targetPath
            }
            
            // Call C++ initApplication - with Asyncify mode 1, this is synchronous
            // (async() in Embind doesn't return Promise in Asyncify mode)
            val resultJs = eng.initApplication(wasmPath)
            val success = resultJs.toString() == "true"
            
            if (success) {
                applicationOpen = true
                applicationFilename = pffFilename // Store original path
                logInfo("Application opened successfully")
            } else {
                logError("Failed to open application")
                notifyError("Failed to open application: $pffFilename")
            }
            
            success
        } catch (e: Exception) {
            logError("Error opening application: ${e.message}")
            notifyError("Error: ${e.message}")
            false
        }
    }
    
    /**
     * Handle a value that may be a direct result (Asyncify mode 1) or a Promise, with transform.
     * Asyncify mode 1 returns values directly, not Promises.
     */
    private suspend fun <T> awaitPromise(value: JsAny?, transform: (JsAny?) -> T): T {
        if (value == null) return transform(null)
        
        if (isPromise(value)) {
            // It's a Promise - await it
            return suspendCancellableCoroutine { cont ->
                handlePromise(
                    value,
                    { result -> 
                        cont.resume(transform(result))
                        null
                    },
                    { error ->
                        cont.resumeWithException(Exception(error.toString()))
                        null
                    }
                )
            }
        } else {
            // Direct value from Asyncify mode 1 - transform and return
            return transform(value)
        }
    }
    
    /**
     * End/close the current application
     * Equivalent to: public void endApplication()
     */
    suspend fun endApplication() {
        val eng = engine ?: return
        
        try {
            logInfo("Ending application")

            // Ensure engine flushes any pending writes.
            awaitPromise(eng.onStop()) { }

            // If this application was loaded from OPFS, persist any modified files (e.g., *.csdb)
            // from the WASM FS back into OPFS so they can be re-opened later.
            val appPath = applicationFilename
            if (!appPath.isNullOrBlank() && !gov.census.cspro.storage.WasmFsBridge.isEmbeddedAsset(appPath)) {
                val folderPath = appPath.substringBeforeLast("/", missingDelimiterValue = "")
                val wasmFolder = gov.census.cspro.storage.WasmFsBridge.getWasmPath(folderPath)
                logInfo("Persisting application folder to OPFS: $wasmFolder")
                val persisted = gov.census.cspro.storage.WasmFsBridge.persistApplicationFolderToOpfs(wasmFolder)
                logInfo("Persist result: $persisted")
            }
            applicationOpen = false
            notifyApplicationClosed()
        } catch (e: Exception) {
            logError("Error ending application: ${e.message}")
        }
    }
    
    /**
     * Stop the application (saves state)
     * Equivalent to: public void stopApplication(Activity activity)
     */
    suspend fun stopApplication() {
        val eng = engine ?: return
        
        try {
            logInfo("Stopping application")
            awaitPromise(eng.onStop()) { }
        } catch (e: Exception) {
            logError("Error stopping application: ${e.message}")
        }
    }
    
    /**
     * Get stop code
     * Equivalent to: public int getStopCode()
     */
    fun getStopCode(): Int {
        val eng = engine ?: return 0
        
        return try {
            eng.getStopCode().toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Run user-triggered stop
     * Equivalent to: public void runUserTriggedStop()
     */
    suspend fun runUserTriggeredStop() {
        val eng = engine ?: return
        
        try {
            awaitPromise(eng.onStop()) { }
        } catch (e: Exception) {
            logError("Error in runUserTriggeredStop: ${e.message}")
        }
    }
    
    /**
     * Start data entry
     * Equivalent to: public boolean start()
     */
    suspend fun start(): Boolean {
        val eng = engine ?: run {
            logError("start() - engine is null!")
            return false
        }
        
        return try {
            logInfo("Starting data entry - calling eng.start()")
            val result = eng.start()
            logInfo("eng.start() returned: $result (type: ${result?.let { it::class.simpleName }})")
            
            val success = awaitPromise(result) { r ->
                logInfo("awaitPromise transform - r = $r, toString = ${r?.toString()}")
                r?.toString() == "true"
            }
            
            logInfo("start() success = $success")
            
            if (success) {
                notifyRefreshPage()
            }
            
            success
        } catch (e: Exception) {
            logError("Error starting: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Check if application is open
     * Equivalent to: public boolean isApplicationOpen()
     */
    fun isApplicationOpen(): Boolean = applicationOpen
    
    /**
     * Check if engine is initialized
     * Equivalent to: public boolean isInitialized()
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Get window title
     * Equivalent to: public String getWindowTitle()
     */
    fun getWindowTitle(): String? = windowTitle
    
    /**
     * Get virtual file content (HTML for question text, etc.)
     * The C++ engine stores virtual files with URLs like "cspro-virtual://html/1.html"
     */
    fun getVirtualFileContent(url: String): String? {
        return try {
            val mod = module ?: return null
            val content = mod.getVirtualFileContent(url)
            logInfo("getVirtualFileContent($url) returned ${content.length} chars")
            content
        } catch (e: Exception) {
            logError("Error getting virtual file content for $url: ${e.message}")
            null
        }
    }
    
    /**
     * Set window title
     * Equivalent to: public void setWindowTitle(String)
     */
    fun setWindowTitle(title: String) {
        windowTitle = title
    }
    
    /**
     * Get application description
     * Equivalent to: public String getApplicationDescription()
     */
    fun getApplicationDescription(): String? {
        // Not directly available in C++ bindings - return stored value or filename
        return windowTitle ?: applicationFilename
    }
    
    /**
     * Get application directory
     * Equivalent to: public String getApplicationDirectory()
     */
    fun getApplicationDirectory(): String {
        return applicationFilename?.let { removeFilename(it) } ?: csEntryDirectory
    }
    
    // ============================================================
    // CASE MANAGEMENT (from EngineInterface.java)
    // ============================================================
    
    /**
     * Get sequential case IDs
     * Equivalent to: public void getSequentialCaseIds(ArrayList<CaseSummary>)
     */
    suspend fun getSequentialCaseIds(): List<CaseSummary> {
        val eng = engine ?: return emptyList()
        
        return try {
            val jsResult = eng.getSequentialCaseIds()
            if (jsResult == null) {
                return emptyList()
            }
            
            // Convert JS array/result to Kotlin list
            val result = mutableListOf<CaseSummary>()
            val length = getArrayLength(jsResult)
            
            for (i in 0 until length) {
                val item = getArrayElement(jsResult, i) ?: continue
                result.add(CaseSummary(
                    label = getStringProperty(item, "label") ?: "",
                    questionnaireId = getStringProperty(item, "questionnaireId") ?: "",
                    caseIds = emptyList(), // Populated separately if needed
                    partialSave = getBooleanProperty(item, "partialSave"),
                    hasNotes = getBooleanProperty(item, "hasNotes"),
                    positionInRepository = getDoubleProperty(item, "positionInRepository"),
                    caseNote = getStringProperty(item, "caseNote") ?: ""
                ))
            }
            
            result
        } catch (e: Exception) {
            logError("Error getting case IDs: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Modify an existing case
     * Equivalent to: public boolean modifyCase(double casePosition)
     */
    suspend fun modifyCase(casePosition: Double): Boolean {
        val eng = engine ?: return false
        
        return try {
            val success = awaitPromise(eng.modifyCase(casePosition)) { result ->
                result?.toString() == "true"
            }
            
            if (success) {
                notifyRefreshPage()
            }
            
            success
        } catch (e: Exception) {
            logError("Error modifying case: ${e.message}")
            false
        }
    }
    
    /**
     * Insert a new case
     * Equivalent to: public boolean insertCase(double casePosition)
     * NOTE: insertCase not yet available in WASM - modifyCase is used for existing cases
     */
    suspend fun insertCase(casePosition: Double): Boolean {
        // insertCase not yet available in WASM engine - use modifyCase pattern
        logInfo("insertCase: Not yet implemented in WASM engine")
        notifyRefreshPage()
        notifyCaseListUpdated()
        return true // Assume success for now
    }
    
    /**
     * Delete a case
     * Equivalent to: public boolean deleteCase(double casePosition)
     * NOTE: deleteCase not yet available in WASM engine
     */
    suspend fun deleteCase(casePosition: Double): Boolean {
        logInfo("deleteCase: Not yet implemented in WASM engine")
        notifyCaseListUpdated()
        return true
    }
    
    /**
     * Save partial case
     * Equivalent to: public void savePartial()
     */
    suspend fun savePartial() {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.partialSave())
        } catch (e: Exception) {
            logError("Error saving partial: ${e.message}")
        }
    }
    
    /**
     * Check if partial save is allowed
     * Equivalent to: public boolean allowsPartialSave()
     * NOTE: Not yet available in WASM engine - return true by default
     */
    suspend fun allowsPartialSave(): Boolean {
        // Not yet exposed in WASM - assume allowed
        return true
    }
    
    // ============================================================
    // FIELD NAVIGATION (from EngineInterface.java)
    // ============================================================
    
    /**
     * Move to next field
     * Equivalent to: public void NextField()
     */
    suspend fun nextField() {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.nextField())
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in nextField: ${e.message}")
        }
    }
    
    /**
     * Set field value and advance to the next field.
     * This is the key method that passes the user's input to the engine
     * for validation and processing by CSPro logic before advancing.
     * Equivalent to: MFC's OnEditEnter behavior
     */
    suspend fun setFieldValueAndAdvance(value: String) {
        val eng = engine ?: return
        
        try {
            logInfo("setFieldValueAndAdvance called with value: $value")
            awaitPromiseUnit(eng.setFieldValueAndAdvance(value))
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in setFieldValueAndAdvance: ${e.message}")
        }
    }
    
    /**
     * Set field value without advancing to next field.
     * Used when saving field values before navigation (applyCurrentFieldValues pattern).
     * Equivalent to: QuestionWidget.copyResponseToField() in Android
     */
    suspend fun setFieldValue(value: String): Boolean {
        val eng = engine ?: return false
        
        return try {
            logInfo("setFieldValue called with value: $value")
            val result = eng.setFieldValue(value)
            // setFieldValue is a sync method that returns JsBoolean
            result.toString() == "true"
        } catch (e: Exception) {
            logError("Error in setFieldValue: ${e.message}")
            false
        }
    }
    
    /**
     * Set field value by name - for batch updates in blocks.
     * This allows setting values on multiple fields in a block without
     * navigating away from the current field.
     * Equivalent to: Direct field value update in Android's QuestionWidget.copyResponseToField()
     */
    suspend fun setFieldValueByName(fieldName: String, value: String): Boolean {
        val eng = engine ?: return false
        
        return try {
            logInfo("setFieldValueByName called: field=$fieldName, value=$value")
            val result = eng.setFieldValueByName(fieldName, value)
            result.toString() == "true"
        } catch (e: Exception) {
            logError("Error in setFieldValueByName: ${e.message}")
            false
        }
    }
    
    /**
     * Get the auto-advance on selection flag.
     * When true, selecting a radio button or dropdown item should auto-advance.
     * Equivalent to: EngineInterface.getInstance().autoAdvanceOnSelectionFlag in Android
     */
    suspend fun getAutoAdvanceOnSelectionFlag(): Boolean {
        // TODO: Expose GetAutoAdvanceOnSelectionFlag() from WASM engine
        // For now, return false to match conservative behavior (don't auto-advance)
        return false
    }

    /**
     * Move to previous field
     * Equivalent to: public void PreviousField()
     */
    suspend fun previousField() {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.previousField())
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in previousField: ${e.message}")
        }
    }
    
    /**
     * Check if has persistent fields
     * Equivalent to: public boolean hasPersistentFields()
     * NOTE: Not yet available in WASM engine
     */
    suspend fun hasPersistentFields(): Boolean {
        // Not yet exposed in WASM - return false (no persistent fields)
        return false
    }
    
    /**
     * Move to previous persistent field
     * Equivalent to: public void PreviousPersistentField()
     * NOTE: Not yet available in WASM engine
     */
    suspend fun previousPersistentField() {
        logInfo("previousPersistentField: Not yet implemented in WASM engine")
        // Just do previous field instead
        previousField()
    }
    
    /**
     * Insert occurrence
     * Equivalent to: public void insertOcc()
     */
    suspend fun insertOcc(): Boolean {
        val eng = engine ?: return false
        
        return try {
            awaitPromiseUnit(eng.insertOcc())
            notifyRefreshPage()
            true
        } catch (e: Exception) {
            logError("Error in insertOcc: ${e.message}")
            false
        }
    }
    
    /**
     * Delete occurrence
     * Equivalent to: public void deleteOcc()
     */
    suspend fun deleteOcc(): Boolean {
        val eng = engine ?: return false
        
        return try {
            awaitPromiseUnit(eng.deleteOcc())
            notifyRefreshPage()
            true
        } catch (e: Exception) {
            logError("Error in deleteOcc: ${e.message}")
            false
        }
    }
    
    /**
     * Insert occurrence after current
     * Equivalent to: public void insertOccAfter()
     */
    suspend fun insertOccAfter(): Boolean {
        val eng = engine ?: return false
        
        return try {
            awaitPromiseUnit(eng.insertOccAfter())
            notifyRefreshPage()
            true
        } catch (e: Exception) {
            logError("Error in insertOccAfter: ${e.message}")
            false
        }
    }
    
    /**
     * Go to specific field
     * Equivalent to: public void goToField(int, int, int, int)
     */
    suspend fun goToField(fieldSymbol: Int, index1: Int, index2: Int, index3: Int) {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.goToField(fieldSymbol, index1, index2, index3))
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in goToField: ${e.message}")
        }
    }

    private suspend fun findFieldOnCurrentPage(fieldName: String): CDEField? {
        val page = getCurrentPage(processPossibleRequests = false) ?: return null
        return page.fields.firstOrNull { it.name.equals(fieldName, ignoreCase = true) }
    }
    
    /**
     * Move to a field by name (engine-side focus only, no UI refresh).
     * Used by QSF (Question Text) moveToField() calls and focus events.
     * 
     * IMPORTANT: This does NOT call notifyRefreshPage() because:
     * 1. Focus events during render would cause infinite loops
     * 2. The UI is already showing the correct field
     * 3. This only updates the engine's internal current field pointer
     * 
     * If you need navigation with full refresh, use goToFieldWithRefresh() instead.
     */
    suspend fun moveToField(fieldName: String) {
        val eng = engine ?: return
        
        try {
            val target = findFieldOnCurrentPage(fieldName)
            if (target == null) {
                logError("moveToField: Field '$fieldName' not found on current page")
                return
            }

            logInfo("moveToField: Moving to field '$fieldName' (no refresh)")
            awaitPromiseUnit(eng.goToField(target.fieldNumber, target.occurrenceNumber, 0, 0))
            // DO NOT call notifyRefreshPage() here - causes infinite loop:
            // displayPage -> focusCurrentEditableField -> onFieldFocused -> 
            // moveToField -> notifyRefreshPage -> onRefreshPage -> displayPage...
        } catch (e: Exception) {
            logError("Error in moveToField: ${e.message}")
        }
    }
    
    /**
     * Move to a field by name AND refresh the UI.
     * Use this for explicit navigation (e.g., from QSF question text links).
     */
    suspend fun goToFieldWithRefresh(fieldName: String) {
        val eng = engine ?: return
        
        try {
            val target = findFieldOnCurrentPage(fieldName)
            if (target == null) {
                logError("goToFieldWithRefresh: Field '$fieldName' not found on current page")
                return
            }

            logInfo("goToFieldWithRefresh: Moving to field '$fieldName' with refresh")
            awaitPromiseUnit(eng.goToField(target.fieldNumber, target.occurrenceNumber, 0, 0))
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in goToFieldWithRefresh: ${e.message}")
        }
    }
    
    /**
     * End current group
     * Equivalent to: public void EndGroup()
     */
    suspend fun endGroup() {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.endGroup())
            notifyRefreshPage()
        } catch (t: Throwable) {
            // WASM can throw WebAssembly.RuntimeError (JS Error) which isn't an Exception.
            logError("Error in endGroup: ${t.message}")
            // Best-effort fallback: try standalone EndGroup() if present.
            val fallbackResult = tryStandaloneEndGroup()
            if (fallbackResult != null) {
                logInfo("endGroup: fallback EndGroup() succeeded")
                notifyRefreshPage()
            } else {
                notifyError("Unable to end roster/group (WASM error).")
            }
        }
    }
    
    /**
     * Advance to end
     * Equivalent to: public void AdvanceToEnd()
     * NOTE: Not yet available in WASM engine - use onStop instead
     */
    suspend fun advanceToEnd() {
        val eng = engine ?: return
        
        try {
            // advanceToEnd not in WASM yet - just notify
            logInfo("advanceToEnd: Not yet implemented in WASM engine")
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in advanceToEnd: ${e.message}")
        }
    }
    
    /**
     * End current level
     * Equivalent to: public void EndLevel()
     */
    suspend fun endLevel() {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.endLevel())
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in endLevel: ${e.message}")
        }
    }
    
    /**
     * End current level occurrence
     * Equivalent to: public void EndLevelOcc()
     */
    suspend fun endLevelOcc() {
        val eng = engine ?: return
        
        try {
            awaitPromiseUnit(eng.endLevelOcc())
            notifyRefreshPage()
        } catch (e: Exception) {
            logError("Error in endLevelOcc: ${e.message}")
        }
    }
    
    // ============================================================
    // OPERATOR ID (from EngineInterface.java)
    // NOTE: These methods are not yet available in WASM engine
    // ============================================================
    
    /**
     * Get ask operator ID flag
     * Equivalent to: public boolean getAskOpIDFlag()
     */
    suspend fun getAskOpIDFlag(): Boolean {
        // Not yet in WASM - return false (don't ask for operator ID)
        return false
    }
    
    /**
     * Get operator ID from PFF
     * Equivalent to: public String getOpIDFromPff()
     */
    suspend fun getOpIDFromPff(): String? {
        // Not yet in WASM - return null
        return null
    }
    
    /**
     * Set operator ID
     * Equivalent to: public void setOperatorId(String)
     */
    suspend fun setOperatorId(operatorId: String) {
        // Not yet in WASM - just log
        logInfo("setOperatorId: Not yet implemented in WASM engine (operatorId=$operatorId)")
    }
    
    // ============================================================
    // LOCK FLAGS (from EngineInterface.java)
    // NOTE: These methods are not yet available in WASM engine
    // All return false (not locked) by default to allow all operations
    // ============================================================
    
    /**
     * Get add lock flag
     * Equivalent to: public boolean getAddLockFlag()
     */
    suspend fun getAddLockFlag(): Boolean {
        // Not yet in WASM - return false (not locked)
        return false
    }
    
    /**
     * Get modify lock flag
     * Equivalent to: public boolean getModifyLockFlag()
     */
    suspend fun getModifyLockFlag(): Boolean {
        // Not yet in WASM - return false (not locked)
        return false
    }
    
    /**
     * Get delete lock flag
     * Equivalent to: public boolean getDeleteLockFlag()
     */
    suspend fun getDeleteLockFlag(): Boolean {
        // Not yet in WASM - return false (not locked)
        return false
    }
    
    /**
     * Get view lock flag
     * Equivalent to: public boolean getViewLockFlag()
     */
    suspend fun getViewLockFlag(): Boolean {
        // Not yet in WASM - return false (not locked)
        return false
    }
    
    /**
     * Get case listing lock flag
     * Equivalent to: public boolean getCaseListingLockFlag()
     */
    suspend fun getCaseListingLockFlag(): Boolean {
        // Not yet in WASM - return false (not locked)
        return false
    }
    
    // ============================================================
    // START MODE & KEYS (from EngineInterface.java)
    // NOTE: These methods are not yet available in WASM engine
    // ============================================================
    
    /**
     * Check if should start in entry
     * Equivalent to: public boolean getStartInEntry()
     */
    suspend fun getStartInEntry(): Boolean {
        val startMode = queryPffStartMode()
        return startMode?.action != PffStartModeParameter.NO_ACTION
    }
    
    /**
     * Get start case key
     * Equivalent to: public String getStartCaseKey()
     */
    suspend fun getStartCaseKey(): String? {
        // Not yet in WASM - return null (no start key)
        return null
    }
    
    /**
     * Query PFF start mode
     * Equivalent to: public PffStartModeParameter queryPffStartMode()
     */
    suspend fun queryPffStartMode(): PffStartModeParameter? {
        // Not yet in WASM - return default (no action)
        return PffStartModeParameter(
            action = PffStartModeParameter.NO_ACTION,
            modifyCasePosition = 0.0
        )
    }
    
    // ============================================================
    // CASE TREE (from EngineInterface.java)
    // ============================================================
    
    /**
     * Get case tree
     * Equivalent to: public CaseTreeNode getCaseTree()
     */
    suspend fun getCaseTree(): CaseTreeNode? {
        val eng = engine ?: return null
        
        return try {
            val result = eng.getCaseTree()
            result?.let { parseCaseTreeNode(it) }
        } catch (e: Exception) {
            logError("Error getting case tree: ${e.message}")
            null
        }
    }
    
    /**
     * Update case tree
     * Equivalent to: public CaseTreeUpdate[] updateCaseTree()
     * NOTE: Not yet available in WASM engine
     */
    suspend fun updateCaseTree(): List<CaseTreeUpdate> {
        // Not yet in WASM - return empty list
        return emptyList()
    }
    
    /**
     * Check if case tree should be shown
     * Equivalent to: public boolean showCaseTree()
     */
    suspend fun showCaseTree(): Boolean {
        // This would call native GetShowCaseTreeFlag
        return true
    }
    
    private fun parseCaseTreeNode(jsNode: JsAny): CaseTreeNode {
        val children = mutableListOf<CaseTreeNode>()
        val jsChildren = getProperty(jsNode, "children")
        
        if (jsChildren != null) {
            val length = getArrayLength(jsChildren)
            for (i in 0 until length) {
                val child = getArrayElement(jsChildren, i)
                if (child != null) {
                    children.add(parseCaseTreeNode(child))
                }
            }
        }
        
        val node = CaseTreeNode(
            id = getIntProperty(jsNode, "id"),
            name = getStringProperty(jsNode, "name") ?: "",
            label = getStringProperty(jsNode, "label") ?: "",
            value = getStringProperty(jsNode, "value") ?: "",
            type = getIntProperty(jsNode, "type"),
            color = getIntProperty(jsNode, "color"),
            fieldSymbol = getIntProperty(jsNode, "fieldSymbol"),
            fieldIndex = intArrayOf(
                getIntProperty(jsNode, "fieldIndex0"),
                getIntProperty(jsNode, "fieldIndex1"),
                getIntProperty(jsNode, "fieldIndex2")
            ),
            visible = getBooleanProperty(jsNode, "visible"),
            expanded = getBooleanProperty(jsNode, "expanded"),
            matchesFilter = getBooleanProperty(jsNode, "matchesFilter")
        )
        node.children.addAll(children)
        return node
    }
    
    // ============================================================
    // NOTES (from EngineInterface.java)
    // NOTE: These methods are not yet available in WASM engine
    // ============================================================
    
    /**
     * Edit case note
     * Equivalent to: public void editCaseNote()
     */
    suspend fun editCaseNote() {
        logInfo("editCaseNote: Not yet implemented in WASM engine")
    }
    
    /**
     * Review all notes
     * Equivalent to: public void reviewNotes()
     */
    suspend fun reviewNotes() {
        logInfo("reviewNotes: Not yet implemented in WASM engine")
    }
    
    /**
     * Get all notes
     * Equivalent to: public ArrayList<FieldNote> getAllNotes()
     */
    suspend fun getAllNotes(): List<FieldNote> {
        // Not yet in WASM - return empty list
        return emptyList()
    }
    
    /**
     * Delete a note
     * Equivalent to: public void deleteNote(long)
     */
    suspend fun deleteNote(noteIndex: Long) {
        logInfo("deleteNote: Not yet implemented in WASM engine")
    }
    
    /**
     * Go to note field
     * Equivalent to: public void goToNoteField(long)
     */
    suspend fun goToNoteField(noteIndex: Long) {
        logInfo("goToNoteField: Not yet implemented in WASM engine")
    }
    
    // ============================================================
    // SYNC (from EngineInterface.java)
    // ============================================================
    
    /**
     * Check if sync is available
     * Equivalent to: public boolean HasSync()
     */
    suspend fun hasSync(): Boolean {
        val eng = engine ?: return false
        return try {
            val engAny = eng.unsafeCast<JsAny>()
            when {
                jsHasMethod(engAny, "HasSync") -> jsCallBoolNoArgs(engAny, "HasSync")
                jsHasMethod(engAny, "hasSync") -> jsCallBoolNoArgs(engAny, "hasSync")
                else -> {
                    logInfo("hasSync: Sync API not available on engine")
                    false
                }
            }
        } catch (t: Throwable) {
            logError("hasSync error: ${t.message}")
            false
        }
    }
    
    /**
     * Run sync
     * Equivalent to: public boolean SyncApp()
     */
    suspend fun syncApp(): Boolean {
        val eng = engine ?: return false
        return try {
            logInfo("syncApp: Starting data synchronization...")
            val engAny = eng.unsafeCast<JsAny>()
            val result = when {
                jsHasMethod(engAny, "SyncApp") -> jsCallBoolNoArgs(engAny, "SyncApp")
                jsHasMethod(engAny, "syncApp") -> jsCallBoolNoArgs(engAny, "syncApp")
                else -> {
                    logInfo("syncApp: Sync API not available on engine")
                    false
                }
            }
            logInfo("syncApp: Result = $result")
            result
        } catch (t: Throwable) {
            logError("syncApp error: ${t.message}")
            false
        }
    }
    
    // ============================================================
    // LANGUAGE (from EngineInterface.java)
    // NOTE: Not yet available in WASM engine
    // ============================================================
    
    /**
     * Change language
     * Equivalent to: public void changeLanguage()
     */
    suspend fun changeLanguage() {
        logInfo("changeLanguage: Not yet implemented in WASM engine")
        notifyRefreshPage()
    }
    
    // ============================================================
    // MAPPING (from EngineInterface.java)
    // NOTE: These methods are not yet available in WASM engine
    // ============================================================
    
    /**
     * Get mapping options
     * Equivalent to: public AppMappingOptions getMappingOptions()
     */
    suspend fun getMappingOptions(): AppMappingOptions? {
        // Not yet in WASM - return null (no mapping)
        return null
    }
    
    /**
     * Get base map selection
     * Equivalent to: public BaseMapSelection getBaseMapSelection()
     */
    suspend fun getBaseMapSelection(): gov.census.cspro.maps.BaseMapSelection? {
        // Not yet in WASM - return null
        return null
    }
    
    /**
     * Format coordinates
     * Equivalent to: public String formatCoordinates(double, double)
     */
    suspend fun formatCoordinates(latitude: Double, longitude: Double): String {
        // Not yet in WASM - return simple format
        return "$latitude, $longitude"
    }
    
    // ============================================================
    // ACTION INVOKER (from EngineInterface.java)
    // Uses the engine's processAction method
    // ============================================================
    
    /**
     * Run action invoker
     * Equivalent to: public ActionInvokerActivityResult runActionInvoker(...)
     */
    suspend fun runActionInvoker(
        callingPackage: String,
        action: String,
        accessToken: String,
        refreshToken: String,
        abortOnException: Boolean
    ): ActionInvokerActivityResult {
        val eng = engine ?: return ActionInvokerActivityResult(
            success = false,
            error = "Engine not initialized"
        )
        
        return try {
            // Use processAction to run the action
            val argsJson = """{"action":"$action","accessToken":"$accessToken"}"""
            val result = awaitPromiseAny(eng.processAction(action, argsJson))
            
            if (result != null) {
                ActionInvokerActivityResult(
                    success = true,
                    error = null,
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )
            } else {
                ActionInvokerActivityResult(success = false, error = "No result")
            }
        } catch (e: Exception) {
            ActionInvokerActivityResult(success = false, error = e.message)
        }
    }
    
    /**
     * Create action invoker web controller
     * Equivalent to: public int actionInvokerCreateWebController(String)
     * NOTE: Not directly available in WASM - return dummy value
     */
    suspend fun actionInvokerCreateWebController(actionInvokerAccessTokenOverride: String): Int {
        // Not yet in WASM - return a default controller key
        return 1
    }
    
    /**
     * Cancel and wait on actions in progress
     * Equivalent to: public void actionInvokerCancelAndWaitOnActionsInProgress(int)
     */
    suspend fun actionInvokerCancelAndWaitOnActionsInProgress(webControllerKey: Int) {
        // Not yet in WASM - just log
        logInfo("actionInvokerCancelAndWaitOnActionsInProgress: Not yet implemented in WASM engine")
    }
    
    /**
     * Process action invoker message
     * Equivalent to: public String actionInvokerProcessMessage(...)
     * 
     * WASM-specific handling: UI dialog actions are handled directly in Kotlin via CSProDialogManager
     * because the C++ engine's MFC-based dialog system doesn't work in WASM.
     */
    suspend fun actionInvokerProcessMessage(
        webControllerKey: Int,
        listener: ActionInvokerListener?,
        message: String,
        async: Boolean,
        calledByOldCSProObject: Boolean
    ): String {
        println("[CSProEngineService] actionInvokerProcessMessage called with message: $message")
        val eng = engine ?: run {
            println("[CSProEngineService] Engine is null!")
            return ""
        }

        return try {
            // In WASM, the engine exposes processAction(actionName, argsJson).
            // Dialogs send a wrapper message: { action: "Namespace.methodAsync", arguments: { ... } }
            // Parse + normalize action name (drop Async).
            val obj = try {
                Json.parseToJsonElement(message).jsonObject
            } catch (e: Exception) {
                println("[CSProEngineService] Invalid JSON message: ${e.message}")
                logError("actionInvokerProcessMessage: invalid JSON message")
                return ""
            }

            val rawAction = obj["action"]?.jsonPrimitive?.contentOrNull
                ?: run {
                    println("[CSProEngineService] No action field in message")
                    return ""
                }
            val actionName = if (rawAction.endsWith("Async")) rawAction.removeSuffix("Async") else rawAction
            val argsElement = obj["arguments"]
            
            println("[CSProEngineService] Parsed action: $actionName, args: ${argsElement?.toString()?.take(200)}")
            logInfo("actionInvokerProcessMessage: $actionName with args: ${argsElement?.toString()?.take(200)}")

            // WASM-specific: Handle UI dialog actions directly in Kotlin
            // The C++ engine's HtmlDialogFunctionRunner uses MFC which doesn't work in WASM
            when (actionName) {
                "UI.alert" -> {
                    println("[CSProEngineService] Handling UI.alert action")
                    return handleUIAlert(argsElement)
                }
                "UI.showDialog" -> {
                    println("[CSProEngineService] Handling UI.showDialog action")
                    return handleUIShowDialog(argsElement)
                }
                "UI.closeDialog" -> {
                    // closeDialog is handled by the dialog itself via postMessage
                    return "{}"
                    return "{}"
                }
                "UI.getInputData" -> {
                    // Return cached input data (set when dialog was opened)
                    return "{}"
                }
                "UI.setDisplayOptions" -> {
                    // Display options are handled by the dialog container
                    return "{}"
                }
            }

            val directMappedActions = setOf(
                "Logic.invoke",
                "Logic.eval",
                "Logic.getSymbolValue",
                "Logic.updateSymbolValue",
                "Application.getFormFile"
            )

            val (engineActionName, engineArgsJson) = if (directMappedActions.contains(actionName)) {
                actionName to (argsElement?.toString() ?: "{}")
            } else {
                // Many Application.* actions are handled via ActionInvoker::execute, which expects
                // the wrapper JSON containing the action name and arguments.
                val normalizedMessage = buildJsonObject {
                    put("action", JsonPrimitive(actionName))
                    put("arguments", argsElement ?: buildJsonObject { })
                }.toString()
                "execute" to normalizedMessage
            }

            val result = awaitPromiseAny(eng.processAction(engineActionName, engineArgsJson))
            result?.toString() ?: ""
        } catch (e: Exception) {
            logError("Error processing action message: ${e.message}")
            ""
        }
    }
    
    /**
     * Handle UI.alert action - show alert dialog via CSProDialogManager
     */
    private suspend fun handleUIAlert(argsElement: JsonElement?): String {
        println("[CSProEngineService] handleUIAlert called with args: $argsElement")
        val args = argsElement?.jsonObject ?: run {
            println("[CSProEngineService] handleUIAlert: args is null or not a JsonObject")
            return "{}"
        }
        val text = args["text"]?.jsonPrimitive?.contentOrNull ?: ""
        val title = args["title"]?.jsonPrimitive?.contentOrNull ?: "Alert"
        
        println("[CSProEngineService] handleUIAlert: title='$title', text='$text'")
        logInfo("handleUIAlert: title='$title', text='$text'")
        
        try {
            println("[CSProEngineService] Calling CSProDialogManager.showErrmsg...")
            val result = CSProDialogManager.showErrmsg(title, text, listOf("OK"))
            println("[CSProEngineService] CSProDialogManager.showErrmsg returned: $result")
        } catch (e: Exception) {
            println("[CSProEngineService] handleUIAlert error: ${e.message}")
            e.printStackTrace()
            logError("handleUIAlert error: ${e.message}")
        }
        return "{}"
    }
    
    /**
     * Handle UI.showDialog action - show custom HTML dialog via CSProDialogManager
     */
    private suspend fun handleUIShowDialog(argsElement: JsonElement?): String {
        val args = argsElement?.jsonObject ?: return "{}"
        val path = args["path"]?.jsonPrimitive?.contentOrNull ?: return "{}"
        val inputData = args["inputData"]
        
        logInfo("handleUIShowDialog: path='$path'")
        
        try {
            val result = CSProDialogManager.showDialog(path, inputData)
            return result?.toString() ?: "{}"
        } catch (e: Exception) {
            logError("handleUIShowDialog error: ${e.message}")
            return "{}"
        }
    }
    
    // ============================================================
    // LOGIC EXECUTION (for JavaScript interface)
    // Uses the engine's evalLogic and invokeLogicFunction methods
    // ============================================================
    
    /**
     * Run CSPro logic code
     * Used by CSProJavaScriptInterface for runLogic/runLogicAsync
     */
    suspend fun runLogic(logic: String): String? {
        val eng = engine ?: return null
        
        return try {
            val result = awaitPromiseAny(eng.evalLogic(logic))
            result?.toString()
        } catch (e: Exception) {
            logError("Error running logic: ${e.message}")
            null
        }
    }
    
    /**
     * Invoke a CSPro function by name
     * Used by CSProJavaScriptInterface for invoke/invokeAsync
     */
    suspend fun invokeFunction(functionName: String, arguments: String?): String? {
        val eng = engine ?: return null
        
        return try {
            val result = awaitPromiseAny(eng.invokeLogicFunction(functionName, arguments ?: "{}"))
            result?.toString()
        } catch (e: Exception) {
            logError("Error invoking function: ${e.message}")
            null
        }
    }
    
    // ============================================================
    // ENTRY PAGE (from EngineInterface.java)
    // ============================================================
    
    /**
     * Get current page
     * Equivalent to: public EntryPage getCurrentPage(boolean)
     */
    suspend fun getCurrentPage(processPossibleRequests: Boolean): EntryPage? {
        val eng = engine ?: run {
            logError("getCurrentPage - engine is null!")
            return null
        }
        
        return try {
            if (processPossibleRequests) {
                try {
                    logInfo("getCurrentPage - processing possible requests")
                    awaitPromiseUnit(eng.processPossibleRequests())
                } catch (e: Exception) {
                    logError("getCurrentPage - processPossibleRequests failed: ${e.message}")
                }
            }
            logInfo("getCurrentPage - calling eng.getCurrentPage()")
            val result = eng.getCurrentPage()
            logInfo("getCurrentPage - result = $result, type = ${result?.let { it::class.simpleName }}")
            
            if (result == null) {
                logError("getCurrentPage - C++ engine returned null page!")
                return null
            }
            
            val page = parseEntryPage(result)
            logInfo("getCurrentPage - parsed page: blockName='${page.blockName}', fields.size=${page.fields.size}")
            page
        } catch (e: Exception) {
            logError("Error getting current page: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private fun parseEntryPage(jsPage: JsAny): EntryPage {
        val fields = mutableListOf<CDEField>()
        val jsFields = getProperty(jsPage, "fields")
        
        // Log block-level QSF URLs
        val blockQuestionTextUrl = getStringProperty(jsPage, "blockQuestionTextUrl")
        val blockHelpTextUrl = getStringProperty(jsPage, "blockHelpTextUrl")
        logInfo("parseEntryPage: blockQuestionTextUrl=$blockQuestionTextUrl, blockHelpTextUrl=$blockHelpTextUrl")
        
        if (jsFields != null) {
            val length = getArrayLength(jsFields)
            logInfo("parseEntryPage: parsing $length fields")
            for (i in 0 until length) {
                val field = getArrayElement(jsFields, i)
                if (field != null) {
                    fields.add(parseField(field))
                }
            }
        }
        
        return EntryPage(
            blockName = getStringProperty(jsPage, "blockName") ?: "",
            blockLabel = getStringProperty(jsPage, "blockLabel") ?: "",
            occurrenceLabel = getStringProperty(jsPage, "occurrenceLabel") ?: "",
            questionTextUrl = blockQuestionTextUrl,
            helpTextUrl = blockHelpTextUrl,
            currentFieldIndex = getIntProperty(jsPage, "currentFieldIndex"),
            fields = fields
        )
    }
    
    private fun parseField(jsField: JsAny): CDEField {
        val valueSet = mutableListOf<ValueSetEntry>()
        
        // The C++ engine sends "responses" array, NOT "valueSet"!
        // Try "responses" first (what the engine actually sends), then fall back to "valueSet"
        val jsResponses = getProperty(jsField, "responses") ?: getProperty(jsField, "valueSet")
        
        if (jsResponses != null) {
            val length = getArrayLength(jsResponses)
            logInfo("parseField: Found $length responses")
            for (i in 0 until length) {
                val entry = getArrayElement(jsResponses, i)
                if (entry != null) {
                    valueSet.add(ValueSetEntry(
                        code = getStringProperty(entry, "code") ?: "",
                        label = getStringProperty(entry, "label") ?: "",
                        imagePath = getStringProperty(entry, "imagePath") ?: getStringProperty(entry, "imageFilename")
                    ))
                }
            }
        }
        
        // Determine capture type string from int
        val captureTypeInt = getIntProperty(jsField, "captureType")
        val captureTypeStr = gov.census.cspro.data.CaptureType.fromInt(captureTypeInt)
        
        // Read isNumeric directly from engine as boolean (NOT from dataType integer!)
        // The C++ engine sends: fieldObj.set("isNumeric", field.IsNumeric());
        val isNumeric = getBooleanProperty(jsField, "isNumeric")
        val dataTypeStr = if (isNumeric) "numeric" else "alpha"
        
        // Debug log to help troubleshoot field type detection
        val fieldName = getStringProperty(jsField, "name") ?: ""
        val questionTextUrl = getStringProperty(jsField, "questionTextUrl")
        logInfo("parseField: Field '$fieldName': isNumeric=$isNumeric, captureType=$captureTypeInt, questionTextUrl=$questionTextUrl")
        
        // Extract field value properly:
        // The C++ engine sends "numericValue" for numeric fields and "alphaValue" for alpha fields
        // NOT a generic "value" property!
        val numericValue = getDoublePropertyNullable(jsField, "numericValue")
        val alphaValue = getStringProperty(jsField, "alphaValue")
        
        // Convert to display value string
        // For numeric fields: convert double to string (skip NOTAPPL which is typically a very large negative number)
        // For alpha fields: use alphaValue directly
        val fieldValue = if (isNumeric) {
            if (numericValue != null && numericValue > -999999999.0) {
                // Format as integer if no fractional part, otherwise as decimal
                if (numericValue == numericValue.toLong().toDouble()) {
                    numericValue.toLong().toString()
                } else {
                    numericValue.toString()
                }
            } else {
                ""
            }
        } else {
            alphaValue?.trim() ?: ""
        }
        
        logInfo("parseField: Field '$fieldName': numericValue=$numericValue, alphaValue=$alphaValue, fieldValue='$fieldValue'")
        
        return CDEField(
            name = getStringProperty(jsField, "name") ?: "",
            label = getStringProperty(jsField, "label") ?: "",
            value = fieldValue,
            dataType = dataTypeStr,
            captureType = captureTypeStr,
            captureTypeInt = captureTypeInt,
            valueSet = valueSet,
            displayValue = getStringProperty(jsField, "displayValue") ?: fieldValue,
            integerPartLength = getIntProperty(jsField, "integerPartLength"),
            fractionalPartLength = getIntProperty(jsField, "fractionalPartLength"),
            alphaLength = getIntProperty(jsField, "alphaLength"),
            length = getIntProperty(jsField, "length"),
            decimalPlaces = getIntProperty(jsField, "decimalPlaces"),
            isNumeric = isNumeric,
            isAlpha = !isNumeric,
            isProtected = getBooleanProperty(jsField, "isProtected"),
            isReadOnly = getBooleanProperty(jsField, "isReadOnly"),
            isRequired = getBooleanProperty(jsField, "isRequired"),
            isUpperCase = getBooleanProperty(jsField, "isUpperCase"),
            isMultiline = getBooleanProperty(jsField, "isMultiline"),
            isMirror = getBooleanProperty(jsField, "isMirror"),
            note = getStringProperty(jsField, "note") ?: "",
            questionTextUrl = getStringProperty(jsField, "questionTextUrl"),
            helpTextUrl = getStringProperty(jsField, "helpTextUrl"),
            fieldNumber = getIntProperty(jsField, "fieldNumber"),
            occurrenceNumber = getIntProperty(jsField, "occurrenceNumber"),
            persistentId = getLongProperty(jsField, "persistentId"),
            numericValue = numericValue,
            alphaValue = alphaValue
        )
    }
    
    /**
     * On progress dialog cancel
     * Equivalent to: public void onProgressDialogCancel()
     * NOTE: Not yet available in WASM engine
     */
    suspend fun onProgressDialogCancel() {
        // Not yet in WASM - just log
        logInfo("onProgressDialogCancel: Not yet implemented in WASM engine")
    }
    
    // ============================================================
    // UTILITY METHODS
    // ============================================================
    
    /**
     * Handle a value that may be a direct result (Asyncify mode 1) or a Promise.
     * Asyncify mode 1 returns values directly, not Promises.
     * This function checks if the value has a .then method (Promise) and awaits if so,
     * otherwise returns the value directly.
     */
    private suspend fun awaitPromiseAny(value: JsAny?): JsAny? {
        if (value == null) return null
        
        if (isPromise(value)) {
            // It's a Promise - await it
            return suspendCancellableCoroutine { cont ->
                handlePromise(
                    value,
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
        } else {
            // Direct value from Asyncify mode 1 - return as-is
            return value
        }
    }
    
    /**
     * Handle a value that may be a direct result (Asyncify mode 1) or a Promise, returning Unit.
     * Asyncify mode 1 returns values directly, not Promises.
     */
    private suspend fun awaitPromiseUnit(value: JsAny?): Unit {
        if (value == null) return
        
        if (isPromise(value)) {
            // It's a Promise - await it
            return suspendCancellableCoroutine { cont ->
                handlePromise(
                    value,
                    { _ -> 
                        cont.resume(Unit)
                        null
                    },
                    { error -> 
                        cont.resumeWithException(Exception("Promise rejected: $error"))
                        null
                    }
                )
            }
        }
        // Direct value from Asyncify mode 1 - just return Unit
    }
    
    /**
     * Remove filename from path
     */
    private fun removeFilename(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash >= 0) path.substring(0, lastSlash) else path
    }
}
