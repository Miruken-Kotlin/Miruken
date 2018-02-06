package com.miruken.callback

interface IHandler {
    fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler? = null
    ) : HandleResult

    operator fun plus(other: IHandler) =
            CascadeHandler(this, other)

    operator fun plus(others: Collection<Any>) =
            CompositeHandler(others.toMutableList().add(0, this))
}