package com.miruken.mvc

import com.miruken.event.Event
import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingLayer
import java.time.Duration

class TestViewLayer(val view: Viewing) : ViewingLayer {
    override val index: Int = 0

    override val transitioned = Event<ViewingLayer>()

    override val closed = Event<ViewingLayer>()

    override fun duration(
            duration: Duration,
            complete: (Boolean) -> Unit
    ) = {}

    override fun close() {
    }
}