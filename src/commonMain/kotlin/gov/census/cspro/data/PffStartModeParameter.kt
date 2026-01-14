package gov.census.cspro.data

/**
 * PFF start mode parameters
 * Ported from Android PffStartModeParameter.java
 */
data class PffStartModeParameter(
    val action: Int,
    val modifyCasePosition: Double = 0.0
) {
    companion object {
        const val NO_ACTION = 0
        const val ADD_NEW_CASE = 1
        const val MODIFY_CASE = 2
        const val MODIFY_ERROR = 3
    }
}

/**
 * Start mode enum for higher-level abstraction
 */
enum class StartMode {
    Add,
    Modify,
    Verify,
    CaseListing;
    
    fun toPffAction(): Int = when (this) {
        Add -> PffStartModeParameter.ADD_NEW_CASE
        Modify -> PffStartModeParameter.MODIFY_CASE
        Verify -> PffStartModeParameter.MODIFY_CASE
        CaseListing -> PffStartModeParameter.NO_ACTION
    }
    
    companion object {
        fun fromPffAction(action: Int): StartMode = when (action) {
            PffStartModeParameter.ADD_NEW_CASE -> Add
            PffStartModeParameter.MODIFY_CASE -> Modify
            PffStartModeParameter.MODIFY_ERROR -> Modify
            else -> CaseListing
        }
    }
}
