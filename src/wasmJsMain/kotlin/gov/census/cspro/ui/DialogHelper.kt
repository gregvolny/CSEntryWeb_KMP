package gov.census.cspro.ui

import gov.census.cspro.engine.dialogs.CSProDialogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Helper object for showing native CSPro HTML dialogs
 * Replaces browser window.alert() and window.confirm() with native dialogs
 */
object DialogHelper {
    
    private val scope = MainScope()
    
    /**
     * Show an alert dialog (OK button only)
     * Non-blocking version that uses a coroutine
     */
    fun showAlert(title: String, message: String) {
        scope.launch {
            showAlertAsync(title, message)
        }
    }
    
    /**
     * Show an alert dialog (OK button only) - suspending version
     */
    suspend fun showAlertAsync(title: String, message: String) {
        CSProDialogManager.showErrmsg(title, message, listOf("OK"))
    }
    
    /**
     * Show an error dialog
     */
    fun showError(title: String, message: String) {
        scope.launch {
            CSProDialogManager.showErrmsg(title, message, listOf("OK"))
        }
    }
    
    /**
     * Show a confirmation dialog (OK/Cancel)
     * Returns true if OK was clicked
     */
    suspend fun showConfirmAsync(title: String, message: String): Boolean {
        val result = CSProDialogManager.showErrmsg(title, message, listOf("OK", "Cancel"))
        return result == 0 // First button (OK) has index 0
    }
    
    /**
     * Show a Yes/No confirmation dialog
     * Returns true if Yes was clicked
     */
    suspend fun showYesNoAsync(title: String, message: String): Boolean {
        val result = CSProDialogManager.showErrmsg(title, message, listOf("Yes", "No"))
        return result == 0 // First button (Yes) has index 0
    }
    
    /**
     * Show an info dialog (for About, etc.)
     */
    fun showInfo(title: String, message: String) {
        scope.launch {
            CSProDialogManager.showErrmsg(title, message, listOf("OK"))
        }
    }
    
    /**
     * Show the About dialog with CSEntry KMP information
     */
    fun showAbout() {
        scope.launch {
            CSProDialogManager.showErrmsg(
                "About CSEntry",
                "CSEntry Web\nVersion 8.1\n\nA KMP port of CSEntry Android.\nGregoire VOLNY 2025-2026",
                listOf("OK")
            )
        }
    }
    
    /**
     * Show a success message
     */
    fun showSuccess(title: String, message: String) {
        scope.launch {
            CSProDialogManager.showErrmsg(title, message, listOf("OK"))
        }
    }
}
