package com.miruken.callback

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OptionsTest {
    data class MyOptions(
            var debug: Boolean? = null,
            var level: Int? = null
    ) : Options<MyOptions>() {
        override fun mergeInto(other: MyOptions) {
            if (other.debug == null) other.debug = debug
            if (other.level == null) other.level = level
        }
    }

    @Test fun `Create an option`() {
        val options = MyOptions(true, 5)
        assertEquals(true, options.debug)
        assertEquals(5, options.level)
    }

    @Test fun `Merges two options`() {
        val options = MyOptions()
        MyOptions(true).mergeInto(options)
        assertEquals(true, options.debug)
        assertNull(options.level)
    }
}