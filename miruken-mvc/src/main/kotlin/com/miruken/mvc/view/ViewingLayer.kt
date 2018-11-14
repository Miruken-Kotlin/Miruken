package com.miruken.mvc.view

import com.miruken.event.Event

interface ViewingLayer : AutoCloseable {
    val index:        Int
    val transitioned: Event<ViewingLayer>
    val disposed:     Event<ViewingLayer>

    fun duration(durationMillis: Int, done: (Boolean) -> Unit): () -> Unit
}