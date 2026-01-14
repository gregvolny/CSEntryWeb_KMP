package gov.census.cspro.maps

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

/**
 * External JS function declarations for Map Snapshot functionality
 */

// Check if html2canvas is available
@JsFun("() => typeof html2canvas !== 'undefined'")
private external fun isHtml2CanvasAvailableJs(): JsBoolean

// Check if Leaflet image is available
@JsFun("() => typeof L !== 'undefined' && typeof L.mapToImage !== 'undefined'")
private external fun isLeafletImageAvailableJs(): JsBoolean

// Blob and URL operations
@JsFun("(content, type) => new Blob([content], {type: type})")
private external fun createBlob(content: JsString, type: JsString): JsAny

@JsFun("(blob) => URL.createObjectURL(blob)")
private external fun createObjectURL(blob: JsAny): JsString

@JsFun("(url) => URL.revokeObjectURL(url)")
private external fun revokeObjectURL(url: JsString)

// Base64 decode
@JsFun("(base64) => atob(base64)")
private external fun atobJs(base64: JsString): JsString

// Navigator storage for OPFS
@JsFun("() => navigator.storage.getDirectory()")
private external fun getStorageDirectory(): Promise<JsAny>

// File handle operations
@JsFun("(dir, name) => dir.getDirectoryHandle(name, { create: true })")
private external fun getDirectoryHandle(dir: JsAny, name: JsString): Promise<JsAny>

@JsFun("(dir, name) => dir.getFileHandle(name, { create: true })")
private external fun getFileHandle(dir: JsAny, name: JsString): Promise<JsAny>

@JsFun("(handle) => handle.createWritable()")
private external fun createWritable(handle: JsAny): Promise<JsAny>

@JsFun("(writable, data) => writable.write(data)")
private external fun writeToWritable(writable: JsAny, data: JsAny): Promise<JsAny>

@JsFun("(writable) => writable.close()")
private external fun closeWritable(writable: JsAny): Promise<JsAny>

// Uint8Array operations
@JsFun("(length) => new Uint8Array(length)")
private external fun createUint8Array(length: Int): JsAny

@JsFun("(arr, index, value) => { arr[index] = value; }")
private external fun setUint8ArrayValue(arr: JsAny, index: Int, value: Int)

// html2canvas call
@JsFun("(element, options) => html2canvas(element, options)")
private external fun html2canvasCall(element: JsAny, options: JsAny): Promise<JsAny>

// Canvas operations
@JsFun("(canvas, type, quality) => canvas.toDataURL(type, quality)")
private external fun canvasToDataURL(canvas: JsAny, type: JsString, quality: Double): JsString

@JsFun("(canvas) => canvas.width")
private external fun getCanvasWidth(canvas: JsAny): Int

@JsFun("(canvas) => canvas.height")
private external fun getCanvasHeight(canvas: JsAny): Int

// Html2canvas options builder
@JsFun("""(useCORS, allowTaint, backgroundColor, scale, logging, width, height, x, y, foreignObjectRendering) => ({
    useCORS: useCORS,
    allowTaint: allowTaint,
    backgroundColor: backgroundColor,
    scale: scale,
    logging: logging,
    width: width || undefined,
    height: height || undefined,
    x: x || undefined,
    y: y || undefined,
    foreignObjectRendering: foreignObjectRendering
})""")
private external fun createHtml2CanvasOptions(
    useCORS: Boolean,
    allowTaint: Boolean,
    backgroundColor: JsString?,
    scale: Double,
    logging: Boolean,
    width: Int?,
    height: Int?,
    x: Int?,
    y: Int?,
    foreignObjectRendering: Boolean
): JsAny

// String to JsString helper
@JsFun("(s) => s")
private external fun toJsString(s: String): JsString

// Canvas context style helpers (to avoid JsAny? issues with fillStyle/strokeStyle)
@JsFun("(ctx, style) => { ctx.fillStyle = style; }")
private external fun setFillStyle(ctx: JsAny, style: JsString)

@JsFun("(ctx, style) => { ctx.strokeStyle = style; }")
private external fun setStrokeStyle(ctx: JsAny, style: JsString)

// Canvas drawing helpers
@JsFun("(ctx, img, x, y) => ctx.drawImage(img, x, y)")
private external fun canvasDrawImage(ctx: JsAny, img: JsAny, x: Double, y: Double)

