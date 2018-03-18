package com.miruken.mvc.view

import com.miruken.mvc.policy.PolicyOwner

interface View : PolicyOwner<ViewPolicy> {

    var viewModel: Any?

    fun display(region: ViewRegion): ViewLayer
}

