# CSPro WASM Interface Functions Report

## Overview

This document provides a comprehensive reference of all CSPro logic functions, Action Invoker methods, and UI interactions implemented in the WebAssembly (WASM) interface layer. The implementation bridges the C++ CSPro engine with the Kotlin/JS web frontend.

**Date:** January 14, 2026  
**Files Covered:**
- `wasm-engine/src/WASM/WasmApplicationInterface.cpp` - C++ WASM implementation
- `src/wasmJsMain/kotlin/gov/census/cspro/engine/events/WasmEventBridge.kt` - Kotlin/JS event bridge

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CSPro Logic Functions                        │
│            (errmsg, prompt, accept, show, etc.)                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│              WasmApplicationInterface (C++)                     │
│         BaseApplicationInterface implementation                 │
│                                                                 │
│  • Dialog functions (ShowModalDialog, ShowMessage, etc.)        │
│  • System functions (ExecPff, ExecSystem)                       │
│  • Progress dialogs (ShowProgressDialog, UpdateProgress)        │
│  • File viewing (View, DisplayHtmlDialogFunctionDlg)            │
└─────────────────────────┬───────────────────────────────────────┘
                          │ JSPI (Emscripten Asyncify)
┌─────────────────────────▼───────────────────────────────────────┐
│               JavaScript Bridge Layer                           │
│                                                                 │
│  window.CSProDialogHandler:                                     │
│    • showDialogAsync()                                          │
│    • showHtmlDialogAsync()                                      │
│    • showModalDialogAsync()                                     │
│    • viewFileAsync()                                            │
│                                                                 │
│  window.CSProEventHandler:                                      │
│    • showProgress() / hideProgress() / updateProgress()         │
│    • refreshPage()                                              │
│    • execPffAsync()                                             │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│              WasmEventBridge (Kotlin/JS)                        │
│         WasmDialogBridge (Kotlin/JS)                            │
│                                                                 │
│  Renders dialogs using HTML/CSS in the browser                  │
│  Handles progress indicators and page refresh                   │
│  Routes execPff to ActivityRouter                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## JSPI Async Functions (C++ → JavaScript)

These functions use Emscripten's `EM_ASYNC_JS` macro with Asyncify to enable async C++/JavaScript interop.

### Dialog Functions

| Function | Purpose | Parameters | Returns |
|----------|---------|------------|---------|
| `jspi_showDialog` | Shows a named dialog (errmsg, choice, etc.) | `dialogName`, `inputDataJson` | `char*` (result JSON) or `0` |
| `jspi_showHtmlDialog` | Shows an HTML-based dialog | `dialogPath`, `inputDataJson`, `displayOptionsJson` | `char*` (result JSON) or `0` |
| `jspi_showModalDialog` | Shows a modal message box | `title`, `message`, `mbType` | `int` (button ID: IDOK=1, IDCANCEL=2, etc.) |
| `jspi_getInputData` | Gets input data for a dialog | `dialogId` | `char*` (JSON data) or `0` |
| `jspi_showMessage` | Shows message with custom buttons | `title`, `message`, `buttonsJson` | `int` (button index) |
| `jspi_showChoiceDialog` | Shows choice/select dialog | `title`, `dataJson` | `int` (selected index, -1 if cancelled) |
| `jspi_showShowDialog` | Shows data display dialog | `heading`, `columnsJson`, `dataJson` | `int` (0) |
| `jspi_showSelcaseDialog` | Shows case selection dialog | `heading`, `columnsJson`, `dataJson` | `int` (selected index, -1 if cancelled) |
| `jspi_editNote` | Shows note edit dialog | `note`, `title`, `caseNote` | `char*` (edited note) |
| `jspi_viewFile` | Displays file in viewer | `content`, `contentType`, `title`, `accessToken` | `int` (1=success, 0=failure) |
| `jspi_execPff` | Executes/launches another PFF file | `pffPath` | `int` (1=success, 0=failure) |

---

## WasmApplicationInterface Methods

### Dialog & UI Methods

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `DisplayCSHtmlDlg` | Internal dialog system | Shows CSPro HTML dialogs (errmsg, accept, etc.) | ✅ Implemented |
| `DisplayHtmlDialogFunctionDlg` | `htmldialog()` | Shows custom HTML dialogs | ✅ Implemented |
| `ShowModalDialog` | `errmsg()`, warnings | Shows modal OK/Cancel dialogs | ✅ Implemented |
| `ShowMessage` | `errmsg()` with buttons | Shows message with custom buttons | ✅ Implemented |
| `ShowChoiceDialog` | `accept()` | Shows choice selection dialog | ✅ Implemented |
| `ShowShowDialog` | `show()` | Displays tabular data | ✅ Implemented |
| `ShowSelcaseDialog` | `selcase()` | Case selection dialog | ✅ Implemented |
| `EditNote` | `editnote()` | Note editing dialog | ✅ Implemented |
| `DisplayErrorMessage` | Error handling | Displays error messages | ✅ Implemented |

### Progress Dialog Methods

