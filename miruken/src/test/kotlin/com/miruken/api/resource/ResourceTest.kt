package com.miruken.api.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.miruken.api.JacksonProvider
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Month
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceTest {
    data class MyResource(
            override val id:         Int?           = null,
            override val rowVersion: ByteArray?     = null,
            override val created:    LocalDateTime? = null,
            override val createdBy:  String?        = null,
            override val modified:   LocalDateTime? = null,
            override val modifiedBy: String?        = null
    ) : Resource<Int> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MyResource

            if (id != other.id) return false
            if (rowVersion != null) {
                if (other.rowVersion == null) return false
                if (!rowVersion.contentEquals(other.rowVersion)) return false
            } else if (other.rowVersion != null) return false
            if (created != other.created) return false
            if (createdBy != other.createdBy) return false
            if (modified != other.modified) return false
            if (modifiedBy != other.modifiedBy) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id ?: 0
            result = 31 * result + (rowVersion?.contentHashCode() ?: 0)
            result = 31 * result + (created?.hashCode() ?: 0)
            result = 31 * result + (createdBy?.hashCode() ?: 0)
            result = 31 * result + (modified?.hashCode() ?: 0)
            result = 31 * result + (modifiedBy?.hashCode() ?: 0)
            return result
        }
    }

    @Test fun `Serializes resource into json`() {
        val resource = MyResource(1, byteArrayOf(1,2,3),
                LocalDateTime.of(2015, Month.JULY, 29, 19, 30, 40), "John Smith",
                LocalDateTime.of(2018, Month.NOVEMBER, 12, 17, 11, 19), "Stan Lee")
        val json     = JacksonProvider.mapper.writeValueAsString(resource)
        assertEquals("{\"id\":1,\"rowVersion\":\"AQID\",\"created\":\"2015-07-29T19:30:40\",\"createdBy\":\"John Smith\",\"modified\":\"2018-11-12T17:11:19\",\"modifiedBy\":\"Stan Lee\"}", json)
    }

    @Test fun `Deserializes resource from json`() {
        val json     = "{\"id\":1,\"rowVersion\":\"AQID\",\"created\":\"2015-07-29T19:30:40\",\"createdBy\":\"John Smith\",\"modified\":\"2018-11-12T17:11:19.00\",\"modifiedBy\":\"Stan Lee\"}"
        val resource = JacksonProvider.mapper.readValue<MyResource>(json)
        assertEquals(1, resource.id)
        assertTrue(byteArrayOf(1,2,3).contentEquals(resource.rowVersion!!))
        assertEquals(LocalDateTime.of(2015, Month.JULY, 29, 19, 30, 40), resource.created)
        assertEquals("John Smith", resource.createdBy)
        assertEquals(LocalDateTime.of(2018, Month.NOVEMBER, 12, 17, 11, 19), resource.modified)
        assertEquals("Stan Lee", resource.modifiedBy)
    }

    @Test fun `Resource equality`() {
        val resource1 = MyResource(1, byteArrayOf(1,2,3),
                LocalDateTime.of(2015, Month.JULY, 29, 19, 30, 40), "John Smith",
                LocalDateTime.of(2018, Month.NOVEMBER, 12, 17, 11, 19), "Stan Lee")
        val resource2 = MyResource(1, byteArrayOf(1,2,3),
                LocalDateTime.of(2015, Month.JULY, 29, 19, 30, 40), "John Smith",
                LocalDateTime.of(2018, Month.NOVEMBER, 12, 17, 11, 19), "Stan Lee")
        assertEquals(resource1, resource2)
    }
}