package com.miruken.callback.policy

open class ContravariantPolicy<A : Annotation, in C>(
        val target: (C) -> Any,
        build:  ContravariantPolicyBuilder.() -> Unit
) : CallbackPolicy() {

    init {
        val builder = ContravariantPolicyBuilder()
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
}