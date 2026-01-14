# CSEntry Web KMP - File Organization

## Directory Structure

```
CSEntryWeb_KMP/
├── src/
│   ├── commonMain/kotlin/gov/census/cspro/
│   │   ├── bridge/
│   │   │   └── CNPifFile.kt                    # .pff file parser interface
│   │   ├── data/
│   │   │   ├── ActionInvokerResult.kt         # Action invoker result data
│   │   │   ├── CaseInfo.kt                     # Case metadata
│   │   │   ├── DataModels.kt                   # CaseSummary, ValuePair, FieldNote
│   │   │   ├── EntryModels.kt                  # CDEField, EntryPage, CaseTreeNode
│   │   │   └── PffStartModeParameter.kt       # PFF start mode data
│   │   ├── engine/
│   │   │   ├── IEngineInterface.kt             # Engine interface contract
│   │   │   ├── IEngineMessageListener.kt       # Engine message callbacks
│   │   │   ├── ParadataDriver.kt               # Paradata collection
│   │   │   └── UserbarHandler.kt               # Toolbar/userbar management
│   │   ├── exception/
│   │   │   └── PathException.kt                # Path-related exceptions
│   │   ├── platform/
│   │   │   └── IPlatformServices.kt            # Platform services interface
│   │   ├── ui/
│   │   │   ├── IActivity.kt                    # Activity interface + BaseActivity
│   │   │   └── IFragment.kt                    # Fragment interface + BaseFragment
│   │   └── util/
│   │       ├── CredentialStore.kt              # Credential storage (expect)
│   │       ├── CSEntryDirectory.kt             # Directory management (expect)
│   │       ├── FileInfo.kt                     # File metadata
│   │       └── Util.kt                         # Path manipulation utilities
│   │
│   └── wasmJsMain/kotlin/gov/census/cspro/
│       ├── bridge/
│       │   └── CNPifFileWasm.kt                # WASM .pff parser implementation
│       ├── engine/
│       │   ├── Messenger.kt                    # Engine↔UI messaging system
│       │   └── WasmEngineInterface.kt          # WASM engine implementation
│       ├── platform/
│       │   ├── CSProModuleFactory.kt           # C++ WASM module factory
│       │   ├── CSProWasmModule.kt              # C++ module interface
│       │   ├── ExternalTypes.kt                # JS external types
│       │   └── WasmPlatformServices.kt         # WASM platform implementation
│       ├── ui/
│       │   ├── ApplicationsListActivity.kt     # App listing screen
│       │   ├── CaseListActivity.kt             # Case management screen
│       │   ├── CSEntryApp.kt                   # Main app coordinator
│       │   ├── EntryActivity.kt                # Data entry screen
│       │   ├── NavigationFragment.kt           # Case tree navigation
│       │   └── QuestionnaireFragment.kt        # Form field rendering
│       ├── util/
│       │   ├── CredentialStoreWasm.kt          # localStorage implementation
│       │   ├── CSEntryDirectoryWasm.kt         # Virtual filesystem paths
│       │   └── FileUtils.kt                    # File system utilities
│       └── Main.kt                             # Application entry point
│
├── build.gradle.kts                             # Gradle build configuration
├── clean-build.ps1                              # Clean build script
├── MISSING_FILES_ANALYSIS.md                    # Missing files analysis
└── README.md                                    # Project documentation
```

## File Categories

### Core Infrastructure (commonMain)
Platform-independent core classes that define contracts and shared logic.

**Data Models:**
- `CaseSummary`, `ValuePair`, `FieldNote` - Case and field data
- `CaseInfo`, `CaseStatus` - Case metadata
- `CDEField`, `EntryPage`, `CaseTreeNode` - Entry form models
- `ActionInvokerResult` - Action execution results
- `PffStartModeParameter` - Application start modes

**Engine:**
- `IEngineInterface` - Engine contract (100+ methods from Android)
- `IEngineMessageListener` - Engine callbacks (onRefreshPage, onShowDialog, etc.)
- `UserbarHandler` - Toolbar button management
- `ParadataDriver` - Event logging for data quality

**UI:**
- `IActivity` / `BaseActivity` - Activity lifecycle abstraction
- `IFragment` / `BaseFragment` - Fragment component abstraction

**Utilities:**
- `Util` - Path manipulation (combinePath, removeFilename, removeDirectory)
- `FileInfo` - File metadata wrapper
- `CredentialStore` (expect/actual) - Key-value storage
- `CSEntryDirectory` (expect/actual) - Application directories
- `CNPifFile` - .pff file parser interface

**Platform:**
- `IPlatformServices` - Platform capabilities contract

### WASM Implementation (wasmJsMain)
Web-specific implementations using Kotlin/Wasm + JavaScript interop.

**Engine:**
- `WasmEngineInterface` - Full engine implementation (1092 lines)
  - Application lifecycle (openApplication, closeApplication)
  - Data entry (start, stop, saveCase, newCase)
  - Field navigation (nextField, prevField, goToField)
  - Case management (modifyCase, insertCase, deleteCase)
  - System info (getVersion, getApplicationDescription)
