package gov.census.cspro.engine.functions

import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.js.JsAny

/**
 * Top-level JS interop functions
 */
@JsFun("(event) => { return event.key; }")
private external fun getEventKey(event: JsAny): String

// LoginResult is defined in commonMain

/**
 * Login Dialog Function - displays a login form
 * Ported from Android LoginDialogFunction.java
 */
class LoginDialogFunction(
    private val title: String = "Login",
    private val message: String = "",
    private val initialUsername: String = "",
    private val showRememberMe: Boolean = false
) : EngineFunction {
    
    private var result: LoginResult = LoginResult(null, null, true)
    
    fun getResult(): LoginResult = result
    
    override suspend fun run() {
        val deferred = CompletableDeferred<LoginResult>()
        
        // Create overlay
        val overlay = document.createElement("div") as HTMLDivElement
        overlay.id = "cspro-login-overlay"
        overlay.style.position = "fixed"
        overlay.style.top = "0"
        overlay.style.left = "0"
        overlay.style.width = "100%"
        overlay.style.height = "100%"
        overlay.style.backgroundColor = "rgba(0, 0, 0, 0.5)"
        overlay.style.display = "flex"
        overlay.style.justifyContent = "center"
        overlay.style.alignItems = "center"
        overlay.style.zIndex = "10000"
        
        // Create dialog
        val dialog = document.createElement("div") as HTMLDivElement
        dialog.style.backgroundColor = "white"
        dialog.style.borderRadius = "8px"
        dialog.style.padding = "25px"
        dialog.style.minWidth = "350px"
        dialog.style.maxWidth = "400px"
        dialog.style.boxShadow = "0 4px 20px rgba(0,0,0,0.3)"
        
        // Title
        val titleEl = document.createElement("h2") as HTMLElement
        titleEl.textContent = title
        titleEl.style.margin = "0 0 20px 0"
        titleEl.style.textAlign = "center"
        titleEl.style.color = "#333"
        dialog.appendChild(titleEl)
        
        // Message
        if (message.isNotEmpty()) {
            val messageEl = document.createElement("p") as HTMLElement
            messageEl.textContent = message
            messageEl.style.margin = "0 0 20px 0"
            messageEl.style.textAlign = "center"
            messageEl.style.color = "#666"
            dialog.appendChild(messageEl)
        }
        
        // Username field
        val usernameLabel = document.createElement("label") as HTMLLabelElement
        usernameLabel.textContent = "Username"
        usernameLabel.style.display = "block"
        usernameLabel.style.marginBottom = "5px"
        usernameLabel.style.fontWeight = "bold"
        usernameLabel.style.color = "#333"
        dialog.appendChild(usernameLabel)
        
        val usernameInput = document.createElement("input") as HTMLInputElement
        usernameInput.type = "text"
        usernameInput.value = initialUsername
        usernameInput.placeholder = "Enter username"
        usernameInput.style.width = "100%"
        usernameInput.style.padding = "10px"
        usernameInput.style.fontSize = "14px"
        usernameInput.style.border = "1px solid #ccc"
        usernameInput.style.borderRadius = "4px"
        usernameInput.style.boxSizing = "border-box"
        usernameInput.style.marginBottom = "15px"
        dialog.appendChild(usernameInput)
        
        // Password field
        val passwordLabel = document.createElement("label") as HTMLLabelElement
        passwordLabel.textContent = "Password"
        passwordLabel.style.display = "block"
        passwordLabel.style.marginBottom = "5px"
        passwordLabel.style.fontWeight = "bold"
        passwordLabel.style.color = "#333"
        dialog.appendChild(passwordLabel)
        
        val passwordInput = document.createElement("input") as HTMLInputElement
        passwordInput.type = "password"
        passwordInput.placeholder = "Enter password"
        passwordInput.style.width = "100%"
        passwordInput.style.padding = "10px"
        passwordInput.style.fontSize = "14px"
        passwordInput.style.border = "1px solid #ccc"
        passwordInput.style.borderRadius = "4px"
        passwordInput.style.boxSizing = "border-box"
        passwordInput.style.marginBottom = "20px"
        dialog.appendChild(passwordInput)
        
        // Remember me checkbox (optional)
        if (showRememberMe) {
            val rememberContainer = document.createElement("div") as HTMLDivElement
            rememberContainer.style.display = "flex"
            rememberContainer.style.alignItems = "center"
            rememberContainer.style.marginBottom = "20px"
            
            val rememberCheckbox = document.createElement("input") as HTMLInputElement
            rememberCheckbox.type = "checkbox"
            rememberCheckbox.id = "remember-me"
            rememberCheckbox.style.marginRight = "8px"
            
            val rememberLabel = document.createElement("label") as HTMLLabelElement
            rememberLabel.htmlFor = "remember-me"
            rememberLabel.textContent = "Remember me"
            rememberLabel.style.color = "#666"
            
            rememberContainer.appendChild(rememberCheckbox)
            rememberContainer.appendChild(rememberLabel)
            dialog.appendChild(rememberContainer)
        }
        
        // Buttons
        val buttonContainer = document.createElement("div") as HTMLDivElement
        buttonContainer.style.display = "flex"
        buttonContainer.style.justifyContent = "flex-end"
        buttonContainer.style.setProperty("gap", "10px")
        
        fun closeDialog(username: String?, password: String?, cancelled: Boolean) {
            document.body?.removeChild(overlay)
            deferred.complete(LoginResult(username, password, cancelled))
        }
        
        val cancelBtn = document.createElement("button") as HTMLButtonElement
        cancelBtn.textContent = "Cancel"
        cancelBtn.style.padding = "10px 25px"
        cancelBtn.style.border = "1px solid #ccc"
        cancelBtn.style.borderRadius = "4px"
        cancelBtn.style.cursor = "pointer"
        cancelBtn.style.backgroundColor = "#fff"
        cancelBtn.style.fontSize = "14px"
        cancelBtn.onclick = { closeDialog(null, null, true) }
        buttonContainer.appendChild(cancelBtn)
        
        val loginBtn = document.createElement("button") as HTMLButtonElement
        loginBtn.textContent = "Login"
        loginBtn.style.padding = "10px 25px"
        loginBtn.style.border = "none"
        loginBtn.style.borderRadius = "4px"
        loginBtn.style.cursor = "pointer"
        loginBtn.style.backgroundColor = "#007bff"
        loginBtn.style.color = "#fff"
        loginBtn.style.fontSize = "14px"
        loginBtn.onclick = { closeDialog(usernameInput.value, passwordInput.value, false) }
        buttonContainer.appendChild(loginBtn)
        
        // Handle Enter key
        val handleEnter: (KeyboardEvent) -> Unit = { event ->
            val eventJs = event.unsafeCast<JsAny>()
            val key = getEventKey(eventJs)
            if (key == "Enter") {
                closeDialog(usernameInput.value, passwordInput.value, false)
            }
        }
        usernameInput.onkeydown = handleEnter
        passwordInput.onkeydown = handleEnter
        
        dialog.appendChild(buttonContainer)
        overlay.appendChild(dialog)
        document.body?.appendChild(overlay)
        
        // Focus appropriate field
        if (initialUsername.isEmpty()) {
            usernameInput.focus()
        } else {
            passwordInput.focus()
        }
        
        result = deferred.await()
    }
}

/**
 * Password Query Function - displays a password-only input
 * Ported from Android PasswordQueryFunction.java
 */
class PasswordQueryFunction(
    private val title: String = "Enter Password",
    private val message: String = ""
) : EngineFunction {
    
    private var password: String? = null
    private var cancelled = true
    
    fun getPassword(): String? = password
    fun isCancelled(): Boolean = cancelled
    
    override suspend fun run() {
        val promptFunction = PromptFunction(
            title = title,
            message = message,
            isPassword = true
        )
        promptFunction.run()
        
        val result = promptFunction.getResult()
        password = result.text
        cancelled = result.cancelled
    }
}
