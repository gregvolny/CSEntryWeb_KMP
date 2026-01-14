# WASM Engine Source Verification Report

**Generated:** 2025  
**Purpose:** Verify all Android JNI C++ engine files are copied for WASM build

## Summary

| Metric | Value |
|--------|-------|
| Total Source Files | 2,550+ |
| Source Directories | 45 |
| External Libraries | 8 |
| Android .mk Files Analyzed | 42 |

## Source Directory Verification

All directories from Android JNI `.mk` files have been verified:

| Directory | File Count | Status |
|-----------|------------|--------|
| CEXEntry | 9 | ✅ OK |
| engine | 194 | ✅ OK |
| external | 304 | ✅ OK |
| jni | 55 | ✅ OK |
| rtf2html_dll | 14 | ✅ OK |
| SQLite | 7 | ✅ OK |
| WASM | 7 | ✅ OK |
| zAction | 42 | ✅ OK |
| zAppO | 41 | ✅ OK |
| zBridgeO | 23 | ✅ OK |
| zCapiO | 36 | ✅ OK |
| zCaseO | 61 | ✅ OK |
| zConcatO | 15 | ✅ OK |
| zDataO | 73 | ✅ OK |
| zDictO | 46 | ✅ OK |
| zDiffO | 7 | ✅ OK |
| zEngineF | 30 | ✅ OK |
| zEngineO | 196 | ✅ OK |
| zEntryO | 27 | ✅ OK |
| zExcelO | 5 | ✅ OK |
| zExportO | 32 | ✅ OK |
| zFormatterO | 8 | ✅ OK |
| zFormO | 42 | ✅ OK |
| zFreqO | 33 | ✅ OK |
| zHtml | 57 | ✅ OK |
| zIndexO | 5 | ✅ OK |
| Zissalib | 43 | ✅ OK |
| zJavaScript | 15 | ✅ OK |
| zJson | 25 | ✅ OK |
| zListingO | 27 | ✅ OK |
| zLogicO | 47 | ✅ OK |
| zMapping | 30 | ✅ OK |
| zMessageO | 21 | ✅ OK |
| zMultimediaO | 14 | ✅ OK |
| zNetwork | 16 | ✅ OK |
| zPackO | 10 | ✅ OK |
| zParadataO | 60 | ✅ OK |
| zPlatformO | 21 | ✅ OK |
| zReformatO | 11 | ✅ OK |
| zReportO | 10 | ✅ OK |
| zSortO | 9 | ✅ OK |
| zSyncO | 104 | ✅ OK |
| zToolsO | 110 | ✅ OK |
| zUtilF | 85 | ✅ OK |
| zUtilO | 122 | ✅ OK |
| zZipO | 13 | ✅ OK |

## External Dependencies

| Library | Path | Purpose |
|---------|------|---------|
| jsoncons | external/jsoncons | JSON parsing (header-only) |
| easylogging | external/easylogging | Logging framework |
| variant | external/variant | C++17 variant polyfill |
| geometry.hpp | external/geometry.hpp | Geometry processing |
| rxcpp | external/rxcpp | Reactive extensions |
| zlib | external/zlib | Compression |
| SQLite | external/SQLite | Database |
| yaml-cpp | external/yaml-cpp | YAML parsing |

## Android .mk to WASM Directory Mapping

