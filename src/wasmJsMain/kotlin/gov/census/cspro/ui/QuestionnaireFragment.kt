package gov.census.cspro.ui

import gov.census.cspro.data.CDEField
import gov.census.cspro.data.EntryPage
import gov.census.cspro.engine.CSProEngineService
import gov.census.cspro.util.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

@JsFun("""(e) => e?.data?.type || ''""")
private external fun jsGetQuestionTextMessageType(e: kotlin.js.JsAny?): String

@JsFun("""(e) => e?.data?.frameId || ''""")
private external fun jsGetQuestionTextFrameId(e: kotlin.js.JsAny?): String

@JsFun("""(e) => (e?.data?.height ?? 0)""")
private external fun jsGetQuestionTextFrameHeight(e: kotlin.js.JsAny?): Int

/**
 * Web equivalent of Android's QuestionnaireFragment
 * Renders the data entry form based on CDEField models from the engine
 * 
 * Mirrors Android CSEntryDroid UI exactly, including field labels, inputs,
 * and value set (radio button/dropdown) rendering.
 */
class QuestionnaireFragment : BaseFragment() {
    private var container: HTMLDivElement? = null
    private var onFieldChangeListener: ((CDEField, String) -> Unit)? = null
    private var onFieldFocusListener: ((CDEField) -> Unit)? = null
    private var currentPage: EntryPage? = null

    // If we render question text via innerHTML, <script> tags do NOT execute.
    // CSPro QSF question text frequently relies on scripts (e.g., CSProActionInvoker + Logic.evalAsync).
    // We therefore render question text in sandboxed iframes using srcdoc.
    private val questionTextFrames = linkedMapOf<String, String>()
    private var iframeResizeListener: ((Event) -> Unit)? = null
    
    /**
     * Get the current page being displayed.
     * Used to access currentFieldIndex for proper validation flow.
     */
    fun getCurrentPage(): EntryPage? = currentPage
    
    override fun onCreateView() {
        container = document.createElement("div") as HTMLDivElement
        container?.id = "questionnaire-root"
        container?.className = "questionnaire-fragment"
        
        val parent = document.getElementById("questionnaire-fragment")
        parent?.innerHTML = ""
        parent?.appendChild(container!!)
    }
    
    fun setOnFieldChangeListener(listener: (CDEField, String) -> Unit) {
        onFieldChangeListener = listener
    }

    fun setOnFieldFocusListener(listener: (CDEField) -> Unit) {
        onFieldFocusListener = listener
    }
    
    fun displayPage(page: EntryPage) {
        currentPage = page
        render()
    }
    
    private fun render() {
        val page = currentPage ?: return
        val root = container ?: return

        Logger.page("render", mapOf(
            "fieldCount" to page.fields.size,
            "occurrenceLabel" to page.occurrenceLabel,
            "questionTextUrl" to page.questionTextUrl,
            "helpTextUrl" to page.helpTextUrl,
            "currentFieldIndex" to page.currentFieldIndex
        ))

        // Reset per-render iframe srcdocs
        questionTextFrames.clear()
        
        // Match Android layout: fragment_questionnaire_layout.xml
        // Structure: occurrence_label -> block_question_text -> block_help_text -> divider -> question_list_recycler_view
        val html = StringBuilder()
        
        // Occurrence label (like "Person 1")
        if (page.occurrenceLabel.isNotEmpty()) {
            html.append("""<div class="occurrence-label">${page.occurrenceLabel}</div>""")
        }
        
        // Block question text (CAPI for the entire block, e.g. roster instructions)
        val blockQuestionHtml = getQuestionTextHtml(page.questionTextUrl)
        if (blockQuestionHtml.isNotEmpty()) {
            val frameId = registerQuestionTextFrame(blockQuestionHtml)
            html.append("""<div class="block-question-text">${renderQuestionTextFrame(frameId)}</div>""")
        }
        
        // Block help text (collapsible)
        val blockHelpHtml = getQuestionTextHtml(page.helpTextUrl)
        if (blockHelpHtml.isNotEmpty()) {
            val frameId = registerQuestionTextFrame(blockHelpHtml)
            html.append("""
                <button class="toggle-block-help-btn" onclick="this.nextElementSibling.classList.toggle('hidden')">ℹ️ Help</button>
                <div class="block-help-text hidden">${renderQuestionTextFrame(frameId)}</div>
            """)
        }
        
        // Divider (if any header content was shown)
        if (page.occurrenceLabel.isNotEmpty() || blockQuestionHtml.isNotEmpty() || blockHelpHtml.isNotEmpty()) {
            html.append("""<div class="block-divider"></div>""")
        }
        
        // Fields container - displays ALL fields on the same page (like Android QuestionListAdapter)
        html.append("""<div class="fields-container">""")
        
        // Render each field - multiple fields for blocks/rosters
        for ((index, field) in page.fields.withIndex()) {
            val isFirst = index == 0
            val isCurrent = index == page.currentFieldIndex
            html.append(renderField(field, isFirstField = isFirst, isCurrentField = isCurrent))
        }
        
        html.append("</div>") // Close fields-container
        
        root.innerHTML = html.toString()
        attachFieldListeners()

        // Ensure scripts in question text execute (QSF/CSProActionInvoker)
        attachQuestionTextFrames()
        
        // Focus current editable field (mirrors engine currentFieldIndex)
        focusCurrentEditableField()
    }
    
    private fun focusCurrentEditableField() {
        val page = currentPage ?: return
        val root = container ?: return
        val idx = page.currentFieldIndex.coerceIn(0, (page.fields.size - 1).coerceAtLeast(0))
        val field = page.fields.getOrNull(idx) ?: return

        val selector = "input[data-field-name='${field.name}'], .value-row[data-field-name='${field.name}'], .select-current-value[data-field-name='${field.name}']"
        val el = root.querySelector(selector) as? HTMLElement
        el?.focus()
    }
    
