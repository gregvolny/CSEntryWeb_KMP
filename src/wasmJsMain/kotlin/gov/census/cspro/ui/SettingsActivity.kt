package gov.census.cspro.ui

import gov.census.cspro.storage.OpfsService
import gov.census.cspro.storage.FileInfo
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.*

/**
 * Settings Activity for CSEntry Web
 * Includes OPFS File Browser for managing application storage
 * Matches Android SettingsActivity behavior
 */
class SettingsActivity : BaseActivity() {
    private val scope = MainScope()
    private var container: HTMLElement? = null
    
    // OPFS Browser state
    private var currentPath = ""
    private var currentFiles = mutableListOf<FileInfo>()
    private var selectedItems = mutableSetOf<String>()
    
    override fun onCreate() {
        super.onCreate()
        setContentView("settings")
    }
    
    override fun setContentView(layoutId: String) {
        container = document.getElementById("app") as? HTMLElement
        if (container == null) {
            println("[SettingsActivity] Container not found")
            return
        }
        render()
    }
    
    private fun render() {
        container?.innerHTML = """
            <div class="activity settings-activity">
                <!-- Toolbar -->
                <div class="toolbar">
                    <button class="toolbar-nav-btn" id="btn-back" title="Back">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
                        </svg>
                    </button>
                    <span class="toolbar-title">Settings</span>
                </div>
                
                <!-- Main content -->
                <div class="activity-content">
                    <div class="settings-container">
                        
                        <!-- Settings Sections -->
                        <div class="settings-section">
                            <h2 class="section-title">General</h2>
                            
                            <div class="settings-item">
                                <div class="settings-item-content">
                                    <span class="settings-item-title">Show Hidden Applications</span>
                                    <span class="settings-item-subtitle">Display applications with ShowInApplicationListing=Hidden</span>
                                </div>
                                <label class="switch">
                                    <input type="checkbox" id="setting-show-hidden">
                                    <span class="slider"></span>
                                </label>
                            </div>
                        </div>
                        
                        <div class="settings-section">
                            <h2 class="section-title">Storage</h2>
                            
                            <button class="settings-item settings-item-clickable" id="btn-opfs-browser">
                                <div class="settings-item-content">
                                    <span class="settings-item-title">Browse OPFS Storage</span>
                                    <span class="settings-item-subtitle">Explore and manage files stored in Origin Private File System</span>
                                </div>
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="#666">
                                    <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
                                </svg>
                            </button>
                            
                            <button class="settings-item settings-item-clickable settings-item-danger" id="btn-clear-all">
                                <div class="settings-item-content">
                                    <span class="settings-item-title">Clear All Data</span>
                                    <span class="settings-item-subtitle">Remove all applications and data from storage</span>
                                </div>
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="#d32f2f">
                                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                                </svg>
                            </button>
                        </div>
                        
                        <div class="settings-section">
                            <h2 class="section-title">About</h2>
                            
                            <div class="settings-item">
                                <div class="settings-item-content">
                                    <span class="settings-item-title">Version</span>
                                    <span class="settings-item-subtitle">CSEntry Web 8.1 (KMP Port)</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- OPFS Browser Modal -->
                <div id="opfs-browser-modal" class="modal-overlay" style="display: none;">
                    <div class="modal-dialog opfs-browser-modal">
                        <div class="modal-header">
                            <h2>OPFS File Browser</h2>
                            <button id="opfs-modal-close" class="modal-close-btn">&times;</button>
                        </div>
                        <div class="modal-body">
                            <!-- Navigation Breadcrumb -->
                            <div class="opfs-breadcrumb" id="opfs-breadcrumb">
                                <button class="breadcrumb-item breadcrumb-root" data-path="">/ (root)</button>
                            </div>
                            
                            <!-- Toolbar -->
                            <div class="opfs-toolbar">
                                <button id="opfs-btn-up" class="opfs-tool-btn" title="Go Up">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                        <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
                                    </svg>
                                    Up
                                </button>
                                <button id="opfs-btn-refresh" class="opfs-tool-btn" title="Refresh">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                        <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                                    </svg>
                                    Refresh
                                </button>
                                <div class="opfs-toolbar-spacer"></div>
                                <button id="opfs-btn-delete" class="opfs-tool-btn opfs-tool-btn-danger" title="Delete Selected" disabled>
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                                    </svg>
                                    Delete
                                </button>
                            </div>
                            
                            <!-- File List -->
                            <div class="opfs-file-list" id="opfs-file-list">
                                <div class="opfs-loading">Loading...</div>
                            </div>
                            
                            <!-- Status Bar -->
                            <div class="opfs-status-bar" id="opfs-status-bar">
                                Ready
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <style>
                .settings-activity {
                    display: flex;
                    flex-direction: column;
                    height: 100vh;
                    background: #f5f5f5;
                }
                
                .settings-container {
                    padding: 16px;
                    max-width: 800px;
                    margin: 0 auto;
                    width: 100%;
                }
                
                .settings-section {
                    background: white;
                    border-radius: 8px;
                    margin-bottom: 16px;
                    overflow: hidden;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                }
                
                .section-title {
                    font-size: 14px;
                    font-weight: 500;
                    color: #488840;
                    padding: 16px 16px 8px;
                    margin: 0;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                
                .settings-item {
                    display: flex;
                    align-items: center;
                    padding: 16px;
                    border-bottom: 1px solid #eee;
                    background: none;
                    border: none;
                    width: 100%;
                    text-align: left;
                    font-family: inherit;
                }
                
                .settings-item:last-child {
                    border-bottom: none;
                }
                
                .settings-item-clickable {
                    cursor: pointer;
                    transition: background 0.2s;
                }
                
                .settings-item-clickable:hover {
                    background: #f5f5f5;
                }
                
                .settings-item-content {
                    flex: 1;
                }
                
                .settings-item-title {
                    display: block;
                    font-size: 16px;
                    color: #333;
                    margin-bottom: 4px;
                }
                
                .settings-item-subtitle {
                    display: block;
                    font-size: 13px;
                    color: #666;
                }
                
                .settings-item-danger .settings-item-title {
                    color: #d32f2f;
                }
                
                /* Toggle Switch */
                .switch {
                    position: relative;
                    display: inline-block;
                    width: 48px;
                    height: 28px;
                }
                
                .switch input {
                    opacity: 0;
                    width: 0;
                    height: 0;
                }
                
                .slider {
                    position: absolute;
                    cursor: pointer;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background-color: #ccc;
                    transition: .3s;
                    border-radius: 28px;
                }
                
                .slider:before {
                    position: absolute;
                    content: "";
                    height: 20px;
                    width: 20px;
                    left: 4px;
                    bottom: 4px;
                    background-color: white;
                    transition: .3s;
                    border-radius: 50%;
                }
                
                input:checked + .slider {
                    background-color: #488840;
                }
                
                input:checked + .slider:before {
                    transform: translateX(20px);
                }
                
                /* OPFS Browser Modal */
                .opfs-browser-modal {
                    width: 90%;
                    max-width: 900px;
                    height: 80vh;
                    display: flex;
                    flex-direction: column;
                }
                
                .opfs-browser-modal .modal-body {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                    padding: 0;
                }
                
                .opfs-breadcrumb {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 4px;
                    padding: 12px 16px;
                    background: #f5f5f5;
                    border-bottom: 1px solid #ddd;
                }
                
                .breadcrumb-item {
                    background: #e0e0e0;
                    border: none;
                    padding: 6px 12px;
                    border-radius: 16px;
                    cursor: pointer;
                    font-size: 13px;
                    transition: background 0.2s;
                }
                
                .breadcrumb-item:hover {
                    background: #488840;
                    color: white;
                }
                
                .breadcrumb-root {
                    background: #488840;
                    color: white;
                }
                
                .opfs-toolbar {
                    display: flex;
                    gap: 8px;
                    padding: 8px 16px;
                    background: white;
                    border-bottom: 1px solid #eee;
                }
                
                .opfs-tool-btn {
                    display: flex;
                    align-items: center;
                    gap: 4px;
                    padding: 8px 12px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    background: white;
                    cursor: pointer;
                    font-size: 13px;
                    transition: all 0.2s;
                }
                
                .opfs-tool-btn:hover:not(:disabled) {
                    background: #f0f0f0;
                    border-color: #999;
                }
                
                .opfs-tool-btn:disabled {
                    opacity: 0.5;
                    cursor: not-allowed;
                }
                
                .opfs-tool-btn-danger {
                    color: #d32f2f;
                    border-color: #d32f2f;
                }
                
                .opfs-tool-btn-danger:hover:not(:disabled) {
                    background: #ffebee;
                }
                
                .opfs-toolbar-spacer {
                    flex: 1;
                }
                
                .opfs-file-list {
                    flex: 1;
                    overflow-y: auto;
                    background: white;
                }
                
                .opfs-loading {
                    padding: 40px;
                    text-align: center;
                    color: #666;
                }
                
                .opfs-empty {
                    padding: 40px;
                    text-align: center;
                    color: #999;
                }
                
                .opfs-file-item {
                    display: flex;
                    align-items: center;
                    padding: 12px 16px;
                    border-bottom: 1px solid #f0f0f0;
                    cursor: pointer;
                    transition: background 0.15s;
                }
                
                .opfs-file-item:hover {
                    background: #f5f5f5;
                }
                
                .opfs-file-item.selected {
                    background: #e3f2fd;
                }
                
                .opfs-file-checkbox {
                    width: 20px;
                    height: 20px;
                    margin-right: 12px;
                    cursor: pointer;
                }
                
                .opfs-file-icon {
                    width: 24px;
                    height: 24px;
                    margin-right: 12px;
                }
                
                .opfs-file-icon.folder {
                    color: #ffc107;
                }
                
                .opfs-file-icon.file {
                    color: #607d8b;
                }
                
                .opfs-file-name {
                    flex: 1;
                    font-size: 14px;
                }
                
                .opfs-file-name.folder-name {
                    font-weight: 500;
                }
                
                .opfs-file-size {
                    font-size: 12px;
                    color: #999;
                    margin-left: 16px;
                }
                
                .opfs-status-bar {
                    padding: 8px 16px;
                    background: #f5f5f5;
                    border-top: 1px solid #ddd;
                    font-size: 12px;
                    color: #666;
                }
            </style>
        """.trimIndent()
        
        attachEventListeners()
    }
    
