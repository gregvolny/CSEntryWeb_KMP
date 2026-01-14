# Building CSPro Android C++ Engine for WebAssembly

## Overview

This directory contains the C++ source code for the **Android** version of CSPro, now being compiled to WebAssembly using Emscripten. This replaces the previous MFC-based WASM build.

## Why Android C++ Instead of MFC?

The Android codebase is better suited for web deployment:

| Aspect | Android C++ | MFC C++ |
|--------|-------------|---------|
| **Platform Abstraction** | Clean `BaseApplicationInterface` | Windows-specific APIs |
| **Architecture** | Mobile-ready (touch, GPS, web-like) | Desktop-focused |
| **Maintenance** | Actively developed | Legacy codebase |
| **Code Quality** | Modern C++14/17 | Older C++ patterns |

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    KOTLIN/WASM UI                         │
│          (ported from Android Activities/Kotlin)         │
└────────────────────┬─────────────────────────────────────┘
                     │ JS Interop
                     ▼
┌──────────────────────────────────────────────────────────┐
│              CSProAndroid.js (Emscripten Glue)           │
│                  WebWASMBindings.cpp                      │
│         (Embind exports replacing JNI functions)         │
└────────────────────┬─────────────────────────────────────┘
                     │ C++ Calls
                     ▼
┌──────────────────────────────────────────────────────────┐
│            AndroidEngineInterface.cpp                     │
│         (Core engine - same as Android app)               │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│          WebApplicationInterface.cpp                      │
│    (Replaces Android APIs with Web API stubs)            │
│    • JNI → Emscripten EM_ASM                             │
│    • Android GPS → Web Geolocation API                    │
│    • Android Storage → OPFS (Origin Private File System) │
│    • Android Bluetooth → Web Bluetooth API               │
└──────────────────────────────────────────────────────────┘
```

## Key Files Created for WASM

### 1. `WebWASMBindings.cpp`
**Purpose:** Emscripten Embind exports that replace JNI bindings

**What it does:**
- Exports 100+ engine methods to JavaScript
- Replaces `Java_gov_census_cspro_engine_EngineInterface_*` JNI functions
- Uses `EMSCRIPTEN_BINDINGS` macro instead of JNI `JNIEXPORT`

**Example:**
```cpp
// Android JNI version (gov_census_cspro_engine_EngineInterface_jni.cpp):
JNIEXPORT void JNICALL Java_gov_census_cspro_engine_EngineInterface_NextField
  (JNIEnv *env, jobject obj, jlong nativeReference)
{
    auto engine = (AndroidEngineInterface*)nativeReference;
    engine->NextField();
}

// Web WASM version (WebWASMBindings.cpp):
void NextField() {
    if (!g_engineInterface) return;
    g_engineInterface->NextField();
}

EMSCRIPTEN_BINDINGS(cspro_android_engine) {
    function("NextField", &NextField);
}
```

### 2. `WebApplicationInterface.cpp/h`
**Purpose:** Replaces Android platform APIs with web equivalents

**What it does:**
- Implements `BaseApplicationInterface` (same as `AndroidApplicationInterface`)
- Replaces Android JNI calls with Emscripten `EM_ASM` JavaScript calls
- Provides web-safe stubs for platform features

**Key replacements:**

| Android | Web |
|---------|-----|
| `AccountManager` (user email) | localStorage or user form |
| `LocationManager` (GPS) | `navigator.geolocation` |
| `BluetoothAdapter` | `navigator.bluetooth` |
| `MediaRecorder` | `MediaRecorder` API |
| `Camera` | `getUserMedia` API |
| `File` API | Emscripten FS + OPFS (Origin Private File System) |
| `SharedPreferences` | localStorage (credentials only) |
| JNI callbacks to Java | `EM_ASM` JavaScript calls |

**Example:**
```cpp
// Store credential in localStorage instead of Android KeyStore
void WebApplicationInterface::StoreCredential(
    const std::wstring& attribute,
    const std::wstring& secret_value)
{
    std::string attr = WS2CS(attribute);
    std::string value = WS2CS(secret_value);
    
    EM_ASM({
        if (window.localStorage) {
            localStorage.setItem(UTF8ToString($0), UTF8ToString($1));
        }
    }, attr.c_str(), value.c_str());
}
```

### 3. Modified `AndroidEngineInterface.cpp`
**Change needed:** Replace `AndroidApplicationInterface` with `WebApplicationInterface`

```cpp
// OLD (Android):
#include "AndroidApplicationInterface.h"
AndroidEngineInterface::AndroidEngineInterface()
    : m_pApplicationInterface(new AndroidApplicationInterface(this))
{
    // ...
}

