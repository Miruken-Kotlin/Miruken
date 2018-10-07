package com.miruken.callback.policy.rules

import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo

class MethodRule(vararg val argumentRules: ArgumentRule) {

    var returnRule: ReturnRule? = null
        private set

    constructor(
            returnRule: ReturnRule,
            vararg argumentRules: ArgumentRule
    ) : this(*argumentRules) {
        this.returnRule = returnRule
    }

    fun matches(method: CallableDispatch): Boolean {
        val context   = RuleContext()
        val arguments = method.arguments
        return returnRule?.matches(method, context) != false &&
            arguments.size >= argumentRules.size &&
                arguments.zip(argumentRules) { arg, argRule ->
                    argRule.matches(arg, context) }.all { it }
    }

    fun bind(policy: CallbackPolicy,
             dispatch: CallableDispatch,
             annotation: Annotation): PolicyMemberBinding {
        val strict      = policy.strict || dispatch.strict
        val bindingInfo = PolicyMemberBindingInfo(
                this, dispatch, annotation, strict)
        returnRule?.configure(bindingInfo)
        argumentRules.zip(dispatch.arguments) { argRule, arg ->
            argRule.configure(arg, bindingInfo)
        }
        return policy.bindMethod(bindingInfo)
    }

    fun resolveArguments(callback: Any) =
            argumentRules.map { it.resolve(callback) }
                    .toTypedArray()
}