    private fun attachEventListeners() {
        // Back button
        (document.getElementById("btn-back") as? HTMLButtonElement)?.addEventListener("click", {
            finish()
        })
        
        // OPFS Browser button
        (document.getElementById("btn-opfs-browser") as? HTMLButtonElement)?.addEventListener("click", {
            showOpfsBrowser()
        })
        
        // Clear all data
        (document.getElementById("btn-clear-all") as? HTMLButtonElement)?.addEventListener("click", {
            onClearAllClicked()
        })
        
        // OPFS Modal close
        (document.getElementById("opfs-modal-close") as? HTMLButtonElement)?.addEventListener("click", {
            hideOpfsBrowser()
        })
        
        // OPFS Modal overlay click
        (document.getElementById("opfs-browser-modal") as? HTMLDivElement)?.addEventListener("click", { e ->
            if ((e.target as? HTMLElement)?.classList?.contains("modal-overlay") == true) {
                hideOpfsBrowser()
            }
        })
        
        // OPFS Toolbar buttons
        (document.getElementById("opfs-btn-up") as? HTMLButtonElement)?.addEventListener("click", {
            navigateUp()
        })
        
        (document.getElementById("opfs-btn-refresh") as? HTMLButtonElement)?.addEventListener("click", {
            refreshFileList()
        })
        
        (document.getElementById("opfs-btn-delete") as? HTMLButtonElement)?.addEventListener("click", {
            deleteSelectedItems()
        })
    }
    
