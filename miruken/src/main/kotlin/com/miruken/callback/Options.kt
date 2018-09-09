package com.miruken.callback

abstract class Options<T: Options<T>> : Composition(),
        BoundingCallback, InferringCallback,
        FilteringCallback, BatchingCallback {

    override var bounds: Any?    = null
    final override val canFilter = false
    final override val canBatch  = false

    override fun inferCallback() = this

    abstract fun mergeInto(other: T)
}

inline fun <reified T: Options<T>> Handling.getOptions(options: T) =
        options.takeIf { handle(it, true).handled }