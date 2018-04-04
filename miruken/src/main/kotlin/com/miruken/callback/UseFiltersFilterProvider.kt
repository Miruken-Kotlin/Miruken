package com.miruken.callback

import com.miruken.callback.policy.MemberBinding
import com.miruken.concurrent.Promise
import com.miruken.runtime.PROMISE_TYPE
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure

open class UseFiltersFilterProvider(
        private val useFilters: List<UseFilter>
): FilteringProvider {
    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>> {
        val bundle  = Bundle(true)
        val filters = mutableListOf<Filtering<*,*>>()
        useFilters.forEach {
            getFilters(filterType, it, binding, bundle, filters)
        }
        composer.handle(bundle)
        return filters
    }

    protected open fun acceptFilterType(
            filterType: KType, binding: MemberBinding) = true

    protected open fun useFilterInstance(
            filter: Filtering<*,*>, binding: MemberBinding) = true

    private fun getFilters(
            filterType: KType,
            useFilter:  UseFilter,
            binding:    MemberBinding,
            bundle:     Bundle,
            filters:    MutableList<Filtering<*,*>>
    ) {
        val filterClass       = useFilter.filterClass
        val filterConformance = filterClass.getFilteringInterface()
        val filterImplClasses = mutableSetOf<KClass<out Filtering<*,*>>>()
        getCompatibleFilters(filterType).forEach { cf ->
            val typeBindings = mutableMapOf<KTypeParameter, KType>()
            if (isCompatibleWith(filterConformance, cf.first, typeBindings)) {
                val closedFilterType = when (filterConformance) {
                    FILTERING_STAR -> cf.first
                    else -> filterClass.createType(
                            filterClass.typeParameters.map {
                                KTypeProjection.invariant(typeBindings[it]!!)
                            })
                }
                @Suppress("UNCHECKED_CAST")
                if (acceptFilterType(closedFilterType, binding)) {
                    val order  = useFilter.order
                    val filter = filterClass.objectInstance
                    if (filter != null) {
                        if (useFilterInstance(filter, binding)) {
                            filters.add(filter)
                        }
                    } else if (useFilter.many) {
                        bundle.add {
                            filters.addAll(it.stop.resolveAll(closedFilterType)
                                .filterIsInstance<Filtering<*,*>>()
                                .filter { f ->
                                    filterImplClasses.add(f::class) &&
                                    useFilterInstance(f, binding).also {
                                        if (it && order >= 0) f.order = order
                                    }
                                }.map(cf.second))
                        }
                    } else {
                        bundle.add {
                            (it.stop.resolve(closedFilterType) as? Filtering<*,*>)
                                ?.takeIf { f ->
                                    filterImplClasses.add(f::class) &&
                                    useFilterInstance(f, binding).also {
                                        if (it && order >= 0) f.order = order
                                        filters.add(cf.second(f))
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun getCompatibleFilters(
            filterType: KType
    ): List<Pair<KType, (Filtering<*,*>) -> Filtering<*,*>>> {
        val filterClass  = filterType.jvmErasure
        val callbackType = filterType.arguments[0]
        val returnType   = filterType.arguments[1].type!!
        return when {
            returnType.isSubtypeOf(PROMISE_TYPE) ->
                listOf(filterType to { f -> f },
                    filterClass.createType(listOf(
                            callbackType, returnType.arguments[0])) to { f ->
                        AsyncFilterAdapter(f) })
            else -> listOf(filterType to { f -> f },
                    filterClass.createType(listOf(
                            callbackType, KTypeProjection.invariant(
                    Promise::class.createType(listOf(
                            KTypeProjection.invariant(returnType)))))) to { f ->
                        @Suppress("UNCHECKED_CAST")
                        SyncFilterAdapter(f as Filtering<*,Promise<*>>)
                    })
        }
    }
}