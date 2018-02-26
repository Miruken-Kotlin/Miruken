@file:Suppress("UNUSED_PARAMETER")

package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.callback.policy.PolicyMethodBinding
import com.miruken.callback.policy.PolicyRejectedException
import com.miruken.concurrent.Promise
import com.miruken.runtime.typeOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.test.*

class HandlerTest {
    @Rule @JvmField val testName = TestName()

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

    @Test fun `Handles composed callbacks`() {
        val foo     = Foo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(Composition(foo)))
        assertEquals(1, foo.handled)
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

    @Test fun `Rejects @Handles that do not satisfy policy`() {
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
        assertNotNull(bar!!)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides callbacks implicitly using adapter`() {
        val handler = HandlerAdapter(Controller())
        val bar     = handler.resolve<Bar>()
        assertNotNull(bar!!)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides callbacks implicitly with composer`() {
        val handler = SimpleHandler()
        val boo     = handler.resolve<Boo>()
        assertNotNull(boo!!)
        assertEquals(Boo::class, boo::class)
        assertTrue(boo.hasComposer)
    }

    @Test fun `Provides callbacks implicitly async`() {
        val handler = SimpleAsyncHandler()
        assertAsync(testName) { done ->
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
        assertAsync(testName) { done ->
            handler.resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it!!.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
    }

    @Test fun `Provides callbacks covariantly`() {
        val handler = SimpleHandler()
        val bar     = handler.resolve<SuperBar>()
        assertNotNull(bar!!)
        assertEquals(SuperBar::class, bar::class)
        assertTrue(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides null if not handled`() {
        val handler = SimpleHandler()
        val bam     = handler.resolve<Bam>()
        assertNull(bam)
    }

    @Test fun `Provides null if not handled async`() {
        val handler = SimpleAsyncHandler()
        assertAsync(testName) { done ->
            handler.resolveAsync<Bam>() then {
                assertNull(it)
                done()
            }
        }
    }

    @Test fun `Provides null if not handled coerce async`() {
        val handler = SimpleHandler()
        assertAsync(testName) { done ->
            handler.resolveAsync<Bam>() then {
                assertNull(it)
                done()
            }
        }
    }

    @Test fun `Provides callbacks implicitly waiting async`() {
        val handler = SimpleAsyncHandler()
        val bar     = handler.resolve<Bar>()
        assertNotNull(bar!!)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides many callbacks implicitly`() {
        val handler = SpecialHandler()
        val bar     = handler.resolve<Bar>()
        assertNotNull(bar)
        val bars    = handler.resolveAll<Bar>()
        assertEquals(3, bars.size)
        assertTrue(bars.map { it.handled to it.hasComposer }
                .containsAll(listOf(1 to false, 2 to false, 3 to false)))
    }

    @Test fun `Provides many callbacks implicitly async`() {
        val handler = SpecialHandler()
        assertAsync(testName) { done ->
            handler.resolveAsync<Bar>() then {
                assertNotNull(it)
                done()
            }
        }
        assertAsync(testName) { done ->
            handler.resolveAllAsync<Bar>() then {
                assertEquals(3, it.size)
                assertTrue(it.map { it.handled to it.hasComposer }
                        .containsAll(listOf(1 to false, 2 to false, 3 to false)))
                done()
            }
        }
    }

    @Test fun `Provides callbacks greedily`() {
        val handler = SimpleHandler() + SimpleHandler()
        var bars    = handler.resolveAll<Bar>()
        assertEquals(4, bars.size)
        bars = handler.resolveAll<SuperBar>()
        assertEquals(2, bars.size)
    }

    @Test fun `Provides callbacks explicitly`() {
        val handler = SimpleHandler()
        val baz     = handler.resolve<Baz>()
        assertNotNull(baz!!)
        assertEquals(SuperBaz::class, baz::class)
        assertFalse(baz.hasComposer)
    }

    @Test fun `Provides many callbacks explicitly`() {
        val handler = SpecialHandler()
        val baz     = handler.resolve<Baz>()
        assertNotNull(baz!!)
        assertEquals(SuperBaz::class, baz::class)
        assertFalse(baz.hasComposer)
        val bazs    = handler.resolveAll<Baz>()
        assertEquals(2, bazs.size)
        assertTrue(bazs.map { it::class to it.hasComposer }
                .containsAll(listOf(SuperBaz::class to false,
                        Baz::class to false)))
    }

    @Test fun `Provides callbacks generically`() {
        val handler = SimpleHandler()
        val baz     = handler.resolve<BazT<Int>>()
        assertNotNull(baz!!)
        assertEquals("providesGenericBaz", baz.tag)
    }

    @Test fun `Provides callbacks generically using arity`() {
        val handler = SimpleHandler()
        val baz     = handler.resolve<BazTR<Int, String>>()
        assertNotNull(baz!!)
        assertEquals("providesGenericBazArity", baz.tag)
    }

    @Test fun `Provides many callbacks`() {
        val handler = SimpleHandler()
        val bars    = handler.resolveAll<Bar>()
        assertEquals(2, bars.size)
    }

    @Test fun `Provides empty list if no matches`() {
        val handler = Handler()
        val bars    = handler.resolveAll<Bar>()
        assertEquals(0, bars.size)
    }

    @Test fun `Provides callbacks by string key`() {
        val handler = SimpleHandler()
        val bar     = handler.resolve("Bar")
        assertNotNull(bar)
    }

    @Test fun `Provides callbacks using constraints`() {
        val handler = SpecialHandler()
        assertNotNull(handler.resolve<Foo>())
        assertNotNull(handler.resolve<SuperFoo>())
    }

    @Test fun `Rejects @Provides that do not satisfy policy`() {
        try {
            val handler = InvalidProvider()
            handler.resolve<Foo>()
        } catch (e: PolicyRejectedException) {
            assertSame(ProvidesPolicy, e.policy)
            assertEquals("add", e.culprit.name)
        }
    }

    @Test fun `Filters async provides`() {
        val handler = SimpleHandler()
        assertAsync(testName) { done ->
            handler.aspectBefore({ _, _ -> true })
                    .resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it!!.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
    }

    @Test fun `Filters provides async`() {
        val handler = SimpleHandler()
        val bar     = handler.aspectBefore({ _, _ -> Promise.TRUE })
                .resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar!!.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Filters async provides async`() {
        val handler = SimpleHandler()
        assertAsync(testName) { done ->
            handler.aspectBefore({ _, _ -> Promise.TRUE })
                    .resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it!!.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
    }

    @Test fun `Cancels provides async`() {
        val handler = SimpleHandler()
        assertFailsWith(RejectedException::class) {
            handler.aspectBefore({ _, _ -> Promise.FALSE })
                    .resolve<Bar>()
        }
    }

    @Test fun `Cancels async provides`() {
        val handler = SimpleHandler()
        assertFailsWith(RejectedException::class) {
            handler.aspectBefore({ _, _ -> false })
                    .resolveAsync<Bar>().get()
        }
    }

    @Test fun `Cancels async provides async`() {
        val handler = SimpleHandler()
        assertFailsWith(RejectedException::class) {
            handler.aspectBefore({ _, _ -> Promise.FALSE })
                    .resolveAsync<Bar>().get()
        }
    }

    @Test fun `Provides self implicitly`() {
        val handler = SimpleHandler()
        val result  = handler.resolve<SimpleHandler>()
        assertSame(handler, result)
    }

    @Test fun `Provides self decorated`() {
        val handler = SimpleHandler()
        val result  = handler.broadcast.resolve<SimpleHandler>()
        assertSame(handler, result)
    }

    @Test fun `Provides adapted self implicitly`() {
        val controller = Controller()
        val handler    = HandlerAdapter(controller)
        val result     = handler.resolve<Controller>()
        assertSame(controller, result)
    }

    @Test fun `Provides adapted self decorated`() {
        val controller = Controller()
        val handler    = HandlerAdapter(controller)
        val result     = handler.broadcast.resolve<Controller>()
        assertSame(controller, result)
    }

    @Test fun `Provides all callbacks`() {
        val simple  = SimpleHandler()
        val special = SpecialHandler()
        val handler = simple + special
        val all     = handler.resolveAll<Any>()
        assertEquals(8, all.size)
        assertTrue(all.contains(simple))
        assertTrue(all.contains(special))
    }

    @Test fun `Broadcasts callbacks`() {
        val foo   = Foo()
        val group = SimpleHandler() + SimpleHandler() + SimpleHandler()
        assertEquals(HandleResult.HANDLED, group.broadcast.handle(foo))
        assertEquals(3, foo.handled)
    }

    @Test fun `Can override providers`() {
        val handler = Handler()
        val foo     = handler.provide(Foo()).resolve<Foo>()
        assertNotNull(foo)
    }

    @Test fun `Can override providers many`() {
        val foo1    = Foo()
        val foo2    = Foo()
        val handler = Handler()
        val foos    = handler.provide(listOf(foo1, foo2))
                .resolveAll<Foo>()
        assertTrue(foos.containsAll(listOf(foo1, foo2)))
    }

    @Test fun `Ignores providers that don't match`() {
        val handler = Handler()
        val foo     = handler.provide(Bar()).resolve<Foo>()
        assertNull(foo)
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
            var stuff: T?    = null,
            var tag: String? = null
    ) : Baz()

    class BazTR<T, R>(
            stuff: T?    =       null,
            var otherStuff: R? = null,
            tag: String? =       null
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

        @Provides
        fun provideBooImplicitly(composer: Handling) : Boo  {
            return Boo().apply { hasComposer = true }
        }

        @Provides
        fun provideSuperBarImplicitly(composer: Handling) : SuperBar  {
            return SuperBar().apply {
                handled     = 1
                hasComposer = true
            }
        }

        @Provides
        fun notProvideBazImplicitly() : Baz? = null

        @Provides
        fun <T> provideBazGenerically() : BazT<T> {
            return BazT(tag = "providesGenericBaz")
        }

        @Provides
        fun <T,R> provideGenericBazWithArity() : BazTR<T,R> {
            return BazTR(tag = "providesGenericBazArity")
        }

        @Provides
        fun provideBazExplicitly(inquiry: Inquiry, composer: Handling) {
            if (inquiry.key == typeOf<Baz>())
                inquiry.resolve(SuperBaz(), composer)
        }

        @Provides
        fun providesByName(inquiry: Inquiry): Any? {
            return when (inquiry.key) {
                "Foo" -> Foo()
                "Bar" -> Bar()
                else -> Promise.EMPTY
            }
        }
    }

    class SimpleAsyncHandler : Handler() {
        // Providers

        @Provides
        fun provideBarImplicitly() : Promise<Bar>
        {
            return Promise.resolve(Bar().apply { handled = 1 })
        }


        @Provides
        fun provideBooImplicitly(composer: Handling) : Promise<Boo>
        {
            return Promise.resolve(Boo().apply {
                hasComposer = true
            })
        }

        @Provides
        fun provideSuperBarImplicitly() : Promise<SuperBar>
        {
            return Promise.resolve(SuperBar().apply {
                handled     = 1
                hasComposer = true
            })
        }

        @Suppress("RemoveExplicitTypeArguments")
        @Provides
        fun notProvideBazImplicitly() : Promise<Baz?> =
                Promise.resolve<Baz?>(null)

        @Provides
        fun <T> provideGenericBaz() : Promise<BazT<T>> {
            return Promise.resolve(BazT(tag = "providesGenericBaz"))
        }

        @Provides
        fun <T,R> provideGenericBazWithArity() : Promise<BazTR<T,R>> {
            return Promise.resolve(BazTR(tag = "providesGenericBazArity"))
        }

        @Provides
        fun provideBazExplicitly(inquiry: Inquiry, composer: Handling) {
            if (inquiry.key == typeOf<Baz>())
                inquiry.resolve(Promise.resolve(SuperBaz()), composer)
        }

        @Provides
        fun providesByName(inquiry: Inquiry): Promise<Any?> {
            return when (inquiry.key) {
                "Foo" -> Promise.resolve(Foo())
                "Bar" -> Promise.resolve(Bar())
                else -> Promise.EMPTY
            }
        }
    }

    class SpecialHandler : Handler() {
        @Handles
        fun handleAnything(cb: Any?) {}

        @Provides
        fun providesManyBars(): List<Bar> {
            return listOf(
                    Bar().apply { handled = 1 },
                    Bar().apply { handled = 2 })
        }

        @Provides
        val providesPropertyBar : Bar
            get() = Bar().apply { handled = 3 }

        @Provides
        fun providesBazExplicitly(
                inquiry:  Inquiry,
                composer: Handling,
                binding:  PolicyMethodBinding
        ) {
            if (inquiry.key == typeOf<Baz>()) {
                inquiry.resolve(SuperBaz(), composer)
                inquiry.resolve(Baz(), composer)
            }
        }

        @Provides
        fun <T: Foo> providesNewFoo(inquiry: Inquiry): T?
        {
            @Suppress("UNCHECKED_CAST")
            return ((inquiry.key as? KType)
                    ?.classifier as? KClass<*>)?.let {
                it.java.newInstance() as T
            }
        }
    }

    class SpecialAsyncHandler : Handler() {
        @Provides
        fun providesManyBars(): Promise<List<Bar>> {
            return Promise.resolve(listOf(
                    Bar().apply { handled = 1 },
                    Bar().apply { handled = 2 }))
        }

        @Provides
        val providesPropertyBar : Promise<Bar>
            get() = Promise.resolve(Bar().apply { handled = 3 })

        @Provides
        fun providesBazExplicitly(
                inquiry:  Inquiry,
                composer: Handling,
                binding:  PolicyMethodBinding
        ) {
            if (inquiry.key == typeOf<Baz>()) {
                inquiry.resolve(Promise.resolve(SuperBaz()), composer)
                inquiry.resolve(Promise.resolve(Baz()), composer)
            }
        }

        @Provides
        inline fun <reified T: Foo> providesNewFoo(): T
        {
            return T::class.java.newInstance()
        }
    }

    class OpenGenericHandler : Handler() {
        @Handles
        fun <T> handleAnything(cb: T?) {}
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

    class InvalidProvider : Handler() {
        @Provides
        fun add(op1: Int, op2: Int){}
    }

    class Controller {
        @Handles
        fun handleFooImplicitly(foo: Foo) {
            ++foo.handled
        }

        @Handles
        fun notHandleBeeExplicitly(bam: Bam) =
                HandleResult.NOT_HANDLED_AND_STOP

        @Provides
        fun provideBarImplicitly() : Bar {
            return Bar().apply { handled = 1 }
        }
    }
}