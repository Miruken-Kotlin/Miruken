package com.miruken.callback

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("ReplaceCallWithComparison")
class StringKeyTest {
    @Test fun `Matches other key`() {
        val key = StringKey("abc")
        assertEquals(key, StringKey("abc"))
    }

    @Test fun `Matches other key case sensitive`() {
        val key = StringKey("abc", true)
        assertNotEquals(key, StringKey("AbC"))
    }

    @Test fun `Matches other key case insensitive`() {
        val key = StringKey("abc")
        assertEquals(key, StringKey("ABC"))
    }
}