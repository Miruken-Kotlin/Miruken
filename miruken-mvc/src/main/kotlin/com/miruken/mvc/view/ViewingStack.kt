package com.miruken.mvc.view

interface ViewingStack : ViewingRegion {
    fun pushLayer(): () -> Unit
    fun unwindLayers()
}