package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling

class PolicyMethodBinding(
        val policy:  CallbackPolicy,
        bindingInfo: PolicyMethodBindingInfo
) : MethodBinding(bindingInfo.dispatch) {

    val rule             = bindingInfo.rule
    val callbackArgument = bindingInfo.callbackArgument
    val key              = policy.createKey(bindingInfo)

    override fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results:  CollectResultsBlock?
    ): HandleResult {
        TODO("not implemented")
    }
}