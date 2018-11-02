package com.miruken.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.miruken.Either
import com.miruken.fold
import kotlin.reflect.KType

object JacksonHelper {
    val json: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(JavaTimeModule())
            .registerModule(MirukenModule)

    object MirukenModule : SimpleModule() {
        init {
            addSerializer(EitherSerializer())

            setMixInAnnotation(
                    NamedType::class.java,
                    NamedTypeMixin::class.java)

            setMixInAnnotation(
                    KType::class.java,
                    IgnoreKTypeMixin::class.java)
        }

        @JsonIgnoreType
        interface IgnoreKTypeMixin

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
                    value:         Any,
                    suggestedType: Class<*>?
            ) = idFromValue(value)

            // TODO: override typeFromId to map .Net $type to java type

            override fun getMechanism() = JsonTypeInfo.Id.CUSTOM
        }

        class EitherSerializer : StdSerializer<Either<*,*>>(
                Either::class.java
        ), ContextualSerializer {
            override fun createContextual(
                    prov:     SerializerProvider,
                    property: BeanProperty?
            ): JsonSerializer<*> {
                return this
            }

            override fun serialize(
                    value:    Either<*,*>?,
                    gen:      JsonGenerator,
                    provider: SerializerProvider) {
                if (value == null) return
                gen.writeStartObject()
                value.fold({
                    gen.writeBooleanField("isLeft", true)
                    gen.writeObjectField("value", it)
                }, {
                    gen.writeBooleanField("isLeft", false)
                    gen.writeObjectField("value", it)
                })
                gen.writeEndObject()
            }
        }
    }
}