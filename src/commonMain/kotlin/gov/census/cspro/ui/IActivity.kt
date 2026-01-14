package gov.census.cspro.ui

/**
 * Platform-independent Activity abstraction
 * Represents the Android Activity concept for web
 */
interface IActivity {
    /**
     * Called when the activity is first created
     */
    fun onCreate()
    
    /**
     * Called after onCreate to initialize the view
     */
    fun onStart()
    
    /**
     * Called when activity becomes visible
     */
    fun onResume()
    
    /**
     * Called when activity is no longer visible
     */
    fun onPause()
    
    /**
     * Called when activity is being destroyed
     */
    fun onDestroy()
    
    /**
     * Set the title of the activity
     */
    fun setTitle(title: String)
    
    /**
     * Navigate to another activity
     */
    fun startActivity(activityClass: String, extras: Map<String, Any>? = null)
    
    /**
     * Finish this activity
     */
    fun finish()
}

/**
 * Base Activity implementation for web
 */
abstract class BaseActivity : IActivity {
    protected var isCreated = false
    protected var isStarted = false
    protected var isResumed = false
    protected var extras: Map<String, Any>? = null
    
    /**
     * Set extras (intent data) for this activity
     */
    fun setExtras(extras: Map<String, Any>?) {
        this.extras = extras
    }
    
    /**
     * Get a string extra by key
     */
    protected fun getStringExtra(key: String): String? {
        return extras?.get(key) as? String
    }
    
    /**
     * Get a double extra by key with default value
     */
    protected fun getDoubleExtra(key: String, defaultValue: Double): Double {
        return when (val value = extras?.get(key)) {
            is Double -> value
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: defaultValue
            else -> defaultValue
        }
    }
    
    override fun onCreate() {
        isCreated = true
    }
    
    override fun onStart() {
        isStarted = true
    }
    
    override fun onResume() {
        isResumed = true
    }
    
    override fun onPause() {
        isResumed = false
    }
    
    override fun onDestroy() {
        isCreated = false
        isStarted = false
        isResumed = false
    }
    
    abstract fun setContentView(layoutId: String)
}