    private fun renderField(field: CDEField, isFirstField: Boolean = false, isCurrentField: Boolean = false): String {
        val fieldClass = buildString {
            append("field-container")
            if (isFirstField) append(" first-field")
            if (isCurrentField) append(" current-field")
        }
        val protectedAttr = if (field.isProtected || field.isReadOnly) "readonly" else ""
        
        // Get question text HTML content from virtual file system
        val questionTextHtml = getQuestionTextHtml(field.questionTextUrl)

        val questionTextRendered = if (questionTextHtml.isNotEmpty()) {
            val frameId = registerQuestionTextFrame(questionTextHtml)
            "<div class='question-text'>${renderQuestionTextFrame(frameId)}</div>"
        } else {
            ""
        }
        
        // Debug log for field rendering
        Logger.field(field.name, "renderField", mapOf(
            "isNumeric" to field.isNumeric,
            "isAlpha" to field.isAlpha,
            "captureType" to field.captureType,
            "questionTextUrl" to field.questionTextUrl,
            "valueSetSize" to field.valueSet.size,
            "hasQuestionText" to questionTextRendered.isNotEmpty()
        ))
        
        // Render based on captureType first (matching Android's QuestionWidgetFactory)
        // The captureType is the authoritative source for how to render the field
        val content = when (field.captureType.lowercase()) {
            "radiobutton" -> {
                // Radio buttons - use valueSet if available
                if (field.valueSet.isNotEmpty()) {
                    renderRadioButtons(field)
                } else {
                    // Fallback to text input if no value set (shouldn't happen normally)
                    Logger.warn("radiobutton captureType but no valueSet", mapOf("fieldName" to field.name))
                    renderTextInput(field, protectedAttr)
                }
            }
            "checkbox" -> renderCheckbox(field)
            "dropdown" -> {
                if (field.valueSet.isNotEmpty()) {
                    renderDropdown(field)
                } else {
                    renderTextInput(field, protectedAttr)
                }
            }
            "combobox" -> {
                // Combo box is like a dropdown but with editable text
                if (field.valueSet.isNotEmpty()) {
                    renderDropdown(field) // For now render as dropdown
                } else {
                    renderTextInput(field, protectedAttr)
                }
            }
            "date" -> renderDateInput(field, protectedAttr)
            "slider" -> renderSlider(field)
            "togglebutton" -> {
                if (field.valueSet.isNotEmpty()) {
                    renderRadioButtons(field) // Render toggles as radio for now
                } else {
                    renderTextInput(field, protectedAttr)
                }
            }
            else -> {
                // Default: textbox - check if has valueSet for backwards compatibility
                if (field.valueSet.isNotEmpty() && field.valueSet.size <= 5) {
                    renderRadioButtons(field)
                } else if (field.valueSet.isNotEmpty()) {
                    renderDropdown(field)
                } else {
                    renderTextInput(field, protectedAttr)
                }
            }
        }
        
        // Android question_layout.xml structure:
        // 1. questionLabel (field label) - Title style
        // 2. questionText (CAPI text) - Medium style, below label
        // 3. Input widget (text box, radio buttons, etc.)
        // 4. Notes (optional)
        
        return """
            <div class="$fieldClass" data-field-name="${field.name}">
                <div class="question-label">${field.label}</div>
                $questionTextRendered
                <div class="field-input-container">
                    $content
                </div>
                ${if (field.note.isNotEmpty()) "<div class='field-note'><span class='note-icon'>📝</span> Note: ${field.note}</div>" else ""}
            </div>
        """.trimIndent()
    }

    private fun renderQuestionTextFrame(frameId: String): String {
        return """
            <iframe
                id="$frameId"
                class="question-text-iframe"
                scrolling="no"
                sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
                style="width:100%; border:0; display:block; background:transparent;"
            ></iframe>
        """.trimIndent()
    }

    private fun registerQuestionTextFrame(html: String): String {
        val frameId = "qt-frame-${questionTextFrames.size + 1}"
        questionTextFrames[frameId] = injectScriptsForQsf(frameId, html)
        return frameId
    }