| Method | Description | Status |
|--------|-------------|--------|
| `ShowProgressDialog` | Shows progress overlay with message | ✅ Implemented |
| `HideProgressDialog` | Hides progress overlay | ✅ Implemented |
| `UpdateProgressDialog` | Updates progress percentage and message | ✅ Implemented |

### System & Execution Methods

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `ExecPff` | `execpff()` | Launches another PFF application | ✅ Implemented |
| `ExecSystem` | `execsystem()` | Executes system commands | ❌ Not available in browser |
| `ExecSystemApp` | System app launch | Launches system applications | ❌ Not available in browser |
| `RunPffExecutor` | PFF execution | Runs PFF executor | ❌ Not available in browser |

### File & View Methods

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `View` | `view()` | Views files (HTML, text, images) | ✅ Implemented |
| `GetHtmlDialogsDirectory` | Internal | Returns HTML dialogs path | ✅ Implemented |

### GPS & Location Methods

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `GpsOpen` | `gps(open)` | Opens GPS connection | ⚠️ Stub (could use Web Geolocation API) |
| `GpsClose` | `gps(close)` | Closes GPS connection | ⚠️ Stub |
| `GpsRead` | `gps(read)` | Reads GPS coordinates | ⚠️ Stub |
| `GpsReadLast` | `gps(readlast)` | Reads last GPS reading | ⚠️ Stub |
| `GpsReadInteractive` | `gps()` interactive | Interactive GPS reading | ⚠️ Stub |

### Authentication & Credentials

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `ShowLoginDialog` | Login UI | Shows login dialog | ⚠️ Stub |
| `AuthorizeDropbox` | Dropbox OAuth | Dropbox authorization | ⚠️ Stub |
| `StoreCredential` | `setproperty()` | Stores credentials (in memory) | ✅ Implemented (memory only) |
| `RetrieveCredential` | `getproperty()` | Retrieves credentials | ✅ Implemented (memory only) |
| `GetPassword` | Password input | Password input dialog | ⚠️ Stub |

### Bluetooth Methods

| Method | Description | Status |
|--------|-------------|--------|
| `ChooseBluetoothDevice` | Bluetooth device selection | ❌ Not available (limited Web Bluetooth support) |
| `CreateAndroidBluetoothAdapter` | Bluetooth adapter | ❌ Not available |

### Network Methods

| Method | Description | Status |
|--------|-------------|--------|
| `CreateAndroidHttpConnection` | HTTP connection | ⚠️ Stub (use fetch API) |
| `CreateAndroidFtpConnection` | FTP connection | ❌ Not available |
| `IsNetworkConnected` | Network status | ✅ Implemented (assumes online) |

### Device & System Info Methods

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `GetProperty` | `getproperty()` | Gets system properties | ✅ Implemented |
| `SetProperty` | `setproperty()` | Sets system properties | ✅ Implemented |
| `GetMaxDisplaySize` | Display info | Returns screen dimensions | ✅ Implemented (default values) |
| `GetMediaFilenames` | Media access | Gets media file list | ⚠️ Stub |
| `GetUsername` | `username()` | Returns username | ✅ Implemented ("WebUser") |
| `GetDeviceId` | `getdeviceid()` | Returns unique device ID | ✅ Implemented (random UUID) |
| `GetLocaleLanguage` | `getlanguage()` | Returns locale language | ✅ Implemented ("en") |
| `GetUpTime` | System uptime | Returns system uptime | ⚠️ Stub |

### Multimedia Methods

| Method | CSPro Logic Function | Description | Status |
|--------|---------------------|-------------|--------|
| `BarcodeRead` | `barcode()` | Reads barcode | ⚠️ Stub (could use BarcodeDetector API) |
| `AudioPlay` | `audio(play)` | Plays audio file | ⚠️ Stub (could use Web Audio API) |
| `AudioStartRecording` | `audio(record)` | Starts audio recording | ⚠️ Stub (could use MediaRecorder API) |
| `AudioStopRecording` | `audio(stop)` | Stops audio recording | ⚠️ Stub |
| `AudioRecordInteractive` | `audio()` interactive | Interactive audio recording | ⚠️ Stub |
| `CaptureImage` | `image()` | Captures image | ⚠️ Stub (could use getUserMedia) |

### Geometry Methods

| Method | Description | Status |
|--------|-------------|--------|
| `CapturePolygonTrace` | Polygon tracing | ⚠️ Stub |
| `CapturePolygonWalk` | Polygon walking | ⚠️ Stub |

### Paradata Methods

| Method | Description | Status |
|--------|-------------|--------|
| `ParadataDriverManager` | Paradata management | ⚠️ Stub |
| `ParadataDeviceInfoQuery` | Device info for paradata | ✅ Implemented (browser values) |
| `ParadataDeviceStateQuery` | Device state for paradata | ⚠️ Stub |

### Other Methods

| Method | Description | Status |
|--------|-------------|--------|
| `GetObjectTransporter` | ActionInvoker support | ✅ Implemented (WasmObjectTransporter) |
| `RefreshPage` | Page refresh trigger | ✅ Implemented |
| `EngineAbort` | Engine abort | ⚠️ Stub |
| `PartialSave` | Partial case save | ✅ Implemented (returns true) |
| `RunEngineUIProcessor` | Engine UI processing | ✅ Implemented |
| `CreateMapUI` | Map UI creation | ⚠️ Stub |
| `CreateUserbar` | Userbar creation | ⚠️ Stub |

