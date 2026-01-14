package gov.census.cspro.platform

import kotlin.js.Promise

/**
 * External declarations for the Emscripten CSPro WASM module
 * 
 * This bridges Kotlin/Wasm to the C++ engine compiled with Emscripten.
 * The C++ exports a CSProEngine CLASS via Embind, not standalone functions.
 * 
 * Architecture:
 * 1. Call createCSEntryKMPModule() to get the Emscripten Module
 * 2. Create a CSProEngine instance: new module.CSProEngine()
 * 3. Call methods on that engine instance
 * 
 * See: CSEntryWeb/cspro-dev/cspro/WASM/WASMBindings.cpp for actual exports
 */

/**
 * The Emscripten Module object returned by createCSEntryKMPModule()
 * Contains FS (filesystem) and CSProEngine constructor
 */
external interface CSProWasmModule : kotlin.js.JsAny {
    /**
     * Emscripten File System
     */
    val FS: FileSystem
    
    /**
     * The CSProEngine class constructor (from Embind)
     * Usage: val engine = js("new module.CSProEngine()") 
     */
    val CSProEngine: kotlin.js.JsAny
    
    /**
     * Standalone function exported by Embind
     */
    fun getVirtualFileContent(url: String): String
}

/**
 * CSProEngine instance interface
 * Matches WASMBindings.cpp class_<CSProEngine> Embind exports
 * 
 * NOTE: Method names are camelCase to match C++ Embind bindings
 * 
 * IMPORTANT: With Asyncify mode 1 (-sASYNCIFY=1), functions marked with async()
 * in Embind return values DIRECTLY (not Promises). They can suspend internally
 * via stack unwinding, but the JS API is synchronous.
 */
external interface CSProEngineInstance : kotlin.js.JsAny {
    // ============================================================
    // METHODS marked with async() in Embind - with Asyncify mode 1
    // these return DIRECT values (not Promises)
    // ============================================================
    
    fun initApplication(pffPath: String): kotlin.js.JsBoolean
    fun start(): kotlin.js.JsBoolean
    fun modifyCase(position: Double): kotlin.js.JsAny?
    fun nextField(): kotlin.js.JsAny?
    fun previousField(): kotlin.js.JsAny?
    fun goToField(fieldSymbol: Int, occ1: Int, occ2: Int, occ3: Int): kotlin.js.JsAny?
    fun endGroup(): kotlin.js.JsAny?
    fun endLevel(): kotlin.js.JsAny?
    fun endLevelOcc(): kotlin.js.JsAny?
    fun insertOcc(): kotlin.js.JsAny?
    fun insertOccAfter(): kotlin.js.JsAny?
    fun deleteOcc(): kotlin.js.JsAny?
    fun onStop(): kotlin.js.JsAny?
    fun partialSave(): kotlin.js.JsAny?
    fun setFieldValueAndAdvance(value: String): kotlin.js.JsAny?
    fun processPossibleRequests(): kotlin.js.JsAny?
    
    // Action Invoker / Logic evaluation methods
    fun invokeLogicFunction(name: String, args: String): kotlin.js.JsAny?
    fun evalLogic(expression: String): kotlin.js.JsAny?
    fun processAction(action: String, argsJson: String): kotlin.js.JsAny?
    
    // ============================================================
    // SYNC METHODS (no async() in Embind - return immediately)
    // ============================================================
    
    fun getCurrentPage(): kotlin.js.JsAny?
    fun isSystemControlled(): kotlin.js.JsBoolean
    fun getStopCode(): kotlin.js.JsNumber
    fun getCaseTree(): kotlin.js.JsAny?
    fun getFormData(): kotlin.js.JsAny?
    fun getQuestionText(): kotlin.js.JsAny?
    fun getCapiDebugInfo(): kotlin.js.JsAny?
    fun setFieldValue(value: String): kotlin.js.JsBoolean
    fun setFieldValueByName(fieldName: String, value: String): kotlin.js.JsBoolean
    fun getSequentialCaseIds(): kotlin.js.JsAny?
    
    // Sync methods (for data synchronization)
    fun HasSync(): kotlin.js.JsBoolean
    fun SyncApp(): kotlin.js.JsBoolean
}

/**
 * Emscripten File System interface
 */
external interface FileSystem : kotlin.js.JsAny {
    fun writeFile(path: String, data: kotlin.js.JsAny)
    fun readFile(path: String, options: kotlin.js.JsAny? = definedExternally): kotlin.js.JsAny
    fun mkdir(path: String)
    fun readdir(path: String): kotlin.js.JsArray<kotlin.js.JsString>
    fun unlink(path: String)
    fun stat(path: String): FileStat
    fun rmdir(path: String)
    fun rename(oldPath: String, newPath: String)
}

external interface FileStat : kotlin.js.JsAny {
    val size: Int
    val mode: Int
    val mtime: Double
}

/**
 * Page information from the engine
 */
external interface PageInfo : kotlin.js.JsAny {
    val pageNumber: Int
    val totalPages: Int
    val fields: kotlin.js.JsArray<FieldInfo>
}

/**
 * Field information from the engine
 */
external interface FieldInfo : kotlin.js.JsAny {
    val name: String
    val label: String
    val value: String
    val fieldType: String
    val x: Int
    val y: Int
    val width: Int
    val height: Int
}

