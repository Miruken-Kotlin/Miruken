package com.miruken.protocol

import com.miruken.TypeReference
import com.miruken.concurrent.Promise
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class Interceptor(
        private val adapter:  ProtocolAdapter,
        private val protocol: TypeReference
) : InvocationHandler {
    override fun invoke(
            proxy:  Any,
            method: Method,
            args:   Array<Any?>?
    ): Any? {
        try {
            return adapter.dispatch(protocol, method, args ?: NO_ARGS)
                ?: getDefaultPrimitiveValueOf(method.returnType)
        }
        catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    companion object {
        private fun getDefaultPrimitiveValueOf(clazz: Class<*>): Any? {
            return when {
                clazz.isPrimitive -> when (clazz.name) {
                    "boolean" -> false
                    "char"    -> '\u0000'
                    "byte"    -> 0
                    "short"   -> 0
                    "int"     -> 0
                    "float"   -> 0.0f
                    "long"    -> 0L
                    "double"  -> 0.0
                    else -> null
                }
                clazz.kotlin == Promise::class -> Promise.EMPTY
                else -> null
            }
        }

        private val NO_ARGS = arrayOfNulls<Any>(0)
    }
}