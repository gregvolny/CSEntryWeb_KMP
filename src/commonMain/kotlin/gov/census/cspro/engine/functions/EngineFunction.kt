package gov.census.cspro.engine.functions

/**
 * Base interface for engine functions that require UI interaction
 * Ported from Android EngineFunction.java
 */
interface EngineFunction {
    /**
     * Execute the engine function
     * Returns when the function is complete
     */
    suspend fun run()
}

/**
 * Result from a modal dialog
 */
data class ModalDialogResult(
    val buttonPressed: Int,
    val cancelled: Boolean = false
) {
    companion object {
        const val BUTTON_OK = 1
        const val BUTTON_CANCEL = 2
        const val BUTTON_YES = 6
        const val BUTTON_NO = 7
    }
}

/**
 * Result from a prompt dialog
 */
data class PromptResult(
    val text: String?,
    val cancelled: Boolean = false
)

/**
 * Result from a list selection dialog
 */
data class ListSelectionResult(
    val selectedIndex: Int,
    val selectedValue: String?,
    val cancelled: Boolean = false
)

/**
 * Result from a choice dialog
 */
data class ChoiceDialogResult(
    val selectedIndices: List<Int>,
    val cancelled: Boolean = false
)

/**
 * Progress dialog configuration
 */
data class ProgressDialogConfig(
    val title: String,
    val message: String,
    val isIndeterminate: Boolean = true,
    val maxProgress: Int = 100,
    val cancellable: Boolean = true
)

/**
 * Login dialog result
 */
data class LoginResult(
    val username: String?,
    val password: String?,
    val cancelled: Boolean = false
)
