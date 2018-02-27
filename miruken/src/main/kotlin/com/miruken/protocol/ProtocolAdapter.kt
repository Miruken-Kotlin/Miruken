package com.miruken.protocol

import com.miruken.runtime.typeOf
import java.lang.reflect.Method
import kotlin.reflect.KType

@FunctionalInterface
interface ProtocolAdapter {
    fun dispatch(
            protocol: KType,
            method:   Method,
            args:     Array<Any?>
    ): Any?
}

fun ProtocolAdapter.proxy(protocol: KType): Any =
        Protocol.proxy(this, protocol)

inline fun <reified T: Any> ProtocolAdapter.proxy(): T =
        Protocol.proxy(this, typeOf<T>()) as T