package com.miruken.graph

enum class TraversingAxis {
    SELF,
    ROOT,
    CHILD,
    SIBLING,
    ANCESTOR,
    DESCENDANT,
    DESCENDANT_REVERSE,
    SELF_CHILD,
    SELF_SIBLING,
    SELF_ANCESTOR,
    SELF_DESCENDANT,
    SELF_DESCENDANT_REVERSE,
    SELF_SIBLING_ANCESTOR
}

typealias Visitor = (Traversing) -> Boolean

interface Traversing {
    val parent: Traversing?

    val children: List<Traversing>

    fun traverse(axis: TraversingAxis, visitor: Visitor) {
        when (axis) {
            TraversingAxis.SELF -> traverseSelf(visitor)
            TraversingAxis.ROOT -> traverseRoot(visitor)
            TraversingAxis.CHILD -> traverseChildren(visitor, false)
            TraversingAxis.SIBLING -> traverseSelfSiblingOrAncestor(visitor, false, false)
            TraversingAxis.ANCESTOR -> traverseAncestors(visitor, false)
            TraversingAxis.DESCENDANT -> traverseDescendants(visitor, false)
            TraversingAxis.DESCENDANT_REVERSE -> traverseDescendantsReverse(visitor, false)
            TraversingAxis.SELF_CHILD -> traverseChildren(visitor)
            TraversingAxis.SELF_SIBLING -> traverseSelfSiblingOrAncestor(visitor, true, false)
            TraversingAxis.SELF_ANCESTOR -> traverseAncestors(visitor)
            TraversingAxis.SELF_DESCENDANT -> traverseDescendants(visitor)
            TraversingAxis.SELF_DESCENDANT_REVERSE -> traverseDescendantsReverse(visitor)
            TraversingAxis.SELF_SIBLING_ANCESTOR -> traverseSelfSiblingOrAncestor(visitor)
        }
    }
}