/**
 * CSEntry Web - Service Worker
 * 
 * Provides:
 * - Offline functionality via caching
 * - Background sync for data synchronization
 * - Push notifications
 * - Periodic sync for scheduled tasks
 */

const CACHE_VERSION = 'v1.0.2';
const CACHE_NAME = `csentry-cache-${CACHE_VERSION}`;
const RUNTIME_CACHE = 'csentry-runtime';
const SYNC_QUEUE_CACHE = 'csentry-sync-queue';

// Files to cache for offline functionality
const PRECACHE_URLS = [
    '/',
    '/index.html',
    '/offline.html',
    '/styles.css',
    '/manifest.json',
    // Icons
    '/icons/icon.svg',
    // SQLite WASM (official sqlite.org/sqlite-wasm)
    '/sqlite3/sqlite3.js',
    '/sqlite3/sqlite3.wasm',
    '/sqlite3/sqlite3-opfs-async-proxy.js',
    // Note: csentry-web.js and csentry-web.wasm are dynamically loaded
    // Note: csentryKMP.* files are large and cached on-demand
];

// Install event - cache essential files
self.addEventListener('install', (event) => {
    console.log('[ServiceWorker] Installing...');
    
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('[ServiceWorker] Pre-caching files');
                return cache.addAll(PRECACHE_URLS.map(url => {
                    return new Request(url, { cache: 'reload' });
                })).catch(err => {
                    console.warn('[ServiceWorker] Some files failed to cache:', err);
                    // Continue even if some files fail
                    return Promise.resolve();
                });
            })
            .then(() => {
                console.log('[ServiceWorker] Installation complete');
                return self.skipWaiting();
            })
    );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
    console.log('[ServiceWorker] Activating...');
    
    event.waitUntil(
        caches.keys()
            .then((cacheNames) => {
                return Promise.all(
                    cacheNames
                        .filter((cacheName) => {
                            return cacheName.startsWith('csentry-cache-') && 
                                   cacheName !== CACHE_NAME;
                        })
                        .map((cacheName) => {
                            console.log('[ServiceWorker] Deleting old cache:', cacheName);
                            return caches.delete(cacheName);
                        })
                );
            })
            .then(() => {
                console.log('[ServiceWorker] Activation complete');
                return self.clients.claim();
            })
    );
});

// Fetch event - serve from cache, fall back to network
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);
    
    // Skip non-GET requests
    if (event.request.method !== 'GET') {
        return;
    }
    
    // Skip cross-origin requests (we don't use CDNs)
    if (url.origin !== self.location.origin) {
        return;
    }
    
    // Skip chrome-extension and other special protocols
    if (!url.protocol.startsWith('http')) {
        return;
    }
    
    // Handle API requests differently (network first)
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/cspro-api/')) {
        event.respondWith(networkFirst(event.request));
        return;
    }
    
    // Cache first for static assets
    event.respondWith(cacheFirst(event.request));
});

// Cache-first strategy
async function cacheFirst(request) {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
        // Update cache in background (don't await)
        fetchAndCache(request).catch(() => {});
        return cachedResponse;
    }
    
    try {
        return await fetchAndCache(request);
    } catch (error) {
        // Return a basic error response instead of throwing
        console.warn('[ServiceWorker] Resource unavailable:', request.url);
        return new Response('Resource unavailable', {
            status: 503,
            statusText: 'Service Unavailable'
        });
    }
}

// Network-first strategy
async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(RUNTIME_CACHE);
            cache.put(request, response.clone());
        }
        return response;
    } catch (error) {
        const cachedResponse = await caches.match(request);
        if (cachedResponse) {
            return cachedResponse;
        }
        throw error;
    }
}

// Fetch and update cache
async function fetchAndCache(request) {
    try {
        const response = await fetch(request, { 
            cache: 'no-cache',
            credentials: 'same-origin'
        });
        
        if (response.ok && (response.type === 'basic' || response.type === 'default')) {
            const cache = await caches.open(RUNTIME_CACHE);
            cache.put(request, response.clone()).catch(() => {});
        }
        
        return response;
    } catch (error) {
        // Only log for non-extension requests
        const url = request.url || request;
        if (!url.includes('chrome-extension://')) {
            console.debug('[ServiceWorker] Fetch failed (will use cache if available):', url);
        }
        
        // Return offline page for navigation requests
        if (request.mode === 'navigate') {
            const cached = await caches.match('/offline.html');
            if (cached) return cached;
            return new Response('Offline - Please check your connection', {
                status: 503,
                headers: { 'Content-Type': 'text/html' }
            });
        }
        
        // Try to return cached version
        const cachedResponse = await caches.match(request);
        if (cachedResponse) {
            return cachedResponse;
        }
        
        throw error;
    }
}

