package com.miruken.callback.policy.rules

import com.miruken.TypeReference
import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo

class CallbackArgument(
        private val callbackType: TypeReference
) : ArgumentRule {
    override fun matches(argument: Argument, context: RuleContext) =
            argument.satisfies(callbackType)

    override fun configure(argument: Argument, bindingInfo: PolicyMemberBindingInfo) {
        if (bindingInfo.callbackArg == null)
            bindingInfo.callbackArg = argument
    }

    override fun resolve(callback: Any) = callback
}
