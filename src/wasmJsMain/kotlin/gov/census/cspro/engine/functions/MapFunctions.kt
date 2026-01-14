package gov.census.cspro.engine.functions

import gov.census.cspro.maps.*
import kotlinx.coroutines.CompletableDeferred

/**
 * Map Show Engine Function
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/engine/functions/MapShowFunction.java
 */
class MapShowFunction(private val mapUI: MapUI) {
    
    /**
     * Execute map show
     * @return 1 on success, 0 on failure
     */
    suspend fun execute(): Int {
        return mapUI.show()
    }
}

/**
 * Map Hide Engine Function
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/engine/functions/MapHideFunction.java
 */
class MapHideFunction(private val mapUI: MapUI) {
    
    /**
     * Execute map hide
     * @return 1 on success, 0 on failure
     */
    suspend fun execute(): Int {
        return mapUI.hide()
    }
}

/**
 * Map Manager - singleton for managing map instances
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/maps/MapManager.java
 */
object MapManager {
    private var currentMap: MapUI? = null
    private var eventDeferred: CompletableDeferred<MapEvent>? = null
    
    /**
     * Get or create current map instance
     */
    fun currentMap(): MapUI {
        if (currentMap == null) {
            currentMap = MapUI()
        }
        return currentMap!!
    }
    
    /**
     * Show the current map
     */
    suspend fun showMap(mapUI: MapUI): Int {
        currentMap = mapUI
        return mapUI.show()
    }
    
    /**
     * Hide the current map
     */
    suspend fun hideMap(): Int {
        val result = currentMap?.hide() ?: 0
        currentMap = null
        return result
    }
    
    /**
     * Wait for map event
     */
    suspend fun waitForEvent(): MapEvent? {
        return currentMap?.waitForEvent()
    }
    
    /**
     * Create new map instance
     */
    fun createMap(): MapUI {
        return MapUI()
    }
    
    /**
     * Check if a map is currently showing
     */
    fun isMapShowing(): Boolean {
        return currentMap != null
    }
    
    /**
     * Dispatch map event
     */
    fun onMapEvent(event: MapEvent) {
        eventDeferred?.complete(event)
        eventDeferred = null
    }
}

/**
 * Capture Polygon Function for capturing geographic polygons
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/maps/CapturePolygonActivity.kt
 */
class CapturePolygonFunction(
    private val baseMapSelection: BaseMapSelection = BaseMapSelection()
) {
    private var capturedPoints = mutableListOf<Pair<Double, Double>>()
    
    /**
     * Execute polygon capture
     * @return GeoJSON string of the captured polygon, or empty on cancel
     */
    suspend fun execute(): String {
        val map = MapUI()
        
        // Configure map for polygon capture
        map.setBaseMap(baseMapSelection)
        map.setShowCurrentLocation(true)
        map.setTitle("Capture Polygon")
        
        // Add instruction buttons
        map.addTextButton("Add Point", 1)
        map.addTextButton("Undo", 2)
        map.addTextButton("Done", 3)
        map.addTextButton("Cancel", 4)
        
        if (map.show() != 1) {
            return ""
        }
        
        // Wait for events
        var done = false
        while (!done) {
            val event = map.waitForEvent() ?: break
            
            when (event.eventCode) {
                MapEventType.MAP_CLICKED -> {
                    // Add point to polygon
                    capturedPoints.add(Pair(event.latitude, event.longitude))
                    updatePolygonDisplay(map)
                }
                MapEventType.BUTTON_CLICKED -> {
                    when (event.callbackId) {
                        1 -> {
                            // Add Point - already handled by map click
                        }
                        2 -> {
                            // Undo
                            if (capturedPoints.isNotEmpty()) {
                                capturedPoints.removeAt(capturedPoints.lastIndex)
                                updatePolygonDisplay(map)
                            }
                        }
                        3 -> {
                            // Done
                            done = true
                        }
                        4 -> {
                            // Cancel
                            capturedPoints.clear()
                            done = true
                        }
                    }
                }
                MapEventType.MAP_CLOSED -> {
                    capturedPoints.clear()
                    done = true
                }
            }
        }
        
        map.hide()
        
        return if (capturedPoints.size >= 3) {
            buildPolygonGeoJson()
        } else {
            ""
        }
    }
    
    private fun updatePolygonDisplay(map: MapUI) {
        map.clearMarkers()
        map.clearGeometry()
        
        // Add markers for each point
        for ((index, point) in capturedPoints.withIndex()) {
            val markerId = map.addMarker(point.first, point.second)
            map.setMarkerText(markerId, "${index + 1}", 0x3498db, 0xFFFFFF)
        }
        
        // Add polygon geometry if we have enough points
        if (capturedPoints.size >= 3) {
            val polygonFeature = GeoJsonFeature(
                type = "Feature",
                geometry = GeoJsonGeometry(
                    type = "Polygon",
                    coordinates = listOf(capturedPoints.map { listOf(it.second, it.first) })
                )
            )
            map.addGeometry(FeatureCollection(features = listOf(polygonFeature)))
        }
    }
    
    private fun buildPolygonGeoJson(): String {
        // Close the polygon by adding first point at end
        val coordinates = capturedPoints.map { listOf(it.second, it.first) } +
                listOf(listOf(capturedPoints.first().second, capturedPoints.first().first))
        
        return """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [${coordinates.map { "[${it[0]}, ${it[1]}]" }.joinToString(",")}]
                },
                "properties": {}
            }
        """.trimIndent()
    }
}
