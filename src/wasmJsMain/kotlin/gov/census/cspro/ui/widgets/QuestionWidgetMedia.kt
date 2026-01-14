package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import gov.census.cspro.media.AudioRecorder
import gov.census.cspro.media.AudioPlayer
import gov.census.cspro.media.RecordingState
import gov.census.cspro.media.SignaturePad
import gov.census.cspro.media.SignaturePadOptions
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.Blob
import org.w3c.files.File
import org.w3c.files.FileReader

// Top-level functions for CSPro dialog integration
private fun showCSProAlert(message: String) {
    println("[CSPro Alert] $message")
}

/**
 * Photo capture widget - Web equivalent of Android's QuestionWidgetPhoto
 * 
 * Supports:
 * - Taking photo with camera (MediaDevices API)
 * - Choosing existing image file
 * - Displaying captured/selected image
 * - Deleting image
 */
class QuestionWidgetPhoto(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var currentImageDataUrl: String? = null
    private var imageElement: HTMLImageElement? = null
    private var fileInput: HTMLInputElement? = null
    private var cameraInput: HTMLInputElement? = null
    
    override fun render(parentElement: HTMLElement) {
        val widgetId = "photo-widget-${field.name}"
        
        container = document.createElement("div") as HTMLDivElement
        container?.apply {
            className = "question-widget question-widget-photo"
            id = widgetId
            
            innerHTML = """
                ${createQuestionHeader()}
                <div class="photo-container">
                    <div class="photo-preview" id="${widgetId}-preview">
                        <img id="${widgetId}-image" class="photo-image" style="display: none;" />
                        <div class="photo-placeholder" id="${widgetId}-placeholder">
                            <span class="photo-icon">📷</span>
                            <span class="photo-text">No photo captured</span>
                        </div>
                    </div>
                    <div class="photo-controls" id="${widgetId}-controls">
                        <input type="file" accept="image/*" capture="environment" 
                               id="${widgetId}-camera" style="display: none;" />
                        <input type="file" accept="image/*" 
                               id="${widgetId}-file" style="display: none;" />
                        <button type="button" class="photo-btn photo-btn-camera" id="${widgetId}-take">
                            📸 Take Photo
                        </button>
                        <button type="button" class="photo-btn photo-btn-choose" id="${widgetId}-choose">
                            📁 Choose File
                        </button>
                        <button type="button" class="photo-btn photo-btn-delete" id="${widgetId}-delete" 
                                style="display: none;">
                            🗑️ Delete
                        </button>
                    </div>
                </div>
            """.trimIndent()
        }
        
        parentElement.appendChild(container!!)
        
        // Get elements
        imageElement = document.getElementById("${widgetId}-image") as? HTMLImageElement
        cameraInput = document.getElementById("${widgetId}-camera") as? HTMLInputElement
        fileInput = document.getElementById("${widgetId}-file") as? HTMLInputElement
        
        // Set up event listeners
        setupEventListeners(widgetId)
        
        // Load existing value if any
        if (field.value.isNotEmpty()) {
            loadExistingImage(field.value)
        }
        
        // Disable controls if protected
        if (field.isProtected) {
            disableControls(widgetId)
        }
    }
    
    private fun setupEventListeners(widgetId: String) {
        // Camera button
        document.getElementById("${widgetId}-take")?.addEventListener("click", {
            cameraInput?.click()
        })
        
        // Choose file button
        document.getElementById("${widgetId}-choose")?.addEventListener("click", {
            fileInput?.click()
        })
        
        // Delete button
        document.getElementById("${widgetId}-delete")?.addEventListener("click", {
            deletePhoto(widgetId)
        })
        
        // Camera input change
        cameraInput?.addEventListener("change", { event ->
            handleFileSelected(event, widgetId)
        })
        
        // File input change
        fileInput?.addEventListener("change", { event ->
            handleFileSelected(event, widgetId)
        })
        
        // Click on image to view
        imageElement?.addEventListener("click", {
            viewPhoto()
        })
    }
    
    private fun handleFileSelected(event: Event, widgetId: String) {
        val input = event.target as? HTMLInputElement ?: return
        val files = input.files ?: return
        if (files.length == 0) return
        
        val file = files.item(0) ?: return
        
        // Read file as data URL
        val reader = FileReader()
        reader.onload = { loadEvent ->
            val result = (loadEvent.target as? FileReader)?.result?.toString()
            if (result != null) {
                currentImageDataUrl = result
                displayImage(result, widgetId)
                onValueChanged?.invoke(result)
            }
        }
        reader.readAsDataURL(file)
    }
    
    private fun displayImage(dataUrl: String, widgetId: String) {
        imageElement?.apply {
            src = dataUrl
            style.display = "block"
        }
        
        document.getElementById("${widgetId}-placeholder")?.let {
            (it as HTMLElement).style.display = "none"
        }
        
        document.getElementById("${widgetId}-delete")?.let {
            (it as HTMLElement).style.display = "inline-flex"
        }
    }
    
    private fun deletePhoto(widgetId: String) {
        currentImageDataUrl = null
        
        imageElement?.apply {
            src = ""
            style.display = "none"
        }
        
        document.getElementById("${widgetId}-placeholder")?.let {
            (it as HTMLElement).style.display = "flex"
        }
        
        document.getElementById("${widgetId}-delete")?.let {
            (it as HTMLElement).style.display = "none"
        }
        
        // Clear file inputs
        cameraInput?.value = ""
        fileInput?.value = ""
        
        onValueChanged?.invoke("")
    }
    
    private fun loadExistingImage(value: String) {
        // Value could be a data URL or a file path
        if (value.startsWith("data:") || value.startsWith("http")) {
            currentImageDataUrl = value
            displayImage(value, "photo-widget-${field.name}")
        }
    }
    
    private fun viewPhoto() {
        val dataUrl = currentImageDataUrl ?: return
        // Open image in new window/tab
        window.open(dataUrl, "_blank")
    }
    
    private fun disableControls(widgetId: String) {
        document.getElementById("${widgetId}-controls")?.let {
            it.querySelectorAll("button").asList().forEach { btn ->
                (btn as? HTMLButtonElement)?.disabled = true
            }
        }
    }
    
    override fun getValue(): String = currentImageDataUrl ?: ""
    
    override fun setValue(value: String) {
        if (value.isNotEmpty()) {
            loadExistingImage(value)
        } else {
            deletePhoto("photo-widget-${field.name}")
        }
    }
    
    override fun focus() {
        document.getElementById("photo-widget-${field.name}-take")?.let {
            (it as? HTMLElement)?.focus()
        }
    }
}

