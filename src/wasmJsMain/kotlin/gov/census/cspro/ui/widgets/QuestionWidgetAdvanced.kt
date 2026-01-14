package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import gov.census.cspro.data.ValueSetEntry
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.asList
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.Event

// Top-level @JsFun helpers for WASM-compatible JS interop

@JsFun("(e) => e.key")
private external fun jsGetEventKey(e: JsAny): String

@JsFun("(fieldName) => { navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } }).then(function(stream) { var video = document.getElementById('video-' + fieldName); if (video) { video.srcObject = stream; video._stream = stream; } }).catch(function(err) { console.error('Error accessing camera:', err); alert('Could not access camera: ' + err.message); }); }")
private external fun jsStartCamera(fieldName: String)

@JsFun("(fieldName) => { var video = document.getElementById('video-' + fieldName); if (video && video._stream) { video._stream.getTracks().forEach(function(track) { track.stop(); }); video.srcObject = null; } }")
private external fun jsStopCamera(fieldName: String)

/**
 * ComboBox widget for alpha fields with searchable dropdown
 * Web equivalent of Android's QuestionWidgetComboBoxAlpha
 */
open class QuestionWidgetComboBoxAlpha(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var inputElement: HTMLInputElement? = null
    private var dropdownElement: HTMLElement? = null
    private var currentValue: String = field.alphaValue ?: field.value
    private var allResponses: List<ValueSetEntry> = field.valueSet
    private var filteredResponses: List<ValueSetEntry> = field.valueSet
    private var isDropdownOpen = false
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "field-${field.name}"
        val dropdownId = "dropdown-${field.name}"
        val readOnly = if (field.isProtected) "readonly" else ""
        val disabledClass = if (field.isProtected) "disabled" else ""
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input combobox-container $disabledClass">
                <input 
                    type="text" 
                    id="$fieldId" 
                    class="field-input combobox-input"
                    value="${escapeHtml(currentValue)}"
                    placeholder="Type to search..."
                    autocomplete="off"
                    $readOnly
                />
                <button class="combobox-toggle" id="toggle-$fieldId">▼</button>
                <div class="combobox-dropdown hidden" id="$dropdownId">
                    <!-- Options will be rendered here -->
                </div>
            </div>
        """.trimIndent()
        
        inputElement = document.getElementById(fieldId) as? HTMLInputElement
        dropdownElement = document.getElementById(dropdownId) as? HTMLElement
        
        // Render initial options
        renderDropdownOptions()
        
        // Input event - filter as user types
        inputElement?.addEventListener("input", {
            val value = inputElement?.value ?: ""
            currentValue = value
            filterAndShowDropdown(value)
            onValueChanged?.invoke(value)
        })
        
        // Focus event - show dropdown
        inputElement?.addEventListener("focus", {
            showDropdown()
        })
        
        // Keydown event
        inputElement?.addEventListener("keydown", { event ->
            val jsEvent = event.toJsReference()
            val key = jsGetEventKey(jsEvent)
            when (key) {
                "Enter" -> {
                    hideDropdown()
                    if (emitNextPage) {
                        event.preventDefault()
                        onNextField?.invoke()
                    }
                }
                "Escape" -> hideDropdown()
                "ArrowDown" -> {
                    if (!isDropdownOpen) showDropdown()
                }
            }
        })
        
        // Toggle button
        val toggleBtn = document.getElementById("toggle-$fieldId") as? HTMLButtonElement
        toggleBtn?.addEventListener("click", {
            if (isDropdownOpen) hideDropdown() else showDropdown()
        })
        
        // Click outside to close
        document.addEventListener("click", { event ->
            val target = event.target as? HTMLElement
            val dropdown = dropdownElement
            if (target != inputElement && target != toggleBtn && 
                dropdown != null && !dropdown.contains(target)) {
                hideDropdown()
            }
        })
        
        if (setFocus) {
            focus()
        }
    }
    
    private fun renderDropdownOptions() {
        val dropdown = dropdownElement ?: return
        
        val optionsHtml = filteredResponses.map { entry ->
            val displayLabel = if (showCodes && entry.code.isNotEmpty()) {
                "${entry.code} - ${entry.label}"
            } else {
                entry.label
            }
            val selectedClass = if (entry.code == currentValue || entry.label == currentValue) "selected" else ""
            """<div class="combobox-option $selectedClass" data-value="${escapeHtml(entry.code)}">${escapeHtml(displayLabel)}</div>"""
        }.joinToString("\n")
        
        dropdown.innerHTML = if (optionsHtml.isEmpty()) {
            """<div class="combobox-no-results">No matches found</div>"""
        } else {
            optionsHtml
        }
        
        // Attach click handlers
        dropdown.querySelectorAll(".combobox-option").asList().forEach { opt ->
            val option = opt as? HTMLElement ?: return@forEach
            option.addEventListener("click", {
                val value = option.getAttribute("data-value") ?: ""
                selectValue(value)
            })
        }
    }
    
    private fun filterAndShowDropdown(pattern: String) {
        filteredResponses = if (pattern.isEmpty()) {
            allResponses
        } else {
            val lowerPattern = pattern.lowercase()
            allResponses.filter { entry ->
                entry.code.lowercase().contains(lowerPattern) ||
                entry.label.lowercase().contains(lowerPattern)
            }
        }
        renderDropdownOptions()
        showDropdown()
    }
    
    private fun showDropdown() {
        dropdownElement?.classList?.remove("hidden")
        isDropdownOpen = true
    }
    
    private fun hideDropdown() {
        dropdownElement?.classList?.add("hidden")
        isDropdownOpen = false
    }
    
    private fun selectValue(value: String) {
        currentValue = value
        inputElement?.value = value
        hideDropdown()
        onValueChanged?.invoke(value)
        
        if (emitNextPage) {
            onNextField?.invoke()
        }
    }
    
    override fun getValue(): String = currentValue
    
    override fun setValue(value: String) {
        currentValue = value
        inputElement?.value = value
        field.alphaValue = value
    }
    
    override fun focus() {
        inputElement?.focus()
    }
    
    override fun supportsResponseFilter(): Boolean = true
    
    override fun filterResponses(pattern: String) {
        filterAndShowDropdown(pattern)
    }
}

/**
 * ComboBox widget for numeric fields
 * Web equivalent of Android's QuestionWidgetComboBoxNumeric
 */
class QuestionWidgetComboBoxNumeric(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidgetComboBoxAlpha(field, setFocus, emitNextPage, showCodes) {
    
    override fun copyResponseToField() {
        val value = getValue()
        field.numericValue = value.toDoubleOrNull()
    }
}

/**
 * Barcode widget for alpha fields
 * Web equivalent of Android's QuestionWidgetBarcodeAlpha
 * Uses camera API for barcode scanning
 */
open class QuestionWidgetBarcodeAlpha(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var inputElement: HTMLInputElement? = null
    private var currentValue: String = field.alphaValue ?: field.value
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldId = "field-${field.name}"
        val readOnly = if (field.isProtected) "readonly" else ""
        val disabledClass = if (field.isProtected) "disabled" else ""
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input barcode-container $disabledClass">
                <input 
                    type="text" 
                    id="$fieldId" 
                    class="field-input barcode-input"
                    value="${escapeHtml(currentValue)}"
                    placeholder="Scan or enter barcode..."
                    $readOnly
                />
                <button class="barcode-scan-btn" id="scan-$fieldId" ${if (field.isProtected) "disabled" else ""}>
                    📷 Scan
                </button>
            </div>
            <div class="barcode-scanner hidden" id="scanner-${field.name}">
                <video id="video-${field.name}" class="scanner-video" autoplay></video>
                <button class="scanner-close-btn" id="close-scanner-${field.name}">✕ Close</button>
            </div>
        """.trimIndent()
        
        inputElement = document.getElementById(fieldId) as? HTMLInputElement
        
        // Input change handler
        inputElement?.addEventListener("input", {
            currentValue = inputElement?.value ?: ""
            onValueChanged?.invoke(currentValue)
        })
        
        // Keydown for Enter
        inputElement?.addEventListener("keydown", { event ->
            val jsEvent = event.toJsReference()
            val key = jsGetEventKey(jsEvent)
            if (key == "Enter" && emitNextPage) {
                event.preventDefault()
                onNextField?.invoke()
            }
        })
        
        // Scan button - opens camera
        val scanBtn = document.getElementById("scan-$fieldId") as? HTMLButtonElement
        scanBtn?.addEventListener("click", {
            startBarcodeScanner()
        })
        
        // Close scanner button
        val closeBtn = document.getElementById("close-scanner-${field.name}") as? HTMLButtonElement
        closeBtn?.addEventListener("click", {
            stopBarcodeScanner()
        })
        
        if (setFocus) {
            focus()
        }
    }
    
    private fun startBarcodeScanner() {
        val scannerDiv = document.getElementById("scanner-${field.name}") as? HTMLElement
        scannerDiv?.classList?.remove("hidden")
        
        // Note: Actual barcode detection would require a library like ZXing.js or QuaggaJS
        // For now, we just show the camera feed using the top-level @JsFun helper
        jsStartCamera(field.name)
    }
    
    private fun stopBarcodeScanner() {
        val scannerDiv = document.getElementById("scanner-${field.name}") as? HTMLElement
        scannerDiv?.classList?.add("hidden")
        
        // Stop the video stream using the top-level @JsFun helper
        jsStopCamera(field.name)
    }
    
    override fun getValue(): String = currentValue
    
    override fun setValue(value: String) {
        currentValue = value
        inputElement?.value = value
        field.alphaValue = value
    }
    
    override fun focus() {
        inputElement?.focus()
    }
    
    override fun destroy() {
        stopBarcodeScanner()
        super.destroy()
    }
}

