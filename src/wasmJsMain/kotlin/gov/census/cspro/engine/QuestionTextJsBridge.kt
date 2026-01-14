package gov.census.cspro.engine

import kotlinx.browser.window

/**
 * Question Text JavaScript Bridge
 * 
 * Exposes global JavaScript functions that CSPro question text HTML can call.
 * These functions are called directly from HTML links in question text (QSF),
 * such as "end the roster (with a household size of 0)" which calls endRoster().
 * 
 * Mirrors Android's CSProJavaScriptInterface behavior:
 * - All CSPro logic functions are executed via ActionInvoker (Logic.eval / Logic.invoke)
 * - The engine handles navigation internally after logic execution
 * - No event dispatching - direct ActionInvoker calls
 * 
 * Functions exposed:
 * - endRoster() / endGroup() - Executes endgroup CSPro logic
 * - moveToField(fieldName) - Executes move(to) CSPro logic  
 * - advance() - Executes advance CSPro logic
 * - CSPro.runLogic(code) - Runs CSPro logic code via Logic.eval
 * - CSPro.invoke(functionName, args) - Invokes user-defined function via Logic.invoke
 */

/**
 * Install global QSF functions that call ActionInvoker directly
 * Mirrors Android CSProJavaScriptInterface behavior
 */
@JsFun("""() => {
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
}""")
private external fun installQsfFunctions()

/**
 * Question Text JavaScript Bridge object
 * Registers handlers for question text function calls
 */
object QuestionTextJsBridge {
    private var isRegistered = false
    
    /**
     * Register the QSF JavaScript bridge
     * Call this during application initialization
     */
    fun register() {
        if (isRegistered) {
            println("[QuestionTextBridge] Already registered")
            return
        }
        
        println("[QuestionTextBridge] Registering QSF functions via ActionInvoker...")
        
        try {
            // Install all QSF global functions
            installQsfFunctions()
            
            isRegistered = true
            println("[QuestionTextBridge] QSF functions registered successfully")
            
        } catch (e: Exception) {
            println("[QuestionTextBridge] Error registering: ${e.message}")
        }
    }
}
