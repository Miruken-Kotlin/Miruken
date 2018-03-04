package com.miruken.map

import com.miruken.assertAsync
import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.protocol.proxy
import com.miruken.runtime.typeOf
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.InvalidClassException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

@Suppress("UNUSED_PARAMETER")
class MappingTest {
    @Rule
    @JvmField val testName = TestName()

    private lateinit var _handler: Handling

    @Before
    fun setup() {
        _handler = MappingHandler()
    }

    @Test fun `Performs mapping implicitly`() {
        val entity = PlayerEntity(1, "Tim Howard")
        val data   = (EntityMapping() + _handler)
                .proxy<Mapping>().map<PlayerData>(entity)
        assertEquals(entity.id, data!!.id)
        assertEquals(entity.name, data.name)
    }

    @Test fun `Performs mapping implicitly async`() {
        val entity = PlayerEntity(1, "Tim Howard")
        assertAsync(testName) { done ->
            (EntityMapping() + _handler).proxy<Mapping>()
                    .mapAsync<PlayerData>(entity) then {
                assertEquals(entity.id, it!!.id)
                assertEquals(entity.name, it.name)
                done()
            }
        }
    }

    @Test fun `Performs mapping explicitly`() {
        val handler = ExplicitMapping() + _handler
        val player  = PlayerData(3, "Franz Beckenbauer")
        val json    = handler.proxy<Mapping>().map<String>(
                player, "application/json")
        assertEquals("{id:3,name:'Franz Beckenbauer'}", json)
        val data    = handler.proxy<Mapping>().map<PlayerData>(
                json!!, "application/json")
        assertEquals(3, data!!.id)
        assertEquals("Franz Beckenbauer", player.name)
    }

    @Test fun `Performs mapping explicitly async`() {
        val handler = ExplicitMapping() + _handler
        val player  = PlayerData(3, "Franz Beckenbauer")
        assertAsync(testName) { done ->
            handler.proxy<Mapping>().mapAsync<String>(
                    player, "application/json") then {
                assertEquals("{id:3,name:'Franz Beckenbauer'}", it)
                val data = handler.proxy<Mapping>().map<PlayerData>(
                        it!!, "application/json")
                assertEquals(3, data!!.id)
                assertEquals("Franz Beckenbauer", player.name)
                done()
            }
        }
    }

    @Test fun `Performs mapping on existing instance`() {
        val entity = PlayerEntity(9, "Diego Maradona")
        val player = PlayerData(9, "")
        val data   = (EntityMapping() + _handler)
                .proxy<Mapping>().mapInto(entity, player)
        assertSame(player, data)
        assertEquals(entity.id, data!!.id)
        assertEquals(entity.name, data.name)
    }

    @Test fun `Rejects missing mapping`() {
        val entity = PlayerEntity(1, "Tim Howard")
        assertFailsWith(NotHandledException::class) {
            _handler.proxy<Mapping>().map<PlayerData>(entity)
        }
    }

    @Test fun `Rejects missing mapping async`() {
        val entity = PlayerEntity(1, "Tim Howard")
        assertFailsWith(NotHandledException::class) {
            _handler.proxy<Mapping>().mapAsync<PlayerData>(entity)
        }
    }

    @Test fun `Performs mapping to map of key values`() {
        val entity = PlayerEntity(1, "Marco Royce")
        val data   = (EntityMapping() + _handler)
                .proxy<Mapping>().map<Map<String, Any>>(entity)
        assertEquals(2, data!!.size)
        assertEquals(1, data["id"])
        assertEquals("Marco Royce", data["name"])
    }

    @Ignore("Type erasure prevent check")
    @Test fun `Performs mapping from map of key values`() {
        val data   = mapOf("id" to 1, "name" to "Geroge Best")
        val entity = (EntityMapping() + _handler)
                .proxy<Mapping>().map<PlayerEntity>(data)
        assertEquals(1, entity!!.id)
        assertEquals("Marco Royce", entity.name)
    }

    @Test fun `Performs mapping resolving`() {
        HandlerDescriptor.getDescriptorFor<ExplicitMapping>()
        val exception = IllegalArgumentException("Value is bad")
        val value     = (ExceptionMapping() + _handler).resolving
                .proxy<Mapping>().map<Any>(exception)
        assertEquals("java.lang.IllegalArgumentException: Value is bad", value)
    }

