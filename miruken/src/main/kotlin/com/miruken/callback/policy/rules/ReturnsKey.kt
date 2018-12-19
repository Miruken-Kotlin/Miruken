package com.miruken.callback.policy.rules

import com.miruken.TypeFlags
import com.miruken.callback.Key
import com.miruken.callback.StringKey
import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo

object ReturnsKey : ReturnRule {
    override fun matches(method:  CallableDispatch, context: RuleContext) =
            method.returnsSomething

    override fun configure(bindingInfo: PolicyMemberBindingInfo) {
        if (bindingInfo.outKey == null) {
            val dispatch = bindingInfo.dispatcher
            bindingInfo.outKey = dispatch.annotations
                    .firstOrNull { it is Key }
                    ?.let {
                        val key = it as Key
                        StringKey(key.key, key.caseSensitive)
                    } ?: dispatch.returnInfo.componentType.takeUnless {
                        !bindingInfo.strict &&
                            (dispatch.returnInfo.flags has TypeFlags.PRIMITIVE)
                    } ?: StringKey(getCanonicalName(dispatch.callable.name))
        }
    }

    private fun getCanonicalName(name: String) = when {
        name.startsWith("<get-") ->
            name.removeSurrounding("<get-", ">")
        name.startsWith("<set-") ->
            name.removeSurrounding("<get-", ">")
        else -> name
    }
}