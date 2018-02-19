package com.miruken.callback

import com.miruken.runtime.getKType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.reflect.KType

class CompositionTest {
    class Foo
    class Bar<T>(override val resultType: KType? = null) : Callback {
        override var result: Any? = null
    }

    @Test fun `Extracts the composed value statically`() {
        assertNotNull(Composition.get<Foo>(Composition(Foo())))
        assertNull(Composition.get<Bar<String>>(Composition(Bar<String>())))
        assertNull(Composition.get<Bar<Int>>(Composition(Bar<String>())))
        assertNotNull(Composition.get<Bar<String>>(
                Composition(Bar<String>(getKType<Bar<String>>()))))
    }

    @Test fun `Extracts the composed value dynamically`() {
        assertNotNull(Composition.get(Composition(Foo()), Foo::class))
        assertNotNull(Composition.get(Composition(Foo()), getKType<Foo>()))
        assertNull(Composition.get(Composition(Bar<String>()), getKType<Bar<String>>()))
        assertNull(Composition.get(Composition(Bar<String>()), Bar::class))
        assertNull(Composition.get(Composition(Foo()), Int::class))
    }
}