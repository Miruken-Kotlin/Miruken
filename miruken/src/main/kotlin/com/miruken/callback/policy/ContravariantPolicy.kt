package com.miruken.callback.policy

open class ContravariantPolicy(
        build: ContravariantTargetBuilder.() -> Unit
) : CallbackPolicy() {

    init {
        @Suppress("LeakingThis")
        val builder = ContravariantTargetBuilder(this)
        builder.build()
    }

    override fun getKey(callback: Any): Any? {
        TODO("not implemented")
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