package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.allInterfaces
import com.miruken.runtime.isOpenGeneric
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class UseFiltersFilterProvider(
        val useFilters: List<UseFilter<*>>
): FilteringProvider {
    override fun getFilters(
            binding:    MethodBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>> {
        if (filterType.isOpenGeneric)
            return emptyList()
        return emptyList()
    }

    /*
    private fun getFilterKey(
            filterType:          KType,
            proposedFilterClass: KClass<*>
    ):Any? {
        val typeParams = proposedFilterClass.typeParameters
        if (typeParams.isEmpty()) return proposedFilterClass
        if (typeParams.size > 2) return null
        if (proposedFilterClass == Filtering::class)
            return proposedFilterClass.createType(
                    listOf(KTypeProjection.invariant(callbackType),
                           KTypeProjection.invariant(logicalResultType)))
        return proposedFilterClass.allInterfaces
                .firstOrNull { it.classifier == Filtering::class }?.let {
                    val cbOpen  = it.arguments[0].type!!.isOpenGeneric
                    val resOpen = it.arguments[1].type!!.isOpenGeneric
                    val typeArgs = when {
                        cbOpen && resOpen ->
                            listOf(callbackType, logicalResultType)
                        cbOpen -> listOf(callbackType)
                        resOpen -> listOf(logicalResultType)
                        else -> emptyList()
                    }
                    return proposedFilterClass.createType(
                            typeArgs.map(KTypeProjection.Companion::invariant)
                    )
                }
    }
    */
}