package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo

open class CovariantPolicy(
        build:  CovariantKeyBuilder.() -> Unit
) : CallbackPolicy() {

    lateinit var keyFunctor: (Any) -> Any?
        internal set

    init {
        @Suppress("LeakingThis")
        val builder = CovariantKeyBuilder(this)
        builder.build()
    }

    override fun getKey(callback: Any): Any? =
            keyFunctor(callback)

    override fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any> =
         output.filter {
             key != it && isAssignableTo(key, it)
         }

    override fun compare(o1: Any?, o2: Any?): Int {
        return when {
            o1 == o2 -> 0
            o1 == null -> 1
            o2 == null -> -1
            isAssignableTo(o1, o2) -> -1
            else -> 1
        }
    }
}