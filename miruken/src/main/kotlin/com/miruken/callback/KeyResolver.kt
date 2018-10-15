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
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = when {
        typeInfo.flags has TypeFlags.LAZY ->
            resolveKeyLazy(inquiry, typeInfo, composer)
        typeInfo.flags has TypeFlags.FUNC ->
            resolveKeyFunc(inquiry, typeInfo, composer)
        else -> resolveKeyInfer(inquiry, typeInfo, handler, composer)
    }

    open fun resolveKey(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolve(inquiry)

    open fun resolveKeyAsync(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAsync(inquiry) then {
        check (it != null ||
                typeInfo.componentType.isMarkedNullable) {
            "Unable to resolve key ${inquiry.key}"
        }
        it
    }

    open fun resolveKeyAll(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAll(inquiry)

    private fun resolveKeyAllArray(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = resolveKeyAll(
            inquiry, typeInfo, handler, composer)
            .toTypedArray(typeInfo.componentType.jvmErasure)

    open fun resolveKeyAllAsync(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAllAsync(inquiry)

    private fun resolveKeyAllArrayAsync(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ) = resolveKeyAllAsync(
            inquiry, typeInfo, handler, composer) then {
        it.toTypedArray(typeInfo.componentType.jvmErasure)
    }

    private fun resolveKeyLazy(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            composer: Handling
    ) = lazy(LazyThreadSafetyMode.NONE) {
        // ** MUST ** use composer, composer since
        // handler may be invalidated at this point
        resolveKeyInfer(inquiry, typeInfo, composer, composer)
    }

    private fun resolveKeyFunc(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            composer: Handling
    ): () -> Any? = {
        // ** MUST ** use composer, composer since
        // handler may be invalidated at this point
        resolveKeyInfer(inquiry, typeInfo, composer, composer)
    }

    private fun resolveKeyInfer(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling
    ): Any? {
        val flags = typeInfo.flags
        return when {
            flags has TypeFlags.COLLECTION  ->
                when {
                    inquiry.wantsAsync ->
                        resolveKeyAllAsync(
                                inquiry, typeInfo, handler, composer)
                    else -> resolveKeyAll(
                            inquiry, typeInfo, handler, composer)
                }
            flags has TypeFlags.ARRAY ->
                when {
                    inquiry.wantsAsync ->
                        resolveKeyAllArrayAsync(
                                inquiry, typeInfo, handler, composer)
                    else -> resolveKeyAllArray(
                            inquiry, typeInfo, handler, composer)
                }
            inquiry.wantsAsync -> resolveKeyAsync(
                    inquiry, typeInfo, handler, composer)
            else -> resolveKey(inquiry, typeInfo, handler, composer)
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