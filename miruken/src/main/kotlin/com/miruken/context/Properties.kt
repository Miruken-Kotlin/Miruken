package com.miruken.context

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias ResolverFactory<T> =
        (Any, TypeInfo, Contextual<*>) -> ReadOnlyProperty<Any, T>

@Suppress("unused")
fun <T> Contextual<*>.get()  = ResolvePropertyProvider<T> {
    key, typeInfo, contextual -> GetProperty(key, typeInfo, contextual)
}

@Suppress("unused")
fun <T> Contextual<*>.link()  = ResolvePropertyProvider<T> {
    key, typeInfo, contextual -> LinkProperty(key, typeInfo, contextual)
}

fun <T> Contextual<*>.getAsync()  = get<Promise<T>>()
fun <T> Contextual<*>.linkAsync() = link<Promise<T>>()

class ResolvePropertyProvider<out T>(
        private val factory: ResolverFactory<T>
) {
    operator fun provideDelegate(
            thisRef:  Contextual<*>,
            property: KProperty<*>
    ): ReadOnlyProperty<Any, T> {
        val typeInfo = TypeFlags.parse(property.returnType)
        val key      = KeyResolver.getKey(property, typeInfo, property.name)
        return factory(key, typeInfo, thisRef)
    }
}

open class LinkProperty<out T>(
        val key:        Any,
        val typeInfo:   TypeInfo,
        val contextual: Contextual<*>
) : ReadOnlyProperty<Any, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val context = contextual.requireContext()
        return KeyResolver.getResolver(property, context)?.let {
            it.validate(key, typeInfo)
            validateProperty(property, key, it.resolve(
                    key, typeInfo, context, context)) as T
        } ?: if (property.returnType.isMarkedNullable) (null as T) else
            error("Unable to resolve '$property' with key '$key'")
    }
}

class GetProperty<out T>(
        key:        Any,
        typeInfo:   TypeInfo,
        contextual: Contextual<*>
) : LinkProperty<T>(key, typeInfo, contextual) {
    private var _value: T? = null

    init {
        contextual.contextChanged += { _ -> _value = null }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (_value != null) return _value!!
        val value = super.getValue(thisRef, property)
        _value = value
        return value
    }
}