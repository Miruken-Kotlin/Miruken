package com.miruken

import com.miruken.concurrent.Promise
import java.lang.reflect.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaType

interface TypeReference {
    val type:       Type
    val kotlinType: KType

    companion object {
        fun getType(key: Any?): Type? = when(key) {
            is Type -> key
            is KType -> key.javaType
            is KClass<*> -> key.java
            is TypeReference -> key.type
            else -> null
        }

        fun getKType(key: Any?): KType? = when(key) {
            is KType -> key
            is KClass<*> -> key.starProjectedType
            is TypeReference -> key.kotlinType
            is Type ->  TypeToKTypeMapping.getOrPut(key) { key.toKType() }
            else -> null
        }

        val ANY_STAR by lazy(LazyThreadSafetyMode.NONE) {
            Any::class.starProjectedType
        }

        val ANY_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            typeOf<Any>().kotlinType.withNullability(true)
        }

        val COLLECTION_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            typeOf<Collection<Any>>().kotlinType.withNullability(true)
        }

        val PROMISE_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            typeOf<Promise<Any>>().kotlinType.withNullability(true)
        }

        val UNIT_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            Unit::class.starProjectedType
        }
    }
}

abstract class TypeReferenceImpl : TypeReference {
    override fun toString() = type.toString()

    override fun equals(other: Any?) =
            (other as? TypeReference)?.type == type

    override fun hashCode() = type.hashCode()
}

abstract class SuperTypeReference<T>
    protected constructor() : TypeReferenceImpl() {

    final override val type: Type =
            (javaClass.genericSuperclass as? ParameterizedType)
            ?.actualTypeArguments?.get(0)
            ?: error("TypeReference constructed withou type information")

    final override val kotlinType: KType =
        TypeToKTypeMapping.getOrPut(type) {
            javaClass.genericSuperclass.toKType().arguments.single().type!!
        }
}

class GivenTypeReference(
        override val kotlinType: KType
) : TypeReferenceImpl() {
    override val type: Type

    init {
        type = when (val javaType = kotlinType.javaType) {
            is Class<*> -> javaType
            is ParameterizedType -> javaType.rawType
            else -> error("Unable to determine java type from kotlin type '$kotlinType'")
        }
        TypeToKTypeMapping.putIfAbsent(type, kotlinType)
    }
}

private val TypeToKTypeMapping = ConcurrentHashMap<Type, KType>()

fun Type.toKType(): KType = if (this == Void.TYPE) {
    TypeReference.UNIT_TYPE
} else {
    toKTypeProjection().type!!
}

fun KClass<*>.toInvariantFlexibleProjection(
        arguments: List<KTypeProjection> = emptyList()
): KTypeProjection {
    // TODO: there should be an API in kotlin-reflect which creates KType instances corresponding to flexible types
    // Currently we always produce a non-null type, which is obviously wrong
    return KTypeProjection.invariant(
            createType(arguments, nullable = false))
}

fun Type.toKTypeProjection(): KTypeProjection = when (this) {
    is Class<*> -> this.kotlin.toInvariantFlexibleProjection(
            if(this.isArray) listOf(this.componentType.toKTypeProjection())
            else emptyList())
    is ParameterizedType -> {
        val erasure = (rawType as Class<*>).kotlin
        erasure.toInvariantFlexibleProjection(
                (erasure.typeParameters.zip(actualTypeArguments).map {
                    (parameter, argument) ->
                    val projection = argument.toKTypeProjection()
                    projection.takeIf {
                        // Get rid of use-site projections on arguments, where the corresponding parameters already have a declaration-site projection
                        parameter.variance == KVariance.INVARIANT || parameter.variance != projection.variance
                    } ?: KTypeProjection.invariant(projection.type!!)
                }))
    }
    is WildcardType -> when {
        lowerBounds.isNotEmpty() -> KTypeProjection.contravariant(
                lowerBounds.single().toKType())
        upperBounds.isNotEmpty() -> KTypeProjection.covariant(
                upperBounds.single().toKType())
        // This looks impossible to obtain through Java reflection API, but someone may construct and pass such an instance here anyway
        else -> KTypeProjection.STAR
    }
    is GenericArrayType -> Array<Any>::class.toInvariantFlexibleProjection(
            listOf(genericComponentType.toKTypeProjection()))
    is TypeVariable<*> -> TODO() // TODO
    else -> throw IllegalArgumentException("Unsupported type: $this")
}
