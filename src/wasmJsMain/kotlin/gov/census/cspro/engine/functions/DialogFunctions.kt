package gov.census.cspro.engine.functions

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

/**
 * Modal Dialog Function - displays OK, OK/Cancel, or Yes/No dialogs
 * Ported from Android ModalDialogFunction.java
 */
class ModalDialogFunction(
    private val title: String,
    private val message: String,
    private val type: Int
) : EngineFunction {
    
    companion object {
        // Match Windows C++ message box constants
        const val MB_OK = 0
        const val MB_OKCANCEL = 1
        const val MB_YESNO = 4
    }
    
    private var result: ModalDialogResult = ModalDialogResult(ModalDialogResult.BUTTON_OK)
    
    fun getResult(): ModalDialogResult = result
    
    override suspend fun run() {
        val deferred = CompletableDeferred<ModalDialogResult>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-modal-overlay"
        overlay.style.apply {
            position = "fixed"
            top = "0"
            left = "0"
            width = "100%"
            height = "100%"
            backgroundColor = "rgba(0, 0, 0, 0.5)"
            display = "flex"
            justifyContent = "center"
            alignItems = "center"
            zIndex = "10000"
        }
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.apply {
            backgroundColor = "white"
            borderRadius = "8px"
            padding = "20px"
            minWidth = "300px"
            maxWidth = "500px"
            boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
        }
        
        // Title
        val titleEl = document.createElement("h3") as HTMLElement
        titleEl.textContent = title
        titleEl.style.apply {
            margin = "0 0 15px 0"
            color = "#333"
        }
        dialog.appendChild(titleEl)
        
        // Message
        val messageEl = document.createElement("p") as HTMLElement
        messageEl.textContent = message
        messageEl.style.apply {
            margin = "0 0 20px 0"
            color = "#666"
        }
        dialog.appendChild(messageEl)
        
        // Buttons container
        val buttonContainer = document.createElement("div") as HTMLDivElement
        buttonContainer.style.apply {
            display = "flex"
            justifyContent = "flex-end"
            setProperty("gap", "10px")
        }
        
        fun createButton(text: String, isPrimary: Boolean): HTMLButtonElement {
            val btn = document.createElement("button") as HTMLButtonElement
            btn.textContent = text
            btn.style.apply {
                padding = "8px 20px"
                border = if (isPrimary) "none" else "1px solid #ccc"
                borderRadius = "4px"
                cursor = "pointer"
                fontSize = "14px"
                backgroundColor = if (isPrimary) "#007bff" else "#fff"
                color = if (isPrimary) "#fff" else "#333"
            }
            return btn
        }
        
        fun closeDialog(buttonResult: Int) {
            document.body?.removeChild(overlay)
            deferred.complete(ModalDialogResult(buttonResult))
        }
        
        when (type) {
            MB_OK -> {
                val okBtn = createButton("OK", true)
                okBtn.onclick = { closeDialog(ModalDialogResult.BUTTON_OK) }
                buttonContainer.appendChild(okBtn)
            }
            MB_OKCANCEL -> {
                val cancelBtn = createButton("Cancel", false)
                cancelBtn.onclick = { closeDialog(ModalDialogResult.BUTTON_CANCEL) }
                buttonContainer.appendChild(cancelBtn)
                
                val okBtn = createButton("OK", true)
                okBtn.onclick = { closeDialog(ModalDialogResult.BUTTON_OK) }
                buttonContainer.appendChild(okBtn)
            }
            MB_YESNO -> {
                val noBtn = createButton("No", false)
                noBtn.onclick = { closeDialog(ModalDialogResult.BUTTON_NO) }
                buttonContainer.appendChild(noBtn)
                
                val yesBtn = createButton("Yes", true)
                yesBtn.onclick = { closeDialog(ModalDialogResult.BUTTON_YES) }
                buttonContainer.appendChild(yesBtn)
            }
        }
        
        dialog.appendChild(buttonContainer)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        result = deferred.await()
    }
}

/**
 * Prompt Function - displays an input dialog
 * Ported from Android PromptFunction.java
 */