// Background Sync
self.addEventListener('sync', (event) => {
    console.log('[ServiceWorker] Sync event:', event.tag);
    
    if (event.tag === 'csentry-data-sync') {
        event.waitUntil(performDataSync());
    } else if (event.tag === 'csentry-upload-queue') {
        event.waitUntil(processUploadQueue());
    } else if (event.tag.startsWith('csentry-sync-')) {
        event.waitUntil(performTaggedSync(event.tag));
    }
});

// Periodic Background Sync
self.addEventListener('periodicsync', (event) => {
    console.log('[ServiceWorker] Periodic sync:', event.tag);
    
    if (event.tag === 'csentry-periodic-sync') {
        event.waitUntil(performPeriodicSync());
    }
});

// Perform data synchronization
async function performDataSync() {
    console.log('[ServiceWorker] Performing data sync...');
    
    try {
        // Get pending sync items from IndexedDB
        const syncQueue = await getSyncQueue();
        
        if (syncQueue.length === 0) {
            console.log('[ServiceWorker] No items to sync');
            return;
        }
        
        console.log(`[ServiceWorker] Syncing ${syncQueue.length} items`);
        
        for (const item of syncQueue) {
            try {
                await syncItem(item);
                await removeSyncItem(item.id);
                console.log('[ServiceWorker] Synced item:', item.id);
            } catch (error) {
                console.error('[ServiceWorker] Failed to sync item:', item.id, error);
                // Keep in queue for retry
            }
        }
        
        // Notify clients
        const clients = await self.clients.matchAll();
        clients.forEach(client => {
            client.postMessage({
                type: 'SYNC_COMPLETE',
                success: true
            });
        });
        
    } catch (error) {
        console.error('[ServiceWorker] Sync failed:', error);
        throw error; // Retry later
    }
}

// Process upload queue
async function processUploadQueue() {
    console.log('[ServiceWorker] Processing upload queue...');
    
    const queue = await getUploadQueue();
    
    for (const upload of queue) {
        try {
            const response = await fetch(upload.url, {
                method: upload.method || 'POST',
                headers: upload.headers || {},
                body: upload.body
            });
            
            if (response.ok) {
                await removeFromUploadQueue(upload.id);
                notifyClients({
                    type: 'UPLOAD_COMPLETE',
                    id: upload.id,
                    success: true
                });
            }
        } catch (error) {
            console.error('[ServiceWorker] Upload failed:', upload.id, error);
        }
    }
}

// Perform periodic sync
async function performPeriodicSync() {
    console.log('[ServiceWorker] Performing periodic sync...');
    
    // Check for pending data
    const hasData = await checkPendingData();
    
    if (hasData) {
        await performDataSync();
    }
    
    // Update cached data from server
    await updateCachedData();
}

// Tagged sync for specific operations
async function performTaggedSync(tag) {
    const operation = tag.replace('csentry-sync-', '');
    console.log('[ServiceWorker] Tagged sync:', operation);
    
    const syncData = await getSyncDataForTag(tag);
    if (!syncData) return;
    
    try {
        const response = await fetch(syncData.url, {
            method: syncData.method,
            headers: syncData.headers,
            body: syncData.body
        });
        
        if (response.ok) {
            await clearSyncTag(tag);
            notifyClients({
                type: 'TAGGED_SYNC_COMPLETE',
                tag: tag,
                success: true
            });
        }
    } catch (error) {
        console.error('[ServiceWorker] Tagged sync failed:', tag, error);
        throw error;
    }
}

// Push notification handling
self.addEventListener('push', (event) => {
    console.log('[ServiceWorker] Push received');
    
    let data = { title: 'CSEntry', body: 'New notification' };
    
    if (event.data) {
        try {
            data = event.data.json();
        } catch (e) {
            data.body = event.data.text();
        }
    }
    
    const options = {
        body: data.body,
        icon: '/icons/icon-192x192.png',
        badge: '/icons/badge-72x72.png',
        vibrate: [100, 50, 100],
        data: data,
        actions: data.actions || [
            { action: 'view', title: 'View' },
            { action: 'dismiss', title: 'Dismiss' }
        ]
    };
    
    event.waitUntil(
        self.registration.showNotification(data.title, options)
    );
});

// Notification click handling
self.addEventListener('notificationclick', (event) => {
    console.log('[ServiceWorker] Notification clicked:', event.action);
    
    event.notification.close();
    
    if (event.action === 'dismiss') {
        return;
    }
    
    const urlToOpen = event.notification.data?.url || '/';
    
    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then((clients) => {
                // Focus existing window
                for (const client of clients) {
                    if (client.url.includes(self.location.origin) && 'focus' in client) {
                        client.focus();
                        client.postMessage({
                            type: 'NOTIFICATION_CLICK',
                            action: event.action,
                            data: event.notification.data
                        });
                        return;
                    }
                }
                // Open new window
                if (self.clients.openWindow) {
                    return self.clients.openWindow(urlToOpen);
                }
            })
    );
});

