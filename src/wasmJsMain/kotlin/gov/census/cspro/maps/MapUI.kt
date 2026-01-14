package gov.census.cspro.maps

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsString
import kotlin.js.Promise
import org.w3c.dom.*

/**
 * External JS function declarations for Leaflet.js
 */

// Check if Leaflet is available
@JsFun("() => typeof L !== 'undefined'")
private external fun isLeafletAvailableJs(): JsBoolean

// Leaflet map operations
@JsFun("(element, options) => L.map(element, options)")
private external fun leafletMapCreate(element: JsAny, options: JsAny): JsAny

@JsFun("(map, lat, lng, zoom) => { map.setView([lat, lng], zoom); return map; }")
private external fun leafletMapSetView(map: JsAny, lat: Double, lng: Double, zoom: Int): JsAny

@JsFun("(map) => map.remove()")
private external fun leafletMapRemove(map: JsAny)

@JsFun("(map) => map.invalidateSize()")
private external fun leafletMapInvalidateSize(map: JsAny)

@JsFun("(map) => { var c = map.getCenter(); return { lat: c.lat, lng: c.lng }; }")
private external fun leafletMapGetCenter(map: JsAny): JsAny

@JsFun("(obj) => obj.lat")
private external fun getLatFromObj(obj: JsAny): Double

@JsFun("(obj) => obj.lng")
private external fun getLngFromObj(obj: JsAny): Double

@JsFun("(map) => map.getZoom()")
private external fun leafletMapGetZoom(map: JsAny): Int

@JsFun("(map, layer) => { map.addLayer(layer); return map; }")
private external fun leafletMapAddLayer(map: JsAny, layer: JsAny): JsAny

@JsFun("(map, layer) => { map.removeLayer(layer); return map; }")
private external fun leafletMapRemoveLayer(map: JsAny, layer: JsAny): JsAny

@JsFun("(map, minLat, minLng, maxLat, maxLng, padding) => { map.fitBounds([[minLat, minLng], [maxLat, maxLng]], { padding: [padding, padding] }); return map; }")
private external fun leafletMapFitBounds(map: JsAny, minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, padding: Int): JsAny

// Leaflet tile layer
@JsFun("(url, attribution) => L.tileLayer(url, { attribution: attribution })")
private external fun leafletTileLayer(url: JsString, attribution: JsString): JsAny

@JsFun("(layer, map) => { layer.addTo(map); return layer; }")
private external fun leafletLayerAddTo(layer: JsAny, map: JsAny): JsAny

// Leaflet marker operations
@JsFun("(lat, lng) => L.marker([lat, lng])")
private external fun leafletMarkerCreate(lat: Double, lng: Double): JsAny

@JsFun("(marker, map) => { marker.addTo(map); return marker; }")
private external fun leafletMarkerAddTo(marker: JsAny, map: JsAny): JsAny

@JsFun("(marker) => { marker.remove(); return marker; }")
private external fun leafletMarkerRemove(marker: JsAny): JsAny

@JsFun("(marker, lat, lng) => { marker.setLatLng([lat, lng]); return marker; }")
private external fun leafletMarkerSetLatLng(marker: JsAny, lat: Double, lng: Double): JsAny

@JsFun("(marker, content) => { marker.bindPopup(content); return marker; }")
private external fun leafletMarkerBindPopup(marker: JsAny, content: JsString): JsAny

@JsFun("(marker) => { var ll = marker.getLatLng(); return { lat: ll.lat, lng: ll.lng }; }")
private external fun leafletMarkerGetLatLng(marker: JsAny): JsAny

// Leaflet icon
@JsFun("(iconUrl, iconSize, iconAnchor) => L.icon({ iconUrl: iconUrl, iconSize: iconSize, iconAnchor: iconAnchor })")
private external fun leafletIconCreate(iconUrl: JsString, iconSize: JsAny, iconAnchor: JsAny): JsAny

@JsFun("(html, iconSize, iconAnchor) => L.divIcon({ className: 'custom-div-icon', html: html, iconSize: iconSize, iconAnchor: iconAnchor })")
private external fun leafletDivIconCreate(html: JsString, iconSize: JsAny, iconAnchor: JsAny): JsAny

@JsFun("(marker, icon) => { marker.setIcon(icon); return marker; }")
private external fun leafletMarkerSetIcon(marker: JsAny, icon: JsAny): JsAny

