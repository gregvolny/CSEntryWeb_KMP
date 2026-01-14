package gov.census.cspro.maps

/**
 * Map UI interface
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/maps/MapUI.java
 */

/**
 * Map event listener interface
 */
interface IMapEventListener {
    fun onMapEvent(event: MapEvent): Boolean
}

/**
 * Map UI interface - platform specific implementations
 */
expect class MapUI() {
    /**
     * Map data containing markers, buttons, geometry, etc.
     */
    val mapData: MapData
    
    /**
     * Show the map
     * @return 1 on success, 0 on failure
     */
    suspend fun show(): Int
    
    /**
     * Hide the map
     * @return 1 on success, 0 on failure
     */
    suspend fun hide(): Int
    
    /**
     * Save a snapshot of the map to an image file
     * @param imagePath Path to save the image
     * @return The path of the saved image, or empty on failure
     */
    suspend fun saveSnapshot(imagePath: String): String
    
    /**
     * Wait for a map event (blocks until event occurs)
     * @return The map event that occurred
     */
    suspend fun waitForEvent(): MapEvent?
    
    // Marker operations
    fun addMarker(latitude: Double, longitude: Double): Int
    fun removeMarker(markerId: Int): Int
    fun clearMarkers()
    fun setMarkerImage(markerId: Int, imageFilePath: String): Int
    fun setMarkerText(markerId: Int, text: String, backgroundColor: Int, textColor: Int): Int
    fun setMarkerOnClick(markerId: Int, onClickCallback: Int): Int
    fun setMarkerOnClickInfoWindow(markerId: Int, onClickCallback: Int): Int
    fun setMarkerOnDrag(markerId: Int, onDragCallback: Int): Int
    fun setMarkerDescription(markerId: Int, description: String): Int
    fun setMarkerLocation(markerId: Int, latitude: Double, longitude: Double): Int
    fun getMarkerLocation(markerId: Int): Pair<Double, Double>?
    
    // Button operations
    fun addImageButton(imagePath: String, callbackId: Int): Int
    fun addTextButton(label: String, callbackId: Int): Int
    fun removeButton(buttonId: Int): Int
    fun clearButtons()
    
    // Geometry operations
    fun addGeometry(geometry: FeatureCollection): Int
    fun removeGeometry(geometryId: Int): Int
    fun clearGeometry()
    
    // Map settings
    fun setBaseMap(baseMapSelection: BaseMapSelection): Int
    fun setShowCurrentLocation(show: Boolean): Int
    fun setTitle(title: String): Int
    fun zoomToPoint(latitude: Double, longitude: Double, zoom: Float): Int
    fun zoomToBounds(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double, paddingPct: Double): Int
    fun setCamera(cameraPosition: MapCameraPosition): Int
    
    // Clear all map data
    fun clear()
}
