package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class TargetArgument<in C, out R: Any>(
        private val callbackType: KType,
        private val targetType:   KType,
        private val target:       (C) -> R
) : ArgumentRule {

    override fun matches(argument: Argument) =
            !argument.satisfies(callbackType) &&  //CallbackArgument matches
                    argument.satisfies(targetType)

    override fun configure(
            argument:    Argument,
            bindingInfo: PolicyMethodBindingInfo)
    {
        if (bindingInfo.callbackArgument == null) {
            bindingInfo.callbackArgument = argument
            bindingInfo.inKey =
                    if ((argument.flags - TypeFlags.OPTIONAL) == TypeFlags.OPEN)
                        argument.logicalType
                    else argument.parameterType
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any) =
            if (isAssignableTo(callbackType, callback))
                target(callback as C) else callback
}
