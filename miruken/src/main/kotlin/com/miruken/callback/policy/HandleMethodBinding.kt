package com.miruken.callback.policy

import com.miruken.callback.HandleMethod
import com.miruken.callback.HandleResult
import com.miruken.callback.HandleResultException
import com.miruken.callback.Handling
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class HandleMethodBinding(method: Method): MethodBinding(method) {
    init {
        method.isAccessible = true
    }

    fun dispatch(
            target:   Any,
            callback: Any,
            composer: Handling
    ): HandleResult {

        val oldComposer  = COMPOSER.get()
        val handleMethod = callback as HandleMethod

        try {
            COMPOSER.set(composer)
            handleMethod.result =  handleMethod
                    .method.invoke(target, *handleMethod.arguments)
            return HandleResult.HANDLED
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
        } finally {
            COMPOSER.set(oldComposer)
        }
    }

    companion object {
        @PublishedApi
        internal val COMPOSER = ThreadLocal<Handling?>()
    }
}