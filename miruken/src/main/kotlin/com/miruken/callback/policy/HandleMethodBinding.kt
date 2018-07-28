package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.runtime.getTaggedAnnotations
import com.miruken.runtime.normalize
import com.miruken.toKType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class HandleMethodBinding(
        val protocolMethod: Method,
        method: Method
): MemberBinding(method) {
    init { method.isAccessible = true }

    override val returnType = method.genericReturnType.toKType()

    override val skipFilters =
        method.getAnnotation(SkipFilters::class.java) != null ||
        method.declaringClass.getAnnotation(SkipFilters::class.java) != null

    private val useFilters by lazy {
        (method.getTaggedAnnotations<UseFilter>() +
         method.declaringClass.getTaggedAnnotations() +
         protocolMethod.getTaggedAnnotations() +
         protocolMethod.declaringClass.getTaggedAnnotations())
                .flatMap { it.second }
                .normalize()
    }

    private val useFilterProviders by lazy {
        (method.getTaggedAnnotations<UseFilterProvider>() +
         method.declaringClass.getTaggedAnnotations() +
         protocolMethod.getTaggedAnnotations() +
         protocolMethod.declaringClass.getTaggedAnnotations())
                .flatMap { it.second }
                .normalize()
    }

    fun dispatch(
            target:   Any,
            callback: Any,
            composer: Handling
    ): HandleResult {
        val handleMethod = callback as HandleMethod
        handleMethod.result = try {
            val filters = resolveFilters(target, handleMethod, composer)
            if (filters.isEmpty()) {
                withComposer(composer) {
                    invoke(handleMethod, target)
                }
            } else filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (!proceed) notHandled()
                withComposer(comp) {
                    Promise.resolve(invoke(handleMethod, target))
                }
            }, { pipeline, next -> { comp, proceed ->
                    if (!proceed) notHandled()
                    pipeline.next(handleMethod, this, comp, { c,p ->
                        next((c ?: comp).skipFilters(), p ?: true)
                    })
                }
            })(composer, true).let {
                it.takeIf {
                    protocolMethod.returnType.isInstance(it)
                } ?: it.get()
            }
        } catch (e: Throwable) {
            when (e) {
                is HandleResultException -> return e.result
                is InvocationTargetException -> {
                    val cause = e.cause ?: e
                    if (cause is HandleResultException) {
                        return cause.result
                    } else {
                        handleMethod.exception = cause
                        throw cause
                    }
                }
                else -> {
                    handleMethod.exception = e
                    throw e
                }
            }
        }
        return HandleResult.HANDLED
    }

    private fun invoke(
            handleMethod: HandleMethod,
            target:       Any
    ) = (member as Method)
            .invoke(target, *handleMethod.arguments)

    @Suppress("UNCHECKED_CAST")
    private fun resolveFilters(
            target:       Any,
            handleMethod: HandleMethod,
            composer:     Handling
    ): List<Filtering<Any,Any?>> {
        val filterType = Filtering::class.createType(listOf(
                KTypeProjection.invariant(HandleMethod.TYPE),
                KTypeProjection.invariant(handleMethod.resultType!!)))
        return composer.getOrderedFilters(filterType, this,
                (target as? Filtering<*,*>)?.let {
                    GLOBAL_FILTERS + InstanceFilterProvider(it)
                } ?: GLOBAL_FILTERS,
                useFilterProviders, useFilters
        ) as List<Filtering<Any,Any?>>
    }

    companion object {
        private var GLOBAL_FILTERS = mutableListOf<FilteringProvider>()

        val globalFilters: List<FilteringProvider> get() = GLOBAL_FILTERS

        fun addFilters(vararg filters: Filtering<*,*>) {
            if (filters.isNotEmpty()) {
                GLOBAL_FILTERS.add(InstanceFilterProvider(*filters))
            }
        }

        fun addFilterProviders(vararg providers: FilteringProvider) {
            GLOBAL_FILTERS.addAll(providers)
        }
    }
}
