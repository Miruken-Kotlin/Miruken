package com.miruken.runtime

import org.junit.Test
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RuntimeHelpersTest {
    class Foo

    @Test fun `Identifies open generic type`() {
        var x = listOf(1, 'A', Foo(), "HELLO", 22)
        assertEquals(Int::class.javaObjectType, x[0]::class.java)
        var y = x.filterIsInstance(Int::class.javaObjectType)
        assertTrue { y[1] == 22 }
    }

    @Test fun `Can obtain KType from reified T`() {
        var type: KType = getKType<Set<String?>>()
        assertEquals(getKType<Set<String>>(), getKType<Set<String>>())
        assertNotEquals(getKType<Set<String>>(), getKType<Set<Int>>())
        assertEquals(getKType<List<*>>(), getKType<List<Any>>())
    }

    @Test fun `Can determine KType covariance`() {
        assertTrue { getKType<MutableList<Int>>()
                .isSubtypeOf(getKType<MutableList<*>>()) }
    }

    @Test fun `Can determine KType contravariance`() {
        val c1: Comparable<Any> = object : Comparable<Any> {
            override fun compareTo(other: Any): Int {
                TODO("not implemented")
            }
        }
        val c2: Comparable<Any> = c1
        assertTrue { getKType<Comparable<String>>()
                .isSubtypeOf(getKType<Comparable<*>>()) }
    }
}
