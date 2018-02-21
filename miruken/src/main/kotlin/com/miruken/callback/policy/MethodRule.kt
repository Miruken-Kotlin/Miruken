package com.miruken.callback.policy

import kotlin.reflect.KCallable
import kotlin.reflect.full.valueParameters

typealias MethodBinderBlock = (PolicyMethodBindingInfo) -> PolicyMethodBinding

class MethodRule(
        val methodBinder: MethodBinderBlock,
        vararg val arguments: ArgumentRule) {

    var returnRule: ReturnRule? = null
        private set

    constructor(
            methodBinder: MethodBinderBlock,
            returnRule: ReturnRule,
            vararg arguments: ArgumentRule
    ) : this(methodBinder, *arguments) {
        this.returnRule = returnRule
    }

    fun matches(method: KCallable<*>) : Boolean {
        val parameters = method.valueParameters
        if (parameters.size < arguments.size ||
                !parameters.zip(arguments) { param, arg ->
                    arg.matches(param) }.all { it })
            return false
        return returnRule?.matches(method.returnType, parameters) ?: true
    }

    fun bind(
            dispatch: MethodDispatch,
            annotation: Annotation
    ): PolicyMethodBinding {
        val bindingInfo = PolicyMethodBindingInfo(this, dispatch, annotation)
        returnRule?.configure(bindingInfo)
        arguments.zip(dispatch.callable.valueParameters) { arg, param ->
            arg.configure(param, bindingInfo)
        }
        return methodBinder(bindingInfo)
    }

    fun resolveArguments(callback: Any) : List<Any> =
            arguments.map { it.resolve(callback) }
}