// Array creation for icon sizes
@JsFun("(a, b) => [a, b]")
private external fun createJsArray2(a: Int, b: Int): JsAny

// Leaflet GeoJSON
@JsFun("(jsonString) => L.geoJSON(JSON.parse(jsonString))")
private external fun leafletGeoJSON(jsonString: JsString): JsAny

// Event handlers
@JsFun("(map, event, callback) => { map.on(event, function(e) { callback(e.latlng.lat, e.latlng.lng); }); }")
private external fun leafletMapOnClick(map: JsAny, event: JsString, callback: (Double, Double) -> Unit)

@JsFun("(marker, event, callback) => { marker.on(event, function(e) { callback(); }); }")
private external fun leafletMarkerOnClick(marker: JsAny, event: JsString, callback: () -> Unit)

// Create empty options object
@JsFun("() => ({ zoomControl: true })")
private external fun createMapOptions(): JsAny

// String to JsString
@JsFun("(s) => s")
private external fun toJsString(s: String): JsString

// Element style operations
@JsFun("(el, css) => { el.style.cssText = css; }")
private external fun setElementStyle(el: JsAny, css: JsString)

@JsFun("(el, handler) => { el.onclick = handler; }")
private external fun setElementOnClick(el: JsAny, handler: () -> Unit)

/**
 * WASM/Web implementation of MapUI
 * Uses Leaflet.js for mapping (can be extended to use Google Maps or Mapbox)
 * 
 * Mirrors: CSEntryDroid/app/src/main/java/gov/census/cspro/maps/MapUI.java
 */