// Message handling from main thread
self.addEventListener('message', (event) => {
    console.log('[ServiceWorker] Message received:', event.data.type);
    
    switch (event.data.type) {
        case 'SKIP_WAITING':
            self.skipWaiting();
            break;
            
        case 'QUEUE_SYNC':
            queueSyncItem(event.data.item);
            break;
            
        case 'QUEUE_UPLOAD':
            queueUpload(event.data.upload);
            break;
            
        case 'CLEAR_CACHE':
            clearAllCaches();
            break;
            
        case 'GET_CACHE_STATUS':
            getCacheStatus().then(status => {
                event.ports[0]?.postMessage(status);
            });
            break;
            
        case 'REGISTER_PERIODIC_SYNC':
            registerPeriodicSync(event.data.interval);
            break;
    }
});

// IndexedDB helpers for sync queue
const DB_NAME = 'csentry-sw-db';
const DB_VERSION = 1;

function openDatabase() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);
        
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            
            if (!db.objectStoreNames.contains('syncQueue')) {
                db.createObjectStore('syncQueue', { keyPath: 'id', autoIncrement: true });
            }
            if (!db.objectStoreNames.contains('uploadQueue')) {
                db.createObjectStore('uploadQueue', { keyPath: 'id', autoIncrement: true });
            }
            if (!db.objectStoreNames.contains('syncTags')) {
                db.createObjectStore('syncTags', { keyPath: 'tag' });
            }
        };
    });
}

async function getSyncQueue() {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('syncQueue', 'readonly');
        const store = transaction.objectStore('syncQueue');
        const request = store.getAll();
        
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function queueSyncItem(item) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('syncQueue', 'readwrite');
        const store = transaction.objectStore('syncQueue');
        const request = store.add({ ...item, timestamp: Date.now() });
        
        request.onsuccess = () => {
            // Request background sync
            self.registration.sync.register('csentry-data-sync');
            resolve(request.result);
        };
        request.onerror = () => reject(request.error);
    });
}

async function removeSyncItem(id) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('syncQueue', 'readwrite');
        const store = transaction.objectStore('syncQueue');
        const request = store.delete(id);
        
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
    });
}

async function getUploadQueue() {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('uploadQueue', 'readonly');
        const store = transaction.objectStore('uploadQueue');
        const request = store.getAll();
        
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function queueUpload(upload) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('uploadQueue', 'readwrite');
        const store = transaction.objectStore('uploadQueue');
        const request = store.add({ ...upload, timestamp: Date.now() });
        
        request.onsuccess = () => {
            self.registration.sync.register('csentry-upload-queue');
            resolve(request.result);
        };
        request.onerror = () => reject(request.error);
    });
}

async function removeFromUploadQueue(id) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('uploadQueue', 'readwrite');
        const store = transaction.objectStore('uploadQueue');
        const request = store.delete(id);
        
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
    });
}

async function getSyncDataForTag(tag) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('syncTags', 'readonly');
        const store = transaction.objectStore('syncTags');
        const request = store.get(tag);
        
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function clearSyncTag(tag) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction('syncTags', 'readwrite');
        const store = transaction.objectStore('syncTags');
        const request = store.delete(tag);
        
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
    });
}

async function syncItem(item) {
    // Implement actual sync logic based on item type
    const response = await fetch(item.url || '/api/sync', {
        method: item.method || 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(item.headers || {})
        },
        body: JSON.stringify(item.data)
    });
    
    if (!response.ok) {
        throw new Error(`Sync failed: ${response.status}`);
    }
    
    return response.json();
}

async function checkPendingData() {
    const queue = await getSyncQueue();
    return queue.length > 0;
}

async function updateCachedData() {
    // Refresh critical cached data
    try {
        const cache = await caches.open(RUNTIME_CACHE);
        // Add any data refresh logic here
    } catch (error) {
        console.warn('[ServiceWorker] Cache update failed:', error);
    }
}

function notifyClients(message) {
    self.clients.matchAll().then(clients => {
        clients.forEach(client => client.postMessage(message));
    });
}

async function clearAllCaches() {
    const cacheNames = await caches.keys();
    await Promise.all(cacheNames.map(name => caches.delete(name)));
    notifyClients({ type: 'CACHES_CLEARED' });
}

async function getCacheStatus() {
    const cacheNames = await caches.keys();
    const status = {};
    
    for (const name of cacheNames) {
        const cache = await caches.open(name);
        const keys = await cache.keys();
        status[name] = keys.length;
    }
    
    return status;
}

async function registerPeriodicSync(interval) {
    try {
        await self.registration.periodicSync.register('csentry-periodic-sync', {
            minInterval: interval || 24 * 60 * 60 * 1000 // Default: 24 hours
        });
        console.log('[ServiceWorker] Periodic sync registered');
    } catch (error) {
        console.warn('[ServiceWorker] Periodic sync not supported:', error);
    }
}

console.log('[ServiceWorker] Script loaded');
