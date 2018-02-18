package com.miruken.runtime

import org.junit.Test
import kotlin.reflect.KClass
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
        assertEquals(getKType<Set<String>>(), getKType<Set<String>>())
        assertNotEquals(getKType<Set<String>>(), getKType<Set<Int>>())
        assertEquals(getKType<List<*>>(), getKType<List<Any>>())

        var x = getKType<List<String>>().classifier as KClass<*>
        println(x.qualifiedName)
    }
}