    @Test fun `Performs mapping on simple results`() {
        HandlerDescriptor.getDescriptorFor<ExplicitMapping>()
        val exception = IllegalStateException("Close not found")
        var value     = (ExceptionMapping() + _handler).resolving
                .proxy<Mapping>().map<Any>(exception)
        assertEquals(500, value)
        value     = (ExceptionMapping() + _handler).resolving
                .proxy<Mapping>().map(IllegalAccessException(
                        "Operation not allowed"))
        assertEquals("Operation not allowed", value)
    }

    @Test fun `Maps to null if best effort`() {
        HandlerDescriptor.getDescriptorFor<ExplicitMapping>()
        val value     = (ExceptionMapping() + _handler).resolving.bestEffort
                .proxy<Mapping>().map<Int>(InvalidClassException(""))
        assertNull(value)
    }

    @Test fun `Maps to null if best effort async`() {
        HandlerDescriptor.getDescriptorFor<ExplicitMapping>()
        assertAsync { done ->
            (ExceptionMapping() + _handler).resolving.bestEffort
                    .proxy<Mapping>()
                    .mapAsync<Int>(InvalidClassException("")) then {
                assertNull(it)
                done()
            }
        }
    }

    open class Entity(var id: Int)

    class PlayerEntity(id: Int, var name: String) : Entity(id)

    data class PlayerData(var id: Int, var name: String)

    class EntityMapping : Handler() {
        @Maps
        fun mapToPlayerData(
                entity:  PlayerEntity,
                mapping: MapFrom
        ): PlayerData {
            val instance = mapping.target as? PlayerData
            return instance?.let {
                instance.id = entity.id
                instance.name = entity.name
                instance
            } ?: PlayerData(entity.id, entity.name)
        }

        @Maps
        fun mapToPlayerDictionary(
                entity: PlayerEntity
        ) = mapOf("id"   to entity.id,
                  "name" to entity.name)

        @Maps
        fun mapFromPlayerDictionary(
                keyValues: Map<String, Any>
        ): PlayerEntity {
            val id   = keyValues["id"] as? Int
            val name = keyValues["name"] as? String
            return if (id != null && name != null)
                PlayerEntity(id, name)
            else notHandled()
        }

        @Maps
        fun mapFromPlayerKeyValues(
                keyValues: List<Map.Entry<String, Any>>
        ): PlayerEntity {
            var id: Int? = null
            var name: String? = null
            for (keyValue in keyValues) {
                when (keyValue.key) {
                    "id" -> id = keyValue.value as? Int
                    "name" -> name = keyValue.value as String?
                }
            }
            return if (id != null && name != null)
                PlayerEntity(id, name)
            else notHandled()
        }
    }

    class ExceptionMapping : Handler() {
        @Maps
        fun mapArgumentException(e: IllegalArgumentException) = e.toString()

        @Maps
        fun mapArgumentException(e: IllegalStateException) = 500

        @Maps
        fun mapException(e: Throwable) = e.message ?: "Unknown exception"
    }

    class OpenMapping : Handler() {
        @Maps
        fun map(mapping: MapFrom) =
                (mapping.source as? PlayerEntity)?.takeIf {
                    mapping.targetType == typeOf<PlayerData>()
                }?.let { PlayerData(it.id, it.name) } ?: notHandled()
    }

    class ExplicitMapping : Handler() {
        @Maps @Format("application/json")
        fun toJsonPlayer(player: PlayerData) =
                "{id:${player.id},name:'${player.name}'}"

        @Maps @Format("application/json")
        fun fromJsonPlayer(json: String): PlayerData {
            var id:   Int?    = null
            var name: String? = null
            json.split('{', '}', ',').forEach {
                val keyValue = it.split(':')
                when (keyValue[0].toLowerCase()) {
                    "id"   -> id = Integer.parseInt(keyValue[1])
                    "name" -> name = keyValue[1]
                }
            }
            return if (id != null && name != null)
                PlayerData(id!!, name!!)
            else notHandled()
        }
    }
}