/**
 * Barcode widget for numeric fields
 * Web equivalent of Android's QuestionWidgetBarcodeNumeric
 */
class QuestionWidgetBarcodeNumeric(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidgetBarcodeAlpha(field, setFocus, emitNextPage, showCodes) {
    
    override fun copyResponseToField() {
        val value = getValue()
        field.numericValue = value.toDoubleOrNull()
    }
}

/**
 * Toggle button widget - styled radio buttons
 * Web equivalent of Android's QuestionWidgetToggleButton
 */
class QuestionWidgetToggleButton(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var selectedValue: String = ""
    private var selectedIndex: Int = field.selectedIndex
    private var allResponses: List<ValueSetEntry> = field.valueSet
    
    init {
        // Get initial selection
        if (selectedIndex >= 0 && selectedIndex < allResponses.size) {
            selectedValue = allResponses[selectedIndex].code
        }
    }
    
    override fun render(parentElement: HTMLElement) {
        container = parentElement
        
        val fieldName = "toggle-${field.name}"
        val disabledClass = if (field.isProtected) "disabled" else ""
        
        val buttonsHtml = allResponses.mapIndexed { index, entry ->
            val selected = if (index == selectedIndex) "selected" else ""
            val disabled = if (field.isProtected) "disabled" else ""
            val displayLabel = if (showCodes && entry.code.isNotEmpty()) {
                "${entry.code} - ${entry.label}"
            } else {
                entry.label
            }
            
            // Add image if available
            val imageHtml = if (!entry.imagePath.isNullOrEmpty()) {
                """<img src="${escapeHtml(entry.imagePath)}" class="toggle-image" alt="" />"""
            } else ""
            
            """
            <button 
                type="button"
                class="toggle-button $selected" 
                data-index="$index"
                data-value="${escapeHtml(entry.code)}"
                $disabled
            >
                $imageHtml
                <span class="toggle-label">${escapeHtml(displayLabel)}</span>
            </button>
            """.trimIndent()
        }.joinToString("\n")
        
        parentElement.innerHTML = """
            ${createQuestionHeader()}
            <div class="question-input toggle-group $disabledClass">
                $buttonsHtml
            </div>
        """.trimIndent()
        
        // Attach click handlers
        parentElement.querySelectorAll(".toggle-button").asList().forEach { elem ->
            val button = elem as? HTMLElement ?: return@forEach
            button.addEventListener("click", { event ->
                if (field.isProtected) return@addEventListener
                
                val btn = event.currentTarget as? HTMLElement ?: return@addEventListener
                val index = btn.getAttribute("data-index")?.toIntOrNull() ?: return@addEventListener
                val value = btn.getAttribute("data-value") ?: ""
                
                selectOption(index, value)
            })
        }
        
        if (setFocus) {
            focus()
        }
    }
    
    private fun selectOption(index: Int, value: String) {
        // Update UI - remove selected from all, add to clicked
        container?.querySelectorAll(".toggle-button")?.asList()?.forEach { btn ->
            (btn as? HTMLElement)?.classList?.remove("selected")
        }
        container?.querySelector(".toggle-button[data-index='$index']")?.let {
            (it as? HTMLElement)?.classList?.add("selected")
        }
        
        selectedIndex = index
        selectedValue = value
        
        onValueChanged?.invoke(value)
        
        if (emitNextPage) {
            onNextField?.invoke()
        }
    }
    
    override fun getValue(): String = selectedValue
    
    override fun setValue(value: String) {
        selectedValue = value
        // Find index for this value
        val index = allResponses.indexOfFirst { it.code == value }
        if (index >= 0) {
            selectedIndex = index
            field.selectedIndex = index
        }
    }
    
    override fun copyResponseToField() {
        field.selectedIndex = selectedIndex
    }
    
    override fun focus() {
        container?.querySelector(".toggle-button")?.let {
            (it as? HTMLButtonElement)?.focus()
        }
    }
    
    override val initialScrollPosition: Int get() = if (selectedIndex >= 0) selectedIndex else 0
}
