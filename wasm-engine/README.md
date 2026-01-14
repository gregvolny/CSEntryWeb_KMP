# CSEntry KMP WASM Engine Build

This folder contains the build configuration for compiling the **full Android C++ CSPro engine** to WebAssembly for use with Kotlin Multiplatform.

## Output Files

After building, the following files are produced:
- `csentryKMP.js` - ES6 module with Emscripten loader
- `csentryKMP.wasm` - WebAssembly binary (~17MB)
- `csentryKMP.data` - Preloaded assets (optional)

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Browser                                    │
│  ┌─────────────────────┐    ┌──────────────────────────────┐ │
│  │  csentry-web.wasm   │───▶│  csentryKMP.js + .wasm       │ │
│  │  (Kotlin UI ~140KB) │    │  (C++ Engine - full build)   │ │
│  │                     │    │                              │ │
│  │  CSProEngineService │───▶│  WebEngineInterface          │ │
│  │  window.CSEntryKMP  │    │  WebWASMBindings_full.cpp    │ │
│  └─────────────────────┘    └──────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## JNI to WASM Adaptation

The Android JNI code is adapted for web:

| Android JNI | WASM Equivalent |
|-------------|-----------------|
| AndroidEngineInterface.cpp | WebEngineInterface.cpp |
| AndroidApplicationInterface.cpp | WebApplicationInterface.cpp |
| gov_census_cspro_*_jni.cpp | WebWASMBindings_full.cpp (Embind) |
| JNI callbacks to Java | EM_ASM JavaScript calls |

## Prerequisites

1. **Emscripten SDK** - Install at C:\emsdk or set EMSDK environment variable
   ```powershell
   git clone https://github.com/emscripten-core/emsdk.git C:\emsdk
   cd C:\emsdk
   .\emsdk install latest
   .\emsdk activate latest
   ```

2. **Ninja Build** - Required for fast builds
   ```powershell
   choco install ninja  # or scoop install ninja
   ```

## Building

Run from the CSEntryWeb_KMP root:
```powershell
cd wasm-engine
.\build-wasm.ps1
```

Options:
- `-Clean` - Clean build directory first
- `-Debug` - Build with debug symbols
- `-Verbose` - Show verbose build output
- `-NoCopy` - Don't copy to public/wasm

## Module Export

The built module exports as ES6:
```javascript
import createCSEntryKMPModule from '/wasm/csentryKMP.js';

const module = await createCSEntryKMPModule();

// Use standalone functions
module.InitNativeEngineInterface();
module.InitApplication('/path/to/app.pff');
module.Start();

// Or use the FS API
module.FS.writeFile('/data.csdb', data);
```

## Kotlin Integration

The Kotlin code imports the module in `CSProEngineModule.kt`:
```kotlin
@file:JsModule("./csentryKMP.js")
external fun createCSEntryKMPModule(): Promise<CSProModule>
```

And loads it in `CSProEngineService.kt`:
```kotlin
val module = createCSEntryKMPModule().await()
module.InitNativeEngineInterface()
module.InitApplication(pffPath)
```

## Emscripten Settings

Key settings in CMakeLists.txt:
- `--bind` - Embind for JavaScript bindings
- `-sEXPORT_ES6=1` - ES6 module output
- `-sMODULARIZE=1` - Factory function
- `-sJSPI` - JavaScript Promise Integration for async
- `-fwasm-exceptions` - Native WASM exceptions
- `-sALLOW_MEMORY_GROWTH=1` - Dynamic memory

## Files

- `CMakeLists.txt` - CMake configuration for WASM build
- `build-wasm.ps1` - PowerShell build script
- `README.md` - This file

## Troubleshooting

### Build Fails: emcc not found
Ensure Emscripten is installed and activated:
```powershell
C:\emsdk\emsdk activate latest
C:\emsdk\emsdk_env.ps1
```

### Build Fails: Missing headers
The build uses sources from:
- `cspro-dev/cspro/CSEntryDroid/app/src/main/jni/src/` - Web-adapted code
- `cspro-dev/cspro/CSEntryDroid/app/src/main/jni/build-wasm/` - WASM-specific code
- `cspro-dev/cspro/` - Main CSPro source tree

### Runtime Error: Module not found
Ensure the server is configured to serve `/wasm/csentryKMP.js`:
```javascript
app.use('/wasm', express.static(join(__dirname, 'public/wasm')));
```
