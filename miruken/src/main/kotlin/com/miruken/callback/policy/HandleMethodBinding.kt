package com.miruken.callback.policy

import com.miruken.callback.*
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

        val oldComposer  = threadComposer.get()
        val handleMethod = callback as HandleMethod

        try {
            threadComposer.set(composer)
            val filters = resolveFilters(target, handleMethod, composer)
            if (filters.isEmpty())
                invoke(handleMethod, target)
            else filters.foldRight(
                    { invoke(handleMethod, target) },
                    { pipeline, next -> {
                        pipeline.next(handleMethod, this, composer, next) }
                    })()
            return HandleResult.HANDLED
        } catch (e: Throwable) {
            return when (e) {
                is HandleResultException -> e.result
                is InvocationTargetException -> {
                    val cause = e.cause ?: e
                    if (cause is HandleResultException) {
                        cause.result
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
        } finally {
            threadComposer.set(oldComposer)
        }
    }

    private fun invoke(
            handleMethod: HandleMethod,
            target:       Any
    ) = (member as Method)
            .invoke(target, *handleMethod.arguments)?.also {
                handleMethod.result = it
            }

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
                    listOf(InstanceFilterProvider(it))
                } ?: emptyList(),
                useFilterProviders, useFilters
        ) as List<Filtering<Any,Any?>>
    }
}

private val threadComposer = ThreadLocal<Handling?>()
val COMPOSER get() = threadComposer.get()