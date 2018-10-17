package com.miruken.mvc.view

import com.miruken.event.Event
import java.time.Duration

interface ViewingLayer : AutoCloseable {
    val transitioned: Event<ViewingLayer>
    val closed:       Event<ViewingLayer>

    val index: Int

    fun duration(
            duration: Duration,
            complete: (Boolean) -> Unit
    ): () -> Unit
}