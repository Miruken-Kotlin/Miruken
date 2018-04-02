package com.miruken.callback

import com.miruken.graph.TraversingAxis
import com.miruken.typeOf
import kotlin.reflect.KType

interface HandlingAxis : Handling {
    fun handle(
            axis:         TraversingAxis,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean = false,
            composer:     Handling? = null
    ) : HandleResult
}

inline fun <reified T: Any> HandlingAxis.handle(
        axis:     TraversingAxis,
        callback: T,
        greedy:   Boolean   = false,
        composer: Handling? = null
) = handle(axis, callback, typeOf<T>(), greedy, composer)