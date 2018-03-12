package com.miruken.callback.policy

data class PolicyMethodBindingInfo(
        val rule:       MethodRule,
        val dispatcher: CallableDispatch,
        val annotation: Annotation,
        val strict:     Boolean
) {
    var inKey:       Any?      = null
    var outKey:      Any?      = null
    var callbackArg: Argument? = null
}