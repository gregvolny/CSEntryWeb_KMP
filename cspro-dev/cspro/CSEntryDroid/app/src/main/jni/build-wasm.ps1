# Build CSProAndroid for WebAssembly using Emscripten
# This compiles the CSEntryDroid JNI C++ code to WASM for Kotlin Multiplatform

Write-Host "Building CSProAndroid for WebAssembly..." -ForegroundColor Cyan

# Set Emscripten SDK path
$emsdkPath = "C:\emsdk"
if (-not (Test-Path $emsdkPath)) {
    Write-Host "ERROR: Emscripten SDK not found at $emsdkPath!" -ForegroundColor Red
    exit 1
}

# Activate Emscripten environment
Write-Host "Activating Emscripten environment..." -ForegroundColor Yellow
& "$emsdkPath\emsdk_env.ps1"

Write-Host "Emscripten SDK found at: $emsdkPath" -ForegroundColor Green

# Set working directory
$jniDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $jniDir

# Create build directory
$buildDir = "build-wasm"
if (Test-Path $buildDir) {
    Write-Host "Cleaning existing build directory..." -ForegroundColor Yellow
    Remove-Item $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $buildDir | Out-Null
Set-Location $buildDir

Write-Host "`nConfiguring with Emscripten CMake..." -ForegroundColor Cyan

# Configure with emcmake
& emcmake cmake .. `
    -DCMAKE_BUILD_TYPE=Release `
    -DEMSCRIPTEN=ON

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: CMake configuration failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`nBuilding with Emscripten..." -ForegroundColor Cyan

# Build with emmake
& emmake make -j4

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`n==================================" -ForegroundColor Green
Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green

# List output files
Write-Host "`nOutput files:" -ForegroundColor Cyan
Get-ChildItem -Filter "CSProAndroid.*" | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 2)
    Write-Host "  $($_.Name) ($size MB)" -ForegroundColor White
}

# Copy to project build directory
$targetDir = "..\..\..\..\..\..\build\wasm"
Write-Host "`nCopying outputs to: $targetDir" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
Copy-Item "CSProAndroid.*" -Destination $targetDir -Force

Write-Host "`nDone! WASM files are ready for Kotlin Multiplatform." -ForegroundColor Green
