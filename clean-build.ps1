#!/usr/bin/env pwsh
# Clean build script for CSEntry Web KMP
# Ensures all caches are cleared before building

Write-Host "CSEntry Web KMP - Clean Build Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Stop Gradle daemon
Write-Host "`nStopping Gradle daemon..." -ForegroundColor Yellow
& .\gradlew --stop

# Kill any Java/Kotlin processes
Write-Host "Killing Java/Kotlin processes..." -ForegroundColor Yellow
Stop-Process -Name java, kotlin -Force -ErrorAction SilentlyContinue

# Remove all build artifacts and caches
Write-Host "Removing build artifacts and caches..." -ForegroundColor Yellow
$foldersToRemove = @(
    "build",
    ".gradle\kotlin",
    ".gradle\7.6",
    ".gradle\8.0",
    ".kotlin"
)

foreach ($folder in $foldersToRemove) {
    if (Test-Path $folder) {
        Write-Host "  Removing $folder..." -ForegroundColor Gray
        Remove-Item -Recurse -Force $folder -ErrorAction SilentlyContinue
    }
}

# Run clean
Write-Host "`nRunning gradle clean..." -ForegroundColor Yellow
& .\gradlew clean

# Compile
Write-Host "`nCompiling Kotlin/Wasm..." -ForegroundColor Yellow
& .\gradlew compileKotlinWasmJs

# Count errors
Write-Host "`nCounting compilation errors..." -ForegroundColor Yellow
$errors = & .\gradlew compileKotlinWasmJs 2>&1 | Select-String "^e: "
$errorCount = ($errors | Measure-Object).Count

if ($errorCount -eq 0) {
    Write-Host "`n✓ BUILD SUCCESSFUL - No errors!" -ForegroundColor Green
} else {
    Write-Host "`n✗ BUILD FAILED - $errorCount errors found" -ForegroundColor Red
    Write-Host "`nFirst 20 errors:" -ForegroundColor Yellow
    $errors | Select-Object -First 20 | ForEach-Object {
        Write-Host $_.Line -ForegroundColor Red
    }
}

Write-Host "`nDone!" -ForegroundColor Cyan
