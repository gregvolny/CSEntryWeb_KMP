package gov.census.cspro.data

import kotlinx.serialization.Serializable

/**
 * Represents a summary of a data entry case
 * Ported from Android to be platform-independent
 * 
 * Matches Android CaseSummary fields from gov.census.cspro.data.CaseSummary
 */
@Serializable
data class CaseSummary(
    val label: String,
    val questionnaireId: String = "",
    val caseIds: List<String> = emptyList(),
    val partialSave: Boolean = false,
    val hasNotes: Boolean = false,
    val positionInRepository: Double = -1.0,  // Added: case position for modifyCase
    val caseNote: String = ""  // Added: case note text
) {
    /**
     * Get trimmed key for display (matches Android GetTrimmedKeyForDisplay)
     */
    fun getTrimmedKeyForDisplay(useCaseLabel: Boolean): String {
        val key = if (useCaseLabel) label else questionnaireId
        return key.trim()
    }
}

/**
 * Represents a key-value pair for form data
 */
@Serializable
data class ValuePair(
    val code: String,
    val label: String
)

/**
 * Represents field note information
 */
@Serializable
data class FieldNote(
    val index: Long,
    val note: String,
    val operatorId: String,
    val isFieldNote: Boolean,
    val groupSymbolIndex: Int,
    val groupLabel: String,
    val label: String
)