class PromptFunction(
    private val title: String,
    private val message: String,
    private val initialValue: String = "",
    private val isPassword: Boolean = false,
    private val isNumeric: Boolean = false,
    private val maxLength: Int = -1
) : EngineFunction {
    
    private var result: PromptResult = PromptResult(null, true)
    
    fun getResult(): PromptResult = result
    
    override suspend fun run() {
        val deferred = CompletableDeferred<PromptResult>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-prompt-overlay"
        overlay.style.apply {
            position = "fixed"
            top = "0"
            left = "0"
            width = "100%"
            height = "100%"
            backgroundColor = "rgba(0, 0, 0, 0.5)"
            display = "flex"
            justifyContent = "center"
            alignItems = "center"
            zIndex = "10000"
        }
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.apply {
            backgroundColor = "white"
            borderRadius = "8px"
            padding = "20px"
            minWidth = "350px"
            maxWidth = "500px"
            boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
        }
        
        // Title
        val titleEl = document.createElement("h3") as HTMLElement
        titleEl.textContent = title
        titleEl.style.margin = "0 0 15px 0"
        dialog.appendChild(titleEl)
        
        // Message
        if (message.isNotEmpty()) {
            val messageEl = document.createElement("p") as HTMLElement
            messageEl.textContent = message
            messageEl.style.margin = "0 0 15px 0"
            dialog.appendChild(messageEl)
        }
        
        // Input
        val input = document.createElement("input") as HTMLInputElement
        input.type = when {
            isPassword -> "password"
            isNumeric -> "number"
            else -> "text"
        }
        input.value = initialValue
        if (maxLength > 0) {
            input.maxLength = maxLength
        }
        input.style.apply {
            width = "100%"
            padding = "10px"
            fontSize = "14px"
            border = "1px solid #ccc"
            borderRadius = "4px"
            boxSizing = "border-box"
            marginBottom = "20px"
        }
        dialog.appendChild(input)
        
        // Buttons
        val buttonContainer = document.createElement("div") as HTMLDivElement
        buttonContainer.style.apply {
            display = "flex"
            justifyContent = "flex-end"
            setProperty("gap", "10px")
        }
        
        fun closeDialog(value: String?, cancelled: Boolean) {
            document.body?.removeChild(overlay)
            deferred.complete(PromptResult(value, cancelled))
        }
        
        val cancelBtn = document.createElement("button") as HTMLButtonElement
        cancelBtn.textContent = "Cancel"
        cancelBtn.style.apply {
            padding = "8px 20px"
            border = "1px solid #ccc"
            borderRadius = "4px"
            cursor = "pointer"
            backgroundColor = "#fff"
        }
        cancelBtn.onclick = { closeDialog(null, true) }
        buttonContainer.appendChild(cancelBtn)
        
        val okBtn = document.createElement("button") as HTMLButtonElement
        okBtn.textContent = "OK"
        okBtn.style.apply {
            padding = "8px 20px"
            border = "none"
            borderRadius = "4px"
            cursor = "pointer"
            backgroundColor = "#007bff"
            color = "#fff"
        }
        okBtn.onclick = { closeDialog(input.value, false) }
        buttonContainer.appendChild(okBtn)
        
        // Handle Enter key
        input.onkeydown = { event ->
            val keyEvent = event.unsafeCast<org.w3c.dom.events.KeyboardEvent>()
            if (keyEvent.key == "Enter") {
                closeDialog(input.value, false)
            }
            Unit
        }
        
        dialog.appendChild(buttonContainer)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        // Focus input
        input.focus()
        
        result = deferred.await()
    }
}

/**
 * Error Message Function - displays an error message
 * Ported from Android ErrmsgFunction.java
 */
class ErrmsgFunction(
    private val message: String,
    private val isWarning: Boolean = false
) : EngineFunction {
    
    override suspend fun run() {
        val title = if (isWarning) "Warning" else "Error"
        val dialog = ModalDialogFunction(title, message, ModalDialogFunction.MB_OK)
        dialog.run()
    }
}

