package com.miruken

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlagsTest {
    sealed class Direction(value: Long) : Flags<Direction>(value) {
        object LEFT  : Direction(1 shl 0)
        object RIGHT : Direction(1 shl 1)
        object UP    : Direction(1 shl 2)
        object DOWN  : Direction(1 shl 3)
        object ALL   : Direction(+(LEFT + RIGHT + UP + DOWN))
    }

    @Test fun `Flags support presence`() {
        assertTrue(Direction.LEFT has Direction.LEFT)
        assertFalse(Direction.LEFT has Direction.DOWN)
        assertTrue(Direction.LEFT + Direction.UP has Direction.UP)
        assertFalse(Direction.LEFT + Direction.UP has Direction.RIGHT)
    }

    @Test fun `Empty flag fails presence`() {
        var flag = -Direction.UP
        flag -= Direction.UP
        assertFalse(flag has flag)
        assertFalse(Direction.DOWN has flag)
    }

    @Test fun `Flags can be combined`() {
        var flag = -Direction.UP
        flag += Direction.DOWN
        assertTrue(flag has Direction.UP)
        assertTrue(flag has Direction.DOWN)
        flag -= Direction.UP
        assertFalse(flag has Direction.UP)
        assertTrue(flag has Direction.DOWN)
        assertEquals(Direction.DOWN, flag)
    }

    @Test fun `Conditional sets flags`() {
        var flag = -Direction.UP
        flag = flag.set(Direction.LEFT)
        assertTrue(flag has Direction.UP)
        assertTrue(flag has Direction.LEFT)
    }

    @Test fun `Conditional unsets flags`() {
        var flag = Direction.UP + Direction.DOWN
        flag = flag.set(Direction.UP, false)
        assertFalse(flag has Direction.UP)
        assertTrue(flag has Direction.DOWN)
    }

    @Test fun `Flags support equality`() {
        val flags1 = -Direction.LEFT
        val flags2 = -Direction.RIGHT
        val flags3 = flags1 + flags2
        assertTrue(Direction.LEFT == flags1)
        assertFalse(Direction.LEFT == flags2)
        assertFalse(Direction.LEFT == flags3)
    }

    @Test fun `Flags support exact equality`() {
        val flags1 = -Direction.LEFT
        val flags2 = -Direction.RIGHT
        val flags3 = flags1 + flags2
        assertTrue(Direction.LEFT === flags1)
        assertFalse(Direction.LEFT === flags2)
        assertFalse(Direction.LEFT === flags3)
    }

    @Test
    fun `Obtains the name of the flag`() {
        val flag = Direction.RIGHT + Direction.DOWN
        assertEquals("LEFT", Direction.LEFT.name)
        assertEquals("RIGHT", Direction.RIGHT.name)
        assertEquals("UP", Direction.UP.name)
        assertEquals("DOWN", Direction.DOWN.name)
        assertEquals("10", flag.name)
    }

    @Test fun `Flags expose numerical value`() {
        assertEquals(0x01, Direction.LEFT.value)
        assertEquals(0x02, Direction.RIGHT.value)
        assertEquals(0x04, Direction.UP.value)
        assertEquals(0x08, Direction.DOWN.value)
    }

    @Test fun `Flags expose numerical value with +`() {
        assertEquals(0x01, +Direction.LEFT)
        assertEquals(0x02, +Direction.RIGHT)
        assertEquals(0x04, +Direction.UP)
        assertEquals(0x08, +Direction.DOWN)
    }

    @Test fun `Retrieves all flags`() {
        val flags = Flags.valuesOf<Direction>()
        assertEquals(5, flags.size)
        assertTrue(flags.containsAll(listOf(
                Direction.LEFT, Direction.RIGHT,
                Direction.UP, Direction.DOWN,
                Direction.ALL)))
    }
}