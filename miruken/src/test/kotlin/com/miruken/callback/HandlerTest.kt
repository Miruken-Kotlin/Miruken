@file:Suppress("UNUSED_PARAMETER")

package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.callback.policy.PolicyRejectedException
import com.miruken.concurrent.Promise
import org.junit.Test
import kotlin.test.*

class HandlerTest {
    @Test fun `Indicates not handled`() {
        val handler = SimpleHandler()
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bee()))
    }

    @Test fun `Indicates not handled using adapter`() {
        val handler = HandlerAdapter(Controller())
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bee()))
    }

    @Test fun `Indicates not handled explicitly`() {
        val handler = SimpleHandler()
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Bam()))
    }

    @Test fun `Indicates not handled explicitly using adapter`() {
        val handler = HandlerAdapter(Controller())
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Bam()))
    }

    @Test fun `Handles callbacks implicitly`() {
        val foo     = Foo()
        val handler = SimpleHandler()
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
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bar))
        assertTrue { bar.hasComposer }
        assertEquals(1, bar.handled)
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(bar))
        assertEquals(2, bar.handled)
    }

    @Test fun `Handles callbacks covariantly`() {
        val foo     = SuperFoo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(2, foo.handled)
        assertTrue { foo.hasComposer }
    }

    @Test fun `Handles callbacks generically`() {
        val baz     = BazT(22)
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(baz))
        assertEquals("handlesGenericBaz", baz.tag)
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(BazT('M')))
    }

    @Test fun `Handles callbacks generically using arity`() {
        val baz     = BazTR(22, 15.5)
        val handler = SimpleHandler()
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

    @Test fun `Rejects open callbacks generically`() {
        val handler = OpenGenericRejectHandler()
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Foo()))
    }

    @Test fun `Handles bounded callbacks generically`() {
        val foo     = Foo()
        val handler = BoundedGenericHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
        assertTrue { foo.hasComposer }
    }

    @Test fun `Rejects bounded callbacks generically`() {
        val handler = BoundedGenericHandler()
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bar()))
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(SuperBar()))
    }

    @Test fun `Rejects handles that do not satisfy policy`() {
        try {
            val handler = InvalidHandler()
            handler.handle(Foo())
        } catch (e: PolicyRejectedException) {
            assertSame(HandlesPolicy, e.policy)
            assertEquals("reset", e.culprit.name)
        }
    }

    @Test fun `Indicates not provided`() {
        val handler = SimpleHandler()
        val bee     = handler.resolve<Bee>()
        assertNull(bee)
    }

    @Test fun `Indicates not provided using adapter`() {
        val handler = HandlerAdapter(Controller())
        val bee     = handler.resolve<Bee>()
        assertNull(bee)
    }

    @Test fun `Provides callbacks implicitly`() {
        val handler = SimpleHandler()
        val bar     = handler.resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar!!.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides callbacks implicitly async`() {
        val handler = SimpleAsyncHandler()
        assertAsync { done ->
            handler.resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it!!.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
    }

    @Test fun `Provides callbacks implicitly coerce async`() {
        val handler = SimpleHandler()
        assertAsync { done ->
            handler.resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it!!.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
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

    class SimpleHandler : Handler() {
        // Handles

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

        // Provides

        @Provides
        fun provideBarImplicitly() : Bar  {
            return Bar().apply { handled = 1 }
        }
    }

    class SimpleAsyncHandler : Handler() {
        // Providers

        @Provides
        fun provideBarImplicitly() : Promise<Bar>
        {
            return Promise.resolve(Bar().apply { handled = 1 });
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

    class OpenGenericRejectHandler : Handler() {
        @Handles
        fun <T> rejectEverything(cb: T?) =
                HandleResult.NOT_HANDLED_AND_STOP
    }

    class BoundedGenericHandler : Handler() {
        @Handles
        fun <T: Foo> handleAnything(cb: T?, composer: Handling) {
            cb?.apply {
                ++handled
                hasComposer = true
            }
        }

        @Handles
        fun <T: Bar> rejectBars(cb: T?): HandleResult? = null
    }

    class InvalidHandler : Handler() {
        @Handles
        fun reset() = 22
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