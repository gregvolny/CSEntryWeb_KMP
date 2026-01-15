package gov.census.cspro.ui

import gov.census.cspro.storage.OpfsService
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.*

// Top-level @JsFun helpers for WASM-compatible JS interop

@JsFun("() => { const input = document.createElement('input'); input.type = 'file'; return input; }")
private external fun jsCreateInputElement(): HTMLInputElement

@JsFun("(input) => { input.multiple = true; }")
private external fun jsSetMultiple(input: JsAny)

@JsFun("(input) => { input.webkitdirectory = true; }")
private external fun jsSetWebkitDirectory(input: JsAny)

@JsFun("(input, accept) => { input.accept = accept; }")
private external fun jsSetAccept(input: JsAny, accept: String)

@JsFun("(input) => input.files")
private external fun jsGetFileList(input: JsAny): JsAny?

@JsFun("(file) => file.webkitRelativePath || file.name")
private external fun jsGetFileWebkitRelativePath(file: JsAny): String

@JsFun("(file) => file.arrayBuffer()")
private external fun jsReadFileAsArrayBuffer(file: JsAny): JsAny  // Returns Promise

@JsFun("(ab) => ab ? ab.byteLength : 0")
private external fun jsGetArrayBufferByteLength(ab: JsAny): Int

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

@JsFun("(files, i) => files.item(i)")
private external fun jsFileListItem(files: JsAny, i: Int): JsAny?

@JsFun("(files) => files ? files.length : 0")
private external fun jsFileListLength(files: JsAny?): Int

@JsFun("(file) => file.name")
private external fun jsGetFileName(file: JsAny): String

@JsFun("(file) => file.size")
private external fun jsGetFileSize(file: JsAny): Double

@JsFun("(promise, onResolve, onReject) => promise.then(onResolve).catch(onReject)")
private external fun jsPromiseThen(promise: JsAny, onResolve: JsAny, onReject: JsAny)

// Helper wrapper class for File since we use JsAny internally
private data class FileWrapper(val jsFile: JsAny, val name: String, val size: Double)

/**
 * Web equivalent of Android's file chooser for adding CSPro applications
 * Allows uploading application folders or individual PFF files to OPFS
 */
class AddApplicationActivity : BaseActivity() {
    private val scope = MainScope()
    private var container: HTMLElement? = null
    private val pendingFiles = mutableListOf<Pair<String, FileWrapper>>() // path to FileWrapper
    private var isUploading = false
    
    override fun onCreate() {
        super.onCreate()
        setContentView("add_application")
    }
    
    override fun setContentView(layoutId: String) {
        container = document.getElementById("app") as? HTMLElement
        container?.innerHTML = ""
        
        // Initialize OPFS
        scope.launch {
            val initialized = OpfsService.initialize()
            if (!initialized) {
                showError("OPFS storage is not available in this browser")
            }
            render()
        }
    }
    