---

## WasmEventBridge (Kotlin/JS)

### Registered JavaScript Handlers

The `WasmEventBridge` registers handlers on `window.CSProEventHandler`:

| Handler | Purpose | Called By |
|---------|---------|-----------|
| `execPffAsync(pffPath)` | Stores pending PFF path for later execution | `jspi_execPff` in C++ |
| `getPendingPff()` | Returns and clears pending PFF path | Kotlin `EntryActivity` |
| `hasPendingPff()` | Checks if a pending PFF exists | Kotlin `EntryActivity` |
| `showProgress(message)` | Shows progress dialog (via EM_ASM) | C++ `ShowProgressDialog` |
| `hideProgress()` | Hides progress dialog (via EM_ASM) | C++ `HideProgressDialog` |
| `updateProgress(percent, message)` | Updates progress (via EM_ASM) | C++ `UpdateProgressDialog` |
| `refreshPage(contents)` | Triggers page refresh (via EM_ASM) | C++ `RefreshPage` |

### Kotlin API

```kotlin
object WasmEventBridge {
    // Registration
    fun register()           // Register handlers with window.CSProEventHandler
    fun unregister()         // Unregister handlers
    
    // ExecPff Support
    fun hasPendingPff(): Boolean      // Check for pending PFF
    fun getPendingPff(): String?      // Get and clear pending PFF path
    
    // Progress Dialog
    fun handleShowProgress(message: String)
    fun handleHideProgress()
    fun handleUpdateProgress(percent: Int, message: String): Boolean  // Returns cancelled
    fun cancelProgress()
    fun isProgressCancelled(): Boolean
    
    // Page Refresh
    fun handleRefreshPage(contents: Int)
    
    // Listeners
    fun addRefreshListener(listener: (Int) -> Unit)
    fun removeRefreshListener(listener: (Int) -> Unit)
    fun addProgressListener(listener: ProgressListener)
    fun removeProgressListener(listener: ProgressListener)
}

interface ProgressListener {
    fun onShowProgress(message: String)
    fun onHideProgress()
    fun onUpdateProgress(percent: Int, message: String?): Boolean
}
```

---

## ActionInvoker Support

### WasmObjectTransporter

The `WasmObjectTransporter` class provides ActionInvoker runtime support:

```cpp
class WasmObjectTransporter : public CommonObjectTransporter {
    // Disables access token checks for external callers
    bool DisableAccessTokenCheckForExternalCallers() const override { return true; }
    
    // Provides InterpreterAccessor for Logic.eval support
    std::shared_ptr<InterpreterAccessor> OnGetInterpreterAccessor() override;
    
    // Set engine driver after initialization
    void SetEngineDriver(CEngineDriver* engineDriver);
};
```

### Supported ActionInvoker Actions

| Action | Description |
|--------|-------------|
| `UI_getInputData` | Gets input data for dialogs |
| Logic evaluation | Via `InterpreterAccessor` from engine driver |

---

## Implementation Status Summary

| Category | Implemented | Stub/Partial | Not Available |
|----------|-------------|--------------|---------------|
| **Dialogs** | 10 | 0 | 0 |
| **Progress** | 3 | 0 | 0 |
| **System/Exec** | 1 | 0 | 3 |
| **File/View** | 2 | 0 | 0 |
| **GPS** | 0 | 5 | 0 |
| **Auth/Creds** | 2 | 3 | 0 |
| **Bluetooth** | 0 | 0 | 2 |
| **Network** | 1 | 1 | 1 |
| **Device Info** | 6 | 2 | 0 |
| **Multimedia** | 0 | 5 | 0 |
| **Paradata** | 1 | 2 | 0 |
| **Other** | 5 | 3 | 0 |

**Legend:**
- ✅ **Implemented** - Fully functional in browser environment
- ⚠️ **Stub/Partial** - Returns default values or could be implemented with Web APIs
- ❌ **Not Available** - Cannot be implemented in browser due to platform limitations

---

## Browser API Opportunities

Several stub methods could potentially be implemented using modern Web APIs:

| Feature | Web API | Browser Support |
|---------|---------|-----------------|
| GPS | `navigator.geolocation` | All modern browsers |
| Barcode | `BarcodeDetector` | Chrome 83+, Edge 83+ |
| Audio Recording | `MediaRecorder` | All modern browsers |
| Audio Playback | `HTMLAudioElement`, Web Audio API | All modern browsers |
| Camera/Image | `navigator.mediaDevices.getUserMedia()` | All modern browsers |
| Bluetooth | Web Bluetooth API | Chrome, Edge (HTTPS required) |

---

## Version History

| Date | Changes |
|------|---------|
| 2026-01-14 | Added ExecPff support (`jspi_execPff`, `WasmEventBridge.execPffAsync`) |
| 2026-01-14 | Initial documentation |
