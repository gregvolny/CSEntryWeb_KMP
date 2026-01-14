package gov.census.cspro.ui

import gov.census.cspro.data.CaseTreeNode
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

/**
 * Web equivalent of Android's NavigationFragment
 * Displays the case tree (sidebar) for navigating through the questionnaire
 * 
 * Mirrors Android CSEntryDroid UI exactly, with hierarchical tree nodes,
 * icons, and occurrence indicators.
 */
class NavigationFragment : BaseFragment() {
    private var container: HTMLDivElement? = null
    private var caseTree: List<CaseTreeNode> = emptyList()
    
    override fun onCreateView() {
        container = document.createElement("div") as HTMLDivElement
        container?.id = "navigation-root"
        container?.className = "navigation-fragment"
        
        val parent = document.getElementById("navigation-fragment")
        parent?.innerHTML = ""
        parent?.appendChild(container!!)
    }
    
    fun updateCaseTree(nodes: List<CaseTreeNode>) {
        caseTree = nodes
        // Debug: print case tree structure
        println("[NavigationFragment] updateCaseTree: ${nodes.size} root nodes")
        for (node in nodes) {
            printNodeTree(node, 0)
        }
        // Auto-expand to show current node
        expandToCurrentNode(caseTree)
        render()
    }
    
    private fun printNodeTree(node: CaseTreeNode, level: Int) {
        val indent = "  ".repeat(level)
        println("$indent[Node] id=${node.id} type=${node.type} label='${node.label}' expanded=${node.expanded} children=${node.children.size}")
        for (child in node.children) {
            printNodeTree(child, level + 1)
        }
    }
    
    private fun render() {
        val root = container ?: return
        
        // Match Android layout: fragment_navigation_drawer.xml
        val html = StringBuilder()
        
        html.append("""
            <div class="navigation-header">
                <div class="nav-header-row">
                    <h3>Case Navigation</h3>
                    <button id="btn-close-nav" class="btn-close-nav" title="Close">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                        </svg>
                    </button>
                </div>
                <div class="navigation-search">
                    <input type="text" placeholder="Search items..." id="nav-search" />
                </div>
            </div>
            <div class="case-tree">
        """)
        
        // Render tree nodes recursively
        for (node in caseTree) {
            html.append(renderNode(node, 0))
        }
        
        html.append("</div>") // Close case-tree
        
        root.innerHTML = html.toString()
        attachNodeListeners()
    }
    
    private fun renderNode(node: CaseTreeNode, level: Int): String {
        val paddingLeft = level * 16 + 12
        val hasChildren = node.children.isNotEmpty()
        val expandIcon = if (hasChildren) {
            if (node.expanded) "▼" else "▶"
        } else {
            ""  // No icon for leaf nodes
        }
        
        // Build CSS classes based on node type and state
        // Match Android CaseTreeNode constants: ITEM_LEVEL, ITEM_GROUP, ITEM_FIELD, etc.
        val nodeClasses = StringBuilder("tree-node")
        
        // Node type classes
        when (node.type) {
            CaseTreeNode.ITEM_LEVEL, CaseTreeNode.ITEM_GROUP, 
            CaseTreeNode.ITEM_GROUP_PLACEHOLDER, CaseTreeNode.ITEM_GROUP_OCCURRENCE -> {
                nodeClasses.append(" group-node")
            }
            CaseTreeNode.ITEM_FIELD -> {
                nodeClasses.append(" field-node")
            }
        }
        
        // Color/state classes (matching Android FRM_FIELDCOLOR constants)
        when (node.color) {
            CaseTreeNode.FRM_FIELDCOLOR_CURRENT -> nodeClasses.append(" current-node field-color-current")
            CaseTreeNode.FRM_FIELDCOLOR_VISITED -> nodeClasses.append(" field-color-visited")
            CaseTreeNode.FRM_FIELDCOLOR_SKIPPED -> nodeClasses.append(" field-color-skipped")
            CaseTreeNode.FRM_FIELDCOLOR_NEVERVISITED -> nodeClasses.append(" field-color-nevervisited")
            CaseTreeNode.FRM_FIELDCOLOR_PROTECTED -> nodeClasses.append(" field-color-protected")
            CaseTreeNode.FRM_FIELDCOLOR_PARENT -> nodeClasses.append(" field-color-parent")
        }
        
        if (!node.visible) nodeClasses.append(" node-hidden")
        
        // Count children for occurrence badge (for rosters/groups)
        val childCount = if (hasChildren && node.type != CaseTreeNode.ITEM_FIELD) {
            node.children.size
        } else {
            0
        }
        
        val html = StringBuilder()
        
        // Render based on node type (matching Android layouts)
        if (node.type == CaseTreeNode.ITEM_FIELD) {
            // Field node - Android case_tree_field_node.xml: label + value stacked
            html.append("""
                <div class="$nodeClasses" data-node-id="${node.id}" style="padding-left: ${paddingLeft}px;">
                    <span class="node-label">${node.label}</span>
                    ${if (node.value.isNotEmpty()) "<span class='node-value'>${node.value}</span>" else ""}
                </div>
            """)
        } else {
            // Group node - Android case_tree_group_node.xml: label + occurrence badge
            html.append("""
                <div class="$nodeClasses" data-node-id="${node.id}" style="padding-left: ${paddingLeft}px;">
                    ${if (expandIcon.isNotEmpty()) "<span class='expand-icon'>$expandIcon</span>" else ""}
                    <span class="node-label">${node.label}</span>
                    ${if (childCount > 0) "<span class='occurrence-badge'>$childCount</span>" else ""}
                </div>
            """)
        }
        
        // Render children if expanded
        if (node.expanded && hasChildren) {
            for (child in node.children) {
                html.append(renderNode(child, level + 1))
            }
        }
        
        return html.toString()
    }
    
