package gov.census.cspro.engine.dialogs

/**
 * Dialog Configuration for CSPro Native HTML Dialogs
 * All dialogs use native CSPro HTML templates via iframe
 */
object DialogConfig {
    /**
     * Base path to dialog HTML files
     */
    var dialogBasePath = "/dialogs"
    
    init {
        CSProDialogManager.dialogBasePath = dialogBasePath
    }
}

/**
 * Native Dialog Functions - uses CSPro HTML dialogs exclusively
 * All dialogs use the native CSPro HTML templates via iframe
 */
object NativeDialogFunctions {
    
    // Match Windows C++ message box constants
    const val MB_OK = 0
    const val MB_OKCANCEL = 1
    const val MB_YESNO = 4
    
    // Button result constants
    const val BUTTON_OK = 1
    const val BUTTON_CANCEL = 2
    const val BUTTON_YES = 6
    const val BUTTON_NO = 7
    
    /**
     * Show a modal dialog (OK, OK/Cancel, Yes/No)
     * Uses: errmsg.html
     */
    suspend fun showModalDialog(
        title: String,
        message: String,
        type: Int
    ): ModalDialogResult {
        val buttons = when (type) {
            MB_OK -> listOf("OK")
            MB_OKCANCEL -> listOf("OK", "Cancel")
            MB_YESNO -> listOf("Yes", "No")
            else -> listOf("OK")
        }
        
        val result = CSProDialogManager.showErrmsg(title, message, buttons)
        
        val buttonResult = when (type) {
            MB_OK -> BUTTON_OK
            MB_OKCANCEL -> if (result == 0) BUTTON_OK else BUTTON_CANCEL
            MB_YESNO -> if (result == 0) BUTTON_YES else BUTTON_NO
            else -> BUTTON_OK
        }
        
        return ModalDialogResult(buttonResult)
    }
    
    /**
     * Show a prompt dialog for text input
     * Uses: text-input.html
     */
    suspend fun showPrompt(
        title: String,
        initialValue: String = "",
        multiline: Boolean = false
    ): PromptResult {
        val result = CSProDialogManager.showTextInput(
            title = title,
            initialValue = initialValue,
            multiline = multiline
        )
        
        return PromptResult(
            value = result,
            cancelled = result == null
        )
    }
    
    /**
     * Show an error message dialog
     * Uses: errmsg.html
     */
    suspend fun showErrmsg(
        title: String,
        message: String,
        buttons: List<String>? = null
    ): Int {
        return CSProDialogManager.showErrmsg(title, message, buttons)
    }
    
    /**
     * Show a choice dialog
     * Uses: select.html
     */
    suspend fun showChoice(
        title: String,
        choices: List<String>,
        allowDirectInput: Boolean = false
    ): Int {
        val result = CSProDialogManager.showChoice(title, choices, allowDirectInput)
        // Return 0 if cancelled, otherwise 1-indexed selection
        return if (result.selectedIndex < 0) 0 else result.selectedIndex + 1
    }
    
    /**
     * Show a list selection dialog
     * Uses: select.html
     */
    suspend fun showList(
        title: String,
        items: List<String>,
        allowMultiple: Boolean = false
    ): ListSelectionResult {
        val result = CSProDialogManager.showChoice(title, items)
        return ListSelectionResult(
            selectedIndex = result.selectedIndex,
            selectedValue = if (result.selectedIndex >= 0) items.getOrNull(result.selectedIndex) else null,
            cancelled = result.selectedIndex < 0
        )
    }
    
    /**
     * Show a note edit dialog
     * Uses: note-edit.html
     */
    suspend fun showNoteEdit(
        title: String,
        initialNote: String,
        operatorId: String = ""
    ): NoteResult {
        val result = CSProDialogManager.showNoteEdit(title, initialNote, operatorId)
        return NoteResult(
            note = result,
            cancelled = result == null
        )
    }
    
    /**
     * Show an image viewer
     * Uses: Image-view.html
     */
    suspend fun showImage(imageUrl: String, title: String? = null) {
        CSProDialogManager.showImageViewer(imageUrl, title)
    }
    
