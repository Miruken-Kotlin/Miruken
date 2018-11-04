package com.miruken.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.lang.reflect.Type
import kotlin.reflect.KType

object JacksonHelper {
    private val typeIdMapping = mutableMapOf<String, JavaType>()

    val mapper: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(JavaTimeModule())
            .registerModule(MirukenModule)

    inline fun <reified T> register(typeId: String) =
            register(typeId, jacksonTypeRef<T>().type)

    fun register(typeId: String, type: Type) {
        typeIdMapping[typeId] = TypeFactory.defaultInstance()
                .constructType(type)
    }

    object MirukenModule : SimpleModule() {
        init {
            setMixInAnnotation(
                    NamedType::class.java,
                    NamedTypeMixin::class.java)

            setMixInAnnotation(
                    KType::class.java,
                    IgnoreKTypeMixin::class.java)

            addSerializer(Throwable::class.java, ThrowableSerializer())
            addDeserializer(Throwable::class.java, ThrowableDeserializer())

            addSerializer(Try::class.java, TrySerializer())
            addDeserializer(Try::class.java, TryDeserializer())
        }

        @JsonIgnoreType
        interface IgnoreKTypeMixin

        @JsonTypeInfo(
                use      = JsonTypeInfo.Id.NAME,
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

            override fun typeFromId(
                    context: DatabindContext,
                    id:      String?
            ) = typeIdMapping[id]

            override fun getMechanism() = JsonTypeInfo.Id.NAME
        }

        class TrySerializer : StdSerializer<Try<*,*>>(
                Try::class.java
        ) {
            override fun serialize(
                    value:    Try<*,*>?,
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

        class TryDeserializer : StdDeserializer<Try<*,*>>(
                Try::class.java
        ), ContextualDeserializer {
            private lateinit var _tryType: JavaType

            override fun createContextual(
                    ctxt:     DeserializationContext,
                    property: BeanProperty?
            ): JsonDeserializer<*> {
                val tryType = ctxt.contextualType ?: property?.type
                        ?: error("Unable to determine Try parameters")
                return TryDeserializer().apply { _tryType = tryType }
            }

            override fun deserialize(
                    parser: JsonParser,
                    ctxt:   DeserializationContext
            ): Try<*,*>? {
                val tree    = parser.codec.readTree<JsonNode>(parser)
                val isError = tree.get("isLeft")?.booleanValue()
                        ?: ctxt.reportInputMismatch(this,
                            "Expected field 'isLeft' was missing")

                val value   = tree.get("value")
                        ?: ctxt.reportInputMismatch(this,
                        "Expected field 'value' was missing")

                return when (isError) {
                    true -> Try.error(value.traverse(parser.codec)
                            .readValueAs(_tryType.containedType(0).rawClass)
                                as Throwable)
                    false ->Try.result(value.traverse(parser.codec)
                            .readValueAs(_tryType.containedType(1).rawClass))
                }
            }
        }

        class ThrowableSerializer : StdSerializer<Throwable>(
                Throwable::class.java
        ) {
            override fun serialize(
                    value:    Throwable?,
                    gen:      JsonGenerator,
                    provider: SerializerProvider) {
                if (value == null) return
                gen.writeStartObject()
                gen.writeStringField("message", value.message)
                gen.writeEndObject()
            }
        }

        class ThrowableDeserializer : StdDeserializer<Throwable>(
                Throwable::class.java
        ) {
            override fun deserialize(
                    parser: JsonParser,
                    ctxt: DeserializationContext
            ): Throwable? {
                val tree = parser.codec.readTree<JsonNode>(parser)
                return tree.get("message")?.textValue()?.let {
                    Exception(it)
                } ?: ctxt.reportInputMismatch<Throwable>(this,
                        "Expected field 'message' was missing")
            }
        }
    }
}