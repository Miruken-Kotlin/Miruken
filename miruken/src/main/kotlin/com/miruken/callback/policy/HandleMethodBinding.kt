package com.miruken.callback.policy

import com.miruken.callback.HandleMethod
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class HandleMethodBinding(method: Method): MethodBinding(method) {
    fun dispatch(
            target:   Any,
            callback: Any,
            composer: Handling
    ): HandleResult {

        val oldComposer  = COMPOSER.get()
        val oldUnhandled = HANDLE_RESULT.get()
        val handleMethod = callback as HandleMethod

        try {
            COMPOSER.set(composer)
            HANDLE_RESULT.set(HandleResult.HANDLED)
            return invoke(handleMethod, target, composer)
        } catch (e: Throwable) {
            handleMethod.exception = when (e) {
                is InvocationTargetException -> e.cause ?: e
                else -> e
            }
            throw e
        } finally {
            HANDLE_RESULT.set(oldUnhandled)
            COMPOSER.set(oldComposer)
        }
    }

    private fun invoke(
            handleMethod: HandleMethod,
            target:       Any,
            composer:     Handling
    ): HandleResult {
        val returnValue = handleMethod
                .method.invoke(target, *handleMethod.arguments)
        val result = HANDLE_RESULT.get() ?: HandleResult.HANDLED
        if (result.handled)
            handleMethod.result = returnValue
        return result
    }

    companion object {
        @PublishedApi
        internal val COMPOSER = ThreadLocal<Handling?>()

        @PublishedApi
        internal val HANDLE_RESULT = ThreadLocal<HandleResult?>()
    }
}