/**
 * Show List Function - displays a list selection dialog
 * Ported from Android ShowListFunction.java
 */
class ShowListFunction(
    private val title: String,
    private val items: List<String>,
    private val allowMultiple: Boolean = false,
    private val initialSelection: Int = -1
) : EngineFunction {
    
    private var result: ListSelectionResult = ListSelectionResult(-1, null, true)
    
    fun getResult(): ListSelectionResult = result
    
    override suspend fun run() {
        val deferred = CompletableDeferred<ListSelectionResult>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-list-overlay"
        overlay.style.apply {
            position = "fixed"
            top = "0"
            left = "0"
            width = "100%"
            height = "100%"
            backgroundColor = "rgba(0, 0, 0, 0.5)"
            display = "flex"
            justifyContent = "center"
            alignItems = "center"
            zIndex = "10000"
        }
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.apply {
            backgroundColor = "white"
            borderRadius = "8px"
            padding = "20px"
            minWidth = "300px"
            maxWidth = "400px"
            maxHeight = "80vh"
            display = "flex"
            flexDirection = "column"
            boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
        }
        
        // Title
        val titleEl = document.createElement("h3") as HTMLElement
        titleEl.textContent = title
        titleEl.style.margin = "0 0 15px 0"
        dialog.appendChild(titleEl)
        
        // List container
        val listContainer = document.createElement("div") as HTMLDivElement
        listContainer.style.apply {
            overflowY = "auto"
            maxHeight = "300px"
            border = "1px solid #ddd"
            borderRadius = "4px"
            marginBottom = "15px"
        }
        
        var selectedIndex = initialSelection
        val listItems = mutableListOf<HTMLDivElement>()
        
        items.forEachIndexed { index, item ->
            val listItem = document.createElement("div") as HTMLDivElement
            listItem.textContent = item
            listItem.style.apply {
                padding = "10px 15px"
                cursor = "pointer"
                borderBottom = if (index < items.size - 1) "1px solid #eee" else "none"
                backgroundColor = if (index == selectedIndex) "#e3f2fd" else "transparent"
            }
            
            listItem.onclick = {
                // Update selection
                listItems.forEachIndexed { i, li ->
                    li.style.backgroundColor = if (i == index) "#e3f2fd" else "transparent"
                }
                selectedIndex = index
                Unit
            }
            
            listItem.ondblclick = {
                document.body?.removeChild(overlay)
                deferred.complete(ListSelectionResult(index, item, false))
                Unit
            }
            
            listItems.add(listItem)
            listContainer.appendChild(listItem)
        }
        
        dialog.appendChild(listContainer)
        
        // Buttons
        val buttonContainer = document.createElement("div") as HTMLDivElement
        buttonContainer.style.apply {
            display = "flex"
            justifyContent = "flex-end"
            setProperty("gap", "10px")
        }
        
        val cancelBtn = document.createElement("button") as HTMLButtonElement
        cancelBtn.textContent = "Cancel"
        cancelBtn.style.apply {
            padding = "8px 20px"
            border = "1px solid #ccc"
            borderRadius = "4px"
            cursor = "pointer"
            backgroundColor = "#fff"
        }
        cancelBtn.onclick = {
            document.body?.removeChild(overlay)
            deferred.complete(ListSelectionResult(-1, null, true))
        }
        buttonContainer.appendChild(cancelBtn)
        
        val okBtn = document.createElement("button") as HTMLButtonElement
        okBtn.textContent = "Select"
        okBtn.style.apply {
            padding = "8px 20px"
            border = "none"
            borderRadius = "4px"
            cursor = "pointer"
            backgroundColor = "#007bff"
            color = "#fff"
        }
        okBtn.onclick = {
            document.body?.removeChild(overlay)
            val selectedValue = if (selectedIndex >= 0) items[selectedIndex] else null
            deferred.complete(ListSelectionResult(selectedIndex, selectedValue, false))
        }
        buttonContainer.appendChild(okBtn)
        
        dialog.appendChild(buttonContainer)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        result = deferred.await()
    }
}

