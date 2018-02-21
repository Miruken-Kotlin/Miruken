package com.miruken.callback.policy

import kotlin.reflect.KClass

open class ContravariantPolicy(
        build: ContravariantTargetBuilder.() -> Unit
) : CallbackPolicy() {

    lateinit var targetFunctor: (Any) -> Any?
        internal set

    init {
        @Suppress("LeakingThis")
        val builder = ContravariantTargetBuilder(this)
        builder.build()
    }

    override fun getKey(callback: Any): Any? {
        return targetFunctor(callback)?.let {
            (it as? KClass<*>) ?: it::class
        } ?: callback::class
    }

    override fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any> {
        TODO("not implemented")
    }

    override fun compare(o1: Any?, o2: Any?): Int {
        TODO("not implemented")
    }
}