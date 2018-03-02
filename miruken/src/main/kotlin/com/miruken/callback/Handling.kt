package com.miruken.callback

import com.miruken.protocol.ProtocolAdapter
import java.lang.reflect.Method
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

@FunctionalInterface
interface Handling : ProtocolAdapter {
    fun handle(
            callback: Any,
            greedy:   Boolean   = false,
            composer: Handling? = null
    ) : HandleResult

    override fun dispatch(
            protocol: KType,
            method:   Method,
            args:     Array<Any?>
    ): Any? {
        var handler   = this
        var options   = -CallbackOptions.NONE
        val semantics = CallbackSemantics()
        handler.handle(semantics, true)

        val clazz = protocol.jvmErasure

        if (!semantics.isSpecified(CallbackOptions.DUCK) &&
                clazz.findAnnotation<Duck>() != null) {
            options += CallbackOptions.DUCK
        }

        if (!semantics.isSpecified(CallbackOptions.STRICT) &&
                clazz.findAnnotation<Strict>() != null) {
            options += CallbackOptions.STRICT
        }

        if (clazz.findAnnotation<Resolving>() != null) {
            if (semantics.isSpecified(CallbackOptions.BROADCAST)) {
                options += CallbackOptions.BROADCAST
            }
            handler = handler.resolving
        }

        if (options != CallbackOptions.NONE) {
            semantics.setOption(options, true)
            handler = handler.semantics(options)
        }

        val handleMethod = HandleMethod(protocol, method, args, semantics)
        return handler.handle(handleMethod) failure  {
            throw NotHandledException(handleMethod,
                    "Method '$method' on $protocol not handled")
        } ?: handleMethod.result
    }
}