    private fun render() {
        container?.innerHTML = """
            <div class="activity add-application-activity">
                <!-- Toolbar -->
                <div class="toolbar">
                    <button id="btn-back" class="toolbar-back-btn" title="Back">←</button>
                    <span class="toolbar-title">Add Application</span>
                </div>
                
                <!-- Main content -->
                <div class="activity-content">
                    <div class="add-app-container">
                        <h2 class="section-title">Add CSPro Application</h2>
                        
                        <p class="instruction-text">
                            Upload a CSPro application folder containing the PFF file and all required files 
                            (dictionary files, data entry forms, etc.)
                        </p>
                        
                        <div class="upload-options">
                            <div class="upload-card" id="card-folder">
                                <div class="upload-icon">📁</div>
                                <h3>Upload Application Folder</h3>
                                <p>Select a complete application folder with all files</p>
                                <button id="btn-upload-folder" class="btn btn-primary">
                                    Select Folder
                                </button>
                            </div>
                            
                            <div class="upload-card" id="card-files">
                                <div class="upload-icon">📄</div>
                                <h3>Upload Individual Files</h3>
                                <p>Select multiple files to upload</p>
                                <button id="btn-upload-files" class="btn btn-secondary">
                                    Select Files
                                </button>
                            </div>
                        </div>
                        
                        <!-- Upload progress area -->
                        <div id="upload-progress" class="upload-progress" style="display: none;">
                            <h3>Uploading Files...</h3>
                            <div class="progress-bar-container">
                                <div id="progress-bar" class="progress-bar"></div>
                            </div>
                            <p id="progress-text">0 / 0 files</p>
                            <ul id="file-list" class="file-list"></ul>
                        </div>
                        
                        <!-- Pending files preview -->
                        <div id="pending-preview" class="pending-preview" style="display: none;">
                            <h3>Files to Upload</h3>
                            <ul id="pending-list" class="pending-list"></ul>
                            <div class="button-group">
                                <button id="btn-confirm-upload" class="btn btn-primary">
                                    Upload to Device
                                </button>
                                <button id="btn-cancel" class="btn btn-secondary">
                                    Cancel
                                </button>
                            </div>
                        </div>
                        
                        <!-- Success message -->
                        <div id="upload-success" class="upload-success" style="display: none;">
                            <div class="success-icon">✓</div>
                            <h3>Application Added Successfully!</h3>
                            <button id="btn-done" class="btn btn-primary">
                                Go to Applications List
                            </button>
                        </div>
                        
                        <!-- Error message -->
                        <div id="upload-error" class="upload-error" style="display: none;">
                            <div class="error-icon">✗</div>
                            <h3 id="error-title">Error</h3>
                            <p id="error-message"></p>
                            <button id="btn-retry" class="btn btn-secondary">
                                Try Again
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
        
        attachEventListeners()
    }
    
    private fun attachEventListeners() {
        // Back button
        (document.getElementById("btn-back") as? HTMLButtonElement)?.addEventListener("click", {
            finish()
        })
        
        // Upload folder button
        (document.getElementById("btn-upload-folder") as? HTMLButtonElement)?.addEventListener("click", {
            selectFolder()
        })
        
        // Upload files button
        (document.getElementById("btn-upload-files") as? HTMLButtonElement)?.addEventListener("click", {
            selectFiles()
        })
        
        // Confirm upload button
        (document.getElementById("btn-confirm-upload") as? HTMLButtonElement)?.addEventListener("click", {
            scope.launch { uploadPendingFiles() }
        })
        
        // Cancel button
        (document.getElementById("btn-cancel") as? HTMLButtonElement)?.addEventListener("click", {
            cancelUpload()
        })
        
        // Done button
        (document.getElementById("btn-done") as? HTMLButtonElement)?.addEventListener("click", {
            navigateToApplicationsList()
        })
        
        // Retry button
        (document.getElementById("btn-retry") as? HTMLButtonElement)?.addEventListener("click", {
            resetToInitialState()
        })
    }
    
    private fun createFileInput(multiple: Boolean, webkitdirectory: Boolean): HTMLInputElement {
        val input = jsCreateInputElement()
        if (multiple) jsSetMultiple(input)
        if (webkitdirectory) jsSetWebkitDirectory(input)
        jsSetAccept(input, ".pff,.pen,.dcf,.fmf,.ent,.csdb,.csdbe,.apc,.mgf,.xlsx,.txt")
        return input
    }
    
    private fun selectFolder() {
        val input = createFileInput(multiple = true, webkitdirectory = true)
        
        input.addEventListener("change", {
            val filesJs = jsGetFileList(input)
            if (filesJs != null) {
                processFolderSelection(filesJs)
            }
        })
        
        input.click()
    }
    
    private fun selectFiles() {
        val input = createFileInput(multiple = true, webkitdirectory = false)
        
        input.addEventListener("change", {
            val filesJs = jsGetFileList(input)
            if (filesJs != null) {
                processFilesSelection(filesJs)
            }
        })
        
        input.click()
    }
    
    private fun processFolderSelection(filesJs: JsAny) {
        pendingFiles.clear()
        
        // Get folder name from the first file's path
        var folderName = "application"
        val length = jsFileListLength(filesJs)
        
        for (i in 0 until length) {
            val fileJs = jsFileListItem(filesJs, i) ?: continue
            val fileName = jsGetFileName(fileJs)
            val fileSize = jsGetFileSize(fileJs)
            val relativePath = jsGetFileWebkitRelativePath(fileJs)
            
            // Extract root folder name
            if (folderName == "application" && relativePath.contains("/")) {
                folderName = relativePath.split("/").firstOrNull() ?: "application"
            }
            
            // Build OPFS path: /applications/{folderName}/...
            val opfsPath = "applications/$relativePath"
            pendingFiles.add(opfsPath to FileWrapper(fileJs, fileName, fileSize))
        }
        
        if (pendingFiles.isNotEmpty()) {
            showPendingPreview()
        } else {
            showError("No files found in the selected folder")
        }
    }
    
    private fun processFilesSelection(filesJs: JsAny) {
        pendingFiles.clear()
        
        // For individual files, create a folder based on PFF name if present
        var folderName: String? = null
        val length = jsFileListLength(filesJs)
        
        // First pass: find PFF file for folder name
        for (i in 0 until length) {
            val fileJs = jsFileListItem(filesJs, i) ?: continue
            val fileName = jsGetFileName(fileJs)
            
            if (fileName.endsWith(".pff", ignoreCase = true)) {
                folderName = fileName.removeSuffix(".pff").removeSuffix(".PFF")
                break
            }
        }
        
        // Default folder name
        if (folderName == null) {
            folderName = "application_${jsDateNow().toLong()}"
        }
        
        // Second pass: add files
        for (i in 0 until length) {
            val fileJs = jsFileListItem(filesJs, i) ?: continue
            val fileName = jsGetFileName(fileJs)
            val fileSize = jsGetFileSize(fileJs)
            val opfsPath = "applications/$folderName/$fileName"
            pendingFiles.add(opfsPath to FileWrapper(fileJs, fileName, fileSize))
        }
        
        if (pendingFiles.isNotEmpty()) {
            showPendingPreview()
        } else {
            showError("No files selected")
        }
    }
    
    private fun showPendingPreview() {
        // Hide upload options
        document.getElementById("card-folder")?.let { (it as HTMLElement).style.display = "none" }
        document.getElementById("card-files")?.let { (it as HTMLElement).style.display = "none" }
        
        // Show pending preview
        val preview = document.getElementById("pending-preview") as? HTMLElement
        preview?.style?.display = "block"
        
        val list = document.getElementById("pending-list") as? HTMLElement
        list?.innerHTML = pendingFiles.take(20).joinToString("") { (_, fileWrapper) ->
            val icon = when {
                fileWrapper.name.endsWith(".pff", ignoreCase = true) -> "📋"
                fileWrapper.name.endsWith(".pen", ignoreCase = true) -> "📝"
                fileWrapper.name.endsWith(".dcf", ignoreCase = true) -> "📖"
                fileWrapper.name.endsWith(".csdb", ignoreCase = true) -> "💾"
                else -> "📄"
            }
            "<li>$icon ${fileWrapper.name} <span class='file-size'>(${formatFileSize(fileWrapper.size.toLong())})</span></li>"
        }
        
        if (pendingFiles.size > 20) {
            list?.innerHTML += "<li class='more-files'>... and ${pendingFiles.size - 20} more files</li>"
        }
    }
    
    private suspend fun uploadPendingFiles() {
        if (isUploading) return
        isUploading = true
        
        // Hide preview, show progress
        (document.getElementById("pending-preview") as? HTMLElement)?.style?.display = "none"
        val progressDiv = document.getElementById("upload-progress") as? HTMLElement
        progressDiv?.style?.display = "block"
        
        val progressBar = document.getElementById("progress-bar") as? HTMLElement
        val progressText = document.getElementById("progress-text") as? HTMLElement
        
        var uploaded = 0
        val total = pendingFiles.size
        
        try {
            for ((path, fileWrapper) in pendingFiles) {
                // Update progress
                val percent = (uploaded * 100) / total
                progressBar?.style?.width = "$percent%"
                progressText?.textContent = "$uploaded / $total files"
                
                // Read file as ArrayBuffer and write to OPFS
                try {
                    println("[AddApplicationActivity] Reading file: ${fileWrapper.name}, size: ${fileWrapper.size}")
                    val arrayBufferPromise = jsReadFileAsArrayBuffer(fileWrapper.jsFile)
                    println("[AddApplicationActivity] Got promise, awaiting...")
                    val arrayBuffer = awaitJsPromise(arrayBufferPromise)
                    println("[AddApplicationActivity] ArrayBuffer received: ${arrayBuffer != null}")
                    if (arrayBuffer != null) {
                        val bufferSize = jsGetArrayBufferByteLength(arrayBuffer)
                        println("[AddApplicationActivity] ArrayBuffer size: $bufferSize bytes")
                        val success = OpfsService.writeFileFromArrayBuffer(path, arrayBuffer)
                        if (success) {
                            println("[AddApplicationActivity] Uploaded: $path ($bufferSize bytes)")
                        } else {
                            println("[AddApplicationActivity] Failed to write: $path")
                        }
                    } else {
                        println("[AddApplicationActivity] ArrayBuffer is null for: $path")
                    }
                } catch (e: Exception) {
                    println("[AddApplicationActivity] Error uploading $path: ${e.message}")
                    e.printStackTrace()
                }
                
                uploaded++
            }
            
            // Complete
            progressBar?.style?.width = "100%"
            progressText?.textContent = "$total / $total files - Complete!"
            
            // Show success after a brief delay
            window.setTimeout({
                progressDiv?.style?.display = "none"
                (document.getElementById("upload-success") as? HTMLElement)?.style?.display = "block"
                null  // Return null for JsAny?
            }, 500)
            
        } catch (e: Exception) {
            showError("Upload failed: ${e.message}")
        } finally {
            isUploading = false
        }
    }
    
    private fun cancelUpload() {
        pendingFiles.clear()
        resetToInitialState()
    }
    
    private fun resetToInitialState() {
        pendingFiles.clear()
        isUploading = false
        
        // Show upload options
        document.getElementById("card-folder")?.let { (it as HTMLElement).style.display = "flex" }
        document.getElementById("card-files")?.let { (it as HTMLElement).style.display = "flex" }
        
        // Hide other sections
        (document.getElementById("pending-preview") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("upload-progress") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("upload-success") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("upload-error") as? HTMLElement)?.style?.display = "none"
    }
    
    private fun showError(message: String) {
        // Hide other sections
        document.getElementById("card-folder")?.let { (it as HTMLElement).style.display = "none" }
        document.getElementById("card-files")?.let { (it as HTMLElement).style.display = "none" }
        (document.getElementById("pending-preview") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("upload-progress") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("upload-success") as? HTMLElement)?.style?.display = "none"
        
        // Show error
        val errorDiv = document.getElementById("upload-error") as? HTMLElement
        errorDiv?.style?.display = "block"
        (document.getElementById("error-message") as? HTMLElement)?.textContent = message
    }
    
    private fun navigateToApplicationsList() {
        // Navigate back to applications list
        ActivityRouter.navigateTo("ApplicationsListActivity")
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    override fun setTitle(title: String) {
        document.title = title
    }
    
    override fun startActivity(activityClass: String, extras: Map<String, Any>?) {
        ActivityRouter.navigateTo(activityClass, extras)
    }
    
    override fun finish() {
        ActivityRouter.navigateTo("ApplicationsListActivity")
    }
    
    // Suspend function to await a JS promise
    private suspend fun awaitJsPromise(promise: JsAny): JsAny? {
        return suspendCancellableCoroutine { continuation ->
            val onResolve: (JsAny?) -> JsAny? = { result ->
                continuation.resume(result, null)
                null
            }
            val onReject: (JsAny?) -> JsAny? = { _ ->
                continuation.resume(null, null)
                null
            }
            jsPromiseThen(promise, onResolve.toJsReference(), onReject.toJsReference())
        }
    }
}
