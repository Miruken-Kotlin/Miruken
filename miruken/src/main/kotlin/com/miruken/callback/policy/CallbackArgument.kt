package com.miruken.callback.policy

import kotlin.reflect.KType

class CallbackArgument(private val callbackType: KType) : ArgumentRule {
    override fun matches(argument: Argument) =
            argument.satisfies(callbackType)

    override fun configure(
            argument:    Argument,
            bindingInfo: PolicyMethodBindingInfo
    ) {
        if (bindingInfo.callbackArgument == null)
            bindingInfo.callbackArgument = argument
    }

    override fun resolve(callback: Any) = callback
}
