package com.miruken.callback

import com.miruken.graph.TraversingAxis

fun HandlingAxis.axis(axis: TraversingAxis): HandlingAxis =
        TraversingHandler(this, axis)

val HandlingAxis.xpublish get() = xselfOrDescendant.notify

val HandlingAxis.xself get() =
    TraversingHandler(this, TraversingAxis.SELF)

val HandlingAxis.xroot get() =
    TraversingHandler(this, TraversingAxis.ROOT)

val HandlingAxis.xchild get() =
    TraversingHandler(this, TraversingAxis.CHILD)

val HandlingAxis.xsibling get() =
    TraversingHandler(this, TraversingAxis.SIBLING)

val HandlingAxis.xancestor get() =
    TraversingHandler(this, TraversingAxis.ANCESTOR)

val HandlingAxis.xdescendant get() =
    TraversingHandler(this, TraversingAxis.DESCENDANT)

val HandlingAxis.xdescendantReverse get() =
    TraversingHandler(this, TraversingAxis.DESCENDANT_REVERSE)

val HandlingAxis.xselfOrChild get() =
    TraversingHandler(this, TraversingAxis.SELF_CHILD)

val HandlingAxis.xselfOrSibling get() =
    TraversingHandler(this, TraversingAxis.SELF_SIBLING)

val HandlingAxis.xselfOrAncestor get() =
    TraversingHandler(this, TraversingAxis.SELF_ANCESTOR)

val HandlingAxis.xselfOrDescendant get() =
    TraversingHandler(this, TraversingAxis.SELF_DESCENDANT)

val HandlingAxis.xselfOrDescendantReverse get() =
    TraversingHandler(this, TraversingAxis.SELF_DESCENDANT_REVERSE)

val HandlingAxis.xselfSiblingOrAncestor get() =
    TraversingHandler(this, TraversingAxis.SELF_SIBLING_ANCESTOR)