/**
 * Audio recording widget - Web equivalent of Android's QuestionWidgetAudio
 * 
 * Supports:
 * - Recording audio with microphone (MediaRecorder API)
 * - Choosing existing audio file
 * - Playing recorded/selected audio
 * - Deleting audio
 * 
 * Uses AudioRecorder class for complete MediaRecorder API integration
 */
class QuestionWidgetAudio(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var currentAudioDataUrl: String? = null
    private var audioElement: HTMLAudioElement? = null
    private var fileInput: HTMLInputElement? = null
    private var recordingTimer: Int? = null
    
    // Use the complete AudioRecorder implementation
    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer()
    private val scope = MainScope()
    
    override fun render(parentElement: HTMLElement) {
        val widgetId = "audio-widget-${field.name}"
        
        // Setup audio recorder callbacks
        audioRecorder.onStateChanged = { state ->
            when (state) {
                RecordingState.RECORDING -> onRecordingStarted(widgetId)
                RecordingState.STOPPED -> { /* handled in stopRecording */ }
                RecordingState.PAUSED -> updateStatusText(widgetId, "Recording paused")
                RecordingState.ERROR -> updateStatusText(widgetId, "Recording error")
                RecordingState.IDLE -> { }
            }
        }
        
        audioRecorder.onAudioLevel = { level ->
            updateAudioLevelIndicator(widgetId, level)
        }
        
        container = document.createElement("div") as HTMLDivElement
        container?.apply {
            className = "question-widget question-widget-audio"
            id = widgetId
            
            innerHTML = """
                ${createQuestionHeader()}
                <div class="audio-container">
                    <div class="audio-status" id="${widgetId}-status">
                        <span class="audio-icon">🎤</span>
                        <span class="audio-text" id="${widgetId}-status-text">No audio recorded</span>
                        <span class="audio-duration" id="${widgetId}-duration"></span>
                    </div>
                    <div class="audio-level" id="${widgetId}-level" style="display: none;">
                        <div class="audio-level-bar" id="${widgetId}-level-bar"></div>
                    </div>
                    <audio id="${widgetId}-audio" style="display: none;" controls></audio>
                    <div class="audio-controls" id="${widgetId}-controls">
                        <input type="file" accept="audio/*" 
                               id="${widgetId}-file" style="display: none;" />
                        <button type="button" class="audio-btn audio-btn-record" id="${widgetId}-record">
                            🎙️ Record
                        </button>
                        <button type="button" class="audio-btn audio-btn-stop" id="${widgetId}-stop" 
                                style="display: none;">
                            ⏹️ Stop
                        </button>
                        <button type="button" class="audio-btn audio-btn-play" id="${widgetId}-play" 
                                style="display: none;">
                            ▶️ Play
                        </button>
                        <button type="button" class="audio-btn audio-btn-choose" id="${widgetId}-choose">
                            📁 Choose File
                        </button>
                        <button type="button" class="audio-btn audio-btn-delete" id="${widgetId}-delete" 
                                style="display: none;">
                            🗑️ Delete
                        </button>
                    </div>
                    <div class="audio-recording-indicator" id="${widgetId}-recording" style="display: none;">
                        <span class="recording-dot"></span>
                        <span class="recording-time" id="${widgetId}-timer">00:00</span>
                    </div>
                </div>
            """.trimIndent()
        }
        
        parentElement.appendChild(container!!)
        
        // Get elements
        audioElement = document.getElementById("${widgetId}-audio") as? HTMLAudioElement
        fileInput = document.getElementById("${widgetId}-file") as? HTMLInputElement
        
        // Set up event listeners
        setupEventListeners(widgetId)
        
        // Load existing value if any
        if (field.value.isNotEmpty()) {
            loadExistingAudio(field.value)
        }
        
        // Disable controls if protected
        if (field.isProtected) {
            disableControls(widgetId)
        }
    }
    
    private fun setupEventListeners(widgetId: String) {
        // Record button
        document.getElementById("${widgetId}-record")?.addEventListener("click", {
            startRecording(widgetId)
        })
        
        // Stop button
        document.getElementById("${widgetId}-stop")?.addEventListener("click", {
            stopRecording(widgetId)
        })
        
        // Play button
        document.getElementById("${widgetId}-play")?.addEventListener("click", {
            playAudio()
        })
        
        // Choose file button
        document.getElementById("${widgetId}-choose")?.addEventListener("click", {
            fileInput?.click()
        })
        
        // Delete button
        document.getElementById("${widgetId}-delete")?.addEventListener("click", {
            deleteAudio(widgetId)
        })
        
        // File input change
        fileInput?.addEventListener("change", { event ->
            handleFileSelected(event, widgetId)
        })
        
        // Audio ended event
        audioElement?.addEventListener("ended", {
            updatePlayButton(widgetId, false)
        })
    }
    
    private fun startRecording(widgetId: String) {
        // Use the complete AudioRecorder class
        if (!audioRecorder.isSupported()) {
            showCSProAlert("Audio recording is not supported in this browser")
            return
        }
        
        scope.launch {
            val started = audioRecorder.startRecording()
            if (!started) {
                showCSProAlert("Could not start recording. Please check microphone permissions.")
            }
        }
    }
    
    private fun onRecordingStarted(widgetId: String) {
        // Show stop button, hide record button
        document.getElementById("${widgetId}-record")?.let {
            (it as HTMLElement).style.display = "none"
        }
        document.getElementById("${widgetId}-stop")?.let {
            (it as HTMLElement).style.display = "inline-flex"
        }
        document.getElementById("${widgetId}-recording")?.let {
            (it as HTMLElement).style.display = "flex"
        }
        document.getElementById("${widgetId}-level")?.let {
            (it as HTMLElement).style.display = "block"
        }
        
        // Start timer
        recordingTimer = window.setInterval({
            updateRecordingTimer(widgetId)
            null
        }, 100)
    }
    
    private fun updateRecordingTimer(widgetId: String) {
        val elapsed = audioRecorder.getCurrentDuration()
        val seconds = ((elapsed / 1000) % 60).toInt()
        val minutes = ((elapsed / 1000) / 60).toInt()
        val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        
        document.getElementById("${widgetId}-timer")?.textContent = timeStr
    }
    
    private fun updateAudioLevelIndicator(widgetId: String, level: Float) {
        document.getElementById("${widgetId}-level-bar")?.let { bar ->
            (bar as HTMLElement).style.width = "${(level * 100).toInt()}%"
        }
    }
    
    private fun updateStatusText(widgetId: String, text: String) {
        document.getElementById("${widgetId}-status-text")?.textContent = text
    }
    
    private fun stopRecording(widgetId: String) {
        // Clear timer
        recordingTimer?.let { window.clearInterval(it) }
        recordingTimer = null
        
        // Hide stop button and recording indicator
        document.getElementById("${widgetId}-stop")?.let {
            (it as HTMLElement).style.display = "none"
        }
        document.getElementById("${widgetId}-recording")?.let {
            (it as HTMLElement).style.display = "none"
        }
        document.getElementById("${widgetId}-level")?.let {
            (it as HTMLElement).style.display = "none"
        }
        document.getElementById("${widgetId}-record")?.let {
            (it as HTMLElement).style.display = "inline-flex"
        }
        
        // Stop recording and get result
        scope.launch {
            val recording = audioRecorder.stopRecording()
            if (recording != null) {
                val dataUrl = recording.toDataUrl()
                onRecordingComplete(widgetId, dataUrl)
            }
        }
    }
    
    private fun onRecordingComplete(widgetId: String, dataUrl: String) {
        currentAudioDataUrl = dataUrl
        displayAudio(dataUrl, widgetId)
        onValueChanged?.invoke(dataUrl)
    }
    
    private fun handleFileSelected(event: Event, widgetId: String) {
        val input = event.target as? HTMLInputElement ?: return
        val files = input.files ?: return
        if (files.length == 0) return
        
        val file = files.item(0) ?: return
        
        val reader = FileReader()
        reader.onload = { loadEvent ->
            val result = (loadEvent.target as? FileReader)?.result?.toString()
            if (result != null) {
                currentAudioDataUrl = result
                displayAudio(result, widgetId)
                onValueChanged?.invoke(result)
            }
        }
        reader.readAsDataURL(file)
    }
    
    private fun displayAudio(dataUrl: String, widgetId: String) {
        audioElement?.src = dataUrl
        
        document.getElementById("${widgetId}-status-text")?.textContent = "Audio recorded"
        document.getElementById("${widgetId}-play")?.let {
            (it as HTMLElement).style.display = "inline-flex"
        }
        document.getElementById("${widgetId}-delete")?.let {
            (it as HTMLElement).style.display = "inline-flex"
        }
    }
    
    private fun playAudio() {
        audioElement?.play()
    }
    
    private fun updatePlayButton(widgetId: String, playing: Boolean) {
        val playBtn = document.getElementById("${widgetId}-play") as? HTMLButtonElement
        playBtn?.textContent = if (playing) "⏸️ Pause" else "▶️ Play"
    }
    
    private fun deleteAudio(widgetId: String) {
        currentAudioDataUrl = null
        audioElement?.src = ""
        
        document.getElementById("${widgetId}-status-text")?.textContent = "No audio recorded"
        document.getElementById("${widgetId}-duration")?.textContent = ""
        document.getElementById("${widgetId}-play")?.let {
            (it as HTMLElement).style.display = "none"
        }
        document.getElementById("${widgetId}-delete")?.let {
            (it as HTMLElement).style.display = "none"
        }
        
        fileInput?.value = ""
        onValueChanged?.invoke("")
    }
    
    private fun loadExistingAudio(value: String) {
        if (value.startsWith("data:") || value.startsWith("http")) {
            currentAudioDataUrl = value
            displayAudio(value, "audio-widget-${field.name}")
        }
    }
    
    private fun disableControls(widgetId: String) {
        document.getElementById("${widgetId}-controls")?.let {
            it.querySelectorAll("button").asList().forEach { btn ->
                (btn as? HTMLButtonElement)?.disabled = true
            }
        }
    }
    
    override fun getValue(): String = currentAudioDataUrl ?: ""
    
    override fun setValue(value: String) {
        if (value.isNotEmpty()) {
            loadExistingAudio(value)
        } else {
            deleteAudio("audio-widget-${field.name}")
        }
    }
    
    override fun focus() {
        document.getElementById("audio-widget-${field.name}-record")?.let {
            (it as? HTMLElement)?.focus()
        }
    }
    
    override fun destroy() {
        // Stop recording if active and cleanup
        audioRecorder.destroy()
        audioPlayer.destroy()
        recordingTimer?.let { window.clearInterval(it) }
        super.destroy()
    }
}

