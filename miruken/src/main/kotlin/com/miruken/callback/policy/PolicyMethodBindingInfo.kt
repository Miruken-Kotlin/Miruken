package com.miruken.callback.policy

data class PolicyMethodBindingInfo(
        val rule:       MethodRule,
        val dispatch:   MethodDispatch,
        val annotation: Annotation
) {
    var inKey:         Any?    = null
    var outKey:        Any?    = null
    var callbackIndex: Int?    = null
    var strict:        Boolean = false
}