    private fun attachNodeListeners() {
        val root = container ?: return
        val nodes = root.querySelectorAll(".tree-node")
        
        for (i in 0 until nodes.length) {
            val nodeElement = nodes.item(i) as? HTMLElement ?: continue
            val nodeId = nodeElement.getAttribute("data-node-id")?.toIntOrNull() ?: continue
            
            nodeElement.addEventListener("click", {
                onNodeClicked(nodeId)
            })
        }
        
        // Close button listener
        val closeBtn = document.getElementById("btn-close-nav") as? HTMLElement
        closeBtn?.addEventListener("click", {
            val sidebar = document.getElementById("navigation-sidebar")
            // Desktop uses width-based collapse ("collapsed"), while mobile uses overlay ("open").
            // Ensure the sidebar actually hides in both modes.
            sidebar?.classList?.remove("open")
            sidebar?.classList?.add("collapsed")
        })
    }
    
    private fun onNodeClicked(nodeId: Int) {
        println("[NavigationFragment] Node clicked: $nodeId")
        
        // Find the node and toggle expansion or navigate to field
        val node = findNodeById(caseTree, nodeId)
        if (node != null) {
            if (node.type == CaseTreeNode.ITEM_FIELD) {
                // Field node clicked - navigate to this field
                // TODO: Call engine to navigate to this field
                println("[NavigationFragment] Navigating to field: ${node.label}")
            } else {
                // Group node clicked - toggle expansion
                node.expanded = !node.expanded
                println("[NavigationFragment] Toggled expansion for ${node.label}: ${node.expanded}")
                render()
            }
        }
    }
    
    /**
     * Find a node by ID in the tree recursively
     */
    private fun findNodeById(nodes: List<CaseTreeNode>, nodeId: Int): CaseTreeNode? {
        for (node in nodes) {
            if (node.id == nodeId) return node
            val found = findNodeById(node.children, nodeId)
            if (found != null) return found
        }
        return null
    }
    
    /**
     * Expand all nodes from root to the current field
     */
    private fun expandToCurrentNode(nodes: List<CaseTreeNode>): Boolean {
        var foundCurrent = false
        for (node in nodes) {
            if (node.color == CaseTreeNode.FRM_FIELDCOLOR_CURRENT) {
                foundCurrent = true
            } else if (node.children.isNotEmpty()) {
                val childHasCurrent = expandToCurrentNode(node.children)
                if (childHasCurrent) {
                    node.expanded = true
                    foundCurrent = true
                }
            }
        }
        return foundCurrent
    }
}
