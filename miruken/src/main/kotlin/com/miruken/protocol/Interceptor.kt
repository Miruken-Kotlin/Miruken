package com.miruken.protocol

import com.miruken.callback.NotHandledException
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
                ?: getDefaultPrimitiveValueOf(method.returnType)
        }
        catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    companion object {
        private fun getDefaultPrimitiveValueOf(clazz: Class<*>): Any? {
            return if (clazz.isPrimitive)
                when (clazz.name) {
                    "boolean" -> false
                    "char"    -> '\u0000'
                    "byte"    -> 0
                    "short"   -> 0
                    "int"     -> 0
                    "float"   -> 0.0f
                    "long"    -> 0L
                    "double"  -> 0.0
                else -> null
            } else null
        }

        private val NO_ARGS = arrayOfNulls<Any>(0)
    }
}