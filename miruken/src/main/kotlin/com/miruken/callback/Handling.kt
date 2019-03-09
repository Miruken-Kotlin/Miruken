package com.miruken.callback

import com.miruken.*
import com.miruken.protocol.ProtocolAdapter
import java.lang.reflect.Method

interface Handling : ProtocolAdapter, ResolveArgs {
    fun handle(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean   = false,
            composer:     Handling? = null
    ): HandleResult

    override fun dispatch(
            protocol: TypeReference,
            method:   Method,
            args:     Array<Any?>
    ): Any? {
        var handler   = this
        var options   = -CallbackOptions.NONE
        val semantics = CallbackSemantics()
        handler.handle(semantics, true)

        val protocolClass = protocol.type as Class<*>
        val annotations   = protocolClass.annotations

        if (!semantics.isSpecified(CallbackOptions.DUCK) &&
                annotations.filterIsInstance<Duck>().isNotEmpty()) {
            options += CallbackOptions.DUCK
        }

        if (!semantics.isSpecified(CallbackOptions.STRICT) &&
                annotations.filterIsInstance<Strict>().isNotEmpty()) {
            options += CallbackOptions.STRICT
        }

        if (annotations.filterIsInstance<Resolving>().isNotEmpty()) {
            if (semantics.isSpecified(CallbackOptions.BROADCAST)) {
                options += CallbackOptions.BROADCAST
            }
            handler = handler.infer
        }

        if (options != CallbackOptions.NONE) {
            semantics.setOption(options, true)
            handler = handler.semantics(options)
        }

        val handleMethod = HandleMethod(protocol, method, args, semantics)
        return handler.handle(handleMethod) failure  {
            throw NotHandledException(handleMethod,
                    "Method $method'not handled")
        } ?: handleMethod.result
    }

    override fun invoke(types: Array<out TypeReference>): List<Any?>? {
        return if (types.isEmpty()) emptyList() else types.map { key ->
            val typeInfo = TypeInfo.parse(key.kotlinType)
            val inquiry  = typeInfo.createInquiry(typeInfo.componentType)
            KeyResolver.resolve(inquiry, typeInfo, this) ?: when {
                typeInfo.flags has TypeFlags.OPTIONAL -> null
                else -> return null
            }
        }
    }
}

inline fun <reified T: Any> Handling.handle(
        callback: T,
        greedy:   Boolean   = false,
        composer: Handling? = null
) = handle(callback, typeOf<T>(), greedy, composer)