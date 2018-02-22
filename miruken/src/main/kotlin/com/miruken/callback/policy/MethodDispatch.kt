package com.miruken.callback.policy

import kotlin.reflect.KCallable
import kotlin.reflect.full.valueParameters

class MethodDispatch(val callable: KCallable<*>) {
    val arguments: List<Argument> =
            callable.valueParameters.map { Argument(it) }

    inline val returnType  get() = callable.returnType
    inline val annotations get() = callable.annotations
}