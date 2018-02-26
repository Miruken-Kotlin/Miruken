package com.miruken.callback

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("ReplaceCallWithComparison")
class StringKeyTest {
    @Test fun `Matches other key`() {
        val key = StringKey("abc")
        assertTrue(key.equals(StringKey("abc")))
    }

    @Test fun `Matches other key case sensitive`() {
        val key = StringKey("abc", true)
        assertFalse(key.equals(StringKey("AbC")))
    }

    @Test fun `Matches other key case insensitive`() {
        val key = StringKey("abc")
        assertTrue(key.equals(StringKey("ABC")))
    }
}