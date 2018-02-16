package com.miruken

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeHelpersTest {
    class Foo
    @Test fun `Identifies open generic type`() {
        var x = listOf(1, 'A', Foo(), "HELLO", 22)
        assertEquals(Int::class.javaObjectType, x[0]::class.java)
        var y = x.filterIsInstance(Int::class.javaObjectType)
        assertTrue { y[1] == 22 }
    }
}
