package com.miruken.callback

abstract class Options<T: Options<T>> : Composition(),
        BoundingCallback, ResolvingCallback,
        FilteringCallback, BatchingCallback {

    override var bounds: Any?    = null
    final override val canFilter = false
    final override val canBatch  = false

    override fun getResolveCallback() = this

    abstract fun mergeInto(other: T)

    fun decorate(handler: Handling) : OptionsHandler<T>
    {
        @Suppress("UNCHECKED_CAST")
        return OptionsHandler(handler, this as T)
    }
}
