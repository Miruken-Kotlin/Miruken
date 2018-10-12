package com.miruken.callback

import com.miruken.OrderedComparator
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.runtime.getMetaAnnotations
import com.miruken.runtime.normalize
import java.lang.reflect.AnnotatedElement
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

fun Handling.skipFilters(skip: Boolean = true) =
        withOptions(FilterOptions().apply {
            skipFilters = skip
        })

fun Handling.enableFilters(enable: Boolean = true) =
        withOptions(FilterOptions().apply {
            skipFilters = !enable
        })

fun Handling.withFilters(vararg filters: Filtering<*,*>) =
        withOptions(FilterOptions().apply {
            providers = listOf(FilterInstanceProvider(*filters))
        })

fun Handling.withFilterProviders(vararg providers: FilteringProvider) =
        withOptions(FilterOptions().apply {
            this.providers = providers.toList()
        })

fun Handling.getOrderedFilters(
        filterType:      KType,
        binding:         MemberBinding,
        filterProviders: Sequence<Collection<FilteringProvider>>
): List<Pair<Filtering<*,*>, FilteringProvider>>? {
    val options   = getOptions(FilterOptions())
    val providers = filterProviders.flatten() +
            (options?.providers ?: emptyList())

    val handler = when (options?.skipFilters) {
        true -> return when {
            binding.filters.any { it.required } ||
            providers.any       { it.required } -> null
            else -> emptyList()
        }
        null -> when (binding.skipFilters) {
            true -> return emptyList()
            else -> skipFilters()
        }
        else -> this
    }
    return providers.toList().flatMap {
        it.getFilters(binding, filterType, handler)
                .map { filter -> filter to it }
    }.sortedWith(FilterComparator)
}

object FilterComparator :
        Comparator<Pair<Filtering<*,*>, FilteringProvider>> {
    override fun compare(
            o1: Pair<Filtering<*, *>, FilteringProvider>?,
            o2: Pair<Filtering<*, *>, FilteringProvider>?
    ) = OrderedComparator.compare(o1?.first, o2?.first)
}

fun KAnnotatedElement.getFilterProviders() =
        (getMetaAnnotations<UseFilterProvider>()
                .flatMap { it.second }
                .asSequence()
                .mapNotNull {
                    it.provideBy.objectInstance
                } +
         getMetaAnnotations<UseFilterProviderFactory>()
                .flatMap {
                    it.second.mapNotNull { f ->
                        f.createBy.objectInstance
                                ?.createProvider(it.first) }
                } +
        (getMetaAnnotations<UseFilter>()
                .flatMap { it.second }
                .map(::createSpec)
                .takeIf { it.isNotEmpty() }?.let {
                    sequenceOf(FilterSpecProvider(it))
                } ?: emptySequence())
        ).toList()
         .normalize()


fun AnnotatedElement.getFilterProviders() =
        (getMetaAnnotations<UseFilterProvider>()
                .flatMap { it.second }
                .asSequence()
                .mapNotNull {
                    it.provideBy.objectInstance
                } +
         getMetaAnnotations<UseFilterProviderFactory>()
                        .flatMap {
                            it.second.mapNotNull { f ->
                                f.createBy.objectInstance
                                        ?.createProvider(it.first) }
                        } +
        (getMetaAnnotations<UseFilter>()
                        .flatMap { it.second }
                        .map(::createSpec)
                        .takeIf { it.isNotEmpty() }?.let {
                            sequenceOf(FilterSpecProvider(it))
                        } ?: emptySequence())
        ).toList()
         .normalize()


private fun createSpec(useFilter: UseFilter) = FilterSpec(
        useFilter.filterBy,
        useFilter.many,
        useFilter.order.takeIf { it >=0 },
        useFilter.required)