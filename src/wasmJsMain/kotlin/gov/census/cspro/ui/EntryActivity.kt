package gov.census.cspro.ui

import gov.census.cspro.data.CDEField
import gov.census.cspro.data.EntryPage
import gov.census.cspro.engine.CSProEngineService
import gov.census.cspro.engine.EngineEventListener
import gov.census.cspro.engine.dialogs.CSProDialogManager
// DialogHelper is in the same package
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.js.JsAny
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

// ActionInvoker (web mode) message helpers
// Note: These receive the PARSED message data object, not the raw event (see jsSetupMessageListener)

@JsFun("(data) => (data && data.type !== undefined) ? String(data.type) : ''")
private external fun jsGetWindowMessageType(data: JsAny): String

@JsFun("(data) => (data && data.requestId !== undefined) ? Number(data.requestId) : 0")
private external fun jsGetActionRequestId(data: JsAny): Int

@JsFun("(data) => (data && data.method !== undefined) ? String(data.method) : ''")
private external fun jsGetActionMethod(data: JsAny): String

@JsFun("(data) => data ? data.args : null")
private external fun jsGetActionArgs(data: JsAny): JsAny?

// CSPro HTML dialogs (CSProDialogManager) push a synthetic history entry with { csproDialog: <id> }.
@JsFun("() => { try { const s = window.history && window.history.state; return !!(s && s.csproDialog); } catch(e) { return false; } }")
private external fun jsHasOpenCsproDialogInHistory(): Boolean

@JsFun("() => { try { window.history.back(); } catch(e) {} }")
private external fun jsHistoryBack(): Unit

// For shim messages: get actionCode (numeric) and convert to method name
@JsFun("(data) => (data && data.actionCode !== undefined) ? Number(data.actionCode) : 0")
private external fun jsGetActionCode(data: JsAny): Int

@JsFun("(requestId, result, error) => ({ type:'cspro-action-response', requestId: requestId, result: result, error: error })")
private external fun jsMakeActionResponse(requestId: Int, result: String?, error: String?): JsAny

@JsFun("(method, args) => JSON.stringify({ action: method, arguments: args || {} })")
private external fun jsBuildActionInvokerMessage(method: String, args: JsAny?): String

// Global variables for message source storage and response posting
@JsFun("(source, message) => { try { if (source && source.postMessage) { source.postMessage(message, '*'); return true; } } catch (e) {} return false; }")
private external fun jsPostMessage(source: JsAny?, message: JsAny): Boolean

@JsFun("() => window.__csproLastMessageSource || null")
private external fun jsGetLastMessageSource(): JsAny?

// Setup message listener in pure JavaScript - this avoids WASM interop issues with MessageEvent
// The callback receives the data object directly, not the raw event
@JsFun("""(callback) => {
    window.addEventListener('message', function(event) {
        // Store the source for potential responses
        window.__csproLastMessageSource = event.source;
        
        const data = event.data;
        if (!data || typeof data !== 'object') {
            return;
        }
        
        // Call the Kotlin callback with the data object
        callback(data);
    });
    console.log('[EntryActivity] Message listener installed via JavaScript');
}""")
private external fun jsSetupMessageListener(callback: (JsAny) -> Unit)

