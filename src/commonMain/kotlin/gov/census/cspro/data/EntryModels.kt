package gov.census.cspro.data

/**
 * Represents a case tree node for navigation
 * Web equivalent of Android's CaseTreeNode from form package
 * 
 * Matches Android's CaseTreeNode.kt exactly with all properties and constants
 */
data class CaseTreeNode(
    var id: Int = 0,
    var name: String = "",
    var label: String = "",
    var value: String = "",
    var type: Int = ITEM_FIELD,
    var color: Int = FRM_FIELDCOLOR_NEVERVISITED,
    var fieldSymbol: Int = 0,
    var fieldIndex: IntArray = intArrayOf(0, 0, 0),
    var visible: Boolean = true,
    var expanded: Boolean = false,
    var matchesFilter: Boolean = false,
    val children: MutableList<CaseTreeNode> = mutableListOf()
) {
    /**
     * Add a child node (called from C++)
     */
    fun addChild(node: CaseTreeNode) {
        children.add(node)
    }
    
    /**
     * Insert child at specific index
     */
    fun insertChild(childIndex: Int, node: CaseTreeNode) {
        children.add(childIndex, node)
    }
    
    /**
     * Remove child at specific index
     */
    fun removeChild(childIndex: Int) {
        children.removeAt(childIndex)
    }
    
    /**
     * Copy content from another node
     */
    fun copyContent(newNode: CaseTreeNode) {
        id = newNode.id
        name = newNode.name
        label = newNode.label
        value = newNode.value
        type = newNode.type
        color = newNode.color
        fieldSymbol = newNode.fieldSymbol
        fieldIndex = newNode.fieldIndex
        expanded = true
        visible = newNode.visible
    }
    
    /**
     * Check if this node or any child is the current group
     */
    val isCurrentGroup: Boolean
        get() {
            for (c in children) {
                if (c.type == ITEM_FIELD) {
                    if (c.color == FRM_FIELDCOLOR_CURRENT) return true
                }
                if (c.isCurrentGroup) return true
            }
            return false
        }
    
    /**
     * Check if this is the current item (for highlighting)
     */
    val isCurrentItem: Boolean
        get() = color == FRM_FIELDCOLOR_CURRENT
    
    /**
     * Check if this item was skipped
     */
    val isSkipped: Boolean
        get() = color == FRM_FIELDCOLOR_SKIPPED
    
    /**
     * Check if field has a value
     */
    val hasValue: Boolean
        get() = color == FRM_FIELDCOLOR_VISITED || color == FRM_FIELDCOLOR_CURRENT
    
    /**
     * Get occurrence number from node (for groups)
     */
    val occurrenceNumber: Int
        get() = if (type == ITEM_GROUP_OCCURRENCE && fieldIndex.isNotEmpty()) fieldIndex[0] + 1 else 1
    
    /**
     * Get total occurrence count (placeholder for dynamic lookup)
     */
    val occurrenceCount: Int
        get() = 1 // Would be populated from engine
    
    companion object {
        // Node types - matching Android CaseTreeNode.kt exactly
        const val ITEM_LEVEL = 0
        const val ITEM_GROUP = 1
        const val ITEM_GROUP_PLACEHOLDER = 2
        const val ITEM_GROUP_OCCURRENCE = 3
        const val ITEM_FIELD = 4
        
        // Field colors - matching Android CaseTreeNode.kt exactly
        const val FRM_FIELDCOLOR_PARENT = -1       // Only valid for fields
        const val FRM_FIELDCOLOR_NEVERVISITED = 0
        const val FRM_FIELDCOLOR_SKIPPED = 1
        const val FRM_FIELDCOLOR_VISITED = 2
        const val FRM_FIELDCOLOR_CURRENT = 3
        const val FRM_FIELDCOLOR_PROTECTED = 4
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CaseTreeNode
        return id == other.id
    }
    
    override fun hashCode(): Int = id
}

/**
 * Represents an update to the case tree
 * Web equivalent of Android's CaseTreeUpdate from form package
 * Matches Android's CaseTreeUpdate.kt exactly
 */
data class CaseTreeUpdate(
    val type: Int,
    val node: CaseTreeNode?,
    val parentNodeId: Int = 0,
    val childIndex: Int = 0
) {
    companion object {
        const val NODE_MODIFIED = 1
        const val NODE_ADDED = 2
        const val NODE_REMOVED = 3
    }
}

/**
 * Represents a field note for UI display
 * Web equivalent of Android's FieldNote from form package
 * NOTE: The primary FieldNote definition is in DataModels.kt
 */
data class UIFieldNote(
    val noteIndex: Long,
    val fieldName: String,
    val fieldLabel: String,
    val noteText: String,
    val modifiedTime: Long = 0
)

/**
 * Represents a value set entry for dropdown/radio selections
 * Web equivalent of Android's ValuePair from dict package
 */
data class ValueSetEntry(
    val code: String,
    val label: String,
    val imagePath: String? = null,
    val textColor: Int = 0xFF000000.toInt(),
    val isSelectable: Boolean = true
) {
    // Alias for backward compatibility
    val value: String get() = code
    val imageUrl: String? get() = imagePath
}

/**
 * Represents an entry page with fields
 * Web equivalent of Android's EntryPage from form package
 */
