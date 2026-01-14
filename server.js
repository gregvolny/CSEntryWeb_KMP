/**
 * Development server for CSEntry Web (Kotlin/Wasm)
 * Serves the Kotlin/Wasm application with proper MIME types and CORS headers
 */

import express from 'express';
import cors from 'cors';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3002;

// Enable CORS
app.use(cors());

// Parse JSON bodies for logging endpoint
app.use(express.json());

// Create logs directory if it doesn't exist
const logsDir = join(__dirname, 'logs');
if (!fs.existsSync(logsDir)) {
    fs.mkdirSync(logsDir, { recursive: true });
}

// Get current log file path (new file per day)
function getLogFilePath() {
    const date = new Date().toISOString().split('T')[0];
    return join(logsDir, `csentry-${date}.log`);
}

// Server-side logging endpoint
app.post('/api/log', (req, res) => {
    const { level, message, timestamp, data } = req.body;
    const logEntry = {
        timestamp: timestamp || new Date().toISOString(),
        level: level || 'INFO',
        message: message || '',
        data: data || null
    };
    
    const logLine = JSON.stringify(logEntry) + '\n';
    
    // Write to log file
    fs.appendFile(getLogFilePath(), logLine, (err) => {
        if (err) {
            console.error('[Server] Failed to write log:', err);
        }
    });
    
    // Also log to console with color coding
    const colors = {
        'ERROR': '\x1b[31m',   // Red
        'WARN': '\x1b[33m',    // Yellow
        'INFO': '\x1b[36m',    // Cyan
        'DEBUG': '\x1b[90m',   // Gray
    };
    const reset = '\x1b[0m';
    const color = colors[logEntry.level] || '';
    console.log(`${color}[${logEntry.level}]${reset} ${logEntry.message}`);
    
    res.json({ ok: true });
});

// Batch logging endpoint
app.post('/api/log/batch', (req, res) => {
    const { logs } = req.body;
    if (!Array.isArray(logs)) {
        return res.status(400).json({ error: 'logs must be an array' });
    }
    
    const logLines = logs.map(log => JSON.stringify({
        timestamp: log.timestamp || new Date().toISOString(),
        level: log.level || 'INFO',
        message: log.message || '',
        data: log.data || null
    })).join('\n') + '\n';
    
    fs.appendFile(getLogFilePath(), logLines, (err) => {
        if (err) {
            console.error('[Server] Failed to write batch logs:', err);
        }
    });
    
    res.json({ ok: true, count: logs.length });
});

// View logs endpoint
app.get('/api/logs', (req, res) => {
    const logFile = getLogFilePath();
    if (!fs.existsSync(logFile)) {
        return res.json({ logs: [], message: 'No logs for today' });
    }
    
    const content = fs.readFileSync(logFile, 'utf8');
    const logs = content.trim().split('\n').filter(line => line).map(line => {
        try {
            return JSON.parse(line);
        } catch {
            return { raw: line };
        }
    });
    
    // Optional filtering by level
    const level = req.query.level;
    const filteredLogs = level 
        ? logs.filter(log => log.level === level.toUpperCase())
        : logs;
    
    res.json({ logs: filteredLogs, count: filteredLogs.length });
});

// Clear logs endpoint
app.delete('/api/logs', (req, res) => {
    const logFile = getLogFilePath();
    if (fs.existsSync(logFile)) {
        fs.unlinkSync(logFile);
    }
    res.json({ ok: true, message: 'Logs cleared' });
});

// Set proper MIME types for WASM and JS files
app.use((req, res, next) => {
    if (req.url.endsWith('.wasm')) {
        res.type('application/wasm');
    } else if (req.url.endsWith('.mjs')) {
        res.type('application/javascript');
    } else if (req.url.endsWith('.js')) {
        res.type('application/javascript');
    } else if (req.url.endsWith('.data')) {
        res.type('application/octet-stream');
    }
    
    // Required headers for SharedArrayBuffer (if needed)
    res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
    res.setHeader('Cross-Origin-Embedder-Policy', 'require-corp');
    
    // Allow resources to be loaded from srcdoc iframes and cross-origin contexts
    res.setHeader('Cross-Origin-Resource-Policy', 'cross-origin');

    // Development convenience: avoid stale JS/WASM being cached.
    // This server is primarily used for local iteration and debugging.
    res.setHeader('Cache-Control', 'no-store');
    
    next();
});

