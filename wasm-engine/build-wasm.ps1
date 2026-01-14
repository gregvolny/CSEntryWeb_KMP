# build-wasm.ps1 - Build CSEntry KMP WASM Engine
# Compiles the FULL Android C++ CSPro engine to WebAssembly for Kotlin Multiplatform
#
# This script:
# 1. Activates the Emscripten SDK
# 2. Configures CMake with emcmake
# 3. Builds with Ninja
# 4. Copies output to public/wasm for serving

param(
    [switch]$Clean,
    [switch]$Debug,
    [switch]$Verbose,
    [switch]$NoCopy
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  CSEntry KMP WASM Engine Builder" -ForegroundColor Cyan
Write-Host "  Full Android C++ Engine -> WebAssembly" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# Get script directory (wasm-engine folder)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

Write-Host "Project root: $projectRoot" -ForegroundColor Yellow
Write-Host "WASM engine dir: $scriptDir" -ForegroundColor Yellow

# Check for Emscripten SDK
$emsdkPaths = @(
    "C:\emsdk",
    "$env:USERPROFILE\emsdk",
    "$env:EMSDK"
)

$emsdkPath = $null
foreach ($path in $emsdkPaths) {
    if ($path -and (Test-Path "$path\emsdk_env.ps1")) {
        $emsdkPath = $path
        break
    }
}

if (-not $emsdkPath) {
    Write-Host "ERROR: Emscripten SDK not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Emscripten SDK:" -ForegroundColor Yellow
    Write-Host "  1. git clone https://github.com/emscripten-core/emsdk.git C:\emsdk" -ForegroundColor White
    Write-Host "  2. cd C:\emsdk" -ForegroundColor White
    Write-Host "  3. .\emsdk install latest" -ForegroundColor White
    Write-Host "  4. .\emsdk activate latest" -ForegroundColor White
    Write-Host ""
    Write-Host "See: https://emscripten.org/docs/getting_started/downloads.html" -ForegroundColor Yellow
    exit 1
}

Write-Host "OK: Found Emscripten SDK at: $emsdkPath" -ForegroundColor Green

# Activate Emscripten environment
Write-Host "`nActivating Emscripten environment..." -ForegroundColor Yellow
Push-Location $emsdkPath
try {
    $env:EMSDK_QUIET = "1"
    . "$emsdkPath\emsdk_env.ps1"
} catch {
    Write-Host "Warning: emsdk_env.ps1 had issues, trying to continue..." -ForegroundColor Yellow
}
Pop-Location

# Verify emcc is available
$emcc = Get-Command emcc -ErrorAction SilentlyContinue
if (-not $emcc) {
    Write-Host "ERROR: emcc not found in PATH after activation!" -ForegroundColor Red
    Write-Host "Try running: $emsdkPath\emsdk_env.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "OK: Emscripten compiler: $($emcc.Source)" -ForegroundColor Green

# Build directory
$buildDir = Join-Path $scriptDir "build"

# Clean if requested
if ($Clean) {
    if (Test-Path $buildDir) {
        Write-Host "`nCleaning build directory..." -ForegroundColor Yellow
        Remove-Item $buildDir -Recurse -Force
    }
}

# Create build directory
if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir | Out-Null
}

Set-Location $buildDir

# Configure with CMake
Write-Host "`n------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Configuring with Emscripten CMake..." -ForegroundColor Cyan
Write-Host "------------------------------------------------------" -ForegroundColor Cyan

$buildType = if ($Debug) { "Debug" } else { "Release" }
Write-Host "Build type: $buildType" -ForegroundColor Yellow

# Run emcmake cmake
$cmakeArgs = @(
    "..",
    "-G", "Ninja",
    "-DCMAKE_BUILD_TYPE=$buildType"
)

if ($Verbose) {
    $cmakeArgs += "-DCMAKE_VERBOSE_MAKEFILE=ON"
}

Write-Host "Running: emcmake cmake $($cmakeArgs -join ' ')" -ForegroundColor Gray
& emcmake cmake @cmakeArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: CMake configuration failed!" -ForegroundColor Red
    Set-Location $scriptDir
    exit $LASTEXITCODE
}

Write-Host "OK: CMake configuration complete" -ForegroundColor Green

# Build
Write-Host "`n------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Building with Ninja..." -ForegroundColor Cyan
Write-Host "------------------------------------------------------" -ForegroundColor Cyan

$ninjaArgs = @("-j4")  # 4 parallel jobs
if ($Verbose) {
    $ninjaArgs += "-v"
}

& ninja @ninjaArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Build failed!" -ForegroundColor Red
    Set-Location $scriptDir
    exit $LASTEXITCODE
}

# Check output files
Write-Host "`n======================================================" -ForegroundColor Green
Write-Host "  Build Completed Successfully!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green

Write-Host "`nOutput files:" -ForegroundColor Cyan
$outputFiles = @(
    @{ Name = "csentryKMP.js"; Required = $true },
    @{ Name = "csentryKMP.wasm"; Required = $true },
    @{ Name = "csentryKMP.data"; Required = $false }
)

$allRequired = $true
foreach ($file in $outputFiles) {
    $filePath = Join-Path $buildDir $file.Name
    if (Test-Path $filePath) {
        $size = [math]::Round((Get-Item $filePath).Length / 1MB, 2)
        Write-Host "  OK  $($file.Name) ($size MB)" -ForegroundColor Green
    } else {
        if ($file.Required) {
            Write-Host "  ERR $($file.Name) (MISSING - REQUIRED)" -ForegroundColor Red
            $allRequired = $false
        } else {
            Write-Host "  - $($file.Name) (not generated)" -ForegroundColor Gray
        }
    }
}

if (-not $allRequired) {
    Write-Host "`nERROR: Required output files are missing!" -ForegroundColor Red
    Set-Location $scriptDir
    exit 1
}

# Copy to public folder for serving
if (-not $NoCopy) {
    $publicDir = Join-Path $projectRoot "public\wasm"
    Write-Host "`nCopying to: $publicDir" -ForegroundColor Cyan

    if (-not (Test-Path $publicDir)) {
        New-Item -ItemType Directory -Path $publicDir -Force | Out-Null
    }

    foreach ($file in $outputFiles) {
        $srcPath = Join-Path $buildDir $file.Name
        if (Test-Path $srcPath) {
            Copy-Item $srcPath -Destination $publicDir -Force
            Write-Host "  OK  Copied: $($file.Name)" -ForegroundColor Green
        }
    }
    
    Write-Host "`nOK: WASM engine ready at: $publicDir" -ForegroundColor Green
}

# Return to original directory
Set-Location $scriptDir

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Next Steps:" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  1. Update server.js to serve csentryKMP.* files" -ForegroundColor White
Write-Host "  2. Update CSProEngineModule.kt to import csentryKMP.js" -ForegroundColor White
Write-Host "  3. Run: node server.js" -ForegroundColor White
Write-Host ""
