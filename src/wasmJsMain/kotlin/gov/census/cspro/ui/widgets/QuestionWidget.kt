package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import gov.census.cspro.data.CaptureType as DataCaptureType
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement

/**
 * Base class for question widgets
 * Web equivalent of Android's QuestionWidget
 * 
 * Multiple fields on screen are displayed in a container.
 * Each field is represented by a QuestionWidget that handles:
 * - Creating the UI elements
 * - Managing field values
 * - Handling user input
 * - Notes/help text display
 * 
 * To add support for a new field type:
 * - Create a new class that extends QuestionWidget
 * - Implement render(), getValue(), setValue(), focus()
 * - Add case to QuestionWidgetFactory
 */
abstract class QuestionWidget(
    val field: CDEField,
    var setFocus: Boolean = false,
    val emitNextPage: Boolean = true,
    val showCodes: Boolean = false
) {
    protected var container: HTMLElement? = null
    protected var onValueChanged: ((String) -> Unit)? = null
    protected var onNextField: (() -> Unit)? = null
    
    // Field note management (matching Android)
    private var fieldNoteText: String = field.note
    private var helpTextShowing: Boolean = false
    private var setFocusToNotes: Boolean = false
    
    /**
     * Check if field has a note
     */
    fun hasNote(): Boolean = fieldNoteText.isNotEmpty()
    
    /**
     * Save the current note and field value to the CDEField object
     * Called before navigating away from field
     */
    fun save() {
        if (!field.isReadOnly && !field.isProtected) {
            copyResponseToField()
        }
        if (!field.isMirror) {
            field.note = fieldNoteText
        }
    }
    
    /**
     * Copy current widget value to field - implemented by subclasses
     */
    protected open fun copyResponseToField() {
        field.value = getValue()
    }
    
    /**
     * Set callback for when field value changes
     */
    fun setOnValueChangedListener(listener: (String) -> Unit) {
        onValueChanged = listener
    }
    
    /**
     * Set callback for when user completes input (e.g., presses Enter)
     */
    fun setOnNextFieldListener(listener: () -> Unit) {
        onNextField = listener
    }
    
    /**
     * Create the widget HTML and attach to the container
     */
    abstract fun render(parentElement: HTMLElement)
    
    /**
     * Get the current value from the widget
     */
    abstract fun getValue(): String
    
    /**
     * Set the widget value
     */
    abstract fun setValue(value: String)
    
    /**
     * Focus the input element
     */
    abstract fun focus()
    
    /**
     * Check if the widget supports response filtering
     */
    open fun supportsResponseFilter(): Boolean = false
    
    /**
     * Filter responses by pattern
     */
    open fun filterResponses(pattern: String) {}
    
    /**
     * Get the field note
     */
    fun getNote(): String = field.note
    
    /**
     * Cleanup when widget is removed
     */
    open fun destroy() {
        container?.innerHTML = ""
        container = null
    }
    
    /**
     * Create the common question header (label, CAPI text, help text, notes)
     * Matches Android's CommonPartsViewHolder layout
     */
    protected fun createQuestionHeader(editNotes: Boolean = false): String {
        val noteIcon = if (hasNote()) {
            """<span class="field-note-icon" title="${escapeHtml(fieldNoteText)}">📝</span>"""
        } else ""
        
        val protectedBadge = if (field.isProtected || field.isReadOnly) {
            """<span class="protected-badge">🔒</span>"""
        } else ""
        
        val mirrorBadge = if (field.isMirror) {
            """<span class="mirror-badge">🔗</span>"""
        } else ""
        
        // Question text (CAPI text) - loaded from URL if available
        val questionText = if (!field.questionTextUrl.isNullOrEmpty()) {
            """<div class="question-text" id="question-text-${field.name}"></div>"""
        } else ""
        
        // Help text - toggle button and content
        val helpTextSection = if (!field.helpTextUrl.isNullOrEmpty()) {
            """
            <button class="toggle-help-btn" id="toggle-help-${field.name}">❓ Help</button>
            <div class="help-text ${if (helpTextShowing) "" else "hidden"}" id="help-text-${field.name}"></div>
            """
        } else ""
        
        // Notes section - editable if editNotes is true
        val notesSection = if (hasNote() || editNotes) {
            val readonlyAttr = if (editNotes) "" else "readonly"
            val minRows = if (editNotes) 4 else 1
            """
            <div class="notes-section">
                <label class="notes-label">📝 Notes</label>
                <textarea 
                    class="notes-input" 
                    id="note-${field.name}" 
                    rows="$minRows"
                    $readonlyAttr
                >${escapeHtml(fieldNoteText)}</textarea>
            </div>
            """
        } else ""
        
        return """
            <div class="question-header">
                <label class="question-label">
                    ${escapeHtml(field.label)}
                    $noteIcon
                    $protectedBadge
                    $mirrorBadge
                </label>
                $questionText
                $helpTextSection
            </div>
            $notesSection
        """.trimIndent()
    }
    
    /**
     * Get index to scroll to initially (for selection widgets)
     * For radio buttons/checkboxes, scroll to selected item
     */
    open val initialScrollPosition: Int get() = 0
    
    /**
     * Set focus to notes field
     */
    fun setFocusToNotes() {
        setFocusToNotes = true
    }
    
    /**
     * Update the note text
     */
    fun setNoteText(text: String) {
        fieldNoteText = text
    }
    
    /**
     * Escape HTML special characters
     */
    protected fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }
}