    /**
     * Inject required scripts into QSF HTML so it can execute CSPro logic.
     * Native Android injects AndroidActionInvoker via addJavascriptInterface().
     * For web, we inject a custom CSProActionInvoker shim directly into the HTML.
     * This shim forwards all actions to the parent window via postMessage.
     * Also inject a resize observer so the parent can size the iframe properly.
     * 
     * Based on CSEntryWeb approach in capi-renderer.js and question-text-handler.js.
     */
    private fun injectScriptsForQsf(frameId: String, html: String): String {
        // Base tag so relative URLs resolve against the parent's origin
        val baseTag = """<base href="/">"""

        // CSProActionInvoker shim - forwards all actions to parent window via postMessage.
        // This is injected inline (not via script src) so it executes immediately in srcdoc iframes.
        val actionInvokerShim = """
<script>
// CSProActionInvoker shim - forwards actions to parent frame -> WASM engine
console.log('[CAPI Shim] Loading CSProActionInvoker shim...');
(function() {
    class CSProActionInvoker {
        static ${"$"}Impl = {
            pendingCallbacks: {},
            nextRequestId: 1,
            
            run(aiThis, actionCode, args) {
                // Sync calls just send message and return empty (real response requires async)
                console.log('[CAPI Shim] sync action:', actionCode, args);
                window.parent.postMessage({ 
                    type: 'cspro-action', 
                    actionCode: actionCode, 
                    args: args,
                    accessToken: aiThis.accessToken
                }, '*');
                return '';
            },
            
            runAsync(aiThis, actionCode, args) {
                const requestId = CSProActionInvoker.${"$"}Impl.nextRequestId++;
                console.log('[CAPI Shim] async action:', actionCode, 'requestId:', requestId, 'args:', args);
                console.log('[CAPI Shim] window.parent:', window.parent, 'posting to *');
                return new Promise((resolve, reject) => {
                    CSProActionInvoker.${"$"}Impl.pendingCallbacks[requestId] = { resolve, reject };
                    window.parent.postMessage({ 
                        type: 'cspro-action-async', 
                        actionCode: actionCode, 
                        args: args,
                        requestId: requestId,
                        accessToken: aiThis.accessToken
                    }, '*');
                    console.log('[CAPI Shim] message posted');
                    // Timeout after 30 seconds
                    setTimeout(() => {
                        if (CSProActionInvoker.${"$"}Impl.pendingCallbacks[requestId]) {
                            delete CSProActionInvoker.${"$"}Impl.pendingCallbacks[requestId];
                            resolve('');
                        }
                    }, 30000);
                });
            },
            
            handleResponse(requestId, result) {
                const callback = CSProActionInvoker.${"$"}Impl.pendingCallbacks[requestId];
                if (callback) {
                    delete CSProActionInvoker.${"$"}Impl.pendingCallbacks[requestId];
                    callback.resolve(result);
                }
            }
        };

        constructor(accessToken) { 
            this.accessToken = accessToken || ''; 
            this._setupNamespaces();
        }
        
        _setupNamespaces() {
            const self = this;
            
            // UI namespace
            this.UI = {
                alert: (args) => CSProActionInvoker.${"$"}Impl.run(self, 31133, args),
                alertAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 31133, args),
                closeDialog: (args) => CSProActionInvoker.${"$"}Impl.run(self, 60265, args),
                closeDialogAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 60265, args),
                getInputData: () => CSProActionInvoker.${"$"}Impl.run(self, 57200, undefined),
                getInputDataAsync: () => CSProActionInvoker.${"$"}Impl.runAsync(self, 57200, undefined),
                getDisplayOptions: () => ({}),
                getDisplayOptionsAsync: () => Promise.resolve({}),
                getMaxDisplayDimensions: () => ({ width: window.innerWidth || 800, height: window.innerHeight || 600 }),
                getMaxDisplayDimensionsAsync: () => Promise.resolve({ width: window.innerWidth || 800, height: window.innerHeight || 600 }),
                setDisplayOptions: (args) => null,
                setDisplayOptionsAsync: (args) => Promise.resolve(null),
                showDialog: (args) => CSProActionInvoker.${"$"}Impl.run(self, 49835, args),
                showDialogAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 49835, args)
            };
            
            // Logic namespace - key for QSF-defined HTML controls using Logic.evalAsync
            this.Logic = {
                eval: (args) => CSProActionInvoker.${"$"}Impl.run(self, 50799, args),
                evalAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 50799, args),
                invoke: (args) => CSProActionInvoker.${"$"}Impl.run(self, 41927, args),
                invokeAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 41927, args),
                getSymbol: (args) => CSProActionInvoker.${"$"}Impl.run(self, 44034, args),
                getSymbolAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 44034, args),
                getSymbolValue: (args) => CSProActionInvoker.${"$"}Impl.run(self, 22923, args),
                getSymbolValueAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 22923, args),
                updateSymbolValue: (args) => CSProActionInvoker.${"$"}Impl.run(self, 65339, args),
                updateSymbolValueAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 65339, args)
            };
            
            // File namespace
            this.File = {
                copy: (args) => CSProActionInvoker.${"$"}Impl.run(self, 37688, args),
                copyAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 37688, args),
                readText: (args) => CSProActionInvoker.${"$"}Impl.run(self, 29118, args),
                readTextAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 29118, args),
                writeText: (args) => CSProActionInvoker.${"$"}Impl.run(self, 60631, args),
                writeTextAsync: (args) => CSProActionInvoker.${"$"}Impl.runAsync(self, 60631, args)
            };
            
            // Settings namespace
            this.Settings = {
                getValue: (args) => { try { return localStorage.getItem('cspro_' + (args?.key || args)); } catch(e) { return null; } },
                getValueAsync: (args) => Promise.resolve(self.Settings.getValue(args)),
                putValue: (args) => { try { localStorage.setItem('cspro_' + args?.key, args?.value); } catch(e) {} return null; },
                putValueAsync: (args) => Promise.resolve(self.Settings.putValue(args))
            };
        }
        
        getWindowForEventListener() { return window; }
        
        execute(args) { return CSProActionInvoker.${"$"}Impl.run(this, 11276, args); }
        executeAsync(args) { return CSProActionInvoker.${"$"}Impl.runAsync(this, 11276, args); }
        
        registerAccessToken(args) { this.accessToken = args?.accessToken || args || ''; return null; }
        registerAccessTokenAsync(args) { return Promise.resolve(this.registerAccessToken(args)); }
    }

    // Make globally available
    window.CSProActionInvoker = CSProActionInvoker;
    console.log('[CAPI Shim] CSProActionInvoker class registered globally');
    
    // ============================================================
    // Create a default CSProActionInvoker instance for the CSPro global object
    // This matches how Android exposes the CSPro JavaScript interface
    // ============================================================
    var _csproAI = new CSProActionInvoker('');
    
    // ============================================================
    // CSPro GLOBAL OBJECT - matches Android CSProJavaScriptInterface
    // This is the key interface that QSF HTML uses to interact with CSPro
    // ============================================================
    window.CSPro = {
        // CSPro.runLogic(logic) - Execute CSPro logic synchronously
        // Android: CSProJavaScriptInterface.runLogic -> Logic.eval
        runLogic: function(logic) {
            console.log('[CAPI Shim] CSPro.runLogic() called:', logic);
            // Use async internally since we're in iframe, but fire-and-forget for sync API
            _csproAI.Logic.evalAsync({ logic: logic }).then(function(result) {
                console.log('[CAPI Shim] CSPro.runLogic result:', result);
            }).catch(function(err) {
                console.error('[CAPI Shim] CSPro.runLogic error:', err);
            });
            return null; // Sync returns null, actual execution is async
        },
        
        // CSPro.runLogicAsync(logic) - Execute CSPro logic asynchronously (no callback)
        runLogicAsync: function(logic, callback) {
            console.log('[CAPI Shim] CSPro.runLogicAsync() called:', logic, 'callback:', callback);
            _csproAI.Logic.evalAsync({ logic: logic }).then(function(result) {
                console.log('[CAPI Shim] CSPro.runLogicAsync result:', result);
                window.lastAsyncResult = result;
                if (callback) {
                    try { eval(callback); } catch(e) { console.error('[CAPI Shim] Callback error:', e); }
                }
            }).catch(function(err) {
                console.error('[CAPI Shim] CSPro.runLogicAsync error:', err);
            });
        },
        
        // CSPro.invoke(functionName, arguments) - Call a CSPro user function
        invoke: function(functionName, args) {
            console.log('[CAPI Shim] CSPro.invoke() called:', functionName, args);
            _csproAI.Logic.invokeAsync({ 'function': functionName, 'arguments': args }).then(function(result) {
                console.log('[CAPI Shim] CSPro.invoke result:', result);
            });
            return null;
        },
        
        // CSPro.invokeAsync(functionName, arguments, callback)
        invokeAsync: function(functionName, args, callback) {
            console.log('[CAPI Shim] CSPro.invokeAsync() called:', functionName, args);
            _csproAI.Logic.invokeAsync({ 'function': functionName, 'arguments': args }).then(function(result) {
                console.log('[CAPI Shim] CSPro.invokeAsync result:', result);
                window.lastAsyncResult = result;
                if (callback) {
                    try { eval(callback); } catch(e) { console.error('[CAPI Shim] Callback error:', e); }
                }
            });
        },
        
        // CSPro.do(action) - Perform an action (e.g., "close")
        'do': function(action, input) {
            console.log('[CAPI Shim] CSPro.do() called:', action, input);
            if (action === 'close') {
                _csproAI.UI.closeDialog();
            }
            return null;
        },
        
        // CSPro.getInputData() - Get input data passed to dialog
        getInputData: function() {
            return _csproAI.UI.getInputData();
        },
        
        // CSPro.returnData(jsonText) - Return data from dialog and close
        returnData: function(jsonText) {
            console.log('[CAPI Shim] CSPro.returnData() called:', jsonText);
            _csproAI.UI.closeDialog({ result: jsonText });
        },
        
        // CSPro.getAsyncResult() - Get the last async result
        getAsyncResult: function() {
            return window.lastAsyncResult || null;
        },
        
        // CSPro.getMaxDisplayWidth/Height - Display dimensions
        getMaxDisplayWidth: function() {
            var dims = _csproAI.UI.getMaxDisplayDimensions();
            return dims ? dims.width : window.innerWidth || 800;
        },
        getMaxDisplayHeight: function() {
            var dims = _csproAI.UI.getMaxDisplayDimensions();
            return dims ? dims.height : window.innerHeight || 600;
        },
        
        // CSPro.setDisplayOptions - Set display options
        setDisplayOptions: function(jsonText) {
            _csproAI.UI.setDisplayOptions(typeof jsonText === 'string' ? JSON.parse(jsonText) : jsonText);
        }
    };
    console.log('[CAPI Shim] CSPro global object created with runLogic, invoke, do, etc.');
    
    // ============================================================
    // NAVIGATION FUNCTIONS - for roster/field navigation
    // These dispatch special actions that EntryActivity handles
    // ============================================================
    function dispatchQsfAction(action, payload) {
        console.log('[CAPI Shim] Dispatching QSF action:', action, payload);
        window.parent.postMessage({
            type: 'cspro-qsf-action',
            action: action,
            payload: payload || ''
        }, '*');
    }
    
    // endRoster() - called when user clicks "end the roster" link
    window.endRoster = function() {
        console.log('[CAPI Shim] endRoster() called');
        dispatchQsfAction('endRoster', '');
    };
    
    // moveToField(fieldName) - navigate to a specific field
    window.moveToField = function(fieldName) {
        console.log('[CAPI Shim] moveToField() called:', fieldName);
        dispatchQsfAction('moveToField', fieldName || '');
    };
    
    // advance() - advance to next field
    window.advance = function() {
        console.log('[CAPI Shim] advance() called');
        dispatchQsfAction('advance', '');
    };
    
    // UI namespace for compatibility (some QSF may use UI.endRoster)
    window.UI = window.UI || {};
    window.UI.endRoster = function() { return window.endRoster(); };
    window.UI.moveToField = function(fieldName) { return window.moveToField(fieldName); };
    window.UI.advance = function() { return window.advance(); };
    
    console.log('[CAPI Shim] Navigation functions registered (endRoster, moveToField, advance)');
    
    // ============================================================
    // MESSAGE HANDLER - Listen for responses from parent
    // ============================================================
    window.addEventListener('message', function(event) {
        if (event.data && event.data.type === 'cspro-action-response') {
            console.log('[CAPI Shim] Got response:', event.data.requestId, event.data.result);
            CSProActionInvoker.${"$"}Impl.handleResponse(event.data.requestId, event.data.result);
        }
    });
})();
</script>
        """

        // Resize observer script - throttled to avoid loops
        val resizeScript = """
            <script>
            (function(){
                var lastH = -1;
                var pending = false;
                function post(){
                    if (pending) return;
                    pending = true;
                    requestAnimationFrame(function(){
                        pending = false;
                        var h = Math.max(
                            document.documentElement ? document.documentElement.scrollHeight : 0,
                            document.body ? document.body.scrollHeight : 0
                        );
                        if (h > 0 && h !== lastH) {
                            lastH = h;
                            parent.postMessage({type:'cspro-iframe-resize', frameId:${jsString(frameId)}, height:h}, '*');
                        }
                    });
                }
                window.addEventListener('load', post);
                window.addEventListener('resize', post);
                try {
                    new MutationObserver(post).observe(document.documentElement || document.body, {subtree:true, childList:true, attributes:true});
                } catch(e) {}
                post();
            })();
            </script>
        """.trimIndent()

        // Combine scripts - action invoker shim first so CSProActionInvoker is defined
        val combinedScripts = actionInvokerShim + "\n" + resizeScript

        // Escape $ in replacement strings to avoid regex group reference errors
        val safeScripts = Regex.escapeReplacement(combinedScripts)
        
        // CRITICAL: Remove external action-invoker.js script tags to prevent conflicts.
        // The QSF HTML may include <script src="/action-invoker.js"> or similar references.
        // We inject our own inline shim, so loading the external one causes duplicate
        // CSProActionInvoker definitions and message handler conflicts.
        var result = html
            .replace(Regex("""<script[^>]*src\s*=\s*["'][^"']*action-invoker\.js["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE), "<!-- action-invoker.js removed, using inline shim -->")
            .replace(Regex("""<script[^>]*src\s*=\s*["']/action-invoker\.js["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE), "<!-- action-invoker.js removed, using inline shim -->")
        
        // Insert base tag in <head> (or at start if no <head>)
        if (result.contains("<head>", ignoreCase = true)) {
            result = result.replaceFirst(
                Regex("<head>", RegexOption.IGNORE_CASE),
                "<head>$baseTag"
            )
        } else if (result.contains("<html>", ignoreCase = true)) {
            result = result.replaceFirst(
                Regex("<html>", RegexOption.IGNORE_CASE),
                "<html><head>$baseTag</head>"
            )
        } else {
            // No HTML structure - wrap with minimal structure
            result = "<!DOCTYPE html><html><head>$baseTag</head><body>$result</body></html>"
        }

        // Insert scripts before </body> or </html>, or append at end
        return when {
            result.contains("</body>", ignoreCase = true) -> result.replaceFirst(
                Regex("</body>", RegexOption.IGNORE_CASE),
                safeScripts + "</body>"
            )
            result.contains("</html>", ignoreCase = true) -> result.replaceFirst(
                Regex("</html>", RegexOption.IGNORE_CASE),
                safeScripts + "</html>"
            )
            else -> result + combinedScripts
        }
    }

    private fun jsString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "\"$escaped\""
    }

