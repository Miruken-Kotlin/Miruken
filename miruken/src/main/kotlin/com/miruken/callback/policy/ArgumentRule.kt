package com.miruken.callback.policy

import kotlin.reflect.KParameter

interface ArgumentRule {
    fun matches(parameter: KParameter) : Boolean

    fun resolve(callback: Any): Any
}