/**
 * View type codes for use in widget factory and list adapters
 * Matching Android QuestionWidget companion object constants exactly
 */
object ViewType {
    const val QUESTION_COMMON_PARTS = 1
    const val NUMERIC_TEXT_BOX = 2
    const val ALPHA_TEXT_BOX = 3
    const val RADIO_BUTTON_LIST_ITEM = 4
    const val CHECK_BOX_LIST_ITEM = 5
    const val DROP_DOWN = 6
    const val COMBO_BOX_NUMERIC = 7
    const val DATE_PICKER = 8
    const val COMBO_BOX_ALPHA = 9
    const val BARCODE_BUTTON_ALPHA = 10
    const val BARCODE_BUTTON_NUMERIC = 11
    const val TOGGLE_BUTTON_SLIDER = 12
    const val SLIDER_NUMERIC = 13
    const val PHOTO = 14
    const val SIGNATURE = 15
    const val AUDIO = 16
    
    /**
     * Get all view types used by a specific widget type
     * Uses DataCaptureType constants from EntryModels
     */
    fun allItemViewTypes(captureType: Int, dataType: String): IntArray {
        return when (captureType) {
            DataCaptureType.DATA_CAPTURE_TEXT_BOX -> intArrayOf(QUESTION_COMMON_PARTS, 
                if (dataType == DataType.NUMERIC) NUMERIC_TEXT_BOX else ALPHA_TEXT_BOX)
            DataCaptureType.DATA_CAPTURE_RADIO_BUTTON -> intArrayOf(QUESTION_COMMON_PARTS, RADIO_BUTTON_LIST_ITEM)
            DataCaptureType.DATA_CAPTURE_CHECK_BOX -> intArrayOf(QUESTION_COMMON_PARTS, CHECK_BOX_LIST_ITEM)
            DataCaptureType.DATA_CAPTURE_DROP_DOWN -> intArrayOf(QUESTION_COMMON_PARTS, DROP_DOWN)
            DataCaptureType.DATA_CAPTURE_COMBO_BOX -> intArrayOf(QUESTION_COMMON_PARTS, 
                if (dataType == DataType.NUMERIC) COMBO_BOX_NUMERIC else COMBO_BOX_ALPHA)
            DataCaptureType.DATA_CAPTURE_DATE -> intArrayOf(QUESTION_COMMON_PARTS, DATE_PICKER)
            DataCaptureType.DATA_CAPTURE_BARCODE -> intArrayOf(QUESTION_COMMON_PARTS, 
                if (dataType == DataType.NUMERIC) BARCODE_BUTTON_NUMERIC else BARCODE_BUTTON_ALPHA)
            DataCaptureType.DATA_CAPTURE_SLIDER -> intArrayOf(QUESTION_COMMON_PARTS, SLIDER_NUMERIC)
            DataCaptureType.DATA_CAPTURE_TOGGLE_BUTTON -> intArrayOf(QUESTION_COMMON_PARTS, TOGGLE_BUTTON_SLIDER)
            DataCaptureType.DATA_CAPTURE_PHOTO -> intArrayOf(QUESTION_COMMON_PARTS, PHOTO)
            DataCaptureType.DATA_CAPTURE_SIGNATURE -> intArrayOf(QUESTION_COMMON_PARTS, SIGNATURE)
            DataCaptureType.DATA_CAPTURE_AUDIO -> intArrayOf(QUESTION_COMMON_PARTS, AUDIO)
            else -> intArrayOf(QUESTION_COMMON_PARTS, ALPHA_TEXT_BOX)
        }
    }
}

/**
 * Data type constants for fields
 */
object DataType {
    const val NUMERIC = "numeric"
    const val ALPHA = "alpha"
}
