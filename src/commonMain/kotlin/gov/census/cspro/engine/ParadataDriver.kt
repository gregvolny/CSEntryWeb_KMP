package gov.census.cspro.engine

/**
 * Paradata collection and event logging
 * Ported from Android ParadataDriver
 */
class ParadataDriver {
    
    private val events = mutableListOf<ParadataEvent>()
    
    fun logEvent(event: ParadataEvent) {
        events.add(event)
    }
    
    fun getEvents(): List<ParadataEvent> = events
    
    fun clearEvents() {
        events.clear()
    }
    
    fun getCachedEvents(): List<ParadataEvent> {
        return events.toList()
    }
}

data class ParadataEvent(
    val timestamp: Long,
    val eventType: String,
    val fieldName: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
