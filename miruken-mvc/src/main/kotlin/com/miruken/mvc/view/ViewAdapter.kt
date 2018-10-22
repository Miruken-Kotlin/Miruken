package com.miruken.mvc.view

abstract class ViewAdapter : Viewing {
    override var viewModel: Any? = null

    var layer: ViewingLayer? = null
        protected set
}
