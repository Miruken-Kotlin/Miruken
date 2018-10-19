package com.miruken.mvc.view

interface ViewingStack : ViewingRegion {
    fun pushLayer(): ViewingLayer
    fun unwindLayers()
}