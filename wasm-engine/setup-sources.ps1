# setup-sources.ps1
# Copies and organizes CSPro engine source files for standalone WASM compilation
# This creates a self-contained copy of all required sources in the wasm-engine/src folder

param(
    [switch]$Force,
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"

# Paths
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DestRoot = Join-Path $ScriptDir "src"
$CSProRoot = Join-Path $ScriptDir "..\cspro-dev\cspro"

# Verify source exists
if (-not (Test-Path $CSProRoot)) {
    Write-Error "CSPro source not found at: $CSProRoot"
    exit 1
}

Write-Host "=== CSPro WASM Engine Source Setup ===" -ForegroundColor Cyan
Write-Host "Source: $CSProRoot"
Write-Host "Destination: $DestRoot"

# Clean destination if Force
if ($Force -and (Test-Path $DestRoot)) {
    Write-Host "Cleaning existing src folder..." -ForegroundColor Yellow
    Remove-Item $DestRoot -Recurse -Force
}

# Create destination
New-Item -ItemType Directory -Path $DestRoot -Force | Out-Null

# Libraries to copy (in dependency order)
$Libraries = @(
    # External dependencies
    @{ Name = "external/jsoncons"; Dest = "external/jsoncons"; Pattern = "*.hpp" }
    @{ Name = "external/easylogging"; Dest = "external/easylogging"; Pattern = "*.*" }
    @{ Name = "external/variant/include"; Dest = "external/variant/include"; Pattern = "*.hpp" }
    @{ Name = "external/geometry.hpp/include"; Dest = "external/geometry.hpp/include"; Pattern = "*.hpp" }
    @{ Name = "external/rxcpp"; Dest = "external/rxcpp"; Pattern = "*.hpp" }
    @{ Name = "external/zlib"; Dest = "external/zlib"; Pattern = "*.[ch]" }
    @{ Name = "external/SQLite"; Dest = "external/SQLite"; Pattern = "*.[ch]" }
    @{ Name = "external/yaml-cpp"; Dest = "external/yaml-cpp"; Pattern = "*" }
    
    # Core engine libraries
    @{ Name = "engine"; Dest = "engine"; Pattern = "*" }
    @{ Name = "rtf2html_dll"; Dest = "rtf2html_dll"; Pattern = "*" }
    @{ Name = "SQLite"; Dest = "SQLite"; Pattern = "*" }
    @{ Name = "Cexentry"; Dest = "CEXEntry"; Pattern = "*" }
    @{ Name = "Zissalib"; Dest = "Zissalib"; Pattern = "*" }
    
    # z* libraries (CSPro modules)
    @{ Name = "zAction"; Dest = "zAction"; Pattern = "*" }
    @{ Name = "zAppO"; Dest = "zAppO"; Pattern = "*" }
    @{ Name = "zBridgeO"; Dest = "zBridgeO"; Pattern = "*" }
    @{ Name = "zCaseO"; Dest = "zCaseO"; Pattern = "*" }
    @{ Name = "zConcatO"; Dest = "zConcatO"; Pattern = "*" }
    @{ Name = "zDataO"; Dest = "zDataO"; Pattern = "*" }
    @{ Name = "zDictO"; Dest = "zDictO"; Pattern = "*" }
    @{ Name = "zDiffO"; Dest = "zDiffO"; Pattern = "*" }
    @{ Name = "zEngineF"; Dest = "zEngineF"; Pattern = "*" }
    @{ Name = "zEngineO"; Dest = "zEngineO"; Pattern = "*" }
    @{ Name = "zEntryO"; Dest = "zEntryO"; Pattern = "*" }
    @{ Name = "zExcelO"; Dest = "zExcelO"; Pattern = "*" }
    @{ Name = "zExportO"; Dest = "zExportO"; Pattern = "*" }
    @{ Name = "zFormatterO"; Dest = "zFormatterO"; Pattern = "*" }
    @{ Name = "zFormO"; Dest = "zFormO"; Pattern = "*" }
    @{ Name = "zFreqO"; Dest = "zFreqO"; Pattern = "*" }
    @{ Name = "zHtml"; Dest = "zHtml"; Pattern = "*" }
    @{ Name = "zIndexO"; Dest = "zIndexO"; Pattern = "*" }
    @{ Name = "zJavaScript"; Dest = "zJavaScript"; Pattern = "*" }
    @{ Name = "zJson"; Dest = "zJson"; Pattern = "*" }
    @{ Name = "zListingO"; Dest = "zListingO"; Pattern = "*" }
    @{ Name = "zLogicO"; Dest = "zLogicO"; Pattern = "*" }
    @{ Name = "zMapping"; Dest = "zMapping"; Pattern = "*" }
    @{ Name = "zMessageO"; Dest = "zMessageO"; Pattern = "*" }
    @{ Name = "zMultimediaO"; Dest = "zMultimediaO"; Pattern = "*" }
    @{ Name = "zNetwork"; Dest = "zNetwork"; Pattern = "*" }
    @{ Name = "zPackO"; Dest = "zPackO"; Pattern = "*" }
    @{ Name = "zParadataO"; Dest = "zParadataO"; Pattern = "*" }
    @{ Name = "zPlatformO"; Dest = "zPlatformO"; Pattern = "*" }
    @{ Name = "zReformatO"; Dest = "zReformatO"; Pattern = "*" }
    @{ Name = "zReportO"; Dest = "zReportO"; Pattern = "*" }
    @{ Name = "zSortO"; Dest = "zSortO"; Pattern = "*" }
    @{ Name = "zSyncO"; Dest = "zSyncO"; Pattern = "*" }
    @{ Name = "zToolsO"; Dest = "zToolsO"; Pattern = "*" }
    @{ Name = "zUtilF"; Dest = "zUtilF"; Pattern = "*" }
    @{ Name = "zUtilO"; Dest = "zUtilO"; Pattern = "*" }
    @{ Name = "zZipO"; Dest = "zZipO"; Pattern = "*" }
    @{ Name = "zCapiO"; Dest = "zCapiO"; Pattern = "*" }
    
    # WASM-specific files
    @{ Name = "WASM"; Dest = "WASM"; Pattern = "*" }
    
    # Android JNI files needed for web adaptation
    @{ Name = "CSEntryDroid/app/src/main/jni/build-wasm"; Dest = "jni/build-wasm"; Pattern = "*" }
    @{ Name = "CSEntryDroid/app/src/main/jni/src"; Dest = "jni/src"; Pattern = "*" }
    
    # Zentryo (shared headers)
    @{ Name = "Zentryo"; Dest = "Zentryo"; Pattern = "*" }
)

# Copy each library
$TotalFiles = 0
foreach ($Lib in $Libraries) {
    $SrcPath = Join-Path $CSProRoot $Lib.Name
    $DstPath = Join-Path $DestRoot $Lib.Dest
    
    if (-not (Test-Path $SrcPath)) {
        Write-Host "  Skipping (not found): $($Lib.Name)" -ForegroundColor DarkGray
        continue
    }
    
    # Create destination directory
    New-Item -ItemType Directory -Path $DstPath -Force | Out-Null
    
    # Copy files recursively, excluding build artifacts
    $Files = Get-ChildItem $SrcPath -Recurse -File | Where-Object {
        $_.Extension -match '\.(cpp|c|h|hpp|inl|cmake|txt|json)$' -or
        $_.Name -match '^CMakeLists\.txt$' -or
        ($Lib.Pattern -eq "*" -and $_.Extension -notmatch '\.(obj|o|a|lib|pdb|ilk|exp|dll|exe|wasm|js|data)$')
    } | Where-Object {
        # Exclude build directories
        $_.FullName -notmatch '\\(build|Debug|Release|x64|\.vs|CMakeFiles)\\'
    }
    
    $FileCount = 0
    $SrcPathNormalized = (Resolve-Path $SrcPath).Path
    foreach ($File in $Files) {
        $FilePathNormalized = (Resolve-Path $File.FullName).Path
        if ($FilePathNormalized.Length -le $SrcPathNormalized.Length) {
            continue
        }
        $RelPath = $FilePathNormalized.Substring($SrcPathNormalized.Length).TrimStart('\', '/')
        if ([string]::IsNullOrEmpty($RelPath)) {
            continue
        }
        $DstFile = Join-Path $DstPath $RelPath
        $DstDir = Split-Path $DstFile -Parent
        
        if (-not (Test-Path $DstDir)) {
            New-Item -ItemType Directory -Path $DstDir -Force | Out-Null
        }
        
        Copy-Item $File.FullName $DstFile -Force
        $FileCount++
        
        if ($Verbose) {
            Write-Host "    $RelPath" -ForegroundColor DarkGray
        }
    }
    
    Write-Host "  Copied: $($Lib.Dest) ($FileCount files)" -ForegroundColor Green
    $TotalFiles += $FileCount
}

# Create a unified stdafx.h for WASM builds
$StdAfxContent = @'
// stdafx.h - Unified precompiled header for CSPro WASM build
// This file provides all common includes needed by CSPro libraries

#pragma once

// Standard system includes
#include <engine/StandardSystemIncludes.h>

// Core CSPro includes
#include <zToolsO/Utf8Convert.h>
#include <zToolsO/Serializer.h>
#include <zUtilO/Interapp.h>
#include <zJson/Json.h>
#include <zJson/JsonSpecFile.h>

#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#include <emscripten/bind.h>
#endif
'@

$StdAfxPath = Join-Path $DestRoot "stdafx.h"
Set-Content -Path $StdAfxPath -Value $StdAfxContent
Write-Host "  Created: stdafx.h (unified)" -ForegroundColor Green

Write-Host ""
Write-Host "=== Setup Complete ===" -ForegroundColor Cyan
Write-Host "Total files copied: $TotalFiles"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Update CMakeLists.txt to use src/ folder"
Write-Host "  2. Run: cd build && emcmake cmake .. -G Ninja && ninja"
