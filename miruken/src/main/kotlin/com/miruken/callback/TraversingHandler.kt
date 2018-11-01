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
    ): HandleResult {
        return when (callback) {
            !is Composition ->
                handler.handle(axis, callback, callbackType, greedy, composer)
            else -> handler.handle(callback, callbackType, greedy, composer)
        } otherwise {
            super.handle(callback, callbackType, greedy, composer)
        }
    }

    override fun handle(
            axis:         TraversingAxis,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling?
    ): HandleResult =
            handler.handle(axis, callback, callbackType, greedy, composer)
}