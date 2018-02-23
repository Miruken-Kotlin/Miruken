@file:Suppress("UNUSED_PARAMETER")

package com.miruken.callback

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandlerTest {
    @Test fun `Indicates not handled`() {
        val handler = FeatureHandler()
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bee()))
    }

    @Test fun `Indicates not handled using adapter`() {
        val handler = HandlerAdapter(Controller())
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bee()))
    }

    @Test fun `Indicates not handled explicitly`() {
        val handler = FeatureHandler()
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Bam()))
    }

    @Test fun `Indicates not handled explicitly using adapter`() {
        val handler = HandlerAdapter(Controller())
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Bam()))
    }

    @Test fun `Handles callbacks implicitly`() {
        val foo     = Foo()
        val handler = FeatureHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles callbacks implicitly using adapter`() {
        val foo     = Foo()
        val handler = HandlerAdapter(Controller())
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles callbacks explicitly`() {
        val bar     = Bar()
        val handler = FeatureHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bar))
        assertTrue { bar.hasComposer }
        assertEquals(1, bar.handled)
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(bar))
        assertEquals(2, bar.handled)
    }

    @Test fun `Handles callbacks covariantly`() {
        val foo     = SuperFoo()
        val handler = FeatureHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(2, foo.handled)
        assertTrue { foo.hasComposer }
    }

    @Test fun `Handles callbacks generically`() {
        val baz     = BazT(22)
        val handler = FeatureHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(baz))
        assertEquals("handlesGenericBaz", baz.tag)
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(BazT('M')))
    }

    @Test fun `Handles callbacks generically using arity`() {
        val baz     = BazTR(22, 15.5)
        val handler = FeatureHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(baz))
        assertEquals("handlesGenericBazArity", baz.tag)
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(BazTR('M',2)))
    }

    @Test fun `Handles all callbacks`() {
        val handler = SpecialHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(Foo()))
    }

    @Test fun `Handles open callbacks generically`() {
        val handler = OpenGenericHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(Foo()))
    }

    @Test fun `Handles bounded callbacks generically`() {
        val foo     = Foo()
        val handler = BoundedGenericHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
        assertTrue { foo.hasComposer }
    }

    /** Callbacks */

    open class Foo {
        var handled:     Int     = 0
        var hasComposer: Boolean = false
    }

    class SuperFoo : Foo()

    class FooDecorator(foo: Foo) : Foo()

    open class Bar {
        var handled:     Int     = 0
        var hasComposer: Boolean = false
    }

    class SuperBar : Bar()

    open class Boo {
        var hasComposer: Boolean = false
    }

    open class Baz {
        var hasComposer: Boolean = false
    }

    class SuperBaz : Baz()

    open class BazT<T>(
            var stuff: T,
            var tag: String? = null
    ) : Baz()

    class BazTR<T, R>(
            stuff: T,
            var otherStuff: R,
            tag: String? = null
    ) : BazT<T>(stuff, tag)

    class Bee

    class Bam

    /** Handlers */

    class FeatureHandler : Handler() {
        @Handles
        fun handleFooImplicitly(foo: Foo)
        {
            ++foo.handled
        }

        @Handles
        fun handleSuperFooImplicitly(foo: SuperFoo) : HandleResult?
        {
            ++foo.handled
            foo.hasComposer = true
            return null
        }

        @Handles
        fun notHandleBeeExplicitly(bam: Bam) =
                HandleResult.NOT_HANDLED_AND_STOP

        @Handles
        fun handleBarExplicitly(bar: Bar, composer: Handling): HandleResult?
        {
            ++bar.handled
            bar.hasComposer = true
            return when {
                bar.handled % 2 == 1 -> HandleResult.HANDLED
                else -> null
            }
        }

        @Handles
        fun <T> handlesGenericBaz(baz: BazT<T>) : HandleResult? {
            if ((baz.stuff as Any)::class == Char::class) return null
            baz.tag = "handlesGenericBaz"
            return HandleResult.HANDLED
        }

        @Handles
        fun <T,R> handlesGenericBazArity(baz: BazTR<T, R>) : HandleResult? {
            if ((baz.stuff as Any)::class == Char::class) return null
            baz.tag = "handlesGenericBazArity"
            return HandleResult.HANDLED
        }
    }

    class SpecialHandler : Handler() {
        @Handles
        fun handleAnything(cb: Any?) {

        }
    }

    class OpenGenericHandler : Handler() {
        @Handles
        fun <T> handleAnything(cb: T?) {

        }
    }

    class BoundedGenericHandler : Handler() {
        @Handles
        fun <T: Foo> handleAnything(cb: T?, composer: Handling) {
            cb?.apply {
                ++handled
                hasComposer = true
            }
        }
    }

    class Controller {
        @Handles
        fun handleFooImplicitly(foo: Foo)
        {
            ++foo.handled
        }

        @Handles
        fun notHandleBeeExplicitly(bam: Bam) =
                HandleResult.NOT_HANDLED_AND_STOP
    }
}