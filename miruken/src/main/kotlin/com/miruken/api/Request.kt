package com.miruken.api

import com.miruken.TypeReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance

interface Request<out TResp: NamedType?> : NamedType

interface MessageWrapper : NamedType {
    val message:     NamedType
    val messageType: TypeReference?
}

interface RequestWrapper<out TResp: NamedType?> : Request<TResp> {
    val request:     Request<TResp>
    val requestType: TypeReference?
}

val Request<*>.responseTypeName get(): String {
    return when (this) {
        is RequestWrapper<*> ->
            request.responseTypeName
        is MessageWrapper ->
            (message as? Request<*>)?.responseTypeName
                ?: error("Unable to determine responseTypeName for request $this::class")
        else -> {
            val requestClass = this::class
            return (RESPONSE_TYPES.getOrPut(requestClass) {
                (requestClass.allSupertypes.firstOrNull {
                    it.classifier == Request::class
                })?.arguments?.first()?.type?.let { rt ->
                    (rt.classifier as? KClass<*>)?.let {
                        (it.companionObjectInstance as? NamedType)?.typeName
                    }
                } ?: ""
            }).takeUnless { it.isNullOrBlank() }
                    ?: error("Unable to determine responseTypeName for request $requestClass")
        }
    }
}


private val RESPONSE_TYPES = ConcurrentHashMap<KClass<*>, String>()