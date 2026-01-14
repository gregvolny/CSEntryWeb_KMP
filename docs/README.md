# CSEntry Web - Kotlin/Wasm + Emscripten Hybrid Architecture

## Project Overview

This is a **complete port** of the CSEntryDroid Android application to the web using a hybrid architecture:

- **Kotlin Multiplatform** for the UI and business logic layer (ported from Android)
- **Emscripten/WebAssembly** for the existing C++ engine (already compiled in `WASM/build/`)

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                        WEB BROWSER                             │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │           Kotlin/Wasm UI Layer                          │ │
│  │  (Ported from CSEntryDroid Activities/Fragments)        │ │
│  │                                                          │ │
│  │  • ApplicationsListActivity → HTML/DOM                  │ │
│  │  • CaseListActivity → HTML/DOM                          │ │
│  │  • EntryApplicationActivity → HTML/DOM                  │ │
│  │  • SettingsActivity → HTML/DOM                          │ │
│  └──────────────────────┬──────────────────────────────────┘ │
│                         │                                     │
│                         │ Kotlin JS Interop                   │
│                         ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │        WasmEngineInterface.kt                           │ │
│  │  (Ported from EngineInterface.java - 851 lines)         │ │
│  │                                                          │ │
│  │  Replaces 100+ JNI native methods with:                 │ │
│  │  • JavaScript interop calls                             │ │
│  │  • Emscripten Embind bindings                           │ │
│  │  • JSPI async operations                                │ │
│  └──────────────────────┬──────────────────────────────────┘ │
│                         │                                     │
│                         │ @JsModule import                    │
│                         ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │           CSPro.js (Emscripten Glue Code)               │ │
│  │  • Module initialization                                │ │
│  │  • Memory management                                    │ │
│  │  • Embind type conversions                              │ │
│  │  • File system (FS) operations                          │ │
│  └──────────────────────┬──────────────────────────────────┘ │
│                         │                                     │
│                         │ WebAssembly                         │
│                         ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │           CSPro.wasm (C++ Engine)                       │ │
│  │  • CoreEntryEngineInterface                             │ │
│  │  • Form rendering engine                                │ │
│  │  • Logic interpreter                                    │ │
│  │  • Data management                                      │ │
│  │  • All 40+ zModules (zEngineO, zFormO, etc.)           │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

## What Was Ported

### 1. Java to Kotlin Conversion

**Android EngineInterface.java (851 lines) → WasmEngineInterface.kt**

| Android (JNI) | Web (Emscripten) | Status |
|---------------|------------------|---------|
| `native long InitNativeEngineInterface()` | `CSProModuleFactory.getInstance()` | ✅ Done |
| `native boolean InitApplication(pffFilename)` | `csproModule.InitApplication().await()` | ✅ Done |
| `native void NextField()` | `csproModule.NextField().await()` | ✅ Done |
| `native void PrevField()` | `csproModule.PrevField().await()` | ✅ Done |
| `native boolean InsertCase(position)` | `csproModule.InsertCase(position).await()` | ✅ Done |
| `native void SetAndroidEnvironmentVariables(...)` | `csproModule.SetEnvironmentVariables(...).await()` | ✅ Done |
| ... 95+ more native methods | ... corresponding async calls | 🔄 In Progress |

###  2. Android UI to Web UI Conversion

**Activities/Fragments → HTML/DOM**

| Android | Web | Status |
|---------|-----|---------|
| `ApplicationsListActivity.kt` | HTML list with CSS Grid | ⏳ Todo |
| `CaseListActivity.kt` | HTML table/cards | ⏳ Todo |
| `EntryApplicationActivity.kt` | Canvas + DOM inputs | ⏳ Todo |
| `AboutActivity.kt` | HTML dialog | ⏳ Todo |
| `SettingsActivity.kt` | HTML form | ⏳ Todo |
| 160+ XML layouts | HTML templates | ⏳ Todo |

### 3. Android APIs to Web APIs

