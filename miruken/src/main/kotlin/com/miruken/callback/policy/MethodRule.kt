package com.miruken.callback.policy

typealias MethodBinderBlock = (PolicyMethodBindingInfo) -> PolicyMethodBinding

class MethodRule(
        val methodBinder: MethodBinderBlock,
        vararg val argumentRules: ArgumentRule) {

    var returnRule: ReturnRule? = null
        private set

    fun matches(method: MethodDispatch) : Boolean {
        val arguments = method.arguments
        if (arguments.size < argumentRules.size ||
                !arguments.zip(argumentRules) { arg, argRule ->
                    argRule.matches(arg) }.all { it })
            return false
        return returnRule?.matches(method) ?: true
    }

    infix fun returns(returnRule: ReturnRule) {
        this.returnRule = returnRule
    }

    fun bind(dispatch: MethodDispatch,
             annotation: Annotation): PolicyMethodBinding {
        val bindingInfo = PolicyMethodBindingInfo(this, dispatch, annotation)
        returnRule?.configure(bindingInfo)
        argumentRules.zip(dispatch.arguments) { argRule, arg ->
            argRule.configure(arg, bindingInfo)
        }
        return methodBinder(bindingInfo)
    }

    fun resolveArguments(callback: Any) : List<Any> =
            argumentRules.map { it.resolve(callback) }
}