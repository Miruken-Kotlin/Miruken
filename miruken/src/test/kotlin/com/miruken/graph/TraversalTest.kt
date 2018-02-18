package com.miruken.graph

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("PrivatePropertyName")
class TraversalTest {
    private lateinit var root:     TreeNode
    private lateinit var child1:   TreeNode
    private lateinit var child1_1: TreeNode
    private lateinit var child2:   TreeNode
    private lateinit var child2_1: TreeNode
    private lateinit var child2_2: TreeNode
    private lateinit var child3:   TreeNode
    private lateinit var child3_1: TreeNode
    private lateinit var child3_2: TreeNode
    private lateinit var child3_3: TreeNode

    private lateinit var _visited: MutableList<TreeNode>

    @Before
    fun setup() {
        _visited = mutableListOf()
        root     = TreeNode("root")
        child1   = TreeNode("child1")
        child1_1 = TreeNode("child1_1")
        child2   = TreeNode("child2")
        child2_1 = TreeNode("child2_1")
        child2_2 = TreeNode("child2_2")
        child3   = TreeNode("child3")
        child3_1 = TreeNode("child3_1")
        child3_2 = TreeNode("child3_2")
        child3_3 = TreeNode("child3_3")
        child1.addChild(child1_1)
        child2.addChild(child2_1, child2_2)
        child3.addChild(child3_1, child3_2, child3_3)
        root.addChild(child1, child2, child3)
    }

    @Test fun `Traverse pre order`() {
        Traversal.preOrder(root, ::visit)
        assertEquals(listOf(
                root, child1, child1_1, child2,
                child2_1, child2_2, child3, child3_1,
                child3_2, child3_3), _visited)
    }

    @Test fun `Traverse post order`() {
        Traversal.postOrder(root, ::visit)
        assertEquals(listOf(
                child1_1, child1, child2_1, child2_2,
                child2, child3_1, child3_2, child3_3,
                child3, root), _visited)
    }

    @Test fun `Traverse level order`() {
        Traversal.levelOrder(root, ::visit)
        assertEquals(listOf(
                root, child1, child2, child3, child1_1,
                child2_1, child2_2, child3_1, child3_2,
                child3_3), _visited)
    }

    @Test fun `Traverse reverse level order`() {
        Traversal.reverseLevelOrder(root, ::visit)
        assertEquals(listOf(
                child1_1, child2_1, child2_2, child3_1,
                child3_2, child3_3, child1, child2,
                child3, root), _visited)
    }

    private fun visit(node: Traversing) : Boolean {
        _visited.add(node as TreeNode)
        return false
    }
}