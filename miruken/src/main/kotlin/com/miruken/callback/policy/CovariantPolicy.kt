package com.miruken.callback.policy

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
    ): Collection<Any> {
        TODO("not implemented")
    }

    override fun compare(o1: Any?, o2: Any?): Int {
        TODO("not implemented")
    }
}