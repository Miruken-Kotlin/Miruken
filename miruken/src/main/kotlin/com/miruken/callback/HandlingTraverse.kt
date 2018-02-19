package com.miruken.callback

import com.miruken.graph.TraversingAxis

fun HandlingAxis.axis(axis: TraversingAxis) : HandlingAxis =
        TraversingHandler(this, axis)

fun HandlingAxis.publish() = selfOrDescendant().notify()

fun HandlingAxis.self() = TraversingHandler(this, TraversingAxis.SELF)

fun HandlingAxis.root() = TraversingHandler(this, TraversingAxis.ROOT)

fun HandlingAxis.child() = TraversingHandler(this, TraversingAxis.CHILD)

fun HandlingAxis.sibling() = TraversingHandler(this, TraversingAxis.SIBLING)

fun HandlingAxis.ancestor() = TraversingHandler(this, TraversingAxis.ANCESTOR)

fun HandlingAxis.descendant() = TraversingHandler(this, TraversingAxis.DESCENDANT)

fun HandlingAxis.descendantReverse() = TraversingHandler(this, TraversingAxis.DESCENDANT_REVERSE)

fun HandlingAxis.selfOrChild() = TraversingHandler(this, TraversingAxis.SELF_CHILD)

fun HandlingAxis.selfOrSibling() = TraversingHandler(this, TraversingAxis.SELF_SIBLING)

fun HandlingAxis.selfOrAncestor() = TraversingHandler(this, TraversingAxis.SELF_ANCESTOR)

fun HandlingAxis.selfOrDescendant() = TraversingHandler(this, TraversingAxis.SELF_DESCENDANT)

fun HandlingAxis.selfOrDescendantReverse() = TraversingHandler(this, TraversingAxis.SELF_DESCENDANT_REVERSE)

fun HandlingAxis.selfSiblingOrAncestor() = TraversingHandler(this, TraversingAxis.SELF_SIBLING_ANCESTOR)