/**
 * Signature capture widget - Web equivalent of Android's QuestionWidgetSignature
 * 
 * Uses SignaturePad class for complete canvas-based signature capture with:
 * - Smooth Bezier curve drawing
 * - Pressure-sensitive line width
 * - Touch and mouse support
 * - Undo functionality
 * - Export as PNG
 */
class QuestionWidgetSignature(
    field: CDEField,
    setFocus: Boolean = false,
    emitNextPage: Boolean = true,
    showCodes: Boolean = false
) : QuestionWidget(field, setFocus, emitNextPage, showCodes) {
    
    private var signaturePad: SignaturePad? = null
    private var hasSignature = false
    
    override fun render(parentElement: HTMLElement) {
        val widgetId = "signature-widget-${field.name}"
        
        container = document.createElement("div") as HTMLDivElement
        container?.apply {
            className = "question-widget question-widget-signature"
            id = widgetId
            
            innerHTML = """
                ${createQuestionHeader()}
                <div class="signature-container">
                    <div class="signature-canvas-wrapper" id="${widgetId}-pad-container">
                        <!-- SignaturePad will be initialized here -->
                    </div>
                    <div class="signature-placeholder" id="${widgetId}-placeholder">
                        Sign here
                    </div>
                    <div class="signature-controls" id="${widgetId}-controls">
                        <button type="button" class="signature-btn signature-btn-undo" 
                                id="${widgetId}-undo">
                            ↩️ Undo
                        </button>
                        <button type="button" class="signature-btn signature-btn-clear" 
                                id="${widgetId}-clear">
                            🗑️ Clear
                        </button>
                        <button type="button" class="signature-btn signature-btn-done" 
                                id="${widgetId}-done">
                            ✅ Done
                        </button>
                    </div>
                </div>
            """.trimIndent()
        }
        
        parentElement.appendChild(container!!)
        
        // Initialize SignaturePad with options
        val padOptions = SignaturePadOptions(
            width = 400,
            height = 200,
            penColor = "#000000",
            penWidth = 3.0,
            minWidth = 0.5,
            backgroundColor = "#ffffff",
            borderColor = "#cccccc",
            borderWidth = 1,
            borderRadius = 4,
            velocitySensitivity = 0.7,
            usePressure = true
        )
        
        signaturePad = SignaturePad("${widgetId}-pad-container", padOptions)
        signaturePad?.initialize()
        
        // Setup callbacks
        signaturePad?.onBeginStroke = {
            hidePlaceholder(widgetId)
            hasSignature = true
        }
        
        signaturePad?.onClear = {
            showPlaceholder(widgetId)
            hasSignature = false
        }
        
        signaturePad?.onChange = {
            // Optional: auto-save on change
        }
        
        // Setup button event listeners
        setupEventListeners(widgetId)
        
        // Load existing value if any
        if (field.value.isNotEmpty()) {
            loadExistingSignature(field.value)
        }
        
        // Disable controls if protected
        if (field.isProtected) {
            disableControls(widgetId)
        }
    }
    
    private fun setupEventListeners(widgetId: String) {
        // Undo button
        document.getElementById("${widgetId}-undo")?.addEventListener("click", {
            signaturePad?.undo()
            if (signaturePad?.isEmpty() == true) {
                showPlaceholder(widgetId)
                hasSignature = false
            }
        })
        
        // Clear button
        document.getElementById("${widgetId}-clear")?.addEventListener("click", {
            clearSignature(widgetId)
        })
        
        // Done button
        document.getElementById("${widgetId}-done")?.addEventListener("click", {
            saveSignature()
        })
    }
    
    private fun hidePlaceholder(widgetId: String) {
        document.getElementById("${widgetId}-placeholder")?.let {
            (it as HTMLElement).style.display = "none"
        }
    }
    
    private fun showPlaceholder(widgetId: String) {
        document.getElementById("${widgetId}-placeholder")?.let {
            (it as HTMLElement).style.display = "flex"
        }
    }
    
    private fun clearSignature(widgetId: String) {
        signaturePad?.clear()
        hasSignature = false
        showPlaceholder(widgetId)
        onValueChanged?.invoke("")
    }
    
    private fun saveSignature() {
        if (!hasSignature || signaturePad?.isEmpty() == true) {
            onNextField?.invoke()
            return
        }
        
        // Get cropped signature for better storage efficiency
        val dataUrl = signaturePad?.getCroppedDataUrl(padding = 10) 
            ?: signaturePad?.toPngDataUrl() 
            ?: ""
        onValueChanged?.invoke(dataUrl)
        onNextField?.invoke()
    }
    
    private fun loadExistingSignature(value: String) {
        if (!value.startsWith("data:")) return
        
        signaturePad?.fromDataUrl(value)
        hasSignature = true
        hidePlaceholder("signature-widget-${field.name}")
    }
    
    private fun disableControls(widgetId: String) {
        // Disable signature pad
        signaturePad?.setEnabled(false)
        
        document.getElementById("${widgetId}-controls")?.let {
            it.querySelectorAll("button").asList().forEach { btn ->
                (btn as? HTMLButtonElement)?.disabled = true
            }
        }
    }
    
    override fun getValue(): String {
        return if (hasSignature && signaturePad?.isEmpty() == false) {
            signaturePad?.getCroppedDataUrl(padding = 10) ?: ""
        } else ""
    }
    
    override fun setValue(value: String) {
        if (value.isNotEmpty()) {
            loadExistingSignature(value)
        } else {
            clearSignature("signature-widget-${field.name}")
        }
    }
    
    override fun focus() {
        // Focus on the canvas container
        document.getElementById("signature-widget-${field.name}-pad-container")?.let {
            (it as? HTMLElement)?.focus()
        }
    }
    
    override fun destroy() {
        signaturePad?.destroy()
        signaturePad = null
        super.destroy()
    }
}
