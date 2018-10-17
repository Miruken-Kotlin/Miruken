package com.miruken.mvc.view

abstract class ViewAdapter : Viewing {
    @Suppress("LeakingThis")
    override var policy = ViewPolicy(this)

    override var viewModel: Any? = null

    var layer: ViewingLayer? = null
        protected set
}
