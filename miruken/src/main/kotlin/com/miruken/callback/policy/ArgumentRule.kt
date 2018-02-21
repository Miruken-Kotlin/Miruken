package com.miruken.callback.policy

import kotlin.reflect.KParameter

interface ArgumentRule {
    fun matches(parameter: KParameter) : Boolean

    fun configure(
            parameter:   KParameter,
            bindingInfo: PolicyMethodBindingInfo) {}

    fun resolve(callback: Any): Any
}