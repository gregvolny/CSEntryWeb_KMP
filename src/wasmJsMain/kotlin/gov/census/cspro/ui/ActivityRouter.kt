package gov.census.cspro.ui

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HashChangeEvent

/**
 * Activity Router for navigation between web activities
 * Uses URL hash for SPA-style navigation
 */
object ActivityRouter {
    private var currentActivity: BaseActivity? = null
    private val activityStack = mutableListOf<Pair<String, Map<String, Any>?>>()
    private val extras = mutableMapOf<String, Map<String, Any>?>()
    
    // Flag to prevent handling hash change when we're programmatically navigating
    private var navigatingProgrammatically = false
    
    /**
     * Initialize the router and set up hash change listener
     */
    fun initialize() {
        // Listen for hash changes
        window.addEventListener("hashchange", { event ->
            // Skip if this hash change was caused by our own navigation
            if (navigatingProgrammatically) {
                return@addEventListener
            }
            
            val hash = window.location.hash.removePrefix("#")
            if (hash.isNotEmpty()) {
                handleRoute(hash)
            }
        })
        
        // Handle initial route
        val initialHash = window.location.hash.removePrefix("#")
        if (initialHash.isEmpty()) {
            // Default to ApplicationsListActivity
            navigateTo("ApplicationsListActivity")
        } else {
            handleRoute(initialHash)
        }
    }
    
    /**
     * Navigate to an activity
     */
    fun navigateTo(activityClass: String, extras: Map<String, Any>? = null) {
        println("[ActivityRouter] Navigating to: $activityClass")
        
        // Store extras for the activity
        this.extras[activityClass] = extras
        
        // Set flag to prevent hash change handler from also handling this navigation
        navigatingProgrammatically = true
        
        // Update URL hash
        window.location.hash = activityClass
        
        // Reset flag after hash change event would have fired (use 0 delay)
        window.setTimeout({
            navigatingProgrammatically = false
            null  // Return null for JsAny?
        }, 0)
        
        // Create and show the activity
        showActivity(activityClass, extras)
    }
    
    /**
     * Go back to the previous activity
     */
    fun goBack() {
        if (activityStack.size > 1) {
            activityStack.removeLast()
            val (prevActivity, prevExtras) = activityStack.last()
            window.location.hash = prevActivity
            showActivity(prevActivity, prevExtras, addToStack = false)
        }
    }
    
    /**
     * Get extras for the current activity
     */
    fun getExtras(activityClass: String): Map<String, Any>? {
        return extras[activityClass]
    }
    
    private fun handleRoute(route: String) {
        val activityExtras = extras[route]
        showActivity(route, activityExtras)
    }
    
    private fun showActivity(activityClass: String, activityExtras: Map<String, Any>?, addToStack: Boolean = true) {
        // Destroy current activity
        currentActivity?.onPause()
        currentActivity?.onDestroy()
        
        // Create new activity
        val activity = createActivity(activityClass)
        
        if (activity != null) {
            currentActivity = activity
            
            if (addToStack) {
                activityStack.add(activityClass to activityExtras)
            }
            
            // Lifecycle methods
            activity.onCreate()
            activity.onStart()
            activity.onResume()
        } else {
            println("[ActivityRouter] Unknown activity: $activityClass")
            // Fallback to applications list
            if (activityClass != "ApplicationsListActivity") {
                navigateTo("ApplicationsListActivity")
            }
        }
    }
    
    private fun createActivity(activityClass: String): BaseActivity? {
        val activityExtras = extras[activityClass]
        
        return when (activityClass) {
            "ApplicationsListActivity" -> ApplicationsListActivity()
            "AddApplicationActivity" -> AddApplicationActivity()
            "CaseListActivity" -> {
                // CaseListActivity requires a filename extra
                val filename = activityExtras?.get("filename") as? String
                if (filename.isNullOrEmpty()) {
                    println("[ActivityRouter] CaseListActivity requires filename - redirecting to ApplicationsListActivity")
                    // Clear the bad state and redirect
                    window.location.hash = ""
                    return null
                }
                val activity = CaseListActivity()
                activity.setExtras(activityExtras)
                activity
            }
            "EntryActivity" -> {
                // EntryActivity requires a filename extra
                val filename = activityExtras?.get("filename") as? String
                if (filename.isNullOrEmpty()) {
                    println("[ActivityRouter] EntryActivity requires filename - redirecting to ApplicationsListActivity")
                    // Clear the bad state and redirect
                    window.location.hash = ""
                    return null
                }
                val activity = EntryActivity()
                activity.setExtras(activityExtras)
                activity
            }
            // Add more activities as needed
            else -> null
        }
    }
}
