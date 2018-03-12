package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.Strict
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
    val strict      = annotations.any { it is Strict }
    val returnInfo  = TypeFlags.parse(callable.returnType)
    val arguments   = callable.valueParameters.map { Argument(it) }

    val owningClass = callable.instanceParameter?.let {
        it.type.classifier as? KClass<*>
    } ?: throw IllegalArgumentException(
            "Only class bindings are supported: $callable")

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
        (callable.getTaggedAnnotations<UseFilter>() +
         owningClass.getTaggedAnnotations()).flatMap { it.second } +
         callable.annotations.filterIsInstance<UseFilter>() +
         owningClass.annotations.filterIsInstance<UseFilter>()
                .normalize()
    }

    val useFilterProviders by lazy {
        (callable.getTaggedAnnotations<UseFilterProvider>() +
         owningClass.getTaggedAnnotations()).flatMap { it.second } +
         callable.annotations.filterIsInstance<UseFilterProvider>() +
         owningClass.annotations.filterIsInstance<UseFilterProvider>()
                .normalize()
    }

    fun invoke(receiver: Any, arguments: Array<Any?>): Any? {
        return try {
            callable.call(receiver, *arguments)
        } catch (e: Throwable) {
            val cause = (e as? InvocationTargetException)?.cause ?: e
            if (returnInfo.flags has TypeFlags.PROMISE) {
                Promise.reject(cause)
            } else {
                throw cause
            }
        }
    }
}