// Canvas toDataURL helper for HTMLCanvasElement
@JsFun("(canvas, type, quality) => canvas.toDataURL(type, quality)")
private external fun canvasElementToDataURL(canvas: JsAny, type: String, quality: Double): String

/**
 * Map Snapshot Service - Capture maps as images
 * 
 * Implements multiple capture strategies:
 * 1. html2canvas for DOM-based capture
 * 2. leaflet-image for Leaflet-specific capture
 * 3. Canvas compositing for custom rendering
 * 
 * Mirrors: Android map snapshot functionality
 */
object MapSnapshot {
    
    // Check if html2canvas is available
    private fun isHtml2CanvasAvailable(): Boolean {
        return try {
            isHtml2CanvasAvailableJs().toBoolean()
        } catch (e: Exception) {
            false
        }
    }
    
    // Check if leaflet-image is available
    private fun isLeafletImageAvailable(): Boolean {
        return try {
            isLeafletImageAvailableJs().toBoolean()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Load html2canvas library dynamically if not present
     */
    suspend fun loadHtml2Canvas(): Boolean {
        if (isHtml2CanvasAvailable()) return true
        
        return try {
            val script = document.createElement("script") as HTMLScriptElement
            script.src = "https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js"
            script.async = true
            
            val deferred = CompletableDeferred<Boolean>()
            script.onload = { deferred.complete(true) }
            script.onerror = { _, _, _, _, _ -> deferred.complete(false); null }
            
            document.head?.appendChild(script)
            deferred.await()
        } catch (e: Exception) {
            println("[MapSnapshot] Failed to load html2canvas: ${e.message}")
            false
        }
    }
    
    /**
     * Capture a DOM element as an image using html2canvas
     */
    suspend fun captureElement(
        element: Element,
        options: SnapshotOptions = SnapshotOptions()
    ): SnapshotResult? {
        // Ensure html2canvas is loaded
        if (!loadHtml2Canvas()) {
            return captureElementFallback(element, options)
        }
        
        return try {
            val html2canvasOptions = createHtml2CanvasOptions(
                useCORS = options.useCORS,
                allowTaint = options.allowTaint,
                backgroundColor = options.backgroundColor?.let { toJsString(it) },
                scale = options.scale,
                logging = false,
                width = options.width,
                height = options.height,
                x = options.x,
                y = options.y,
                foreignObjectRendering = true
            )
            
            val canvas = suspendCancellableCoroutine<JsAny> { cont ->
                val elementJs = element.unsafeCast<JsAny>()
                val promise = html2canvasCall(elementJs, html2canvasOptions)
                promise.then { result: JsAny? ->
                    if (result != null) {
                        cont.resume(result)
                    }
                    null
                }
            }
            
            val dataUrl = canvasToDataURL(canvas, toJsString(options.format), options.quality)
            val width = getCanvasWidth(canvas)
            val height = getCanvasHeight(canvas)
            
            SnapshotResult(
                dataUrl = dataUrl.toString(),
                width = width,
                height = height,
                format = options.format
            )
        } catch (e: Exception) {
            println("[MapSnapshot] html2canvas capture failed: ${e.message}")
            captureElementFallback(element, options)
        }
    }
    
    /**
     * Fallback capture using manual canvas drawing
     */
    private suspend fun captureElementFallback(
        element: Element,
        options: SnapshotOptions
    ): SnapshotResult? {
        return try {
            val rect = element.getBoundingClientRect()
            val width = options.width ?: rect.width.toInt()
            val height = options.height ?: rect.height.toInt()
            
            // Create canvas
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = (width * options.scale).toInt()
            canvas.height = (height * options.scale).toInt()
            
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            val ctxJs = ctx.unsafeCast<JsAny>()
            ctx.scale(options.scale, options.scale)
            
            // Fill background
            options.backgroundColor?.let {
                setFillStyle(ctxJs, toJsString(it))
                ctx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
            }
            
            // Try to capture using SVG foreignObject
            val outerHTML = element.outerHTML
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height">
                    <foreignObject width="100%" height="100%">
                        <div xmlns="http://www.w3.org/1999/xhtml">
                            $outerHTML
                        </div>
                    </foreignObject>
                </svg>
            """.trimIndent()
            
            val svgBlob = createBlob(toJsString(svg), toJsString("image/svg+xml;charset=utf-8"))
            val url = createObjectURL(svgBlob)
            
            val result = CompletableDeferred<SnapshotResult?>()
            
            val img = document.createElement("img") as HTMLImageElement
            img.onload = {
                canvasDrawImage(ctxJs, img.unsafeCast<JsAny>(), 0.0, 0.0)
                revokeObjectURL(url)
                
                val dataUrl = canvasElementToDataURL(canvas.unsafeCast<JsAny>(), options.format, options.quality)
                result.complete(SnapshotResult(
                    dataUrl = dataUrl,
                    width = canvas.width,
                    height = canvas.height,
                    format = options.format
                ))
                null
            }
            img.onerror = { _, _, _, _, _ ->
                revokeObjectURL(url)
                result.complete(null)
                null
            }
            img.src = url.toString()
            
            result.await()
        } catch (e: Exception) {
            println("[MapSnapshot] Fallback capture failed: ${e.message}")
            null
        }
    }
    
    /**
     * Capture a Leaflet map
     */
    suspend fun captureLeafletMap(
        mapId: String = "cspro-leaflet-map",
        options: SnapshotOptions = SnapshotOptions()
    ): SnapshotResult? {
        val mapElement = document.getElementById(mapId) ?: return null
        
        // Wait for tiles to load
        delay(500)
        
        // Use html2canvas with special handling for tiles
        val updatedOptions = options.copy(
            useCORS = true,
            allowTaint = false,
            ignoreElements = options.ignoreElements + listOf("leaflet-control-zoom", "leaflet-control-attribution")
        )
        
        return captureElement(mapElement, updatedOptions)
    }
    
    /**
     * Capture map with markers overlay
     */
    suspend fun captureMapWithOverlay(
        mapElement: Element,
        markers: List<MarkerInfo>,
        options: SnapshotOptions = SnapshotOptions()
    ): SnapshotResult? {
        // First capture the base map
        val baseCapture = captureElement(mapElement, options) ?: return null
        
        // If no markers, return base capture
        if (markers.isEmpty()) return baseCapture
        
        // Create composite canvas
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = baseCapture.width
        canvas.height = baseCapture.height
        
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        val ctxJs = ctx.unsafeCast<JsAny>()
        
        // Draw base map
        val baseImage = loadImage(baseCapture.dataUrl)
        canvasDrawImage(ctxJs, baseImage.unsafeCast<JsAny>(), 0.0, 0.0)
        
        // Draw markers
        for (marker in markers) {
            drawMarker(ctx, marker)
        }
        
        val dataUrl = canvasElementToDataURL(canvas.unsafeCast<JsAny>(), options.format, options.quality)
        
        return SnapshotResult(
            dataUrl = dataUrl,
            width = canvas.width,
            height = canvas.height,
            format = options.format
        )
    }
    
    /**
     * Load an image from URL/data URL
     */
    private suspend fun loadImage(src: String): HTMLImageElement {
        return suspendCancellableCoroutine { cont ->
            val img = document.createElement("img") as HTMLImageElement
            img.onload = { cont.resume(img) }
            img.onerror = { _, _, _, _, _ ->
                cont.resumeWithException(Exception("Failed to load image"))
                null
            }
            img.src = src
        }
    }
    
    /**
     * Draw a marker on canvas
     */
    private fun drawMarker(ctx: CanvasRenderingContext2D, marker: MarkerInfo) {
        val ctxJs = ctx.unsafeCast<JsAny>()
        ctx.save()
        
        // Draw marker pin
        setFillStyle(ctxJs, toJsString(marker.color))
        setStrokeStyle(ctxJs, toJsString("#ffffff"))
        ctx.lineWidth = 2.0
        
        // Pin shape
        ctx.beginPath()
        ctx.arc(marker.x, marker.y - 15, 12.0, 0.0, PI * 2)
        ctx.fill()
        ctx.stroke()
        
        // Pin point
        ctx.beginPath()
        ctx.moveTo(marker.x - 8, marker.y - 10)
        ctx.lineTo(marker.x, marker.y)
        ctx.lineTo(marker.x + 8, marker.y - 10)
        ctx.closePath()
        ctx.fill()
        ctx.stroke()
        
        // Label if present
        marker.label?.let { label ->
            ctx.font = "bold 12px Arial"
            ctx.textAlign = CanvasTextAlign.CENTER
            ctx.textBaseline = CanvasTextBaseline.MIDDLE
            setFillStyle(ctxJs, toJsString("#ffffff"))
            ctx.fillText(label, marker.x, marker.y - 15)
        }
        
        ctx.restore()
    }
    
    /**
     * Capture the current viewport
     */
    suspend fun captureViewport(options: SnapshotOptions = SnapshotOptions()): SnapshotResult? {
        val body = document.body ?: return null
        return captureElement(body, options.copy(
            width = window.innerWidth,
            height = window.innerHeight
        ))
    }
    
    /**
     * Simple delay function
     */
    private suspend fun delay(ms: Int) {
        suspendCancellableCoroutine<Unit> { cont ->
            window.setTimeout({ cont.resume(Unit); null }, ms)
        }
    }
    
    /**
     * Save snapshot to OPFS
     */
    suspend fun saveToOPFS(snapshot: SnapshotResult, path: String): Boolean {
        return try {
            val bytes = snapshot.toByteArray()
            
            val root = suspendCancellableCoroutine<JsAny> { cont ->
                getStorageDirectory().then { result: JsAny? ->
                    if (result != null) cont.resume(result)
                    null
                }
            }
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return false
            
            var dir = root
            for (i in 0 until parts.size - 1) {
                dir = suspendCancellableCoroutine { cont ->
                    getDirectoryHandle(dir, toJsString(parts[i])).then { result: JsAny? ->
                        if (result != null) cont.resume(result)
                        null
                    }
                }
            }
            
            val fileHandle = suspendCancellableCoroutine<JsAny> { cont ->
                getFileHandle(dir, toJsString(parts.last())).then { result: JsAny? ->
                    if (result != null) cont.resume(result)
                    null
                }
            }
            
            val writable = suspendCancellableCoroutine<JsAny> { cont ->
                createWritable(fileHandle).then { result: JsAny? ->
                    if (result != null) cont.resume(result)
                    null
                }
            }
            
            val uint8Array = createUint8Array(bytes.size)
            for (i in bytes.indices) {
                setUint8ArrayValue(uint8Array, i, bytes[i].toInt() and 0xFF)
            }
            
            suspendCancellableCoroutine<Unit> { cont ->
                writeToWritable(writable, uint8Array).then { _: JsAny? ->
                    cont.resume(Unit)
                    null
                }
            }
            
            suspendCancellableCoroutine<Unit> { cont ->
                closeWritable(writable).then { _: JsAny? ->
                    cont.resume(Unit)
                    null
                }
            }
            
            println("[MapSnapshot] Saved to OPFS: $path (${bytes.size} bytes)")
            true
        } catch (e: Exception) {
            println("[MapSnapshot] Failed to save to OPFS: ${e.message}")
            false
        }
    }
    
    /**
     * Download snapshot as file
     */
    fun downloadSnapshot(snapshot: SnapshotResult, filename: String) {
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = snapshot.dataUrl
        link.download = filename
        link.click()
    }
}

/**
 * Snapshot options
 */
data class SnapshotOptions(
    val format: String = "image/png",
    val quality: Double = 0.92,
    val scale: Double = 1.0,
    val backgroundColor: String? = "#ffffff",
    val width: Int? = null,
    val height: Int? = null,
    val x: Int? = null,
    val y: Int? = null,
    val useCORS: Boolean = true,
    val allowTaint: Boolean = false,
    val ignoreElements: List<String> = emptyList()
)

/**
 * Snapshot result
 */
data class SnapshotResult(
    val dataUrl: String,
    val width: Int,
    val height: Int,
    val format: String
) {
    /**
     * Get as ByteArray
     */
    suspend fun toByteArray(): ByteArray {
        return suspendCancellableCoroutine { cont ->
            try {
                val base64 = dataUrl.substringAfter(",")
                val binaryString = atobJs(toJsString(base64)).toString()
                val bytes = ByteArray(binaryString.length)
                for (i in binaryString.indices) {
                    bytes[i] = binaryString[i].code.toByte()
                }
                cont.resume(bytes)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }
    
    /**
     * Get file extension based on format
     */
    fun getFileExtension(): String {
        return when {
            format.contains("png") -> "png"
            format.contains("jpeg") || format.contains("jpg") -> "jpg"
            format.contains("webp") -> "webp"
            else -> "png"
        }
    }
    
    /**
     * Get approximate file size in bytes
     */
    fun getApproximateSize(): Int {
        val base64 = dataUrl.substringAfter(",")
        return (base64.length * 3) / 4
    }
}

/**
 * Marker info for overlay
 */
data class MarkerInfo(
    val x: Double,
    val y: Double,
    val color: String = "#e74c3c",
    val label: String? = null
)

/**
 * Extension function for MapUI to save snapshot
 */
suspend fun MapUI.saveSnapshotToFile(imagePath: String): String {
    val mapElement = document.getElementById("cspro-leaflet-map")
    if (mapElement == null) {
        println("[MapUI] Map element not found for snapshot")
        return ""
    }
    
    val snapshot = MapSnapshot.captureLeafletMap(options = SnapshotOptions(
        format = when {
            imagePath.endsWith(".jpg") || imagePath.endsWith(".jpeg") -> "image/jpeg"
            imagePath.endsWith(".webp") -> "image/webp"
            else -> "image/png"
        }
    ))
    
    if (snapshot == null) {
        println("[MapUI] Failed to capture map snapshot")
        return ""
    }
    
    // Save to OPFS
    val saved = MapSnapshot.saveToOPFS(snapshot, imagePath)
    
    return if (saved) imagePath else ""
}

/**
 * Static map renderer - generates map images without interactive map
 */
object StaticMapRenderer {
    
    /**
     * Generate a static map image using OpenStreetMap tiles
     */
    suspend fun generateStaticMap(
        centerLat: Double,
        centerLon: Double,
        zoom: Int,
        width: Int = 400,
        height: Int = 300,
        markers: List<StaticMarker> = emptyList()
    ): SnapshotResult? {
        return try {
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = width
            canvas.height = height
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            val ctxJs = ctx.unsafeCast<JsAny>()
            
            // Calculate tile coordinates
            val tileSize = 256
            val n = 1 shl zoom
            val centerTileX = ((centerLon + 180) / 360 * n)
            val centerTileY = ((1 - ln(tan(centerLat * PI / 180) + 
                1 / cos(centerLat * PI / 180)) / PI) / 2 * n)
            
            // Calculate offset from tile edge
            val offsetX = ((centerTileX - centerTileX.toInt()) * tileSize).toInt()
            val offsetY = ((centerTileY - centerTileY.toInt()) * tileSize).toInt()
            
            // Calculate tile range needed
            val tilesX = (width / tileSize) + 2
            val tilesY = (height / tileSize) + 2
            
            val startTileX = centerTileX.toInt() - tilesX / 2
            val startTileY = centerTileY.toInt() - tilesY / 2
            
            // Load and draw tiles
            for (ty in 0 until tilesY) {
                for (tx in 0 until tilesX) {
                    val tileX = startTileX + tx
                    val tileY = startTileY + ty
                    
                    if (tileX >= 0 && tileX < n && tileY >= 0 && tileY < n) {
                        val tileUrl = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                        
                        try {
                            val img = loadTileImage(tileUrl)
                            val drawX = tx * tileSize - offsetX + (width / 2) - (tilesX / 2) * tileSize
                            val drawY = ty * tileSize - offsetY + (height / 2) - (tilesY / 2) * tileSize
                            canvasDrawImage(ctxJs, img.unsafeCast<JsAny>(), drawX.toDouble(), drawY.toDouble())
                        } catch (e: Exception) {
                            // Draw placeholder for failed tile
                            setFillStyle(ctxJs, toJsString("#e0e0e0"))
                            val drawX = tx * tileSize - offsetX + (width / 2) - (tilesX / 2) * tileSize
                            val drawY = ty * tileSize - offsetY + (height / 2) - (tilesY / 2) * tileSize
                            ctx.fillRect(drawX.toDouble(), drawY.toDouble(), tileSize.toDouble(), tileSize.toDouble())
                        }
                    }
                }
            }
            
            // Draw markers
            for (marker in markers) {
                val markerPixel = latLonToPixel(marker.lat, marker.lon, centerLat, centerLon, zoom, width, height)
                drawStaticMarker(ctx, markerPixel.first, markerPixel.second, marker)
            }
            
            // Add attribution
            setFillStyle(ctxJs, toJsString("rgba(255, 255, 255, 0.8)"))
            ctx.fillRect(0.0, (height - 20).toDouble(), width.toDouble(), 20.0)
            setFillStyle(ctxJs, toJsString("#333333"))
            ctx.font = "10px Arial"
            ctx.textAlign = CanvasTextAlign.LEFT
            ctx.fillText("© OpenStreetMap contributors", 5.0, (height - 6).toDouble())
            
            val dataUrl = canvasElementToDataURL(canvas.unsafeCast<JsAny>(), "image/png", 1.0)
            
            SnapshotResult(
                dataUrl = dataUrl,
                width = width,
                height = height,
                format = "image/png"
            )
        } catch (e: Exception) {
            println("[StaticMapRenderer] Failed to generate static map: ${e.message}")
            null
        }
    }
    
    private suspend fun loadTileImage(url: String): HTMLImageElement {
        return suspendCancellableCoroutine { cont ->
            val img = document.createElement("img") as HTMLImageElement
            img.crossOrigin = "anonymous"
            img.onload = { cont.resume(img) }
            img.onerror = { _, _, _, _, _ ->
                cont.resumeWithException(Exception("Failed to load tile"))
                null
            }
            img.src = url
        }
    }
    
    private fun latLonToPixel(
        lat: Double, lon: Double,
        centerLat: Double, centerLon: Double,
        zoom: Int,
        width: Int, height: Int
    ): Pair<Double, Double> {
        val tileSize = 256
        val n = 1 shl zoom
        
        // World coordinates
        val worldX = ((lon + 180) / 360 * n * tileSize)
        val worldY = ((1 - ln(tan(lat * PI / 180) + 
            1 / cos(lat * PI / 180)) / PI) / 2 * n * tileSize)
        
        val centerWorldX = ((centerLon + 180) / 360 * n * tileSize)
        val centerWorldY = ((1 - ln(tan(centerLat * PI / 180) + 
            1 / cos(centerLat * PI / 180)) / PI) / 2 * n * tileSize)
        
        val pixelX = (worldX - centerWorldX) + width / 2
        val pixelY = (worldY - centerWorldY) + height / 2
        
        return pixelX to pixelY
    }
    
    private fun drawStaticMarker(ctx: CanvasRenderingContext2D, x: Double, y: Double, marker: StaticMarker) {
        val ctxJs = ctx.unsafeCast<JsAny>()
        ctx.save()
        
        // Draw shadow
        setFillStyle(ctxJs, toJsString("rgba(0, 0, 0, 0.3)"))
        ctx.beginPath()
        ctx.ellipse(x, y + 2, 8.0, 4.0, 0.0, 0.0, PI * 2)
        ctx.fill()
        
        // Draw pin
        setFillStyle(ctxJs, toJsString(marker.color))
        setStrokeStyle(ctxJs, toJsString("#ffffff"))
        ctx.lineWidth = 2.0
        
        ctx.beginPath()
        ctx.arc(x, y - 20, 14.0, 0.0, PI * 2)
        ctx.fill()
        ctx.stroke()
        
        ctx.beginPath()
        ctx.moveTo(x - 10, y - 12)
        ctx.lineTo(x, y)
        ctx.lineTo(x + 10, y - 12)
        ctx.closePath()
        ctx.fill()
        ctx.stroke()
        
        // Draw inner circle
        setFillStyle(ctxJs, toJsString("#ffffff"))
        ctx.beginPath()
        ctx.arc(x, y - 20, 6.0, 0.0, PI * 2)
        ctx.fill()
        
        // Draw label if present
        marker.label?.let { label ->
            ctx.font = "bold 11px Arial"
            ctx.textAlign = CanvasTextAlign.CENTER
            ctx.textBaseline = CanvasTextBaseline.TOP
            setFillStyle(ctxJs, toJsString("rgba(0, 0, 0, 0.7)"))
            ctx.fillRect(x - 30, y + 4, 60.0, 18.0)
            setFillStyle(ctxJs, toJsString("#ffffff"))
            ctx.fillText(label, x, y + 7)
        }
        
        ctx.restore()
    }
}

/**
 * Static marker for static maps
 */
data class StaticMarker(
    val lat: Double,
    val lon: Double,
    val color: String = "#e74c3c",
    val label: String? = null
)
