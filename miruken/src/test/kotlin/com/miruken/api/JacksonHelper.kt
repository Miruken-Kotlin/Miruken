package com.miruken.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KType

object JacksonHelper {
    val json: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(JavaTimeModule())
            .registerModule(MirukenModule)

    object MirukenModule : SimpleModule() {
        init {
            setMixInAnnotation(NamedType::class.java,
                    NamedTypeMixin::class.java)
            setMixInAnnotation(KType::class.java,
                    IgnoreTypeMixin::class.java)
        }

        @JsonIgnoreType
        interface IgnoreTypeMixin

        @JsonTypeInfo(
                use      = JsonTypeInfo.Id.CUSTOM,
                include  = JsonTypeInfo.As.PROPERTY,
                property = "\$type")
        @JsonTypeIdResolver(NamedTypeIdResolver::class)
        interface NamedTypeMixin {
            @get:JsonIgnore
            val typeName: String
        }

        object NamedTypeIdResolver : TypeIdResolverBase() {
            override fun idFromValue(value: Any): String? {
                val typeName = (value as? NamedType)?.typeName
                check(!typeName.isNullOrBlank()) {
                    "${value::class.qualifiedName} requires a valid typeName"
                }
                return typeName
            }

            override fun idFromValueAndType(
                    value: Any,
                    suggestedType: Class<*>?) = idFromValue(value)

            // TODO: override typeFromId to map .Net $type to java type

            override fun getMechanism() = JsonTypeInfo.Id.CUSTOM
        }
    }
}