/**
 * Choice Dialog Function - displays a multi-select choice dialog
 * Ported from Android ChoiceDialog.java
 */
class ChoiceDialogFunction(
    private val title: String,
    private val items: List<String>,
    private val initialSelections: List<Int> = emptyList()
) : EngineFunction {
    
    private var result: ChoiceDialogResult = ChoiceDialogResult(emptyList(), true)
    
    fun getResult(): ChoiceDialogResult = result
    
    override suspend fun run() {
        val deferred = CompletableDeferred<ChoiceDialogResult>()
        
        val selectedIndices = initialSelections.toMutableSet()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-choice-overlay"
        overlay.style.apply {
            position = "fixed"
            top = "0"
            left = "0"
            width = "100%"
            height = "100%"
            backgroundColor = "rgba(0, 0, 0, 0.5)"
            display = "flex"
            justifyContent = "center"
            alignItems = "center"
            zIndex = "10000"
        }
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.apply {
            backgroundColor = "white"
            borderRadius = "8px"
            padding = "20px"
            minWidth = "300px"
            maxWidth = "400px"
            maxHeight = "80vh"
            display = "flex"
            flexDirection = "column"
            boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
        }
        
        // Title
        val titleEl = document.createElement("h3") as HTMLElement
        titleEl.textContent = title
        titleEl.style.margin = "0 0 15px 0"
        dialog.appendChild(titleEl)
        
        // List container with checkboxes
        val listContainer = document.createElement("div") as HTMLDivElement
        listContainer.style.apply {
            overflowY = "auto"
            maxHeight = "300px"
            marginBottom = "15px"
        }
        
        items.forEachIndexed { index, item ->
            val itemContainer = document.createElement("div") as HTMLDivElement
            itemContainer.style.apply {
                display = "flex"
                alignItems = "center"
                padding = "8px 0"
            }
            
            val checkbox = document.createElement("input") as HTMLInputElement
            checkbox.type = "checkbox"
            checkbox.checked = index in selectedIndices
            checkbox.style.marginRight = "10px"
            checkbox.onchange = {
                if (checkbox.checked) {
                    selectedIndices.add(index)
                } else {
                    selectedIndices.remove(index)
                }
                Unit
            }
            
            val label = document.createElement("label") as HTMLElement
            label.textContent = item
            label.style.cursor = "pointer"
            label.onclick = {
                checkbox.checked = !checkbox.checked
                if (checkbox.checked) {
                    selectedIndices.add(index)
                } else {
                    selectedIndices.remove(index)
                }
                Unit
            }
            
            itemContainer.appendChild(checkbox)
            itemContainer.appendChild(label)
            listContainer.appendChild(itemContainer)
        }
        
        dialog.appendChild(listContainer)
        
        // Buttons
        val buttonContainer = document.createElement("div") as HTMLDivElement
        buttonContainer.style.apply {
            display = "flex"
            justifyContent = "flex-end"
            setProperty("gap", "10px")
        }
        
        val cancelBtn = document.createElement("button") as HTMLButtonElement
        cancelBtn.textContent = "Cancel"
        cancelBtn.style.apply {
            padding = "8px 20px"
            border = "1px solid #ccc"
            borderRadius = "4px"
            cursor = "pointer"
            backgroundColor = "#fff"
        }
        cancelBtn.onclick = {
            document.body?.removeChild(overlay)
            deferred.complete(ChoiceDialogResult(emptyList(), true))
        }
        buttonContainer.appendChild(cancelBtn)
        
        val okBtn = document.createElement("button") as HTMLButtonElement
        okBtn.textContent = "OK"
        okBtn.style.apply {
            padding = "8px 20px"
            border = "none"
            borderRadius = "4px"
            cursor = "pointer"
            backgroundColor = "#007bff"
            color = "#fff"
        }
        okBtn.onclick = {
            document.body?.removeChild(overlay)
            deferred.complete(ChoiceDialogResult(selectedIndices.sorted(), false))
        }
        buttonContainer.appendChild(okBtn)
        
        dialog.appendChild(buttonContainer)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        result = deferred.await()
    }
}
