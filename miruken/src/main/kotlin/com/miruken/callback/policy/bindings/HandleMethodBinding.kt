package com.miruken.callback.policy.bindings

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.runtime.normalize
import com.miruken.toKType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class HandleMethodBinding(
        val protocolMethod: Method,
        val method:         Method
): MemberBinding() {
    init { method.isAccessible = true }

    override val returnType = method.genericReturnType.toKType()

    override val skipFilters = SkipFilters::class.java.let {
        method.getAnnotation(it) != null ||
                method.declaringClass.getAnnotation(it) != null
    }

    private val filterProviders by lazy {
        method.getFilterProviders() +
        method.declaringClass.getFilterProviders() +
        protocolMethod.getFilterProviders() +
        protocolMethod.declaringClass.getFilterProviders()
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
                    ?: return HandleResult.NOT_HANDLED
            if (filters.isEmpty()) {
                withComposer(composer) { invoke(handleMethod, target) }
            } else filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (!proceed) notHandled()
                withComposer(comp) {
                    Promise.resolve(invoke(handleMethod, target))
                }
            }, { pipeline, next -> { comp, proceed ->
                    if (!proceed) notHandled()
                    pipeline.first.next(handleMethod, handleMethod, this, comp,
                            { c,p -> next((c ?: comp), p ?: true)
                    }, pipeline.second)
                }
            })(composer, true).let { result ->
                result.takeIf {
                    protocolMethod.returnType.let {
                        it != Any::class.java && it.isInstance(result)
                    }
                } ?: result.get()
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
    ) = method.invoke(target, *handleMethod.arguments)

    @Suppress("UNCHECKED_CAST")
    private fun resolveFilters(
            target:       Any,
            handleMethod: HandleMethod,
            composer:     Handling
    ): List<Pair<Filtering<Any,Any?>, FilteringProvider>>? {
        val filterType = Filtering::class.createType(listOf(
                KTypeProjection.invariant(HandleMethod.TYPE.kotlinType),
                KTypeProjection.invariant(handleMethod.resultType)))
        return composer.getOrderedFilters(
                filterType, this, handleMethod, HandleMethod.TYPE,
                    sequenceOf(
                        filterProviders,
                        filters,
                        ((target as? Filtering<*,*>)?.let {
                            listOf(FilterInstanceProvider(it))
                        } ?: emptyList()))
        ) as? List<Pair<Filtering<Any,Any?>, FilteringProvider>>?
    }

    companion object : FilteredObject()
}
