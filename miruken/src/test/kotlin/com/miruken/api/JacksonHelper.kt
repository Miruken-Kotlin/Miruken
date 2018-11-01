package com.miruken.api

import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KType

object JacksonHelper {
    val json: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .addMixIn(KType::class.java, IgnoreTypeMixin::class.java)
            .addMixIn(NamedType::class.java, NamedTypeMixin::class.java)
            .registerModule(JavaTimeModule())

    @JsonIgnoreType
    abstract class IgnoreTypeMixin

    @JsonPropertyOrder("\$type")
    abstract class NamedTypeMixin {
        @get:JsonProperty("\$type")
        abstract val typeName: String
    }
}