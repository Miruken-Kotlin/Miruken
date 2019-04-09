package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.runtime.closeType
import com.miruken.runtime.getFirstMetaAnnotation
import com.miruken.runtime.toTypedArray
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.jvmErasure

open class KeyResolver : KeyResolving {
    override fun resolve(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = when {
        typeInfo.flags has TypeFlags.LAZY ->
            resolveKeyLazy(inquiry, typeInfo, handler)
        typeInfo.flags has TypeFlags.FUNC ->
            resolveKeyFunc(inquiry, typeInfo, handler)
        else -> resolveKeyInfer(inquiry, typeInfo, handler)
    }

    open fun resolveKey(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = handler.resolve(inquiry)

    open fun resolveKeyAsync(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = handler.resolveAsync(inquiry) then {
        when {
            it == null -> when {
                typeInfo.componentType.isMarkedNullable ||
                typeInfo.flags has TypeFlags.OPTIONAL-> null
                typeInfo.flags has TypeFlags.OPTIONAL_EXPLICIT ->
                    Optional.empty<Any>()
                else -> error("Unable to resolve key '${inquiry.key}'")
            }
            typeInfo.flags has TypeFlags.OPTIONAL_EXPLICIT ->
                Optional.of(it)
            else -> it
        }
    }

    open fun resolveKeyAll(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = handler.resolveAll(inquiry)

    private fun resolveKeyAllArray(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = resolveKeyAll(
            inquiry, typeInfo, handler)
            .toTypedArray(typeInfo.componentType.jvmErasure)

    open fun resolveKeyAllAsync(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = handler.resolveAllAsync(inquiry)

    private fun resolveKeyAllArrayAsync(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = resolveKeyAllAsync(
            inquiry, typeInfo, handler) then {
        it.toTypedArray(typeInfo.componentType.jvmErasure)
    }

    private fun resolveKeyLazy(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ) = lazy(LazyThreadSafetyMode.NONE) {
        resolveKeyInfer(inquiry, typeInfo, handler)
    }

    private fun resolveKeyFunc(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ): () -> Any? = {
        resolveKeyInfer(inquiry, typeInfo, handler)
    }

    private fun resolveKeyInfer(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ): Any? {
        val flags = typeInfo.flags
        return when {
            flags has TypeFlags.COLLECTION  ->
                when {
                    inquiry.wantsAsync ->
                        resolveKeyAllAsync(
                                inquiry, typeInfo, handler)
                    else -> resolveKeyAll(
                            inquiry, typeInfo, handler)
                }
            flags has TypeFlags.ARRAY ->
                when {
                    inquiry.wantsAsync ->
                        resolveKeyAllArrayAsync(
                                inquiry, typeInfo, handler)
                    else -> resolveKeyAllArray(
                            inquiry, typeInfo, handler)
                }
            inquiry.wantsAsync -> resolveKeyAsync(
                    inquiry, typeInfo, handler)
            else -> resolveKey(inquiry, typeInfo, handler)
        }?.let {
            if (typeInfo.flags has TypeFlags.OPTIONAL_EXPLICIT) {
                Optional.of(it)
            } else it
        } ?: if (typeInfo.flags has TypeFlags.OPTIONAL_EXPLICIT) {
            Optional.empty<Any>()
        } else null
    }

    companion object : KeyResolver() {
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
                handler:         Handling
        ) = getResolver(getResolverClass(annotatedElement), handler)

        fun getResolver(
                resolverClass: KClass<out KeyResolving>?,
                handler:      Handling
        ) = if (resolverClass == null) this
            else resolverClass.objectInstance
                ?: handler.resolve(resolverClass) as? KeyResolving

        fun getResolverClass(annotatedElement: KAnnotatedElement) =
                annotatedElement.getFirstMetaAnnotation<UseKeyResolver>()
                        ?.keyResolverClass
    }
}