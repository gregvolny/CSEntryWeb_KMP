package gov.census.cspro.platform

import kotlin.js.Promise

/**
 * External JavaScript type definitions for Kotlin/Wasm interop.
 * 
 * Kotlin/Wasm requires all JavaScript interop to use external interfaces,
 * primitives, strings, or function types. These declarations provide type-safe
 * bridges to JavaScript APIs.
 */

// ============================================================================
// File System Access API
// ============================================================================

external interface FileSystemHandle : kotlin.js.JsAny {
    val kind: String  // "file" or "directory"
    val name: String
}

external interface FileSystemFileHandle : FileSystemHandle {
    fun getFile(): Promise<kotlin.js.JsAny?>
    fun createWritable(): Promise<kotlin.js.JsAny?>
}

external interface FileSystemDirectoryHandle : FileSystemHandle {
    fun getFileHandle(name: String, options: FileSystemGetFileOptions = definedExternally): Promise<kotlin.js.JsAny?>
    fun getDirectoryHandle(name: String, options: FileSystemGetFileOptions = definedExternally): Promise<kotlin.js.JsAny?>
    fun removeEntry(name: String, options: FileSystemRemoveOptions = definedExternally): Promise<kotlin.js.JsAny?>
    fun resolve(possibleDescendant: FileSystemHandle): Promise<kotlin.js.JsArray<kotlin.js.JsString>>
    fun values(): JsIterator<FileSystemHandle>
}

external interface FileSystemGetFileOptions {
    var create: Boolean?
}

external interface FileSystemRemoveOptions {
    var recursive: Boolean?
}

external interface FileSystemWritableFileStream {
    fun write(data: kotlin.js.JsAny): Promise<kotlin.js.JsAny?>
    fun close(): Promise<kotlin.js.JsAny?>
    fun seek(position: Double): Promise<kotlin.js.JsAny?>
    fun truncate(size: Double): Promise<kotlin.js.JsAny?>
}

external interface JsFile : Blob {
    val name: String
    val lastModified: Double
    override fun text(): Promise<kotlin.js.JsString>
    override fun arrayBuffer(): Promise<kotlin.js.JsAny?>
}

external interface Blob {
    val size: Double
    val type: String
    fun slice(start: Double = definedExternally, end: Double = definedExternally, contentType: String = definedExternally): Blob
    fun text(): Promise<kotlin.js.JsString>
    fun arrayBuffer(): Promise<kotlin.js.JsAny?>
}

external interface JsArrayBuffer {
    val byteLength: Int
}

// ============================================================================
// Navigator APIs
// ============================================================================

external interface NavigatorGeolocation {
    fun getCurrentPosition(
        successCallback: (GeolocationPosition) -> Unit,
        errorCallback: ((GeolocationPositionError) -> Unit)? = definedExternally,
        options: GeolocationOptions? = definedExternally
    )
    fun watchPosition(
        successCallback: (GeolocationPosition) -> Unit,
        errorCallback: ((GeolocationPositionError) -> Unit)? = definedExternally,
        options: GeolocationOptions? = definedExternally
    ): Int
    fun clearWatch(watchId: Int)
}

external interface GeolocationOptions {
    var enableHighAccuracy: Boolean?
    var timeout: Double?
    var maximumAge: Double?
}

external interface GeolocationPosition {
    val coords: GeolocationCoordinates
    val timestamp: Double
}

external interface GeolocationCoordinates {
    val latitude: Double
    val longitude: Double
    val altitude: Double?
    val accuracy: Double
    val altitudeAccuracy: Double?
    val heading: Double?
    val speed: Double?
}

external interface GeolocationPositionError {
    val code: Short
    val message: String
}

external interface NavigatorBluetooth {
    fun requestDevice(options: BluetoothRequestDeviceOptions): Promise<kotlin.js.JsAny?>
    fun getAvailability(): Promise<kotlin.js.JsAny?>
}

external interface BluetoothRequestDeviceOptions {
    var filters: kotlin.js.JsArray<BluetoothLEScanFilter>?
    var optionalServices: kotlin.js.JsArray<kotlin.js.JsString>?
    var acceptAllDevices: Boolean?
}

external interface BluetoothLEScanFilter : kotlin.js.JsAny {
    var services: kotlin.js.JsArray<kotlin.js.JsString>?
    var name: String?
    var namePrefix: String?
}

external interface BluetoothRemoteGATTDevice {
    val id: String
    val name: String?
    val gatt: BluetoothRemoteGATTServer?
    fun watchAdvertisements(): Promise<kotlin.js.JsAny?>
    fun unwatchAdvertisements()
}

external interface BluetoothRemoteGATTServer {
    val connected: Boolean
    val device: BluetoothRemoteGATTDevice
    fun connect(): Promise<kotlin.js.JsAny?>
    fun disconnect()
    fun getPrimaryService(service: String): Promise<kotlin.js.JsAny?>
}

external interface BluetoothRemoteGATTService {
    val device: BluetoothRemoteGATTDevice
    val uuid: String
    fun getCharacteristic(characteristic: String): Promise<kotlin.js.JsAny?>
}

external interface BluetoothRemoteGATTCharacteristic {
    val service: BluetoothRemoteGATTService
    val uuid: String
    val value: JsDataView?
    fun readValue(): Promise<kotlin.js.JsAny?>
    fun writeValue(value: JsArrayBuffer): Promise<kotlin.js.JsAny?>
}

