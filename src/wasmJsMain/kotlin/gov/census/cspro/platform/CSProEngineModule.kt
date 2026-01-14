@file:JsModule("./csentryKMP.js")

package gov.census.cspro.platform

import kotlin.js.Promise

/**
 * External declarations for the CSEntry KMP WASM module (csentryKMP.js)
 * 
 * This module contains the FULL Android C++ CSPro engine compiled to WebAssembly
 * with Embind bindings replacing JNI functions.
 * 
 * The module exports:
 * - createCSEntryKMPModule() factory function (ES6 default export)
 * - CSProEngine class with all data entry methods (from WASMBindings.cpp)
 * - Standalone functions from WebWASMBindings_full.cpp
 * - getVirtualFileContent() for virtual file access
 * 
 * Two binding layers are available:
 * 1. CSProEngine class: Object-oriented interface (from WASMBindings.cpp)
 * 2. Standalone functions: Procedural interface (from WebWASMBindings_full.cpp)
 */

/**
 * Factory function to create the CSEntry KMP WASM module
 * Returns a Promise that resolves to the module with CSProEngine class
 */
@JsName("default")
external fun createCSEntryKMPModule(config: JsAny? = definedExternally): Promise<CSProModule>

/**
 * The loaded CSPro WASM module
 * Contains the CSProEngine class and utility functions
 */
external interface CSProModule : JsAny {
    /**
     * Emscripten File System
     */
    val FS: EmscriptenFS
    
    /**
     * The CSProEngine constructor
     * Usage: val engine = module.CSProEngine.new()
     */
    val CSProEngine: CSProEngineConstructor
    
    // ============================================================
    // Standalone Functions from WebWASMBindings_full.cpp
    // These provide an alternative procedural interface
    // ============================================================
    
    // Initialization
    fun InitNativeEngineInterface(): JsAny
    fun SetEnvironmentVariables(
        email: String,
        tempFolder: String,
        applicationFolder: String,
        versionNumber: String,
        assetsDirectory: String,
        csEntryDirectory: String,
        opfsRootPath: String,
        downloadsDirectory: String
    )
    
    // Application Lifecycle
    fun InitApplication(pffFilename: String): Boolean
    fun EndApplication()
    fun Start(): Boolean
    fun ModifyCase(positionInRepository: Double): Boolean
    fun InsertCase(insertBeforePosition: Double): Boolean
    fun DeleteCase(positionInRepository: Double): Boolean
    
    // Field Navigation (returns JSON strings)
    fun GetCurrentEntryPageJson(): String
    fun NextField(): String
    fun PreviousField(): String
    fun GoToField(fieldName: String): String
    fun SetFieldValueAndAdvance(value: String): String
    fun EndGroup(): String
    fun EndLevel(): String
    fun AdvanceToEnd(): String
    
    // Occurrence Management
    fun InsertOcc(): Boolean
    fun InsertOccAfter(): Boolean
    fun DeleteOcc(): Boolean
    
    // Case Management
    fun GetCaseListJson(sortAscending: Boolean): String
    fun PartialSave()
    fun Save()
    fun Cancel()
    
    // Case Tree
    fun GetCaseTree(): String
    fun UpdateCaseTree(): String
    
    // Lock Flags
    fun GetAddLockFlag(): Boolean
    fun GetModifyLockFlag(): Boolean
    fun GetDeleteLockFlag(): Boolean
    fun GetViewLockFlag(): Boolean
    fun GetCaseListingLockFlag(): Boolean
    
    // Application Info
    fun GetApplicationDescription(): String
    fun GetOperatorIdFromPff(): String
    fun GetStartKey(): String
    fun GetAskOperatorIdFlag(): Boolean
    fun SetOperatorId(operatorId: String)
    fun IsSystemControlled(): Boolean
    fun ContainsMultipleLanguages(): Boolean
    fun ChangeLanguage()
    
    // Engine Control
    fun GetStopCode(): Int
    fun OnStop()
    fun RunUserbarFunction(userbarIndex: Int)
    fun OnProgressDialogCancel()
    
    // Paradata
    fun GetParadataCachedEvents()
}

/**
 * CSProEngine class constructor
 */
external interface CSProEngineConstructor : JsAny {
    // This is a workaround - we use a helper function to instantiate
}

/**
 * Emscripten virtual file system
 */
external interface EmscriptenFS : JsAny {
    fun writeFile(path: String, data: JsAny)
    fun readFile(path: String, options: JsAny? = definedExternally): JsAny
    fun mkdir(path: String)
    fun readdir(path: String): JsArray<JsString>
    fun unlink(path: String)
    fun stat(path: String): EmscriptenFileStat
    fun isFile(mode: Int): Boolean
    fun isDir(mode: Int): Boolean
}

external interface EmscriptenFileStat : JsAny {
    val size: Int
    val mode: Int
    val mtime: Double
}

/**
 * CSProEngine instance - the main engine interface
 * All methods that may involve file I/O or CSPro logic are async (return Promise)
 * 
 * From WASMBindings.cpp EMSCRIPTEN_BINDINGS(cspro_engine):
 */
