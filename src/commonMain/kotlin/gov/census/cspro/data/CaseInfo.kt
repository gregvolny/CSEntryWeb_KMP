package gov.census.cspro.data

/**
 * Case metadata for case listing
 * Ported from Android CaseSummary
 */
data class CaseInfo(
    val id: String,
    val label: String = "",
    val isComplete: Boolean = false,
    val isPartial: Boolean = false,
    val lastModified: Long = 0, // milliseconds since epoch
    val positionInRepository: Double = 0.0
) {
    val status: CaseStatus
        get() = when {
            isComplete -> CaseStatus.Complete
            isPartial -> CaseStatus.Partial
            else -> CaseStatus.NotStarted
        }
}

enum class CaseStatus {
    NotStarted,
    Partial,
    Complete
}
