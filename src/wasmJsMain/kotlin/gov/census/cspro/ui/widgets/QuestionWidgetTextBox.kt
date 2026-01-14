package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent

/**
 * Text box widget for alpha fields
 * Web equivalent of Android's QuestionWidgetTextBoxAlpha
 */
class QuestionWidgetTextBoxAlpha(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var inputElement: HTMLInputElement? = null
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "field-${field.name}"
        val maxLength = if (field.dataType == DataType.ALPHA) {
            // Get length from field if available, default to 255
            255
        } else 50
        
        val readOnly = if (field.isProtected) "readonly" else ""
        val disabledClass = if (field.isProtected) "disabled" else ""
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input $disabledClass">
                <input 
                    type="text" 
                    id="$fieldId" 
                    class="field-input field-input-alpha"
                    value="${escapeHtml(field.value)}"
                    maxlength="$maxLength"
                    $readOnly
                    placeholder="Enter text..."
                />
            </div>
        """.trimIndent()
        
        inputElement = document.getElementById(fieldId) as? HTMLInputElement
        
        // Attach event listeners
        inputElement?.addEventListener("input", {
            val value = inputElement?.value ?: ""
            onValueChanged?.invoke(value)
        })
        
        inputElement?.addEventListener("keydown", { event ->
            val keyEvent = event as? KeyboardEvent
            if (keyEvent?.key == "Enter" && emitNextPage) {
                event.preventDefault()
                onNextField?.invoke()
            }
        })
        
        if (setFocus) {
            focus()
        }
    }
    
    override fun getValue(): String {
        return inputElement?.value ?: field.value
    }
    
    override fun setValue(value: String) {
        inputElement?.value = value
        field.value = value
    }
    
    override fun focus() {
        inputElement?.focus()
        inputElement?.select()
    }
}

/**
 * Text box widget for numeric fields
 * Web equivalent of Android's QuestionWidgetTextBoxNumeric
 */
class QuestionWidgetTextBoxNumeric(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var inputElement: HTMLInputElement? = null
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "field-${field.name}"
        val readOnly = if (field.isProtected) "readonly" else ""
        val disabledClass = if (field.isProtected) "disabled" else ""
        
        // Use text input with pattern for numeric to handle leading zeros
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input $disabledClass">
                <input 
                    type="text" 
                    inputmode="numeric"
                    id="$fieldId" 
                    class="field-input field-input-numeric"
                    value="${escapeHtml(field.value)}"
                    pattern="[0-9]*"
                    $readOnly
                    placeholder="Enter number..."
                />
            </div>
        """.trimIndent()
        
        inputElement = document.getElementById(fieldId) as? HTMLInputElement
        
        // Filter non-numeric input
        inputElement?.addEventListener("input", {
            var value = inputElement?.value ?: ""
            // Allow only digits and decimal point
            value = value.replace(Regex("[^0-9.-]"), "")
            inputElement?.value = value
            onValueChanged?.invoke(value)
        })
        
        inputElement?.addEventListener("keydown", { event ->
            val keyEvent = event as? KeyboardEvent
            if (keyEvent?.key == "Enter" && emitNextPage) {
                event.preventDefault()
                onNextField?.invoke()
            }
        })
        
        if (setFocus) {
            focus()
        }
    }
    
    override fun getValue(): String {
        return inputElement?.value ?: field.value
    }
    
    override fun setValue(value: String) {
        inputElement?.value = value
        field.value = value
    }
    
    override fun focus() {
        inputElement?.focus()
        inputElement?.select()
    }
}
