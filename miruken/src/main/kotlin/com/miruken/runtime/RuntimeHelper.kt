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
                    checkOpenConformance(rightSide, leftSide, typeBindings) ||
                    (leftSide.isGeneric &&
                            checkOpenConformance(leftSide, rightSide, typeBindings))
                leftSide.isOpenGeneric ->
                    checkOpenConformance(leftSide, rightSide, typeBindings)
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

fun checkOpenConformance(
        openType:   KType,
        otherType:  KType,
        parameters: MutableMap<KTypeParameter, KType>? = null
): Boolean {
    return (openType.classifier as? KClass<*>)?.let { openClass ->
        val other       = otherType.classifier
        val conformance = when (other) {
            is KClass<*> -> when (openClass) {
                other -> otherType.arguments.map { it.type }
                else -> other.allSupertypes.firstOrNull {
                    it.classifier == openClass
                }?.let { conform ->
                    val otherArgs   = otherType.arguments
                    val otherParams = other.typeParameters
                    conform.arguments.map { arg ->
                        val classifier = arg.type?.classifier
                        arg.type.takeIf { classifier is KClass<*> }
                                ?: otherParams.indexOf(classifier)
                                        .takeIf { it >= 0 }
                                        ?.let { otherArgs[it].type }
                                ?: return false
                    }
                }
            }
            is KTypeParameter ->
                other.upperBounds.firstOrNull {
                    it.classifier == openClass
                }?.arguments?.map { it.type }
            else -> null
        }
        conformance?.zip(
                openType.arguments.map { it.type }.zip(
                openClass.typeParameters)) { ls, rs ->
            when {
                ls == null -> true /* Star */
                rs.first == null -> true /* Star */
                ls.isOpenGeneric || rs.first!!.isOpenGeneric ->
                    isCompatibleWith(ls, rs.first!!, parameters)
                rs.second.variance == KVariance.IN ->
                    ls.isSubtypeOf(rs.first!!)
                else ->
                    rs.first!!.isSubtypeOf(ls)
            }
        }?.all { it }
    } ?: false
}

fun KType.mapOpenParameters(
        closedType:    KType,
        typeBindings:  MutableMap<KTypeParameter, KType>? = null,
        skipOpenCheck: Boolean = false
): MutableMap<KTypeParameter, KType>? {
    if (skipOpenCheck || (isOpenGeneric && !closedType.isGeneric)) {
        val closed   = closedType.classifier
        val openType = when (classifier) {
            closed -> this
            else -> (classifier as? KClass<*>)
                    ?.allSupertypes?.firstOrNull {
                        it.classifier == closed
                    } ?: this
        }
        val bindings by lazy(LazyThreadSafetyMode.NONE) {
            typeBindings ?: mutableMapOf() }
        (openType.classifier as? KClass<*>)?.let { openClass ->
            val conformance = when (closed) {
                is KClass<*> -> when (openClass) {
                    closed -> closedType.arguments.map { it.type }
                    else -> closed.allSupertypes.firstOrNull {
                        it.classifier == openClass
                    }?.let { conform ->
                        val otherArgs   = closedType.arguments
                        val otherParams = closed.typeParameters
                        conform.arguments.map { arg ->
                            val classifier = arg.type?.classifier
                            arg.type.takeIf { classifier is KClass<*> }
                                    ?: otherParams.indexOf(classifier)
                                            .takeIf { it >= 0 }
                                            ?.let { otherArgs[it].type }
                                    ?: return bindings
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
                        isCompatibleWith(ls, rs.first!!, bindings)
                    rs.second.variance == KVariance.IN ->
                        ls.isSubtypeOf(rs.first!!)
                    else ->
                        rs.first!!.isSubtypeOf(ls)
                }
            }?.all { it }
        } ?: (classifier as? KTypeParameter)?.let { p ->
             if (p.upperBounds.all { closedType.isSubtypeOf(it) }) {
                 bindings[p] = closedType
                 return bindings
             }
        }
        return bindings
    }
    return typeBindings
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
    get() = arguments.isNotEmpty() && arguments.all { arg ->
        (arg.type?.classifier as? KTypeParameter)
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
    return allInterfaces.asSequence().filter { iface ->
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
        .getMetaAnnotations() = annotations.mapNotNull {
            val tags = it.annotationClass.annotations.filterIsInstance<T>()
            if (tags.isNotEmpty()) it to tags else null
        }

inline fun <reified T: Annotation> AnnotatedElement
        .getMetaAnnotations() = annotations.mapNotNull {
    val tags = it.annotationClass.annotations.filterIsInstance<T>()
    if (tags.isNotEmpty()) it to tags else null
}

inline fun <reified T: Annotation> KAnnotatedElement
        .getFirstMetaAnnotation() = getMetaAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

inline fun <reified T: Annotation> AnnotatedElement
        .getFirstMetaAnnotation() = getMetaAnnotations<T>()
        .firstOrNull()?.second?.firstOrNull()

fun KClass<*>.matchMethod(method: Method): Method? {
    return try {
        java.getMethod(method.name, *method.parameterTypes)
    } catch (e: NoSuchMethodError) {
        null
    }
}

val KCallable<*>.isInstanceCallable: Boolean get() =
        parameters.takeIf { it.isNotEmpty() }
                ?.get(0)?.kind == KParameter.Kind.INSTANCE

fun Collection<*>.toTypedArray(componentType: KClass<*>) =
        toTypedArray(componentType.java)

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