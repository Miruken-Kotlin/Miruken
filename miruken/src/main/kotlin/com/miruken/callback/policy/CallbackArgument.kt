package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.isNothing
import kotlin.reflect.KType

class CallbackArgument(private val callbackType: KType) : ArgumentRule {
    override fun matches(argument: Argument) : Boolean {
        val logicalType = argument.logicalType
        return !logicalType.isNothing &&
                isAssignableTo(callbackType, logicalType)
    }

    override fun configure(
            argument:    Argument,
            bindingInfo: PolicyMethodBindingInfo)
    {
        bindingInfo.callbackArgument = argument
    }

    override fun resolve(callback: Any): Any = callback
}
