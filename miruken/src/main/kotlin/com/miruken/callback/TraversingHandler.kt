package com.miruken.callback

import com.miruken.graph.TraversingAxis

class TraversingHandler(
        val handler: HandlingAxis,
        val axis:    TraversingAxis
) : Handler(), HandlingAxis {

    override fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: Handling?
    ): HandleResult {
        return when (callback) {
            is Composition ->
                handler.handle(axis, callback, greedy, composer)
            else -> handler.handle(callback, greedy, composer)
        } otherwise {
            super.handle(callback, greedy, composer)
        }
    }

    override fun handle(
            axis:     TraversingAxis,
            callback: Any,
            greedy:   Boolean,
            composer: Handling?
    ): HandleResult =
            handler.handle(axis, callback, greedy, composer)
}