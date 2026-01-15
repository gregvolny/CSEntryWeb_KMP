# Fix WasmEngineInterface.kt corruption
$file = "src\wasmJsMain\kotlin\gov\census\cspro\engine\WasmEngineInterface.kt"
$content = Get-Content $file -Raw

# Fix all duplicate "val result" patterns
# Pattern: val result = X().await<...>().unsafeCast<Boolean>()val result = X(params).await<...>()
# Keep: val result = X(params).await<...>().unsafeCast<Boolean>()

$content = $content -replace 'val result = csproModule\.InitApplication\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.InitApplication\(pffFilename\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.InitApplication(pffFilename).await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

$content = $content -replace 'val result = csproModule\.Start\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.Start\(\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.Start().await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

$content = $content -replace 'val result = csproModule\.InsertOcc\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.InsertOcc\(\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.InsertOcc().await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

$content = $content -replace 'val result = csproModule\.DeleteOcc\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.DeleteOcc\(\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.DeleteOcc().await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

$content = $content -replace 'val result = csproModule\.InsertOccAfter\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.InsertOccAfter\(\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.InsertOccAfter().await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

$content = $content -replace 'val result = csproModule\.InsertCase\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.InsertCase\(0\.0\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.InsertCase(0.0).await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

$content = $content -replace 'val result = csproModule\.SetFieldValue\(\)\.await<kotlin\.js\.JsAny\?>(\)\.unsafeCast<Boolean>\(\))?val result = csproModule\.SetFieldValue\(fieldName,\s*value\)\.await<kotlin\.js\.JsAny\?\(\)>', 'val result = csproModule.SetFieldValue(fieldName, value).await<kotlin.js.JsAny?>().unsafeCast<Boolean>()'

# Fix duplicate "if (result)" patterns
$content = $content -replace 'if \(result\)if \(result\)', 'if (result)'

# Fix duplicate conditions in if statements
$content = $content -replace 'if \(result\)\s*==\s*result\)', 'if (result)'
$content = $content -replace 'if \(result\s*\|\|\s*result\)', 'if (result)'

Write-Host "Fixed WasmEngineInterface.kt duplicates"
Set-Content $file $content -NoNewline
