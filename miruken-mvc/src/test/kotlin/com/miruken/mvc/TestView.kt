package com.miruken.mvc

import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingLayer
import com.miruken.mvc.view.ViewingRegion

class TestView : Viewing {
    override var viewModel: Any? = null

    override fun display(region: ViewingRegion) = region.show(this)
}