package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.concurrent.Promise
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias HandlingPropertyFactory<T> =
        (Any, TypeInfo, Handling, KeyResolving) -> ReadOnlyProperty<Any, T>

fun <T> Handling.get() = HandlingPropertyProvider<T>(this) {
    key, typeInfo, resolver, handler ->
        GetProperty(key, typeInfo, resolver, handler)
}

fun <T> Handling.link() = HandlingPropertyProvider<T>(this) {
    key, typeInfo, resolver, handler ->
        LinkProperty(key, typeInfo, resolver, handler)
}

fun <T> Handling.getAll() = get<List<T>>()
fun <T> Handling.linkAll() = link<List<T>>()
fun <T> Handling.getArray() = get<Array<T>>()
fun <T> Handling.linkArray() = link<Array<T>>()
fun <T> Handling.getAsync() = get<Promise<T>>()
fun <T> Handling.linkAsync() = link<Promise<T>>()
fun <T> Handling.getAllAsync() = get<Promise<List<T>>>()
fun <T> Handling.linkAllAsync() = link<Promise<List<T>>>()
fun <T> Handling.getArrayAsync() = get<Promise<Array<T>>>()
fun <T> Handling.linkArrayAsync() = link<Promise<Array<T>>>()

class HandlingPropertyProvider<out T>(
        val         handler: Handling,
        private val factory: HandlingPropertyFactory<T>
) {
    operator fun provideDelegate(
            thisRef:  Any,
            property: KProperty<*>
    ): ReadOnlyProperty<Any, T> {
        val typeInfo = TypeFlags.parse(property.returnType)
        val key      = KeyResolver.getKey(property, typeInfo, property.name)
            ?: error("Unable to determine key for '$property'")
        return KeyResolver.getResolver(property, handler)?.let {
            it.validate(key, typeInfo)
            factory(key, typeInfo, handler, it)
        } ?: error("Unable to resolve '$property' with key '$key'")
    }
}

open class LinkProperty<out T>(
        val         key:      Any,
        val         typeInfo: TypeInfo,
        val         handler:  Handling,
        private val resolver: KeyResolving
) : ReadOnlyProperty<Any, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val flags   = typeInfo.flags
        val inquiry =  Inquiry(key,
                flags has TypeFlags.COLLECTION ||
                flags has TypeFlags.ARRAY).apply {
            wantsAsync = flags has TypeFlags.PROMISE
        }
        return validateProperty(property, key, resolver.resolve(
                inquiry, typeInfo, handler, handler)) as T
    }
}

class GetProperty<out T>(
        key:      Any,
        typeInfo: TypeInfo,
        handler:  Handling,
        resolver: KeyResolving
) : LinkProperty<T>(key, typeInfo, handler, resolver) {
    private var _value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (_value != null) return _value!!
        val value = super.getValue(thisRef, property)
        _value = value
        return value
    }
}

fun validateProperty(property: KProperty<*>, key: Any, value: Any?): Any? {
    if (value != null || property.returnType.isMarkedNullable) {
        return value
    }
    error(when (key) {
        property.returnType ->
            "Non-nullable '$property' could not be resolved"
        else ->
            "Non-nullable '$property' with key '$key' could not be resolved"
    })
}
