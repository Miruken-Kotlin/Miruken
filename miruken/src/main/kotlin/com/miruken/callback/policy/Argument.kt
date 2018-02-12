package com.miruken.callback.policy

import com.miruken.concurrent.Promise
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf

class Argument(val parameter: KParameter) {

    val parameterClass: KClass<*>?
    val logicalClass:   KClass<*>?
    val isLazy:         Boolean
    val isList:         Boolean
    val isPromise:      Boolean
    val isOptional:     Boolean
    val annotations:    List<Annotation>

    init {
        var type        = parameter.type
        parameterClass  = getClass(type.classifier)
        isOptional      = type.isMarkedNullable
        val lazyType    = extractType(type, Function0::class)
        isLazy          = lazyType != null
        type            = lazyType ?: type
        val promiseType = extractType(type, Promise::class)
        isPromise       = promiseType != null
        type            = promiseType ?: type
        val listType    = extractType(type, List::class)
        isList          = listType != null
        type            = listType ?: type
        logicalClass    = getClass(type.classifier)
        annotations     = parameter.annotations
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