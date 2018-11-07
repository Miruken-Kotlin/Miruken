package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.graph.TraversingAxis
import com.miruken.typeOf

interface HandlingAxis : Handling {
    fun handle(
            axis:         TraversingAxis,
            callback:     Any,
            callbackType: TypeReference?,
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