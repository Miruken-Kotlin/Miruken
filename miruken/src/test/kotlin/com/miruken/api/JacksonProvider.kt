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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.miruken.TypeReference
import com.miruken.api.Try
import com.miruken.api.schedule.ScheduleResult
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.temporal.ChronoField
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance

object JacksonProvider {
    private val idToTypeMapping = ConcurrentHashMap<String, JavaType>()
    private val typeToIdMapping = ConcurrentHashMap<Type, String>()

    init {
        register(ScheduleResult)
    }

    val mapper: ObjectMapper by lazy {
        jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(MirukenModule)
    }

    inline fun <reified T: NamedType> register(namedType: T) {
        jacksonTypeRef<T>().type.let {
            (it as? Class<*>)?.enclosingClass ?: it
        }?.also {
            register(namedType.typeName, it)
        }
    }

    inline fun <reified T: Any> register(typeId: String) =
            register(typeId, jacksonTypeRef<T>().type)

    fun register(typeId: String, type: Type) {
        require(typeId.isNotBlank()) {
            "Type Identifier for $type cannot be empty"
        }
        idToTypeMapping[typeId] = TypeFactory.defaultInstance()
                .constructType(type)
        typeToIdMapping[type] = typeId
    }

    object MirukenModule : SimpleModule() {
        init {
            setMixInAnnotation(
                    KType::class.java,
                    IgnoreMixin::class.java)

            setMixInAnnotation(
                    TypeReference::class.java,
                    IgnoreMixin::class.java)

            setMixInAnnotation(
                    NamedType::class.java,
                    NamedTypeMixin::class.java)

            addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer())
            addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())

            addSerializer(Throwable::class.java, ThrowableSerializer())
            addDeserializer(Throwable::class.java, ThrowableDeserializer())

            addSerializer(Try::class.java, TrySerializer())
            addDeserializer(Try::class.java, TryDeserializer())
        }

        @JsonIgnoreType
        interface IgnoreMixin

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
            override fun idFromValue(value: Any) =
                    idFromValueAndType(value, value::class.java)

            override fun idFromValueAndType(
                    value:         Any,
                    suggestedType: Class<*>?
            ): String? {
                val typeName = (value as? NamedType)?.typeName
                if (!typeName.isNullOrBlank()) {
                    registerResponseTypeId(value)
                    return typeName
                }
                return suggestedType?.let { typeToIdMapping[it] } ?:
                error("${value::class} requires a valid typeName")
            }

            override fun typeFromId(
                    context: DatabindContext,
                    id:      String
            ) = idToTypeMapping[id]

            override fun getMechanism() = JsonTypeInfo.Id.NAME

            private fun registerResponseTypeId(value: Any) {
                (value::class.allSupertypes.firstOrNull {
                    it.classifier == Request::class
                }) ?.arguments?.first()?.type?.also { responseType ->
                    (responseType.classifier as? KClass<*>)?.also { nt ->
                        val companion = nt.companionObjectInstance as? NamedType
                        companion?.typeName?.takeUnless { it.isBlank() }?.also {
                            idToTypeMapping.getOrPut(it) {
                                val javaType = nt.java
                                typeToIdMapping[nt.java] = it
                                TypeFactory.defaultInstance().constructType(javaType)
                            }
                        }
                    }
                }
            }
        }

        private val DATETIME_FORMATTER =
                DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                        .toFormatter()

        class LocalDateTimeSerializer : StdSerializer<LocalDateTime>(
                LocalDateTime::class.java
        ) {
            override fun serialize(
                    value:    LocalDateTime,
                    gen:      JsonGenerator,
                    provider: SerializerProvider) {
                gen.writeString(DATETIME_FORMATTER.format(value))
            }
        }

        class LocalDateTimeDeserializer : StdDeserializer<LocalDateTime>(
                LocalDateTime::class.java
        ) {
            override fun deserialize(
                    parser: JsonParser,
                    ctxt:   DeserializationContext
            ): LocalDateTime {
               return LocalDateTime.parse(parser.text, DATETIME_FORMATTER)
            }
        }

        class TrySerializer : StdSerializer<Try<*, *>>(
                Try::class.java
        ) {
            override fun serialize(
                    value:    Try<*, *>?,
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

        class TryDeserializer : StdDeserializer<Try<*, *>>(
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
            ): Try<*, *>? {
                val tree    = parser.codec.readTree<JsonNode>(parser)
                val isError = tree.get("isLeft")?.booleanValue()
                        ?: throw JsonMappingException.from(parser,
                                "Expected field 'isLeft' was missing")

                val value   = tree.get("value")
                        ?: throw JsonMappingException.from(parser,
                                "Expected field 'value' was missing")

                return when (isError) {
                    true -> Try.error(value.traverse(parser.codec)
                            .readValueAs(_tryType.containedType(0).rawClass)
                            as Throwable)
                    false -> Try.result(value.traverse(parser.codec)
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
                } ?: throw JsonMappingException.from(parser,
                        "Expected field 'message' was missing")
            }
        }
    }
}