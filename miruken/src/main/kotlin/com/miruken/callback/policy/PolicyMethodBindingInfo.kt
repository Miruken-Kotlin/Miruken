package com.miruken.callback.policy

data class PolicyMethodBindingInfo(
        val rule:       MethodRule,
        val dispatcher: MethodDispatch,
        val annotation: Annotation
) {
    var inKey:            Any?      = null
    var outKey:           Any?      = null
    var callbackArgument: Argument? = null
    var strict:           Boolean   = false
}