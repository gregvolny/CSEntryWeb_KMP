package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.KeyboardEvent

/**
 * Dropdown widget for single-select fields
 * Web equivalent of Android's QuestionWidgetDropDown
 */
class QuestionWidgetDropDown(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var selectElement: HTMLSelectElement? = null
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "select-${field.name}"
        val disabled = if (field.isProtected) "disabled" else ""
        
        val optionsHtml = buildString {
            append("""<option value="">-- Select --</option>""")
            field.responses.forEach { (value, label) ->
                val selected = if (value == field.value) "selected" else ""
                val displayLabel = if (showCodes && value.isNotEmpty()) {
                    "$value - $label"
                } else {
                    label
                }
                append("""<option value="${escapeHtml(value)}" $selected>${escapeHtml(displayLabel)}</option>""")
            }
        }
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input">
                <select id="$fieldId" class="field-select" $disabled>
                    $optionsHtml
                </select>
            </div>
        """.trimIndent()
        
        selectElement = document.getElementById(fieldId) as? HTMLSelectElement
        
        selectElement?.addEventListener("change", {
            val value = selectElement?.value ?: ""
            onValueChanged?.invoke(value)
            
            if (emitNextPage && value.isNotEmpty()) {
                onNextField?.invoke()
            }
        })
        
        if (setFocus) {
            focus()
        }
    }
    
    override fun getValue(): String = selectElement?.value ?: field.value
    
    override fun setValue(value: String) {
        selectElement?.value = value
        field.value = value
    }
    
    override fun focus() {
        selectElement?.focus()
    }
}

/**
 * Date picker widget
 * Web equivalent of Android's QuestionWidgetDate
 */
class QuestionWidgetDate(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var inputElement: HTMLInputElement? = null
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "date-${field.name}"
        val readOnly = if (field.isProtected) "readonly" else ""
        
        // Convert CSPro date format (YYYYMMDD) to HTML date format (YYYY-MM-DD)
        val htmlValue = convertToHtmlDate(field.value)
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input">
                <input 
                    type="date" 
                    id="$fieldId" 
                    class="field-input field-input-date"
                    value="$htmlValue"
                    $readOnly
                />
            </div>
        """.trimIndent()
        
        inputElement = document.getElementById(fieldId) as? HTMLInputElement
        
        inputElement?.addEventListener("change", {
            val htmlDate = inputElement?.value ?: ""
            val csproDate = convertFromHtmlDate(htmlDate)
            onValueChanged?.invoke(csproDate)
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
        val htmlDate = inputElement?.value ?: ""
        return convertFromHtmlDate(htmlDate)
    }
    
    override fun setValue(value: String) {
        val htmlDate = convertToHtmlDate(value)
        inputElement?.value = htmlDate
        field.value = value
    }
    
    override fun focus() {
        inputElement?.focus()
    }
    
    /**
     * Convert CSPro date (YYYYMMDD) to HTML date (YYYY-MM-DD)
     */
    private fun convertToHtmlDate(csproDate: String): String {
        if (csproDate.length != 8) return ""
        return try {
            "${csproDate.substring(0, 4)}-${csproDate.substring(4, 6)}-${csproDate.substring(6, 8)}"
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Convert HTML date (YYYY-MM-DD) to CSPro date (YYYYMMDD)
     */
    private fun convertFromHtmlDate(htmlDate: String): String {
        if (htmlDate.isEmpty()) return ""
        return htmlDate.replace("-", "")
    }
}

/**
 * Slider widget for numeric fields
 * Web equivalent of Android's QuestionWidgetSliderNumeric
 */
class QuestionWidgetSlider(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var sliderElement: HTMLInputElement? = null
    private var valueDisplay: HTMLElement? = null
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "slider-${field.name}"
        val valueId = "slider-value-${field.name}"
        val disabled = if (field.isProtected) "disabled" else ""
        
        // Get min/max from responses if available
        val min = field.responses.firstOrNull()?.first?.toIntOrNull() ?: 0
        val max = field.responses.lastOrNull()?.first?.toIntOrNull() ?: 100
        val currentValue = field.value.toIntOrNull() ?: min
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input slider-container">
                <input 
                    type="range" 
                    id="$fieldId" 
                    class="field-slider"
                    min="$min"
                    max="$max"
                    value="$currentValue"
                    $disabled
                />
                <span id="$valueId" class="slider-value">$currentValue</span>
            </div>
        """.trimIndent()
        
        sliderElement = document.getElementById(fieldId) as? HTMLInputElement
        valueDisplay = document.getElementById(valueId) as? HTMLElement
        
        sliderElement?.addEventListener("input", {
            val value = sliderElement?.value ?: "0"
            valueDisplay?.textContent = value
            onValueChanged?.invoke(value)
        })
        
        sliderElement?.addEventListener("change", {
            if (emitNextPage) {
                onNextField?.invoke()
            }
        })
        
        if (setFocus) {
            focus()
        }
    }
    
    override fun getValue(): String = sliderElement?.value ?: field.value
    
    override fun setValue(value: String) {
        sliderElement?.value = value
        valueDisplay?.textContent = value
        field.value = value
    }
    
    override fun focus() {
        sliderElement?.focus()
    }
}
