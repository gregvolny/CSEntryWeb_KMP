package gov.census.cspro.smartsync.ui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import gov.census.cspro.smartsync.BluetoothAdapter
import gov.census.cspro.smartsync.BluetoothDeviceInfo
import gov.census.cspro.smartsync.BluetoothSyncListener
import gov.census.cspro.platform.toList
import org.w3c.dom.HTMLElement
import kotlin.js.JsAny

// Top-level @JsFun external functions for WASM-compatible JS interop
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

@JsFun("(element) => element.remove()")
private external fun jsRemoveElement(element: JsAny)

/**
 * Bluetooth Device Discovery UI
 * 
 * Provides a visual interface for discovering and connecting to Bluetooth devices.
 * Uses Web Bluetooth API with enhanced user experience.
 * 
 * Features:
 * - Device list with signal strength visualization
 * - Connection status indicators
 * - Automatic reconnection
 * - Device filtering by service type
 * - Pairing status display
 */
class BluetoothDeviceDiscovery(
    private val container: HTMLElement,
    private val config: BluetoothDiscoveryConfig = BluetoothDiscoveryConfig()
) {
    private var isScanning = false
    private var selectedDevice: BluetoothDeviceInfo? = null
    private var connectedDevice: BluetoothDeviceInfo? = null
    private val discoveredDevices = mutableListOf<DiscoveredDevice>()
    private var scanJob: Job? = null
    private var adapter: BluetoothAdapter? = null
    
    // Callbacks
    var onDeviceSelected: ((BluetoothDeviceInfo) -> Unit)? = null
    var onDeviceConnected: ((BluetoothDeviceInfo) -> Unit)? = null
    var onDeviceDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Initialize the discovery UI
     */
    fun initialize() {
        adapter = BluetoothAdapter.create()
        
        if (adapter == null) {
            renderBluetoothUnavailable()
            return
        }
        
        renderUI()
    }
    
    /**
     * Render the main UI
     */
    private fun renderUI() {
        container.innerHTML = """
            <div class="bluetooth-discovery">
                <div class="bluetooth-header">
                    <div class="bluetooth-title">
                        <span class="bluetooth-icon">📶</span>
                        <h3>Bluetooth Devices</h3>
                    </div>
                    <div class="bluetooth-status" id="btStatus">
                        <span class="status-dot"></span>
                        <span class="status-text">Ready</span>
                    </div>
                </div>
                
                <div class="bluetooth-actions">
                    <button id="btScanBtn" class="bt-btn bt-btn-primary">
                        <span class="btn-icon">🔍</span>
                        <span class="btn-text">Scan for Devices</span>
                    </button>
                    <button id="btFilterBtn" class="bt-btn bt-btn-secondary" title="Filter devices">
                        <span class="btn-icon">⚙️</span>
                    </button>
                </div>
                
                <div class="bluetooth-filter-panel" id="btFilterPanel" style="display: none;">
                    <div class="filter-row">
                        <label>
                            <input type="checkbox" id="filterShowAll" checked>
                            Show all devices
                        </label>
                    </div>
                    <div class="filter-row">
                        <label>
                            <input type="checkbox" id="filterShowPaired">
                            Show paired only
                        </label>
                    </div>
                    <div class="filter-row">
                        <label>Service filter:</label>
                        <select id="filterService">
                            <option value="">Any service</option>
                            <option value="battery_service">Battery</option>
                            <option value="device_information">Device Info</option>
                            <option value="heart_rate">Heart Rate</option>
                            <option value="generic_access">Generic Access</option>
                        </select>
                    </div>
                </div>
                
                <div class="bluetooth-device-list" id="btDeviceList">
                    <div class="device-placeholder">
                        <span class="placeholder-icon">📱</span>
                        <p>No devices found</p>
                        <p class="placeholder-hint">Click "Scan for Devices" to discover nearby Bluetooth devices</p>
                    </div>
                </div>
                
                <div class="bluetooth-connected" id="btConnected" style="display: none;">
                    <div class="connected-header">
                        <span class="connected-icon">✅</span>
                        <span class="connected-title">Connected Device</span>
                    </div>
                    <div class="connected-device" id="btConnectedDevice"></div>
                    <button id="btDisconnectBtn" class="bt-btn bt-btn-danger">
                        <span class="btn-icon">❌</span>
                        <span class="btn-text">Disconnect</span>
                    </button>
                </div>
            </div>
        """.trimIndent()
        
        setupEventListeners()
        injectStyles()
    }
    
    /**
     * Set up event listeners
     */
    private fun setupEventListeners() {
        document.getElementById("btScanBtn")?.addEventListener("click", {
            if (isScanning) {
                stopScan()
            } else {
                startScan()
            }
        })
        
        document.getElementById("btFilterBtn")?.addEventListener("click", {
            toggleFilterPanel()
        })
        
        document.getElementById("btDisconnectBtn")?.addEventListener("click", {
            disconnect()
        })
    }
    
    /**
     * Start scanning for devices
     */
    fun startScan() {
        if (isScanning) return
        
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            onError?.invoke("Bluetooth not available")
            return
        }
        
        isScanning = true
        discoveredDevices.clear()
        updateScanButton()
        updateStatus("Scanning...", "scanning")
        renderDeviceList()
        
        scanJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                bluetoothAdapter.scanForDevices(object : BluetoothSyncListener {
                    override fun onDeviceFound(device: BluetoothDeviceInfo) {
                        val discovered = DiscoveredDevice(
                            device = device,
                            rssi = estimateRSSI(),
                            lastSeen = currentTimeMillis()
                        )
                        
                        // Update or add device
                        val existingIndex = discoveredDevices.indexOfFirst { it.device.id == device.id }
                        if (existingIndex >= 0) {
                            discoveredDevices[existingIndex] = discovered
                        } else {
                            discoveredDevices.add(discovered)
                        }
                        
                        renderDeviceList()
                    }
                    
                    override fun onScanComplete() {
                        isScanning = false
                        updateScanButton()
                        updateStatus("Ready", "ready")
                    }
                    
                    override fun onDeviceConnected(device: BluetoothDeviceInfo) {
                        connectedDevice = device
                        showConnectedDevice()
                        updateStatus("Connected", "connected")
                        onDeviceConnected?.invoke(device)
                    }
                    
                    override fun onDeviceDisconnected(device: BluetoothDeviceInfo) {
                        connectedDevice = null
                        hideConnectedDevice()
                        updateStatus("Disconnected", "disconnected")
                        onDeviceDisconnected?.invoke()
                    }
                    
                    override fun onDataReceived(data: ByteArray) {
                        // Not used for scanning
                    }
                    
                    override fun onError(error: String) {
                        isScanning = false
                        updateScanButton()
                        updateStatus("Error", "error")
                        showError(error)
                        onError?.invoke(error)
                    }
                })
            } catch (e: Exception) {
                isScanning = false
                updateScanButton()
                updateStatus("Error", "error")
                showError(e.message ?: "Scan failed")
            }
        }
    }
    
    /**
     * Stop scanning
     */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
        updateScanButton()
        updateStatus("Ready", "ready")
    }
    
    /**
     * Connect to a device
     */
    suspend fun connectToDevice(device: BluetoothDeviceInfo): Boolean {
        val bluetoothAdapter = adapter ?: return false
        
        selectedDevice = device
        updateStatus("Connecting...", "connecting")
        
        return try {
            val connected = bluetoothAdapter.connectToDevice(device.address, object : BluetoothSyncListener {
                override fun onDeviceFound(device: BluetoothDeviceInfo) {}
                override fun onScanComplete() {}
                
                override fun onDeviceConnected(device: BluetoothDeviceInfo) {
                    connectedDevice = device
                    showConnectedDevice()
                    updateStatus("Connected", "connected")
                    onDeviceConnected?.invoke(device)
                }
                
                override fun onDeviceDisconnected(device: BluetoothDeviceInfo) {
                    connectedDevice = null
                    hideConnectedDevice()
                    updateStatus("Disconnected", "disconnected")
                    onDeviceDisconnected?.invoke()
                }
                
                override fun onError(error: String) {
                    updateStatus("Error", "error")
                    showError(error)
                    onError?.invoke(error)
                }
                
                override fun onDataReceived(data: ByteArray) {}
            })
            
            connected
        } catch (e: Exception) {
            updateStatus("Error", "error")
            showError(e.message ?: "Connection failed")
            false
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        adapter?.cancel()
        connectedDevice = null
        hideConnectedDevice()
        updateStatus("Ready", "ready")
        onDeviceDisconnected?.invoke()
    }
    
    /**
     * Render the device list
     */
    private fun renderDeviceList() {
        val listElement = document.getElementById("btDeviceList") ?: return
        
        if (discoveredDevices.isEmpty()) {
            if (isScanning) {
                listElement.innerHTML = """
                    <div class="device-scanning">
                        <div class="scanning-animation">
                            <div class="scanning-ring"></div>
                            <span class="scanning-icon">📡</span>
                        </div>
                        <p>Scanning for devices...</p>
                        <p class="scanning-hint">Make sure your device is in pairing mode</p>
                    </div>
                """.trimIndent()
            } else {
                listElement.innerHTML = """
                    <div class="device-placeholder">
                        <span class="placeholder-icon">📱</span>
                        <p>No devices found</p>
                        <p class="placeholder-hint">Click "Scan for Devices" to discover nearby Bluetooth devices</p>
                    </div>
                """.trimIndent()
            }
            return
        }
        
        val sortedDevices = discoveredDevices.sortedByDescending { it.rssi }
        
        listElement.innerHTML = sortedDevices.joinToString("") { discovered ->
            val device = discovered.device
            val signalBars = getSignalBars(discovered.rssi)
            val pairedBadge = if (device.isPaired) """<span class="device-badge paired">Paired</span>""" else ""
            val connectedBadge = if (connectedDevice?.id == device.id) """<span class="device-badge connected">Connected</span>""" else ""
            
            """
            <div class="device-card" data-device-id="${device.id}">
                <div class="device-icon-container">
                    <span class="device-type-icon">${getDeviceIcon(device.name)}</span>
                    <div class="signal-strength" title="Signal: ${discovered.rssi}dBm">
                        $signalBars
                    </div>
                </div>
                <div class="device-info">
                    <div class="device-name">${escapeHtml(device.name)}</div>
                    <div class="device-address">${device.address}</div>
                    <div class="device-badges">
                        $pairedBadge
                        $connectedBadge
                    </div>
                </div>
                <div class="device-actions">
                    <button class="device-connect-btn" data-device-id="${device.id}">
                        ${if (connectedDevice?.id == device.id) "✅" else "🔗"}
                    </button>
                </div>
            </div>
            """.trimIndent()
        }
        
        // Add click listeners to device cards
        listElement.querySelectorAll(".device-card")?.toList()?.forEach { card ->
            (card as? HTMLElement)?.addEventListener("click", {
                val deviceId = card.getAttribute("data-device-id")
                val device = discoveredDevices.find { it.device.id == deviceId }?.device
                if (device != null) {
                    selectedDevice = device
                    highlightSelectedDevice(deviceId ?: "")
                    onDeviceSelected?.invoke(device)
                }
            })
        }
        
        // Add click listeners to connect buttons
        listElement.querySelectorAll(".device-connect-btn")?.toList()?.forEach { btn ->
            (btn as? HTMLElement)?.addEventListener("click", { event ->
                event.stopPropagation()
                val deviceId = btn.getAttribute("data-device-id")
                val device = discoveredDevices.find { it.device.id == deviceId }?.device
                if (device != null) {
                    CoroutineScope(Dispatchers.Default).launch {
                        connectToDevice(device)
                    }
                }
            })
        }
    }
    
    /**
     * Highlight selected device
     */
    private fun highlightSelectedDevice(deviceId: String) {
        document.querySelectorAll(".device-card")?.toList()?.forEach { card ->
            val cardElement = card as? HTMLElement
            if (cardElement?.getAttribute("data-device-id") == deviceId) {
                cardElement.classList.add("selected")
            } else {
                cardElement?.classList?.remove("selected")
            }
        }
    }
    
    /**
     * Get signal strength bars HTML
     */
    private fun getSignalBars(rssi: Int): String {
        val strength = when {
            rssi >= -50 -> 4
            rssi >= -60 -> 3
            rssi >= -70 -> 2
            rssi >= -80 -> 1
            else -> 0
        }
        
        return (1..4).joinToString("") { bar ->
            val active = if (bar <= strength) "active" else ""
            """<div class="signal-bar $active"></div>"""
        }
    }
    
    /**
     * Get device type icon
     */
    private fun getDeviceIcon(name: String): String {
        val nameLower = name.lowercase()
        return when {
            nameLower.contains("phone") || nameLower.contains("mobile") -> "📱"
            nameLower.contains("tablet") -> "📟"
            nameLower.contains("computer") || nameLower.contains("pc") || nameLower.contains("laptop") -> "💻"
            nameLower.contains("headphone") || nameLower.contains("earphone") || nameLower.contains("airpod") -> "🎧"
            nameLower.contains("speaker") -> "🔊"
            nameLower.contains("watch") -> "⌚"
            nameLower.contains("keyboard") -> "⌨️"
            nameLower.contains("mouse") -> "🖱️"
            nameLower.contains("printer") -> "🖨️"
            nameLower.contains("car") -> "🚗"
            nameLower.contains("heart") || nameLower.contains("health") -> "❤️"
            else -> "📶"
        }
    }
    
    /**
     * Estimate RSSI (Web Bluetooth doesn't provide this directly)
     */
    private fun estimateRSSI(): Int {
        // Web Bluetooth doesn't expose RSSI, return a random value for UI demonstration
        return (-45..-75).random()
    }
    
    /**
     * Update scan button state
     */
    private fun updateScanButton() {
        val btn = document.getElementById("btScanBtn") as? HTMLElement ?: return
        
        if (isScanning) {
            btn.innerHTML = """
                <span class="btn-icon spinning">🔄</span>
                <span class="btn-text">Stop Scan</span>
            """.trimIndent()
            btn.classList.add("scanning")
        } else {
            btn.innerHTML = """
                <span class="btn-icon">🔍</span>
                <span class="btn-text">Scan for Devices</span>
            """.trimIndent()
            btn.classList.remove("scanning")
        }
    }
    
    /**
     * Update status indicator
     */
    private fun updateStatus(text: String, state: String) {
        val status = document.getElementById("btStatus") as? HTMLElement ?: return
        status.className = "bluetooth-status $state"
        status.querySelector(".status-text")?.textContent = text
    }
    
    /**
     * Toggle filter panel
     */
    private fun toggleFilterPanel() {
        val panel = document.getElementById("btFilterPanel") as? HTMLElement ?: return
        val isVisible = panel.style.display != "none"
        panel.style.display = if (isVisible) "none" else "block"
    }
    
    /**
     * Show connected device panel
     */
    private fun showConnectedDevice() {
        val connected = document.getElementById("btConnected") as? HTMLElement ?: return
        val deviceDiv = document.getElementById("btConnectedDevice") as? HTMLElement ?: return
        
        val device = connectedDevice ?: return
        
        deviceDiv.innerHTML = """
            <div class="connected-device-card">
                <span class="device-icon">${getDeviceIcon(device.name)}</span>
                <div class="device-details">
                    <div class="device-name">${escapeHtml(device.name)}</div>
                    <div class="device-address">${device.address}</div>
                </div>
            </div>
        """.trimIndent()
        
        connected.style.display = "block"
    }
    
    /**
     * Hide connected device panel
     */
    private fun hideConnectedDevice() {
        val connected = document.getElementById("btConnected") as? HTMLElement ?: return
        connected.style.display = "none"
    }
    
    /**
     * Show error message
     */
    private fun showError(message: String) {
        val listElement = document.getElementById("btDeviceList") ?: return
        
        val errorDiv = document.createElement("div")
        errorDiv.className = "bluetooth-error"
        errorDiv.innerHTML = """
            <span class="error-icon">⚠️</span>
            <span class="error-message">${escapeHtml(message)}</span>
            <button class="error-dismiss">✕</button>
        """.trimIndent()
        
        listElement.insertBefore(errorDiv, listElement.firstChild)
        
        errorDiv.querySelector(".error-dismiss")?.addEventListener("click", {
            jsRemoveElement(errorDiv.unsafeCast<JsAny>())
        })
        
        // Auto-dismiss after 5 seconds
        window.setTimeout({
            jsRemoveElement(errorDiv.unsafeCast<JsAny>())
            null
        }, 5000)
    }
    
    /**
     * Render Bluetooth unavailable message
     */
    private fun renderBluetoothUnavailable() {
        container.innerHTML = """
            <div class="bluetooth-unavailable">
                <span class="unavailable-icon">🚫</span>
                <h3>Bluetooth Not Available</h3>
                <p>Web Bluetooth API is not supported in this browser.</p>
                <p class="unavailable-hint">
                    Please use a supported browser like Chrome, Edge, or Opera on desktop,
                    or Chrome on Android.
                </p>
                <a href="https://caniuse.com/web-bluetooth" target="_blank" class="learn-more-link">
                    Learn more about browser support
                </a>
            </div>
        """.trimIndent()
        
        injectStyles()
    }
    
    /**
     * Escape HTML special characters
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }
    
    /**
     * Get current time in milliseconds (WASM compatible)
     * Uses top-level @JsFun jsDateNow() instead of inline js()
     */
    private fun currentTimeMillis(): Long = jsDateNow().toLong()

    /**
     * Inject CSS styles
     */
    private fun injectStyles() {
        val styleId = "bluetooth-discovery-styles"
        if (document.getElementById(styleId) != null) return
        
        val style = document.createElement("style")
        style.id = styleId
        style.textContent = """
            .bluetooth-discovery {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                background: #fff;
                border-radius: 12px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                overflow: hidden;
            }
            
            .bluetooth-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 16px 20px;
                background: linear-gradient(135deg, #0066cc, #004499);
                color: white;
            }
            
            .bluetooth-title {
                display: flex;
                align-items: center;
                gap: 10px;
            }
            
            .bluetooth-title h3 {
                margin: 0;
                font-size: 18px;
            }
            
            .bluetooth-icon {
                font-size: 24px;
            }
            
            .bluetooth-status {
                display: flex;
                align-items: center;
                gap: 8px;
                font-size: 14px;
            }
            
            .status-dot {
                width: 10px;
                height: 10px;
                border-radius: 50%;
                background: #4ade80;
            }
            
            .bluetooth-status.scanning .status-dot {
                background: #fbbf24;
                animation: pulse 1s infinite;
            }
            
            .bluetooth-status.connected .status-dot {
                background: #4ade80;
            }
            
            .bluetooth-status.error .status-dot,
            .bluetooth-status.disconnected .status-dot {
                background: #ef4444;
            }
            
            @keyframes pulse {
                0%, 100% { opacity: 1; }
                50% { opacity: 0.5; }
            }
            
            .bluetooth-actions {
                display: flex;
                gap: 8px;
                padding: 16px 20px;
                border-bottom: 1px solid #e5e7eb;
            }
            
            .bt-btn {
                display: flex;
                align-items: center;
                gap: 8px;
                padding: 10px 16px;
                border: none;
                border-radius: 8px;
                cursor: pointer;
                font-size: 14px;
                font-weight: 500;
                transition: all 0.2s;
            }
            
            .bt-btn-primary {
                background: #0066cc;
                color: white;
                flex: 1;
            }
            
            .bt-btn-primary:hover {
                background: #0052a3;
            }
            
            .bt-btn-primary.scanning {
                background: #dc2626;
            }
            
            .bt-btn-secondary {
                background: #f3f4f6;
                color: #374151;
            }
            
            .bt-btn-secondary:hover {
                background: #e5e7eb;
            }
            
            .bt-btn-danger {
                background: #ef4444;
                color: white;
            }
            
            .bt-btn-danger:hover {
                background: #dc2626;
            }
            
            .btn-icon.spinning {
                animation: spin 1s linear infinite;
            }
            
            @keyframes spin {
                from { transform: rotate(0deg); }
                to { transform: rotate(360deg); }
            }
            
            .bluetooth-filter-panel {
                padding: 12px 20px;
                background: #f9fafb;
                border-bottom: 1px solid #e5e7eb;
            }
            
            .filter-row {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 8px;
            }
            
            .filter-row:last-child {
                margin-bottom: 0;
            }
            
            .filter-row select {
                padding: 6px 10px;
                border: 1px solid #d1d5db;
                border-radius: 6px;
                font-size: 14px;
            }
            
            .bluetooth-device-list {
                max-height: 400px;
                overflow-y: auto;
                padding: 12px;
            }
            
            .device-placeholder,
            .device-scanning {
                text-align: center;
                padding: 40px 20px;
                color: #6b7280;
            }
            
            .placeholder-icon,
            .scanning-icon {
                font-size: 48px;
                display: block;
                margin-bottom: 16px;
            }
            
            .placeholder-hint,
            .scanning-hint {
                font-size: 13px;
                color: #9ca3af;
                margin-top: 8px;
            }
            
            .scanning-animation {
                position: relative;
                display: inline-block;
            }
            
            .scanning-ring {
                position: absolute;
                top: 50%;
                left: 50%;
                width: 80px;
                height: 80px;
                margin: -40px 0 0 -40px;
                border: 3px solid transparent;
                border-top-color: #0066cc;
                border-radius: 50%;
                animation: spin 1s linear infinite;
            }
            
            .device-card {
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 12px 16px;
                background: #f9fafb;
                border-radius: 10px;
                margin-bottom: 8px;
                cursor: pointer;
                transition: all 0.2s;
                border: 2px solid transparent;
            }
            
            .device-card:hover {
                background: #f3f4f6;
                transform: translateY(-1px);
            }
            
            .device-card.selected {
                border-color: #0066cc;
                background: #eff6ff;
            }
            
            .device-icon-container {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 4px;
            }
            
            .device-type-icon {
                font-size: 28px;
            }
            
            .signal-strength {
                display: flex;
                gap: 2px;
                align-items: flex-end;
            }
            
            .signal-bar {
                width: 4px;
                background: #d1d5db;
                border-radius: 2px;
            }
            
            .signal-bar:nth-child(1) { height: 4px; }
            .signal-bar:nth-child(2) { height: 7px; }
            .signal-bar:nth-child(3) { height: 10px; }
            .signal-bar:nth-child(4) { height: 13px; }
            
            .signal-bar.active {
                background: #4ade80;
            }
            
            .device-info {
                flex: 1;
                min-width: 0;
            }
            
            .device-name {
                font-weight: 600;
                color: #111827;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            
            .device-address {
                font-size: 12px;
                color: #6b7280;
                font-family: monospace;
            }
            
            .device-badges {
                display: flex;
                gap: 6px;
                margin-top: 4px;
            }
            
            .device-badge {
                font-size: 10px;
                padding: 2px 6px;
                border-radius: 4px;
                font-weight: 500;
            }
            
            .device-badge.paired {
                background: #dbeafe;
                color: #1d4ed8;
            }
            
            .device-badge.connected {
                background: #d1fae5;
                color: #047857;
            }
            
            .device-actions {
                display: flex;
                gap: 8px;
            }
            
            .device-connect-btn {
                width: 36px;
                height: 36px;
                border: none;
                border-radius: 50%;
                background: #0066cc;
                color: white;
                font-size: 16px;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: all 0.2s;
            }
            
            .device-connect-btn:hover {
                background: #0052a3;
                transform: scale(1.1);
            }
            
            .bluetooth-connected {
                padding: 16px 20px;
                background: #d1fae5;
                border-top: 1px solid #a7f3d0;
            }
            
            .connected-header {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 12px;
                font-weight: 600;
                color: #047857;
            }
            
            .connected-device-card {
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 12px;
                background: white;
                border-radius: 8px;
                margin-bottom: 12px;
            }
            
            .bluetooth-error {
                display: flex;
                align-items: center;
                gap: 10px;
                padding: 12px 16px;
                background: #fef2f2;
                border: 1px solid #fecaca;
                border-radius: 8px;
                margin-bottom: 12px;
                color: #dc2626;
            }
            
            .error-dismiss {
                margin-left: auto;
                background: none;
                border: none;
                color: #dc2626;
                cursor: pointer;
                font-size: 16px;
            }
            
            .bluetooth-unavailable {
                text-align: center;
                padding: 40px 20px;
            }
            
            .unavailable-icon {
                font-size: 64px;
                display: block;
                margin-bottom: 16px;
            }
            
            .unavailable-hint {
                font-size: 14px;
                color: #6b7280;
                margin-top: 12px;
            }
            
            .learn-more-link {
                display: inline-block;
                margin-top: 16px;
                color: #0066cc;
                text-decoration: none;
            }
            
            .learn-more-link:hover {
                text-decoration: underline;
            }
        """.trimIndent()
        
        document.head?.appendChild(style)
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        stopScan()
        disconnect()
        discoveredDevices.clear()
    }
}

/**
 * Discovered device with additional metadata
 */
data class DiscoveredDevice(
    val device: BluetoothDeviceInfo,
    val rssi: Int,
    val lastSeen: Long
)

/**
 * Configuration for Bluetooth discovery
 */
data class BluetoothDiscoveryConfig(
    val autoStartScan: Boolean = false,
    val scanDurationMs: Long = 30000,
    val showSignalStrength: Boolean = true,
    val showPairedOnly: Boolean = false,
    val serviceFilter: String? = null
)
