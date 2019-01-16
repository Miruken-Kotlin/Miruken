package com.miruken.callback.policy.rules

import com.miruken.TypeReference
import com.miruken.callback.policy.Argument

class ExtractArgument<C, out R: Any>(
        private val extractType: TypeReference,
        private val extract:     C.() -> R
): ArgumentRule {

    override fun matches(argument: Argument, context: RuleContext) =
            argument.satisfies(extractType)

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any) = extract(callback as C)
}