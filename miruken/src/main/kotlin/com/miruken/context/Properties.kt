package com.miruken.context

import com.miruken.TypeInfo
import com.miruken.callback.KeyResolver
import com.miruken.callback.validateProperty
import com.miruken.concurrent.Promise
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias ContextualPropertyFactory<T> =
        (Any, TypeInfo, Contextual) -> ReadOnlyProperty<Contextual, T>

@Suppress("unused")
fun <T> Contextual.get() = ContextualPropertyProvider<T> {
    key, typeInfo, contextual -> GetProperty(key, typeInfo, contextual)
}

@Suppress("unused")
fun <T> Contextual.link() = ContextualPropertyProvider<T> {
    key, typeInfo, _ -> LinkProperty(key, typeInfo)
}

fun <T> Contextual.getAll() = get<List<T>>()
fun <T> Contextual.linkAll() = link<List<T>>()
fun <T> Contextual.getArray() = get<Array<T>>()
fun <T> Contextual.linkArray() = link<Array<T>>()
fun <T> Contextual.getAsync() = get<Promise<T>>()
fun <T> Contextual.linkAsync() = link<Promise<T>>()
fun <T> Contextual.getAllAsync() = get<Promise<List<T>>>()
fun <T> Contextual.linkAllAsync() = link<Promise<List<T>>>()
fun <T> Contextual.getArrayAsync() = get<Promise<Array<T>>>()
fun <T> Contextual.linkArrayAsync() = link<Promise<Array<T>>>()

class ContextualPropertyProvider<out T>(
        private val factory: ContextualPropertyFactory<T>
) {
    operator fun provideDelegate(
            contextual: Contextual,
            property:   KProperty<*>
    ): ReadOnlyProperty<Contextual, T> {
        val typeInfo = TypeInfo.parse(property.returnType, property)
        val key      = KeyResolver.getKey(property, typeInfo, property.name)
                ?: error("Unable to determine key for '$property'")
        return factory(key, typeInfo, contextual)
    }
}

open class LinkProperty<out T>(
        val key:        Any,
        val typeInfo:   TypeInfo
) : ReadOnlyProperty<Contextual, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Contextual, property: KProperty<*>): T {
        val optional = property.returnType.isMarkedNullable
        val context  = thisRef.getContext(!optional) ?: return null as T
        return KeyResolver.getResolver(property, context)?.let {
            it.validate(key, typeInfo)
            val inquiry = typeInfo.createInquiry(key)
            validateProperty(property, key, it.resolve(
                    inquiry, typeInfo, context)) as T
        } ?: if (optional) (null as T) else
            error("Unable to resolve '$property' with key '$key'")
    }
}

class GetProperty<out T>(
        key:        Any,
        typeInfo:   TypeInfo,
        contextual: Contextual
) : LinkProperty<T>(key, typeInfo) {
    private var _value: T? = null

    init {
        contextual.contextChanged += { _value = null }
    }

    override fun getValue(thisRef: Contextual, property: KProperty<*>): T {
        if (_value != null) return _value!!
        val value = super.getValue(thisRef, property)
        _value = value
        return value
    }
}