    private fun showOpfsBrowser() {
        val modal = document.getElementById("opfs-browser-modal") as? HTMLDivElement
        modal?.style?.display = "flex"
        
        // Reset state
        currentPath = ""
        selectedItems.clear()
        
        // Load root directory
        loadDirectory("")
    }
    
    private fun hideOpfsBrowser() {
        val modal = document.getElementById("opfs-browser-modal") as? HTMLDivElement
        modal?.style?.display = "none"
    }
    
    private fun loadDirectory(path: String) {
        currentPath = path
        selectedItems.clear()
        updateDeleteButton()
        
        scope.launch {
            try {
                // Ensure OPFS is initialized
                OpfsService.ensureInitialized()
                
                // Update status
                updateStatus("Loading...")
                
                // List files
                currentFiles.clear()
                val files = OpfsService.listFiles(path)
                currentFiles.addAll(files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })))
                
                // Render
                renderFileList()
                renderBreadcrumb()
                updateStatus("${currentFiles.size} items")
                
            } catch (e: Exception) {
                println("[SettingsActivity] Error loading directory: $e")
                updateStatus("Error: ${e.message}")
            }
        }
    }
    
    private fun renderBreadcrumb() {
        val breadcrumb = document.getElementById("opfs-breadcrumb") as? HTMLDivElement ?: return
        
        val parts = if (currentPath.isEmpty()) listOf<String>() else currentPath.split("/").filter { it.isNotEmpty() }
        
        val html = StringBuilder()
        html.append("""<button class="breadcrumb-item breadcrumb-root" data-path="">/ (root)</button>""")
        
        var accumulatedPath = ""
        for (part in parts) {
            accumulatedPath = if (accumulatedPath.isEmpty()) part else "$accumulatedPath/$part"
            html.append("""<button class="breadcrumb-item" data-path="$accumulatedPath">$part</button>""")
        }
        
        breadcrumb.innerHTML = html.toString()
        
        // Attach click listeners to breadcrumb items
        breadcrumb.querySelectorAll(".breadcrumb-item").asList().forEach { element ->
            (element as? HTMLButtonElement)?.addEventListener("click", {
                val path = element.getAttribute("data-path") ?: ""
                loadDirectory(path)
            })
        }
    }
    
    private fun renderFileList() {
        val fileList = document.getElementById("opfs-file-list") as? HTMLDivElement ?: return
        
        if (currentFiles.isEmpty()) {
            fileList.innerHTML = """<div class="opfs-empty">This folder is empty</div>"""
            return
        }
        
        val html = currentFiles.mapIndexed { index, file ->
            val iconSvg = if (file.isDirectory) {
                """<svg class="opfs-file-icon folder" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
                </svg>"""
            } else {
                """<svg class="opfs-file-icon file" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
                </svg>"""
            }
            
            val sizeText = if (file.isDirectory) "" else formatFileSize(file.size)
            val nameClass = if (file.isDirectory) "opfs-file-name folder-name" else "opfs-file-name"
            val selected = selectedItems.contains(file.path)
            val selectedClass = if (selected) " selected" else ""
            
            """
            <div class="opfs-file-item$selectedClass" data-path="${file.path}" data-index="$index" data-is-dir="${file.isDirectory}">
                <input type="checkbox" class="opfs-file-checkbox" ${if (selected) "checked" else ""}>
                $iconSvg
                <span class="$nameClass">${file.name}</span>
                <span class="opfs-file-size">$sizeText</span>
            </div>
            """.trimIndent()
        }.joinToString("")
        
        fileList.innerHTML = html
        
        // Attach event listeners
        fileList.querySelectorAll(".opfs-file-item").asList().forEach { element ->
            val item = element as? HTMLDivElement ?: return@forEach
            val path = item.getAttribute("data-path") ?: return@forEach
            val isDir = item.getAttribute("data-is-dir") == "true"
            
            // Checkbox click
            val checkbox = item.querySelector(".opfs-file-checkbox") as? HTMLInputElement
            checkbox?.addEventListener("click", { e ->
                e.stopPropagation()
                toggleSelection(path, item, checkbox)
            })
            
            // Item click (for navigation or selection)
            item.addEventListener("click", { e ->
                // If it's a directory, navigate into it
                if (isDir) {
                    loadDirectory(path)
                } else {
                    // Toggle selection for files
                    checkbox?.checked = !(checkbox?.checked ?: false)
                    toggleSelection(path, item, checkbox)
                }
            })
        }
    }
    
    private fun toggleSelection(path: String, item: HTMLDivElement, checkbox: HTMLInputElement?) {
        if (selectedItems.contains(path)) {
            selectedItems.remove(path)
            item.classList.remove("selected")
            checkbox?.checked = false
        } else {
            selectedItems.add(path)
            item.classList.add("selected")
            checkbox?.checked = true
        }
        updateDeleteButton()
    }
    
    private fun updateDeleteButton() {
        val deleteBtn = document.getElementById("opfs-btn-delete") as? HTMLButtonElement
        deleteBtn?.disabled = selectedItems.isEmpty()
    }
    
    private fun navigateUp() {
        if (currentPath.isEmpty()) return
        
        val parts = currentPath.split("/").filter { it.isNotEmpty() }
        val newPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
        loadDirectory(newPath)
    }
    
    private fun refreshFileList() {
        loadDirectory(currentPath)
    }
    
    private fun deleteSelectedItems() {
        if (selectedItems.isEmpty()) return
        
        scope.launch {
            val confirmed = DialogHelper.showYesNoAsync(
                "Delete Files",
                "Are you sure you want to delete ${selectedItems.size} item(s)? Folders will be deleted recursively."
            )
            
            if (confirmed) {
                updateStatus("Deleting...")
                
                var deletedCount = 0
                var failedCount = 0
                
                for (path in selectedItems.toList()) {
                    try {
                        // Find if it's a directory
                        val isDir = currentFiles.find { it.path == path }?.isDirectory ?: false
                        val success = OpfsService.delete(path, recursive = isDir)
                        if (success) {
                            deletedCount++
                        } else {
                            failedCount++
                        }
                    } catch (e: Exception) {
                        failedCount++
                        println("[SettingsActivity] Delete error: $e")
                    }
                }
                
                // Clear selection and refresh
                selectedItems.clear()
                loadDirectory(currentPath)
                
                if (failedCount > 0) {
                    updateStatus("Deleted $deletedCount items, $failedCount failed")
                } else {
                    updateStatus("Deleted $deletedCount items")
                }
            }
        }
    }
    
    private fun onClearAllClicked() {
        scope.launch {
            val confirmed = DialogHelper.showYesNoAsync(
                "Clear All Data",
                "This will permanently delete all applications and data stored in the browser. This action cannot be undone.\n\nAre you sure?"
            )
            
            if (confirmed) {
                try {
                    // Delete applications folder
                    OpfsService.delete("applications", recursive = true)
                    // Delete data folder
                    OpfsService.delete("data", recursive = true)
                    // Delete temp folder
                    OpfsService.delete("temp", recursive = true)
                    
                    // Re-initialize to recreate empty folders
                    OpfsService.initialize()
                    
                    DialogHelper.showSuccess("Success", "All data has been cleared.")
                } catch (e: Exception) {
                    DialogHelper.showError("Error", "Failed to clear data: ${e.message}")
                }
            }
        }
    }
    
    private fun updateStatus(text: String) {
        val statusBar = document.getElementById("opfs-status-bar") as? HTMLDivElement
        statusBar?.textContent = text
    }
    
    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
    
    override fun setTitle(title: String) {
        document.title = title
    }
    
    override fun startActivity(activityClass: String, extras: Map<String, Any>?) {
        ActivityRouter.navigateTo(activityClass, extras)
    }
    
    override fun finish() {
        onDestroy()
        ActivityRouter.navigateTo("ApplicationsListActivity", null)
    }
}

// Extension function for Kotlin/WASM
private fun String.format(vararg args: Any): String {
    var result = this
    args.forEach { arg ->
        result = result.replaceFirst(Regex("%\\.?\\d*[sdfbx]"), arg.toString())
    }
    return result
}
