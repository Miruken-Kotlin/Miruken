package com.miruken.callback

abstract class Options<T: Options<T>> : Composition(),
        BoundingCallback, ResolvingCallback,
        FilteringCallback, BatchingCallback {

    override var bounds: Any?    = null
    final override val canFilter = false
    final override val canBatch  = false

    override fun getResolveCallback() = this

    abstract fun mergeInto(other: T)
}
