package com.miruken.protocol

import com.miruken.typeOf
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KType

@FunctionalInterface
interface ProtocolAdapter {
    fun dispatch(
            protocol: KType,
            method:   Method,
            args:     Array<Any?>
    ): Any?
}

fun ProtocolAdapter.proxy(protocol: KType): Any {
    val protocolClass = protocol.classifier as? KClass<*>
    require(protocolClass?.java?.isInterface ?: false) {
        "Protocol '$protocol' is not an interface"
    }

    return Proxy.newProxyInstance(
            protocol.javaClass.classLoader,
            arrayOf(protocolClass!!.java),
            Interceptor(this, protocol))
}

inline fun <reified T: Any> ProtocolAdapter.proxy() =
        proxy(typeOf<T>()) as T