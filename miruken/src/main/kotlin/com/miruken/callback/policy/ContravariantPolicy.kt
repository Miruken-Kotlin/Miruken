package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KClass
import kotlin.reflect.KType

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
            it as? KClass<*> ?: it::class
        } ?: callback::class
    }

    override fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any> =
            output.filter { key != it && isAssignableTo(it, key)
    }

    override fun compare(o1: Any?, o2: Any?): Int {
        return when {
            o1 == o2 -> 0
            o1 == null -> 1
            o2 == null -> -1
            else -> compareGenericArity(o1, o2)?.let { it }
                ?: if (isAssignableTo(o2, o1)) -1 else 1
        }
    }
    
    private fun compareGenericArity(o1: Any?, o2: Any?): Int? =
            when (o1) {
                is KType -> when (o2) {
                    is KType -> o2.arguments.size - o1.arguments.size
                    is KClass<*> ->
                        o2.typeParameters.size - o1.arguments.size
                    is Class<*> ->
                        o2.typeParameters.size - o1.arguments.size
                    else -> null
                }
                is KClass<*> -> when (o2) {
                    is KType -> o2.arguments.size - o1.typeParameters.size
                    is KClass<*> -> o2.typeParameters.size - o1.typeParameters.size
                    is Class<*> -> o2.typeParameters.size - o1.typeParameters.size
                    else -> null
                }
                is Class<*> -> when (o2) {
                    is KType -> o2.arguments.size - o1.typeParameters.size
                    is Class<*> -> o2.typeParameters.size - o1.typeParameters.size
                    else -> null
                }
                else -> null
            }
}