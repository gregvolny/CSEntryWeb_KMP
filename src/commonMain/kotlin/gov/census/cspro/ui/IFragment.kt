package gov.census.cspro.ui

/**
 * Platform-independent Fragment abstraction
 * Represents the Android Fragment concept for web
 */
interface IFragment {
    /**
     * Called when fragment is attached to activity
     */
    fun onAttach()
    
    /**
     * Called to create the fragment's view
     */
    fun onCreateView(): Any?
    
    /**
     * Called after view is created
     */
    fun onViewCreated()
    
    /**
     * Called when fragment becomes visible
     */
    fun onResume()
    
    /**
     * Called when fragment is no longer visible
     */
    fun onPause()
    
    /**
     * Called when fragment view is being destroyed
     */
    fun onDestroyView()
    
    /**
     * Called when fragment is detached from activity
     */
    fun onDetach()
}

/**
 * Base Fragment implementation for web
 */
abstract class BaseFragment : IFragment {
    internal var activity: BaseActivity? = null
    protected var view: Any? = null
    
    override fun onAttach() {
        // Override in subclass
    }
    
    override fun onCreateView(): Any? {
        return null
    }
    
    override fun onViewCreated() {
        // Override in subclass
    }
    
    override fun onResume() {
        // Override in subclass
    }
    
    override fun onPause() {
        // Override in subclass
    }
    
    override fun onDestroyView() {
        view = null
    }
    
    override fun onDetach() {
        activity = null
    }
}
