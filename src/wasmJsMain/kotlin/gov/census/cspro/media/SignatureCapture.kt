package gov.census.cspro.media

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Uint8Array
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Complete Signature Capture implementation using Canvas API
 * 
 * Features:
 * - Smooth Bezier curve drawing
 * - Pressure-sensitive line width (if available)
 * - Touch and mouse support
 * - Undo/redo functionality
 * - Export as PNG/JPEG/SVG
 * - Configurable styling
 * 
 * Mirrors: Android signature capture functionality
 */

// Top-level JS interop functions for WASM compatibility
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

@JsFun("(base64) => atob(base64)")
private external fun jsAtob(base64: String): String

// Helper to convert Kotlin String to JsString for canvas context
@JsFun("(s) => s")
private external fun toJsString(s: String): JsString

// Canvas context style setters (since properties expect JsAny)
@JsFun("(ctx, style) => { ctx.fillStyle = style; }")
private external fun setFillStyle(ctx: CanvasRenderingContext2D, style: String)

@JsFun("(ctx, style) => { ctx.strokeStyle = style; }")
private external fun setStrokeStyle(ctx: CanvasRenderingContext2D, style: String)

@JsFun("(el, events) => { el.style.pointerEvents = events; }")
private external fun setPointerEvents(el: HTMLCanvasElement, events: String)

@JsFun("(el, opacity) => { el.style.opacity = opacity; }")
private external fun setOpacity(el: HTMLCanvasElement, opacity: String)

// Canvas toDataURL with quality parameter
@JsFun("(canvas, type, quality) => canvas.toDataURL(type, quality)")
private external fun canvasToDataURL(canvas: HTMLCanvasElement, type: String, quality: Double): String

// TouchEvent external interface
external interface JsTouchEvent : JsAny {
    val touches: JsTouchList
}

external interface JsTouchList : JsAny {
    val length: Int
    fun item(index: Int): JsTouch?
}

external interface JsTouch : JsAny {
    val clientX: Double
    val clientY: Double
}

@JsFun("(touch) => touch.force !== undefined ? touch.force : 0.5")
private external fun jsGetTouchForce(touch: JsTouch): Double

// Pointer event helpers
@JsFun("(event) => event.pointerType")
private external fun jsGetPointerType(event: JsAny): String

@JsFun("(event) => event.offsetX")
private external fun jsGetOffsetX(event: JsAny): Double

@JsFun("(event) => event.offsetY")
private external fun jsGetOffsetY(event: JsAny): Double

@JsFun("(event) => event.pressure !== undefined ? event.pressure : 0.5")
private external fun jsGetPressure(event: JsAny): Double

