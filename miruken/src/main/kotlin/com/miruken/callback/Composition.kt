package com.miruken.callback

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.safeCast

open class Composition(callback: Any) : Trampoline(callback),
        Callback, ResolvingCallback,
        FilteringCallback, BatchingCallback {

    protected constructor() : this(Unit)

    override val resultType: KType?
        get() = (callback as? Callback)?.resultType

    override var result: Any?
        get() = (callback as? Callback)?.result
        set(value) {
            (callback as? Callback)?.result = value
        }

    override val allowFiltering: Boolean
        get() = (callback as? FilteringCallback)?.allowFiltering != false

    override val allowBatching: Boolean
        get() = (callback as? BatchingCallback)?.allowBatching != false

    override fun getResolveCallback(): Any {
        val resolve = (callback as? ResolvingCallback)?.getResolveCallback()
        return if (resolve === callback) this else
            Composition(resolve ?: Resolution.getDefaultResolvingCallback(callback))
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Any> get(callback: Any) : T? {
            return (callback as? Composition)
                    ?.let { it.callback as? T }
        }

        fun get(callback: Any, clazz: KClass<*>) : Any? {
            return (callback as? Composition)?.let {
                clazz.safeCast(it.callback)
            }
        }
    }
}