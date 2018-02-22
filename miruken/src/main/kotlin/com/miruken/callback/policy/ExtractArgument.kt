package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class ExtractArgument<C, out R: Any>(
        private val resultType: KType,
        private val extract:    (C) -> R
): ArgumentRule {

    override fun matches(argument: Argument) : Boolean {
        return isAssignableTo(resultType, argument.parameterType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any) = extract(callback as C)
}