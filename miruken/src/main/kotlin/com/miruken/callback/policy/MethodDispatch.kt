package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.runtime.isNothing
import com.miruken.runtime.isUnit
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters

class MethodDispatch(val callable: KCallable<*>) {
    val arguments: List<Argument> =
            callable.valueParameters.map { Argument(it) }

    val logicalReturnType: KType
    val returnFlags:       Flags<TypeFlags>
    val owningClass:       KClass<*> =
            callable.instanceParameter?.let {
                it.type.classifier as? KClass<*>
            } ?: throw IllegalArgumentException(
                    "Only instance methods are currently supported")

    init {
        val typeFlags = TypeFlags.parse(returnType)
        logicalReturnType   = typeFlags.second
        returnFlags         = typeFlags.first
    }

    inline val returnType  get() = callable.returnType
    inline val annotations get() = callable.annotations

    val returnsSomething get() =
        !returnType.isUnit && !returnType.isNothing
}