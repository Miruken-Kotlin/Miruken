package com.miruken.runtime

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeHelpersTest {
    @Test fun `Can check KType for Unit`() {
        assertTrue(typeOf<Unit>().isUnit)
    }

    @Test fun `Non generic types are closed`() {
        assertFalse(typeOf<String>().isOpenGeneric)
    }

    @Test fun `Determines if generic type is closed`() {
        assertFalse(typeOf<List<String>>().isOpenGeneric)
    }

    @Test fun `Obtains component type of collection`() {
        val componentType = typeOf<List<Int>>().componentType
        assertEquals(typeOf<Int>(), componentType)
    }

    @Test fun `Obtains component type of array`() {
        val componentType = typeOf<Array<String>>().componentType
        assertEquals(typeOf<String>(), componentType)
    }
}
