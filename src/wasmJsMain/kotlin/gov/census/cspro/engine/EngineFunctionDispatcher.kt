package gov.census.cspro.engine

import gov.census.cspro.engine.functions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Engine Function Dispatcher
 * Handles routing of engine function requests to the appropriate UI functions
 * Ported from Android's use of EngineFunction interface
 */
object EngineFunctionDispatcher {
    
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Display a modal dialog (alert/confirm)
     */
    suspend fun showModalDialog(title: String, message: String, type: Int): ModalDialogResult {
        val dialog = ModalDialogFunction(title, message, type)
        dialog.run()
        return dialog.getResult()
    }
    
    /**
     * Display an error message
     */
    suspend fun showError(message: String, isWarning: Boolean = false) {
        val errMsg = ErrmsgFunction(message, isWarning)
        errMsg.run()
    }
    
    /**
     * Display a prompt dialog
     */
    suspend fun showPrompt(
        title: String,
        message: String,
        initialValue: String = "",
        isPassword: Boolean = false,
        isNumeric: Boolean = false,
        maxLength: Int = -1
    ): PromptResult {
        val prompt = PromptFunction(title, message, initialValue, isPassword, isNumeric, maxLength)
        prompt.run()
        return prompt.getResult()
    }
    
    /**
     * Display a list selection dialog
     */
    suspend fun showList(
        title: String,
        items: List<String>,
        allowMultiple: Boolean = false,
        initialSelection: Int = -1
    ): ListSelectionResult {
        val list = ShowListFunction(title, items, allowMultiple, initialSelection)
        list.run()
        return list.getResult()
    }
    
    /**
     * Display a choice dialog with checkboxes
     */
    suspend fun showChoiceDialog(
        title: String,
        items: List<String>,
        initialSelections: List<Int> = emptyList()
    ): ChoiceDialogResult {
        val dialog = ChoiceDialogFunction(title, items, initialSelections)
        dialog.run()
        return dialog.getResult()
    }
    
    /**
     * Display a login dialog
     */
    suspend fun showLoginDialog(
        title: String = "Login",
        message: String = "",
        initialUsername: String = ""
    ): LoginResult {
        val login = LoginDialogFunction(title, message, initialUsername)
        login.run()
        return login.getResult()
    }
    
    /**
     * Display a password dialog
     */
    suspend fun showPasswordDialog(
        title: String = "Enter Password",
        message: String = ""
    ): PromptResult {
        val password = PasswordQueryFunction(title, message)
        password.run()
        return PromptResult(password.getPassword(), password.isCancelled())
    }
    
    /**
     * Show progress dialog
     */
    fun showProgress(
        title: String,
        message: String,
        indeterminate: Boolean = true,
        cancellable: Boolean = true,
        onCancel: (() -> Unit)? = null
    ) {
        ProgressDialogManager.show(title, message, indeterminate, cancellable, onCancel)
    }
    
    /**
     * Update progress dialog
     */
    fun updateProgress(progress: Int, message: String? = null) {
        ProgressDialogManager.updateProgress(progress, message)
    }
    
    /**
     * Update progress message only
     */
    fun updateProgressMessage(message: String) {
        ProgressDialogManager.updateMessage(message)
    }
    
    /**
     * Dismiss progress dialog
     */
    fun dismissProgress() {
        ProgressDialogManager.dismiss()
    }
    
    /**
     * Show HTML dialog
     */
    suspend fun showHtmlDialog(
        url: String,
        accessToken: String? = null,
        title: String? = null,
        width: Int? = null,
        height: Int? = null
    ): String? {
        val dialog = DisplayCSHtmlDlgFunction(url, accessToken, title, width, height)
        dialog.run()
        return if (dialog.isCancelled()) null else dialog.getResult()
    }
    
    /**
     * Edit a field note
     */
    suspend fun editNote(
        title: String = "Edit Note",
        initialNote: String = "",
        fieldName: String = ""
    ): String? {
        val editor = EditNoteFunction(title, initialNote, fieldName)
        editor.run()
        return if (editor.isCancelled()) null else editor.getNoteText()
    }
    
    /**
     * Show file picker dialog
     */
    suspend fun showFilePicker(
        mimeTypes: List<String> = listOf("*/*"),
        multiple: Boolean = false
    ): List<SelectedFile> {
        val dialog = SelectDocumentDialogFunction(mimeTypes, multiple)
        dialog.run()
        return dialog.getSelectedFiles()
    }
    
    /**
     * Trigger file download
     */
    suspend fun downloadFile(
        filename: String,
        content: String,
        mimeType: String = "text/plain"
    ): Boolean {
        val dialog = SaveFileDialogFunction(filename, content, mimeType)
        dialog.run()
        return dialog.isSaved()
    }
    
    /**
     * Trigger file download with bytes
     */
    suspend fun downloadBytes(
        filename: String,
        content: ByteArray,
        mimeType: String = "application/octet-stream"
    ): Boolean {
        val dialog = SaveBytesDialogFunction(filename, content, mimeType)
        dialog.run()
        return dialog.isSaved()
    }
    
    /**
     * Get a system property
     */
    fun getProperty(name: String): String {
        return PropertyFunction.getProperty(name)
    }
    
    /**
     * Set a custom property
     */
    fun setProperty(name: String, value: String): Boolean {
        return PropertyFunction.setProperty(name, value)
    }
    
    /**
     * Execute a system command (limited on web)
     */
    fun execSystem(command: String): Int {
        return ExecSystemFunction.exec(command)
    }
}

/**
 * Extension functions for easy access
 */
suspend fun CSProEngineService.showAlert(title: String, message: String): ModalDialogResult {
    return EngineFunctionDispatcher.showModalDialog(title, message, ModalDialogFunction.MB_OK)
}

suspend fun CSProEngineService.showConfirm(title: String, message: String): ModalDialogResult {
    return EngineFunctionDispatcher.showModalDialog(title, message, ModalDialogFunction.MB_YESNO)
}

suspend fun CSProEngineService.showPrompt(title: String, message: String, initialValue: String = ""): PromptResult {
    return EngineFunctionDispatcher.showPrompt(title, message, initialValue)
}

suspend fun CSProEngineService.showError(message: String) {
    EngineFunctionDispatcher.showError(message, false)
}

suspend fun CSProEngineService.showWarning(message: String) {
    EngineFunctionDispatcher.showError(message, true)
}
