package com.miruken.mvc.view

import com.miruken.event.Event
import java.time.Duration

interface ViewLayer : AutoCloseable {
    val transitioned: Event<ViewLayer>
    val closed:       Event<ViewLayer>

    val index: Int

    fun duration(
            duration: Duration,
            complete: (Boolean) -> Unit
    ): () -> Unit
}