| Android API | Web API | Implementation |
|-------------|---------|----------------|
| `Context`, `Application` | Window, Document | `WasmPlatformServices.kt` |
| `SharedPreferences` | `localStorage` | ✅ Done |
| `File` API | IndexedDB / OPFS | ✅ Done |
| `AccountManager` | Form input | ✅ Done |
| `MediaScannerConnection` | N/A (not needed) | ✅ Done |
| GPS (`LocationManager`) | `navigator.geolocation` | ✅ Done |
| Bluetooth | `navigator.bluetooth` | ✅ Done |
| Camera | `navigator.mediaDevices` | ⏳ Todo |

### 4. JNI to Emscripten Bridge

**Android:**
```java
// Java calls into C++
private native boolean InitApplication(long ref, String pffFilename);

// JNI implementation (AndroidEngineInterface.cpp)
JNIEXPORT jboolean JNICALL 
Java_gov_census_cspro_engine_EngineInterface_InitApplication(
    JNIEnv* env, jobject obj, jlong ref, jstring pffFilename) {
    // Call C++ engine
    return engineInterface->InitApplication(filename);
}
```

**Web:**
```kotlin
// Kotlin/Wasm calls JavaScript
suspend fun openApplication(pffFilename: String): Boolean {
    return csproModule.InitApplication(pffFilename).await()
}

// JavaScript interop declaration
@JsModule("./wasm/CSProAndroid.js")
external interface CSProWasmModule {
    fun InitApplication(pffPath: String): Promise<Boolean>
}

// C++ exposed via Emscripten Embind (WebWASMBindings.cpp - NEW!)
bool InitApplication(std::string pffFilename) {
    if (!g_engineInterface) return false;
    CString csPffFilename(pffFilename.c_str());
    return g_engineInterface->InitApplication(csPffFilename);
}

EMSCRIPTEN_BINDINGS(cspro_android_engine) {
    function("InitApplication", &InitApplication);
}
```

**Key Change:** Using **Android C++ codebase** instead of MFC codebase for WASM:
- **Source:** `CSEntryDroid/app/src/main/jni/src/`
- **Entry point:** `AndroidEngineInterface.cpp` + `WebApplicationInterface.cpp`
- **Bindings:** `WebWASMBindings.cpp` (replaces JNI with Embind)
- **Build:** See `cspro-dev/cspro/CSEntryDroid/app/src/main/jni/BUILD_WASM.md`
- **Why:** Android C++ is cleaner, mobile-ready, actively maintained (not legacy MFC)

## Project Structure

```
CSEntryWeb_KMP/
├── build.gradle.kts                  # Kotlin Multiplatform build config
├── settings.gradle.kts               # Gradle settings
├── package.json                      # Node.js dependencies
├── server.js                         # Express server
│
├── src/
│   ├── commonMain/kotlin/            # Platform-independent code
│   │   └── gov/census/cspro/
│   │       ├── data/                 # Data models (CaseSummary, FieldNote)
│   │       ├── engine/               # Engine interfaces
│   │       └── platform/             # Platform abstraction
│   │
│   └── wasmJsMain/kotlin/            # Web-specific implementations
│       └── gov/census/cspro/
│           ├── Main.kt               # Entry point
│           ├── engine/
│           │   └── WasmEngineInterface.kt  # 851-line port of EngineInterface.java
│           ├── platform/
│           │   ├── CSProWasmModule.kt      # Emscripten bindings
│           │   └── WasmPlatformServices.kt # Web APIs
│           └── ui/
│               └── CSEntryApp.kt     # Main UI controller
│
├── public/                           # Static web assets
│   ├── index.html                    # Main HTML page
│   ├── css/
│   │   └── main.css                  # Styles
│   └── wasm-ui/                      # Kotlin/Wasm output (generated)
│       ├── csentry-web.mjs           # Kotlin/Wasm module
│       └── csentry-web.wasm          # Kotlin/Wasm binary
│
└── cspro-dev/cspro/
    ├── WASM/build/                   # Pre-built Emscripten output
    │   ├── CSPro.js                  # Emscripten glue code
    │   ├── CSPro.wasm                # C++ engine binary
    │   └── CSPro.data                # Embedded assets
    │
    └── CSEntryDroid/                 # Android source (reference)
        └── app/src/main/
            ├── java/gov/census/cspro/
            │   └── engine/
            │       └── EngineInterface.java  # Original Java (851 lines)
            ├── jni/src/                      # JNI C++ bridge (63 files)
            └── res/layout/                   # XML layouts (160+ files)
```

