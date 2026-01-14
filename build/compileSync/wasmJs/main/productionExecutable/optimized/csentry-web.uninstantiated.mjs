
export async function instantiate(imports={}, runInitializer=true) {
    const cachedJsObjects = new WeakMap();
    // ref must be non-null
    function getCachedJsObject(ref, ifNotCached) {
        if (typeof ref !== 'object' && typeof ref !== 'function') return ifNotCached;
        const cached = cachedJsObjects.get(ref);
        if (cached !== void 0) return cached;
        cachedJsObjects.set(ref, ifNotCached);
        return ifNotCached;
    }

    const _ref_Li9jc2VudHJ5S01QLmpz_ = imports['./csentryKMP.js'];
    const _ref_QGpzLWpvZGEvY29yZQ_ = imports['@js-joda/core'];
    
    const js_code = {
        'kotlin.captureStackTrace' : () => new Error().stack,
        'kotlin.wasm.internal.stringLength' : (x) => x.length,
        'kotlin.wasm.internal.jsExportStringToWasm' : (src, srcOffset, srcLength, dstAddr) => { 
            const mem16 = new Uint16Array(wasmExports.memory.buffer, dstAddr, srcLength);
            let arrayIndex = 0;
            let srcIndex = srcOffset;
            while (arrayIndex < srcLength) {
                mem16.set([src.charCodeAt(srcIndex)], arrayIndex);
                srcIndex++;
                arrayIndex++;
            }     
             },
        'kotlin.wasm.internal.externrefToInt' : (ref) => Number(ref),
        'kotlin.wasm.internal.importStringFromWasm' : (address, length, prefix) => { 
            const mem16 = new Uint16Array(wasmExports.memory.buffer, address, length);
            const str = String.fromCharCode.apply(null, mem16);
            return (prefix == null) ? str : prefix + str;
             },
        'kotlin.wasm.internal.getJsEmptyString' : () => '',
        'kotlin.wasm.internal.externrefToString' : (ref) => String(ref),
        'kotlin.wasm.internal.externrefEquals' : (lhs, rhs) => lhs === rhs,
        'kotlin.wasm.internal.externrefHashCode' : 
        (() => {
        const dataView = new DataView(new ArrayBuffer(8));
        function numberHashCode(obj) {
            if ((obj | 0) === obj) {
                return obj | 0;
            } else {
                dataView.setFloat64(0, obj, true);
                return (dataView.getInt32(0, true) * 31 | 0) + dataView.getInt32(4, true) | 0;
            }
        }
        
        const hashCodes = new WeakMap();
        function getObjectHashCode(obj) {
            const res = hashCodes.get(obj);
            if (res === undefined) {
                const POW_2_32 = 4294967296;
                const hash = (Math.random() * POW_2_32) | 0;
                hashCodes.set(obj, hash);
                return hash;
            }
            return res;
        }
        
        function getStringHashCode(str) {
            var hash = 0;
            for (var i = 0; i < str.length; i++) {
                var code  = str.charCodeAt(i);
                hash  = (hash * 31 + code) | 0;
            }
            return hash;
        }
        
        return (obj) => {
            if (obj == null) {
                return 0;
            }
            switch (typeof obj) {
                case "object":
                case "function":
                    return getObjectHashCode(obj);
                case "number":
                    return numberHashCode(obj);
                case "boolean":
                    return obj ? 1231 : 1237;
                default:
                    return getStringHashCode(String(obj)); 
            }
        }
        })(),
        'kotlin.wasm.internal.isNullish' : (ref) => ref == null,
        'kotlin.wasm.internal.externrefToDouble' : (ref) => Number(ref),
        'kotlin.wasm.internal.getJsTrue' : () => true,
        'kotlin.wasm.internal.getJsFalse' : () => false,
        'kotlin.wasm.internal.newJsArray' : () => [],
        'kotlin.wasm.internal.jsArrayPush' : (array, element) => { array.push(element); },
        'kotlin.wasm.internal.getCachedJsObject_$external_fun' : (p0, p1) => getCachedJsObject(p0, p1),
        'kotlin.js.__convertKotlinClosureToJsClosure_(()->Unit)' : (f) => getCachedJsObject(f, () => wasmExports['__callFunction_(()->Unit)'](f, )),
        'kotlin.io.printError' : (error) => console.error(error),
        'kotlin.io.printlnImpl' : (message) => console.log(message),
        'kotlin.js.__convertKotlinClosureToJsClosure_((Js?)->Js?)' : (f) => getCachedJsObject(f, (p0) => wasmExports['__callFunction_((Js?)->Js?)'](f, p0)),
        'kotlin.js.__convertKotlinClosureToJsClosure_((Js)->Js?)' : (f) => getCachedJsObject(f, (p0) => wasmExports['__callFunction_((Js)->Js?)'](f, p0)),
        'kotlin.random.initialSeed' : () => ((Math.random() * Math.pow(2, 32)) | 0),
        'kotlin.wasm.internal.getJsClassName' : (jsKlass) => jsKlass.name,
        'kotlin.wasm.internal.getConstructor' : (obj) => obj.constructor,
        'kotlinx.coroutines.tryGetProcess' : () => (typeof(process) !== 'undefined' && typeof(process.nextTick) === 'function') ? process : null,
        'kotlinx.coroutines.tryGetWindow' : () => (typeof(window) !== 'undefined' && window != null && typeof(window.addEventListener) === 'function') ? window : null,
        'kotlinx.coroutines.nextTick_$external_fun' : (_this, p0) => _this.nextTick(p0),
        'kotlinx.coroutines.error_$external_fun' : (_this, p0) => _this.error(p0),
        'kotlinx.coroutines.console_$external_prop_getter' : () => console,
        'kotlinx.coroutines.createScheduleMessagePoster' : (process) => () => Promise.resolve(0).then(process),
        'kotlinx.coroutines.__callJsClosure_(()->Unit)' : (f, ) => f(),
        'kotlinx.coroutines.createRescheduleMessagePoster' : (window) => () => window.postMessage('dispatchCoroutine', '*'),
        'kotlinx.coroutines.subscribeToWindowMessages' : (window, process) => {
            const handler = (event) => {
                if (event.source == window && event.data == 'dispatchCoroutine') {
                    event.stopPropagation();
                    process();
                }
            }
            window.addEventListener('message', handler, true);
        },
        'kotlinx.coroutines.setTimeout' : (window, handler, timeout) => window.setTimeout(handler, timeout),
        'kotlinx.coroutines.clearTimeout' : (handle) => { if (typeof clearTimeout !== 'undefined') clearTimeout(handle); },
        'kotlinx.coroutines.setTimeout_$external_fun' : (p0, p1) => setTimeout(p0, p1),
        'kotlinx.browser.document_$external_prop_getter' : () => document,
        'kotlinx.browser.window_$external_prop_getter' : () => window,
        'org.w3c.dom.length_$external_prop_getter' : (_this) => _this.length,
        'org.w3c.dom.css.alignItems_$external_prop_setter' : (_this, v) => _this.alignItems = v,
        'org.w3c.dom.css.backgroundColor_$external_prop_setter' : (_this, v) => _this.backgroundColor = v,
        'org.w3c.dom.css.border_$external_prop_setter' : (_this, v) => _this.border = v,
        'org.w3c.dom.css.borderRadius_$external_prop_setter' : (_this, v) => _this.borderRadius = v,
        'org.w3c.dom.css.boxShadow_$external_prop_setter' : (_this, v) => _this.boxShadow = v,
        'org.w3c.dom.css.display_$external_prop_getter' : (_this) => _this.display,
        'org.w3c.dom.css.display_$external_prop_setter' : (_this, v) => _this.display = v,
        'org.w3c.dom.css.height_$external_prop_getter' : (_this) => _this.height,
        'org.w3c.dom.css.height_$external_prop_setter' : (_this, v) => _this.height = v,
        'org.w3c.dom.css.justifyContent_$external_prop_setter' : (_this, v) => _this.justifyContent = v,
        'org.w3c.dom.css.left_$external_prop_setter' : (_this, v) => _this.left = v,
        'org.w3c.dom.css.minHeight_$external_prop_setter' : (_this, v) => _this.minHeight = v,
        'org.w3c.dom.css.position_$external_prop_setter' : (_this, v) => _this.position = v,
        'org.w3c.dom.css.top_$external_prop_setter' : (_this, v) => _this.top = v,
        'org.w3c.dom.css.width_$external_prop_setter' : (_this, v) => _this.width = v,
        'org.w3c.dom.css.zIndex_$external_prop_setter' : (_this, v) => _this.zIndex = v,
        'org.w3c.dom.css.setProperty_$external_fun' : (_this, p0, p1, p2, isDefault0) => _this.setProperty(p0, p1, isDefault0 ? undefined : p2, ),
        'org.w3c.dom.css.style_$external_prop_getter' : (_this) => _this.style,
        'org.w3c.dom.encryptedmedia.__convertKotlinClosureToJsClosure_((Js)->Unit)' : (f) => getCachedJsObject(f, (p0) => wasmExports['__callFunction_((Js)->Unit)'](f, p0)),
        'org.w3c.dom.events.addEventListener_$external_fun' : (_this, p0, p1) => _this.addEventListener(p0, p1),
        'org.w3c.dom.events.removeEventListener_$external_fun' : (_this, p0, p1) => _this.removeEventListener(p0, p1),
        'org.w3c.dom.events.target_$external_prop_getter' : (_this) => _this.target,
        'org.w3c.dom.events.stopPropagation_$external_fun' : (_this, ) => _this.stopPropagation(),
        'org.w3c.dom.events.preventDefault_$external_fun' : (_this, ) => _this.preventDefault(),
        'org.w3c.dom.events.Event_$external_class_instanceof' : (x) => x instanceof Event,
        'org.w3c.dom.events.key_$external_prop_getter' : (_this) => _this.key,
        'org.w3c.dom.events.KeyboardEvent_$external_class_instanceof' : (x) => x instanceof KeyboardEvent,
        'org.w3c.dom.location_$external_prop_getter' : (_this) => _this.location,
        'org.w3c.dom.innerWidth_$external_prop_getter' : (_this) => _this.innerWidth,
        'org.w3c.dom.innerHeight_$external_prop_getter' : (_this) => _this.innerHeight,
        'org.w3c.dom.open_$external_fun' : (_this, p0, p1, p2, isDefault0, isDefault1, isDefault2) => _this.open(isDefault0 ? undefined : p0, isDefault1 ? undefined : p1, isDefault2 ? undefined : p2, ),
        'org.w3c.dom.onload_$external_prop_setter' : (_this, v) => _this.onload = v,
        'org.w3c.dom.setTimeout_$external_fun' : (_this, p0, p1, p2, isDefault0, isDefault1) => _this.setTimeout(p0, isDefault0 ? undefined : p1, ...p2, ),
        'org.w3c.dom.__convertKotlinClosureToJsClosure_(()->Js?)' : (f) => getCachedJsObject(f, () => wasmExports['__callFunction_(()->Js?)'](f, )),
        'org.w3c.dom.clearTimeout_$external_fun' : (_this, p0, isDefault0) => _this.clearTimeout(isDefault0 ? undefined : p0, ),
        'org.w3c.dom.title_$external_prop_setter' : (_this, v) => _this.title = v,
        'org.w3c.dom.body_$external_prop_getter' : (_this) => _this.body,
        'org.w3c.dom.createElement_$external_fun' : (_this, p0, p1, isDefault0) => _this.createElement(p0, isDefault0 ? undefined : p1, ),
        'org.w3c.dom.getElementById_$external_fun' : (_this, p0) => _this.getElementById(p0),
        'org.w3c.dom.querySelector_$external_fun' : (_this, p0) => _this.querySelector(p0),
        'org.w3c.dom.hash_$external_prop_getter' : (_this) => _this.hash,
        'org.w3c.dom.hash_$external_prop_setter' : (_this, v) => _this.hash = v,
        'org.w3c.dom.id_$external_prop_setter' : (_this, v) => _this.id = v,
        'org.w3c.dom.className_$external_prop_setter' : (_this, v) => _this.className = v,
        'org.w3c.dom.classList_$external_prop_getter' : (_this) => _this.classList,
        'org.w3c.dom.innerHTML_$external_prop_getter' : (_this) => _this.innerHTML,
        'org.w3c.dom.innerHTML_$external_prop_setter' : (_this, v) => _this.innerHTML = v,
        'org.w3c.dom.getAttribute_$external_fun' : (_this, p0) => _this.getAttribute(p0),
        'org.w3c.dom.closest_$external_fun' : (_this, p0) => _this.closest(p0),
        'org.w3c.dom.data_$external_prop_getter' : (_this) => _this.data,
        'org.w3c.dom.parentElement_$external_prop_getter' : (_this) => _this.parentElement,
        'org.w3c.dom.textContent_$external_prop_getter' : (_this) => _this.textContent,
        'org.w3c.dom.textContent_$external_prop_setter' : (_this, v) => _this.textContent = v,
        'org.w3c.dom.appendChild_$external_fun' : (_this, p0) => _this.appendChild(p0),
        'org.w3c.dom.removeChild_$external_fun' : (_this, p0) => _this.removeChild(p0),
        'org.w3c.dom.querySelector_$external_fun_1' : (_this, p0) => _this.querySelector(p0),
        'org.w3c.dom.querySelectorAll_$external_fun' : (_this, p0) => _this.querySelectorAll(p0),
        'org.w3c.dom.click_$external_fun' : (_this, ) => _this.click(),
        'org.w3c.dom.focus_$external_fun' : (_this, ) => _this.focus(),
        'org.w3c.dom.HTMLElement_$external_class_instanceof' : (x) => x instanceof HTMLElement,
        'org.w3c.dom.item_$external_fun' : (_this, p0) => _this.item(p0),
        'org.w3c.dom.contains_$external_fun' : (_this, p0) => _this.contains(p0),
        'org.w3c.dom.add_$external_fun' : (_this, p0) => _this.add(...p0),
        'org.w3c.dom.remove_$external_fun' : (_this, p0) => _this.remove(...p0),
        'org.w3c.dom.toggle_$external_fun' : (_this, p0, p1, isDefault0) => _this.toggle(p0, isDefault0 ? undefined : p1, ),
        'org.w3c.dom.HTMLDivElement_$external_class_instanceof' : (x) => x instanceof HTMLDivElement,
        'org.w3c.dom.accept_$external_prop_setter' : (_this, v) => _this.accept = v,
        'org.w3c.dom.value_$external_prop_getter' : (_this) => _this.value,
        'org.w3c.dom.value_$external_prop_setter' : (_this, v) => _this.value = v,
        'org.w3c.dom.HTMLInputElement_$external_class_instanceof' : (x) => x instanceof HTMLInputElement,
        'org.w3c.dom.HTMLButtonElement_$external_class_instanceof' : (x) => x instanceof HTMLButtonElement,
        'org.w3c.dom.src_$external_prop_setter' : (_this, v) => _this.src = v,
        'org.w3c.dom.srcdoc_$external_prop_setter' : (_this, v) => _this.srcdoc = v,
        'org.w3c.dom.contentWindow_$external_prop_getter' : (_this) => _this.contentWindow,
        'org.w3c.dom.HTMLIFrameElement_$external_class_instanceof' : (x) => x instanceof HTMLIFrameElement,
        'gov.census.cspro.engine.isModuleLoaded' : () => window.CSProModule != null,
        'gov.census.cspro.engine.getGlobalModule' : () => window.CSProModule,
        'gov.census.cspro.engine.createEngineInstance' : (mod) => new mod.CSProEngine(),
        'gov.census.cspro.engine.isPromise' : (value) => value !== null && value !== undefined && typeof value.then === 'function',
        'gov.census.cspro.engine.handlePromise' : (promise, resolve, reject) => {
            try {
                promise.then(
                    (result) => { try { resolve(result); } catch(e) { console.error('[handlePromise] Error in resolve callback:', e); } },
                    (error) => { try { reject(error); } catch(e) { console.error('[handlePromise] Error in reject callback:', e); } }
                );
            } catch(e) {
                console.error('[handlePromise] Error setting up promise:', e);
                try { reject(e); } catch(e2) { console.error('[handlePromise] Error calling reject:', e2); }
            }
        },
        'gov.census.cspro.engine.getProperty' : (obj, key) => obj[key],
        'gov.census.cspro.engine.getStringProperty' : (obj, key) => typeof obj[key] === 'string' ? obj[key] : null,
        'gov.census.cspro.engine.getIntProperty' : (obj, key) => typeof obj[key] === 'number' ? obj[key] : 0,
        'gov.census.cspro.engine.getDoubleProperty' : (obj, key) => typeof obj[key] === 'number' ? obj[key] : 0.0,
        'gov.census.cspro.engine.getDoublePropertyNullable' : (obj, key) => typeof obj[key] === 'number' ? obj[key] : null,
        'gov.census.cspro.engine.getLongProperty' : (obj, key) => typeof obj[key] === 'number' ? BigInt(obj[key]) : 0n,
        'gov.census.cspro.engine.getBooleanProperty' : (obj, key) => obj[key] === true,
        'gov.census.cspro.engine.getArrayLength' : (arr) => arr.length,
        'gov.census.cspro.engine.getArrayElement' : (arr, idx) => arr[idx],
        'gov.census.cspro.engine.logInfo' : (msg) => { if (window.logger) { window.logger.engine(msg); } else { console.log('[CSProEngineService] ' + msg); } },
        'gov.census.cspro.engine.logError' : (msg) => { if (window.logger) { window.logger.error('[CSProEngineService] ' + msg); } else { console.error('[CSProEngineService] ' + msg); } },
        'gov.census.cspro.engine.hasCSProEngine' : (obj) => obj.CSProEngine != null,
        'gov.census.cspro.engine.scheduleModuleCheck' : (delayMs, callback) => setTimeout(() => callback(), delayMs),
        'gov.census.cspro.engine.jsHasMethod' : (obj, name) => !!obj && typeof obj[name] === 'function',
        'gov.census.cspro.engine.jsCallBoolNoArgs' : (obj, name) => {
            const fn = obj && obj[name];
            if (typeof fn !== 'function') return false;
            return !!fn.call(obj);
        },
        'gov.census.cspro.engine.installQsfFunctions' : () => {
            console.log('[QuestionTextBridge] Installing global functions via ActionInvoker...');
        
            // Flag to indicate bridge is ready
            window._questionTextBridgeReady = true;
        
            // Helper to get CSProActionInvoker instance
            // QSF content runs in iframe, so we need to create or use existing instance
            const getActionInvoker = function() {
                if (!window._qsfActionInvoker && typeof CSProActionInvoker !== 'undefined') {
                    // Use same access token as parent if available
                    const token = window._csproPffAccessToken || 'qsf-default-token';
                    window._qsfActionInvoker = new CSProActionInvoker(token);
                }
                return window._qsfActionInvoker;
            };
        
            // Helper to run Logic.eval via ActionInvoker (async)
            const runLogicEval = function(logic) {
                const ai = getActionInvoker();
                if (ai && ai.Logic && ai.Logic.evalAsync) {
                    console.log('[QSF] Executing via Logic.evalAsync:', logic);
                    return ai.Logic.evalAsync({ logic: logic });
                } else {
                    console.warn('[QSF] ActionInvoker not available, falling back to postMessage');
                    // Fallback: send to parent frame for processing
                    window.parent.postMessage({
                        type: 'cspro-action-request',
                        method: 'Logic.evalAsync',
                        args: { logic: logic },
                        requestId: Date.now()
                    }, '*');
                    return Promise.resolve(null);
                }
            };
        
            // Helper to run Logic.invoke via ActionInvoker (async)
            const runLogicInvoke = function(functionName, args) {
                const ai = getActionInvoker();
                if (ai && ai.Logic && ai.Logic.invokeAsync) {
                    console.log('[QSF] Executing via Logic.invokeAsync:', functionName, args);
                    return ai.Logic.invokeAsync({ function: functionName, arguments: args });
                } else {
                    console.warn('[QSF] ActionInvoker not available, falling back to postMessage');
                    window.parent.postMessage({
                        type: 'cspro-action-request',
                        method: 'Logic.invokeAsync',
                        args: { function: functionName, arguments: args },
                        requestId: Date.now()
                    }, '*');
                    return Promise.resolve(null);
                }
            };
        
            // endRoster / endGroup - Execute 'endgroup' CSPro logic
            // Android: CSProJavaScriptInterface.runLogic("endgroup")
            window.endRoster = function() {
                console.log('[QSF] endRoster() -> Logic.eval("endgroup")');
                return runLogicEval('endgroup');
            };
        
            window.endGroup = function() {
                console.log('[QSF] endGroup() -> Logic.eval("endgroup")');
                return runLogicEval('endgroup');
            };
        
            // moveToField - Execute move(to fieldname) CSPro logic
            // Android: CSProJavaScriptInterface.runLogic("move(to fieldname)")
            window.moveToField = function(fieldName) {
                if (!fieldName) {
                    console.warn('[QSF] moveToField called without field name');
                    return Promise.resolve(null);
                }
                const logic = 'move(to ' + fieldName + ')';
                console.log('[QSF] moveToField("' + fieldName + '") -> Logic.eval("' + logic + '")');
                return runLogicEval(logic);
            };
        
            // advance - Execute 'advance' CSPro logic
            window.advance = function() {
                console.log('[QSF] advance() -> Logic.eval("advance")');
                return runLogicEval('advance');
            };
        
            // CSPro namespace for direct logic execution
            window.CSPro = window.CSPro || {};
        
            // CSPro.runLogic(code) - Run arbitrary CSPro logic
            // Android: CSProJavaScriptInterface.runLogic(code)
            window.CSPro.runLogic = function(code) {
                console.log('[QSF] CSPro.runLogic("' + code + '")');
                return runLogicEval(code);
            };
        
            // CSPro.runLogicAsync(code) - Async version
            window.CSPro.runLogicAsync = function(code) {
                return runLogicEval(code);
            };
        
            // CSPro.invoke(functionName) - Invoke user-defined function
            // Android: CSProJavaScriptInterface.invoke(functionName)
            window.CSPro.invoke = function(functionName, args) {
                console.log('[QSF] CSPro.invoke("' + functionName + '")', args);
                return runLogicInvoke(functionName, args);
            };
        
            // CSPro.invokeAsync(functionName, args) - Async version
            window.CSPro.invokeAsync = function(functionName, args) {
                return runLogicInvoke(functionName, args);
            };
        
            // CSPro.do(action) - Legacy action handler
            window.CSPro.do = function(action) {
                console.log('[QSF] CSPro.do("' + action + '")');
                if (action === 'close') {
                    // Close dialog via ActionInvoker
                    const ai = getActionInvoker();
                    if (ai && ai.UI && ai.UI.closeDialog) {
                        return ai.UI.closeDialog();
                    }
                }
                return null;
            };
        
            // CSPro.getAsyncResult() - For compatibility with older question text
            window.CSPro.getAsyncResult = function() {
                return window._qsfLastAsyncResult || null;
            };
        
            // UI namespace aliases for ActionInvoker compatibility
            window.UI = window.UI || {};
            window.UI.endRoster = window.endRoster;
            window.UI.endGroup = window.endGroup;
            window.UI.moveToField = window.moveToField;
        
            console.log('[QuestionTextBridge] Global QSF functions installed via ActionInvoker');
        },
        'gov.census.cspro.engine.dialogs.jsonStringify' : (data) => JSON.stringify(data),
        'gov.census.cspro.engine.dialogs.jsonParse' : (jsonStr) => JSON.parse(jsonStr),
        'gov.census.cspro.engine.dialogs.getJsProperty' : (obj, key) => obj[key],
        'gov.census.cspro.engine.dialogs.getJsStringProperty' : (obj, key) => typeof obj[key] === 'string' ? obj[key] : null,
        'gov.census.cspro.engine.dialogs.getJsIntProperty' : (obj, key) => typeof obj[key] === 'number' ? obj[key] : null,
        'gov.census.cspro.engine.dialogs.postMessageToWindow' : (win, msg, origin) => win.postMessage(msg, origin),
        'gov.census.cspro.engine.dialogs.createInputMessage' : (type, input) => ({ type: type, input: input }),
        'gov.census.cspro.engine.dialogs.createResponseMessage' : (type, requestId, result) => ({ type: type, requestId: requestId, result: result }),
        'gov.census.cspro.engine.dialogs.createErrorMessage' : (type, requestId, error) => ({ type: type, requestId: requestId, error: error }),
        'gov.census.cspro.engine.dialogs.pushDialogHistory' : (dialogId) => { try { window.history.pushState({ csproDialog: dialogId }, '', window.location.href); return true; } catch(e) { return false; } },
        'gov.census.cspro.engine.dialogs.isCurrentDialogHistory' : (dialogId) => { try { const s = window.history.state; return !!(s && s.csproDialog === dialogId); } catch(e) { return false; } },
        'gov.census.cspro.engine.dialogs.popDialogHistory' : () => { try { window.history.back(); } catch(e) { } },
        'gov.census.cspro.engine.dialogs.initDialogHandler' : () => { window.CSProDialogHandler = window.CSProDialogHandler || {}; },
        'gov.census.cspro.engine.dialogs.registerDialogFunctions' : 
        (showDialogFn, showHtmlDialogFn, showModalDialogFn) => {
            window.CSProDialogHandler = window.CSProDialogHandler || {};
        
            // Helper: Create and show an HTML dialog in an iframe overlay
            window.CSProDialogHandler._showHtmlDialogInIframe = function(dialogPath, inputData) {
                return new Promise((resolve, reject) => {
                    console.log("[WasmDialogBridge JS] Opening HTML dialog:", dialogPath, "with input:", inputData);
            
                    // Create overlay container
                    const overlay = document.createElement('div');
                    overlay.id = 'cspro-dialog-overlay';
                    overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:10000;display:flex;align-items:center;justify-content:center;';
            
                    // Create dialog container - set explicit initial height
                    const dialogContainer = document.createElement('div');
                    dialogContainer.id = 'cspro-dialog-container';
                    dialogContainer.style.cssText = 'background:white;border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.3);max-width:90vw;max-height:90vh;overflow:hidden;min-width:300px;min-height:180px;width:500px;height:200px;';
            
                    // Create iframe for dialog - explicit dimensions
                    const iframe = document.createElement('iframe');
                    iframe.id = 'cspro-dialog-iframe';
                    iframe.style.cssText = 'width:100%;height:100%;border:none;';
                    iframe.src = dialogPath;
            
                    // Handle iframe load errors
                    iframe.onerror = function(e) {
                        console.error('[WasmDialogBridge JS] iframe load error:', e);
                    };
            
                    dialogContainer.appendChild(iframe);
                    overlay.appendChild(dialogContainer);
                    document.body.appendChild(overlay);
            
                    // Store input data for the dialog to access
                    window.CSProDialogHandler._currentDialogInput = inputData;
                    window.CSProDialogHandler._currentDialogResolve = resolve;
            
                    // Listen for dialog close messages
                    const messageHandler = function(event) {
                        if (event.data && event.data.type === 'cspro-dialog-close') {
                            console.log("[WasmDialogBridge JS] Dialog closed with result:", event.data.result);
                            window.removeEventListener('message', messageHandler);
                            document.body.removeChild(overlay);
                            window.CSProDialogHandler._currentDialogInput = null;
                            window.CSProDialogHandler._currentDialogResolve = null;
                            resolve(event.data.result);
                        } else if (event.data && event.data.type === 'cspro-dialog-ready') {
                            // Dialog is ready, send input data
                            console.log("[WasmDialogBridge JS] Dialog ready, sending input data");
                            iframe.contentWindow.postMessage({
                                type: 'cspro-dialog-input',
                                input: inputData
                            }, '*');
                        } else if (event.data && event.data.type === 'cspro-dialog-resize') {
                            // Dialog wants to resize - update iframe dimensions
                            console.log("[WasmDialogBridge JS] Dialog resize request:", event.data.width, "x", event.data.height);
                            iframe.style.width = event.data.width + 'px';
                            iframe.style.height = event.data.height + 'px';
                            iframe.style.minWidth = event.data.width + 'px';
                            iframe.style.minHeight = event.data.height + 'px';
                        }
                    };
                    window.addEventListener('message', messageHandler);
            
                    // Handle ESC key to close dialog
                    const keyHandler = function(event) {
                        if (event.key === 'Escape') {
                            window.removeEventListener('keydown', keyHandler);
                            window.removeEventListener('message', messageHandler);
                            document.body.removeChild(overlay);
                            window.CSProDialogHandler._currentDialogInput = null;
                            window.CSProDialogHandler._currentDialogResolve = null;
                            resolve(null);
                        }
                    };
                    window.addEventListener('keydown', keyHandler);
                });
            };
        
            // showDialogAsync - called by jspi_showDialog in C++
            // C++ expects 1-based indices in the result!
            window.CSProDialogHandler.showDialogAsync = async function(dialogName, inputDataJson) {
                console.log("[WasmDialogBridge JS] showDialogAsync:", dialogName);
                try {
                    const data = inputDataJson ? JSON.parse(inputDataJson) : {};
            
                    if (dialogName === 'errmsg') {
                        // Try to use native HTML dialog
                        const dialogPath = '/dialogs/errmsg.html';
                        try {
                            const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, data);
                            if (result && result.result && typeof result.result.index === 'number') {
                                console.log("[WasmDialogBridge JS] HTML dialog returned index:", result.result.index);
                                return JSON.stringify({ index: result.result.index });
                            }
                        } catch (e) {
                            console.warn("[WasmDialogBridge JS] HTML dialog failed, using fallback:", e);
                        }
                
                        // Fallback to browser alert if HTML dialog fails
                        const message = data.message || data.title || "Error";
                        const title = data.title || "";
                        window.alert(title ? (title + "\n\n" + message) : message);
                        return JSON.stringify({ index: 1 }); // 1-based index!
                    }
            
                    if (dialogName === 'choice' || dialogName === 'select') {
                        const dialogPath = '/dialogs/' + dialogName + '.html';
                        try {
                            const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, data);
                            if (result && result.result) {
                                return JSON.stringify(result.result);
                            }
                        } catch (e) {
                            console.warn("[WasmDialogBridge JS] HTML dialog failed:", e);
                        }
                        return JSON.stringify({ index: 1 });
                    }
            
                    // Default: try HTML dialog, fallback to alert
                    const dialogPath = '/dialogs/' + dialogName + '.html';
                    try {
                        const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, data);
                        if (result && result.result) {
                            return JSON.stringify(result.result);
                        }
                    } catch (e) {
                        console.warn("[WasmDialogBridge JS] HTML dialog failed:", e);
                    }
            
                    const message = data.message || data.title || "Dialog: " + dialogName;
                    window.alert(message);
                    return JSON.stringify({ index: 1 }); // 1-based index!
                } catch (e) {
                    console.error("[WasmDialogBridge JS] showDialogAsync error:", e);
                    return JSON.stringify({ index: 1 }); // 1-based index!
                }
            };
        
            // showHtmlDialogAsync - for HTML-based dialogs
            window.CSProDialogHandler.showHtmlDialogAsync = async function(dialogPath, inputDataJson, optionsJson) {
                console.log("[WasmDialogBridge JS] showHtmlDialogAsync:", dialogPath);
                try {
                    const inputData = inputDataJson ? JSON.parse(inputDataJson) : {};
                    const options = optionsJson ? JSON.parse(optionsJson) : {};
            
                    const result = await window.CSProDialogHandler._showHtmlDialogInIframe(dialogPath, inputData);
                    if (result && result.result) {
                        return JSON.stringify(result.result);
                    }
                    return JSON.stringify({});
                } catch (e) {
                    console.error("[WasmDialogBridge JS] showHtmlDialogAsync error:", e);
                    return null;
                }
            };
        
            // showModalDialogAsync - for modal message boxes
            window.CSProDialogHandler.showModalDialogAsync = async function(title, message, mbType) {
                console.log("[WasmDialogBridge JS] showModalDialogAsync:", title, mbType);
                try {
                    // mbType determines button layout (OK, OK/Cancel, Yes/No, etc.)
                    // Use HTML dialog for better UI
                    let buttons = [{ caption: 'OK', index: 1 }];
                    if (mbType === 4 || mbType === 36) { // MB_YESNO
                        buttons = [{ caption: 'Yes', index: 1 }, { caption: 'No', index: 2 }];
                    } else if (mbType === 1 || mbType === 33) { // MB_OKCANCEL
                        buttons = [{ caption: 'OK', index: 1 }, { caption: 'Cancel', index: 2 }];
                    } else if (mbType === 3 || mbType === 35) { // MB_YESNOCANCEL
                        buttons = [{ caption: 'Yes', index: 1 }, { caption: 'No', index: 2 }, { caption: 'Cancel', index: 3 }];
                    }
            
                    const inputData = { title: title, message: message, buttons: buttons };
            
                    try {
                        const result = await window.CSProDialogHandler._showHtmlDialogInIframe('/dialogs/errmsg.html', inputData);
                        if (result && result.result && typeof result.result.index === 'number') {
                            // Convert dialog index to Windows button ID
                            const idx = result.result.index;
                            if (mbType === 4 || mbType === 36) { // MB_YESNO
                                return idx === 1 ? 6 : 7; // 6=IDYES, 7=IDNO
                            } else if (mbType === 1 || mbType === 33) { // MB_OKCANCEL
                                return idx === 1 ? 1 : 2; // 1=IDOK, 2=IDCANCEL
                            } else if (mbType === 3 || mbType === 35) { // MB_YESNOCANCEL
                                if (idx === 1) return 6; // IDYES
                                if (idx === 2) return 7; // IDNO
                                return 2; // IDCANCEL
                            }
                            return 1; // IDOK
                        }
                    } catch (e) {
                        console.warn("[WasmDialogBridge JS] HTML modal failed, using fallback:", e);
                    }
            
                    // Fallback to browser dialogs
                    if (mbType === 4 || mbType === 36) { // MB_YESNO types
                        const result = window.confirm(title + "\n\n" + message);
                        return result ? 6 : 7; // 6=IDYES, 7=IDNO
                    } else if (mbType === 1 || mbType === 33) { // MB_OKCANCEL types
                        const result = window.confirm(title + "\n\n" + message);
                        return result ? 1 : 2; // 1=IDOK, 2=IDCANCEL
                    } else {
                        window.alert(title + "\n\n" + message);
                        return 1; // IDOK
                    }
                } catch (e) {
                    console.error("[WasmDialogBridge JS] showModalDialogAsync error:", e);
                    return 1;
                }
            };
        
            console.log("[WasmDialogBridge JS] All dialog handlers registered with HTML dialog support");
        }
        ,
        'gov.census.cspro.engine.events.initEventHandler' : () => { window.CSProEventHandler = window.CSProEventHandler || {}; },
        'gov.census.cspro.platform.getVirtualFileContent_$external_fun' : (_this, p0) => _this.getVirtualFileContent(p0),
        'gov.census.cspro.platform.initApplication_$external_fun' : (_this, p0) => _this.initApplication(p0),
        'gov.census.cspro.platform.start_$external_fun' : (_this, ) => _this.start(),
        'gov.census.cspro.platform.modifyCase_$external_fun' : (_this, p0) => _this.modifyCase(p0),
        'gov.census.cspro.platform.nextField_$external_fun' : (_this, ) => _this.nextField(),
        'gov.census.cspro.platform.previousField_$external_fun' : (_this, ) => _this.previousField(),
        'gov.census.cspro.platform.onStop_$external_fun' : (_this, ) => _this.onStop(),
        'gov.census.cspro.platform.partialSave_$external_fun' : (_this, ) => _this.partialSave(),
        'gov.census.cspro.platform.setFieldValueAndAdvance_$external_fun' : (_this, p0) => _this.setFieldValueAndAdvance(p0),
        'gov.census.cspro.platform.processPossibleRequests_$external_fun' : (_this, ) => _this.processPossibleRequests(),
        'gov.census.cspro.platform.processAction_$external_fun' : (_this, p0, p1) => _this.processAction(p0, p1),
        'gov.census.cspro.platform.getCurrentPage_$external_fun' : (_this, ) => _this.getCurrentPage(),
        'gov.census.cspro.platform.getStopCode_$external_fun' : (_this, ) => _this.getStopCode(),
        'gov.census.cspro.platform.getCaseTree_$external_fun' : (_this, ) => _this.getCaseTree(),
        'gov.census.cspro.platform.setFieldValueByName_$external_fun' : (_this, p0, p1) => _this.setFieldValueByName(p0, p1),
        'gov.census.cspro.platform.getSequentialCaseIds_$external_fun' : (_this, ) => _this.getSequentialCaseIds(),
        'gov.census.cspro.storage.jsIsOpfsAvailable' : () => typeof navigator !== 'undefined' && navigator.storage && typeof navigator.storage.getDirectory === 'function',
        'gov.census.cspro.storage.jsGetStorageRoot' : () => navigator.storage.getDirectory(),
        'gov.census.cspro.storage.jsHandlePromise' : (promise, resolve, reject) => {
            try {
                promise.then(
                    (result) => { try { resolve(result); } catch(e) { console.error('[jsHandlePromise] Error in resolve callback:', e); } },
                    (error) => { try { reject(error); } catch(e) { console.error('[jsHandlePromise] Error in reject callback:', e); } }
                );
            } catch(e) {
                console.error('[jsHandlePromise] Error setting up promise:', e);
                try { reject(e); } catch(e2) { console.error('[jsHandlePromise] Error calling reject:', e2); }
            }
        },
        'gov.census.cspro.storage.jsGetDirectoryHandle' : (dir, name, create) => dir.getDirectoryHandle(name, { create: create }),
        'gov.census.cspro.storage.jsGetFileHandle' : (dir, name, create) => dir.getFileHandle(name, { create: create }),
        'gov.census.cspro.storage.jsCreateWritable' : (handle) => handle.createWritable(),
        'gov.census.cspro.storage.jsWriteToStream' : (writable, data) => writable.write(data),
        'gov.census.cspro.storage.jsCloseStream' : (writable) => writable.close(),
        'gov.census.cspro.storage.jsGetFile' : (handle) => handle.getFile(),
        'gov.census.cspro.storage.jsFileToArrayBuffer' : (file) => file.arrayBuffer(),
        'gov.census.cspro.storage.jsGetEntries' : (dir) => dir.entries(),
        'gov.census.cspro.storage.jsIteratorToArray' : (iterator) => {
            return (async function() {
                const arr = [];
                for await (const entry of iterator) {
                    arr.push(entry[1]);
                }
                return arr;
            })();
        },
        'gov.census.cspro.storage.jsArrayLength' : (arr) => arr.length,
        'gov.census.cspro.storage.jsArrayGet' : (arr, idx) => arr[idx],
        'gov.census.cspro.storage.jsGetStringProperty' : (obj, key) => typeof obj[key] === 'string' ? obj[key] : null,
        'gov.census.cspro.storage.jsCreateUint8Array' : (length) => new Uint8Array(length),
        'gov.census.cspro.storage.jsSetUint8ArrayElement' : (arr, idx, value) => { arr[idx] = value; },
        'gov.census.cspro.storage.jsUint8ArrayLength' : (arr) => arr.length,
        'gov.census.cspro.storage.jsGetUint8ArrayElement' : (arr, idx) => arr[idx],
        'gov.census.cspro.storage.jsUint8ArrayFromBuffer' : (buffer) => new Uint8Array(buffer),
        'gov.census.cspro.storage.jsLogInfo' : (msg) => console.log('[OpfsService] ' + msg),
        'gov.census.cspro.storage.jsLogError' : (msg) => console.error('[OpfsService] ' + msg),
        'gov.census.cspro.storage.jsLogInfo_1' : (msg) => console.log('[WasmFsBridge] ' + msg),
        'gov.census.cspro.storage.jsLogError_1' : (msg) => console.error('[WasmFsBridge] ' + msg),
        'gov.census.cspro.storage.jsCreateWasmDirectory' : (path) => {
            if (!window.CSProModule || !window.CSProModule.FS) {
                console.error('[WasmFsBridge] CSProModule.FS not available');
                return;
            }
            const FS = window.CSProModule.FS;
        
            // Split path into parts and create each directory
            const parts = path.split('/').filter(p => p.length > 0);
            let currentPath = '';
        
            for (const part of parts) {
                currentPath += '/' + part;
                try {
                    FS.stat(currentPath);
                } catch (e) {
                    // Directory doesn't exist, create it
                    try {
                        FS.mkdir(currentPath);
                        console.log('[WasmFsBridge] Created directory: ' + currentPath);
                    } catch (mkdirError) {
                        // Might already exist, ignore
                    }
                }
            }
        },
        'gov.census.cspro.storage.jsWriteFileToWasmFsInternal' : (path, contentJson) => {
            if (!window.CSProModule || !window.CSProModule.FS) {
                throw new Error('CSProModule.FS not available');
            }
            const FS = window.CSProModule.FS;
        
            // Parse the JSON array back to Uint8Array
            const contentArray = JSON.parse(contentJson);
            const uint8Array = new Uint8Array(contentArray);
        
            // Write file to WASM FS
            FS.writeFile(path, uint8Array);
            console.log('[WasmFsBridge] Wrote file: ' + path + ' (' + uint8Array.length + ' bytes)');
        },
        'gov.census.cspro.storage.jsListWasmFilesRecursiveInternal' : (path) => {
            if (!window.CSProModule || !window.CSProModule.FS) {
                console.error('[WasmFsBridge] CSProModule.FS not available');
                return [];
            }
            const FS = window.CSProModule.FS;
            const out = [];
            const stack = [path];
            while (stack.length) {
                const dir = stack.pop();
                let entries = [];
                try {
                    entries = FS.readdir(dir);
                } catch (e) {
                    continue;
                }
                for (const name of entries) {
                    if (name === '.' || name === '..') continue;
                    const full = (dir.endsWith('/') ? dir.slice(0, -1) : dir) + '/' + name;
                    let st;
                    try {
                        st = FS.stat(full);
                    } catch (e) {
                        continue;
                    }
                    try {
                        if (FS.isDir(st.mode)) stack.push(full);
                        else out.push(full);
                    } catch (e) {
                        // Ignore unexpected stat/mode values
                    }
                }
            }
            return out;
        },
        'gov.census.cspro.storage.jsReadWasmFileAsArrayBufferInternal' : (path) => {
            if (!window.CSProModule || !window.CSProModule.FS) {
                throw new Error('CSProModule.FS not available');
            }
            const FS = window.CSProModule.FS;
            const data = FS.readFile(path); // Uint8Array
            // Slice to a standalone ArrayBuffer for safe use by OPFS writer.
            const ab = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
            return ab;
        },
        'gov.census.cspro.storage.jsGetArrayLength' : (arr) => arr.length,
        'gov.census.cspro.storage.jsGetArrayStringElement' : (arr, idx) => arr[idx],
        'gov.census.cspro.ui.jsCreateInputElement' : () => { const input = document.createElement('input'); input.type = 'file'; return input; },
        'gov.census.cspro.ui.jsSetMultiple' : (input) => { input.multiple = true; },
        'gov.census.cspro.ui.jsSetWebkitDirectory' : (input) => { input.webkitdirectory = true; },
        'gov.census.cspro.ui.jsSetAccept' : (input, accept) => { input.accept = accept; },
        'gov.census.cspro.ui.jsGetFileList' : (input) => input.files,
        'gov.census.cspro.ui.jsGetFileWebkitRelativePath' : (file) => file.webkitRelativePath || file.name,
        'gov.census.cspro.ui.jsReadFileAsArrayBuffer' : (file) => file.arrayBuffer(),
        'gov.census.cspro.ui.jsDateNow' : () => Date.now(),
        'gov.census.cspro.ui.jsFileListItem' : (files, i) => files.item(i),
        'gov.census.cspro.ui.jsFileListLength' : (files) => files ? files.length : 0,
        'gov.census.cspro.ui.jsGetFileName' : (file) => file.name,
        'gov.census.cspro.ui.jsGetFileSize' : (file) => file.size,
        'gov.census.cspro.ui.jsPromiseThen' : (promise, onResolve, onReject) => promise.then(onResolve).catch(onReject),
        'gov.census.cspro.ui.jsCreateInputElement_1' : () => { const input = document.createElement('input'); input.type = 'file'; return input; },
        'gov.census.cspro.ui.jsSetWebkitDirectory_1' : (input) => { input.webkitdirectory = true; },
        'gov.census.cspro.ui.jsGetInputFiles' : (input) => input.files,
        'gov.census.cspro.ui.jsFileListItem_1' : (files, i) => files.item(i),
        'gov.census.cspro.ui.jsFileListLength_1' : (files) => files ? files.length : 0,
        'gov.census.cspro.ui.jsGetRelativePath' : (file) => file.webkitRelativePath || file.name,
        'gov.census.cspro.ui.jsReadArrayBuffer' : (file) => file.arrayBuffer(),
        'gov.census.cspro.ui.jsArrayBufferToUint8Array' : (buffer) => new Uint8Array(buffer),
        'gov.census.cspro.ui.jsUint8ArrayGet' : (arr, i) => arr[i],
        'gov.census.cspro.ui.jsUint8ArrayLength' : (arr) => arr.length,
        'gov.census.cspro.ui.jsDateNow_1' : () => Date.now(),
        'gov.census.cspro.ui.jsPromiseThenCallback' : (promise, callback) => promise.then((result) => callback(result)),
        'gov.census.cspro.ui.jsNodeListLength' : (nodeList) => nodeList.length,
        'gov.census.cspro.ui.jsNodeListItem' : (nodeList, i) => nodeList.item(i),
        'gov.census.cspro.ui.jsGetWindowMessageType' : (data) => (data && data.type !== undefined) ? String(data.type) : '',
        'gov.census.cspro.ui.jsGetActionRequestId' : (data) => (data && data.requestId !== undefined) ? Number(data.requestId) : 0,
        'gov.census.cspro.ui.jsGetActionMethod' : (data) => (data && data.method !== undefined) ? String(data.method) : '',
        'gov.census.cspro.ui.jsGetActionArgs' : (data) => data ? data.args : null,
        'gov.census.cspro.ui.jsGetActionCode' : (data) => (data && data.actionCode !== undefined) ? Number(data.actionCode) : 0,
        'gov.census.cspro.ui.jsMakeActionResponse' : (requestId, result, error) => ({ type:'cspro-action-response', requestId: requestId, result: result, error: error }),
        'gov.census.cspro.ui.jsBuildActionInvokerMessage' : (method, args) => JSON.stringify({ action: method, arguments: args || {} }),
        'gov.census.cspro.ui.jsPostMessage' : (source, message) => { try { if (source && source.postMessage) { source.postMessage(message, '*'); return true; } } catch (e) {} return false; },
        'gov.census.cspro.ui.jsGetLastMessageSource' : () => window.__csproLastMessageSource || null,
        'gov.census.cspro.ui.jsSetupMessageListener' : (callback) => {
            window.addEventListener('message', function(event) {
                // Store the source for potential responses
                window.__csproLastMessageSource = event.source;
        
                const data = event.data;
                if (!data || typeof data !== 'object') {
                    return;
                }
        
                // Call the Kotlin callback with the data object
                callback(data);
            });
            console.log('[EntryActivity] Message listener installed via JavaScript');
        },
        'gov.census.cspro.ui.jsGetQuestionTextMessageType' : (e) => e?.data?.type || '',
        'gov.census.cspro.ui.jsGetQuestionTextFrameId' : (e) => e?.data?.frameId || '',
        'gov.census.cspro.ui.jsGetQuestionTextFrameHeight' : (e) => (e?.data?.height ?? 0),
        'gov.census.cspro.util.jsLog' : (level, message, data) => {
            if (typeof window !== 'undefined' && window.logger) {
                const dataStr = data ? data : null;
                switch(level) {
                    case 'DEBUG': window.logger.debug(message, dataStr); break;
                    case 'WARN': window.logger.warn(message, dataStr); break;
                    case 'ERROR': window.logger.error(message, dataStr); break;
                    default: window.logger.info(message, dataStr);
                }
            } else {
                console.log('[' + level + ']', message, data || '');
            }
        },
        'gov.census.cspro.util.jsLogCategory' : (category, action, details) => {
            if (typeof window !== 'undefined' && window.logger) {
                window.logger.log('INFO', '[' + category + '] ' + action, details ? JSON.parse(details) : null);
            } else {
                console.log('[' + category + ']', action, details || '');
            }
        }
    }
    
    // Placed here to give access to it from externals (js_code)
    let wasmInstance;
    let require; 
    let wasmExports;

    const isNodeJs = (typeof process !== 'undefined') && (process.release.name === 'node');
    const isDeno = !isNodeJs && (typeof Deno !== 'undefined')
    const isStandaloneJsVM =
        !isDeno && !isNodeJs && (
            typeof d8 !== 'undefined' // V8
            || typeof inIon !== 'undefined' // SpiderMonkey
            || typeof jscOptions !== 'undefined' // JavaScriptCore
        );
    const isBrowser = !isNodeJs && !isDeno && !isStandaloneJsVM && (typeof window !== 'undefined' || typeof self !== 'undefined');
    
    if (!isNodeJs && !isDeno && !isStandaloneJsVM && !isBrowser) {
      throw "Supported JS engine not detected";
    }
    
    const wasmFilePath = './csentry-web.wasm';
    const importObject = {
        js_code,

    };
    
    try {
      if (isNodeJs) {
        const module = await import(/* webpackIgnore: true */'node:module');
        const importMeta = import.meta;
        require = module.default.createRequire(importMeta.url);
        const fs = require('fs');
        const url = require('url');
        const filepath = import.meta.resolve(wasmFilePath);
        const wasmBuffer = fs.readFileSync(url.fileURLToPath(filepath));
        const wasmModule = new WebAssembly.Module(wasmBuffer);
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
      }
      
      if (isDeno) {
        const path = await import(/* webpackIgnore: true */'https://deno.land/std/path/mod.ts');
        const binary = Deno.readFileSync(path.fromFileUrl(import.meta.resolve(wasmFilePath)));
        const module = await WebAssembly.compile(binary);
        wasmInstance = await WebAssembly.instantiate(module, importObject);
      }
      
      if (isStandaloneJsVM) {
        const wasmBuffer = read(wasmFilePath, 'binary');
        const wasmModule = new WebAssembly.Module(wasmBuffer);
        wasmInstance = new WebAssembly.Instance(wasmModule, importObject);
      }
      
      if (isBrowser) {
        wasmInstance = (await WebAssembly.instantiateStreaming(fetch(wasmFilePath), importObject)).instance;
      }
    } catch (e) {
      if (e instanceof WebAssembly.CompileError) {
        let text = `Please make sure that your runtime environment supports the latest version of Wasm GC and Exception-Handling proposals.
For more information, see https://kotl.in/wasm-help
`;
        if (isBrowser) {
          console.error(text);
        } else {
          const t = "\n" + text;
          if (typeof console !== "undefined" && console.log !== void 0) 
            console.log(t);
          else 
            print(t);
        }
      }
      throw e;
    }
    
    wasmExports = wasmInstance.exports;
    if (runInitializer) {
        wasmExports._initialize();
    }

    return { instance: wasmInstance,  exports: wasmExports };
}
