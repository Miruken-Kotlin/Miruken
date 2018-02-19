package com.miruken.runtime

import com.miruken.concurrent.Promise
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf

fun isAssignableKeys(leftKey: Any, rightKey: Any): Boolean {
    return when (leftKey) {
        is KType -> when (rightKey) {
            is KType -> rightKey.isSubtypeOf(leftKey)
            is KClass<*> -> leftKey.arguments.isEmpty() &&
                    rightKey.typeParameters.isEmpty() &&
                    (leftKey.classifier as? KClass<*>)
                            ?.isSuperclassOf(rightKey) == true
            else -> false
        }
        is KClass<*> -> when (rightKey) {
            is KClass<*> -> leftKey.typeParameters.isEmpty() &&
                    rightKey.typeParameters.isEmpty() &&
                    rightKey.isSubclassOf(leftKey)
            is KType -> rightKey.arguments.isEmpty() &&
                    leftKey.typeParameters.isEmpty() &&
                    (rightKey.classifier as? KClass<*>)
                            ?.isSubclassOf(leftKey) == true
            else -> false
        }
        else -> false
    }
}

val KType.isOpenGeneric: Boolean
    get() = classifier is KTypeParameter ||
                arguments.any { it.type?.isOpenGeneric == true }

val KType.componentType: KType?
    get() = if (isSubtypeOf(COLLECTION_TYPE))
        arguments.single().type else this

inline fun <reified T> KAnnotatedElement.getTaggedAnnotations() =
        annotations.flatMap { it.annotationClass.annotations }
                   .filterIsInstance<T>()

val COLLECTION_TYPE = getKType<Collection<Any>>()

val PROMISE_TYPE = getKType<Promise<Any>>()