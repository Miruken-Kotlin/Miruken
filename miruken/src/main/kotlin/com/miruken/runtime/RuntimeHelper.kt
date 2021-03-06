package com.miruken.runtime

import com.miruken.TypeReference
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
                    typeParam.upperBounds.any { bound ->
                        isCompatibleWith(bound, leftSide, typeBindings).also {
                            if (it && typeBindings?.containsKey(typeParam) == false)
                                typeBindings[typeParam] = leftSide
                        }
                    }
                }
                leftSide.classifier is KTypeParameter -> {
                    val typeParam = leftSide.classifier as KTypeParameter
                    typeParam.upperBounds.any { bound ->
                        (bound.isSupertypeOf(rightSide) ||
                        isCompatibleWith(bound, rightSide, typeBindings)).also {
                            if (it && typeBindings?.containsKey(typeParam) == false)
                                typeBindings[typeParam] = rightSide
                        }
                    }
                }
                rightSide.isOpenGeneric ->
                    rightSide.checkOpenConformance(
                            leftSide, typeBindings, true) ||
                    (leftSide.isGeneric && leftSide.checkOpenConformance(
                            rightSide, typeBindings, true))
                leftSide.isOpenGeneric ->
                    leftSide.checkOpenConformance(
                            rightSide, typeBindings, true)
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
            is TypeReference ->
                isCompatibleWith(leftSide, rightSide.kotlinType)
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
            is TypeReference ->
                isCompatibleWith(leftSide, rightSide.kotlinType)
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
            is TypeReference ->
                isCompatibleWith(leftSide, rightSide.kotlinType)
            else -> {
                val rightClass = rightSide::class
                leftSide.isAssignableFrom(rightClass.java)
            }
        }
        is TypeReference ->
            isCompatibleWith(leftSide.kotlinType, rightSide)
        else -> false
    }
}

fun KType.checkOpenConformance(
        closedType:    KType,
        typeBindings:  MutableMap<KTypeParameter, KType>? = null,
        skipOpenCheck: Boolean = false
): Boolean {
    if (skipOpenCheck || (isOpenGeneric && !closedType.isOpenGeneric)) {
        val closed   = closedType.classifier
        val openType = if (classifier != closed) {
            (classifier as? KClass<*>)?.allSupertypes?.firstOrNull {
                it.classifier == closed
            } ?: this
        } else this
        (openType.classifier as? KClass<*>)?.let { openClass ->
            val conformance = when (closed) {
                is KClass<*> -> when (openClass) {
                    closed -> closedType.arguments.map { it.type }
                    else -> closed.allSupertypes.firstOrNull {
                        it.classifier == openClass
                    }?.let { conform ->
                        val closedArgs   = closedType.arguments
                        val closedParams = closed.typeParameters
                        conform.arguments.map { arg ->
                            val classifier = arg.type?.classifier
                            arg.type.takeIf { classifier is KClass<*> }
                                    ?: closedParams.indexOf(classifier)
                                            .takeIf { it >= 0 }
                                            ?.let { closedArgs[it].type }
                                    ?: return false
                        }
                    }
                }
                is KTypeParameter ->
                    closed.upperBounds.firstOrNull {
                        it.classifier == openClass
                    }?.arguments?.map { it.type }
                else -> null
            }
            conformance?.zip(
                    arguments.map { it.type }.zip(
                            openClass.typeParameters)) { ls, rs ->
                when {
                    ls == null -> true /* Star */
                    rs.first == null -> true /* Star */
                    ls.isOpenGeneric || rs.first!!.isOpenGeneric ->
                        isCompatibleWith(ls, rs.first!!, typeBindings)
                    rs.second.variance == KVariance.IN ->
                        ls.isSubtypeOf(rs.first!!)
                    else ->
                        rs.first!!.isSubtypeOf(ls)
                }
            }?.all { it }?.also { return it }
        } ?: (classifier as? KTypeParameter)?.let { p ->
             if (p.upperBounds.all { closedType.isSubtypeOf(it) }) {
                 if (typeBindings != null) {
                     typeBindings[p] = closedType
                     return true
                 }
             }
        }
    }
    return false
}

fun KType.closeType(
        typeBindings:  Map<KTypeParameter, KType>,
        skipOpenCheck: Boolean = false
): KType? = when {
    skipOpenCheck || isOpenGeneric -> {
        (classifier as? KTypeParameter)?.let {
            typeBindings[it] ?: return null
        } ?: jvmErasure.createType(arguments.map { arg ->
                (arg.type?.classifier as? KTypeParameter)?.let {
                    KTypeProjection(arg.variance, typeBindings[it] ?:
                    return null)
            } ?: KTypeProjection(arg.variance, arg.type)
        })
    }
    else -> this
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
        isSubtypeOf(TypeReference.COLLECTION_TYPE) -> /* Nothing??? */
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
    get() = arguments.isNotEmpty() && arguments.all { arg ->
        (arg.type?.classifier as? KTypeParameter)
            ?.upperBounds?.all { it == TypeReference.ANY_TYPE } == true }

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
    return allInterfaces.asSequence().filter { iface ->
        allInterfaces.all { iface === it || !it.isSubtypeOf(iface) }
    }.toHashSet()
}

fun KType.isTopLevelInterfaceOf(type: KType): Boolean =
        type.allTopLevelInterfaces.contains(this)

inline fun <reified T: Any> KType.isTopLevelInterfaceOf() =
        typeOf<T>().kotlinType.allTopLevelInterfaces.contains(this)

fun KType.isTopLevelInterfaceOf(clazz: KClass<*>) =
        clazz.allTopLevelInterfaces.contains(this)

inline fun <reified T: Annotation> KAnnotatedElement
        .getMetaAnnotations(includeDirect: Boolean = true) =
        annotations.mapNotNull {
            if (it is T) {
                if (includeDirect) { it to listOf(it) } else null
            } else {
                val tags = it.annotationClass.annotations.filterIsInstance<T>()
                if (tags.isNotEmpty()) it to tags else null
            }
        }

inline fun <reified T: Annotation> AnnotatedElement
        .getMetaAnnotations(includeDirect: Boolean = true) =
        annotations.mapNotNull {
            if (it is T) {
                if (includeDirect) { it to listOf(it) } else null
            } else {
                val tags = it.annotationClass.annotations.filterIsInstance<T>()
                if (tags.isNotEmpty()) it to tags else null
            }
        }

inline fun <reified T: Annotation> KAnnotatedElement
        .getFirstMetaAnnotation() = getMetaAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

inline fun <reified T: Annotation> AnnotatedElement
        .getFirstMetaAnnotation() = getMetaAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

fun KClass<*>.matchMethod(method: Method): Method? {
    return try {
        java.getMethod(method.name, *method.parameterTypes)?.apply {
            isAccessible = true
        }
    } catch (e: NoSuchMethodException) {
        null
    } catch (e: NoSuchMethodError) {
        null
    }
}

val KCallable<*>.isInstanceCallable get() =
    parameters.firstOrNull()?.kind == KParameter.Kind.INSTANCE

val KCallable<*>.requiresReceiver get() =
    when (parameters.firstOrNull()?.kind) {
        KParameter.Kind.INSTANCE,
        KParameter.Kind.EXTENSION_RECEIVER -> true
        else -> false
    }

fun Collection<*>.toTypedArray(componentType: KClass<*>) =
        toTypedArray(componentType.java)

fun Collection<*>.toTypedArray(componentType: Class<*>): Any {
    val array = java.lang.reflect.Array.newInstance(componentType, size)
    forEachIndexed { index, element ->
        java.lang.reflect.Array.set(array, index, element) }
    return array
}

