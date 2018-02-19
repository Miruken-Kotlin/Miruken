package com.miruken.callback

import com.miruken.graph.TraversingAxis

interface HandlingAxis : Handling {
    fun handle(
            axis:     TraversingAxis,
            callback: Any,
            greedy:   Boolean = false,
            composer: Handling? = null
    ) : HandleResult
}