    private fun attachQuestionTextFrames() {
        if (questionTextFrames.isEmpty()) {
            Logger.qsf("attachQuestionTextFrames", mapOf("status" to "no frames to attach"))
            return
        }
        val root = container ?: return

        Logger.qsf("attachQuestionTextFrames", mapOf(
            "frameCount" to questionTextFrames.size,
            "frameIds" to questionTextFrames.keys.toList().toString()
        ))

        for ((frameId, srcdoc) in questionTextFrames) {
            val iframe = root.querySelector("#${frameId}") as? HTMLIFrameElement
            if (iframe != null) {
                Logger.qsf("setIframeSrcdoc", mapOf(
                    "frameId" to frameId,
                    "srcdocLength" to srcdoc.length,
                    "srcdocPreview" to srcdoc.take(200)
                ))
                iframe.srcdoc = srcdoc
                // Set reasonable initial height - will be adjusted by resize message
                // Block QSF tends to be taller, field QSF shorter
                val isBlockQsf = frameId == "qt-frame-1" && questionTextFrames.size == 1
                iframe.style.height = if (isBlockQsf) "auto" else "auto"
                iframe.style.minHeight = if (isBlockQsf) "80px" else "50px"
            } else {
                Logger.warn("iframe not found for QSF", mapOf("frameId" to frameId))
            }
        }

        ensureIframeResizeListenerInstalled()
    }

