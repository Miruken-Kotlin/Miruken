package com.miruken.callback.policy

import com.miruken.concurrent.Promise
import com.miruken.isOpenGeneric
import com.miruken.set
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf

class Argument(val parameter: KParameter) {

    val parameterClass: KClass<*>?
    val logicalClass:   KClass<*>?
    val logicalType:    KType
    val flags:          ArgumentFlags

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations

    init {
        var type        = parameter.type
        parameterClass  = getClass(type.classifier)
        val lazyType    = extractType(type, Function0::class)
        type            = lazyType ?: type
        val promiseType = extractType(type, Promise::class)
        type            = promiseType ?: type
        val listType    = extractType(type, List::class)
        logicalType     = listType ?: type
        logicalClass    = getClass(logicalType.classifier)
        flags           = ArgumentFlags.NONE
            .set(ArgumentFlags.OPEN, parameterType.isOpenGeneric)
            .set(ArgumentFlags.OPTIONAL, parameterType.isMarkedNullable)
            .set(ArgumentFlags.LAZY, lazyType != null)
            .set(ArgumentFlags.PROMISE, promiseType != null)
            .set(ArgumentFlags.LIST, listType != null)
    }

    private fun getClass(classifier: KClassifier?) : KClass<*>? {
        return classifier as? KClass<*> ?:
            (classifier as? KTypeParameter)?.let {
                it.upperBounds.firstOrNull()?.classifier as? KClass<*>
        }
    }

    private fun extractType(type: KType, criteria: KClass<*>) : KType? {
        return (type.classifier as? KClass<*>)?.let {
            if (it.isSubclassOf(criteria))
                 type.arguments.firstOrNull()?.type
            else null
        }
    }
}