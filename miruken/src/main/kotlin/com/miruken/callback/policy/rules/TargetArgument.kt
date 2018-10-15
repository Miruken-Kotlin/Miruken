package com.miruken.callback.policy.rules

import com.miruken.TypeFlags
import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo
import kotlin.reflect.KType

class TargetArgument<in C, out R: Any>(
        private val callbackType: KType,
        private val targetType:   KType,
        private val target:       (C) -> R
) : ArgumentRule {

    override fun matches(
            argument: Argument,
            context: RuleContext
    ) = !argument.satisfies(callbackType) &&  // CallbackArgument matches
                    argument.satisfies(targetType)

    override fun configure(
            argument: Argument,
            bindingInfo: PolicyMemberBindingInfo)
    {
        if (bindingInfo.callbackArg == null) {
            bindingInfo.callbackArg = argument
            bindingInfo.inKey = argument.parameterType.takeUnless {
                (argument.typeInfo.flags - TypeFlags.OPTIONAL) == TypeFlags.OPEN
            } ?: argument.typeInfo.logicalType
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any) = target(callback as C)
}