// NEW (Web):
#ifdef __EMSCRIPTEN__
#include "WebApplicationInterface.h"
AndroidEngineInterface::AndroidEngineInterface()
    : m_pApplicationInterface(new WebApplicationInterface(this))
#else
#include "AndroidApplicationInterface.h"
AndroidEngineInterface::AndroidEngineInterface()
    : m_pApplicationInterface(new AndroidApplicationInterface(this))
#endif
{
    // ...
}
```

## Building with Emscripten

### Prerequisites

1. **Emscripten SDK**
```bash
git clone https://github.com/emscripten-core/emsdk.git
cd emsdk
./emsdk install latest
./emsdk activate latest
source ./emsdk_env.sh
```

2. **CMake 3.20+**
3. **Python 3.6+**

### Build Steps

1. **Create build directory:**
```bash
cd cspro-dev/cspro/CSEntryDroid/app/src/main/jni
mkdir build-wasm
cd build-wasm
```

2. **Configure with Emscripten:**
```bash
emcmake cmake .. \
  -DCMAKE_BUILD_TYPE=Release \
  -DEMSCRIPTEN=ON \
  -DBUILD_FOR_WEB=ON
```

3. **Build:**
```bash
emmake make -j4
```

4. **Output files:**
- `CSProAndroid.js` - JavaScript glue code
- `CSProAndroid.wasm` - Compiled C++ engine
- `CSProAndroid.data` - Embedded file system (if needed)

### CMakeLists.txt Configuration

Key Emscripten flags needed:

```cmake
if(EMSCRIPTEN)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} \
        -s WASM=1 \
        -s ALLOW_MEMORY_GROWTH=1 \
        -s MODULARIZE=1 \
        -s EXPORT_ES6=1 \
        -s EXPORT_NAME=createCSProModule \
        -s ENVIRONMENT=web \
        -s INVOKE_RUN=0 \
        -s NO_EXIT_RUNTIME=1 \
        -s EXPORTED_RUNTIME_METHODS=['FS','callMain'] \
        -lembind \
        --bind \
        -s ASSERTIONS=1 \
        -s SAFE_HEAP=0 \
        -s STACK_OVERFLOW_CHECK=0 \
        -s DISABLE_EXCEPTION_CATCHING=0 \
        -s FILESYSTEM=1 \
        -s FORCE_FILESYSTEM=1")
        
    # For async operations (JSPI - JavaScript Promise Integration)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -s ASYNCIFY")
endif()
```

### Source Files to Compile

**Core engine files (same as Android):**
- `AndroidEngineInterface.cpp` (with WebApplicationInterface)
- All `zModules` (zEngineO, zFormO, zLogicO, etc.)
- Common utilities (zUtilO, zToolsO, etc.)

**New web-specific files:**
- `WebWASMBindings.cpp` - Embind exports
- `WebApplicationInterface.cpp` - Platform stub implementations

**Exclude Android-specific files:**
- ❌ `gov_census_cspro_engine_EngineInterface_jni.cpp` (replaced by WebWASMBindings.cpp)
- ❌ `AndroidBluetoothAdapter.cpp` (uses JNI)
- ❌ `AndroidHttpConnection.cpp` (uses JNI)
- ❌ `AndroidMapUI.cpp` (uses JNI)
- ❌ `JNIHelpers.cpp` (not needed)
- ❌ All `*_jni.cpp` files

## Integration with Kotlin/Wasm

### 1. Load the WASM module

```kotlin
// CSProModuleFactory.kt
@JsModule("./wasm/CSProAndroid.js")
external fun createCSProModule(config: dynamic): Promise<CSProWasmModule>

