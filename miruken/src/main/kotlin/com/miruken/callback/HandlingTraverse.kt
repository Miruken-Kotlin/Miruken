package com.miruken.callback

import com.miruken.graph.TraversingAxis

fun HandlingAxis.axis(axis: TraversingAxis) : HandlingAxis =
        TraversingHandler(this, axis)

val HandlingAxis.publish get() = selfOrDescendant.notify

val HandlingAxis.self get() =
    TraversingHandler(this, TraversingAxis.SELF)

val HandlingAxis.root get() =
    TraversingHandler(this, TraversingAxis.ROOT)

val HandlingAxis.child get() =
    TraversingHandler(this, TraversingAxis.CHILD)

val HandlingAxis.sibling get() =
    TraversingHandler(this, TraversingAxis.SIBLING)

val HandlingAxis.ancestor get() =
    TraversingHandler(this, TraversingAxis.ANCESTOR)

val HandlingAxis.descendant get() =
    TraversingHandler(this, TraversingAxis.DESCENDANT)

val HandlingAxis.descendantReverse get() =
    TraversingHandler(this, TraversingAxis.DESCENDANT_REVERSE)

val HandlingAxis.selfOrChild get() =
    TraversingHandler(this, TraversingAxis.SELF_CHILD)

val HandlingAxis.selfOrSibling get() =
    TraversingHandler(this, TraversingAxis.SELF_SIBLING)

val HandlingAxis.selfOrAncestor get() =
    TraversingHandler(this, TraversingAxis.SELF_ANCESTOR)

val HandlingAxis.selfOrDescendant get() =
    TraversingHandler(this, TraversingAxis.SELF_DESCENDANT)

val HandlingAxis.selfOrDescendantReverse get() =
    TraversingHandler(this, TraversingAxis.SELF_DESCENDANT_REVERSE)

val HandlingAxis.selfSiblingOrAncestor get() =
    TraversingHandler(this, TraversingAxis.SELF_SIBLING_ANCESTOR)


