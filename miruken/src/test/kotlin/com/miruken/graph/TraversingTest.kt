package com.miruken.graph

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("LocalVariableName")
class TraversingTest {
    private lateinit var _visited: MutableList<TreeNode>

    @Before
    fun setup() {
        _visited = mutableListOf()
    }

    @Test fun `Traverses self`() {
        val root = TreeNode("root")
        root.traverse(TraversingAxis.SELF, ::visit)
        assertEquals(listOf(root), _visited)
    }

    @Test fun `Traverses root`() {
        val root   = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        val child3 = TreeNode("child3")
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.ROOT, ::visit)
        assertEquals(listOf(root), _visited)
    }

    @Test fun `Traverses children`() {
        val root   = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        val child3 = TreeNode("child3")
                .addChild(TreeNode("child3 1"))
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.CHILD, ::visit)
        assertEquals(listOf(child1, child2, child3), _visited)
    }

    @Test fun `Traverses siblings`() {
        val root   = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        val child3 = TreeNode("child3")
                .addChild(TreeNode("child3 1"))
        root.addChild(child1, child2, child3)
        child2.traverse(TraversingAxis.SIBLING, ::visit)
        assertEquals(listOf(child1, child3), _visited)
    }

    @Test fun `Traverses children and self`() {
        val root   = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        val child3 = TreeNode("child3")
                .addChild(TreeNode("child3 1"))
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.SELF_CHILD, ::visit)
        assertEquals(listOf(root, child1, child2, child3), _visited)
    }

    @Test fun `Traverses siblings and self`() {
        val root   = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        val child3 = TreeNode("child3")
                .addChild(TreeNode("child3 1"))
        root.addChild(child1, child2, child3)
        child2.traverse(TraversingAxis.SELF_SIBLING, ::visit)
        assertEquals(listOf(child2, child1, child3), _visited)
    }

    @Test fun `Traverses ancestors`() {
        val root       = TreeNode("root")
        val child      = TreeNode("child")
        val grandChild = TreeNode("grandChild")
        root.addChild(child)
        child.addChild(grandChild)
        grandChild.traverse(TraversingAxis.ANCESTOR, ::visit)
        assertEquals(listOf(child, root), _visited)
    }

    @Test fun `Traverses ancestors and self`() {
        val root       = TreeNode("root")
        val child      = TreeNode("child")
        val grandChild = TreeNode("grandChild")
        child.addChild(grandChild)
        root.addChild(child)
        grandChild.traverse(TraversingAxis.SELF_ANCESTOR, ::visit)
        assertEquals(listOf( grandChild, child, root ), _visited)
    }

    @Test fun `Traverses descendants`() {
        val root     = TreeNode("root")
        val child1   = TreeNode("child1")
        val child2   = TreeNode("child2")
        val child3   = TreeNode("child3")
        val child3_1 = TreeNode("child3 1")
        child3.addChild(child3_1)
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.DESCENDANT, ::visit)
        assertEquals(listOf(child1, child2, child3, child3_1), _visited)
    }

    @Test fun `Traverses descendants reverse`() {
        val root     = TreeNode("root")
        val child1   = TreeNode("child1")
        val child2   = TreeNode("child2")
        val child3   = TreeNode("child3")
        val child3_1 = TreeNode("child3 1")
        child3.addChild(child3_1)
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.DESCENDANT_REVERSE, ::visit)
        assertEquals(listOf(child3_1, child1, child2, child3), _visited)
    }

    @Test fun `Traverses descendants and self`() {
        val root     = TreeNode("root")
        val child1   = TreeNode("child1")
        val child2   = TreeNode("child2")
        val child3   = TreeNode("child3")
        val child3_1 = TreeNode("child3 1")
        child3.addChild(child3_1)
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.SELF_DESCENDANT, ::visit)
        assertEquals(listOf(root, child1, child2, child3, child3_1), _visited)
    }

    @Test fun `Traverses descendants and self reverse`() {
        val root     = TreeNode("root")
        val child1   = TreeNode("child1")
        val child2   = TreeNode("child2")
        val child3   = TreeNode("child3")
        val child3_1 = TreeNode("child3 1")
        child3.addChild(child3_1)
        root.addChild(child1, child2, child3)
        root.traverse(TraversingAxis.SELF_DESCENDANT_REVERSE, ::visit)
        assertEquals(listOf(child3_1, child1, child2, child3, root), _visited)
    }

    @Test fun `Traverses ancestors, siblings and self`() {
        val root     = TreeNode("root")
        val parent   = TreeNode("parent")
        val child1   = TreeNode("child1")
        val child2   = TreeNode("child2")
        val child3   = TreeNode("child3")
        val child3_1 = TreeNode("child3 1")
        child3.addChild(child3_1)
        parent.addChild(child1, child2, child3)
        root.addChild(parent)
        child3.traverse(TraversingAxis.SELF_SIBLING_ANCESTOR, ::visit)
        assertEquals(listOf(child3, child1, child2, parent, root), _visited)
    }

    private fun visit(node: Traversing): Boolean {
        _visited.add(node as TreeNode)
        return false
    }
}