class SignaturePad(
    private val containerId: String,
    private val options: SignaturePadOptions = SignaturePadOptions()
) {
    private var canvas: HTMLCanvasElement? = null
    private var ctx: CanvasRenderingContext2D? = null
    
    // Drawing state
    private var isDrawing = false
    private var points: MutableList<Point> = mutableListOf()
    private var strokes: MutableList<Stroke> = mutableListOf()
    private var currentStroke: Stroke? = null
    private var undoStack: MutableList<List<Stroke>> = mutableListOf()
    private var redoStack: MutableList<List<Stroke>> = mutableListOf()
    
    // Velocity tracking for variable line width
    private var lastPoint: Point? = null
    private var lastVelocity = 0.0
    private var lastWidth = options.penWidth
    
    // Callbacks
    var onBeginStroke: (() -> Unit)? = null
    var onEndStroke: (() -> Unit)? = null
    var onClear: (() -> Unit)? = null
    var onChange: (() -> Unit)? = null
    
    /**
     * Initialize the signature pad
     */
    fun initialize(): Boolean {
        val container = document.getElementById(containerId) ?: return false
        
        // Create canvas element
        canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas?.apply {
            id = "${containerId}-canvas"
            width = options.width
            height = options.height
            style.cssText = """
                border: ${options.borderWidth}px solid ${options.borderColor};
                border-radius: ${options.borderRadius}px;
                background-color: ${options.backgroundColor};
                touch-action: none;
                cursor: crosshair;
            """
        }
        
        container.appendChild(canvas!!)
        
        // Get 2D context
        ctx = canvas?.getContext("2d") as? CanvasRenderingContext2D
        ctx?.apply {
            lineCap = CanvasLineCap.ROUND
            lineJoin = CanvasLineJoin.ROUND
        }
        
        // Setup event listeners
        setupEventListeners()
        
        // Clear canvas
        clear()
        
        return true
    }
    
    /**
     * Initialize with existing canvas element
     */
    fun initializeWithCanvas(existingCanvas: HTMLCanvasElement): Boolean {
        canvas = existingCanvas
        ctx = canvas?.getContext("2d") as? CanvasRenderingContext2D
        ctx?.apply {
            lineCap = CanvasLineCap.ROUND
            lineJoin = CanvasLineJoin.ROUND
        }
        
        setupEventListeners()
        clear()
        
        return true
    }
    
    private fun setupEventListeners() {
        val canvasEl = canvas ?: return
        
        // Mouse events
        canvasEl.addEventListener("mousedown", { e ->
            val event = e as MouseEvent
            e.preventDefault()
            handlePointerDown(event.offsetX, event.offsetY, 0.5)
        })
        
        canvasEl.addEventListener("mousemove", { e ->
            val event = e as MouseEvent
            if (isDrawing) {
                handlePointerMove(event.offsetX, event.offsetY, 0.5)
            }
        })
        
        canvasEl.addEventListener("mouseup", { handlePointerUp() })
        canvasEl.addEventListener("mouseleave", { if (isDrawing) handlePointerUp() })
        
        // Touch events
        canvasEl.addEventListener("touchstart", { e ->
            e.preventDefault()
            val touchEvent = e.unsafeCast<JsTouchEvent>()
            val touch = touchEvent.touches.item(0) ?: return@addEventListener
            val rect = canvasEl.getBoundingClientRect()
            val x = touch.clientX - rect.left
            val y = touch.clientY - rect.top
            val pressure = jsGetTouchForce(touch)
            handlePointerDown(x, y, pressure)
        })
        
        canvasEl.addEventListener("touchmove", { e ->
            e.preventDefault()
            if (!isDrawing) return@addEventListener
            val touchEvent = e.unsafeCast<JsTouchEvent>()
            val touch = touchEvent.touches.item(0) ?: return@addEventListener
            val rect = canvasEl.getBoundingClientRect()
            val x = touch.clientX - rect.left
            val y = touch.clientY - rect.top
            val pressure = jsGetTouchForce(touch)
            handlePointerMove(x, y, pressure)
        })
        
        canvasEl.addEventListener("touchend", { handlePointerUp() })
        canvasEl.addEventListener("touchcancel", { handlePointerUp() })
        
        // Pointer events (for pressure support)
        canvasEl.addEventListener("pointerdown", { e ->
            val event = e.unsafeCast<JsAny>()
            val pointerType = jsGetPointerType(event)
            if (pointerType == "touch" || pointerType == "pen") {
                e.preventDefault()
                handlePointerDown(
                    jsGetOffsetX(event),
                    jsGetOffsetY(event),
                    jsGetPressure(event)
                )
            }
        })
        
        canvasEl.addEventListener("pointermove", { e ->
            if (!isDrawing) return@addEventListener
            val event = e.unsafeCast<JsAny>()
            val pointerType = jsGetPointerType(event)
            if (pointerType == "touch" || pointerType == "pen") {
                handlePointerMove(
                    jsGetOffsetX(event),
                    jsGetOffsetY(event),
                    jsGetPressure(event)
                )
            }
        })
        
        canvasEl.addEventListener("pointerup", { handlePointerUp() })
        canvasEl.addEventListener("pointercancel", { handlePointerUp() })
    }
    
    private fun handlePointerDown(x: Double, y: Double, pressure: Double) {
        // Save state for undo
        saveStateForUndo()
        redoStack.clear()
        
        isDrawing = true
        points.clear()
        currentStroke = Stroke(color = options.penColor, minWidth = options.minWidth, maxWidth = options.penWidth)
        
        val point = Point(x, y, pressure, currentTimeMillis())
        points.add(point)
        lastPoint = point
        lastVelocity = 0.0
        lastWidth = options.penWidth
        
        onBeginStroke?.invoke()
    }
    
    private fun handlePointerMove(x: Double, y: Double, pressure: Double) {
        if (!isDrawing) return
        
        val point = Point(x, y, pressure, currentTimeMillis())
        points.add(point)
        
        // Draw with variable width based on velocity
        val velocity = calculateVelocity(lastPoint!!, point)
        val newWidth = calculateWidth(velocity, pressure)
        
        if (points.size >= 3) {
            // Use Bezier curve for smooth lines
            drawBezierCurve(points, newWidth)
        } else if (points.size == 2) {
            drawLine(points[0], points[1], newWidth)
        }
        
        // Update for next segment
        currentStroke?.points?.add(point)
        lastPoint = point
        lastVelocity = velocity
        lastWidth = newWidth
        
        // Keep only last 3 points for Bezier
        if (points.size > 3) {
            points.removeAt(0)
        }
        
        onChange?.invoke()
    }
    
    private fun handlePointerUp() {
        if (!isDrawing) return
        
        isDrawing = false
        
        // Complete the stroke
        currentStroke?.let { stroke ->
            stroke.points.addAll(points)
            strokes.add(stroke)
        }
        currentStroke = null
        points.clear()
        lastPoint = null
        
        onEndStroke?.invoke()
    }
    
    private fun calculateVelocity(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val distance = sqrt(dx * dx + dy * dy)
        val timeDelta = (p2.time - p1.time).coerceAtLeast(1)
        return distance / timeDelta
    }
    
    private fun calculateWidth(velocity: Double, pressure: Double): Double {
        // Smooth velocity
        val smoothedVelocity = 0.7 * velocity + 0.3 * lastVelocity
        
        // Width based on velocity (faster = thinner)
        val velocityFactor = (0.5).pow(smoothedVelocity * options.velocitySensitivity)
        
        // Apply pressure
        val pressureFactor = if (options.usePressure) pressure else 1.0
        
        // Calculate width
        var width = options.minWidth + (options.penWidth - options.minWidth) * velocityFactor * pressureFactor
        
        // Smooth width transition
        width = 0.6 * width + 0.4 * lastWidth
        
        return width.coerceIn(options.minWidth, options.penWidth)
    }
    
    private fun drawLine(p1: Point, p2: Point, width: Double) {
        ctx?.apply {
            beginPath()
            setStrokeStyle(this, options.penColor)
            lineWidth = width
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            stroke()
        }
    }
    
    private fun drawBezierCurve(points: List<Point>, width: Double) {
        if (points.size < 3) return
        
        ctx?.apply {
            beginPath()
            setStrokeStyle(this, options.penColor)
            lineWidth = width
            
            // Calculate control points for smooth curve
            val p0 = points[0]
            val p1 = points[1]
            val p2 = points[2]
            
            // Midpoints
            val mid1X = (p0.x + p1.x) / 2
            val mid1Y = (p0.y + p1.y) / 2
            val mid2X = (p1.x + p2.x) / 2
            val mid2Y = (p1.y + p2.y) / 2
            
            moveTo(mid1X, mid1Y)
            quadraticCurveTo(p1.x, p1.y, mid2X, mid2Y)
            stroke()
        }
    }
    
    /**
     * Clear the signature pad
     */
    fun clear() {
        val canvasEl = canvas ?: return
        ctx?.apply {
            setFillStyle(this, options.backgroundColor)
            fillRect(0.0, 0.0, canvasEl.width.toDouble(), canvasEl.height.toDouble())
        }
        strokes.clear()
        points.clear()
        undoStack.clear()
        redoStack.clear()
        onClear?.invoke()
    }
    
    /**
     * Check if the pad is empty
     */
    fun isEmpty(): Boolean {
        return strokes.isEmpty() && points.isEmpty()
    }
    
    /**
     * Save state for undo
     */
    private fun saveStateForUndo() {
        undoStack.add(strokes.map { it.copy() })
        // Limit undo stack size
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }
    
    /**
     * Undo last stroke
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        
        // Save current state for redo
        redoStack.add(strokes.map { it.copy() })
        
        // Restore previous state
        strokes.clear()
        strokes.addAll(undoStack.removeLast())
        
        // Redraw
        redrawStrokes()
        onChange?.invoke()
        
        return true
    }
    
    /**
     * Redo last undone stroke
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        
        // Save current state for undo
        undoStack.add(strokes.map { it.copy() })
        
        // Restore next state
        strokes.clear()
        strokes.addAll(redoStack.removeLast())
        
        // Redraw
        redrawStrokes()
        onChange?.invoke()
        
        return true
    }
    
    /**
     * Redraw all strokes
     */
    private fun redrawStrokes() {
        val canvasEl = canvas ?: return
        
        // Clear canvas
        ctx?.apply {
            setFillStyle(this, options.backgroundColor)
            fillRect(0.0, 0.0, canvasEl.width.toDouble(), canvasEl.height.toDouble())
        }
        
        // Draw all strokes
        for (stroke in strokes) {
            drawStroke(stroke)
        }
    }
    
    private fun drawStroke(stroke: Stroke) {
        if (stroke.points.size < 2) return
        
        ctx?.apply {
            setStrokeStyle(this, stroke.color)
            lineWidth = stroke.minWidth
            lineCap = CanvasLineCap.ROUND
            lineJoin = CanvasLineJoin.ROUND
            
            beginPath()
            moveTo(stroke.points[0].x, stroke.points[0].y)
            
            for (i in 1 until stroke.points.size - 1) {
                val p0 = stroke.points[i - 1]
                val p1 = stroke.points[i]
                val p2 = stroke.points[i + 1]
                
                val midX = (p1.x + p2.x) / 2
                val midY = (p1.y + p2.y) / 2
                
                quadraticCurveTo(p1.x, p1.y, midX, midY)
            }
            
            // Draw last segment
            val lastPoint = stroke.points.last()
            lineTo(lastPoint.x, lastPoint.y)
            stroke()
        }
    }
    
    /**
     * Export signature as PNG data URL
     */
    fun toPngDataUrl(): String {
        return canvas?.toDataURL("image/png") ?: ""
    }
    
    /**
     * Export signature as JPEG data URL
     */
    fun toJpegDataUrl(quality: Double = 0.92): String {
        val canvasEl = canvas ?: return ""
        return canvasToDataURL(canvasEl, "image/jpeg", quality)
    }
    
    /**
     * Export signature as SVG
     */
    fun toSvg(): String {
        val canvasEl = canvas ?: return ""
        val width = canvasEl.width
        val height = canvasEl.height
        
        val pathData = StringBuilder()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            
            pathData.append("M ${stroke.points[0].x} ${stroke.points[0].y} ")
            for (i in 1 until stroke.points.size) {
                pathData.append("L ${stroke.points[i].x} ${stroke.points[i].y} ")
            }
        }
        
        return """
            <svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height">
                <rect width="100%" height="100%" fill="${options.backgroundColor}"/>
                <path d="${pathData.toString().trim()}" 
                      stroke="${options.penColor}" 
                      stroke-width="${options.penWidth}" 
                      fill="none"
                      stroke-linecap="round"
                      stroke-linejoin="round"/>
            </svg>
        """.trimIndent()
    }
    
    /**
     * Export as ByteArray (PNG)
     */
    suspend fun toByteArray(): ByteArray {
        val dataUrl = toPngDataUrl()
        return dataUrlToByteArray(dataUrl)
    }
    
    private suspend fun dataUrlToByteArray(dataUrl: String): ByteArray {
        return suspendCancellableCoroutine { cont ->
            try {
                // Remove data URL prefix
                val base64 = dataUrl.substringAfter(",")
                val binaryString = jsAtob(base64)
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
     * Load signature from data URL
     */
    fun fromDataUrl(dataUrl: String) {
        if (!dataUrl.startsWith("data:image")) return
        
        val img = document.createElement("img") as HTMLImageElement
        img.onload = {
            clear()
            ctx?.drawImage(img, 0.0, 0.0)
            // Mark as having content
            strokes.add(Stroke("loaded", options.minWidth, options.penWidth, mutableListOf(Point(0.0, 0.0, 1.0, 0))))
        }
        img.src = dataUrl
    }
    
    /**
     * Get the bounding box of the signature
     */
    fun getSignatureBounds(): SignatureBounds? {
        if (strokes.isEmpty()) return null
        
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE
        
        for (stroke in strokes) {
            for (point in stroke.points) {
                if (point.x < minX) minX = point.x
                if (point.y < minY) minY = point.y
                if (point.x > maxX) maxX = point.x
                if (point.y > maxY) maxY = point.y
            }
        }
        
        return SignatureBounds(minX, minY, maxX - minX, maxY - minY)
    }
    
    /**
     * Crop the signature to its bounding box with padding
     */
    fun getCroppedDataUrl(padding: Int = 10): String {
        val bounds = getSignatureBounds() ?: return toPngDataUrl()
        val canvasEl = canvas ?: return ""
        
        // Create temporary canvas
        val tempCanvas = document.createElement("canvas") as HTMLCanvasElement
        val cropWidth = (bounds.width + padding * 2).toInt()
        val cropHeight = (bounds.height + padding * 2).toInt()
        tempCanvas.width = cropWidth
        tempCanvas.height = cropHeight
        
        val tempCtx = tempCanvas.getContext("2d") as CanvasRenderingContext2D
        setFillStyle(tempCtx, options.backgroundColor)
        tempCtx.fillRect(0.0, 0.0, cropWidth.toDouble(), cropHeight.toDouble())
        
        // Copy relevant portion
        tempCtx.drawImage(
            canvasEl,
            (bounds.x - padding).coerceAtLeast(0.0),
            (bounds.y - padding).coerceAtLeast(0.0),
            (bounds.width + padding * 2).coerceAtMost(canvasEl.width.toDouble()),
            (bounds.height + padding * 2).coerceAtMost(canvasEl.height.toDouble()),
            0.0, 0.0,
            cropWidth.toDouble(), cropHeight.toDouble()
        )
        
        return tempCanvas.toDataURL("image/png")
    }
    
    /**
     * Resize the canvas
     */
    fun resize(width: Int, height: Int) {
        val oldData = toPngDataUrl()
        canvas?.width = width
        canvas?.height = height
        clear()
        if (oldData.isNotEmpty()) {
            fromDataUrl(oldData)
        }
    }
    
    /**
     * Enable/disable the signature pad
     */
    fun setEnabled(enabled: Boolean) {
        canvas?.let { 
            setPointerEvents(it, if (enabled) "auto" else "none")
            setOpacity(it, if (enabled) "1" else "0.5")
        }
    }
    
    private fun currentTimeMillis(): Long {
        return jsDateNow().toLong()
    }
    
    fun destroy() {
        canvas?.remove()
        canvas = null
        ctx = null
    }
}

/**
 * Signature pad options
 */
data class SignaturePadOptions(
    val width: Int = 400,
    val height: Int = 200,
    val penColor: String = "#000000",
    val penWidth: Double = 3.0,
    val minWidth: Double = 0.5,
    val backgroundColor: String = "#ffffff",
    val borderColor: String = "#cccccc",
    val borderWidth: Int = 1,
    val borderRadius: Int = 4,
    val velocitySensitivity: Double = 0.7,
    val usePressure: Boolean = true
)

/**
 * Point in a stroke
 */
data class Point(
    val x: Double,
    val y: Double,
    val pressure: Double,
    val time: Long
)

/**
 * A stroke (collection of points)
 */
data class Stroke(
    val color: String,
    val minWidth: Double,
    val maxWidth: Double,
    val points: MutableList<Point> = mutableListOf()
) {
    fun copy(): Stroke = Stroke(color, minWidth, maxWidth, points.toMutableList())
}

/**
 * Signature bounding box
 */
data class SignatureBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

/**
 * Factory for creating signature pad in CSPro dialogs
 */
object SignatureCapture {
    
    /**
     * Show signature capture dialog
     */
    suspend fun capture(
        title: String = "Sign Here",
        options: SignaturePadOptions = SignaturePadOptions()
    ): String? {
        val result = CompletableDeferred<String?>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.style.cssText = """
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 10000;
        """
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.cssText = """
            background: white;
            border-radius: 8px;
            padding: 20px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
            max-width: 90%;
        """
        
        dialog.innerHTML = """
            <h3 style="margin: 0 0 15px 0; text-align: center;">$title</h3>
            <div id="signature-capture-container"></div>
            <div style="display: flex; gap: 10px; justify-content: center; margin-top: 15px;">
                <button id="sig-clear" style="padding: 10px 20px; border: 1px solid #ccc; border-radius: 4px; background: white; cursor: pointer;">
                    Clear
                </button>
                <button id="sig-undo" style="padding: 10px 20px; border: 1px solid #ccc; border-radius: 4px; background: white; cursor: pointer;">
                    Undo
                </button>
                <button id="sig-cancel" style="padding: 10px 20px; border: 1px solid #ccc; border-radius: 4px; background: #f0f0f0; cursor: pointer;">
                    Cancel
                </button>
                <button id="sig-done" style="padding: 10px 20px; border: none; border-radius: 4px; background: #3498db; color: white; cursor: pointer;">
                    Done
                </button>
            </div>
        """
        
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        // Initialize signature pad
        val pad = SignaturePad("signature-capture-container", options)
        pad.initialize()
        
        // Setup button handlers
        document.getElementById("sig-clear")?.addEventListener("click", {
            pad.clear()
        })
        
        document.getElementById("sig-undo")?.addEventListener("click", {
            pad.undo()
        })
        
        document.getElementById("sig-cancel")?.addEventListener("click", {
            pad.destroy()
            overlay.remove()
            result.complete(null)
        })
        
        document.getElementById("sig-done")?.addEventListener("click", {
            val dataUrl = if (pad.isEmpty()) null else pad.toPngDataUrl()
            pad.destroy()
            overlay.remove()
            result.complete(dataUrl)
        })
        
        return result.await()
    }
}
