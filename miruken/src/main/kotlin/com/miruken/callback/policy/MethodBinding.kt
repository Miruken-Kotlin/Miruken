package com.miruken.callback.policy

import com.miruken.callback.FilteringProvider
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.runtime.normalize

abstract class MethodBinding(val dispatch: MethodDispatch) {
    private val _filters: MutableList<FilteringProvider> =
            dispatch.annotations
                    .filterIsInstance<FilteringProvider>()
                    .normalize().toMutableList()

    val filters: Collection<FilteringProvider> = _filters

    fun addFilters(vararg providers: FilteringProvider) =
        _filters.addAll(providers)

    abstract fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results:  CollectResultsBlock? = null
    ): HandleResult
}