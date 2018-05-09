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
        val handleMethod = callback as HandleMethod
        return try {
            val filters = resolveFilters(target, handleMethod, composer)
            if (filters.isEmpty()) {
                withComposer(composer) {
                    invoke(handleMethod, target)
                }
                HandleResult.HANDLED
            } else filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (!proceed) {
                    return@foldRight HandleResult.NOT_HANDLED
                }
                withComposer(comp) {
                    invoke(handleMethod, target)
                }
                HandleResult.HANDLED
            }, { pipeline, next -> { comp, proceed ->
                    if (proceed) {
                        pipeline.next(handleMethod, this, comp, { c,p ->
                            next(c ?: comp, p ?: true)
                        })
                        HandleResult.HANDLED
                    } else {
                        HandleResult.NOT_HANDLED
                    }
                }
            })(composer, true)
        } catch (e: Throwable) {
            when (e) {
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
