package gov.census.cspro.engine

import gov.census.cspro.data.CaseTreeNode
import gov.census.cspro.data.CaseTreeUpdate
import gov.census.cspro.data.EntryPage
import gov.census.cspro.data.FieldNote
import gov.census.cspro.data.PffStartModeParameter

/**
 * Core Engine Interface - Platform-independent interface to CSPro engine
 * This represents the contract that both Android (JNI) and Web (WASM) must implement
 * 
 * Matches Android's EngineInterface.java native methods completely
 */
interface IEngineInterface {
    
    // ============================================================
    // Application lifecycle
    // ============================================================
    
    /**
     * Initialize the engine with a PFF file
     */
    suspend fun openApplication(pffFilename: String): Boolean
    
    /**
     * Close/end the current application
     */
    suspend fun endApplication()
    
    /**
     * Stop the application
     */
    suspend fun stopApplication()
    
    /**
     * Start data entry
     */
    suspend fun start(): Boolean
    
    /**
     * Get stop code after application ends
     */
    fun getStopCode(): Int
    
    /**
     * Run user-triggered stop
     */
    suspend fun runUserTriggeredStop()
    
    /**
     * Check if an application is currently open
     */
    fun isApplicationOpen(): Boolean
    
    /**
     * Check if engine is initialized
     */
    fun isInitialized(): Boolean
    
    // ============================================================
    // Window/Application info
    // ============================================================
    
    /**
     * Get the current window title/application name
     */
    fun getWindowTitle(): String?
    
    /**
     * Set the window title
     */
    fun setWindowTitle(title: String)
    
    /**
     * Get the application description
     */
    fun getApplicationDescription(): String?
    
    /**
     * Get the application directory
     */
    fun getApplicationDirectory(): String?
    
    // ============================================================
    // Field navigation
    // ============================================================
    
    /**
     * Move to next field
     */
    suspend fun nextField()
    
    /**
     * Move to previous field
     */
    suspend fun previousField()
    
    /**
     * Check if application has persistent fields
     */
    fun hasPersistentFields(): Boolean
    
    /**
     * Move to previous persistent field
     */
    suspend fun previousPersistentField()
    
    /**
     * Go to a specific field by symbol and indices
     */
    suspend fun goToField(fieldSymbol: Int, index1: Int, index2: Int, index3: Int)
    
    /**
     * End current group
     */
    suspend fun endGroup()
    
    /**
     * Advance to end of form
     */
    suspend fun advanceToEnd()
    
    /**
     * End current level
     */
    suspend fun endLevel()
    
    /**
     * End current level occurrence
     */
    suspend fun endLevelOcc()
    
    // ============================================================
    // Occurrence management
    // ============================================================
    
    /**
     * Insert new occurrence
     */
    suspend fun insertOcc(): Boolean
    
    /**
     * Insert occurrence after current
     */
    suspend fun insertOccAfter(): Boolean
    
    /**
     * Delete current occurrence
     */
    suspend fun deleteOcc(): Boolean
    
    // ============================================================
    // Case management
    // ============================================================
    
    /**
     * Modify existing case at position
     */
    suspend fun modifyCase(casePosition: Double): Boolean
    
    /**
     * Insert new case at position
     */
    suspend fun insertCase(casePosition: Double): Boolean
    
    /**
     * Delete case at position
     */
    suspend fun deleteCase(casePosition: Double): Boolean
    
    /**
     * Save partial case
     */
    suspend fun savePartial()
    
    /**
     * Check if partial save is allowed
     */
    fun allowsPartialSave(): Boolean
    
    // ============================================================
    // Entry page
    // ============================================================
    
    /**
     * Get current entry page with fields
     */
    fun getCurrentPage(processPossibleRequests: Boolean = true): EntryPage?
    
    // ============================================================
    // Case tree
    // ============================================================
    
    /**
     * Get full case tree
     */
    fun getCaseTree(): CaseTreeNode?
    
    /**
     * Get incremental case tree updates
     */
    fun updateCaseTree(): Array<CaseTreeUpdate>?
    
    /**
     * Check if case tree should be shown
     */
    fun showCaseTree(): Boolean
    
    // ============================================================
    // Operator ID & Locks
    // ============================================================
    
