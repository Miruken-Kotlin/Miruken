package com.miruken.graph

import java.util.LinkedList

object Traversal {
    fun preOrder(node: Traversing, visitor: Visitor) =
            preOrder(node, visitor, mutableListOf())

    private fun preOrder(
            node:    Traversing,
            visitor: Visitor,
            visited: MutableList<Traversing>
    ): Boolean {
        checkCircularity(visited, node)
        if (visitor(node)) return true
        node.traverse { child -> preOrder(child, visitor, visited) }
        return false
    }

    fun postOrder(node: Traversing, visitor: Visitor) =
            postOrder(node, visitor, mutableListOf())

    private fun postOrder(
            node:    Traversing,
            visitor: Visitor,
            visited: MutableList<Traversing>
    ): Boolean {
        checkCircularity(visited, node)
        node.traverse { child -> postOrder(child, visitor, visited) }
        return visitor(node)
    }

    fun levelOrder(node: Traversing, visitor: Visitor) =
            levelOrder(node, visitor, mutableListOf())

    private fun levelOrder(
            node:    Traversing,
            visitor: Visitor,
            visited: MutableList<Traversing>) {
        val queue = LinkedList<Traversing>().apply { add(node) }
        while (queue.isNotEmpty()) {
            val next = queue.remove()
            checkCircularity(visited, next)
            if (visitor(next)) break
            next.traverse { child -> !queue.offer(child) }
        }
    }

    fun reverseLevelOrder(node: Traversing, visitor: Visitor) =
            reverseLevelOrder(node, visitor, mutableListOf())

    private fun reverseLevelOrder(
            node:    Traversing,
            visitor: Visitor,
            visited: MutableList<Traversing>) {
        val queue = LinkedList<Traversing>().apply { add(node) }
        val stack = LinkedList<Traversing>()
        while(queue.isNotEmpty()) {
            val next = queue.remove()
            checkCircularity(visited, next)
            stack.push(next)
            val level = LinkedList<Traversing>()
            next.traverse { child -> level.addFirst(child); false }
            queue.addAll(level)
        }
        while (stack.isNotEmpty() && !visitor(stack.pop())) {}
    }
}