## Building the Project

### Prerequisites

- **JDK 17+** (for Kotlin/Wasm compilation)
- **Node.js 18+** (for server and build tools)
- **Gradle 8.5+** (for Kotlin Multiplatform)

### Build Steps

1. **Install dependencies:**
```powershell
cd "c:\Users\Admin\OneDrive\Documents\Github\CSEntry Web App\CSEntryWeb_KMP"
npm install
```

2. **Build Kotlin/Wasm:**
```powershell
.\gradlew wasmJsBrowserDistribution
```

This compiles Kotlin code to WebAssembly and outputs:
- `build/dist/wasmJs/productionExecutable/csentry-web.mjs`
- `build/dist/wasmJs/productionExecutable/csentry-web.wasm`

3. **Copy to public directory:**
```powershell
.\gradlew copyWasmToPublic
```

4. **Start the server:**
```powershell
npm start
```

Server runs at: **http://localhost:3002**

## How It Works

### Initialization Sequence

1. **Browser loads `index.html`**
2. **`index.html` loads `csentry-web.mjs`** (Kotlin/Wasm module)
3. **`Main.kt` executes:**
   - Initializes `CSProModuleFactory` → loads `CSPro.js` (Emscripten)
   - `CSPro.js` loads `CSPro.wasm` (C++ engine)
   - Creates `WasmEngineInterface` (ported from Android)
   - Creates `WasmPlatformServices` (Web APIs)
   - Mounts `CSEntryApp` UI to DOM

### Data Entry Flow

```
User clicks "Load Application"
    ↓
CSEntryApp.loadApplication()
    ↓
WasmEngineInterface.openApplication("/Assets/examples/Simple CAPI.pff")
    ↓
csproModule.InitApplication(pffPath).await()
    ↓
CSPro.js → CSPro.wasm (C++ InitApplication)
    ↓
C++ parses PFF, loads DIC/FRM files
    ↓
Returns success to Kotlin
    ↓
UI updates: "Application loaded"
```

## Key Differences from Android

| Aspect | Android | Web |
|--------|---------|-----|
| **Language** | Java + Kotlin | Kotlin (transpiled to JS) |
| **UI** | XML layouts + Activities | HTML/CSS + DOM |
| **Native Bridge** | JNI (63 .cpp files) | Embind + JS interop |
| **Threading** | Android threads + Handler | Coroutines + JSPI |
| **File System** | Android File API | IndexedDB / OPFS |
| **Permissions** | AndroidManifest.xml | Browser prompts |
| **Lifecycle** | onCreate/onResume/onPause | DOMContentLoaded/visibility |

## Current Status

✅ **Completed:**
- Kotlin Multiplatform project structure
- Build configuration (build.gradle.kts)
- WasmEngineInterface.kt (ported 851-line EngineInterface.java)
- CSProWasmModule.kt (Emscripten bindings)
- WasmPlatformServices.kt (Web APIs)
- Data models (CaseSummary, FieldNote, ValuePair)
- Basic UI framework (CSEntryApp.kt)
- Node.js server (Express)

🔄 **In Progress:**
- Completing all 100+ engine methods in WasmEngineInterface
- Porting Activities to HTML/DOM UI
- Converting XML layouts to HTML templates

⏳ **Todo:**
- Port Messenger.java (engine ↔ UI communication)
- Port remaining Activities (15+ files)
- Convert 160+ XML layouts to HTML
- Implement form rendering with Canvas
- Add camera/media capture
- Full integration testing

## Testing

```powershell
# Start development server
npm start

# Access application
# Open http://localhost:3002 in Chrome 109+ or Edge 109+
```

## License

See LICENSE.md in cspro-dev directory.