    private fun ensureIframeResizeListenerInstalled() {
        if (iframeResizeListener != null) return

        val listener: (Event) -> Unit = resize@{ ev: Event ->
            val jsEv = ev.toJsReference()
            if (jsGetQuestionTextMessageType(jsEv) != "cspro-iframe-resize") return@resize

            val frameId = jsGetQuestionTextFrameId(jsEv)
            val height = jsGetQuestionTextFrameHeight(jsEv)
            if (frameId.isBlank() || height <= 0) return@resize

            val iframe = document.getElementById(frameId) as? HTMLIFrameElement
            if (iframe != null) {
                val newHeight = "${height}px"
                if (iframe.style.height != newHeight) {
                    iframe.style.height = newHeight
                }
            }
        }

        iframeResizeListener = listener
        window.addEventListener("message", listener)
    }

    override fun onDestroyView() {
        iframeResizeListener?.let { window.removeEventListener("message", it) }
        iframeResizeListener = null
        questionTextFrames.clear()
        super.onDestroyView()
    }

    private fun findFieldByName(name: String): CDEField? {
        val page = currentPage ?: return null
        return page.fields.firstOrNull { it.name == name }
    }
    
    /**
     * Get question text HTML from virtual file URL
     * The C++ engine stores question text in virtual files like "cspro-virtual://html/1.html"
     */
    private fun getQuestionTextHtml(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        
        return try {
            val content = CSProEngineService.getInstance().getVirtualFileContent(url)
            Logger.qsf("getQuestionTextHtml", mapOf(
                "url" to url,
                "contentLength" to (content?.length ?: 0),
                "preview" to (content?.take(100) ?: "null")
            ))
            content ?: ""
        } catch (e: Exception) {
            Logger.error("Error getting question text", mapOf("url" to url, "error" to e.message))
            ""
        }
    }
    
    private fun renderTextInput(field: CDEField, protectedAttr: String): String {
        // Match Android question_widget_text_box_numeric.xml:
        // - Centered text input with border
        // - 50dp height, centered gravity
        val inputType = if (field.isNumeric) "number" else "text"
        val fieldNameSafe = field.name.replace("\"", "&quot;")
        return """
            <input type="$inputType" 
                   class="field-input-text android-style" 
                   value="${field.value}" 
                   $protectedAttr 
                   placeholder="${if (field.isNumeric) "" else ""}"
                   data-field-name="$fieldNameSafe" />
        """.trimIndent()
    }
    
    private fun renderRadioButtons(field: CDEField): String {
        // Render using CSPro native HTML dialog pattern (from Templates.html)
        // Uses CSS classes: value-set, value-row, ct-radio-button (capture type radio button)
        val fieldNameSafe = field.name.replace("\"", "&quot;")
        val options = field.valueSet.mapIndexed { index, entry ->
            val checkedClass = if (entry.code == field.value) "checked" else ""
            val codeSafe = entry.code.replace("\"", "&quot;")
            val labelSafe = entry.label.replace("<", "&lt;").replace(">", "&gt;")
            """
            <div class="value-row highlighted" data-field-name="$fieldNameSafe" data-value="$codeSafe" tabindex="0">
                <div class="value-indicator">
                    <span class="ct-radio-button $checkedClass"></span>
                </div>
                <div class="value-content">
                    <span class="value-code">$codeSafe</span>
                    <span class="value-label">$labelSafe</span>
                </div>
            </div>
            """.trimIndent()
        }.joinToString("\n")
        return """<div class="value-set" data-field-name="$fieldNameSafe" data-capture-type="radiobutton">$options</div>"""
    }
    
