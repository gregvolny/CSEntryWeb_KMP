package gov.census.cspro.ui

import gov.census.cspro.storage.OpfsService
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.*

// Top-level JS interop for file input
@JsFun("() => { const input = document.createElement('input'); input.type = 'file'; return input; }")
private external fun jsCreateInputElement(): HTMLInputElement

@JsFun("(input) => { input.webkitdirectory = true; }")
private external fun jsSetWebkitDirectory(input: JsAny)

@JsFun("(input) => input.files")
private external fun jsGetInputFiles(input: JsAny): JsAny?

@JsFun("(files, i) => files.item(i)")
private external fun jsFileListItem(files: JsAny, i: Int): JsAny?

@JsFun("(files) => files ? files.length : 0")
private external fun jsFileListLength(files: JsAny?): Int

@JsFun("(file) => file.name")
private external fun jsGetFileName(file: JsAny): String

@JsFun("(file) => file.webkitRelativePath || file.name")
private external fun jsGetRelativePath(file: JsAny): String

@JsFun("(file) => file.arrayBuffer()")
private external fun jsReadArrayBuffer(file: JsAny): JsAny

// Array buffer helpers - must be top-level for WASM
@JsFun("(buffer) => new Uint8Array(buffer)")
private external fun jsArrayBufferToUint8Array(buffer: JsAny): JsAny

@JsFun("(arr, i) => arr[i]")
private external fun jsUint8ArrayGet(arr: JsAny, i: Int): Int

@JsFun("(arr) => arr.length")
private external fun jsUint8ArrayLength(arr: JsAny): Int

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

// Simple promise callback wrapper
private fun jsAwaitPromise(promise: JsAny, callback: (JsAny) -> Unit) {
    jsPromiseThenCallback(promise, callback)
}

@JsFun("(promise, callback) => promise.then((result) => callback(result))")
private external fun jsPromiseThenCallback(promise: JsAny, callback: (JsAny) -> Unit)

/**
 * Web equivalent of Android's ApplicationsListActivity
 * Displays list of available CSPro applications
 * Matches Android CSEntryDroid UI design exactly
 */
class ApplicationsListActivity : BaseActivity() {
    private val scope = MainScope()
    private var container: HTMLElement? = null
    private val applications = mutableListOf<ApplicationInfo>()
    private var menuVisible = false
    private var addAppModalVisible = false
    
    data class ApplicationInfo(
        val filename: String,
        val description: String,
        val isEntryApp: Boolean
    )
    
    override fun onCreate() {
        super.onCreate()
        setContentView("applications_list")
    }
    
    override fun setContentView(layoutId: String) {
        container = document.getElementById("app") as? HTMLElement
        
        if (container == null) {
            println("[ApplicationsListActivity] Container not found")
            return
        }
        
        render()
        loadApplications()
    }
    
