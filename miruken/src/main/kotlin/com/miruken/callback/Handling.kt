package com.miruken.callback

interface Handling {
    fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: Handling? = null
    ) : HandleResult

    operator fun plus(other: Handling) =
            CascadeHandler(this, other)

    operator fun plus(others: Collection<Any>) =
            CompositeHandler(others.toMutableList().add(0, this))
}