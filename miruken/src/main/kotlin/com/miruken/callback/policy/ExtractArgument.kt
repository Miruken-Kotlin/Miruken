package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class ExtractArgument<C, out R: Any>(
        private val resultType: KType,
        private val extract:    (C) -> R
): ArgumentRule {

    override fun matches(parameter: KParameter) : Boolean {
        return isAssignableTo(resultType, parameter.type)
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any) = extract(callback as C)
}