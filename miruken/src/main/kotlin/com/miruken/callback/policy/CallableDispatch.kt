package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.runtime.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.*

class CallableDispatch(val callable: KCallable<*>) : KAnnotatedElement {
    val strict     = annotations.any { it is Strict }
    val returnInfo = TypeInfo.parse(callable.returnType)
    val arguments  = callable.valueParameters.map { Argument(it) }

    init { callable.isAccessible = true }

    inline   val arity       get() = arguments.size
    inline   val owningClass get() = owningType.jvmErasure
    inline   val returnType  get() = callable.returnType
    override val annotations get() = callable.annotations

    val owningType get() = callable.instanceParameter?.type
            ?: callable.returnType

    val filterProviders by lazy { getFilterProviders() }

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