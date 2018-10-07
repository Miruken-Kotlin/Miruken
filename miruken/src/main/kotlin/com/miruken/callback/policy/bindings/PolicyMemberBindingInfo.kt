package com.miruken.callback.policy.bindings

import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.rules.MethodRule

data class PolicyMemberBindingInfo(
        val rule:       MethodRule?,
        val dispatcher: CallableDispatch,
        val annotation: Annotation,
        val strict:     Boolean
) {
    var inKey:       Any?      = null
    var outKey:      Any?      = null
    var callbackArg: Argument? = null
}