    /**
     * Show a file selection dialog
     * Uses: Path-selectFile.html
     */
    suspend fun showFileDialog(
        title: String = "Select File",
        rootDir: String = "",
        filter: String? = null,
        selectDirectory: Boolean = false
    ): FileDialogResult {
        val result = CSProDialogManager.showFileDialog(title, rootDir, filter, selectDirectory)
        return FileDialogResult(
            path = result,
            cancelled = result == null
        )
    }
    
    /**
     * Show a custom HTML dialog
     * Uses: Any URL/HTML file
     */
    suspend fun showHtmlDialog(
        dialogPath: String,
        inputData: String? = null,
        width: Int? = null,
        height: Int? = null
    ): String? {
        val input = if (inputData != null) {
            kotlinx.serialization.json.Json.parseToJsonElement(inputData)
        } else null
        
        val result = CSProDialogManager.showDialog(dialogPath, input, width, height)
        return result?.toString()
    }
    
    /**
     * Show a login dialog
     * Uses: text-input.html for each field
     */
    suspend fun showLoginDialog(
        serverName: String,
        showInvalidLoginError: Boolean = false
    ): LoginResult {
        // Use text input for username
        val titlePrefix = "Login to $serverName"
        val title = if (showInvalidLoginError) "$titlePrefix\nInvalid username or password" else titlePrefix
        
        val usernameResult = CSProDialogManager.showTextInput(
            title = title,
            initialValue = ""
        )
        
        if (usernameResult == null) {
            return LoginResult(null, null, true)
        }
        
        // Use text input for password  
        val passwordResult = CSProDialogManager.showTextInput(
            title = "Enter password",
            initialValue = ""
        )
        
        if (passwordResult == null) {
            return LoginResult(null, null, true)
        }
        
        return LoginResult(usernameResult, passwordResult, false)
    }
    
    /**
     * Show a password query dialog
     * Uses: text-input.html
     */
    suspend fun showPasswordQuery(
        title: String,
        description: String,
        requireConfirmation: Boolean = true
    ): PasswordResult {
        val passwordResult = CSProDialogManager.showTextInput(
            title = "$title\n$description",
            initialValue = ""
        )
        
        if (passwordResult == null) {
            return PasswordResult(null, true)
        }
        
        if (requireConfirmation) {
            val confirmResult = CSProDialogManager.showTextInput(
                title = "Re-enter password",
                initialValue = ""
            )
            
            if (confirmResult == null) {
                return PasswordResult(null, true)
            }
            
            if (confirmResult != passwordResult) {
                // Passwords don't match - show error and retry
                CSProDialogManager.showErrmsg("Error", "Passwords do not match", listOf("OK"))
                return showPasswordQuery(title, description, requireConfirmation)
            }
        }
        
        return PasswordResult(passwordResult, false)
    }
    
    /**
     * Show a progress dialog
     * Uses: CSProDialogManager progress dialog
     */
    fun showProgress(
        message: String,
        total: Int = 0,
        cancellable: Boolean = true
    ): ProgressHandle {
        val handle = CSProDialogManager.showProgressDialog("", message, total, cancellable)
        return ProgressHandle(handle)
    }
}

// Result data classes

data class ModalDialogResult(val button: Int) {
    companion object {
        const val BUTTON_OK = 1
        const val BUTTON_CANCEL = 2
        const val BUTTON_YES = 6
        const val BUTTON_NO = 7
    }
}

data class PromptResult(
    val value: String?,
    val cancelled: Boolean
)

data class ListSelectionResult(
    val selectedIndex: Int,
    val selectedValue: String?,
    val cancelled: Boolean
)

data class NoteResult(
    val note: String?,
    val cancelled: Boolean
)

data class FileDialogResult(
    val path: String?,
    val cancelled: Boolean
)

// LoginResult is defined in commonMain: gov.census.cspro.engine.functions.LoginResult
typealias LoginResult = gov.census.cspro.engine.functions.LoginResult

data class PasswordResult(
    val password: String?,
    val cancelled: Boolean
)

/**
 * Handle for controlling a progress dialog
 */
class ProgressHandle(private val handle: CSProDialogManager.ProgressDialogHandle) {
    fun update(message: String? = null, progress: Int? = null) {
        handle.update(message, progress)
    }
    
    fun close() {
        handle.close()
    }
    
    fun isCancelled(): Boolean = handle.isCancelled()
}
