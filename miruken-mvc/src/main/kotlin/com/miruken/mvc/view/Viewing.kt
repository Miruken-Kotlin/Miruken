package com.miruken.mvc.view

import com.miruken.mvc.policy.PolicyOwner

interface Viewing : PolicyOwner<ViewPolicy> {

    var viewModel: Any?

    fun display(region: ViewingRegion): ViewingLayer
}

