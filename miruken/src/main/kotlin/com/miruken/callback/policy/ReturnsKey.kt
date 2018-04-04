package com.miruken.callback.policy

import com.miruken.callback.Key
import com.miruken.callback.StringKey
import com.miruken.TypeFlags

object ReturnsKey : ReturnRule {
    override fun matches(method: CallableDispatch) =
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
                    } ?: StringKey(dispatch.callable.name)
        }
    }
}