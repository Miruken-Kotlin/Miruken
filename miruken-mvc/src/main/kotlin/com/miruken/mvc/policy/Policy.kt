package com.miruken.mvc.policy

interface Policy {
    fun track(): Policy

    fun retain(): Policy

    val isTracked: Boolean

    var parent: Policy?

    val dependencies: List<Policy>

    fun isDependency(dependency: Policy): Boolean

    fun addDependency(dependency: Policy): Policy

    fun removeDependency(dependency: Policy): Policy

    fun onRelease(onRelease: () -> Unit): () -> Unit

    fun release()
}