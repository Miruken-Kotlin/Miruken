package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.runtime.getFirstTaggedAnnotation
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> Handling.get() = ProvideResolveDelegate<T>(this)

class ProvideResolveDelegate<out T>(private val handler: Handling) {
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
            ResolveDelegate<T>(key, handler, it, typeInfo)
        } ?: error("Unable to resolve key '$key'")
    }
}

private class ResolveDelegate<out T>(
        val key:      Any,
        val handler:  Handling,
        val resolver: KeyResolving,
        val typeInfo: TypeInfo
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
                else -> "Non-nullable '$property' with key '$key' could not be resolved"
            })
        }
}
