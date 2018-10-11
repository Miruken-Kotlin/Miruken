package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class FilterSpecProvider(
        private val specs: Collection<FilterSpec>
): FilteringProvider {
    override val required = specs.any { it.required }

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>> {
        val bundle  = Bundle(true)
        val filters = mutableListOf<Filtering<*,*>>()
        specs.forEach {
            getFilters(filterType, it, binding, bundle, filters)
        }
        composer.handle(bundle)
        return filters
    }

    protected open fun acceptFilterType(
            filterType: KType, binding: MemberBinding) = true

    private fun getFilters(
            filterType: KType,
            filterSpec: FilterSpec,
            binding:    MemberBinding,
            bundle:     Bundle,
            filters:    MutableList<Filtering<*,*>>
    ) {
        val filterClass       = filterSpec.filterClass
        val filterConformance = filterClass.getFilteringInterface()
        val filterImplClasses = mutableSetOf<KClass<out Filtering<*,*>>>()
        val typeBindings = mutableMapOf<KTypeParameter, KType>()
        if (isCompatibleWith(filterConformance, filterType, typeBindings)) {
            val closedFilterType = when (filterConformance) {
                FILTERING_STAR -> filterType
                else -> filterClass.createType(
                        filterClass.typeParameters.map {
                            KTypeProjection.invariant(typeBindings[it]!!)
                        })
            }
            @Suppress("UNCHECKED_CAST")
            if (acceptFilterType(closedFilterType, binding)) {
                val order  = filterSpec.order
                val filter = filterClass.objectInstance
                if (filter != null) return
                if (filterSpec.many) {
                    bundle.add { b -> filters.addAll(
                            b.stop.resolveAll(closedFilterType)
                            .apply {
                                if (filterSpec.required && isEmpty()) {
                                    error("At least one filter is required for '${filterSpec.filterClass}'")
                                }
                            }
                            .asSequence()
                            .filterIsInstance<Filtering<*,*>>()
                            .filter { f ->
                                filterImplClasses.add(f::class).also {
                                    if (it ) f.order = order
                                }
                            }
                            .toList())
                    }
                } else {
                    bundle.add { b ->
                        (b.stop.resolve(closedFilterType) as? Filtering<*,*>)
                            ?.takeIf { f ->
                                filterImplClasses.add(f::class).also {
                                    if (it) f.order = order
                                    filters.add(f)
                                }
                            } ?: if (filterSpec.required) {
                                    error("A filter is required for '${filterSpec.filterClass}'")
                            }
                    }
                }
            }
        }
    }
}