// Map action codes to method names (matching CSEntryWeb capi-renderer.js)
private fun actionCodeToMethodName(actionCode: Int): String {
    return when (actionCode) {
        50799 -> "Logic.eval"
        44034 -> "Logic.getSymbol"
        22923 -> "Logic.getSymbolValue"
        41927 -> "Logic.invoke"
        65339 -> "Logic.updateSymbolValue"
        60265 -> "UI.closeDialog"
        57200 -> "UI.getInputData"
        62732 -> "UI.setDisplayOptions"
        48073 -> "UI.alert"
        31133 -> "UI.alert"  // Alternative code
        41655 -> "UI.showDialog"
        49835 -> "UI.showDialog"  // Alternative code
        37688 -> "File.copy"
        23568 -> "File.readBytes"
        43700 -> "File.readLines"
        29118 -> "File.readText"
        63893 -> "File.writeBytes"
        55855 -> "File.writeLines"
        60631 -> "File.writeText"
        20380 -> "Path.createDirectory"
        36724 -> "Path.getDirectoryListing"
        59076 -> "Path.getPathInfo"
        62012 -> "Path.selectFile"
        35645 -> "Path.showFileDialog"
        36421 -> "Sqlite.close"
        31287 -> "Sqlite.exec"
        55316 -> "Sqlite.open"
        40839 -> "Sqlite.rekey"
        11276 -> "execute"
        13052 -> "registerAccessToken"
        else -> "action_$actionCode"
    }
}

/**
 * Web equivalent of Android's EntryActivity
 * Main data entry screen with questionnaire and navigation
 * 
 * Mirrors Android CSEntryDroid UI exactly, including the collapsible navigation sidebar
 * and bottom navigation bar.
 */
