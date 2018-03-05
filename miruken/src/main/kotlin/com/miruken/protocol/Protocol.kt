package com.miruken.protocol

import com.miruken.typeOf
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface Protocol {
    companion object {
        fun proxy(adapter: ProtocolAdapter, protocol: KType): Any {
            val protocolClass = protocol.classifier as? KClass<*>
            require(protocolClass?.java!!.isInterface, {
                "Protocol '$protocol' is not an interface"
            })

            return Proxy.newProxyInstance(
                    protocol.javaClass.classLoader,
                    arrayOf(protocolClass.java),
                    Interceptor(adapter, protocol))
        }

        inline fun <reified T: Any> proxy(adapter: ProtocolAdapter): T =
                proxy(adapter, typeOf<T>()) as T
    }
}