external interface CSProEngine : JsAny {
    // ============================================================
    // APPLICATION LIFECYCLE (async - may do file I/O)
    // ============================================================
    
    /**
     * Initialize application from PFF file path
     * @param pffPath Path to the PFF file in the virtual filesystem
     * @return Promise<Boolean> - true if successful
     */
    fun initApplication(pffPath: String): Promise<JsBoolean>
    
    /**
     * Start the data entry session
     * @return Promise<Boolean> - true if successful
     */
    fun start(): Promise<JsBoolean>
    
    // ============================================================
    // CASE MANAGEMENT
    // ============================================================
    
    /**
     * Get list of cases for case listing
     * @return Array of case objects with ids, label, position
     */
    fun getSequentialCaseIds(): JsAny?  // Returns array or null
    
    /**
     * Open a case for modification
     * @param position Case position in repository
     * @return Promise<Boolean>
     */
    fun modifyCase(position: Double): Promise<JsBoolean>
    
    /**
     * Partial save of current case
     * @return Promise<Boolean>
     */
    fun partialSave(): Promise<JsBoolean>
    
    // ============================================================
    // FIELD NAVIGATION (async - triggers CSPro logic)
    // ============================================================
    
    /**
     * Move to next field
     * @return Promise with current page info or null if at end
     */
    fun nextField(): Promise<JsAny?>
    
    /**
     * Move to previous field
     * @return Promise with current page info or null if at beginning
     */
    fun previousField(): Promise<JsAny?>
    
    /**
     * Go to a specific field
     * @param symbolName Field symbol name
     * @param occurrence Field occurrence (1-based)
     * @return Promise with current page info
     */
    fun goToField(symbolName: String, occurrence: Int): Promise<JsAny?>
    
    /**
     * End the current group/roster
     * @return Promise with current page info
     */
    fun endGroup(): Promise<JsAny?>
    
    /**
     * End the current level
     * @return Promise with current page info
     */
    fun endLevel(): Promise<JsAny?>
    
    /**
     * End the current level occurrence
     * @return Promise with current page info
     */
    fun endLevelOcc(): Promise<JsAny?>
    
    /**
     * Insert a new occurrence
     * @return Promise<Boolean>
     */
    fun insertOcc(): Promise<JsBoolean>
    
    /**
     * Insert a new occurrence after current
     * @return Promise<Boolean>
     */
    fun insertOccAfter(): Promise<JsBoolean>
    
    /**
     * Delete current occurrence
     * @return Promise<Boolean>
     */
    fun deleteOcc(): Promise<JsBoolean>
    
    /**
     * Handle stop button press
     */
    fun onStop(): Promise<JsAny?>
    
    // ============================================================
    // FIELD VALUES (sync and async versions)
    // ============================================================
    
    /**
     * Set field value without advancing (sync)
     * @param value The value to set
     * @return Boolean - true if successful
     */
    fun setFieldValue(value: String): Boolean
    
    /**
     * Set field value and advance to next field (async - runs postproc)
     * @param value The value to set
     * @return Promise with current page info after advance
     */
    fun setFieldValueAndAdvance(value: String): Promise<JsAny?>
    
    // ============================================================
    // PAGE/FORM INFO (sync - no file I/O)
    // ============================================================
    
    /**
     * Get current entry page information
     * @return Page object with blockName, blockLabel, fields array, etc.
     */
    fun getCurrentPage(): JsAny?
    
    /**
     * Get the case navigation tree
     * @return Tree structure for NavigationFragment
     */
    fun getCaseTree(): JsAny?
    
    /**
     * Get complete form structure from loaded application
     * @return Form data with all fields, rosters, etc.
     */
    fun getFormData(): JsAny?
    
    /**
     * Get question text HTML for a field
     * @param symbolName Field symbol name
     * @return HTML string or null
     */
    fun getQuestionText(symbolName: String): String?
    
    /**
     * Check if application is system controlled
     * @return Boolean
     */
    fun isSystemControlled(): Boolean
    
    /**
     * Get the stop code from engine
     * @return Stop code integer
     */
    fun getStopCode(): Int
    
    // ============================================================
    // LOGIC EVALUATION (async - may trigger dialogs)
    // ============================================================
    
    /**
     * Evaluate a CSPro logic expression
     * @param expression The logic expression to evaluate
     * @return Promise with JSON result string
     */
    fun evalLogic(expression: String): Promise<JsString>
    
    /**
     * Invoke a CSPro logic function
     * @param functionName Name of the function
     * @param argsJson JSON string of arguments
     * @return Promise with JSON result string
     */
    fun invokeLogicFunction(functionName: String, argsJson: String): Promise<JsString>
    
    /**
     * Process an ActionInvoker action
     * @param actionName Full action name (e.g., "Logic.eval", "UI.alert")
     * @param argsJson JSON string of arguments
     * @return Promise with JSON result string
     */
    fun processAction(actionName: String, argsJson: String): Promise<JsString>
}

/**
 * Page information returned by getCurrentPage()
 */
external interface EnginePageInfo : JsAny {
    val blockName: String
    val blockLabel: String
    val occurrenceLabel: String
    val questionTextUrl: String?
    val fields: JsArray<EngineFieldInfo>
}

/**
 * Field information within a page
 */
external interface EngineFieldInfo : JsAny {
    val name: String
    val label: String
    val value: String
    val displayValue: String
    val captureType: String
    val dataType: String
    val length: Int
    val decimalPlaces: Int
    val isProtected: Boolean
    val isRequired: Boolean
    val hasNote: Boolean
    val noteText: String?
    val valueSet: JsArray<EngineValueSetEntry>?
    val questionTextUrl: String?
}

/**
 * Value set entry for dropdown/radio fields
 */
external interface EngineValueSetEntry : JsAny {
    val value: String
    val label: String
    val imageUrl: String?
}

/**
 * Case tree node for navigation
 */
external interface EngineCaseTreeNode : JsAny {
    val name: String
    val label: String
    val symbolIndex: Int
    val occurrence: Int
    val hasChildren: Boolean
    val isExpanded: Boolean
    val children: JsArray<EngineCaseTreeNode>?
}

/**
 * Case summary for case listing
 */
external interface EngineCaseSummary : JsAny {
    val ids: String
    val label: String
    val position: Double
}
