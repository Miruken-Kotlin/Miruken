package com.miruken.mvc.view

interface ViewStack : ViewingRegion {
    fun pushLayer(): () -> Unit
    fun unwindLayers()
}