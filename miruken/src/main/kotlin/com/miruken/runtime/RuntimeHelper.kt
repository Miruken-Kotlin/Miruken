package com.miruken.runtime

import com.miruken.concurrent.Promise
import com.miruken.toKType
import com.miruken.typeOf
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

fun isCompatibleWith(
        leftSide:     Any,
        rightSide:    Any,
        typeBindings: MutableMap<KTypeParameter, KType>? = null
): Boolean {
    return when (leftSide) {
        is KType -> when (rightSide) {
            is KType -> when {
                leftSide == rightSide -> true
                rightSide.classifier is KTypeParameter -> {
                    val typeParam = rightSide.classifier as KTypeParameter
                    typeParam.upperBounds.any {
                        isCompatibleWith(it, leftSide, typeBindings).also {
                            if (it && typeBindings?.containsKey(typeParam) == false)
                                typeBindings[typeParam] = leftSide
                        }
                    }
                }
                leftSide.classifier is KTypeParameter -> {
                    val typeParam = leftSide.classifier as KTypeParameter
                    typeParam.upperBounds.any {
                        (it.isSupertypeOf(rightSide) ||
                        isCompatibleWith(it, rightSide, typeBindings)).also {
                            if (it && typeBindings?.containsKey(typeParam) == false)
                                typeBindings[typeParam] = rightSide
                        }
                    }
                }
                rightSide.isOpenGeneric ->
                    verifyOpenConformance(rightSide, leftSide, typeBindings) ||
                    (leftSide.isOpenGeneric &&
                            verifyOpenConformance(leftSide, rightSide, typeBindings))
                leftSide.isOpenGeneric ->
                    verifyOpenConformance(leftSide, rightSide, typeBindings)
                else -> rightSide.isSubtypeOf(leftSide)
            }
            is KClass<*> -> {
                leftSide.jvmErasure.isSuperclassOf(rightSide) &&
                        (leftSide.arguments.isEmpty() ||
                        leftSide.isOpenGeneric)
            }
            is Class<*> -> {
                leftSide.jvmErasure.java.isAssignableFrom(rightSide) &&
                        (leftSide.arguments.isEmpty() ||
                                leftSide.isOpenGeneric)
            }
            else -> {
                val rightClass = rightSide::class
                leftSide.jvmErasure.isSuperclassOf(rightClass) &&
                        (leftSide.arguments.isEmpty() ||
                        leftSide.isOpenGeneric)
            }
        }
        is KClass<*> -> when (rightSide) {
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

private fun verifyOpenConformance(
        openType:   KType,
        otherType:  KType,
        parameters: MutableMap<KTypeParameter, KType>?
): Boolean {
    return (openType.classifier as? KClass<*>)?.let { openClass ->
        val other       = otherType.classifier
        val conformance = when (other) {
            is KClass<*> -> when (openClass) {
                other -> otherType
                else -> other.allSupertypes.firstOrNull {
                    it.classifier == openType.classifier }
            }
            is KTypeParameter -> other.upperBounds.firstOrNull {
                it.classifier == openType.classifier
            }
            else -> null
        }
        conformance?.arguments?.zip(openType.arguments
                .zip(openClass.typeParameters)) { ls, rs ->
            when {
                ls.type == null -> true /* Star */
                rs.first.type == null -> true /* Star */
                ls.type!!.isOpenGeneric ||
                rs.first.type!!.isOpenGeneric ->
                    isCompatibleWith(ls.type!!, rs.first.type!!,
                            parameters)
                rs.second.variance == KVariance.IN ->
                    ls.type!!.isSubtypeOf(rs.first.type!!)
                else ->
                    rs.first.type!!.isSubtypeOf(ls.type!!)
            }
        }?.all { it }
    } ?: false
}

fun Iterable<*>.filterIsAssignableTo(key: Any) =
        filterIsAssignableTo(ArrayList(), key)

fun Iterable<*>.filterIsAssignableTo(
        destination: ArrayList<Any>, key: Any
): MutableList<Any> {
    filter { isCompatibleWith(key, it!!) }
            .mapTo(destination) { it!! }
    return destination
}

fun <T> List<T>.normalize() =
        if (isEmpty()) emptyList() else this

val KType.isAny     get() = classifier == Any::class
val KType.isUnit    get() = classifier == Unit::class
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


val KType.isGeneric: Boolean
    get() = classifier is KTypeParameter || arguments.isNotEmpty()

val KType.isOpenGeneric: Boolean
    get() = classifier is KTypeParameter ||
                arguments.any { it.type?.isOpenGeneric == true }

val KType.isOpenGenericDefinition: Boolean
    get() = arguments.isNotEmpty() && arguments.all {
        (it.type?.classifier as? KTypeParameter)
            ?.upperBounds?.all { it == ANY_TYPE } == true }

inline val KClass<*>.isGeneric
    get() = typeParameters.isNotEmpty()

val KType.allInterfaces: Set<KType> get() =
    (classifier as? KClass<*>)?.allInterfaces
            ?: throw IllegalArgumentException(
                    "KType does not represent a class or interface")

val KClass<*>.allInterfaces: Set<KType> get() =
    allSupertypes.filter {
            (it.classifier as KClass<*>).java.isInterface }
            .toHashSet()

val KType.allTopLevelInterfaces : Set<KType> get() =
    (classifier as? KClass<*>)?.allTopLevelInterfaces
            ?: throw IllegalArgumentException(
                    "KType does not represent a class or interface")

val KClass<*>.allTopLevelInterfaces : Set<KType> get() {
    val allInterfaces = allInterfaces
    return allInterfaces.filter { iface ->
        allInterfaces.all { iface === it || !it.isSubtypeOf(iface) }
    }.toHashSet()
}

fun KType.isTopLevelInterfaceOf(type: KType): Boolean =
        type.allTopLevelInterfaces.contains(this)

inline fun <reified T: Any> KType.isTopLevelInterfaceOf() =
        typeOf<T>().allTopLevelInterfaces.contains(this)

fun KType.isTopLevelInterfaceOf(clazz: KClass<*>) =
        clazz.allTopLevelInterfaces.contains(this)

inline fun <reified T: Annotation> KAnnotatedElement
        .getTaggedAnnotations() = annotations.mapNotNull {
            val tags = it.annotationClass.annotations.filterIsInstance<T>()
            if (tags.isNotEmpty()) it to tags else null
        }

inline fun <reified T: Annotation> AnnotatedElement
        .getTaggedAnnotations() = annotations.mapNotNull {
    val tags = it.annotationClass.annotations.filterIsInstance<T>()
    if (tags.isNotEmpty()) it to tags else null
}

inline fun <reified T: Annotation> KAnnotatedElement
        .getFirstTaggedAnnotation() = getTaggedAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

inline fun <reified T: Annotation> AnnotatedElement
        .getFirstTaggedAnnotation() = getTaggedAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

fun KClass<*>.matchMethod(method: Method): Method? {
    return try {
        java.getMethod(method.name, *method.parameterTypes)
    } catch (e: NoSuchMethodError) {
        null
    }
}

fun Collection<*>.toTypedArray(componentType: KClass<*>) =
        toTypedArray(componentType.javaObjectType)

fun Collection<*>.toTypedArray(componentType: Class<*>): Any {
    val array = java.lang.reflect.Array.newInstance(componentType, size)
    forEachIndexed { index, element ->
        java.lang.reflect.Array.set(array, index, element) }
    return array
}

val ANY_STAR        = Any::class.starProjectedType
val ANY_TYPE        = typeOf<Any>().withNullability(true)
val COLLECTION_TYPE = typeOf<Collection<Any>>().withNullability(true)
val PROMISE_TYPE    = typeOf<Promise<Any>>().withNullability(true)