package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.runtime.getFirstTaggedAnnotation
import com.miruken.runtime.toTypedArray
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

open class KeyResolver : KeyResolving {
    override fun resolve(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = when {
        typeInfo.flags has TypeFlags.LAZY ->
                resolveArgumentLazy(key, typeInfo, composer)
        typeInfo.flags has TypeFlags.FUNC ->
            resolveArgumentFunc(key, typeInfo, composer)
        else -> resolveArgument(key, typeInfo, handler, composer)
    }

    open fun resolveKey(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolve(key)

    open fun resolveKeyAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAsync(key,
            (key as? KType)?.isMarkedNullable == false)

    open fun resolveKeyAll(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAll(key)

    private fun resolveKeyAllArray(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = resolveKeyAll(key, typeInfo, handler, composer)
            .toTypedArray(typeInfo.componentType.jvmErasure)

    open fun resolveKeyAllAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAllAsync(key)

    private fun resolveKeyAllArrayAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = resolveKeyAllAsync(key, typeInfo, handler, composer) then {
        it.toTypedArray(typeInfo.componentType.jvmErasure)
    }

    private fun resolveArgumentLazy(
            key:      Any,
            typeInfo: TypeInfo,
            composer: Handling
    ) =
        lazy(LazyThreadSafetyMode.NONE) {
            // ** MUST ** use composer, composer since
            // handler may be invalidated at this point
            resolveArgument(key, typeInfo, composer, composer)
        }

    private fun resolveArgumentFunc(
            key:      Any,
            typeInfo: TypeInfo,
            composer: Handling
    ): () -> Any? = {
        // ** MUST ** use composer, composer since
        // handler may be invalidated at this point
        resolveArgument(key, typeInfo, composer, composer)
    }

    private fun resolveArgument(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ): Any? {
        val flags = typeInfo.flags
        return when {
            flags has TypeFlags.COLLECTION  ->
                when {
                    flags has TypeFlags.PROMISE ->
                        resolveKeyAllAsync(key, typeInfo, handler, composer)
                    else -> resolveKeyAll(key, typeInfo, handler, composer)
                }
            flags has TypeFlags.ARRAY ->
                when {
                    flags has TypeFlags.PROMISE ->
                        resolveKeyAllArrayAsync(key, typeInfo, handler, composer)
                    else -> resolveKeyAllArray(key, typeInfo, handler, composer)
                }
            flags has TypeFlags.PROMISE ->
                resolveKeyAsync(key, typeInfo, handler, composer)
            else -> resolveKey(key, typeInfo, handler, composer)
        }
    }

    companion object {
        fun getKey(
                annotatedElement: KAnnotatedElement,
                typeInfo:         TypeInfo,
                primitiveName:    String?
        ) = annotatedElement.annotations
                .firstOrNull { it is Key }?.let {
                    val key = it as Key
                    StringKey(key.key, key.caseSensitive)
                } ?: primitiveName?.takeIf {
            typeInfo.flags has TypeFlags.PRIMITIVE
        } ?: typeInfo.componentType

        fun getResolver(
                annotatedElement: KAnnotatedElement,
                composer:         Handling
        ) = getResolver(getResolverClass(annotatedElement), composer)

        fun getResolver(
                resolverClass: KClass<out KeyResolving>?,
                composer:      Handling
        ) = if (resolverClass == null)
            DefaultResolver else resolverClass.objectInstance
                ?: composer.resolve(resolverClass) as? KeyResolving

        fun getResolverClass(annotatedElement: KAnnotatedElement) =
                annotatedElement.getFirstTaggedAnnotation<UseKeyResolver>()
                        ?.keyResolverClass

        object DefaultResolver : KeyResolver()
    }
}