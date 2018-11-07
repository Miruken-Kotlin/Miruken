package com.miruken.protocol

import com.miruken.TypeReference
import com.miruken.typeOf
import java.lang.reflect.Method
import java.lang.reflect.Proxy

interface ProtocolAdapter {
    fun dispatch(
            protocol: TypeReference,
            method:   Method,
            args:     Array<Any?>
    ): Any?
}

fun ProtocolAdapter.proxy(protocol: TypeReference): Any {
    val protocolClass = protocol.type as? Class<*>
    require(protocolClass?.isInterface ?: false) {
        "Protocol '$protocol' is not an interface"
    }

    return Proxy.newProxyInstance(
            protocol.javaClass.classLoader,
            arrayOf(protocolClass),
            Interceptor(this, protocol))
}

inline fun <reified T: Any> ProtocolAdapter.proxy() =
        proxy(typeOf<T>()) as T