class EntryActivity : BaseActivity(), EngineEventListener {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("[EntryActivity] Coroutine exception: $throwable")
        throwable.printStackTrace()
    }
    private val scope = CoroutineScope(SupervisorJob() + exceptionHandler)
    private var container: HTMLElement? = null
    private var engineService: CSProEngineService? = null
    
    private var questionnaireFragment: QuestionnaireFragment? = null
    private var navigationFragment: NavigationFragment? = null
    
    private var pffFilename: String? = null
    private var startMode: String? = null
    private var caseId: String? = null
    private var casePosition: Double = -1.0
    private var isEngineInitialized = false
    private var hasShownNullPageError = false
    
    override fun onCreate() {
        super.onCreate()
        
        // Get parameters from extras (set by ActivityRouter)
        pffFilename = getStringExtra("filename") ?: "Unknown.pff"
        startMode = getStringExtra("startMode") ?: "Add"
        caseId = getStringExtra("caseId")
        casePosition = getDoubleExtra("casePosition", -1.0)
        
        setContentView("entry_activity")
    }
    
    override fun setContentView(layoutId: String) {
        container = document.getElementById("app") as? HTMLElement
        
        if (container == null) {
            println("[EntryActivity] Container not found")
            return
        }
        
        render()
        initializeEngineAndStartEntry()
    }
    
    private fun render() {
        container?.innerHTML = """
            <div class="activity entry-activity">
                <!-- Toolbar - Mirrors Android exactly -->
                <div class="toolbar">
                    <button id="btn-toggle-nav" class="toolbar-nav-btn" title="Navigation">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/>
                        </svg>
                    </button>
                    <span id="entry-title" class="toolbar-title">CSEntry</span>
                    <div class="toolbar-menu">
                        <button id="btn-edit-note" class="toolbar-action-btn" title="Edit Note">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                            </svg>
                        </button>
                        <button id="btn-save" class="toolbar-action-btn" title="Save">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z"/>
                            </svg>
                        </button>
                        <!-- Options Menu Button -->
                        <div class="dropdown-menu-container">
                            <button id="btn-options-menu" class="toolbar-action-btn" title="Options">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
                                </svg>
                            </button>
                            <div id="options-dropdown" class="dropdown-menu hidden">
                                <button class="dropdown-item" data-action="partial-save">Save Partial</button>
                                <button class="dropdown-item" data-action="review-notes">Review Notes</button>
                                <button class="dropdown-item" data-action="advance-to-end">Advance to End</button>
                                <div class="dropdown-divider"></div>
                                <button class="dropdown-item" data-action="toggle-nav-controls">Hide Navigation Controls</button>
                                <button class="dropdown-item" data-action="toggle-casetree">Hide Case Tree</button>
                                <div class="dropdown-divider"></div>
                                <button class="dropdown-item" data-action="view-questionnaire">View Questionnaire</button>
                                <button class="dropdown-item" data-action="help">Help</button>
                            </div>
                        </div>
                        <button id="btn-close" class="toolbar-action-btn" title="Close">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                            </svg>
                        </button>
                    </div>
                </div>
                
                <!-- Main Content Area -->
                <div class="activity-content entry-content">
                    <!-- Collapsible Navigation Sidebar (Case Tree) -->
                    <aside id="navigation-sidebar" class="navigation-sidebar">
                        <div id="navigation-fragment"></div>
                    </aside>
                    
                    <!-- Main Questionnaire Area -->
                    <main class="questionnaire-area">
                        <div id="questionnaire-fragment"></div>
                    </main>
                </div>
                
                <!-- Footer Navigation Bar -->
                <footer class="entry-footer">
                    <button id="btn-prev-field" class="btn-nav">← Previous</button>
                    <div class="field-progress-dots"></div>
                    <button id="btn-next-field" class="btn-nav">Next →</button>
                </footer>
            </div>
        """.trimIndent()
        
        initializeFragments()
        attachEventListeners()
    }
    
    private fun initializeFragments() {
        questionnaireFragment = QuestionnaireFragment().apply {
            activity = this@EntryActivity
            onAttach()
            onCreateView()
            onViewCreated()
            
            setOnFieldChangeListener { field, value ->
                onFieldValueChanged(field, value)
            }

            setOnFieldFocusListener { field ->
                onFieldFocused(field)
            }
        }
        
        navigationFragment = NavigationFragment().apply {
            activity = this@EntryActivity
            onAttach()
            onCreateView()
            onViewCreated()
        }
    }
    
    private fun attachEventListeners() {
        val btnToggleNav = document.getElementById("btn-toggle-nav") as? HTMLButtonElement
        val btnEditNote = document.getElementById("btn-edit-note") as? HTMLButtonElement
        val btnSave = document.getElementById("btn-save") as? HTMLButtonElement
        val btnOptionsMenu = document.getElementById("btn-options-menu") as? HTMLButtonElement
        val btnClose = document.getElementById("btn-close") as? HTMLButtonElement
        val btnPrevField = document.getElementById("btn-prev-field") as? HTMLButtonElement
        val btnNextField = document.getElementById("btn-next-field") as? HTMLButtonElement
        val optionsDropdown = document.getElementById("options-dropdown") as? HTMLElement
        
        btnToggleNav?.addEventListener("click", {
            toggleNavigationSidebar()
        })
        
        btnEditNote?.addEventListener("click", {
            toggleEditNote()
        })
        
        btnSave?.addEventListener("click", {
            saveCase()
        })
        
        // Options menu toggle
        btnOptionsMenu?.addEventListener("click", { e ->
            e.stopPropagation()
            optionsDropdown?.classList?.toggle("hidden")
        })
        
        // Close dropdown when clicking outside
        document.addEventListener("click", {
            optionsDropdown?.classList?.add("hidden")
        })
        
        // Options menu items
        val menuItems = optionsDropdown?.querySelectorAll(".dropdown-item")
        if (menuItems != null) {
            for (i in 0 until menuItems.length) {
                val item = menuItems.item(i) as? HTMLElement ?: continue
                val action = item.getAttribute("data-action") ?: continue
                item.addEventListener("click", { e ->
                    e.stopPropagation()
                    optionsDropdown?.classList?.add("hidden")
                    handleMenuAction(action)
                })
            }
        }
        
        btnClose?.addEventListener("click", {
            finish()
        })
        
        btnPrevField?.addEventListener("click", {
            moveToPreviousField()
        })
        
        btnNextField?.addEventListener("click", {
            moveToNextField()
        })
        
        // Handle ActionInvoker web-mode requests from sandboxed question-text iframes.
        // This handles two message formats:
        // 1. 'cspro-action-request' from action-invoker.js (method + args) - PRIMARY
        // 2. 'cspro-action' and 'cspro-action-async' from inline QSF shim (actionCode + args)
        //
        // NOTE: QSF functions (endRoster, moveToField, advance, runLogic) now execute CSPro
        // logic directly via ActionInvoker (Logic.eval/Logic.invoke) in QuestionTextJsBridge,
        // mirroring Android's CSProJavaScriptInterface behavior. No event dispatching needed.
        //
        // IMPORTANT: We use jsSetupMessageListener to install the listener in pure JavaScript.
        // This avoids WASM interop issues where event.toJsReference() loses MessageEvent properties.
        // The callback receives the data object directly, already extracted from event.data.
        setupMessageHandler()
    }
    
    private fun setupMessageHandler() {
        jsSetupMessageListener { data: JsAny ->
            val messageType = jsGetWindowMessageType(data)
            
            // Handle QSF shim messages: cspro-action and cspro-action-async
            if (messageType == "cspro-action" || messageType == "cspro-action-async") {
                val isAsync = messageType == "cspro-action-async"
                val requestId = jsGetActionRequestId(data)
                val actionCode = jsGetActionCode(data)
                val args = jsGetActionArgs(data)
                val source = jsGetLastMessageSource()
                
                val method = actionCodeToMethodName(actionCode)
                println("[EntryActivity] QSF Shim ${if (isAsync) "async" else "sync"} action: code=$actionCode, method=$method, requestId=$requestId")
                
                scope.launch {
                    try {
                        // For logic actions, apply the current UI values first (matches native behavior).
                        if (method.startsWith("Logic.")) {
                            applyCurrentFieldValues()
                        }

                        val service = engineService ?: throw IllegalStateException("Engine not initialized")
                        val message = jsBuildActionInvokerMessage(method, args)
                        println("[EntryActivity] QSF Shim message: $message")
                        val result = service.actionInvokerProcessMessage(
                            webControllerKey = 1,
                            listener = null,
                            message = message,
                            async = isAsync,
                            calledByOldCSProObject = false
                        )

                        println("[EntryActivity] QSF Shim result: $result")
                        
                        // Send response back for async calls
                        if (isAsync && requestId > 0) {
                            jsPostMessage(source, jsMakeActionResponse(requestId, result, null))
                        }
                        
                        // Refresh page after logic actions to reflect any state changes
                        if (method.startsWith("Logic.")) {
                            refreshPage()
                        }
                    } catch (t: Throwable) {
                        println("[EntryActivity] QSF Shim error: ${t.message}")
                        if (isAsync && requestId > 0) {
                            jsPostMessage(source, jsMakeActionResponse(requestId, null, t.message ?: "Action failed"))
                        }
                    }
                }
                return@jsSetupMessageListener
            }
            
            // Handle original action-invoker.js messages: cspro-action-request
            if (messageType != "cspro-action-request") return@jsSetupMessageListener

            val requestId = jsGetActionRequestId(data)
            val method = jsGetActionMethod(data)
            val args = jsGetActionArgs(data)
            val source = jsGetLastMessageSource()

            println("[EntryActivity] ActionInvoker request: method=$method, requestId=$requestId")

            scope.launch {
                try {
                    // For logic actions, apply the current UI values first (matches native behavior).
                    if (method.startsWith("Logic.")) {
                        applyCurrentFieldValues()
                    }

                    val service = engineService ?: throw IllegalStateException("Engine not initialized")
                    val message = jsBuildActionInvokerMessage(method, args)
                    println("[EntryActivity] ActionInvoker message: $message")
                    val result = service.actionInvokerProcessMessage(
                        webControllerKey = 1,
                        listener = null,
                        message = message,
                        async = true,
                        calledByOldCSProObject = false
                    )

                    println("[EntryActivity] ActionInvoker result: $result")
                    jsPostMessage(source, jsMakeActionResponse(requestId, result, null))
                } catch (t: Throwable) {
                    println("[EntryActivity] ActionInvoker error: ${t.message}")
                    jsPostMessage(source, jsMakeActionResponse(requestId, null, t.message ?: "Action failed"))
                }
            }
        }
    }
    
    private fun handleMenuAction(action: String) {
        when (action) {
            "partial-save" -> saveCase()
            "review-notes" -> reviewNotes()
            "advance-to-end" -> advanceToEnd()
            "toggle-nav-controls" -> toggleNavigationControls()
            "toggle-casetree" -> toggleCaseTree()
            "view-questionnaire" -> viewQuestionnaire()
            "help" -> showHelp()
        }
    }
    
    private fun toggleEditNote() {
        DialogHelper.showInfo("Edit Note", "Field note editing is not yet implemented.")
    }
    
    private fun reviewNotes() {
        // TODO: Implement full notes review functionality
        DialogHelper.showInfo("Review Notes", "Field notes review will be available in a future update.")
    }
    
    private fun advanceToEnd() {
        scope.launch {
            try {
                applyCurrentFieldValues()
                engineService?.advanceToEnd()
                refreshPage()
                updateNavigationTree()
            } catch (e: Exception) {
                DialogHelper.showError("Error", "Failed to advance: ${e.message}")
            }
        }
    }
    
    private var navControlsVisible = true
    
    private fun toggleNavigationControls() {
        val footer = document.querySelector(".entry-footer") as? HTMLElement
        if (navControlsVisible) {
            footer?.style?.display = "none"
            navControlsVisible = false
            // Update menu item text
            updateMenuItemText("toggle-nav-controls", "Show Navigation Controls")
        } else {
            footer?.style?.display = "flex"
            navControlsVisible = true
            updateMenuItemText("toggle-nav-controls", "Hide Navigation Controls")
        }
    }
    
    private var caseTreeVisible = true
    
    private fun toggleCaseTree() {
        val sidebar = document.getElementById("navigation-sidebar") as? HTMLElement
        if (caseTreeVisible) {
            sidebar?.classList?.add("collapsed")
            caseTreeVisible = false
            updateMenuItemText("toggle-casetree", "Show Case Tree")
        } else {
            sidebar?.classList?.remove("collapsed")
            caseTreeVisible = true
            updateMenuItemText("toggle-casetree", "Hide Case Tree")
        }
    }
    
    private fun updateMenuItemText(action: String, newText: String) {
        val menuItem = document.querySelector("[data-action='$action']") as? HTMLElement
        menuItem?.textContent = newText
    }
    
    private fun viewQuestionnaire() {
        scope.launch {
            try {
                // Open the native CSPro questionnaire viewer (ported HTML/JS) in an iframe dialog.
                // The viewer will fetch content via ActionInvoker (Application.getQuestionnaireContentAsync).
                val width = (window.innerWidth * 0.95).toInt().coerceAtLeast(320)
                val height = (window.innerHeight * 0.90).toInt().coerceAtLeast(200)

                val input = buildJsonObject {
                    put("drawFrame", true)
                    put("showLanguageBar", true)
                    put("flatRosters", false)
                    put("maxRosterOcc", 0)
                    put("printAllValueSets", false)
                    put("printQuestionnaire", false)
                }

                CSProDialogManager.showDialog("../questionnaire-view/index.html", input, width, height)
            } catch (e: Exception) {
                println("[EntryActivity] viewQuestionnaire error: ${e.message}")
                e.printStackTrace()
                DialogHelper.showError("Error", "Failed to load questionnaire: ${e.message}")
            }
        }
    }
    
    private fun showHelp() {
        DialogHelper.showInfo("Help", "CSEntry Web Help\n\nFor assistance, please refer to the CSPro documentation at:\nhttps://www.census.gov/data/software/cspro.html")
    }
    
    private fun toggleNavigationSidebar() {
        val sidebar = document.getElementById("navigation-sidebar")
        sidebar?.classList?.toggle("open")
    }
    
    private fun initializeEngineAndStartEntry() {
        scope.launch {
            try {
                println("[EntryActivity] Initializing engine...")
                
                engineService = CSProEngineService.getInstance()
                val initialized = engineService?.initialize() ?: false
                
                if (!initialized) {
                    showError("Failed to initialize CSPro engine")
                    return@launch
                }
                
                engineService?.addListener(this@EntryActivity)
                
                val pff = pffFilename
                if (!pff.isNullOrEmpty()) {
                    val opened = engineService?.openApplication(pff) ?: false
                    if (opened) {
                        // Set window title from application
                        val title = engineService?.getWindowTitle() ?: "CSEntry"
                        setTitle(title)
                        
                        // Handle start mode (Add, Modify, View)
                        // Based on Android EntryEngineMessage.kt:
                        // - "Add" with no casePosition: calls start() to begin new case
                        // - "Modify": calls modifyCase(casePosition)
                        // - "Insert": calls insertCase(casePosition)
                        println("[EntryActivity] Start mode: $startMode, caseId: $caseId, casePosition: $casePosition")
                        val started = when (startMode) {
                            "Add" -> {
                                // For Add mode, use start() to begin a new case
                                // (Android: engineInterface.start() when no case position given)
                                println("[EntryActivity] Calling start() for Add mode")
                                engineService?.start() ?: false
                            }
                            "Modify" -> {
                                // Use the actual case position from extras
                                println("[EntryActivity] Calling modifyCase($casePosition) for Modify mode")
                                if (casePosition >= 0) {
                                    engineService?.modifyCase(casePosition) ?: false
                                } else {
                                    // Fallback: if no position, start new case
                                    println("[EntryActivity] No valid case position, falling back to start()")
                                    engineService?.start() ?: false
                                }
                            }
                            "View" -> {
                                // For view mode, use modifyCase with actual position
                                println("[EntryActivity] Calling modifyCase($casePosition) for View mode")
                                if (casePosition >= 0) {
                                    engineService?.modifyCase(casePosition) ?: false
                                } else {
                                    println("[EntryActivity] No valid case position for View mode")
                                    false
                                }
                            }
                            else -> {
                                println("[EntryActivity] Calling start() for default mode")
                                engineService?.start() ?: false
                            }
                        }
                        
                        println("[EntryActivity] started = $started")
                        
                        if (started) {
                            isEngineInitialized = true
                            println("[EntryActivity] Calling refreshPage()...")
                            refreshPage()
                            println("[EntryActivity] Calling updateNavigationTree()...")
                            updateNavigationTree()
                            println("[EntryActivity] Initialization complete")
                        } else {
                            showError("Failed to start data entry")
                        }
                    } else {
                        showError("Failed to open application: $pff")
                    }
                }
            } catch (e: Exception) {
                println("[EntryActivity] Init error: $e")
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun showError(message: String) {
        DialogHelper.showError("Error", message)
    }

    private suspend fun refreshPage() {
        if (!isEngineInitialized) return

        val service = engineService ?: return
        val page = service.getCurrentPage(processPossibleRequests = true)
        if (page != null) {
            hasShownNullPageError = false
            questionnaireFragment?.displayPage(page)
            return
        }

        // If the engine reports no current page, treat it as a terminal/transition state.
        // This can happen after EndGroup/EndRoster or at end-of-flow.
        val stopCode = service.getStopCode()
        if (stopCode != 0) {
            println("[EntryActivity] refreshPage: engine returned null page; stopCode=$stopCode -> finishing activity")
            finish()
            return
        }

        if (!hasShownNullPageError) {
            hasShownNullPageError = true
            println("[EntryActivity] refreshPage: engine returned null page (stopCode=0)")
            showError("Unable to display current page (engine returned no page).")
        }
    }
    
    private suspend fun updateNavigationTree() {
        if (!isEngineInitialized) return
        
        val caseTree = engineService?.getCaseTree()
        if (caseTree != null) {
            navigationFragment?.updateCaseTree(listOf(caseTree))
        }
    }
    
    private fun saveCase() {
        scope.launch {
            try {
                if (isEngineInitialized) {
                    engineService?.savePartial()
                    println("[EntryActivity] Case saved (partial)")
                    DialogHelper.showSuccess("Save", "Case saved successfully")
                }
            } catch (e: Exception) {
                DialogHelper.showError("Save Failed", "Failed to save: ${e.message}")
            }
        }
    }
    
    /**
     * Apply ALL current field values from the UI to the engine.
     * Mirrors Android's applyCurrentFieldValues() -> QuestionnaireFragment.applyCurrentFieldValues()
     * -> QuestionWidget.save() -> copyResponseToField() which sets values on CDEField native objects.
     * 
     * In KMP, since CDEField is a data class (not JNI-backed), we use setFieldValueByName()
     * to push values to the engine.
     * 
     * This MUST be called before nextField()/previousField() navigation so the engine
     * has the current UI values when it runs postproc validation.
     */
    private suspend fun applyCurrentFieldValues(): Boolean {
        val pageValues = questionnaireFragment?.getPageFieldValues() ?: emptyList()
        if (pageValues.isEmpty()) return false

        // Set each field value in the engine (mirrors copyResponseToField's native setter calls)
        for ((field, value) in pageValues) {
            if (field.isReadOnly || field.isProtected) continue
            engineService?.setFieldValueByName(field.name, value)
        }
        return true
    }
    
    /**
     * Move to next field - mirrors Android EntryActivity.initiateFieldMovement(NEXT_FIELD)
     * 
     * Android pattern:
     * 1. applyCurrentFieldValues() - saves all UI values to engine via CDEField native setters
     * 2. SendEntryEngineMessage(NEXT_FIELD) -> EngineInterface.getInstance().NextField()
     * 
     * IMPORTANT: In the KMP/WASM implementation, Android's CDEField is JNI-backed - setting
     * values immediately syncs to native memory. Here we use setFieldValueAndAdvance() which:
     * 1. Sets the raw value on CDEField::m_data (preserving empty/blank correctly)
     * 2. Calls NextField(TRUE) which saves and runs CSPro validation logic
     * 
     * This ensures ID item validation and errmsg work correctly.
     */
    private fun moveToNextField() {
        scope.launch {
            try {
                if (isEngineInitialized) {
                    // Get the current field's value from the UI
                    val currentFieldValue = questionnaireFragment?.getCurrentFieldValue()
                    
                    if (currentFieldValue != null) {
                        val (field, value) = currentFieldValue
                        println("[EntryActivity] moveToNextField: current field=${field.name}, value='$value'")
                        
                        // For block pages with multiple fields, save all OTHER field values first
                        val pageValues = questionnaireFragment?.getPageFieldValues() ?: emptyList()
                        for ((pageField, pageValue) in pageValues) {
                            if (pageField.isReadOnly || pageField.isProtected) continue
                            // Skip the current field - it will be saved via setFieldValueAndAdvance
                            if (pageField.name == field.name) continue
                            engineService?.setFieldValueByName(pageField.name, pageValue)
                        }
                        
                        // Use setFieldValueAndAdvance for the current field
                        // This properly saves the value (including empty/blank) and runs CSPro validation
                        println("[EntryActivity] moveToNextField: calling setFieldValueAndAdvance with value='$value'")
                        engineService?.setFieldValueAndAdvance(value)
                    } else {
                        // Fallback: just call nextField if no current field value
                        println("[EntryActivity] moveToNextField: no current field, calling nextField()")
                        engineService?.nextField()
                    }
                    
                    // Refresh UI with new page state
                    refreshPage()
                    updateNavigationTree()
                }
            } catch (e: Exception) {
                println("[EntryActivity] Error moving to next field: $e")
            }
        }
    }
    
    /**
     * Move to previous field - mirrors Android EntryActivity.initiateFieldMovement(PREVIOUS_FIELD)
     * 
     * Android pattern:
     * 1. applyCurrentFieldValues() - saves all UI values to engine
     * 2. SendEntryEngineMessage(PREVIOUS_FIELD) -> EngineInterface.getInstance().PreviousField()
     */
    private fun moveToPreviousField() {
        scope.launch {
            try {
                if (isEngineInitialized) {
                    // Step 1: Save all UI field values to the engine
                    applyCurrentFieldValues()
                    
                    // Step 2: Tell the engine to move to previous field
                    println("[EntryActivity] moveToPreviousField: calling previousField()")
                    engineService?.previousField()
                    
                    // Step 3: Refresh UI with new page state
                    refreshPage()
                    updateNavigationTree()
                }
            } catch (e: Exception) {
                println("[EntryActivity] Error moving to previous field: $e")
            }
        }
    }
    
    /**
     * Called when a field value is changed in the UI (e.g., radio button click, dropdown selection).
     * Following Android pattern: this should NOT automatically advance.
     * The value is stored in the UI and will be saved when Next/Previous is clicked.
     * 
     * Auto-advance only happens if autoAdvanceOnSelectionFlag is true (controlled by CSPro settings).
     */
    private fun onFieldValueChanged(field: CDEField, value: String) {
        scope.launch {
            try {
                if (isEngineInitialized) {
                    println("[EntryActivity] Field ${field.name} changed to '$value'")

                    // Persist by name without altering engine navigation state.
                    // This avoids corrupting the current page pointer for DisplayTogether blocks.
                    engineService?.setFieldValueByName(field.name, value)

                    // Check if auto-advance is enabled (matches Android behavior)
                    val autoAdvance = engineService?.getAutoAdvanceOnSelectionFlag() ?: false
                    if (autoAdvance) {
                        println("[EntryActivity] Auto-advance enabled, advancing to next field")
                        engineService?.setFieldValueAndAdvance(value)
                        refreshPage()
                        updateNavigationTree()
                    }
                    // Otherwise, the value stays in the UI and will be saved when Next is clicked
                }
            } catch (e: Exception) {
                println("[EntryActivity] Error handling field change: $e")
            }
        }
    }

    private fun onFieldFocused(field: CDEField) {
        // Focus tracking for debugging/UI only
        // Do NOT perform engine navigation on focus - this can corrupt state
        println("[EntryActivity] Field focused: ${field.name}")
    }
    
    // EngineEventListener implementation
    override fun onRefreshPage() {
        if (!isResumed) return
        scope.launch {
            refreshPage()
            updateNavigationTree()
        }
    }
    
    override fun onError(message: String) {
        DialogHelper.showError("Engine Error", message)
    }
    
    override fun onApplicationClosed() {
        finish()
    }
    
    override fun onCaseListUpdated() {
        scope.launch { updateNavigationTree() }
    }

    override fun onDestroy() {
        // Note: The message listener is installed via pure JS (jsSetupMessageListener) and cannot
        // be easily removed. It filters messages by type, so stale listeners are harmless.

        engineService?.removeListener(this)
        super.onDestroy()
    }
    
    override fun setTitle(title: String) {
        document.title = title
        val titleElement = document.getElementById("entry-title")
        titleElement?.textContent = title
    }
    
    override fun startActivity(activityClass: String, extras: Map<String, Any>?) {
        ActivityRouter.navigateTo(activityClass, extras)
    }
    
    override fun finish() {
        scope.launch {
            if (isEngineInitialized) {
                engineService?.stopApplication()
                engineService?.endApplication()
            }
            engineService?.removeListener(this@EntryActivity)
            
            questionnaireFragment?.onPause()
            questionnaireFragment?.onDestroyView()
            questionnaireFragment?.onDetach()
            
            navigationFragment?.onPause()
            navigationFragment?.onDestroyView()
            navigationFragment?.onDetach()
            
            onDestroy()
            container?.innerHTML = ""
            ActivityRouter.goBack()
        }
    }
}
