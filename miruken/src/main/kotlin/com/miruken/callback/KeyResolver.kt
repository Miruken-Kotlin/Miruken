package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.runtime.closeType
import com.miruken.runtime.getFirstMetaAnnotation
import com.miruken.runtime.toTypedArray
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.jvmErasure

open class KeyResolver : KeyResolving {
    override fun resolve(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry?
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
            composer: Handling,
            parent:   Inquiry? = null
    ) = handler.resolve(key, parent)

    open fun resolveKeyAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ) = handler.resolveAsync(key, parent,
            !typeInfo.componentType.isMarkedNullable)

    open fun resolveKeyAll(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ) = handler.resolveAll(key, parent)

    private fun resolveKeyAllArray(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ) = resolveKeyAll(key, typeInfo, handler, composer, parent)
            .toTypedArray(typeInfo.componentType.jvmErasure)

    open fun resolveKeyAllAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ) = handler.resolveAllAsync(key, parent)

    private fun resolveKeyAllArrayAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ) = resolveKeyAllAsync(key, typeInfo, handler, composer, parent) then {
        it.toTypedArray(typeInfo.componentType.jvmErasure)
    }

    private fun resolveArgumentLazy(
            key:      Any,
            typeInfo: TypeInfo,
            composer: Handling,
            parent:   Inquiry? = null
    ) =
        lazy(LazyThreadSafetyMode.NONE) {
            // ** MUST ** use composer, composer since
            // handler may be invalidated at this point
            resolveArgument(key, typeInfo, composer, composer, parent)
        }

    private fun resolveArgumentFunc(
            key:      Any,
            typeInfo: TypeInfo,
            composer: Handling,
            parent:   Inquiry? = null
    ): () -> Any? = {
        // ** MUST ** use composer, composer since
        // handler may be invalidated at this point
        resolveArgument(key, typeInfo, composer, composer, parent)
    }

    private fun resolveArgument(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ): Any? {
        val flags = typeInfo.flags
        return when {
            flags has TypeFlags.COLLECTION  ->
                when {
                    flags has TypeFlags.PROMISE ->
                        resolveKeyAllAsync(key, typeInfo, handler, composer, parent)
                    else -> resolveKeyAll(key, typeInfo, handler, composer, parent)
                }
            flags has TypeFlags.ARRAY ->
                when {
                    flags has TypeFlags.PROMISE ->
                        resolveKeyAllArrayAsync(
                                key, typeInfo, handler, composer, parent)
                    else -> resolveKeyAllArray(
                            key, typeInfo, handler, composer, parent)
                }
            flags has TypeFlags.PROMISE ->
                resolveKeyAsync(key, typeInfo, handler, composer, parent)
            else -> resolveKey(key, typeInfo, handler, composer, parent)
        }
    }

    companion object {
        fun getKey(
                annotatedElement: KAnnotatedElement,
                typeInfo:         TypeInfo,
                primitiveName:    String?,
                typeBindings:     Map<KTypeParameter, KType>? = null
        ) = annotatedElement.annotations
                .firstOrNull { it is Key }?.let {
                    val key = it as Key
                    StringKey(key.key, key.caseSensitive)
                } ?: primitiveName?.takeIf {
            typeInfo.flags has TypeFlags.PRIMITIVE
        } ?: typeInfo.componentType.let { keyType ->
            keyType.takeUnless {
                typeBindings != null && typeInfo.flags has TypeFlags.OPEN
            } ?: keyType.closeType(typeBindings!!)
        }

        fun getResolver(
                annotatedElement: KAnnotatedElement,
                composer:         Handling
        ) = getResolver(getResolverClass(annotatedElement), composer)

        fun getResolver(
                resolverClass: KClass<out KeyResolving>?,
                composer:      Handling
        ) = if (resolverClass == null) DefaultResolver
            else resolverClass.objectInstance
                ?: composer.resolve(resolverClass) as? KeyResolving

        fun getResolverClass(annotatedElement: KAnnotatedElement) =
                annotatedElement.getFirstMetaAnnotation<UseKeyResolver>()
                        ?.keyResolverClass

        object DefaultResolver : KeyResolver()
    }
}