    private fun renderDropdown(field: CDEField): String {
        // Render as a combo box (editable text + dropdown) matching Android MultiBoxNumeric
        // Android ComboBox = editable text field + dropdown list for predefined values
        val fieldNameSafe = field.name.replace("\"", "&quot;")
        val readonlyAttr = if (field.isProtected || field.isReadOnly) "readonly" else ""
        val inputType = if (field.isNumeric) "number" else "text"
        
        // Build the dropdown options
        val options = field.valueSet.mapIndexed { index, entry ->
            val selectedClass = if (entry.code == field.value) "active" else ""
            val codeSafe = entry.code.replace("\"", "&quot;")
            val labelSafe = entry.label.replace("<", "&lt;").replace(">", "&gt;")
            """<tr class="tab-row $selectedClass" data-field-name="$fieldNameSafe" data-value="$codeSafe" data-index="$index" tabindex="0">
                <td class="value-code">$codeSafe</td>
                <td class="value-label">$labelSafe</td>
            </tr>""".trimIndent()
        }.joinToString("\n")
        
        // Combo box: text input + dropdown toggle button + dropdown panel
        return """
            <div class="cspro-combobox-widget" data-field-name="$fieldNameSafe" data-capture-type="combobox">
                <div class="combobox-input-row">
                    <input type="$inputType" 
                           class="combobox-text-input field-input-text" 
                           value="${field.value}" 
                           $readonlyAttr 
                           placeholder="${if (field.isNumeric) "Enter value or select" else "Enter or select"}"
                           data-field-name="$fieldNameSafe" />
                    <button type="button" class="combobox-dropdown-toggle" data-field-name="$fieldNameSafe">▼</button>
                </div>
                <div class="combobox-dropdown-panel" style="display:none;">
                    <table class="select-table">
                        <tbody>$options</tbody>
                    </table>
                </div>
            </div>
        """.trimIndent()
    }

    /**
     * Returns values for ALL fields on the current page.
     * This is the web equivalent of Android's QuestionnaireFragment.applyCurrentFieldValues()
     * which iterates all QuestionWidgets on the page.
     */
    fun getPageFieldValues(): List<Pair<CDEField, String>> {
        val page = currentPage ?: return emptyList()
        val root = container ?: return emptyList()

        fun readValueFor(field: CDEField): String {
            val name = field.name

            val textInput = root.querySelector(
                "input[type='text'][data-field-name='$name'], input[type='number'][data-field-name='$name'], input[type='date'][data-field-name='$name']"
            ) as? HTMLInputElement
            if (textInput != null) return textInput.value

            val slider = root.querySelector("input[type='range'][data-field-name='$name']") as? HTMLInputElement
            if (slider != null) return slider.value

            val dropdownWidget = root.querySelector(".cspro-select-widget[data-field-name='$name']") as? HTMLElement
            if (dropdownWidget != null) {
                val activeRow = dropdownWidget.querySelector(".tab-row.active") as? HTMLElement
                if (activeRow != null) return activeRow.getAttribute("data-value") ?: ""
            }

            val valueSet = root.querySelector(".value-set[data-field-name='$name']") as? HTMLElement
            if (valueSet != null) {
                val captureType = valueSet.getAttribute("data-capture-type") ?: "radiobutton"
                if (captureType == "checkbox") {
                    val allRows = valueSet.querySelectorAll(".value-row")
                    val checkedValues = mutableListOf<String>()
                    for (j in 0 until allRows.length) {
                        val r = allRows.item(j) as? HTMLElement ?: continue
                        val ind = r.querySelector(".ct-check-box") as? HTMLElement
                        if (ind?.classList?.contains("checked") == true) {
                            checkedValues.add(r.getAttribute("data-value") ?: "")
                        }
                    }
                    return checkedValues.joinToString("")
                } else {
                    val checkedRow = valueSet.querySelector(".value-row .ct-radio-button.checked")
                        ?.parentElement?.parentElement as? HTMLElement
                    if (checkedRow != null) return checkedRow.getAttribute("data-value") ?: ""
                }
            }

            return field.value
        }

        return page.fields.map { it to readValueFor(it) }
    }
    
    private fun getDisplayText(field: CDEField): String {
        if (field.value.isEmpty()) return "-- Select --"
        val entry = field.valueSet.find { it.code == field.value }
        return if (entry != null) "${entry.code} - ${entry.label}" else field.value
    }
    
    private fun renderCheckbox(field: CDEField): String {
        // Render using CSPro native HTML pattern (ct-check-box class from Templates.html)
        val fieldNameSafe = field.name.replace("\"", "&quot;")
        
        // For checkbox capture type with value sets, render each value as a checkbox option
        if (field.valueSet.isNotEmpty()) {
            val options = field.valueSet.mapIndexed { index, entry ->
                // For checkbox, check if the value is in the field's selected values
                val isChecked = field.value.contains(entry.code)
                val checkedClass = if (isChecked) "checked" else ""
                val codeSafe = entry.code.replace("\"", "&quot;")
                val labelSafe = entry.label.replace("<", "&lt;").replace(">", "&gt;")
                """
                <div class="value-row highlighted" data-field-name="$fieldNameSafe" data-value="$codeSafe" tabindex="0">
                    <div class="value-indicator">
                        <span class="ct-check-box $checkedClass"></span>
                    </div>
                    <div class="value-content">
                        <span class="value-code">$codeSafe</span>
                        <span class="value-label">$labelSafe</span>
                    </div>
                </div>
                """.trimIndent()
            }.joinToString("\n")
            return """<div class="value-set" data-field-name="$fieldNameSafe" data-capture-type="checkbox">$options</div>"""
        }
        
        // Single checkbox (boolean field)
        val checked = if (field.value == "1") "checked" else ""
        return """
            <div class="value-row highlighted" data-field-name="$fieldNameSafe" data-value="1" tabindex="0">
                <div class="value-indicator">
                    <span class="ct-check-box $checked"></span>
                </div>
                <div class="value-content">
                    <span class="value-label">${field.label}</span>
                </div>
            </div>
        """.trimIndent()
    }
    
    private fun renderDateInput(field: CDEField, protectedAttr: String): String {
        return """
            <input type="date" 
                   class="field-input-text field-input-date" 
                   value="${field.value}" 
                   $protectedAttr 
                   data-field-name="${field.name}" />
        """.trimIndent()
    }
    
    private fun renderSlider(field: CDEField): String {
        val min = field.sliderMin.toInt()
        val max = field.sliderMax.toInt()
        val step = if (field.sliderStep > 0) field.sliderStep.toInt() else 1
        val value = field.value.toIntOrNull() ?: min
        return """
            <div class="slider-container">
                <input type="range" 
                       class="field-input-slider" 
                       min="$min" max="$max" step="$step" 
                       value="$value" 
                       data-field-name="${field.name}" />
                <span class="slider-value">$value</span>
            </div>
        """.trimIndent()
    }
    
