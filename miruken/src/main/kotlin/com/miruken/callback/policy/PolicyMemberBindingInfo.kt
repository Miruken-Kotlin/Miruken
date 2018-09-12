package com.miruken.callback.policy

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