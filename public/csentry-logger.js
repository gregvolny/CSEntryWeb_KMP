/**
 * CSEntry Client-Side Logger
 * Intercepts ALL console.log/warn/error/info calls and sends them to the server
 * for persistent storage in log files.
 * 
 * This script MUST be loaded before any other scripts to capture all logs.
 */
(function(global) {
    'use strict';
    
    const LOG_ENDPOINT = '/api/log';
    const BATCH_ENDPOINT = '/api/log/batch';
    const FLUSH_INTERVAL = 1000; // ms - flush every second
    const MAX_BATCH_SIZE = 100;
    
    let logBuffer = [];
    let flushTimer = null;
    let isEnabled = true;
    let isIntercepting = false; // Prevent recursion
    
    // Save original console methods BEFORE overriding
    const originalConsole = {
        log: console.log.bind(console),
        info: console.info.bind(console),
        warn: console.warn.bind(console),
        error: console.error.bind(console),
        debug: console.debug.bind(console)
    };
    
    const LOG_LEVELS = {
        DEBUG: 'DEBUG',
        INFO: 'INFO',
        WARN: 'WARN',
        ERROR: 'ERROR'
    };
    
    /**
     * Convert arguments to a string message
     */
    function argsToString(args) {
        return Array.from(args).map(arg => {
            if (arg === null) return 'null';
            if (arg === undefined) return 'undefined';
            if (typeof arg === 'string') return arg;
            if (typeof arg === 'number' || typeof arg === 'boolean') return String(arg);
            if (arg instanceof Error) return arg.message + (arg.stack ? '\n' + arg.stack : '');
            try {
                return JSON.stringify(arg, null, 0);
            } catch (e) {
                return String(arg);
            }
        }).join(' ');
    }
    
    /**
     * Format a log entry
     */
    function formatLogEntry(level, message, data) {
        return {
            timestamp: new Date().toISOString(),
            level: level,
            message: String(message).substring(0, 10000), // Limit message size
            data: data || null,
            source: 'client',
            url: window.location.pathname
        };
    }
    
    /**
     * Send logs to server
     */
    function sendToServer(logs) {
        if (!isEnabled || logs.length === 0) return;
        
        const endpoint = logs.length === 1 ? LOG_ENDPOINT : BATCH_ENDPOINT;
        const body = logs.length === 1 ? logs[0] : { logs: logs };
        
        try {
            fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
                keepalive: true
            }).catch(() => {
                // Silently fail
            });
        } catch (e) {
            // Silently fail
        }
    }
    
    /**
     * Flush buffered logs to server
     */
    function flush() {
        if (logBuffer.length === 0) return;
        
        const logsToSend = logBuffer.splice(0, MAX_BATCH_SIZE);
        sendToServer(logsToSend);
        
        if (logBuffer.length > 0) {
            scheduleFlush();
        }
    }
    
    /**
     * Schedule a flush
     */
    function scheduleFlush() {
        if (flushTimer) return;
        flushTimer = setTimeout(() => {
            flushTimer = null;
            flush();
        }, FLUSH_INTERVAL);
    }
    
    /**
     * Core log function - adds to buffer and schedules send
     */
    function logToFile(level, message, data) {
        if (isIntercepting) return; // Prevent recursion
        
        const entry = formatLogEntry(level, message, data);
        logBuffer.push(entry);
        
        // Immediate flush for errors
        if (level === 'ERROR') {
            flush();
        } else {
            scheduleFlush();
        }
    }
    
    /**
     * Create intercepted console method
     */
    function createInterceptor(level, originalMethod) {
        return function(...args) {
            // Always call original console method first
            originalMethod.apply(console, args);
            
            // Then log to file
            if (!isIntercepting) {
                isIntercepting = true;
                try {
                    const message = argsToString(args);
                    logToFile(level, message);
                } finally {
                    isIntercepting = false;
                }
            }
        };
    }
    
    // Override console methods to intercept ALL logs
    console.log = createInterceptor(LOG_LEVELS.INFO, originalConsole.log);
    console.info = createInterceptor(LOG_LEVELS.INFO, originalConsole.info);
    console.warn = createInterceptor(LOG_LEVELS.WARN, originalConsole.warn);
    console.error = createInterceptor(LOG_LEVELS.ERROR, originalConsole.error);
    console.debug = createInterceptor(LOG_LEVELS.DEBUG, originalConsole.debug);
    
    /**
     * Public API for explicit logging
     */
    const CSEntryLogger = {
        debug: function(message, data) { 
            originalConsole.debug('[DEBUG]', message, data || '');
            logToFile(LOG_LEVELS.DEBUG, message, data); 
        },
        info: function(message, data) { 
            originalConsole.info('[INFO]', message, data || '');
            logToFile(LOG_LEVELS.INFO, message, data); 
        },
        warn: function(message, data) { 
            originalConsole.warn('[WARN]', message, data || '');
            logToFile(LOG_LEVELS.WARN, message, data); 
        },
        error: function(message, data) { 
            originalConsole.error('[ERROR]', message, data || '');
            logToFile(LOG_LEVELS.ERROR, message, data); 
        },
        
        log: function(level, message, data) { 
            logToFile(level || LOG_LEVELS.INFO, message, data); 
        },
        
        flush: flush,
        enable: function() { isEnabled = true; },
        disable: function() { isEnabled = false; },
        
        // Category-specific loggers
        qsf: function(action, details) {
            const msg = `[QSF] ${action}`;
            originalConsole.log(msg, details || '');
            logToFile(LOG_LEVELS.INFO, msg, details);
        },
        field: function(fieldName, action, details) {
            const msg = `[FIELD:${fieldName}] ${action}`;
            originalConsole.log(msg, details || '');
            logToFile(LOG_LEVELS.INFO, msg, details);
        },
        page: function(action, details) {
            const msg = `[PAGE] ${action}`;
            originalConsole.log(msg, details || '');
            logToFile(LOG_LEVELS.INFO, msg, details);
        },
        engine: function(action, details) {
            const msg = `[ENGINE] ${action}`;
            originalConsole.log(msg, details || '');
            logToFile(LOG_LEVELS.INFO, msg, details);
        },
        
        // Access to original console (for debugging the logger itself)
        _original: originalConsole
    };
    
    // Flush on page unload
    window.addEventListener('beforeunload', flush);
    window.addEventListener('pagehide', flush);
    window.addEventListener('visibilitychange', function() {
        if (document.visibilityState === 'hidden') {
            flush();
        }
    });
    
    // Capture unhandled errors
    window.addEventListener('error', function(event) {
        logToFile(LOG_LEVELS.ERROR, 'Unhandled error: ' + event.message, {
            filename: event.filename,
            lineno: event.lineno,
            colno: event.colno,
            stack: event.error?.stack
        });
    });
    
    // Capture unhandled promise rejections
    window.addEventListener('unhandledrejection', function(event) {
        logToFile(LOG_LEVELS.ERROR, 'Unhandled promise rejection: ' + String(event.reason), {
            stack: event.reason?.stack
        });
    });
    
    // Expose globally
    global.CSEntryLogger = CSEntryLogger;
    global.logger = CSEntryLogger;
    
    // Log initialization (using original to avoid duplicate)
    originalConsole.log('[CSEntryLogger] ✅ Console interception enabled - all logs will be written to file');
    logToFile(LOG_LEVELS.INFO, 'CSEntry Logger initialized - console interception enabled', {
        url: window.location.href,
        timestamp: new Date().toISOString()
    });
    
})(typeof window !== 'undefined' ? window : this);
