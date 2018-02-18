package com.miruken.graph

fun Traversing.traverse(visitor: Visitor) {
    traverse(TraversingAxis.CHILD, visitor)
}

fun Traversing.traverseSelf(visitor: Visitor) {
    visitor(this)
}

fun Traversing.traverseRoot(visitor: Visitor) {
    var root   = this
    var parent = root.parent
    if (parent != null) {
        val visited = mutableListOf(this)
        while (parent != null) {
            checkCircularity(visited, parent)
            root   = parent
            parent = root.parent
        }
    }
    visitor(root)
}

fun Traversing.traverseChildren(
        visitor:  Visitor,
        withSelf: Boolean = true
) {
    if (withSelf && visitor(this)) return
    children.any(visitor)
}

fun Traversing.traverseAncestors(
        visitor:  Visitor,
        withSelf: Boolean = true
) {
    if (withSelf && visitor(this)) return
    var parent = this.parent
    if (parent != null) {
        val visited = mutableListOf(this)
        while (parent != null && !visitor(parent)) {
            checkCircularity(visited, parent)
            parent = parent.parent
        }
    }
}

fun Traversing.traverseDescendants(
        visitor:  Visitor,
        withSelf: Boolean = true
) {
    if (withSelf) {
        Traversal.levelOrder(this, visitor)
    }
    else {
        Traversal.levelOrder(this, { n -> n != this && visitor(n) } )
    }
}

fun Traversing.traverseDescendantsReverse(
        visitor:  Visitor,
        withSelf: Boolean = true
) {
    if (withSelf) {
        Traversal.reverseLevelOrder(this, visitor)
    }
    else {
        Traversal.reverseLevelOrder(this, { n -> n != this && visitor(n) } )
    }
}

fun Traversing.traverseSelfSiblingOrAncestor(
        visitor:      Visitor,
        withSelf:     Boolean = true,
        withAncestor: Boolean = true
) {
    if (withSelf && visitor(this)) return
    parent?.let {
        if (!it.children.any { sibling ->
                    sibling != this && visitor(sibling)}) {
            if (withAncestor)
                it.traverseAncestors(visitor)
        }
    }
}

internal fun checkCircularity(
        visited: MutableList<Traversing>,
        node:    Traversing) {
    if (visited.contains(node))
        throw Exception("Circularity detected for $node")
    visited.add(node)
}