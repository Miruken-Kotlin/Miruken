package com.miruken.api

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance

interface Request<out TResp: NamedType?> : NamedType

val Request<*>.responseTypeName get(): String {
    val requestClass = this::class
    return RESPONSE_TYPES.getOrPut(requestClass) {
        (requestClass.allSupertypes.firstOrNull {
            it.classifier == Request::class
        })?.arguments?.first()?.type?.let { rt ->
            (rt.classifier as? KClass<*>)?.let {
                (it.companionObjectInstance as? NamedType)?.typeName
            }
        }
    } ?: error("Unable to determine responseTypeName for $requestClass")
}

private val RESPONSE_TYPES = ConcurrentHashMap<KClass<*>, String?>()