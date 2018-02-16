package com.miruken.callback

import org.junit.Assert.*
import org.junit.Test

class CompositionTest {
    class Foo

    @Test fun `Extracts the composed value statically`() {
        val composed = Composition(Foo())
        assertNull(Composition.get<String>(composed))
        assertNotNull(Composition.get<Foo>(composed))
    }

    @Test fun `Extracts the composed value dynamically`() {
        val composed = Composition(Foo())
        assertNull(Composition.get(composed, Int::class))
        assertNotNull(Composition.get(composed, Foo::class))
    }
}