| .mk Variable | WASM src/ Path | Source Count |
|--------------|----------------|--------------|
| CEXENTRY_SRC_PATH | CEXEntry | 6 |
| ENGINE_SRC_PATH | engine | 85 |
| ZCAPIO_SRC_PATH | zCapiO | 9 |
| ZENTRYO_SRC_PATH | zEntryO | 10 |
| ZISSALIB_SRC_PATH | Zissalib | 27 |
| ZACTION_SRC_PATH | zAction | 19 |
| ZAPPO_SRC_PATH | zAppO | 18 |
| ZBRIDGEO_SRC_PATH | zBridgeO | 2 |
| ZCASEO_SRC_PATH | zCaseO | 21 |
| ZCONCATO_SRC_PATH | zConcatO | 5 |
| ZDATAO_SRC_PATH | zDataO | 22 |
| ZDICTO_SRC_PATH | zDictO | 19 |
| ZDIFFO_SRC_PATH | zDiffO | 2 |
| ZENGINEF_SRC_PATH | zEngineF | 6 |
| ZENGINEO_SRC_PATH | zEngineO | 92 |
| ZEXCELO_SRC_PATH | zExcelO | 27 |
| ZEXPORTO_SRC_PATH | zExportO | 34 |
| ZFORMATTERO_SRC_PATH | zFormatterO | 2 |
| ZFORMO_SRC_PATH | zFormO | 17 |
| ZFREQO_SRC_PATH | zFreqO | 11 |
| ZHTML_SRC_PATH | zHtml | 7 |
| ZINDEXO_SRC_PATH | zIndexO | 1 |
| ZJAVASCRIPT_SRC_PATH | zJavaScript | 9 |
| ZJSON_SRC_PATH | zJson | 6 |
| ZLISTINGO_SRC_PATH | zListingO | 10 |
| ZLOGICO_SRC_PATH | zLogicO | 12 |
| ZMAPPING_SRC_PATH | zMapping | 2 |
| ZMESSAGEO_SRC_PATH | zMessageO | 6 |
| ZMULTIMEDIAOO_SRC_PATH | zMultimediaO | 6 |
| ZNETWORK_SRC_PATH | zNetwork | 2 |
| ZPACKO_SRC_PATH | zPackO | 3 |
| ZPARADATAO_SRC_PATH | zParadataO | 25 |
| ZPLATFORMO_SRC_PATH | zPlatformO | 5 |
| ZREFORMATO_SRC_PATH | zReformatO | 2 |
| ZREPORTO_SRC_PATH | zReportO | 14 |
| ZSORTO_SRC_PATH | zSortO | 3 |
| ZSYNCO_SRC_PATH | zSyncO | 34 |
| ZTOOLSO_SRC_PATH | zToolsO | 32 |
| ZUTILF_SRC_PATH | zUtilF | 10 |
| ZUTILO_SRC_PATH | zUtilO | 37 |
| ZZIPO_SRC_PATH | zZipO | 4 |

## Key Files for WASM Adaptation

These files in `wasm-engine/src/WASM/` provide web-specific implementations:

| File | Purpose |
|------|---------|
| WebEngineInterface.cpp | Web implementation of EngineInterface |
| WebApplicationInterface.cpp | Web UI callbacks |
| WebWASMBindings_full.cpp | Emscripten bindings for Kotlin/JS |
| WebUserbar.h | Web userbar implementation |
| StdAfx.h | Precompiled header for WASM |
| Definitions.cmake | Common WASM build definitions |
| CMakeLists.txt | WASM module build configuration |

## Android-to-WASM Mapping

| Android JNI File | WASM Equivalent |
|------------------|-----------------|
| AndroidEngineInterface.cpp | WebEngineInterface.cpp |
| AndroidApplicationInterface.cpp | WebApplicationInterface.cpp |
| gov_census_cspro_*.cpp | WebWASMBindings_full.cpp |
| ActionInvokerPortableRunner.cpp | (direct reuse) |

## Next Steps

1. **Update CMakeLists.txt** - Configure to build from `wasm-engine/src/`
2. **Run Build** - `emcmake cmake .. -G Ninja && ninja`
3. **Test Output** - Verify `csentryKMP.js`, `csentryKMP.wasm`, `csentryKMP.data`
4. **Integrate with Kotlin** - Update CSProEngineModule.kt

## Commands

```powershell
# Re-run source setup if needed
.\setup-sources.ps1 -Force -Verbose

# Build WASM
cd build
emcmake cmake .. -G Ninja
ninja -j8
```