- `Messenger` - Coroutine-based message queue (426 lines)
  - Replaces Android's Handler/Looper pattern
  - FIFO message processing
  - Synchronous engine call support

**Platform:**
- `CSProWasmModule` - C++ WASM module interface
- `CSProModuleFactory` - Module instance management
- `WasmPlatformServices` - Full platform implementation
  - File I/O via OPFS (Origin Private File System)
  - Geolocation API
  - Network status
  - Web Bluetooth
  - localStorage credentials
- `ExternalTypes.kt` - JS interop type definitions

**UI Activities:**
- `ApplicationsListActivity` - Browse .pff applications
- `CaseListActivity` - Manage cases (add, modify, view, delete, sync)
- `EntryActivity` - Data entry coordinator
- `QuestionnaireFragment` - Render form fields
- `NavigationFragment` - Case tree navigation
- `CSEntryApp` - Application state management

**Utilities:**
- `FileUtils` - File system access (mock for now, will use File System Access API)
- `ApplicationLoader` - Load and parse .pff files
- `CredentialStoreWasm` - localStorage implementation
- `CSEntryDirectoryWasm` - Virtual filesystem paths

## Key Design Patterns

### 1. expect/actual Pattern
Used for platform-specific implementations:
```kotlin
// commonMain
expect class CredentialStore {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

// wasmJsMain
actual class CredentialStore {
    actual fun getString(key: String, defaultValue: String): String {
        return localStorage.getItem(key) ?: defaultValue
    }
    actual fun putString(key: String, value: String) {
        localStorage.setItem(key, value)
    }
}
```

### 2. CNPifFile Loader Pattern
```kotlin
// commonMain - interface
class CNPifFile(filename: String) {
    private fun loadPifData(filename: String) {
        val data = CNPifFileLoader.load(filename)
        // ...
    }
}
expect object CNPifFileLoader {
    fun load(filename: String): CNPifFileData
}

// wasmJsMain - implementation
actual object CNPifFileLoader {
    actual fun load(filename: String): CNPifFileData {
        // Call C++ WASM module or return mock data
    }
}
```

### 3. Activity/Fragment Pattern
Mirrors Android architecture:
```kotlin
// Application flow:
Main.kt → ApplicationsListActivity → CaseListActivity → EntryActivity
                                                           ├─ QuestionnaireFragment
                                                           └─ NavigationFragment
```

## Import Organization

All files MUST have correct imports for web environment:

```kotlin
package gov.census.cspro.ui

import gov.census.cspro.engine.WasmEngineInterface
import kotlinx.browser.document
import kotlinx.browser.window  // CRITICAL: Required for console, localStorage, etc.
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
```

### Common Import Requirements

**For console logging:**
```kotlin
import kotlinx.browser.window
// Usage: window.console.log("message")
```

**For DOM manipulation:**
```kotlin
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLDivElement
```

**For coroutines:**
```kotlin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.await
```

**For storage:**
```kotlin
import kotlinx.browser.localStorage
```

## Compilation Issues & Solutions

### Issue 1: "Unresolved reference 'console'"
**Cause**: Missing `import kotlinx.browser.window`  
**Solution**: Add import and use `window.console.log()` instead of `console.log()`

### Issue 2: "Redeclaration"
**Cause**: Duplicate class/interface definitions  
**Solution**: Check if type already exists in DataModels.kt or other files

### Issue 3: "System.currentTimeMillis() not found"
**Cause**: System class not available in commonMain  
**Solution**: 
- Use expect/actual for platform-specific time
- Or pass timestamp from wasmJsMain caller
- Or use `0L` as default in commonMain

### Issue 4: "Type argument not within bounds: JsAny"
**Cause**: Trying to cast JsAny to Kotlin types directly  
**Solution**: Use proper conversion functions:
```kotlin
// Wrong:
val result = jsValue.unsafeCast<String>()

// Correct:
val result = (jsValue as? JsString)?.toString()
```

## Build Process

### Clean Build
```powershell
.\clean-build.ps1
```

### Standard Build
```powershell
.\gradlew compileKotlinWasmJs
```

### Development Build with Webpack
```powershell
.\gradlew wasmJsBrowserDevelopmentWebpack
```

### Production Build
```powershell
.\gradlew wasmJsBrowserProductionWebpack
```

## File Counts

- **Total Kotlin files**: 27
- **commonMain files**: 12
- **wasmJsMain files**: 15
- **Total lines**: ~4,500+

## Next Steps

1. **Fix remaining compilation errors** (~245 errors)
   - Console reference resolution
   - JsAny type casting in WasmEngineInterface
   - Return type mismatches

2. **Connect C++ WASM module**
   - Replace mock CNPifFileLoader
   - Implement actual EngineInterface JNI equivalents
   - Test with real .pff files

3. **Implement File System Access**
   - Replace FileUtils mock implementation
   - Use File System Access API or OPFS
   - Add directory browsing

4. **Testing**
   - Unit tests for utilities
   - Integration tests for engine interface
   - End-to-end UI tests

## Related Documentation

- `MISSING_FILES_ANALYSIS.md` - Detailed analysis of ported files
- `ANDROID_TO_WEB_CONVERSION.md` - Architecture conversion guide
- `build.gradle.kts` - Build configuration
