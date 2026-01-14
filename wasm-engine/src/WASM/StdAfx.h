// StdAfx.h - Precompiled header for WASM build
// Provides basic includes needed by WASM-specific source files

#pragma once

#include <engine/StandardSystemIncludes.h>

// Core CSPro types needed by WASM files
#include <zToolsO/Utf8Convert.h>
#include <zUtilO/Interapp.h>
#include <zJson/Json.h>

#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#endif