external interface JsDataView {
    val buffer: JsArrayBuffer
    val byteLength: Int
    val byteOffset: Int
    fun getUint8(byteOffset: Int): Byte
}

// ============================================================================
// Storage APIs
// ============================================================================

external interface StorageManager {
    fun getDirectory(): Promise<kotlin.js.JsAny?>
    fun estimate(): Promise<kotlin.js.JsAny?>
}

external interface StorageEstimate {
    val usage: Double?
    val quota: Double?
}

// ============================================================================
// DOM & Window APIs
// ============================================================================

external interface JsWindow {
    val console: Console
    val navigator: JsNavigator
    val location: Location
    val document: Document
}

external interface Console {
    fun log(vararg args: kotlin.js.JsAny?)
    fun error(vararg args: kotlin.js.JsAny?)
    fun warn(vararg args: kotlin.js.JsAny?)
    fun info(vararg args: kotlin.js.JsAny?)
    fun debug(vararg args: kotlin.js.JsAny?)
}

external interface JsNavigator : kotlin.js.JsAny {
    val userAgent: String
    val geolocation: NavigatorGeolocation
    val bluetooth: NavigatorBluetooth?
    val storage: StorageManager?
}

external interface Location {
    val href: String
    val protocol: String
    val host: String
    val hostname: String
    val port: String
    val pathname: String
    val search: String
    val hash: String
    fun reload()
}

external interface Document {
    fun getElementById(id: String): HTMLElement?
    fun querySelector(selector: String): HTMLElement?
    fun querySelectorAll(selector: String): NodeList
}

external interface HTMLElement : Element {
    var innerHTML: String
    var textContent: String?
    val style: CSSStyleDeclaration
    val offsetWidth: Double
    val offsetHeight: Double
    fun click()
    fun focus()
}

external interface Element {
    val tagName: String
    val id: String
    val className: String
    fun getAttribute(name: String): String?
    fun setAttribute(name: String, value: String)
    fun removeAttribute(name: String)
    fun appendChild(child: Element): Element
    fun removeChild(child: Element): Element
}

external interface CSSStyleDeclaration {
    var display: String
    var visibility: String
    var position: String
    var top: String
    var left: String
    var width: String
    var height: String
    var backgroundColor: String
    var color: String
    var fontSize: String
    var fontFamily: String
    var margin: String
    var padding: String
    var border: String
    var opacity: String
    var zIndex: String
}

external interface NodeList {
    val length: Int
    fun item(index: Int): Element?
}

// Extension function to convert NodeList to List
fun NodeList.toList(): List<Element> {
    val list = mutableListOf<Element>()
    for (i in 0 until length) {
        item(i)?.let { list.add(it) }
    }
    return list
}

// Extension function for W3C DOM NodeList
fun org.w3c.dom.NodeList.toList(): List<org.w3c.dom.Element> {
    val list = mutableListOf<org.w3c.dom.Element>()
    for (i in 0 until length) {
        val node = item(i)
        if (node is org.w3c.dom.Element) {
            list.add(node)
        }
    }
    return list
}

// ============================================================================
// JavaScript Standard Types
// ============================================================================

external interface JsDate {
    fun getTime(): Double
    fun getFullYear(): Int
    fun getMonth(): Int
    fun getDate(): Int
    fun getHours(): Int
    fun getMinutes(): Int
    fun getSeconds(): Int
    fun getMilliseconds(): Int
    fun toISOString(): String
}

// ============================================================================
// JavaScript Standard Types - Use kotlin.js types directly
// ============================================================================

// Note: kotlin.js.JsAny, kotlin.js.JsArray, kotlin.js.JsString already exist
// We just provide type aliases and additional external interfaces as needed

external interface JsIterator<T : kotlin.js.JsAny?> {
    fun next(): JsIteratorResult<T>
}

external interface JsIteratorResult<T : kotlin.js.JsAny?> {
    val done: Boolean
    val value: T?
}

// ============================================================================
// Emscripten Module Types
// ============================================================================

external interface EmscriptenModule {
    fun ccall(name: String, returnType: String, argTypes: kotlin.js.JsArray<kotlin.js.JsString>, args: kotlin.js.JsArray<kotlin.js.JsAny>): kotlin.js.JsAny?
    fun cwrap(name: String, returnType: String, argTypes: kotlin.js.JsArray<kotlin.js.JsString>): kotlin.js.JsAny
    val FS: gov.census.cspro.platform.EmscriptenFS
    val ERRNO_CODES: EmscriptenErrno
}

// EmscriptenFS is defined in CSProEngineModule.kt

external interface EmscriptenErrno {
    val ENOENT: Int
    val EEXIST: Int
    val EISDIR: Int
    val ENOTDIR: Int
}

// ============================================================================
// Utility Types
// ============================================================================

// Top-level @JsFun helpers (required by Kotlin/Wasm)
@JsFun("() => ({})")
external fun createEmptyJsObject(): kotlin.js.JsAny

@JsFun("() => ([])")
external fun createEmptyJsArray(): kotlin.js.JsAny

@JsFun("() => new Date().toLocaleTimeString()")
external fun jsDateTimeString(): kotlin.js.JsString

