package com.miruken.callback.policy.rules

import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo
import kotlin.reflect.KType

class CallbackArgument(private val callbackType: KType) : ArgumentRule {
    override fun matches(
            argument: Argument,
            context: RuleContext) =
            argument.satisfies(callbackType)

    override fun configure(
            argument: Argument,
            bindingInfo: PolicyMemberBindingInfo
    ) {
        if (bindingInfo.callbackArg == null)
            bindingInfo.callbackArg = argument
    }

    override fun resolve(callback: Any) = callback
}