suspend fun getInstance(): CSProWasmModule {
    if (moduleInstance == null) {
        val config = js("{}")
        config.print = { text: String -> console.log("[CSPro] $text") }
        config.printErr = { text: String -> console.error("[CSPro Error] $text") }
        
        moduleInstance = createCSProModule(config).await()
    }
    return moduleInstance!!
}
```

### 2. Call engine methods

```kotlin
// WasmEngineInterface.kt
override suspend fun moveToNextField(): Boolean {
    return try {
        csproModule.NextField().await() // Calls WebWASMBindings.cpp NextField()
        true
    } catch (e: Exception) {
        console.error("Error:", e)
        false
    }
}
```

### 3. Handle callbacks from C++

JavaScript functions that C++ calls via `EM_ASM`:

```javascript
// In HTML or Kotlin/JS
window.csproRefreshPage = function(contents) {
    // Refresh UI based on engine state
    console.log("Refresh page:", contents);
};

window.csproShowError = function(message) {
    alert("CSPro Error: " + message);
};

window.csproShowProgress = function(message) {
    document.getElementById('progress').innerText = message;
};
```

## Differences from Android Build

| Feature | Android (JNI) | Web (Emscripten) |
|---------|---------------|------------------|
| **Entry point** | `JNI_OnLoad` | Module initialization |
| **Method export** | `JNIEXPORT` macros | `EMSCRIPTEN_BINDINGS` |
| **Callbacks** | JNI `env->CallVoidMethod` | `EM_ASM` JavaScript |
| **Threading** | pthreads | ASYNCIFY (single-threaded) |
| **File system** | Android File API | Emscripten FS (IDBFS/MEMFS) |
| **Memory** | JNI Global/Local refs | Emscripten heap |
| **Object passing** | `jobject`, `jstring` | `emscripten::val` |

## Testing

1. **Build the WASM:**
```bash
cd build-wasm
emmake make
```

2. **Copy outputs:**
```bash
cp CSProAndroid.* ../../../../../public/wasm/
```

3. **Update Kotlin module path:**
```kotlin
// CSProWasmModule.kt
@JsModule("./wasm/CSProAndroid.js")
external fun createCSProModule(config: dynamic): Promise<CSProWasmModule>
```

4. **Run the web app:**
```bash
cd CSEntryWeb_KMP
npm start
```

5. **Test in browser:**
- Open http://localhost:3002
- Load an application
- Test field navigation
- Verify form rendering

## Debugging

### Enable debug output:

```cmake
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g4 -s ASSERTIONS=2 -s SAFE_HEAP=1")
```

### View WASM logs:

```javascript
// In browser console
Module.print = function(text) { console.log('[WASM]', text); };
Module.printErr = function(text) { console.error('[WASM Error]', text); };
```

### Debug with source maps:

```bash
emcc ... -g4 -s ASSERTIONS=2 --source-map-base http://localhost:3002/wasm/
```

Then use Chrome DevTools → Sources → wasm files

## Next Steps

1. ✅ Create WebWASMBindings.cpp
2. ✅ Create WebApplicationInterface.cpp
3. ⏳ Modify AndroidEngineInterface.cpp to use WebApplicationInterface
4. ⏳ Create CMakeLists.txt for Emscripten build
5. ⏳ Build and test WASM output
6. ⏳ Integrate with Kotlin/Wasm frontend

## References

- [Emscripten Documentation](https://emscripten.org/docs/)
- [Embind Documentation](https://emscripten.org/docs/porting/connecting_cpp_and_javascript/embind.html)
- [Android NDK Build System](https://developer.android.com/ndk/guides/build)
- Original Android JNI code: `gov_census_cspro_engine_EngineInterface_jni.cpp`
