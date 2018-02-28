package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.callback.Strict
import com.miruken.concurrent.Promise
import com.miruken.runtime.isNothing
import com.miruken.runtime.isUnit
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

class CallableDispatch(val callable: KCallable<*>){
    val arguments: List<Argument> =
            callable.valueParameters.map { Argument(it) }

    val strict:            Boolean
    val logicalReturnType: KType
    val returnFlags:       Flags<TypeFlags>
    val owningClass:       KClass<*> =
            callable.instanceParameter?.let {
                it.type.classifier as? KClass<*>
            } ?: throw IllegalArgumentException(
                    "Only class bindings are supported: $callable")

    init {
        val typeFlags     = TypeFlags.parse(returnType)
        logicalReturnType = typeFlags.second
        returnFlags       = typeFlags.first
        strict            = annotations.any { it is Strict }
    }

    inline val returnType  get() = callable.returnType
    inline val annotations get() = callable.annotations

    val javaMethod = when (callable) {
        is KFunction<*> -> callable.javaMethod!!
        is KProperty<*> -> callable.javaGetter!!
        else -> throw IllegalStateException()
    }

    val returnsSomething get() =
        !returnType.isUnit && !returnType.isNothing

    fun invoke(receiver: Any, arguments: Array<Any?>): Any? {
        return if (returnFlags has TypeFlags.PROMISE) {
            try {
                callable.call(receiver, *arguments)
            } catch (e: Throwable) {
                if (e is InvocationTargetException)
                    Promise.reject(e.cause ?: e)
                else Promise.reject(e)
            }
        } else {
            callable.call(receiver, *arguments)
        }
    }
}