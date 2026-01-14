# SQLite WASM and Dialog Changes

## Overview

This document describes the changes made to implement:
1. **SQLite OPFS Storage** - Using official sqlite-wasm from sqlite.org instead of sql.js
2. **CSPro HTML Dialogs** - Replacing native `alert()` calls with CSPro HTML dialog system

## Changes Made

### 1. SQLite Settings Store (`sqlite-settings-store.js`)

**Location:** Both `CSEntryWeb_KMP/public/` and `CSEntryWeb/web/`

**Changes:**
- Switched from sql.js to official SQLite WASM (https://sqlite.org/wasm)
- Uses OPFS VFS for persistence when available
- Falls back to in-memory database if OPFS not available
- Creates CommonStore.db with tables: UserSettings, Configurations, PersistentVariables, Credentials

**Key Features:**
```javascript
// Uses official SQLite WASM with OPFS VFS
const SqliteSettingsStore = {
    initialize() // Load/create SQLite database in OPFS
    getValue(key, source) // Get value from table
    putValue(key, value, source) // Store value in table
    deleteValue(key, source) // Delete value
    getAllKeys(source) // Get all keys from table
    getAll(source) // Get all key-value pairs
    clear(source) // Clear table
    getInfo() // Get database info for debugging
}
```

### 2. CSPro Dialog Integration

**Added to `sqlite-settings-store.js`:**
```javascript
const CSProDialogIntegration = {
    async showAlert(message, title = 'CSPro')
    async showError(message, title = 'Error')
}
```

**Features:**
- First tries CSProDialogHandler.showModalDialogAsync()
- Then tries CSProActionInvoker UI.alertAsync()
- Falls back to console.log() (NEVER uses native alert)

### 3. Updated Files

#### `action-invoker.js` (both versions)
**UI.alert and UI.alertAsync methods:**
- Now use CSProDialogHandler or CSProDialogIntegration
- Never calls native `alert()`

```javascript
UI.alert: (args) => {
    if (typeof window.CSProDialogHandler !== 'undefined') {
        window.CSProDialogHandler.showModalDialogAsync(title, message, 0);
    } else if (typeof window.CSProDialogIntegration !== 'undefined') {
        window.CSProDialogIntegration.showAlert(message, title);
    } else {
        console.log(`[CSPro Alert] ${title ? title + ': ' : ''}${message}`);
    }
}
```

#### `cspro-runtime.js`
**showModalDialogAsync fallback:**
- Uses CSProDialogIntegration instead of native alert()

#### `CSPro.js` (WASM build)
**Updated functions:**
- `ASM_CONSTS[1415363]` - Error handler now uses CSProDialogIntegration
- `__asyncjs__jspi_showDialog` - errmsg handling uses CSProDialogIntegration
- `__asyncjs__jspi_showModalDialog` - MB_OK handling uses CSProDialogIntegration

### 4. Kotlin Changes

#### `CredentialStoreWasm.kt`
**Simplified implementation:**
- Uses localStorage directly for synchronous access
- SQLite synchronization handled at JavaScript layer
- Removed problematic Kotlin/WASM interop code

#### `QuestionWidgetMedia.kt`
**Fixed js() calls:**
- Replaced `js("alert(...)")` with `showCSProAlert()` function
- Fixed setInterval return type issue

#### `QuestionWidgetFactory.kt`
**Fixed import:**
- Changed `CaptureType as DataCaptureType` to just `CaptureType`
- Updated all references accordingly

## SQLite WASM Details

### Official SQLite WASM (sqlite.org)
- **CDN Source:** `https://cdn.jsdelivr.net/npm/@aspect-build/aspect-sqlite3-wasm`
- **VFS Options:**
  - `OpfsDb` - OPFS-based persistence (requires OPFS support)
  - `JsStorageDb` - localStorage-based (kvvfs)
  - In-memory - Fallback when persistence not available

### Database Schema
```sql
-- UserSettings table (for loadsetting/savesetting)
CREATE TABLE UserSettings (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT
);

-- Configurations table (for config variables)
CREATE TABLE Configurations (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT
);

-- PersistentVariables table
CREATE TABLE PersistentVariables (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT
);

-- Credentials table (for credential storage)
CREATE TABLE Credentials (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT
);
```

## Testing

### Verify SQLite Initialization
```javascript
// Check SQLite store status
console.log(window.sqliteSettingsStore.getInfo());

// Test storage
await sqliteSettingsStore.putValue('test_key', 'test_value', 'UserSettings');
const value = await sqliteSettingsStore.getValue('test_key', 'UserSettings');
console.log('Retrieved:', value);
```

### Verify Dialog Integration
```javascript
// Test alert dialog
await CSProDialogIntegration.showAlert('Test message', 'Test Title');

// Test error dialog
await CSProDialogIntegration.showError('Error message', 'Error');
```

## Known Issues

### Pre-existing Kotlin/WASM Compilation Errors
The following files have pre-existing errors unrelated to these changes:
- `BarcodeScanner.kt` - Dynamic type issues, js() call placement
- `GpsReader.kt` - Unresolved references (format, toLocalDateTime)
- `EntryModels.kt` - Redeclaration issues
- `IEngineInterface.kt` - Unresolved references

These require separate fixes for Kotlin/WASM interop compatibility.

## Migration Notes

1. **Old sql.js code** - No longer used
2. **Native alert() calls** - All replaced with CSProDialogIntegration
3. **localStorage** - Still used as synchronous fallback and for CredentialStore
4. **OPFS persistence** - Requires browser support for OPFS API

## Browser Compatibility

| Feature | Chrome | Firefox | Safari | Edge |
|---------|--------|---------|--------|------|
| OPFS | ✅ 86+ | ✅ 111+ | ✅ 15.2+ | ✅ 86+ |
| SQLite WASM | ✅ | ✅ | ✅ | ✅ |
| localStorage | ✅ | ✅ | ✅ | ✅ |
