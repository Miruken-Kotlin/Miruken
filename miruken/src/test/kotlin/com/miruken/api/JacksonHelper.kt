package com.miruken.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KType

typealias JacksonNamedType =
        com.fasterxml.jackson.databind.jsontype.NamedType

object JacksonHelper {
    val json: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .addMixIn(NamedType::class.java, NamedTypeMixin::class.java)
            .addMixIn(KType::class.java, IgnoreTypeMixin::class.java)
            .registerModule(JavaTimeModule())

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
        override fun idFromValue(value: Any): String? =
            when (value) {
                is NamedType -> value.typeName
                else -> idFromBaseType()
            }

        override fun idFromValueAndType(
                value: Any,
                suggestedType: Class<*>?) = idFromValue(value)

        // TODO: override typeFromId to map .Net $type to java type

        override fun getMechanism() = JsonTypeInfo.Id.CUSTOM
    }
}