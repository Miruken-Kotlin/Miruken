package com.miruken.protocol

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KType

class Interceptor(
        private val adapter: ProtocolAdapter,
        private val protocol: KType
) : InvocationHandler {
    override fun invoke(
            proxy:  Any,
            method: Method,
            args:   Array<Any?>?
    ): Any? {
        try {
            return adapter.dispatch(protocol, method, args ?: NO_ARGS)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    companion object {
        private val NO_ARGS = arrayOfNulls<Any>(0)
    }
}