data class EntryPage(
    val blockName: String,
    val blockLabel: String,
    val occurrenceLabel: String = "",
    val questionTextUrl: String? = null,
    val helpTextUrl: String? = null,
    val currentFieldIndex: Int = 0,
    val fields: List<CDEField>
)

/**
 * Capture type constants matching Android's CDEField constants
 * These integer values match the C++ engine DATA_CAPTURE_* values
 */
object CaptureType {
    // String constants for web usage
    const val TEXT_BOX = "textbox"
    const val RADIO_BUTTON = "radiobutton"
    const val CHECK_BOX = "checkbox"
    const val DROP_DOWN = "dropdown"
    const val COMBO_BOX = "combobox"
    const val DATE = "date"
    const val SLIDER = "slider"
    const val TOGGLE = "togglebutton"
    const val BARCODE = "barcode"
    const val PHOTO = "photo"
    const val SIGNATURE = "signature"
    const val AUDIO = "audio"
    
    // Integer constants matching C++ engine (from CDEField.kt)
    const val DATA_CAPTURE_TEXT_BOX = 0
    const val DATA_CAPTURE_RADIO_BUTTON = 1
    const val DATA_CAPTURE_CHECK_BOX = 2
    const val DATA_CAPTURE_DROP_DOWN = 3
    const val DATA_CAPTURE_COMBO_BOX = 4
    const val DATA_CAPTURE_DATE = 5
    const val DATA_CAPTURE_BARCODE = 7
    const val DATA_CAPTURE_SLIDER = 8
    const val DATA_CAPTURE_TOGGLE_BUTTON = 9
    const val DATA_CAPTURE_PHOTO = 10
    const val DATA_CAPTURE_SIGNATURE = 11
    const val DATA_CAPTURE_AUDIO = 12
    
    /**
     * Convert integer capture type to string
     */
    fun fromInt(value: Int): String {
        return when (value) {
            DATA_CAPTURE_TEXT_BOX -> TEXT_BOX
            DATA_CAPTURE_RADIO_BUTTON -> RADIO_BUTTON
            DATA_CAPTURE_CHECK_BOX -> CHECK_BOX
            DATA_CAPTURE_DROP_DOWN -> DROP_DOWN
            DATA_CAPTURE_COMBO_BOX -> COMBO_BOX
            DATA_CAPTURE_DATE -> DATE
            DATA_CAPTURE_BARCODE -> BARCODE
            DATA_CAPTURE_SLIDER -> SLIDER
            DATA_CAPTURE_TOGGLE_BUTTON -> TOGGLE
            DATA_CAPTURE_PHOTO -> PHOTO
            DATA_CAPTURE_SIGNATURE -> SIGNATURE
            DATA_CAPTURE_AUDIO -> AUDIO
            else -> TEXT_BOX
        }
    }
}

/**
 * Extended CDEField with UI-specific properties
 * Web equivalent of Android's CDEField from form package
 * 
 * This matches the native CDEField class which has:
 * - Native instance reference for JNI calls
 * - Properties for field metadata
 * - Value getters/setters that sync with native
 */
data class CDEField(
    val name: String,
    val label: String,
    var value: String,
    val dataType: String, // "numeric", "alpha"
    val captureType: String, // See CaptureType constants
    val captureTypeInt: Int = 0, // Native capture type integer
    val responses: List<Pair<String, String>> = emptyList(), // Value-label pairs for radio/combo (legacy)
    val valueSet: List<ValueSetEntry> = emptyList(), // Full value set with images
    val displayValue: String = "",
    
    // Field dimensions (from native)
    val integerPartLength: Int = 10,
    val fractionalPartLength: Int = 0,
    val alphaLength: Int = 255,
    val length: Int = 10, // Computed total length
    val decimalPlaces: Int = 0,
    
    // Field flags
    val isNumeric: Boolean = false,
    val isAlpha: Boolean = true,
    val isProtected: Boolean = false,
    val isReadOnly: Boolean = false,
    val isRequired: Boolean = false,
    val isUpperCase: Boolean = false,
    val isMultiline: Boolean = false,
    val isMirror: Boolean = false,
    
    // Field note
    var note: String = "",
    
    // Question/help text URLs
    val questionTextUrl: String? = null,
    val helpTextUrl: String? = null,
    
    // Validation constraints
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null, // Regex pattern for validation
    
    // Checkbox specific
    val maxCheckboxSelections: Int = Int.MAX_VALUE,
    
    // Slider-specific properties
    val sliderMin: Double = 0.0,
    val sliderMax: Double = 100.0,
    val sliderStep: Double = 1.0,
    
    // Date-specific properties
    val captureDateFormat: String = "",
    val dateFormat: String? = null, // "YYYYMMDD", "MMDDYYYY", etc.
    
    // Additional metadata
    val fieldNumber: Int = 0,
    val occurrenceNumber: Int = 1,
    val persistentId: Long = 0,
    
    // Value tracking for different types
    var numericValue: Double? = null,
    var alphaValue: String? = null,
    var selectedIndex: Int = -1,
    var checkedIndices: IntArray = IntArray(0)
) {
    /**
     * Get responses as list of pairs (for backward compatibility)
     */
    fun getResponsePairs(): List<Pair<String, String>> {
        return if (responses.isNotEmpty()) {
            responses
        } else {
            valueSet.map { Pair(it.value, it.label) }
        }
    }
}

