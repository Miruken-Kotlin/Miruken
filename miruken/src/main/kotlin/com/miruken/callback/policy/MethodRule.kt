package com.miruken.callback.policy

class MethodRule(vararg val argumentRules: ArgumentRule) {

    var returnRule: ReturnRule? = null
        private set

    constructor(
            returnRule: ReturnRule,
            vararg argumentRules: ArgumentRule
    ) : this(*argumentRules) {
        this.returnRule = returnRule
    }

    fun matches(method: CallableDispatch) : Boolean {
        val arguments = method.arguments
        if (arguments.size < argumentRules.size ||
                !arguments.zip(argumentRules) { arg, argRule ->
                    argRule.matches(arg) }.all { it })
            return false
        return returnRule?.matches(method) ?: true
    }

    fun bind(policy:     CallbackPolicy,
             dispatch:   CallableDispatch,
             annotation: Annotation): PolicyMethodBinding {
        val strict      = policy.strict || dispatch.strict
        val bindingInfo = PolicyMethodBindingInfo(
                this, dispatch, annotation, strict)
        returnRule?.configure(bindingInfo)
        argumentRules.zip(dispatch.arguments) { argRule, arg ->
            argRule.configure(arg, bindingInfo)
        }
        return policy.bindMethod(bindingInfo)
    }

    fun resolveArguments(callback: Any) : Array<Any?> =
            argumentRules.map { it.resolve(callback) }
                    .toTypedArray()
}