    private fun render() {
        // Android-style layout matching fragment_applications_layout.xml exactly
        container?.innerHTML = """
            <div class="activity applications-list-activity">
                <!-- Toolbar with menu - matches Android ActionBar -->
                <div class="toolbar">
                    <button class="toolbar-nav-btn" id="btn-nav" title="Navigation">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/>
                        </svg>
                    </button>
                    <span class="toolbar-title">CSEntry</span>
                    <div class="toolbar-menu">
                        <div class="dropdown-menu-container">
                            <button id="btn-menu" class="toolbar-menu-btn" title="More options">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                    <circle cx="12" cy="5" r="2"/>
                                    <circle cx="12" cy="12" r="2"/>
                                    <circle cx="12" cy="19" r="2"/>
                                </svg>
                            </button>
                            <div id="dropdown-menu" class="dropdown-menu hidden">
                                <button class="menu-item" id="menu-about">About CSEntry</button>
                                <button class="menu-item" id="menu-help">Help</button>
                                <div class="menu-divider"></div>
                                <button class="menu-item" id="menu-add">Add Application</button>
                                <button class="menu-item" id="menu-update">Update Installed Applications</button>
                                <div class="menu-divider"></div>
                                <button class="menu-item" id="menu-settings">Settings</button>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Main content - matches fragment_applications_layout.xml -->
                <div class="activity-content">
                    <div class="applications-container">
                        <!-- Title in CSPro green (30sp in Android) -->
                        <h1 class="applications-title">Entry Applications</h1>
                        
                        <!-- Applications list (RecyclerView equivalent) -->
                        <ul id="apps-list" class="applications-list-view">
                            <li class="app-loading">
                                <div class="loading-spinner"></div>
                                <span>Loading applications...</span>
                            </li>
                        </ul>
                    </div>
                </div>
                
                <!-- Add Application Modal -->
                <div id="add-app-modal" class="modal-overlay" style="display: none;">
                    <div class="modal-dialog add-app-modal">
                        <div class="modal-header">
                            <h2>Add Application</h2>
                            <button id="modal-close" class="modal-close-btn">&times;</button>
                        </div>
                        <div class="modal-body">
                            <p class="modal-instruction">Choose where to add an application from:</p>
                            
                            <div class="add-app-options">
                                <button class="add-app-option" id="option-folder">
                                    <div class="option-icon">
                                        <svg width="48" height="48" viewBox="0 0 24 24" fill="#488840">
                                            <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
                                        </svg>
                                    </div>
                                    <div class="option-text">
                                        <h3>Local Folder</h3>
                                        <p>Select a folder containing CSPro application files (.pen, .pff, .dcf)</p>
                                    </div>
                                </button>
                                
                                <button class="add-app-option" id="option-file">
                                    <div class="option-icon">
                                        <svg width="48" height="48" viewBox="0 0 24 24" fill="#488840">
                                            <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
                                        </svg>
                                    </div>
                                    <div class="option-text">
                                        <h3>PFF File</h3>
                                        <p>Select a CSPro PFF file directly</p>
                                    </div>
                                </button>
                                
                                <button class="add-app-option" id="option-url" disabled>
                                    <div class="option-icon">
                                        <svg width="48" height="48" viewBox="0 0 24 24" fill="#999">
                                            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
                                        </svg>
                                    </div>
                                    <div class="option-text">
                                        <h3>From Server (Coming Soon)</h3>
                                        <p>Download from a CSWeb server</p>
                                    </div>
                                </button>
                            </div>
                            
                            <!-- Upload Progress -->
                            <div id="upload-status" class="upload-status" style="display: none;">
                                <div class="progress-container">
                                    <div id="upload-progress-bar" class="progress-bar"></div>
                                </div>
                                <p id="upload-progress-text">Preparing files...</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
        
        attachEventListeners()
    }
    
    private fun attachEventListeners() {
        val btnMenu = document.getElementById("btn-menu") as? HTMLButtonElement
        val dropdownMenu = document.getElementById("dropdown-menu") as? HTMLDivElement
        
        // Toggle dropdown menu
        btnMenu?.addEventListener("click", { e ->
            e.stopPropagation()
            menuVisible = !menuVisible
            if (menuVisible) {
                dropdownMenu?.classList?.remove("hidden")
            } else {
                dropdownMenu?.classList?.add("hidden")
            }
        })
        
        // Close menu when clicking elsewhere
        document.addEventListener("click", {
            if (menuVisible) {
                dropdownMenu?.classList?.add("hidden")
                menuVisible = false
            }
        })
        
        // Menu item handlers (matching Android menu options)
        (document.getElementById("menu-about") as? HTMLButtonElement)?.addEventListener("click", {
            closeDropdownMenu()
            onAboutClicked()
        })
        
        (document.getElementById("menu-help") as? HTMLButtonElement)?.addEventListener("click", {
            closeDropdownMenu()
            onHelpClicked()
        })
        
        (document.getElementById("menu-add") as? HTMLButtonElement)?.addEventListener("click", {
            closeDropdownMenu()
            showAddApplicationModal()
        })
        
        (document.getElementById("menu-update") as? HTMLButtonElement)?.addEventListener("click", {
            closeDropdownMenu()
            onUpdateApplicationsClicked()
        })
        
        (document.getElementById("menu-settings") as? HTMLButtonElement)?.addEventListener("click", {
            closeDropdownMenu()
            onSettingsClicked()
        })
        
        // Modal event listeners
        val modal = document.getElementById("add-app-modal") as? HTMLDivElement
        val modalCloseBtn = document.getElementById("modal-close") as? HTMLButtonElement
        
        modalCloseBtn?.addEventListener("click", {
            hideAddApplicationModal()
        })
        
        // Click outside modal to close
        modal?.addEventListener("click", { e ->
            if ((e.target as? HTMLElement)?.classList?.contains("modal-overlay") == true) {
                hideAddApplicationModal()
            }
        })
        
        // Add app option handlers
        (document.getElementById("option-folder") as? HTMLButtonElement)?.addEventListener("click", {
            onSelectFolderClicked()
        })
        
        (document.getElementById("option-file") as? HTMLButtonElement)?.addEventListener("click", {
            onSelectFileClicked()
        })
    }
    
    private fun closeDropdownMenu() {
        val dropdownMenu = document.getElementById("dropdown-menu") as? HTMLDivElement
        dropdownMenu?.classList?.add("hidden")
        menuVisible = false
    }
    
    private fun showAddApplicationModal() {
        val modal = document.getElementById("add-app-modal") as? HTMLDivElement
        modal?.style?.display = "flex"
        addAppModalVisible = true
        
        // Reset upload status
        val uploadStatus = document.getElementById("upload-status") as? HTMLDivElement
        uploadStatus?.style?.display = "none"
    }
    
    private fun hideAddApplicationModal() {
        val modal = document.getElementById("add-app-modal") as? HTMLDivElement
        modal?.style?.display = "none"
        addAppModalVisible = false
    }
    
    private fun onSelectFolderClicked() {
        scope.launch {
            try {
                println("[ApplicationsListActivity] Selecting folder...")
                
                // Show progress
                val uploadStatus = document.getElementById("upload-status") as? HTMLDivElement
                val progressText = document.getElementById("upload-progress-text") as? HTMLElement
                val progressBar = document.getElementById("upload-progress-bar") as? HTMLDivElement
                
                uploadStatus?.style?.display = "block"
                progressText?.textContent = "Select a folder containing CSPro application files..."
                progressBar?.style?.width = "0%"
                
                // Create file input for folder selection
                val input = jsCreateInputElement()
                jsSetWebkitDirectory(input)
                
                // Wait for user to select folder
                val files = selectFilesWithInput(input)
                
                if (files.isEmpty()) {
                    progressText?.textContent = "No folder selected"
                    return@launch
                }
                
                progressText?.textContent = "Processing ${files.size} files..."
                
                // Process and upload files to OPFS
                var uploadedCount = 0
                var appFolderName: String? = null
                
                for ((index, fileInfo) in files.withIndex()) {
                    val (relativePath, fileData) = fileInfo
                    
                    // Get the root folder name from the first file
                    if (appFolderName == null) {
                        appFolderName = relativePath.split("/").firstOrNull() ?: "application_${jsDateNow().toLong()}"
                    }
                    
                    // Store in OPFS under applications/
                    val opfsPath = "applications/$relativePath"
                    
                    try {
                        OpfsService.writeFile(opfsPath, fileData)
                        uploadedCount++
                        
                        val progress = ((index + 1).toFloat() / files.size * 100).toInt()
                        progressBar?.style?.width = "$progress%"
                        progressText?.textContent = "Uploading: $uploadedCount / ${files.size} files"
                    } catch (e: Exception) {
                        println("[ApplicationsListActivity] Failed to write file: $opfsPath - $e")
                    }
                }
                
                progressText?.textContent = "✓ Application added successfully! ($uploadedCount files)"
                progressBar?.style?.width = "100%"
                progressBar?.classList?.add("complete")
                
                // Reload applications after a short delay
                scheduleRefresh()
                
            } catch (e: Exception) {
                println("[ApplicationsListActivity] Error selecting folder: $e")
                val progressText = document.getElementById("upload-progress-text") as? HTMLElement
                progressText?.textContent = "Error: ${e.message}"
            }
        }
    }
    
    private fun scheduleRefresh() {
        scope.launch {
            kotlinx.coroutines.delay(1500)
            hideAddApplicationModal()
            loadApplications()
        }
    }
    
    private fun onSelectFileClicked() {
        scope.launch {
            try {
                println("[ApplicationsListActivity] Selecting file...")
                
                val input = jsCreateInputElement()
                // Accept PFF and PEN files
                input.accept = ".pff,.pen,.dcf,.ent"
                
                val files = selectFilesWithInput(input)
                
                if (files.isEmpty()) {
                    return@launch
                }
                
                // Show progress
                val uploadStatus = document.getElementById("upload-status") as? HTMLDivElement
                val progressText = document.getElementById("upload-progress-text") as? HTMLElement
                val progressBar = document.getElementById("upload-progress-bar") as? HTMLDivElement
                
                uploadStatus?.style?.display = "block"
                progressText?.textContent = "Uploading files..."
                
                var uploadedCount = 0
                for ((index, fileInfo) in files.withIndex()) {
                    val (fileName, fileData) = fileInfo
                    
                    // Create folder based on filename
                    val appName = fileName.substringBeforeLast(".")
                    val opfsPath = "applications/$appName/$fileName"
                    
                    try {
                        OpfsService.writeFile(opfsPath, fileData)
                        uploadedCount++
                        
                        val progress = ((index + 1).toFloat() / files.size * 100).toInt()
                        progressBar?.style?.width = "$progress%"
                    } catch (e: Exception) {
                        println("[ApplicationsListActivity] Failed to write file: $opfsPath - $e")
                    }
                }
                
                progressText?.textContent = "✓ Application added successfully!"
                progressBar?.style?.width = "100%"
                progressBar?.classList?.add("complete")
                
                scheduleRefresh()
                
            } catch (e: Exception) {
                println("[ApplicationsListActivity] Error selecting file: $e")
            }
        }
    }
    
    private suspend fun selectFilesWithInput(input: HTMLInputElement): List<Pair<String, ByteArray>> {
        return suspendCancellableCoroutine { cont ->
            input.addEventListener("change", {
                scope.launch {
                    val result = mutableListOf<Pair<String, ByteArray>>()
                    
                    val files = jsGetInputFiles(input)
                    val fileCount = jsFileListLength(files)
                    
                    for (i in 0 until fileCount) {
                        val file = jsFileListItem(files!!, i) ?: continue
                        val relativePath = jsGetRelativePath(file)
                        
                        try {
                            val arrayBuffer = readFileAsBytes(file)
                            result.add(Pair(relativePath, arrayBuffer))
                        } catch (e: Exception) {
                            println("[ApplicationsListActivity] Error reading file $relativePath: $e")
                        }
                    }
                    
                    cont.resumeWith(Result.success(result))
                }
            })
            
            input.addEventListener("cancel", {
                cont.resumeWith(Result.success(emptyList()))
            })
            
            input.click()
        }
    }
    
    private suspend fun readFileAsBytes(file: JsAny): ByteArray {
        return suspendCancellableCoroutine { cont ->
            val promise = jsReadArrayBuffer(file)
            jsAwaitPromise(promise) { buffer ->
                try {
                    val byteArray = arrayBufferToByteArray(buffer)
                    cont.resumeWith(Result.success(byteArray))
                } catch (e: Exception) {
                    cont.resumeWith(Result.failure(e))
                }
            }
        }
    }
    
    private fun arrayBufferToByteArray(buffer: JsAny): ByteArray {
        val uint8Array = jsArrayBufferToUint8Array(buffer)
        val length = jsUint8ArrayLength(uint8Array)
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            bytes[i] = jsUint8ArrayGet(uint8Array, i).toByte()
        }
        return bytes
    }
    
    private fun onAboutClicked() {
        println("[ApplicationsListActivity] About clicked")
        DialogHelper.showAbout()
    }
    
    private fun onHelpClicked() {
        println("[ApplicationsListActivity] Help clicked")
        window.open("https://www.census.gov/data/software/cspro.html", "_blank")
    }
    
    private fun onUpdateApplicationsClicked() {
        println("[ApplicationsListActivity] Update applications clicked")
        // TODO: Implement sync/update functionality
        DialogHelper.showInfo("Update Applications", "Update functionality coming soon")
    }
    
    private fun onSettingsClicked() {
        println("[ApplicationsListActivity] Settings clicked")
        // TODO: Show settings dialog
        DialogHelper.showInfo("Settings", "Settings coming soon")
    }
    
    private fun loadApplications() {
        scope.launch {
            try {
                println("[ApplicationsListActivity] Loading applications from OPFS...")
                
                // Initialize OPFS
                val opfsInitialized = OpfsService.initialize()
                
                applications.clear()
                
                if (opfsInitialized) {
                    // Get all entries in the applications directory
                    val appEntries = OpfsService.listFiles("applications")
                    
                    for (entry in appEntries) {
                        if (entry.isDirectory) {
                            // Standard case: Application in its own folder
                            // Look for PFF files in the folder
                            val subFiles = OpfsService.listFiles(entry.path)
                            val pffFile = subFiles.find { it.name.endsWith(".pff", ignoreCase = true) }
                            
                            if (pffFile != null) {
                                // Read PFF to get description
                                val description = readPffDescription(pffFile.path)
                                    ?: entry.name
                                
                                applications.add(
                                    ApplicationInfo(
                                        filename = pffFile.path,
                                        description = description,
                                        isEntryApp = true
                                    )
                                )
                            }
                        } else if (entry.name.endsWith(".pff", ignoreCase = true)) {
                            // Non-standard case: PFF file directly in applications root
                            val description = readPffDescription(entry.path)
                                ?: entry.name.substringBeforeLast(".")
                            
                            applications.add(
                                ApplicationInfo(
                                    filename = entry.path,
                                    description = description,
                                    isEntryApp = true
                                )
                            )
                        }
                    }
                }
                
                // Add built-in example application from WASM Assets 
                // Always include this as it's guaranteed to work
                // The Simple CAPI example is embedded in the WASM virtual file system
                // at /Assets/examples/ (see wasm-engine/src/WASM/Assets)
                val embeddedApp = ApplicationInfo(
                    "/Assets/examples/Simple CAPI.pff",
                    "Simple CAPI (Embedded Demo)",
                    true
                )
                // Add embedded app if not already present (check by description to avoid duplicates)
                if (applications.none { it.description.contains("Embedded") }) {
                    applications.add(0, embeddedApp) // Add at the beginning
                }
                
                renderApplicationsList()
            } catch (e: Exception) {
                println("[ApplicationsListActivity] Error loading applications: $e")
                showError("Failed to load applications")
            }
        }
    }
    
    private suspend fun readPffDescription(pffPath: String): String? {
        return try {
            val content = OpfsService.readFile(pffPath)
            if (content != null) {
                val text = content.decodeToString()
                // Parse [Run Information] section for Label
                val lines = text.lines()
                for (line in lines) {
                    if (line.trim().startsWith("Label=", ignoreCase = true)) {
                        return line.substringAfter("=").trim()
                    }
                    if (line.trim().startsWith("Description=", ignoreCase = true)) {
                        return line.substringAfter("=").trim()
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun renderApplicationsList() {
        val appsList = document.getElementById("apps-list") as? HTMLElement ?: return
        
        if (applications.isEmpty()) {
            // Match Android string: app_no_valid_applications
            appsList.innerHTML = """
                <li class="no-apps-message">
                    There are no applications on your device.<br>
                    Choose "Add Application" from the menu to add one.
                </li>
            """.trimIndent()
            return
        }
        
        // Sort applications alphabetically by description (matching Android behavior)
        val sortedApps = applications.sortedBy { it.description.lowercase() }
        
        // Android-style simple list items - just description text, no cards
        val html = sortedApps.joinToString("") { app ->
            """
            <li>
                <button class="app-list-item" data-filename="${app.filename}">
                    ${app.description}
                </button>
            </li>
            """.trimIndent()
        }
        
        appsList.innerHTML = html
        
        // Attach click listeners to list items
        sortedApps.forEach { app ->
            val item = appsList.querySelector("[data-filename='${app.filename}']")
            (item as? HTMLButtonElement)?.addEventListener("click", {
                launchApplication(app)
            })
        }
    }
    
    private fun launchApplication(app: ApplicationInfo) {
        println("[ApplicationsListActivity] Launching ${app.filename}")
        
        if (app.isEntryApp) {
            startActivity("CaseListActivity", mapOf(
                "filename" to app.filename,
                "description" to app.description
            ))
        }
    }
    
    private fun onAddApplicationClicked() {
        println("[ApplicationsListActivity] Add application clicked")
        ActivityRouter.navigateTo("AddApplicationActivity")
    }
    
    private fun showError(message: String) {
        val appsList = document.getElementById("apps-list") as? HTMLDivElement ?: return
        appsList.innerHTML = """
            <div class="error-state">
                <p class="error-message">$message</p>
                <button id="btn-retry" class="btn btn-primary">Retry</button>
            </div>
        """.trimIndent()
        
        val btnRetry = document.getElementById("btn-retry") as? HTMLButtonElement
        btnRetry?.addEventListener("click", {
            loadApplications()
        })
    }
    
    override fun setTitle(title: String) {
        document.title = title
    }
    
    override fun startActivity(activityClass: String, extras: Map<String, Any>?) {
        // Navigate to another activity using the router
        println("[ApplicationsListActivity] Starting activity: $activityClass")
        ActivityRouter.navigateTo(activityClass, extras)
    }
    
    override fun finish() {
        onDestroy()
        container?.innerHTML = ""
    }
}

