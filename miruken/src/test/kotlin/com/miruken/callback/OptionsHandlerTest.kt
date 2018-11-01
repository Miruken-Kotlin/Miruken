package com.miruken.callback

import com.miruken.typeOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OptionsHandlerTest {
    @Test fun `Ignores unknown options`() {
        val handler = Handler().withOptions(MyOptions())
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(FilterOptions()))
    }

    @Test fun `Matches expected options`() {
        val handler = Handler()
                .withOptions(MyOptions().apply { debug = true })
        val options = MyOptions()
        assertNotEquals(options.debug, true)
        assertEquals(HandleResult.HANDLED, handler.handle(options))
        assertEquals(options.debug, true)
    }

    @Test fun `Matches expected options when composed`() {
        val handler = Handler()
                .withOptions(MyOptions().apply { debug = true })
        val options = MyOptions()
        assertNotEquals(options.debug, true)
        assertEquals(HandleResult.HANDLED, handler.handle(
                Composition(options, typeOf<MyOptions>())))
        assertEquals(options.debug, true)
    }

    class MyOptions : Options<MyOptions>() {
        var debug: Boolean? = null

        override fun mergeInto(other: MyOptions) {
            if (debug != null && other.debug == null)
                other.debug = debug
        }
    }
}