    /**
     * Check if operator ID is required
     */
    fun getAskOpIDFlag(): Boolean
    
    /**
     * Get operator ID from PFF
     */
    fun getOpIDFromPff(): String?
    
    /**
     * Set operator ID
     */
    suspend fun setOperatorId(operatorId: String)
    
    /**
     * Check if adding cases is locked
     */
    fun getAddLockFlag(): Boolean
    
    /**
     * Check if modifying cases is locked
     */
    fun getModifyLockFlag(): Boolean
    
    /**
     * Check if deleting cases is locked
     */
    fun getDeleteLockFlag(): Boolean
    
    /**
     * Check if viewing cases is locked
     */
    fun getViewLockFlag(): Boolean
    
    /**
     * Check if case listing is locked
     */
    fun getCaseListingLockFlag(): Boolean
    
    /**
     * Check if case listing should be hidden
     */
    fun doNotShowCaseListing(): Boolean
    
    // ============================================================
    // System control & display options
    // ============================================================
    
    /**
     * Check if system controlled mode
     */
    fun isSystemControlled(): Boolean
    
    /**
     * Check if auto-advance on selection is enabled
     */
    fun getAutoAdvanceOnSelectionFlag(): Boolean
    
    /**
     * Check if codes should be displayed alongside labels
     */
    fun getDisplayCodesAlongsideLabelsFlag(): Boolean
    
    /**
     * Check if refusals are shown automatically
     */
    fun showsRefusalsAutomatically(): Boolean
    
    /**
     * Show refusals
     */
    fun showRefusals(): Boolean
    
    // ============================================================
    // Language
    // ============================================================
    
    /**
     * Check if application contains multiple languages
     */
    fun containsMultipleLanguages(): Boolean
    
    /**
     * Change current language
     */
    suspend fun changeLanguage()
    
    // ============================================================
    // Notes
    // ============================================================
    
    /**
     * Edit case note
     */
    suspend fun editCaseNote()
    
    /**
     * Review all notes
     */
    suspend fun reviewNotes()
    
    /**
     * Get all field notes
     */
    fun getAllNotes(): List<FieldNote>
    
    /**
     * Delete a note by index
     */
    suspend fun deleteNote(noteIndex: Long)
    
    /**
     * Go to the field associated with a note
     */
    suspend fun goToNoteField(noteIndex: Long)
    
    // ============================================================
    // Userbar
    // ============================================================
    
    /**
     * Run userbar function by index
     */
    suspend fun runUserbarFunction(userbarIndex: Int)
    
    // ============================================================
    // PFF start mode
    // ============================================================
    
    /**
     * Check if should start in entry mode
     */
    fun getStartInEntry(): Boolean
    
    /**
     * Get start case key from PFF
     */
    fun getStartCaseKey(): String?
    
    /**
     * Query PFF start mode
     */
    fun queryPffStartMode(): PffStartModeParameter?
    
    // ============================================================
    // Sync
    // ============================================================
    
    /**
     * Check if sync is available
     */
    fun hasSync(): Boolean
    
    /**
     * Run sync
     */
    suspend fun syncApp(): Boolean
    
    // ============================================================
    // Action Invoker
    // ============================================================
    
    /**
     * Process action invoker message
     */
    suspend fun processActionInvokerMessage(message: String): String
    
    // ============================================================
    // HTML Dialogs
    // ============================================================
    
    /**
     * Check if HTML dialogs should be used
     */
    fun useHtmlDialogs(): Boolean
    
    // ============================================================
    // Static methods (system settings)
    // ============================================================
    
    companion object {
        /**
         * Get system setting as string
         */
        fun getSystemSettingString(settingName: String, defaultValue: String): String = defaultValue
        
        /**
         * Get system setting as boolean
         */
        fun getSystemSettingBoolean(settingName: String, defaultValue: Boolean): Boolean = defaultValue
        
        /**
         * Get runtime string by message number
         */
        fun getRuntimeString(messageNumber: Int, text: String): String = text
        
        /**
         * Get version information
         */
        fun getVersionDetailedString(): String = "CSPro Web 8.0"
        
        /**
         * Get release date
         */
        fun getReleaseDateString(): String = "December 2025"
    }
}
