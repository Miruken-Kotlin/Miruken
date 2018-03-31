package com.miruken.callback.policy

import kotlin.reflect.KType

class ExtractArgument<C, out R: Any>(
        private val extractType: KType,
        private val extract:    C.() -> R
): ArgumentRule {

    override fun matches(argument: Argument) =
            argument.satisfies(extractType)

    @Suppress("UNCHECKED_CAST")
    override fun resolve(callback: Any) = extract(callback as C)
}