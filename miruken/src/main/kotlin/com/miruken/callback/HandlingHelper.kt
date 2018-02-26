package com.miruken.callback

operator fun Handling.plus(other: Handling) =
        CascadeHandler(this, other)

operator fun Handling.plus(others: Collection<Any>) =
        CompositeHandler(others.toMutableList().add(0, this))