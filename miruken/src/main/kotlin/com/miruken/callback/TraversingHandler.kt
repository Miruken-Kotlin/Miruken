package com.miruken.callback

import com.miruken.graph.TraversingAxis
import kotlin.reflect.KType

class TraversingHandler(
        val handler: HandlingAxis,
        val axis:    TraversingAxis
) : Handler(), HandlingAxis {

    override fun handle(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling?
    ) = handler.handle(axis, callback, callbackType, greedy, composer)

    override fun handle(
            axis:         TraversingAxis,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling?
    ) = handler.handle(axis, callback, callbackType, greedy, composer)
}