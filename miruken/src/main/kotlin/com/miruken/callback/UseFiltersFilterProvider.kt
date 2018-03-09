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
    ): List<Filtering<Any,Any?>> {
        if (!filterType.isOpenGeneric) {
            return emptyList()
        }
        val bundle  = Bundle(true)
        val filters = mutableListOf<Filtering<Any,Any?>>()
        useFilters.forEach {
            getFilters(filterType, it, binding, bundle, filters)
        }
        composer.handle(bundle)
        return filters
    }

    protected open fun acceptFilterType(
            filterType: KType, binding: MethodBinding) = true

    protected open fun useFilterInstance(
            filter: Filtering<Any,Any?>, binding: MethodBinding) = true

    private fun getFilters(
            filterType: KType,
            useFilter:  UseFilter<*>,
            binding:    MethodBinding,
            bundle:     Bundle,
            filters:    MutableList<Filtering<Any,Any?>>
    ) {
        val filterClass = useFilter.filterClass
        val filterConformance = filterClass.getFilteringInterface()
        val typeBindings      = mutableMapOf<KTypeParameter, KType>()
        if (isCompatibleWith(filterType, filterConformance, typeBindings)) {
            val closedFilterType = filterClass.createType(
                    filterClass.typeParameters.map {
                            KTypeProjection.invariant(typeBindings[it]!!)
                    })
            @Suppress("UNCHECKED_CAST")
            if (acceptFilterType(closedFilterType, binding)) {
                val order = useFilter.order
                val filter = filterClass.objectInstance as? Filtering<Any,Any?>
                if (filter != null) {
                    if (useFilterInstance(filter, binding)) {
                        filters.add(filter)
                    }
                } else if (useFilter.many) {
                    bundle.add {
                        filters.addAll(
                            it.stop.resolveAll(closedFilterType)
                                .filterIsInstance<Filtering<Any,Any?>>()
                                .filter { f ->
                                    useFilterInstance(f, binding).also {
                                        if (it && order >= 0) f.order = order
                                    }
                                })
                    }
                } else {
                    bundle.add {
                        (it.stop.resolve(closedFilterType)
                                as? Filtering<Any,Any?>)?.takeIf { f ->
                            useFilterInstance(f, binding).also {
                                if (it && order >= 0) f.order = order
                                filters.add(f)
                            }
                        }
                    }
                }
            }
        }
    }
}