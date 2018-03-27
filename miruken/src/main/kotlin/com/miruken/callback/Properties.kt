package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.concurrent.Promise
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> Handling.get()  = GetPropertyProvider<T>(this)
fun <T> Handling.link() = LinkPropertyProvider<T>(this)

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Handling.getAsync()  = get<Promise<T>>()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Handling.linkAsync() = link<Promise<T>>()

abstract class ResolveProperty<out T>(
        val key:      Any,
        val typeInfo: TypeInfo,
        val resolver: KeyResolving
) : ReadOnlyProperty<Any, T> {
    protected fun validate(value: Any?, property: KProperty<*>) =
            if (value != null || property.returnType.isMarkedNullable) {
                @Suppress("UNCHECKED_CAST")
                value as T
            } else {
                error(when (key) {
                    property.returnType ->
                        "Non-nullable '$property' could not be resolved"
                    else ->
                        "Non-nullable '$property' with key '$key' could not be resolved"
                })
            }
}

abstract class ResolvePropertyProvider<out T> {
    operator fun provideDelegate(
            thisRef:  Any,
            property: KProperty<*>
    ): ResolveProperty<T> {
        val typeInfo = TypeFlags.parse(property.returnType)
        val key      = KeyResolver.getKey(property, typeInfo, property.name)
        return getResolver(property)?.let {
            it.validate(key, typeInfo)
            createPropertyResolver(key,typeInfo, it)
        } ?: error("Unable to resolve '$property' with key '$key'")
    }

    abstract fun getResolver(property: KProperty<*>): KeyResolving?

    abstract fun createPropertyResolver(
            key:      Any,
            typeInfo: TypeInfo,
            resolver: KeyResolving
    ): ResolveProperty<T>
}

open class LinkProperty<out T>(
        key:         Any,
        typeInfo:    TypeInfo,
        resolver:    KeyResolving,
        val handler: Handling
) : ResolveProperty<T>(key, typeInfo, resolver) {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
            validate(resolver.resolve(key, typeInfo, handler, handler),
                    property)
}

class GetProperty<out T>(
        key:      Any,
        typeInfo: TypeInfo,
        resolver: KeyResolving,
        handler:  Handling
) : LinkProperty<T>(key, typeInfo, resolver, handler) {
    private var _value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (_value != null) return _value!!
        val value = super.getValue(thisRef, property)
        _value = value
        return value
    }
}

class LinkPropertyProvider<out T>(
        private val handler: Handling
): ResolvePropertyProvider<T>() {
    override fun getResolver(property: KProperty<*>) =
            KeyResolver.getResolver(property, handler)

    override fun createPropertyResolver(
            key:      Any,
            typeInfo: TypeInfo,
            resolver: KeyResolving
    ) = LinkProperty<T>(key, typeInfo, resolver, handler)
}

class GetPropertyProvider<out T>(
        private val handler: Handling
) : ResolvePropertyProvider<T>() {
    override fun getResolver(property: KProperty<*>) =
            KeyResolver.getResolver(property, handler)

    override fun createPropertyResolver(
            key:      Any,
            typeInfo: TypeInfo,
            resolver: KeyResolving
    ) = GetProperty<T>(key, typeInfo, resolver, handler)
}

