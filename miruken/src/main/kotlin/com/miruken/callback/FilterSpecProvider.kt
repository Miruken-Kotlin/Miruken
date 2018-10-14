package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class FilterSpecProvider(
        private val filterSpec: FilterSpec
): FilteringProvider {
    init {
        check (filterSpec.filterby != Filtering::class) {
            "Unspecified filter by is not allowed"
        }
    }

    override val required = filterSpec.required

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>>? {
        val filterClass       = filterSpec.filterby
        val filterConformance = filterClass.getFilteringInterface()
        val typeBindings = mutableMapOf<KTypeParameter, KType>()
        if (isCompatibleWith(filterType, filterConformance, typeBindings)) {
            val closedFilterType = filterClass.createType(
                    filterClass.typeParameters.map {
                        KTypeProjection.invariant(typeBindings[it]!!)
                    })
            @Suppress("UNCHECKED_CAST")
            if (acceptFilterType(closedFilterType, binding)) {
                val filter = filterClass.objectInstance
                    ?: (composer.resolve(closedFilterType)
                            as? Filtering<*,*>)?.apply {
                        filterSpec.order?.also { order = it }
                    }
                return filter?.let { listOf(it) }
            }
        }
        return null
    }

    protected open fun acceptFilterType(
        filterType: KType, binding: MemberBinding) = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FilterSpecProvider
            && other.filterSpec == filterSpec
    }

    override fun hashCode() = filterSpec.hashCode()
}
