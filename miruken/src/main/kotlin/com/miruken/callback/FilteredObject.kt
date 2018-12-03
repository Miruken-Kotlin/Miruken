package com.miruken.callback

import kotlin.reflect.KAnnotatedElement

abstract class FilteredObject() : Filtered {
    private val _filters by lazy {
        mutableSetOf<FilteringProvider>()
    }

    constructor(annotatedElement: KAnnotatedElement) : this() {
        addFilters(annotatedElement.getFilterProviders())
    }

    constructor(filters: Collection<FilteringProvider>) : this() {
        addFilters(filters)
    }

    final override val filters: Collection<FilteringProvider>
        get() = _filters

    final override fun addFilters(vararg providers: FilteringProvider) {
        _filters.addAll(providers)
    }

    final override fun addFilters(providers: Collection<FilteringProvider>) {
        _filters.addAll(providers)
    }
}