// Webpack configuration for Emscripten WASM compatibility
// This handles Node.js modules used by Emscripten-generated JS that aren't available in browsers

config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

// Node.js 'module' is used by Emscripten for dynamic require detection - not needed in browser
config.resolve.fallback.module = false;
config.resolve.fallback.fs = false;
config.resolve.fallback.path = false;
config.resolve.fallback.crypto = false;

// Mark CSPro WASM engine modules as externals
// They are Emscripten ES modules loaded via script tag before the Kotlin bundle
// The script tag sets window.createCSEntryKMPModule which we access via 'self' (globalThis)
config.externals = config.externals || [];
config.externals.push(function({ context, request }, callback) {
    // These modules are loaded via <script> tag and expose createCSEntryKMPModule on window
    if (request === './csentryKMP.js') {
        // Access the global createCSEntryKMPModule set by the script tag
        return callback(null, 'self createCSEntryKMPModule');
    }
    if (request === './wasm/CSProAndroid.js') {
        // Same module, different path
        return callback(null, 'self createCSEntryKMPModule');
    }
    callback();
});

// Add .wasm to resolvable extensions
config.resolve.extensions = config.resolve.extensions || [];
if (!config.resolve.extensions.includes('.wasm')) {
    config.resolve.extensions.push('.wasm');
}

// Configure WASM handling
config.module = config.module || {};
config.module.rules = config.module.rules || [];

// Exclude Emscripten JS files from source-map-loader to avoid warnings
config.module.rules.push({
    test: /csentryKMP\.js$|CSProAndroid\.js$/,
    enforce: 'pre',
    use: ['source-map-loader'],
    exclude: /node_modules/,
    resolve: {
        fullySpecified: false
    }
});

// Handle .wasm files as assets
config.module.rules.push({
    test: /\.wasm$/,
    type: 'asset/resource',
    generator: {
        filename: '[name][ext]'
    }
});

// Handle .data files (Emscripten embedded filesystem data)
config.module.rules.push({
    test: /\.data$/,
    type: 'asset/resource',
    generator: {
        filename: '[name][ext]'
    }
});

// Copy Emscripten files to output
const CopyPlugin = require('copy-webpack-plugin');
const path = require('path');

config.plugins = config.plugins || [];
config.plugins.push(new CopyPlugin({
    patterns: [
        // Copy from public/wasm to output/wasm directory
        {
            from: path.resolve(__dirname, '../public/wasm/csentryKMP.js'),
            to: 'wasm/csentryKMP.js',
            noErrorOnMissing: true
        },
        {
            from: path.resolve(__dirname, '../public/wasm/csentryKMP.wasm'),
            to: 'wasm/csentryKMP.wasm',
            noErrorOnMissing: true
        },
        {
            from: path.resolve(__dirname, '../public/wasm/csentryKMP.data'),
            to: 'wasm/csentryKMP.data',
            noErrorOnMissing: true
        },
        {
            from: path.resolve(__dirname, '../public/wasm/CSProAndroid.js'),
            to: 'wasm/CSProAndroid.js',
            noErrorOnMissing: true
        },
        // Legacy paths for compatibility
        {
            from: path.resolve(__dirname, 'kotlin/wasm/csentryKMP.wasm'),
            to: 'csentryKMP.wasm',
            noErrorOnMissing: true
        },
        {
            from: path.resolve(__dirname, 'kotlin/wasm/csentryKMP.data'),
            to: 'csentryKMP.data',
            noErrorOnMissing: true
        },
        {
            from: path.resolve(__dirname, 'kotlin/csentryKMP.wasm'),
            to: 'csentryKMP.wasm',
            noErrorOnMissing: true
        },
        {
            from: path.resolve(__dirname, 'kotlin/csentryKMP.data'),
            to: 'csentryKMP.data',
            noErrorOnMissing: true
        }
    ]
}));

console.log('[Emscripten Compat] Webpack config extended for WASM/browser compatibility');
