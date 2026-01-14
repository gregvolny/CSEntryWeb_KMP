package gov.census.cspro.engine

/**
 * Userbar handler for managing toolbar actions
 * Ported from Android UserbarHandler.java
 */
class UserbarHandler {
    
    private val userbarButtons = mutableListOf<UserbarButton>()
    
    fun addButton(button: UserbarButton) {
        userbarButtons.add(button)
    }
    
    fun getButtons(): List<UserbarButton> = userbarButtons
    
    fun clear() {
        userbarButtons.clear()
    }
    
    fun handleButtonClick(index: Int) {
        if (index in userbarButtons.indices) {
            userbarButtons[index].onClick()
        }
    }
}

data class UserbarButton(
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)
