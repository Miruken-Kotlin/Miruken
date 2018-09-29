package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.Strict
import com.miruken.callback.UseFilter
import com.miruken.callback.UseFilterProvider
import com.miruken.concurrent.Promise
import com.miruken.runtime.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.*

class CallableDispatch(val callable: KCallable<*>) : KAnnotatedElement {
    val strict      = annotations.any { it is Strict }
    val returnInfo  = TypeFlags.parse(callable.returnType)
    val arguments   = callable.valueParameters.map { Argument(it) }
    val owningType  = callable.instanceParameter?.type ?:
                      callable.returnType

    val useFilters =
            (callable.getMetaAnnotations<UseFilter>() +
                    owningClass.getMetaAnnotations())
                    .flatMap { it.second }
                    .normalize()

    val useFilterProviders =
            (callable.getMetaAnnotations<UseFilterProvider>() +
                    owningClass.getMetaAnnotations())
                    .flatMap { it.second }
                    .normalize()

    init { callable.isAccessible = true }

    inline   val owningClass get() = owningType.jvmErasure
    inline   val arity       get() = arguments.size
    inline   val returnType  get() = callable.returnType
    override val annotations get() = callable.annotations

    val javaMember: Member get() = when (callable) {
        is KFunction<*> -> callable.javaMethod ?: callable.javaConstructor!!
        is KProperty<*> -> callable.javaGetter ?: callable.javaField!!
        else -> error("Unrecognized callable $callable")
    }

    val returnsSomething get() =
        !returnType.isUnit && !returnType.isNothing

    fun invoke(receiver: Any, arguments: Array<Any?>): Any? {
        return try {
            if (callable.requiresReceiver) {
                callable.call(receiver, *arguments)
            } else {
                callable.call(*arguments)
            }
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