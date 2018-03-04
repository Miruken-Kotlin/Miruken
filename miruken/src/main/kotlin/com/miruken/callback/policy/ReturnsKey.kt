package com.miruken.callback.policy

import com.miruken.callback.Key
import com.miruken.callback.StringKey

object ReturnsKey : ReturnRule {
    override fun matches(method: CallableDispatch) =
            method.returnsSomething

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {
        if (bindingInfo.outKey == null) {
            val dispatch = bindingInfo.dispatcher
            bindingInfo.outKey = dispatch.annotations
                    .firstOrNull { it is Key }
                    ?.let {
                        val key = it as Key
                        StringKey(key.key, key.caseSensitive)
                    } ?: dispatch.logicalReturnType.takeUnless {
                        !bindingInfo.strict &&
                            (dispatch.returnFlags has TypeFlags.PRIMITIVE)
                    } ?: StringKey(dispatch.callable.name)
        }
    }
}