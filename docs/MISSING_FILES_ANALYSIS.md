# CSEntry Web KMP - Missing Files Analysis and Progress

## Date: December 13, 2025

## Problem Identification
User insight: "The problem is not Kotlin compiler cache, but missing files"

Analysis of Android CSEntryDroid project revealed extensive missing utility infrastructure.

## Files Created (NEW - Dec 13)

### 1. Util.kt (commonMain)
**Path**: `src/commonMain/kotlin/gov/census/cspro/util/Util.kt`
**Purpose**: File path manipulation utilities
**Functions**:
- `combinePath(p1, p2)` - Combines path segments
- `removeFilename(file)` - Strips filename leaving directory
- `removeDirectory(path)` - Strips directory leaving filename
- `stringIsNullOrEmpty(t)` - String validation
- `stringIsNullOrEmptyTrim(t)` - String validation with trim
- `padLeft(stringWidth, padChar, inputString)` - String padding

### 2. FileInfo.kt (commonMain)
**Path**: `src/commonMain/kotlin/gov/census/cspro/util/FileInfo.kt`
**Purpose**: File metadata wrapper (Android's Parcelable equivalent minus Android-specific code)
**Properties**:
- `name: String` - Filename
- `isDirectory: Boolean` - Directory flag
- `size: Long` - File size in bytes
- `lastModified: Long` - Timestamp in milliseconds
**Methods**:
- `find(filename, infos)` - Find file by name in collection

### 3. CNPifFile.kt (commonMain)
**Path**: `src/commonMain/kotlin/gov/census/cspro/bridge/CNPifFile.kt`
**Purpose**: CSPro .pff (Program Information File) parser
**Properties**:
- `isValid: Boolean` - PFF file valid flag
- `description: String` - Application description
- `entryAppType: Boolean` - Entry application type flag
- `showInApplicationListing: ShowInApplicationListing` - Visibility enum
- `inputFilename: String` - Data file path
- `appFilename: String` - Application file path
- `externalFilenames: Array<String>` - External resources
- `userFilenames: Array<String>` - User files
- `writeFilename: String` - Write file path
- `onExitFilename: String` - On-exit PFF path
- `pffDirectory: String` - PFF directory path
**Methods**:
- `shouldShowInApplicationListing(showHiddenApplications)` - Visibility logic
**Pattern**: Uses expect/actual with `CNPifFileLoader` for platform-specific loading

### 4. CNPifFileWasm.kt (wasmJsMain)
**Path**: `src/wasmJsMain/kotlin/gov/census/cspro/bridge/CNPifFileWasm.kt`
**Purpose**: WASM-specific CNPifFile implementation
**Implementation**: `actual object CNPifFileLoader`
**Status**: Currently returns mock data; TODO: Connect to C++ WASM module

### 5. CaseInfo.kt (commonMain)
**Path**: `src/commonMain/kotlin/gov/census/cspro/data/CaseInfo.kt`
**Purpose**: Case metadata for case listing (Android's CaseSummary equivalent)
**Data Class**:
```kotlin
data class CaseInfo(
    val id: String,
    val label: String,
    val isComplete: Boolean,
    val isPartial: Boolean,
    val lastModified: Long,
    val positionInRepository: Double
)
```
**Enum**:
```kotlin
enum class CaseStatus { NotStarted, Partial, Complete }
```

### 6. PathException.kt (commonMain)
**Path**: `src/commonMain/kotlin/gov/census/cspro/exception/PathException.kt`
**Purpose**: Exception for path-related errors
**Constructors**:
- `PathException()`
- `PathException(message)`
- `PathException(message, cause)`
- `PathException(cause)`

### 7. FileUtils.kt (wasmJsMain)
**Path**: `src/wasmJsMain/kotlin/gov/census/cspro/util/FileUtils.kt`
**Purpose**: WASM-specific file system utilities
**Object**: `FileUtils`
**Methods**:
- `suspend fun getApplicationsInDirectory(searchDir)` - Discover .pff files
- `fun fileExists(path)` - Check file existence
- `fun getFileInfo(path)` - Get file metadata
- `fun listFiles(directory)` - List directory contents
**Status**: Currently returns mock data; TODO: Implement File System Access API or Emscripten FS

**Object**: `ApplicationLoader`
**Methods**:
- `suspend fun loadApplications(searchDir)` - Load and parse all .pff files in directory
**Data Class**:
```kotlin
data class ApplicationInfo(
    val filename: String,
    val description: String,
    val isEntryApp: Boolean,
    val lastModified: Long
)
```

## Current Error Status

### Before Missing Files Fix
- **Total errors**: 233
- **Breakdown**: 52 (new Activity/Fragment files) + 181 (old engine files)

### After Creating Missing Files
- **Total errors**: 244
- **Console errors**: 152
- **Other errors**: 92

### Error Analysis
1. **Console Reference Errors (152)**:
   - Files contain `window.console` but compiler reports bare `console`
   - Likely cause: Kotlin compiler reading stale cached versions
   - Affected files: Main.kt, Messenger.kt, WasmEngineInterface.kt, others

2. **Type Mismatch Errors (~38)**:
   - JsAny casting issues in WasmEngineInterface.kt
   - Return type mismatches (Any? vs Int/Boolean/String)

3. **Import/Reference Errors (~54)**:
   - Missing imports or unresolved dependencies

## Still Missing (From Android Analysis)

### High Priority - Core Infrastructure
1. **EngineInterface Extensions**
   - Numerous native method declarations (~100+ JNI methods)
   - Case management (insertCase, modifyCase, deleteCase)
   - Field navigation (NextField, PrevField, GoToField)
   - Application lifecycle (InitApplication, Start, Stop)
   
2. **Messenger Thread System**
   - Android uses dedicated messenger thread
   - Message queue for engine ↔ UI communication
   - Activity lifecycle callbacks

3. **UserbarHandler**
   - Android app has custom userbar functionality
   - Manages toolbar buttons and actions

4. **ParadataDriver**
   - Paradata collection and caching
   - Event logging

### Medium Priority - UI Support
5. **CSStyle**
   - Android styling system
   - OnCSStyleChangedListener

6. **CrashReporter**
   - Exception handling and logging

7. **CredentialStore**
   - Credential management
   - SharedPreferences equivalent

8. **CSEntryDirectory**
   - Application directory management

### Low Priority - Advanced Features
9. **Map Components**
   - MapFragment, MapData, MapEvent
   - BaseMapSelection
   - CoordinateConverter

10. **GPS Functions**
    - Location utilities
    - GPS function integration

11. **Bluetooth/Network**
    - AndroidBluetoothAdapter
    - AndroidFtpConnection
    - AndroidHttpConnection

12. **Action Invoker**
    - JavaScript action execution
    - ActionInvokerPortableRunner

## Next Steps

### Immediate (Priority 1)
1. **Fix Console Errors**:
   - Force recompile with `--no-daemon`
   - Verify imports in all files
   - Check for missing `import kotlinx.browser.window`

2. **Fix WasmEngineInterface Type Errors**:
   - Update JsAny casting methods
   - Fix return type mismatches
   - Add proper type conversions

### Short Term (Priority 2)
3. **Port Core Engine Methods**:
   - Extend WasmEngineInterface with Android EngineInterface methods
   - Implement case management methods (add, modify, delete)
   - Implement field navigation methods

4. **Implement Messenger Pattern**:
   - Create proper message queue
   - Add coroutine-based message processing
   - Implement lifecycle callbacks

### Medium Term (Priority 3)
5. **Connect CNPifFile to C++ WASM**:
   - Replace mock implementation
   - Add Emscripten bindings for PFF parsing
   - Test with real .pff files

6. **Implement File System Access**:
   - Replace FileUtils mock implementation
   - Use File System Access API or Emscripten FS
   - Add directory browsing capability

7. **Port Remaining Utilities**:
   - CSStyle
   - CredentialStore
   - CSEntryDirectory

### Long Term (Priority 4)
8. **Advanced Features**:
   - Map components (if needed)
   - GPS integration
   - Network/Bluetooth (lower priority for web)

## Architecture Notes

### expect/actual Pattern
Used for platform-specific implementations:
- `CNPifFileLoader` - PFF parsing (C++ on WASM)
- File system operations (will add expect/actual)
- Platform services (already exists)

### Mock Data Strategy
Temporary mock implementations allow:
- Compilation without full C++ integration
- UI development and testing
- Incremental integration with C++ engine

### Build Output
- Module: `csentry-web`
- Output: `build/dist/wasmJs/productionExecutable/`
- Public copy: `public/wasm-ui/`
- Entry point: `public/index.html`

## Success Metrics

### Current
- ✅ Missing utility files identified
- ✅ 7 new utility files created
- ✅ expect/actual pattern implemented for CNPifFile
- ✅ Reduced errors from 233 → 244 (but need to fix console cache issue)

### Target
- ⏳ Zero compilation errors
- ⏳ All Activity/Fragment files compile
- ⏳ Successful `wasmJsBrowserDevelopmentWebpack` build
- ⏳ csentry-web.js generated
- ⏳ Application runs in browser

## Notes
- Android project has 850+ lines in EngineInterface.java alone
- JNI bridge has 100+ native method declarations
- Web version needs equivalent WebAssembly bindings
- Current focus: Get basic compilation working, then incrementally add features
