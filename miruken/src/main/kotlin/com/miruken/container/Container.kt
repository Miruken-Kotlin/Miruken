package com.miruken.container

import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

@Protocol
interface Container {
    fun resolve(key: Any): Any?
    fun resolveAsync(key: Any): Promise<Any?>
    fun resolveAll(key: Any): List<Any>
    fun resolveAllAsync(key: Any): Promise<List<Any>>
    fun release(component: Any)

    companion object {
        val PROTOCOL = typeOf<Container>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Container
    }
}

inline fun <reified T: Any> Container.resolve(): T? =
        resolve(typeOf<T>()) as? T

inline fun <reified T: Any> Container.resolveAsync(): Promise<T?> =
        resolveAsync(typeOf<T>()) then { it as? T }

inline fun <reified T: Any> Container.resolveAll(): List<T> =
        resolveAll(typeOf<T>()).filterIsInstance<T>()

inline fun <reified T: Any> Container.resolveAllAsync(): Promise<List<T>> =
        resolveAllAsync(typeOf<T>()) then { it.filterIsInstance<T>() }