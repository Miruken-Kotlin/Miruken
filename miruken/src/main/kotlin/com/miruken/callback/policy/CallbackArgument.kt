package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.isNothing
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class CallbackArgument(private val callbackType: KType) : ArgumentRule {
    override fun matches(parameter: KParameter) : Boolean {
        return !parameter.type.isNothing && isAssignableTo(callbackType,
                parameter.type.withNullability(false))
    }

    override fun resolve(callback: Any): Any = callback
}
