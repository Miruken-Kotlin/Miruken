package com.miruken.callback

interface FilteringProviderFactory {
    fun createProvider(annotation: Annotation): FilteringProvider
}