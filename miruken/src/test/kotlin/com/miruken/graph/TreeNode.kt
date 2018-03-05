package com.miruken.graph

data class TreeNode(val data: Any) : Traversing {
    private val _children = mutableListOf<TreeNode>()

    override var parent: Traversing? = null
      private set

    override val children: List<Traversing> = _children

    fun addChild(vararg children: TreeNode) : TreeNode {
        for (child in children) {
            child.parent = this
            _children.add(child)
        }
        return this
    }
}