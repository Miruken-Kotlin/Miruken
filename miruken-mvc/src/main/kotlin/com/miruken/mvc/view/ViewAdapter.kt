package com.miruken.mvc.view

abstract class ViewAdapter : View {
    @Suppress("LeakingThis")
    override var policy = ViewPolicy(this)

    override var viewModel: Any? = null

    var layer: ViewLayer? = null
        protected set
}
