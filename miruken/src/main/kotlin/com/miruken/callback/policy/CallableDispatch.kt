package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.Strict
import com.miruken.callback.UseFilter
import com.miruken.callback.UseFilterProvider
import com.miruken.concurrent.Promise
import com.miruken.runtime.getMetaAnnotations
import com.miruken.runtime.isNothing
import com.miruken.runtime.isUnit
import com.miruken.runtime.normalize
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.*

class CallableDispatch(val callable: KCallable<*>) : KAnnotatedElement {
    init { callable.isAccessible = true }

    val strict      = annotations.any { it is Strict }
    val returnInfo  = TypeFlags.parse(callable.returnType)
    val arguments   = callable.valueParameters.map { Argument(it) }
    val owningType  = callable.instanceParameter?.type ?:
                      callable.returnType

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

    val isConstructor get() =
        when (callable.parameters.firstOrNull()?.kind) {
            KParameter.Kind.INSTANCE,
            KParameter.Kind.EXTENSION_RECEIVER -> false
            else -> true
        }

    val useFilters by lazy {
        (callable.getMetaAnnotations<UseFilter>() +
         owningClass.getMetaAnnotations()).flatMap { it.second }
                .normalize()
    }

    val useFilterProviders by lazy {
        (callable.getMetaAnnotations<UseFilterProvider>() +
         owningClass.getMetaAnnotations()).flatMap { it.second }
                .normalize()
    }

    fun invoke(receiver: Any, arguments: Array<Any?>): Any? {
        return try {
            if (isConstructor) {
                callable.call(*arguments)
            } else {
                callable.call(receiver, *arguments)
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