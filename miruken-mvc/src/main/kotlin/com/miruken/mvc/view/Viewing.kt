package com.miruken.mvc.view

interface Viewing {

    var viewModel: Any?

    fun display(region: ViewingRegion): ViewingLayer
}

