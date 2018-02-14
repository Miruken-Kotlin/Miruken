package com.miruken.callback

abstract class Options<T> : Composition(),
    BoundingCallback, ResolvingCallback,
    FilteringCallback, BatchingCallback {

    override var bounds: Any? = null

    abstract fun mergeInto(other: T)

    override val allowFiltering = false
    override val allowBatching  = false

    override fun getResolveCallback() = this
}