    private fun attachFieldListeners() {
        val root = container ?: return
        
        // Text inputs (including date)
        val inputs = root.querySelectorAll("input[type='text'], input[type='number'], input[type='date']")
        for (i in 0 until inputs.length) {
            val input = inputs.item(i) as? HTMLInputElement ?: continue
            val fieldName = input.getAttribute("data-field-name") ?: continue
            val field = currentPage?.fields?.find { it.name == fieldName } ?: continue

            input.addEventListener("focus", {
                onFieldFocusListener?.invoke(field)
            })
            
            input.addEventListener("change", {
                onFieldChangeListener?.invoke(field, input.value)
            })
            
            // Handle Enter key (like Next in Android)
            input.addEventListener("keydown", { e ->
                val keyboardEvent = e as? org.w3c.dom.events.KeyboardEvent
                if (keyboardEvent?.key == "Enter") {
                    onFieldChangeListener?.invoke(field, input.value)
                }
            })
        }
        
        // CSPro native value-row elements (radio buttons and checkboxes)
        val valueRows = root.querySelectorAll(".value-row")
        for (i in 0 until valueRows.length) {
            val row = valueRows.item(i) as? HTMLElement ?: continue
            val fieldName = row.getAttribute("data-field-name") ?: continue
            val value = row.getAttribute("data-value") ?: continue
            val field = currentPage?.fields?.find { it.name == fieldName } ?: continue

            row.addEventListener("focus", {
                onFieldFocusListener?.invoke(field)
            })
            
            // Find the parent value-set to determine capture type
            val valueSet = row.parentElement as? HTMLElement
            val captureType = valueSet?.getAttribute("data-capture-type") ?: "radiobutton"
            
            row.addEventListener("click", { e ->
                if (captureType == "checkbox") {
                    // Toggle checkbox state
                    val indicator = row.querySelector(".ct-check-box") as? HTMLElement
                    val isChecked = indicator?.classList?.contains("checked") == true
                    if (isChecked) {
                        indicator?.classList?.remove("checked")
                    } else {
                        indicator?.classList?.add("checked")
                    }
                    // For checkbox, collect all checked values
                    val allRows = valueSet?.querySelectorAll(".value-row")
                    val checkedValues = mutableListOf<String>()
                    if (allRows != null) {
                        for (j in 0 until allRows.length) {
                            val r = allRows.item(j) as? HTMLElement ?: continue
                            val ind = r.querySelector(".ct-check-box") as? HTMLElement
                            if (ind?.classList?.contains("checked") == true) {
                                checkedValues.add(r.getAttribute("data-value") ?: "")
                            }
                        }
                    }
                    onFieldChangeListener?.invoke(field, checkedValues.joinToString(""))
                } else {
                    // Radio button behavior - only one can be selected
                    val allRows = valueSet?.querySelectorAll(".value-row")
                    if (allRows != null) {
                        for (j in 0 until allRows.length) {
                            val r = allRows.item(j) as? HTMLElement ?: continue
                            val ind = r.querySelector(".ct-radio-button") as? HTMLElement
                            ind?.classList?.remove("checked")
                        }
                    }
                    val indicator = row.querySelector(".ct-radio-button") as? HTMLElement
                    indicator?.classList?.add("checked")
                    onFieldChangeListener?.invoke(field, value)
                }
            })
            
            // Handle keyboard Enter/Space for accessibility
            row.addEventListener("keydown", { e ->
                val keyboardEvent = e as? org.w3c.dom.events.KeyboardEvent
                if (keyboardEvent?.key == "Enter" || keyboardEvent?.key == " ") {
                    e.preventDefault()
                    row.click()
                }
            })
        }
        
        // CSPro native dropdown widgets (old style - for backwards compatibility)
        val dropdownWidgets = root.querySelectorAll(".cspro-select-widget")
        for (i in 0 until dropdownWidgets.length) {
            val widget = dropdownWidgets.item(i) as? HTMLElement ?: continue
            val fieldName = widget.getAttribute("data-field-name") ?: continue
            val field = currentPage?.fields?.find { it.name == fieldName } ?: continue
            
            val currentValue = widget.querySelector(".select-current-value") as? HTMLElement
            val panel = widget.querySelector(".select-dropdown-panel") as? HTMLElement

            currentValue?.addEventListener("focus", {
                onFieldFocusListener?.invoke(field)
            })
            
            // Toggle dropdown panel on click
            currentValue?.addEventListener("click", { e ->
                val isVisible = panel?.style?.display != "none"
                if (isVisible) {
                    panel?.style?.display = "none"
                } else {
                    // Close other open dropdowns first
                    val allPanels = root.querySelectorAll(".select-dropdown-panel, .combobox-dropdown-panel")
                    for (j in 0 until allPanels.length) {
                        (allPanels.item(j) as? HTMLElement)?.style?.display = "none"
                    }
                    panel?.style?.display = "block"
                }
            })
            
            // Handle row selection in dropdown
            val tableRows = widget.querySelectorAll(".tab-row")
            for (j in 0 until tableRows.length) {
                val tableRow = tableRows.item(j) as? HTMLElement ?: continue
                val rowValue = tableRow.getAttribute("data-value") ?: continue
                
                tableRow.addEventListener("click", { e ->
                    // Update display text
                    val displayText = widget.querySelector(".select-display-text") as? HTMLElement
                    val label = tableRow.querySelector(".value-label")?.textContent ?: ""
                    displayText?.textContent = "$rowValue - $label"
                    
                    // Update active state
                    val allTableRows = widget.querySelectorAll(".tab-row")
                    for (k in 0 until allTableRows.length) {
                        (allTableRows.item(k) as? HTMLElement)?.classList?.remove("active")
                    }
                    tableRow.classList.add("active")
                    
                    // Close panel
                    panel?.style?.display = "none"
                    
                    // Notify listener
                    onFieldChangeListener?.invoke(field, rowValue)
                })
            }
        }
        
        // ComboBox widgets (editable text + dropdown - matching Android MultiBoxNumeric)
        val comboboxWidgets = root.querySelectorAll(".cspro-combobox-widget")
        for (i in 0 until comboboxWidgets.length) {
            val widget = comboboxWidgets.item(i) as? HTMLElement ?: continue
            val fieldName = widget.getAttribute("data-field-name") ?: continue
            val field = currentPage?.fields?.find { it.name == fieldName } ?: continue
            
            val textInput = widget.querySelector(".combobox-text-input") as? HTMLInputElement
            val toggleBtn = widget.querySelector(".combobox-dropdown-toggle") as? HTMLElement
            val panel = widget.querySelector(".combobox-dropdown-panel") as? HTMLElement

            textInput?.addEventListener("focus", {
                onFieldFocusListener?.invoke(field)
            })
            
            // Text input change - user can type directly
            textInput?.addEventListener("change", {
                onFieldChangeListener?.invoke(field, textInput.value)
            })
            
            // Enter key on text input
            textInput?.addEventListener("keydown", { e ->
                val keyboardEvent = e as? org.w3c.dom.events.KeyboardEvent
                if (keyboardEvent?.key == "Enter") {
                    onFieldChangeListener?.invoke(field, textInput.value)
                }
            })
            
            // Toggle dropdown panel on button click
            toggleBtn?.addEventListener("click", { e ->
                e.stopPropagation()
                val isVisible = panel?.style?.display != "none"
                if (isVisible) {
                    panel?.style?.display = "none"
                } else {
                    // Close other open dropdowns first
                    val allPanels = root.querySelectorAll(".select-dropdown-panel, .combobox-dropdown-panel")
                    for (j in 0 until allPanels.length) {
                        (allPanels.item(j) as? HTMLElement)?.style?.display = "none"
                    }
                    panel?.style?.display = "block"
                }
            })
            
            // Handle row selection in combobox dropdown
            val tableRows = widget.querySelectorAll(".tab-row")
            for (j in 0 until tableRows.length) {
                val tableRow = tableRows.item(j) as? HTMLElement ?: continue
                val rowValue = tableRow.getAttribute("data-value") ?: continue
                
                tableRow.addEventListener("click", { e ->
                    // Update text input value
                    textInput?.value = rowValue
                    
                    // Update active state
                    val allTableRows = widget.querySelectorAll(".tab-row")
                    for (k in 0 until allTableRows.length) {
                        (allTableRows.item(k) as? HTMLElement)?.classList?.remove("active")
                    }
                    tableRow.classList.add("active")
                    
                    // Close panel
                    panel?.style?.display = "none"
                    
                    // Notify listener
                    onFieldChangeListener?.invoke(field, rowValue)
                })
            }
        }
        
        // Slider inputs
        val sliders = root.querySelectorAll("input[type='range']")
        for (i in 0 until sliders.length) {
            val slider = sliders.item(i) as? HTMLInputElement ?: continue
            val fieldName = slider.getAttribute("data-field-name") ?: continue
            val field = currentPage?.fields?.find { it.name == fieldName } ?: continue

            slider.addEventListener("focus", {
                onFieldFocusListener?.invoke(field)
            })
            
            slider.addEventListener("input", { e ->
                // Update the displayed value
                val valueSpan = slider.parentElement?.querySelector(".slider-value") as? HTMLElement
                valueSpan?.textContent = slider.value
            })
            
            slider.addEventListener("change", {
                onFieldChangeListener?.invoke(field, slider.value)
            })
        }
    }
    
