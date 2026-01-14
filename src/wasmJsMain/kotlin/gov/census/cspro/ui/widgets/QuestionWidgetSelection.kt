package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList

/**
 * Radio button widget for single-select fields
 * Web equivalent of Android's QuestionWidgetRadioButtons
 */
class QuestionWidgetRadioButtons(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var selectedValue: String = field.value
    private var allResponses: List<Pair<String, String>> = field.responses
    private var filteredResponses: List<Pair<String, String>> = field.responses
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        renderOptions()
    }
    
    private fun renderOptions() {
        val container = this.container ?: return
        
        val fieldName = "radio-${field.name}"
        val disabledAttr = if (field.isProtected) "disabled" else ""
        
        val optionsHtml = filteredResponses.mapIndexed { index, (value, label) ->
            val checked = if (value == selectedValue) "checked" else ""
            val displayLabel = if (showCodes && value.isNotEmpty()) {
                "$value - $label"
            } else {
                label
            }
            """
            <label class="radio-option">
                <input 
                    type="radio" 
                    name="$fieldName" 
                    value="${escapeHtml(value)}"
                    $checked
                    $disabledAttr
                />
                <span class="radio-label">${escapeHtml(displayLabel)}</span>
            </label>
            """.trimIndent()
        }.joinToString("\n")
        
        container.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input radio-group">
                $optionsHtml
            </div>
        """.trimIndent()
        
        // Attach event listeners to all radio buttons
        val radioButtons = container.querySelectorAll("input[type='radio']").asList()
        radioButtons.forEach { element ->
            element.addEventListener("change", { event ->
                val input = event.target as? HTMLInputElement
                if (input?.checked == true) {
                    selectedValue = input.value
                    onValueChanged?.invoke(selectedValue)
                    
                    if (emitNextPage) {
                        onNextField?.invoke()
                    }
                }
            })
        }
        
        if (setFocus && radioButtons.isNotEmpty()) {
            (radioButtons.first() as? HTMLInputElement)?.focus()
        }
    }
    
    override fun getValue(): String = selectedValue
    
    override fun setValue(value: String) {
        selectedValue = value
        field.value = value
        
        // Update UI
        container?.querySelectorAll("input[type='radio']")?.asList()?.forEach { element ->
            val input = element as? HTMLInputElement
            input?.checked = (input?.value == value)
        }
    }
    
    override fun focus() {
        container?.querySelector("input[type='radio']")?.let {
            (it as? HTMLInputElement)?.focus()
        }
    }
    
    override fun supportsResponseFilter(): Boolean = true
    
    override fun filterResponses(pattern: String) {
        filteredResponses = if (pattern.isEmpty()) {
            allResponses
        } else {
            val lowerPattern = pattern.lowercase()
            allResponses.filter { (value, label) ->
                value.lowercase().contains(lowerPattern) ||
                label.lowercase().contains(lowerPattern)
            }
        }
        renderOptions()
    }
}

/**
 * Checkbox widget for multi-select fields
 * Web equivalent of Android's QuestionWidgetCheckBoxes
 */
class QuestionWidgetCheckBoxes(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var selectedValues: MutableSet<String> = parseInitialValue(field.value)
    private var allResponses: List<Pair<String, String>> = field.responses
    private var filteredResponses: List<Pair<String, String>> = field.responses
    
    private fun parseInitialValue(value: String): MutableSet<String> {
        // Checkboxes may have comma-separated values
        return if (value.isNotEmpty()) {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        } else {
            mutableSetOf()
        }
    }
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        renderOptions()
    }
    
    private fun renderOptions() {
        val container = this.container ?: return
        
        val fieldName = "checkbox-${field.name}"
        val disabledAttr = if (field.isProtected) "disabled" else ""
        
        val optionsHtml = filteredResponses.mapIndexed { index, (value, label) ->
            val checked = if (selectedValues.contains(value)) "checked" else ""
            val displayLabel = if (showCodes && value.isNotEmpty()) {
                "$value - $label"
            } else {
                label
            }
            """
            <label class="checkbox-option">
                <input 
                    type="checkbox" 
                    name="$fieldName" 
                    value="${escapeHtml(value)}"
                    $checked
                    $disabledAttr
                />
                <span class="checkbox-label">${escapeHtml(displayLabel)}</span>
            </label>
            """.trimIndent()
        }.joinToString("\n")
        
        container.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input checkbox-group">
                $optionsHtml
            </div>
        """.trimIndent()
        
        // Attach event listeners to all checkboxes
        val checkboxes = container.querySelectorAll("input[type='checkbox']").asList()
        checkboxes.forEach { element ->
            element.addEventListener("change", { event ->
                val input = event.target as? HTMLInputElement
                val value = input?.value ?: return@addEventListener
                
                if (input.checked) {
                    selectedValues.add(value)
                } else {
                    selectedValues.remove(value)
                }
                
                onValueChanged?.invoke(getValue())
            })
        }
        
        if (setFocus && checkboxes.isNotEmpty()) {
            (checkboxes.first() as? HTMLInputElement)?.focus()
        }
    }
    
    override fun getValue(): String = selectedValues.joinToString(",")
    
    override fun setValue(value: String) {
        selectedValues = parseInitialValue(value)
        field.value = value
        
        // Update UI
        container?.querySelectorAll("input[type='checkbox']")?.asList()?.forEach { element ->
            val input = element as? HTMLInputElement
            input?.checked = selectedValues.contains(input?.value)
        }
    }
    
    override fun focus() {
        container?.querySelector("input[type='checkbox']")?.let {
            (it as? HTMLInputElement)?.focus()
        }
    }
    
    override fun supportsResponseFilter(): Boolean = true
    
    override fun filterResponses(pattern: String) {
        filteredResponses = if (pattern.isEmpty()) {
            allResponses
        } else {
            val lowerPattern = pattern.lowercase()
            allResponses.filter { (value, label) ->
                value.lowercase().contains(lowerPattern) ||
                label.lowercase().contains(lowerPattern)
            }
        }
        renderOptions()
    }
}
