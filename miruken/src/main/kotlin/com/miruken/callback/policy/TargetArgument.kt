package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.isNothing
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class TargetArgument<in C, out R: Any>(
        private val callbackType: KType,
        private val targetType:   KType,
        private val target:       (C) -> R
) : ArgumentRule {

    override fun matches(parameter: KParameter) : Boolean {
        return !parameter.type.isNothing &&
                isAssignableTo(targetType,
                        parameter.type.withNullability(false))
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any): Any =
            if (isAssignableTo(callbackType, callback))
                target(callback as C) else callback
}