    fun focusNextField() {
        // Find current active element and focus next focusable element
        val current = document.activeElement as? HTMLElement
        val focusables = container?.querySelectorAll("input, .value-row, .select-current-value")
        if (focusables != null && current != null) {
            for (i in 0 until focusables.length) {
                if (focusables.item(i) == current) {
                    val next = focusables.item(i + 1) as? HTMLElement
                    next?.focus()
                    break
                }
            }
        }
    }
    
    fun focusPreviousField() {
        val current = document.activeElement as? HTMLElement
        val focusables = container?.querySelectorAll("input, .value-row, .select-current-value")
        if (focusables != null && current != null) {
            for (i in 0 until focusables.length) {
                if (focusables.item(i) == current && i > 0) {
                    val prev = focusables.item(i - 1) as? HTMLElement
                    prev?.focus()
                    break
                }
            }
        }
    }
    
    /**
     * Get the current value for the current (first) field in the page.
     * Used when the "Next" button is clicked to submit the current value.
     */
    fun getCurrentFieldValue(): Pair<CDEField, String>? {
        val page = currentPage ?: return null
        val idx = page.currentFieldIndex.coerceIn(0, (page.fields.size - 1).coerceAtLeast(0))
        val currentField = page.fields.getOrNull(idx) ?: return null
        
        val root = container ?: return null
        
        // Try to find the text/number/date input element for the current field
        val textInput = root.querySelector("input[type='text'][data-field-name='${currentField.name}'], input[type='number'][data-field-name='${currentField.name}'], input[type='date'][data-field-name='${currentField.name}']") as? HTMLInputElement
        if (textInput != null) {
            return Pair(currentField, textInput.value)
        }
        
        // Try slider
        val slider = root.querySelector("input[type='range'][data-field-name='${currentField.name}']") as? HTMLInputElement
        if (slider != null) {
            return Pair(currentField, slider.value)
        }
        
        // Try CSPro native dropdown widget
        val dropdownWidget = root.querySelector(".cspro-select-widget[data-field-name='${currentField.name}']") as? HTMLElement
        if (dropdownWidget != null) {
            val activeRow = dropdownWidget.querySelector(".tab-row.active") as? HTMLElement
            if (activeRow != null) {
                return Pair(currentField, activeRow.getAttribute("data-value") ?: "")
            }
        }
        
        // Try CSPro native value-set (radio buttons)
        val valueSet = root.querySelector(".value-set[data-field-name='${currentField.name}']") as? HTMLElement
        if (valueSet != null) {
            val captureType = valueSet.getAttribute("data-capture-type") ?: "radiobutton"
            if (captureType == "checkbox") {
                // Collect all checked values
                val allRows = valueSet.querySelectorAll(".value-row")
                val checkedValues = mutableListOf<String>()
                for (j in 0 until allRows.length) {
                    val r = allRows.item(j) as? HTMLElement ?: continue
                    val ind = r.querySelector(".ct-check-box") as? HTMLElement
                    if (ind?.classList?.contains("checked") == true) {
                        checkedValues.add(r.getAttribute("data-value") ?: "")
                    }
                }
                return Pair(currentField, checkedValues.joinToString(""))
            } else {
                // Radio button - find the checked one
                val checkedRow = valueSet.querySelector(".value-row .ct-radio-button.checked")?.parentElement?.parentElement as? HTMLElement
                if (checkedRow != null) {
                    return Pair(currentField, checkedRow.getAttribute("data-value") ?: "")
                }
            }
        }
        
        // Fallback: return the field with its original value
        return Pair(currentField, currentField.value)
    }
}