// Serve static files from public directory (if exists)
app.use(express.static(join(__dirname, 'public')));

// Compatibility routes for native CSPro HTML assets.
// Native viewer pages expect these at the site root.
app.use('/external', express.static(join(__dirname, 'public/dialogs/external')));
app.use('/css', express.static(join(__dirname, 'public/dialogs/css')));

// Serve SQLite WASM explicitly (official sqlite-wasm from sqlite.org)
app.use('/sqlite3', express.static(join(__dirname, 'public/sqlite3')));

// Serve CSEntry KMP WASM engine files from public/wasm but make them available at root
// to satisfy direct imports or webpack resolutions
app.use('/csentryKMP.js', express.static(join(__dirname, 'public/wasm/csentryKMP.js')));
app.use('/csentryKMP.wasm', express.static(join(__dirname, 'public/wasm/csentryKMP.wasm')));
app.use('/csentryKMP.data', express.static(join(__dirname, 'public/wasm/csentryKMP.data')));

// Serve Kotlin/Wasm webpack build output (main application files)
// Try development build first, then production
const devWebpackOutput = join(__dirname, 'build/kotlin-webpack/wasmJs/developmentExecutable');
const prodWebpackOutput = join(__dirname, 'build/kotlin-webpack/wasmJs/productionExecutable');
const webpackOutput = fs.existsSync(devWebpackOutput) ? devWebpackOutput : prodWebpackOutput;
console.log(`Using webpack output from: ${webpackOutput}`);
app.use(express.static(webpackOutput));

// When the Kotlin/Wasm UI is loaded from /wasm-ui/, some runtime fetches may resolve
// companion assets (e.g. csentry-web.wasm) relative to the current page URL.
// Serve the same build output under /wasm-ui to avoid 404s.
app.use('/wasm-ui', express.static(webpackOutput));

// Serve direct compilation output (where .wasm files live for Kotlin/Wasm)
const compileSyncOutput = join(__dirname, 'build/compileSync/wasmJs/main/developmentExecutable/kotlin');
if (fs.existsSync(compileSyncOutput)) {
    console.log(`Serving compilation output from: ${compileSyncOutput}`);
    app.use(express.static(compileSyncOutput));

    // Same rationale as above: allow /wasm-ui/csentry-web.wasm to resolve.
    app.use('/wasm-ui', express.static(compileSyncOutput));
}


// ============================================================
// CSEntry KMP WASM Engine (Full Android C++ Engine)
// ============================================================
// Serve csentryKMP.js, csentryKMP.wasm, csentryKMP.data from public/wasm
const csentryKmpPath = join(__dirname, 'public/wasm');

// Serve at /wasm/ path
app.use('/wasm', express.static(csentryKmpPath));

// Also serve at root for direct imports
app.get('/csentryKMP.js', (req, res) => {
    const filePath = join(csentryKmpPath, 'csentryKMP.js');
    if (fs.existsSync(filePath)) {
        res.sendFile(filePath);
    } else {
        res.status(404).send('csentryKMP.js not found. Run: .\\wasm-engine\\build-wasm.ps1');
    }
});
app.get('/csentryKMP.wasm', (req, res) => {
    const filePath = join(csentryKmpPath, 'csentryKMP.wasm');
    if (fs.existsSync(filePath)) {
        res.sendFile(filePath);
    } else {
        res.status(404).send('csentryKMP.wasm not found. Run: .\\wasm-engine\\build-wasm.ps1');
    }
});
app.get('/csentryKMP.data', (req, res) => {
    const filePath = join(csentryKmpPath, 'csentryKMP.data');
    if (fs.existsSync(filePath)) {
        res.sendFile(filePath);
    } else {
        res.status(404).send('csentryKMP.data not found (optional assets file)');
    }
});

