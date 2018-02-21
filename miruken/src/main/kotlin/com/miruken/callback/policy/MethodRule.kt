package com.miruken.callback.policy

import java.lang.IllegalStateException
import kotlin.reflect.KCallable
import kotlin.reflect.full.valueParameters

class MethodRule(vararg val arguments: ArgumentRule) {
    var returnRule: ReturnRule? = null
        private set

    constructor(
            returnRule: ReturnRule,
            vararg arguments: ArgumentRule
    ) : this(*arguments) {
        this.returnRule = returnRule
    }

    fun matches(method: KCallable<*>) : Boolean {
        val parameters = method.valueParameters
        if (parameters.size < arguments.size ||
                !parameters.zip(arguments) { param, arg ->
                    arg.matches(param) }.all { it })
            return false
        return returnRule?.let {
            it.matches(method.returnType, parameters) ||
                    throw IllegalStateException("$method satisfied the arguments but rejected the return")
        } ?: true
    }

    fun resolveArguments(callback: Any) : List<Any> =
            arguments.map { it.resolve(callback) }
}