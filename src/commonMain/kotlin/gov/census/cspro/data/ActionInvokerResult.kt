package gov.census.cspro.data

/**
 * Action invoker result
 * Ported from Android ActionInvokerActivityResult
 */
data class ActionInvokerResult(
    val success: Boolean,
    val resultCode: Int,
    val message: String = "",
    val data: Map<String, String> = emptyMap()
)
