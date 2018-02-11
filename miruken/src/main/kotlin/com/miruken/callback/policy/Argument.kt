package com.miruken.callback.policy

import com.miruken.concurrent.Promise
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

class Argument(val parameter: KParameter) {

    val parameterClass: KClass<*>
    val logicalClass:   KClass<*>
    val isLazy:         Boolean
    val isList:         Boolean
    val isPromise:      Boolean
    val isOptional:     Boolean
    val annotations:    List<Annotation>

    init {
        var type        = parameter.type
        parameterClass  = type.classifier as KClass<*>
        isOptional      = type.isMarkedNullable
        val lazyType    = extract(type, Function0::class)
        isLazy          = lazyType != null
        type            = lazyType ?: type
        val promiseType = extract(type, Promise::class)
        isPromise       = promiseType != null
        type            = promiseType ?: type
        val listType    = extract(type, List::class)
        isList          = listType != null
        type            = listType ?: type
        logicalClass    = type.classifier as KClass<*>
        annotations     = parameter.annotations
    }

    private fun extract(type: KType, criteria: KClass<*>) : KType? {
        return (type.classifier as? KClass<*>)?.let {
            if (it.isSubclassOf(criteria))
                 type.arguments.firstOrNull()?.type
            else null
        }
    }
}