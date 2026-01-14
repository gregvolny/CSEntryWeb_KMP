// stdafx.h - Unified precompiled header for CSPro WASM build
// This file provides all common includes needed by CSPro libraries

#pragma once

// Include Emscripten headers FIRST before any CSPro headers
// because CSPro defines `#define NONE -1` which conflicts with 
// emscripten/bind.h's `sharing_policy::NONE` enum value
#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#include <emscripten/bind.h>
#endif

// Standard system includes
#include <engine/StandardSystemIncludes.h>

// Core CSPro includes
#include <zToolsO/Special.h>
#include <zToolsO/Utf8Convert.h>
#include <zToolsO/Serializer.h>
#include <zUtilO/Interapp.h>
#include <zJson/Json.h>
#include <zJson/JsonSpecFile.h>
#include <zAppO/Application.h>
#include <zAppO/Properties/JsonProperties.h>
