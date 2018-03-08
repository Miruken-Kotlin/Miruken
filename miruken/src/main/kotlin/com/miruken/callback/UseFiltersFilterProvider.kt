package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.isCompatibleWith
import com.miruken.runtime.isOpenGeneric
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class UseFiltersFilterProvider(
        val useFilters: List<UseFilter<*>>
): FilteringProvider {
    override fun getFilters(
            binding:    MethodBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>> {
        return if (filterType.isOpenGeneric) {
            emptyList()
        } else {
            useFilters.flatMap {
                getFilters(filterType, it, binding, composer)
            }.filter { useFilterInstance(it, binding) }
        }
    }

    protected open fun acceptFilterType(
            filterType: KType, binding: MethodBinding) = true

    protected open fun useFilterInstance(
            filter: Filtering<*,*>, binding: MethodBinding) = true

    private fun getFilters(
            filterType: KType,
            useFilter:  UseFilter<*>,
            binding:    MethodBinding,
            composer:   Handling
    ):List<Filtering<*,*>> {
        val filterClass       = useFilter.filterClass
        val filterConformance = filterClass.getFilteringInterface()
        val typeBindings      = mutableMapOf<KTypeParameter, KType>()
        if (isCompatibleWith(filterType, filterConformance, typeBindings)) {
            val closedFilterType = filterClass.createType(
                    filterClass.typeParameters.map {
                            KTypeProjection.invariant(typeBindings[it]!!)
                    })
            if (acceptFilterType(closedFilterType, binding)) {
                @Suppress("UNCHECKED_CAST")
                val filters = if (useFilter.many) {
                    composer.stop.resolveAll(closedFilterType)
                            as List<Filtering<*,*>>
                } else {
                    (composer.stop.resolve(closedFilterType)
                            as? Filtering<*,*>)?.let { listOf(it) }
                            ?: emptyList()
                }
                val order = useFilter.order
                if (order >= 0) {
                    for (filter in filters) filter.order = order
                }
                return filters
            }
        }
        return emptyList()
    }
}