// Legacy CSPro.js routes (fallback to CSEntryWeb folder if needed)
const csproWasmPath = join(__dirname, '..', 'CSEntryWeb');
app.get('/CSPro.js', (req, res) => {
    res.sendFile(join(csproWasmPath, 'CSPro.js'));
});
app.get('/CSPro.wasm', (req, res) => {
    res.sendFile(join(csproWasmPath, 'CSPro.wasm'));
});
app.get('/CSPro.data', (req, res) => {
    res.sendFile(join(csproWasmPath, 'CSPro.data'));
});

// Integration verification endpoint
app.get('/api/verify-integration', (req, res) => {
    const checks = {
        timestamp: new Date().toISOString(),
        // CSEntry KMP WASM Engine (primary)
        csentryKmpJsExists: fs.existsSync(join(csentryKmpPath, 'csentryKMP.js')),
        csentryKmpWasmExists: fs.existsSync(join(csentryKmpPath, 'csentryKMP.wasm')),
        csentryKmpDataExists: fs.existsSync(join(csentryKmpPath, 'csentryKMP.data')),
        csentryKmpJsSize: fs.existsSync(join(csentryKmpPath, 'csentryKMP.js')) 
            ? fs.statSync(join(csentryKmpPath, 'csentryKMP.js')).size 
            : 0,
        csentryKmpWasmSize: fs.existsSync(join(csentryKmpPath, 'csentryKMP.wasm'))
            ? fs.statSync(join(csentryKmpPath, 'csentryKMP.wasm')).size
            : 0,
        // Kotlin UI WASM
        kotlinWasmExists: fs.existsSync(join(webpackOutput, 'csentry-web.wasm')),
        kotlinJsExists: fs.existsSync(join(webpackOutput, 'csentry-web.js')),
        kotlinWasmSize: fs.existsSync(join(webpackOutput, 'csentry-web.wasm'))
            ? fs.statSync(join(webpackOutput, 'csentry-web.wasm')).size
            : 0,
        // Legacy CSPro (fallback)
        legacyCsproJsExists: fs.existsSync(join(csproWasmPath, 'CSPro.js')),
        legacyCsproWasmExists: fs.existsSync(join(csproWasmPath, 'CSPro.wasm'))
    };
    
    checks.csentryKmpReady = checks.csentryKmpJsExists && checks.csentryKmpWasmExists;
    checks.kotlinUiReady = checks.kotlinWasmExists && checks.kotlinJsExists;
    checks.allReady = checks.csentryKmpReady && checks.kotlinUiReady;
    
    res.json(checks);
});

// Also serve from /wasm-ui path - includes both HTML and WASM files
app.use('/wasm-ui', express.static(join(__dirname, 'public/wasm-ui')));
app.use('/wasm-ui', express.static(webpackOutput));

// Serve C++ WASM files
app.use('/wasm', express.static(join(__dirname, 'build/js/packages/csentry-web/kotlin/wasm')));

// Serve the main application from public/wasm-ui at root as well
// This makes the app accessible at both http://localhost:3002/ and http://localhost:3002/wasm-ui/
app.use(express.static(join(__dirname, 'public/wasm-ui')));

// Fallback: serve index.html for the root route
app.get('/', (req, res) => {
    res.sendFile(join(__dirname, 'public/wasm-ui/index.html'));
});

// Start server
app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║           CSEntry Web - Kotlin/Wasm Server                ║
║                                                            ║
╚════════════════════════════════════════════════════════════╝

Server running at: http://localhost:${PORT}

Endpoints:
  - Main app:        http://localhost:${PORT}/
  - Kotlin/Wasm:     http://localhost:${PORT}/wasm-ui/
  - C++ WASM:        http://localhost:${PORT}/wasm/

Press Ctrl+C to stop the server
`);
});
