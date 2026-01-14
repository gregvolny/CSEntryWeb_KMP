package gov.census.cspro.ui

import gov.census.cspro.data.CaseSummary
import gov.census.cspro.engine.CSProEngineService
import gov.census.cspro.engine.EngineEventListener
// DialogHelper is in the same package
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.NodeList

// Top-level @JsFun helpers for WASM-compatible JS interop
@JsFun("(nodeList) => nodeList.length")
private external fun jsNodeListLength(nodeList: JsAny): Int

@JsFun("(nodeList, i) => nodeList.item(i)")
private external fun jsNodeListItem(nodeList: JsAny, i: Int): JsAny?

/**
 * Web equivalent of Android's CaseListActivity
 * Displays and manages list of cases for an application
 * Mirrors Android CSEntryDroid UI and behavior
 */
class CaseListActivity : BaseActivity(), EngineEventListener {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("[CaseListActivity] Coroutine exception: $throwable")
        throwable.printStackTrace()
    }
    private val scope = CoroutineScope(SupervisorJob() + exceptionHandler)
    private var container: HTMLElement? = null
    private var engineService: CSProEngineService? = null
    private val cases = mutableListOf<CaseSummary>()
    
    private var pffFilename: String? = null
    private var appDescription: String? = null
    private var isEngineInitialized = false
    
    override fun onCreate() {
        super.onCreate()
        
        // Get parameters from extras (set by ActivityRouter)
        pffFilename = getStringExtra("filename") ?: "Unknown.pff"
        appDescription = getStringExtra("description") ?: "Unknown Application"
        
        setContentView("case_list")
    }
    
    override fun setContentView(layoutId: String) {
        container = document.getElementById("app") as? HTMLElement
        
        if (container == null) {
            println("[CaseListActivity] Container not found")
            return
        }
        
        render()
        initializeEngineAndLoadCases()
    }
    
    private fun render() {
        container?.innerHTML = """
            <div class="activity case-list-activity">
                <!-- Toolbar matching Android style -->
                <div class="toolbar">
                    <button id="btn-back" class="toolbar-back-btn" title="Back">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
                        </svg>
                    </button>
                    <span class="toolbar-title">$appDescription</span>
                    <div class="toolbar-menu">
                        <button id="btn-sync" class="toolbar-action-btn" title="Sync">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm-1.1 11c-.85 0-1.55-.7-1.55-1.55 0-.85.7-1.55 1.55-1.55s1.55.7 1.55 1.55c0 .85-.7 1.55-1.55 1.55zM12 18c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"/>
                            </svg>
                        </button>
                    </div>
                </div>
                
                <div class="activity-content">
                    <div class="case-list-toolbar">
                        <div class="search-bar">
                            <input type="text" id="search-cases" placeholder="Search cases..." />
                        </div>
                        <div class="filter-buttons">
                            <button id="filter-all" class="btn btn-filter active">All</button>
                            <button id="filter-complete" class="btn btn-filter">Complete</button>
                            <button id="filter-partial" class="btn btn-filter">Partial</button>
                        </div>
                    </div>
                    
                    <div id="cases-list" class="cases-list">
                        <div class="app-loading">
                            <div class="loading-spinner"></div>
                            <span>Loading cases...</span>
                        </div>
                    </div>
                    
                    <button id="btn-add-case" class="btn btn-fab" title="Add Case">
                        <span class="icon">+</span>
                    </button>
                </div>
            </div>
        """.trimIndent()
        
        attachEventListeners()
    }
    
    private fun attachEventListeners() {
        val btnBack = document.getElementById("btn-back") as? HTMLButtonElement
        val btnSync = document.getElementById("btn-sync") as? HTMLButtonElement
        val btnAddCase = document.getElementById("btn-add-case") as? HTMLButtonElement
        
        btnBack?.addEventListener("click", {
            finish()
        })
        
        btnSync?.addEventListener("click", {
            syncCases()
        })
        
        btnAddCase?.addEventListener("click", {
            addNewCase()
        })
        
        // Filter buttons
        val btnFilterAll = document.getElementById("filter-all") as? HTMLButtonElement
        val btnFilterComplete = document.getElementById("filter-complete") as? HTMLButtonElement
        val btnFilterPartial = document.getElementById("filter-partial") as? HTMLButtonElement
        
        btnFilterAll?.addEventListener("click", {
            filterCases("all")
        })
        btnFilterComplete?.addEventListener("click", {
            filterCases("complete")
        })
        btnFilterPartial?.addEventListener("click", {
            filterCases("partial")
        })
        
        // Search listener
        val searchInput = document.getElementById("search-cases") as? org.w3c.dom.HTMLInputElement
        searchInput?.addEventListener("input", {
            val query = searchInput.value
            filterBySearch(query)
        })
    }
    
    private fun initializeEngineAndLoadCases() {
        scope.launch {
            try {
                println("[CaseListActivity] Initializing engine for $pffFilename")
                
                engineService = CSProEngineService.getInstance()
                println("[CaseListActivity] Got engine service instance")
                val initialized = engineService?.initialize() ?: false
                println("[CaseListActivity] Engine initialized: $initialized")
                
                if (!initialized) {
                    showError("Failed to initialize CSPro engine")
                    return@launch
                }
                
                println("[CaseListActivity] Adding listener...")
                engineService?.addListener(this@CaseListActivity)
                println("[CaseListActivity] Listener added")
                
                val pff = pffFilename
                println("[CaseListActivity] PFF filename: $pff")
                if (!pff.isNullOrEmpty() && pff != "Unknown.pff") {
                    println("[CaseListActivity] Calling openApplication...")
                    val opened = engineService?.openApplication(pff) ?: false
                    println("[CaseListActivity] openApplication returned: $opened")
                    if (opened) {
                        println("[CaseListActivity] Setting isEngineInitialized = true")
                        isEngineInitialized = true
                        
                        // Update sync button visibility based on engine capability
                        updateSyncButtonVisibility()
                        
                        println("[CaseListActivity] About to call loadCases()...")
                        loadCases()
                        println("[CaseListActivity] loadCases() returned")
                    } else {
                        showError("Failed to open application: $pff")
                    }
                } else {
                    showError("No application file specified")
                }
            } catch (e: Exception) {
                println("[CaseListActivity] Init error: $e")
                e.printStackTrace()
                showError("Initialization error: ${e.message}")
            } catch (t: Throwable) {
                println("[CaseListActivity] Init throwable: $t")
                t.printStackTrace()
                showError("Initialization error: ${t.message}")
            }
        }
    }
    
    private suspend fun loadCases() {
        try {
            println("[CaseListActivity] loadCases() starting...")
            if (!isEngineInitialized) {
                println("[CaseListActivity] Engine not initialized, returning")
                return
            }
            
            println("[CaseListActivity] Fetching cases from engine...")
            val caseSummaries = engineService?.getSequentialCaseIds() ?: emptyList()
            println("[CaseListActivity] Got ${caseSummaries.size} cases")
            
            cases.clear()
            cases.addAll(caseSummaries)
            
            println("[CaseListActivity] Calling renderCasesList...")
            renderCasesList()
            println("[CaseListActivity] renderCasesList done")
        } catch (e: Exception) {
            println("[CaseListActivity] Error loading cases: $e")
            e.printStackTrace()
            showError("Failed to load cases")
        }
    }
    
    private fun renderCasesList() {
        val casesList = document.getElementById("cases-list") as? HTMLDivElement ?: return
        
        if (cases.isEmpty()) {
            casesList.innerHTML = """
                <div class="no-apps-message">
                    No cases found for this application.<br>
                    Tap the + button to add a new case.
                </div>
            """.trimIndent()
            return
        }
        
        // Match Android cases_list_item_layout.xml structure:
        // - Left color indicator (red for partial, transparent for complete)
        // - Case ID text
        // - Case note (if present)
        val html = cases.joinToString("") { case ->
            // Use GetTrimmedKeyForDisplay logic: show label if available, otherwise key
            val displayText = when {
                case.label.isNotBlank() -> case.label.trim()
                case.questionnaireId.isNotBlank() -> case.questionnaireId.trim()
                case.caseIds.isNotEmpty() -> case.caseIds.joinToString("-")
                else -> "(No ID)"
            }
            // Android uses RED indicator for partial (incomplete) cases
            val statusIndicatorClass = if (case.partialSave) "status-partial" else "status-complete"
            val noteHtml = if (case.hasNotes) "<div class='case-note'>📝 Notes</div>" else ""
            val caseIdForData = case.questionnaireId.ifBlank { case.caseIds.joinToString("-") }
            
            """
            <div class="case-card" data-case-id="$caseIdForData" data-case-position="${case.questionnaireId}">
                <div class="case-status-indicator $statusIndicatorClass"></div>
                <div class="case-info">
                    <h3 class="case-label">$displayText</h3>
                    $noteHtml
                </div>
                <div class="case-actions-menu">
                    <button class="btn-case-action" data-action="modify" data-case-id="$caseIdForData" title="Modify">✎</button>
                    <button class="btn-case-action" data-action="view" data-case-id="$caseIdForData" title="View">👁</button>
                    <button class="btn-case-action action-danger" data-action="delete" data-case-id="$caseIdForData" title="Delete">🗑</button>
                </div>
            </div>
            """.trimIndent()
        }
        
        casesList.innerHTML = html
        
        // Attach action listeners
        val actionButtons = casesList.querySelectorAll(".btn-case-action")
        for (i in 0 until jsNodeListLength(actionButtons)) {
            val btn = jsNodeListItem(actionButtons, i) as? HTMLButtonElement ?: continue
            val action = btn.getAttribute("data-action")
            val caseId = btn.getAttribute("data-case-id") ?: ""
            val case = cases.find { it.questionnaireId == caseId } ?: continue
            
            btn.addEventListener("click", {
                when (action) {
                    "modify" -> modifyCase(case)
                    "view" -> viewCase(case)
                    "delete" -> deleteCase(case)
                }
            })
        }
        
        // Also make the card clickable (defaults to modify)
        val cards = casesList.querySelectorAll(".case-card")
        for (i in 0 until jsNodeListLength(cards)) {
            val card = jsNodeListItem(cards, i) as? HTMLElement ?: continue
            val caseId = card.getAttribute("data-case-id") ?: ""
            val case = cases.find { it.questionnaireId == caseId } ?: continue
            
            card.addEventListener("click", { e ->
                // Only if didn't click an action button
                if ((e.target as? HTMLElement)?.closest(".btn-case-action") == null) {
                    modifyCase(case)
                }
            })
        }
    }
    
    private fun addNewCase() {
        println("[CaseListActivity] Add new case")
        startActivity("EntryActivity", mapOf(
            "startMode" to "Add",
            "filename" to (pffFilename ?: "")
        ))
    }
    
    private fun modifyCase(case: CaseSummary) {
        println("[CaseListActivity] Modify case: ${case.questionnaireId}, position: ${case.positionInRepository}")
        startActivity("EntryActivity", mapOf(
            "startMode" to "Modify",
            "caseId" to case.questionnaireId,
            "casePosition" to case.positionInRepository,
            "filename" to (pffFilename ?: "")
        ))
    }
    
    private fun viewCase(case: CaseSummary) {
        println("[CaseListActivity] View case: ${case.questionnaireId}, position: ${case.positionInRepository}")
        startActivity("EntryActivity", mapOf(
            "startMode" to "View",
            "caseId" to case.questionnaireId,
            "casePosition" to case.positionInRepository,
            "caseId" to case.questionnaireId,
            "filename" to (pffFilename ?: "")
        ))
    }
    
    private fun deleteCase(case: CaseSummary) {
        scope.launch {
            val confirmed = DialogHelper.showYesNoAsync("Delete Case", "Are you sure you want to delete case '${case.label}'?")
            if (confirmed) {
                try {
                    // In real app, we need case position. For now mock deletion
                    // engineService?.deleteCase(...) 
                    println("[CaseListActivity] Delete case: ${case.questionnaireId}")
                    cases.removeAll { it.questionnaireId == case.questionnaireId }
                    renderCasesList()
                } catch (e: Exception) {
                    DialogHelper.showError("Delete Failed", "Failed to delete case: ${e.message}")
                }
            }
        }
    }
    
    private fun syncCases() {
        scope.launch {
            try {
                println("[CaseListActivity] Syncing...")
                DialogHelper.showInfo("Sync", "Starting synchronization...")
                val success = engineService?.syncApp() ?: false
                if (success) {
                    DialogHelper.showSuccess("Sync", "Sync completed successfully")
                    loadCases()
                } else {
                    DialogHelper.showError("Sync", "Sync failed. Check server connection and try again.")
                }
            } catch (e: Exception) {
                DialogHelper.showError("Sync Error", "Sync error: ${e.message}")
            }
        }
    }
    
    private suspend fun updateSyncButtonVisibility() {
        val btnSync = document.getElementById("btn-sync") as? HTMLElement
        val hasSync = engineService?.hasSync() ?: false
        println("[CaseListActivity] hasSync = $hasSync")
        if (btnSync != null) {
            btnSync.style.display = if (hasSync) "inline-flex" else "none"
        }
    }
    
    private fun filterCases(filter: String) {
        // Update UI active state
        val filters = listOf("all", "complete", "partial")
        filters.forEach { f ->
            document.getElementById("filter-$f")?.classList?.remove("active")
        }
        document.getElementById("filter-$filter")?.classList?.add("active")
        
        // TODO: Filter logic
    }
    
    private fun filterBySearch(query: String) {
        // TODO: Search logic
    }
    
    private fun showError(message: String) {
        val casesList = document.getElementById("cases-list") as? HTMLDivElement ?: return
        casesList.innerHTML = """
            <div class="error-state">
                <p class="error-message">$message</p>
                <button id="btn-retry" class="btn btn-primary">Retry</button>
            </div>
        """.trimIndent()
        
        document.getElementById("btn-retry")?.addEventListener("click", {
            initializeEngineAndLoadCases()
        })
    }
    
    // EngineEventListener implementation
    override fun onRefreshPage() {
        if (!isResumed) return
        scope.launch { loadCases() }
    }
    
    override fun onError(message: String) {
        DialogHelper.showError("Engine Error", message)
    }
    
    override fun onApplicationClosed() {
        finish()
    }
    
    override fun onCaseListUpdated() {
        if (!isResumed) return
        scope.launch { loadCases() }
    }
    
    override fun setTitle(title: String) {
        document.title = title
    }
    
    override fun startActivity(activityClass: String, extras: Map<String, Any>?) {
        ActivityRouter.navigateTo(activityClass, extras)
    }
    
    override fun finish() {
        engineService?.removeListener(this)
        onDestroy()
        container?.innerHTML = ""
        ActivityRouter.goBack()
    }

    override fun onDestroy() {
        // ActivityRouter swaps activities by calling onDestroy() directly.
        // Make sure we unregister so we don't keep refreshing in the background.
        engineService?.removeListener(this)
        super.onDestroy()
    }
}
