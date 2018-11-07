package com.miruken

import com.miruken.concurrent.Promise
import java.lang.reflect.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

abstract class TypeReference<T> protected constructor() {
    var type: Type
        private set

    val kotlinType by lazy(LazyThreadSafetyMode.NONE) {
        javaClass.genericSuperclass.toKType()
                .arguments.single().type!!
    }

    init {
        type = (javaClass.genericSuperclass as? ParameterizedType)?.let {
            it.actualTypeArguments[0]
        } ?: error("TypeReference constructed withou type information")
    }

    companion object {
        val ANY_STAR by lazy(LazyThreadSafetyMode.NONE) {
            Any::class.starProjectedType
        }

        val ANY_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            typeOf<Any>().withNullability(true)
        }

        val COLLECTION_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            typeOf<Collection<Any>>().withNullability(true)
        }

        val PROMISE_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            typeOf<Promise<Any>>().withNullability(true)
        }

        val UNIT_TYPE by lazy(LazyThreadSafetyMode.NONE) {
            Unit::class.starProjectedType
        }
    }
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

fun Type.toKType(): KType = if (this == Void.TYPE) {
    TypeReference.UNIT_TYPE
} else {
    toKTypeProjection().type!!
}