actual class MapUI {
    actual val mapData: MapData = MapData()
    
    private var mapContainer: Element? = null
    private var leafletMap: JsAny? = null
    private var tileLayer: JsAny? = null
    private var markerLayers = mutableMapOf<Int, JsAny>()
    private var geometryLayers = mutableMapOf<Int, JsAny>()
    private var isShowing = false
    private var eventDeferred: CompletableDeferred<MapEvent>? = null
    private var eventListener: IMapEventListener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    actual suspend fun show(): Int {
        if (isShowing) {
            return 1
        }
        
        return try {
            createMapContainer()
            initializeMap()
            renderMapData()
            isShowing = true
            1
        } catch (e: Exception) {
            println("[MapUI] Error showing map: ${e.message}")
            0
        }
    }
    
    actual suspend fun hide(): Int {
        return try {
            // Clean up map
            leafletMap?.let { leafletMapRemove(it) }
            leafletMap = null
            markerLayers.clear()
            geometryLayers.clear()
            
            // Remove container
            mapContainer?.remove()
            mapContainer = null
            
            isShowing = false
            
            // Notify of map closed event
            emitEvent(MapEvent(MapEventType.MAP_CLOSED, camera = getCurrentCamera()))
            
            1
        } catch (e: Exception) {
            println("[MapUI] Error hiding map: ${e.message}")
            0
        }
    }
    
    actual suspend fun saveSnapshot(imagePath: String): String {
        // Use MapSnapshot with html2canvas for capturing the map
        return try {
            val mapElement = document.getElementById("cspro-leaflet-map")
            if (mapElement == null) {
                println("[MapUI] Map element not found for snapshot")
                return ""
            }
            
            // Determine format based on file extension
            val format = when {
                imagePath.endsWith(".jpg", ignoreCase = true) || 
                imagePath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                imagePath.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/png"
            }
            
            // Capture the map using MapSnapshot
            val snapshot = MapSnapshot.captureLeafletMap(
                mapId = "cspro-leaflet-map",
                options = SnapshotOptions(
                    format = format,
                    quality = 0.92,
                    scale = 1.0,
                    useCORS = true,
                    ignoreElements = listOf("leaflet-control-zoom", "leaflet-control-attribution")
                )
            )
            
            if (snapshot == null) {
                println("[MapUI] Failed to capture map snapshot")
                return ""
            }
            
            // Save to OPFS
            val saved = MapSnapshot.saveToOPFS(snapshot, imagePath)
            
            if (saved) {
                println("[MapUI] Snapshot saved: $imagePath (${snapshot.getApproximateSize()} bytes)")
                imagePath
            } else {
                println("[MapUI] Failed to save snapshot to OPFS")
                ""
            }
        } catch (e: Exception) {
            println("[MapUI] Error saving snapshot: ${e.message}")
            ""
        }
    }
    
    actual suspend fun waitForEvent(): MapEvent? {
        eventDeferred = CompletableDeferred()
        return try {
            eventDeferred?.await()
        } catch (e: Exception) {
            println("[MapUI] Error waiting for event: ${e.message}")
            null
        }
    }
    
    private fun emitEvent(event: MapEvent) {
        eventListener?.onMapEvent(event)
        eventDeferred?.complete(event)
        eventDeferred = null
    }
    
    private fun createMapContainer() {
        // Create fullscreen overlay for map
        val container = document.createElement("div")
        container.id = "cspro-map-container"
        setElementStyle(container.unsafeCast<JsAny>(), toJsString("""
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 9999;
            background: white;
            display: flex;
            flex-direction: column;
        """.trimIndent()))
        
        // Add title bar if title is set
        if (!mapData.title.isNullOrBlank()) {
            val titleBar = document.createElement("div")
            setElementStyle(titleBar.unsafeCast<JsAny>(), toJsString("""
                padding: 15px;
                background: #3498db;
                color: white;
                font-size: 18px;
                font-weight: bold;
                display: flex;
                justify-content: space-between;
                align-items: center;
            """.trimIndent()))
            titleBar.textContent = mapData.title
            
            // Add close button
            val closeBtn = document.createElement("button")
            closeBtn.textContent = "✕"
            setElementStyle(closeBtn.unsafeCast<JsAny>(), toJsString("""
                background: none;
                border: none;
                color: white;
                font-size: 24px;
                cursor: pointer;
                padding: 0 10px;
            """.trimIndent()))
            setElementOnClick(closeBtn.unsafeCast<JsAny>()) {
                coroutineScope.launch {
                    hide()
                }
            }
            titleBar.appendChild(closeBtn)
            
            container.appendChild(titleBar)
        }
        
        // Add map div
        val mapDiv = document.createElement("div")
        mapDiv.id = "cspro-leaflet-map"
        setElementStyle(mapDiv.unsafeCast<JsAny>(), toJsString("""
            flex: 1;
            width: 100%;
        """.trimIndent()))
        container.appendChild(mapDiv)
        
        // Add buttons container
        if (mapData.getButtons().isNotEmpty()) {
            val buttonsDiv = document.createElement("div")
            setElementStyle(buttonsDiv.unsafeCast<JsAny>(), toJsString("""
                padding: 10px;
                background: #f5f5f5;
                display: flex;
                gap: 10px;
                justify-content: center;
            """.trimIndent()))
            
            for (button in mapData.getButtons()) {
                val btn = document.createElement("button")
                setElementStyle(btn.unsafeCast<JsAny>(), toJsString("""
                    padding: 10px 20px;
                    border: none;
                    border-radius: 5px;
                    background: #3498db;
                    color: white;
                    cursor: pointer;
                    font-size: 14px;
                """.trimIndent()))
                
                if (button.imagePath != null) {
                    val img = document.createElement("img") as HTMLImageElement
                    img.src = button.imagePath
                    setElementStyle(img.unsafeCast<JsAny>(), toJsString("width: 20px; height: 20px; margin-right: 5px;"))
                    btn.appendChild(img)
                }
                
                if (button.label != null) {
                    btn.appendChild(document.createTextNode(button.label))
                }
                
                val callbackId = button.callbackId
                setElementOnClick(btn.unsafeCast<JsAny>()) {
                    emitEvent(MapEvent(
                        eventCode = MapEventType.BUTTON_CLICKED,
                        callbackId = callbackId,
                        camera = getCurrentCamera()
                    ))
                }
                
                buttonsDiv.appendChild(btn)
            }
            
            container.appendChild(buttonsDiv)
        }
        
        document.body?.appendChild(container)
        mapContainer = container
    }
    
    private fun initializeMap() {
        val mapElement = document.getElementById("cspro-leaflet-map")
        
        // Check if Leaflet is available
        if (!isLeafletAvailable()) {
            println("[MapUI] Leaflet not available, loading...")
            // Would need to dynamically load Leaflet here
            throw Exception("Leaflet library not loaded")
        }
        
        // Initialize Leaflet map
        val options = createMapOptions()
        leafletMap = leafletMapCreate(mapElement!!.unsafeCast<JsAny>(), options)
        
        // Set initial view
        val camera = mapData.cameraPosition ?: mapData.zoomToPoint
        if (camera != null) {
            leafletMapSetView(leafletMap!!, camera.latitude, camera.longitude, camera.zoom.toInt())
        } else {
            // Default view (world)
            leafletMapSetView(leafletMap!!, 0.0, 0.0, 2)
        }
        
        // Add base map layer
        addBaseMapLayer()
        
        // Set up event handlers
        setupEventHandlers()
        
        // Trigger resize to ensure proper rendering
        window.setTimeout({
            leafletMap?.let { leafletMapInvalidateSize(it) }
            null
        }, 100)
    }
    
    private fun isLeafletAvailable(): Boolean {
        return try {
            isLeafletAvailableJs().toBoolean()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun addBaseMapLayer() {
        val tileUrl = when (mapData.baseMapSelection.type) {
            BaseMapType.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            BaseMapType.TERRAIN -> "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png"
            BaseMapType.HYBRID -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            BaseMapType.NONE -> null
            else -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        }
        
        if (tileUrl != null) {
            tileLayer = leafletTileLayer(toJsString(tileUrl), toJsString("© OpenStreetMap contributors"))
            leafletMap?.let { map ->
                tileLayer?.let { layer ->
                    leafletLayerAddTo(layer, map)
                }
            }
        }
    }
    
    private fun setupEventHandlers() {
        leafletMap?.let { map ->
            leafletMapOnClick(map, toJsString("click")) { lat, lng ->
                emitEvent(MapEvent(
                    eventCode = MapEventType.MAP_CLICKED,
                    latitude = lat,
                    longitude = lng,
                    camera = getCurrentCamera()
                ))
            }
        }
    }
    
    private fun renderMapData() {
        // Render markers
        for (marker in mapData.getMarkers()) {
            addMarkerToMap(marker)
        }
        
        // Render geometry
        for ((id, featureCollection) in mapData.getGeometry()) {
            addGeometryToMap(id, featureCollection)
        }
        
        // Handle zoom to bounds
        mapData.zoomToBounds?.let { bounds ->
            val padding = (mapData.zoomToBoundsPaddingPct * 100).toInt()
            leafletMap?.let { map ->
                leafletMapFitBounds(map, bounds.minLat, bounds.minLon, bounds.maxLat, bounds.maxLon, padding)
            }
        }
    }
    
    private fun addMarkerToMap(marker: MapMarker) {
        val map = leafletMap ?: return
        
        val leafletMarker = leafletMarkerCreate(marker.latitude, marker.longitude)
        
        // Set icon if available
        if (marker.text != null) {
            val bgColor = "#${marker.backgroundColor.toString(16).padStart(6, '0')}"
            val txtColor = "#${marker.textColor.toString(16).padStart(6, '0')}"
            val html = "<div style=\"background: $bgColor; color: $txtColor; padding: 5px 10px; border-radius: 4px; font-weight: bold;\">${marker.text}</div>"
            val iconSize = createJsArray2(40, 40)
            val iconAnchor = createJsArray2(20, 40)
            val icon = leafletDivIconCreate(toJsString(html), iconSize, iconAnchor)
            leafletMarkerSetIcon(leafletMarker, icon)
        } else {
            val imagePath = marker.imagePath
            if (imagePath != null) {
                val iconSize = createJsArray2(32, 32)
                val iconAnchor = createJsArray2(16, 32)
                val icon = leafletIconCreate(toJsString(imagePath), iconSize, iconAnchor)
                leafletMarkerSetIcon(leafletMarker, icon)
            }
        }
        
        leafletMarkerAddTo(leafletMarker, map)
        
        // Add popup if description exists
        marker.description?.let { desc ->
            leafletMarkerBindPopup(leafletMarker, toJsString(desc))
        }
        
        // Add click handler
        if (marker.onClickCallback >= 0) {
            val callbackId = marker.onClickCallback
            val markerId = marker.id
            val lat = marker.latitude
            val lng = marker.longitude
            leafletMarkerOnClick(leafletMarker, toJsString("click")) {
                emitEvent(MapEvent(
                    eventCode = MapEventType.MARKER_CLICKED,
                    callbackId = callbackId,
                    markerId = markerId,
                    latitude = lat,
                    longitude = lng,
                    camera = getCurrentCamera()
                ))
            }
        }
        
        markerLayers[marker.id] = leafletMarker
    }
    
    private fun addGeometryToMap(id: Int, featureCollection: FeatureCollection) {
        val map = leafletMap ?: return
        
        try {
            // Convert to GeoJSON string manually
            val geoJsonString = serializeFeatureCollection(featureCollection)
            val layer = leafletGeoJSON(toJsString(geoJsonString))
            leafletMapAddLayer(map, layer)
            geometryLayers[id] = layer
        } catch (e: Exception) {
            println("[MapUI] Error adding geometry: ${e.message}")
        }
    }
    
    private fun serializeFeatureCollection(fc: FeatureCollection): String {
        // Simple GeoJSON serialization
        val featuresJson = fc.features.mapNotNull { feature ->
            val geometry = feature.geometry ?: return@mapNotNull null
            val geometryJson = serializeGeometry(geometry)
            val propertiesJson = serializeProperties(feature.properties)
            """{"type":"Feature","geometry":$geometryJson,"properties":$propertiesJson}"""
        }.joinToString(",")
        return """{"type":"FeatureCollection","features":[$featuresJson]}"""
    }
    
    private fun serializeGeometry(geometry: GeoJsonGeometry): String {
        val coordsJson = serializeCoordinates(geometry.coordinates)
        return """{"type":"${geometry.type}","coordinates":$coordsJson}"""
    }
    
    private fun serializeCoordinates(coords: Any): String {
        return when (coords) {
            is List<*> -> {
                val items = coords.map { item ->
                    when (item) {
                        is List<*> -> serializeCoordinates(item)
                        is Number -> item.toString()
                        else -> item.toString()
                    }
                }
                "[" + items.joinToString(",") + "]"
            }
            is Number -> coords.toString()
            else -> coords.toString()
        }
    }
    
    private fun serializeProperties(props: Map<String, Any?>): String {
        if (props.isEmpty()) return "{}"
        val pairs = props.entries.mapNotNull { (key, value) ->
            val jsonValue = when (value) {
                null -> "null"
                is String -> "\"${value.replace("\"", "\\\"")}\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"${value.toString()}\""
            }
            "\"$key\":$jsonValue"
        }
        return "{" + pairs.joinToString(",") + "}"
    }
    
    private fun getCurrentCamera(): MapCameraPosition {
        val map = leafletMap
        return if (map != null) {
            try {
                val center = leafletMapGetCenter(map)
                MapCameraPosition(
                    latitude = getLatFromObj(center),
                    longitude = getLngFromObj(center),
                    zoom = leafletMapGetZoom(map).toFloat(),
                    bearing = 0f
                )
            } catch (e: Exception) {
                MapCameraPosition(0.0, 0.0, 1f, 0f)
            }
        } else {
            MapCameraPosition(0.0, 0.0, 1f, 0f)
        }
    }
    
    // Marker operations
    actual fun addMarker(latitude: Double, longitude: Double): Int {
        val id = mapData.addMarker(latitude, longitude)
        if (isShowing) {
            mapData.getMarker(id)?.let { addMarkerToMap(it) }
        }
        return id
    }
    
    actual fun removeMarker(markerId: Int): Int {
        return try {
            markerLayers[markerId]?.let { leafletMarkerRemove(it) }
            markerLayers.remove(markerId)
            mapData.removeMarker(markerId)
            1
        } catch (e: Exception) {
            0
        }
    }
    
    actual fun clearMarkers() {
        for ((_, marker) in markerLayers) {
            leafletMarkerRemove(marker)
        }
        markerLayers.clear()
        mapData.clearMarkers()
    }
    
    actual fun setMarkerImage(markerId: Int, imageFilePath: String): Int {
        mapData.setMarkerImage(markerId, imageFilePath)
        // Update existing marker on map
        markerLayers[markerId]?.let { marker ->
            val iconSize = createJsArray2(32, 32)
            val iconAnchor = createJsArray2(16, 32)
            val icon = leafletIconCreate(toJsString(imageFilePath), iconSize, iconAnchor)
            leafletMarkerSetIcon(marker, icon)
        }
        return 1
    }
    
    actual fun setMarkerText(markerId: Int, text: String, backgroundColor: Int, textColor: Int): Int {
        mapData.setMarkerText(markerId, text, backgroundColor, textColor)
        return 1
    }
    
    actual fun setMarkerOnClick(markerId: Int, onClickCallback: Int): Int {
        mapData.setMarkerOnClick(markerId, onClickCallback)
        return 1
    }
    
    actual fun setMarkerOnClickInfoWindow(markerId: Int, onClickCallback: Int): Int {
        mapData.setMarkerOnClickInfoWindow(markerId, onClickCallback)
        return 1
    }
    
    actual fun setMarkerOnDrag(markerId: Int, onDragCallback: Int): Int {
        mapData.setMarkerOnDrag(markerId, onDragCallback)
        return 1
    }
    
    actual fun setMarkerDescription(markerId: Int, description: String): Int {
        mapData.setMarkerDescription(markerId, description)
        markerLayers[markerId]?.let { marker ->
            leafletMarkerBindPopup(marker, toJsString(description))
        }
        return 1
    }
    
    actual fun setMarkerLocation(markerId: Int, latitude: Double, longitude: Double): Int {
        mapData.setMarkerLocation(markerId, latitude, longitude)
        markerLayers[markerId]?.let { marker ->
            leafletMarkerSetLatLng(marker, latitude, longitude)
        }
        return 1
    }
    
    actual fun getMarkerLocation(markerId: Int): Pair<Double, Double>? {
        return mapData.getMarkerLocation(markerId)
    }
    
    // Button operations
    actual fun addImageButton(imagePath: String, callbackId: Int): Int {
        return mapData.addButton(imagePath, null, callbackId)
    }
    
    actual fun addTextButton(label: String, callbackId: Int): Int {
        return mapData.addButton(null, label, callbackId)
    }
    
    actual fun removeButton(buttonId: Int): Int {
        return try {
            mapData.removeButton(buttonId)
            1
        } catch (e: Exception) {
            0
        }
    }
    
    actual fun clearButtons() {
        mapData.clearButtons()
    }
    
    // Geometry operations
    actual fun addGeometry(geometry: FeatureCollection): Int {
        val id = mapData.addGeometry(geometry)
        if (isShowing) {
            addGeometryToMap(id, geometry)
        }
        return id
    }
    
    actual fun removeGeometry(geometryId: Int): Int {
        return try {
            geometryLayers[geometryId]?.let { layer ->
                leafletMap?.let { map -> leafletMapRemoveLayer(map, layer) }
            }
            geometryLayers.remove(geometryId)
            mapData.removeGeometry(geometryId)
            1
        } catch (e: Exception) {
            0
        }
    }
    
    actual fun clearGeometry() {
        for ((_, layer) in geometryLayers) {
            leafletMap?.let { map -> leafletMapRemoveLayer(map, layer) }
        }
        geometryLayers.clear()
        mapData.clearGeometry()
    }
    
    // Map settings
    actual fun setBaseMap(baseMapSelection: BaseMapSelection): Int {
        mapData.setBaseMap(baseMapSelection)
        if (isShowing) {
            tileLayer?.let { layer ->
                leafletMap?.let { map -> leafletMapRemoveLayer(map, layer) }
            }
            addBaseMapLayer()
        }
        return 1
    }
    
    actual fun setShowCurrentLocation(show: Boolean): Int {
        mapData.showCurrentLocation = show
        // Would need to implement current location marker
        return 1
    }
    
    actual fun setTitle(title: String): Int {
        mapData.title = title
        return 1
    }
    
    actual fun zoomToPoint(latitude: Double, longitude: Double, zoom: Float): Int {
        mapData.zoomToPoint(latitude, longitude, zoom)
        leafletMap?.let { map ->
            leafletMapSetView(map, latitude, longitude, zoom.toInt())
        }
        return 1
    }
    
    actual fun zoomToBounds(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double, paddingPct: Double): Int {
        mapData.zoomToBounds(minLat, minLon, maxLat, maxLon, paddingPct)
        val padding = (paddingPct * 100).toInt()
        leafletMap?.let { map ->
            leafletMapFitBounds(map, minLat, minLon, maxLat, maxLon, padding)
        }
        return 1
    }
    
    actual fun setCamera(cameraPosition: MapCameraPosition): Int {
        mapData.setCamera(cameraPosition)
        leafletMap?.let { map ->
            leafletMapSetView(map, cameraPosition.latitude, cameraPosition.longitude, cameraPosition.zoom.toInt())
        }
        return 1
    }
    
    actual fun clear() {
        clearMarkers()
        clearButtons()
        clearGeometry()
        mapData.clear()
    }
}
