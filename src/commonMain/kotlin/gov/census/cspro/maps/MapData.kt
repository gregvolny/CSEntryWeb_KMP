package gov.census.cspro.maps

/**
 * Map Data structures
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/maps/MapData.java
 */

/**
 * Base map types
 */
object BaseMapType {
    const val CUSTOM = 0
    const val NORMAL = 1
    const val HYBRID = 2
    const val SATELLITE = 3
    const val TERRAIN = 4
    const val NONE = 5
}

/**
 * Base map selection
 */
data class BaseMapSelection(
    val type: Int = BaseMapType.NORMAL,
    val filename: String? = null
)

/**
 * Map marker
 */
data class MapMarker(
    val id: Int,
    var latitude: Double,
    var longitude: Double,
    var text: String? = null,
    var textColor: Int = 0,
    var backgroundColor: Int = 0xF44343,
    var imagePath: String? = null,
    var description: String? = null,
    var onClickCallback: Int = -1,
    var onClickInfoWindowCallback: Int = -1,
    var onDragCallback: Int = -1
) {
    fun setLocation(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
    }
}

/**
 * Map button
 */
data class MapButton(
    val id: Int,
    val imagePath: String?,
    val label: String?,
    val callbackId: Int
)

/**
 * Map camera position
 */
data class MapCameraPosition(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float,
    val bearing: Float = 0f
)

/**
 * Map event types
 */
object MapEventType {
    const val MAP_CLOSED = 1
    const val MARKER_CLICKED = 2
    const val MARKER_INFO_WINDOW_CLICKED = 3
    const val MARKER_DRAGGED = 4
    const val MAP_CLICKED = 5
    const val BUTTON_CLICKED = 6
}

/**
 * Map event
 */
data class MapEvent(
    val eventCode: Int,
    val callbackId: Int = -1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val camera: MapCameraPosition? = null,
    val markerId: Int = -1
)

/**
 * GeoJSON feature for geometry
 */
data class GeoJsonFeature(
    val type: String,
    val geometry: GeoJsonGeometry?,
    val properties: Map<String, Any?> = emptyMap()
)

data class GeoJsonGeometry(
    val type: String, // Point, LineString, Polygon, etc.
    val coordinates: Any // Can be various nested arrays
)

data class FeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<GeoJsonFeature> = emptyList()
)

/**
 * Bounding box for geometry
 */
data class BoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double
)

/**
 * Map Data - holds all map state
 */
class MapData {
    private val markers = mutableMapOf<Int, MapMarker>()
    private var nextMarkerId = 1
    
    private val buttons = mutableMapOf<Int, MapButton>()
    private var nextButtonId = 1
    
    private val geometry = mutableMapOf<Int, FeatureCollection>()
    private var nextGeometryId = 1
    
    var showCurrentLocation: Boolean = false
    var title: String? = null
    var baseMapSelection: BaseMapSelection = BaseMapSelection()
    var zoomToPoint: MapCameraPosition? = null
    var zoomToBounds: BoundingBox? = null
    var zoomToBoundsPaddingPct: Double = 0.0
    var cameraPosition: MapCameraPosition? = null
    
    // Marker operations
    fun addMarker(latitude: Double, longitude: Double): Int {
        val id = nextMarkerId++
        markers[id] = MapMarker(id, latitude, longitude)
        return id
    }
    
    fun removeMarker(markerId: Int) {
        markers.remove(markerId)
    }
    
    fun clearMarkers() {
        markers.clear()
    }
    
    fun getMarker(markerId: Int): MapMarker? = markers[markerId]
    
    fun getMarkers(): List<MapMarker> = markers.values.toList()
    
    fun setMarkerImage(markerId: Int, imageFilePath: String) {
        markers[markerId]?.let {
            it.imagePath = imageFilePath
            it.text = null // Can't have both
        }
    }
    
    fun setMarkerText(markerId: Int, text: String, backgroundColor: Int, textColor: Int) {
        markers[markerId]?.let {
            it.text = text
            it.backgroundColor = backgroundColor
            it.textColor = textColor
            it.imagePath = null // Can't have both
        }
    }
    
    fun setMarkerOnClick(markerId: Int, callback: Int) {
        markers[markerId]?.onClickCallback = callback
    }
    
    fun setMarkerOnClickInfoWindow(markerId: Int, callback: Int) {
        markers[markerId]?.onClickInfoWindowCallback = callback
    }
    
    fun setMarkerOnDrag(markerId: Int, callback: Int) {
        markers[markerId]?.onDragCallback = callback
    }
    
    fun setMarkerDescription(markerId: Int, description: String) {
        markers[markerId]?.description = description
    }
    
    fun setMarkerLocation(markerId: Int, latitude: Double, longitude: Double) {
        markers[markerId]?.setLocation(latitude, longitude)
    }
    
    fun getMarkerLocation(markerId: Int): Pair<Double, Double>? {
        val marker = markers[markerId] ?: return null
        return Pair(marker.latitude, marker.longitude)
    }
    
    // Button operations
    fun addButton(imagePath: String?, label: String?, callbackId: Int): Int {
        val id = nextButtonId++
        buttons[id] = MapButton(id, imagePath, label, callbackId)
        return id
    }
    
    fun removeButton(buttonId: Int) {
        buttons.remove(buttonId)
    }
    
    fun clearButtons() {
        buttons.clear()
    }
    
    fun getButtons(): List<MapButton> = buttons.values.toList()
    
    // Geometry operations
    fun addGeometry(featureCollection: FeatureCollection): Int {
        val id = nextGeometryId++
        geometry[id] = featureCollection
        return id
    }
    
    fun removeGeometry(geometryId: Int) {
        geometry.remove(geometryId)
    }
    
    fun clearGeometry() {
        geometry.clear()
    }
    
    fun getGeometry(): List<Pair<Int, FeatureCollection>> = 
        geometry.map { Pair(it.key, it.value) }
    
    // Camera operations
    fun setBaseMap(selection: BaseMapSelection) {
        baseMapSelection = selection
    }
    
    fun zoomToPoint(latitude: Double, longitude: Double, zoom: Float) {
        zoomToPoint = MapCameraPosition(latitude, longitude, zoom)
    }
    
    fun zoomToBounds(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double, paddingPct: Double) {
        zoomToBounds = BoundingBox(minLat, minLon, maxLat, maxLon)
        zoomToBoundsPaddingPct = paddingPct
    }
    
    fun setCamera(position: MapCameraPosition) {
        cameraPosition = position
    }
    
    fun clear() {
        markers.clear()
        buttons.clear()
        geometry.clear()
        nextMarkerId = 1
        nextButtonId = 1
        nextGeometryId = 1
        showCurrentLocation = false
        title = null
        baseMapSelection = BaseMapSelection()
        zoomToPoint = null
        zoomToBounds = null
        cameraPosition = null
    }
}
