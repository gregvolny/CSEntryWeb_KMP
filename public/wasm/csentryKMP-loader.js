// CSEntry KMP WASM Module Loader
// This script loads the Emscripten-compiled csentryKMP.js ES module
// and initializes it fully before exposing to the Kotlin bundle

// Helper to log via CSEntryLogger if available, otherwise console
function logToFile(level, message, data) {
    if (typeof window !== 'undefined' && window.logger) {
        const method = level === 'ERROR' ? 'error' : level === 'WARN' ? 'warn' : 'info';
        window.logger[method](message, data);
    } else {
        const consoleMethod = level === 'ERROR' ? console.error : level === 'WARN' ? console.warn : console.log;
        consoleMethod('[WASM]', message, data || '');
    }
}

(async function() {
    try {
        logToFile('INFO', '[csentryKMP-loader] Loading csentryKMP.js ES module...');
        
        // Dynamically import the ES module
        const module = await import('./csentryKMP.js');
        
        // Get the factory function (default export)
        const createCSEntryKMPModule = module.default || module.createCSEntryKMPModule;
        
        if (typeof createCSEntryKMPModule !== 'function') {
            throw new Error('createCSEntryKMPModule not found in module');
        }
        
        logToFile('INFO', '[csentryKMP-loader] Factory function obtained, initializing module...');
        
        // Actually call the factory to initialize the module
        // The engine can emit extremely verbose VFS registration logs (hundreds/thousands of lines).
        // Filter those to avoid the appearance of an "infinite loop" and keep DevTools responsive.
        let vfsRegisteredCount = 0;
        const shouldSuppressVfsLog = (text) => {
            if (typeof text !== 'string') return false;
            // Typical lines: "[WASM-VFS] Registered: cspro-virtual://html/123.html"
            if (text.includes('[WASM-VFS] Registered:')) {
                vfsRegisteredCount++;
                // Keep a tiny sample + periodic progress.
                if (vfsRegisteredCount <= 5) return false;
                if (vfsRegisteredCount % 250 === 0) {
                    logToFile('INFO', `[WASM-VFS] Registered: (${vfsRegisteredCount} total so far)`);
                }
                return true;
            }
            return false;
        };

        const moduleConfig = {
            onRuntimeInitialized: function() {
                logToFile('INFO', '[csentryKMP-loader] ✅ Emscripten runtime initialized!');
                logToFile('INFO', '[csentryKMP-loader] CSProEngine available: ' + (typeof this.CSProEngine));
            },
            print: function(text) {
                if (shouldSuppressVfsLog(text)) return;
                logToFile('INFO', text);
            },
            printErr: function(text) { logToFile('ERROR', text); },
            onAbort: function(what) { logToFile('ERROR', '[WASM ABORT] ' + what); },
            locateFile: function(path, prefix) {
                logToFile('INFO', '[csentryKMP-loader] locateFile: ' + path);
                // Files are in the same /wasm/ directory
                return '/wasm/' + path;
            }
        };
        
        logToFile('INFO', '[csentryKMP-loader] Calling factory with config...');
        const wasmModule = await createCSEntryKMPModule(moduleConfig);
        
        logToFile('INFO', '[csentryKMP-loader] Factory returned, module type: ' + (typeof wasmModule));
        logToFile('INFO', '[csentryKMP-loader] Module keys: ' + JSON.stringify(Object.keys(wasmModule || {}).slice(0, 30)));
        logToFile('INFO', '[csentryKMP-loader] CSProEngine: ' + (typeof wasmModule?.CSProEngine));
        logToFile('INFO', '[csentryKMP-loader] ccall: ' + (typeof wasmModule?.ccall));
        logToFile('INFO', '[csentryKMP-loader] FS: ' + (typeof wasmModule?.FS));
        
        // Expose the initialized module globally
        window.CSProModule = wasmModule;
        window.createCSEntryKMPModule = createCSEntryKMPModule;
        self.createCSEntryKMPModule = createCSEntryKMPModule;
        
        // Also expose as a module-like object
        window.csentryKMPModule = {
            default: createCSEntryKMPModule,
            createCSEntryKMPModule: createCSEntryKMPModule,
            module: wasmModule
        };
        
        logToFile('INFO', '[csentryKMP-loader] ✅ Module initialized and exposed globally');
        
        // Dispatch event to notify when ready
        window.dispatchEvent(new CustomEvent('csentryKMPReady', { 
            detail: { createCSEntryKMPModule, module: wasmModule } 
        }));
        
    } catch (error) {
        logToFile('ERROR', '[csentryKMP-loader] ❌ Failed to load/initialize csentryKMP.js: ' + error.message, { stack: error.stack });
        window.csentryKMPError = error;
    }
})();
