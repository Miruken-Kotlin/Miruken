package com.miruken.runtime

import com.miruken.concurrent.Promise
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure


fun isAssignableTo(leftSide: Any, rightSide: Any?): Boolean {
    return when (leftSide) {
        is KType -> when (rightSide) {
            null -> false
            is KType -> when {
                leftSide == rightSide -> true
                rightSide.isOpenGenericDefinition ->
                        leftSide.arguments.isNotEmpty() &&
                                leftSide.classifier == rightSide.classifier
                rightSide.classifier is KTypeParameter ->
                    (rightSide.classifier as KTypeParameter).satisfies(leftSide)
                else -> rightSide.isSubtypeOf(leftSide)
            }
            is KClass<*> -> {
                leftSide.jvmErasure.isSuperclassOf(rightSide) &&
                        (leftSide.arguments.isEmpty() ||
                        leftSide.isOpenGenericDefinition)
            }
            is Class<*> -> {
                leftSide.jvmErasure.java.isAssignableFrom(rightSide) &&
                        (leftSide.arguments.isEmpty() ||
                        leftSide.isOpenGenericDefinition)
            }
            else -> {
                val rightClass = rightSide::class
                leftSide.jvmErasure.isSuperclassOf(rightClass) &&
                        (leftSide.arguments.isEmpty() ||
                        leftSide.isOpenGenericDefinition)
            }
        }
        is KClass<*> -> when (rightSide) {
            null -> false
            is KType -> {
                rightSide.jvmErasure.isSubclassOf(leftSide)
            }
            is KClass<*> -> {
                leftSide == rightSide ||
                rightSide.isSubclassOf(leftSide)
            }
            is Class<*> -> {
                leftSide.typeParameters.isEmpty() &&
                rightSide.typeParameters.isEmpty() &&
                leftSide.java.isAssignableFrom(rightSide)
            }
            else -> {
                val rightClass = rightSide::class
                leftSide.typeParameters.isEmpty() &&
                rightClass.typeParameters.isEmpty() &&
                rightClass.isSubclassOf(leftSide)
            }
        }
        is Class<*> -> when (rightSide) {
            null -> false
            is KType -> {
                leftSide.isAssignableFrom(rightSide.jvmErasure.java)
            }
            is KClass<*> -> {
                leftSide.isAssignableFrom(rightSide.java)
            }
            is Class<*> -> {
                leftSide == rightSide ||
                leftSide.isAssignableFrom(rightSide)
            }
            else -> {
                val rightClass = rightSide::class
                leftSide.isAssignableFrom(rightClass.java)
            }
        }
        else -> false
    }
}

fun Iterable<*>.filterIsAssignableTo(key: Any): List<Any> {
    return filterIsAssignableTo(ArrayList(), key)
}

fun Iterable<*>.filterIsAssignableTo(
        destination: ArrayList<Any>, key: Any
): MutableList<Any> {
    filter { isAssignableTo(key, it) }.mapTo(destination) { it!! }
    return destination
}

fun <T> List<T>.normalize() : List<T> =
        if (isEmpty()) emptyList() else this

val KType.isAny get() = classifier == Any::class

val KType.isUnit get() = classifier == Unit::class

val KType.isNothing get() = classifier == Nothing::class

val KType.componentType: KType?
    get() = when {
        isSubtypeOf(COLLECTION_TYPE) -> /* Nothing??? */
            arguments.singleOrNull()?.type ?: this
        classifier is KClass<*> -> {
            val javaClass = (classifier as KClass<*>).javaObjectType
            if (javaClass.isArray)
                javaClass.componentType.toKType()
            else this
        }
        else -> this
    }

val KType.isOpenGeneric: Boolean
    get() = classifier is KTypeParameter ||
                arguments.any { it.type?.isOpenGeneric == true }

val KType.isOpenGenericDefinition: Boolean
    get() = arguments.isNotEmpty() && arguments.all {
        (it.type?.classifier as? KTypeParameter)
            ?.upperBounds?.all { it == ANY_TYPE } == true }

fun KTypeParameter.satisfies(proposedType: KType) =
        upperBounds.any { proposedType.isSubtypeOf(it) }

inline fun <reified T: Annotation> KAnnotatedElement
        .getTaggedAnnotations() = annotations.mapNotNull {
            val tags = it.annotationClass.annotations.filterIsInstance<T>()
            if (tags.isNotEmpty()) it to tags else null
        }

inline fun <reified T: Annotation> KAnnotatedElement
        .getFirstTaggedAnnotation() = getTaggedAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

val ANY_STAR        = Any::class.starProjectedType
val ANY_TYPE        = typeOf<Any>().withNullability(true)
val COLLECTION_TYPE = typeOf<Collection<Any>>().withNullability(true)
val PROMISE_TYPE    = typeOf<Promise<Any>>().withNullability(true)