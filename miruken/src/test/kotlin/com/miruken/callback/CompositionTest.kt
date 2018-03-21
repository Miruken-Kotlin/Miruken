package com.miruken.callback

import com.miruken.typeOf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CompositionTest {
    class Foo
    class Bar<T>

    @Test fun `Extracts the composed value statically`() {
        assertNotNull(Composition.get<Foo>(Composition(Foo(), typeOf<Foo>())))
        assertNotNull(Composition.get<Bar<String>>(
                Composition(Bar<String>(), typeOf<Bar<String>>())))
        assertNull(Composition.get<Bar<Int>>(
                Composition(Bar<String>(), typeOf<Bar<String>>())))
    }

    @Test fun `Extracts the composed value dynamically`() {
        assertNotNull(Composition.get(Composition(Foo(), typeOf<Foo>()), Foo::class))
        assertNotNull(Composition.get(Composition(Foo(), typeOf<Foo>()), typeOf<Foo>()))
        assertNotNull(Composition.get(
                Composition(Bar<String>(), typeOf<Bar<String>>()),
                typeOf<Bar<String>>()))
        assertNotNull(Composition.get(
                Composition(Bar<String>(), typeOf<Bar<String>>()), Bar::class))
        assertNull(Composition.get(Composition(Foo(), typeOf<Foo>()), Int::class))
    }
}