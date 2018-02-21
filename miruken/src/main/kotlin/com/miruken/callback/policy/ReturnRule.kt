package com.miruken.callback.policy

import kotlin.reflect.KParameter
import kotlin.reflect.KType

interface ReturnRule {
    fun matches(returnType: KType, parameters: List<KParameter>) : Boolean

    @Suppress("UNCHECKED_CAST")
    fun <R: ReturnRule> getInnerRule() = this as? R
}