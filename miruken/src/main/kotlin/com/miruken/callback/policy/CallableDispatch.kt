package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.callback.Strict
import com.miruken.callback.TypeFlags
import com.miruken.callback.UseFilter
import com.miruken.callback.UseFilterProvider
import com.miruken.concurrent.Promise
import com.miruken.runtime.getTaggedAnnotations
import com.miruken.runtime.isNothing
import com.miruken.runtime.isUnit
import com.miruken.runtime.normalize
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

class CallableDispatch(val callable: KCallable<*>) : KAnnotatedElement {
    val arguments: List<Argument> =
            callable.valueParameters.map { Argument(it) }

    val logicalReturnType: KType
    val returnFlags:       Flags<TypeFlags>
    val owningClass:       KClass<*> =
            callable.instanceParameter?.let {
                it.type.classifier as? KClass<*>
            } ?: throw IllegalArgumentException(
                    "Only class bindings are supported: $callable")
    val strict = annotations.any { it is Strict }

    init {
        val typeFlags     = TypeFlags.parse(returnType)
        logicalReturnType = typeFlags.second
        returnFlags       = typeFlags.first
    }

    inline   val returnType  get() = callable.returnType
    override val annotations get() = callable.annotations

    val javaMethod = when (callable) {
        is KFunction<*> -> callable.javaMethod!!
        is KProperty<*> -> callable.javaGetter!!
        else -> throw IllegalStateException()
    }

    val returnsSomething get() =
        !returnType.isUnit && !returnType.isNothing


    val useFilters by lazy {
        callable.getTaggedAnnotations<UseFilter<*>>()
                .flatMap { it.second }
                .normalize()
    }

    val useFilterProviders by lazy {
        callable.getTaggedAnnotations<UseFilterProvider<*>>()
                .flatMap { it.second }
                .normalize()
    }

    fun invoke(receiver: Any, arguments: Array<Any?>): Any? {
        return try {
            callable.call(receiver, *arguments)
        } catch (e: Throwable) {
            val cause = (e as? InvocationTargetException)?.cause ?: e
            if (returnFlags has TypeFlags.PROMISE) {
                Promise.reject(cause)
            } else {
                throw cause
            }
        }
    }
}