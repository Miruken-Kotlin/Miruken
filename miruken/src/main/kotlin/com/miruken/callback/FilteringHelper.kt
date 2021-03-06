package com.miruken.callback

import com.miruken.OrderedComparator
import com.miruken.TypeReference
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

fun Handling.withFilters(vararg providers: FilteringProvider) =
        withOptions(FilterOptions().apply {
            this.providers = providers.toList()
        })

fun Handling.withFilters(vararg specs: FilterSpec) =
        withOptions(FilterOptions().apply {
            this.providers = specs.map {
                FilterSpecProvider(it)
            }
        })

fun Handling.getOrderedFilters(
        filterType:      KType,
        binding:         MemberBinding,
        callback:        Any,
        callbackType:    TypeReference?,
        filterProviders: Sequence<Collection<FilteringProvider>>
): List<Pair<Filtering<*,*>, FilteringProvider>>? {
    val options   = getOptions(FilterOptions())
    var providers = filterProviders.flatten().filter {
        it.appliesTo(callback, callbackType) != false
    } + (options?.providers ?: emptyList())

    val handler = when (options?.skipFilters) {
        true -> {
            providers = providers.filter { it.required }
            this
        }
        null -> {
            if (binding.skipFilters) {
                providers = providers.filter { it.required }
            }
            this.skipFilters()
        }
        false -> this.skipFilters()
    }

    return providers.toList().flatMap { provider ->
        provider.getFilters(binding, filterType, callback,
                            callbackType, handler)
                ?.map { filter -> filter to provider }
                ?: return null
    }.asSequence()
     .toSortedSet(FilterComparator)
     .toList()
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
                .flatMap {
                    it.second.flatMap { p -> p.provideBy.toList() }
                             .mapNotNull { p -> p.objectInstance?.apply {
                                configure(it.first) }
                             }
                } +
         getMetaAnnotations<UseFilterProviderFactory>()
                .flatMap { it.second
                    it.second.mapNotNull { f ->
                        f.createBy.objectInstance
                                ?.createProvider(it.first)?.apply {
                                    configure(it.first)
                                }
                    }
                } +
        (getMetaAnnotations<UseFilter>()
                .flatMap {
                    it.second.map { f ->
                        FilterSpecProvider(createSpec(f)).apply {
                            configure(it.first)
                        }
                    }
                })
        ).toList()
         .normalize()


fun AnnotatedElement.getFilterProviders() =
        (getMetaAnnotations<UseFilterProvider>()
                .flatMap {
                    it.second.flatMap { p -> p.provideBy.toList() }
                             .mapNotNull { p -> p.objectInstance?.apply {
                                configure(it.first) }
                             }
                } +
         getMetaAnnotations<UseFilterProviderFactory>()
                .flatMap {
                    it.second.mapNotNull { f ->
                        f.createBy.objectInstance
                                ?.createProvider(it.first)?.apply {
                                    configure(it.first)
                                }
                    }
                } +
        (getMetaAnnotations<UseFilter>()
                .flatMap {
                    it.second.map { f ->
                        FilterSpecProvider(createSpec(f)).apply {
                            configure(it.first)
                        }
                    }
                })
        ).toList()
         .normalize()


private fun createSpec(useFilter: UseFilter) = FilterSpec(
        useFilter.filterBy,
        useFilter.order.takeIf { it >=0 },
        useFilter.required)