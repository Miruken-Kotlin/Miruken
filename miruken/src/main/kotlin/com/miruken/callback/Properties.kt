package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.concurrent.Promise
import com.miruken.runtime.getFirstTaggedAnnotation
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> Handling.get()  = ResolveDelegateProvider<T>(this) {
    key, typeInfo, handler, resolver ->
        GetDelegate(key, typeInfo, handler, resolver)
}

fun <T> Handling.bind() = ResolveDelegateProvider<T>(this) {
    key, typeInfo, handler, resolver ->
        BindDelegate(key, typeInfo, handler, resolver)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Handling.getAsync()  = get<Promise<T>>()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Handling.bindAsync() = bind<Promise<T>>()

class ResolveDelegateProvider<out T>(
        private val handler: Handling,
        private val factory: (Any,TypeInfo,Handling,KeyResolving) -> BindDelegate<T>
) {
    operator fun provideDelegate(
            thisRef:  Any,
            property: KProperty<*>
    ): ReadOnlyProperty<Any, T> {
        val typeInfo = TypeFlags.parse(property.returnType)
        val key = property.annotations
                .firstOrNull { it is Key }
                ?.let {
                    val key = it as Key
                    StringKey(key.key, key.caseSensitive)
                } ?: property.name.takeIf {
            typeInfo.flags has TypeFlags.PRIMITIVE
        } ?: typeInfo.componentType

        val useResolver = property
                .getFirstTaggedAnnotation<UseKeyResolver>()
                ?.keyResolverClass

        return KeyResolver.getResolver(useResolver, handler)?.let {
            it.validate(key, typeInfo)
            factory(key,typeInfo, handler, it)
        } ?: error("Unable to resolve key '$key'")
    }
}

open class BindDelegate<out T>(
        val key:      Any,
        val typeInfo: TypeInfo,
        val handler:  Handling,
        private val resolver: KeyResolving
) : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
            validate(resolver.resolve(key, typeInfo, handler, handler),
                    property)

    private fun validate(value: Any?, property: KProperty<*>) =
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

class GetDelegate<out T>(
        key:      Any,
        typeInfo: TypeInfo,
        handler:  Handling,
        resolver: KeyResolving
) : BindDelegate<T>(key, typeInfo, handler, resolver) {
    private var _value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (_value != null) return _value!!
        val value = super.getValue(thisRef, property)
        _value = value
        return value
    }
}
