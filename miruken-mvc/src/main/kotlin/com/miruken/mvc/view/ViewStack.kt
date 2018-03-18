package com.miruken.mvc.view

interface ViewStack : ViewRegion {
    fun pushLayer(): () -> Unit
    fun unwindLayers()
}