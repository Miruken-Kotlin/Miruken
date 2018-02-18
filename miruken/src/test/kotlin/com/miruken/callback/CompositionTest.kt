package com.miruken.callback

import org.junit.Assert.*
import org.junit.Test

class CompositionTest {
    class Foo

    @Test fun `Extracts the composed value statically`() {
        val x: List<Foo> = listOf(Foo())
        val y = x as List<Int>

        val composed = Composition(listOf(Foo()))
        //assertNull(Composition.get<List<String>>(composed))
        //assertNotNull(Composition.get<List<Foo>>(composed))
    }

    @Test fun `Extracts the composed value dynamically`() {
        val composed = Composition(Foo())
        assertNull(Composition.get(composed, Int::class))
        assertNotNull(Composition.get(composed, Foo::class))
    }
}