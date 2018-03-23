@file:Suppress("UNUSED_PARAMETER")

package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.callback.policy.MethodBinding
import com.miruken.callback.policy.PolicyMethodBinding
import com.miruken.callback.policy.PolicyRejectedException
import com.miruken.concurrent.Promise
import com.miruken.typeOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.reflect.KType
import kotlin.test.*

class HandlerTest {
    @Rule
    @JvmField val testName = TestName()

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

    @Test fun `Handles interface callbacks`() {
        val foo     = SpecialFoo()
        val handler = InterfaceHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
        assertTrue { foo.hasComposer }
    }

    @Test fun `Handles callbacks covariantly`() {
        val foo     = SpecialFoo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(2, foo.handled)
        assertTrue { foo.hasComposer }
    }

    @Test fun `Handles composed callbacks`() {
        val foo     = Foo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(
                Composition(foo, typeOf<Foo>())))
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

    @Test fun `Handles explicit generic callbacks`() {
        val baz     = BazT(Foo())
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(baz))
        assertEquals("handlesConcreteBaz", baz.tag)
        assertEquals(2, baz.stuff!!.handled)
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
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(SpecialBar()))
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

    @Test fun `Provides interface callbacks`() {
        val handler = InterfaceHandler()
        val testing = handler.resolve<Testing>()
        assertNotNull(testing!!)
        assertTrue(testing is Bar)
        assertFalse(testing.hasComposer)
        assertEquals(3, testing.handled)
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
        val bar     = handler.resolve<SpecialBar>()
        assertNotNull(bar!!)
        assertEquals(SpecialBar::class, bar::class)
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

    @Test fun `Provides array of callbacks implicitly`() {
        val handler = SpecialHandler()
        val bams    = handler.resolveAll<Bam>()
        assertEquals(3, bams.size)
    }

    @Test fun `Provides many callbacks implicitly async`() {
        val handler = SpecialAsyncHandler()
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
        bars = handler.resolveAll<SpecialBar>()
        assertEquals(2, bars.size)
    }

    @Test fun `Provides callbacks explicitly`() {
        val handler = SimpleHandler()
        val baz     = handler.resolve<Baz>()
        assertNotNull(baz!!)
        assertEquals(SpecialBaz::class, baz::class)
        assertFalse(baz.hasComposer)
    }

    @Test fun `Provides many callbacks explicitly`() {
        val handler = SpecialHandler()
        val baz     = handler.resolve<Baz>()
        assertNotNull(baz!!)
        assertEquals(SpecialBaz::class, baz::class)
        assertFalse(baz.hasComposer)
        val bazs    = handler.resolveAll<Baz>()
        assertEquals(2, bazs.size)
        assertTrue(bazs.map { it::class to it.hasComposer }
                .containsAll(listOf(SpecialBaz::class to false,
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
        assertNotNull(handler.resolve<SpecialFoo>())
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
        assertEquals(11, all.size)
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

    @Test fun `Can override key with string`() {
        val handler  = SpecialHandler()
        val password = handler.resolve("password")
        assertEquals("ABC123!", password)
    }

    @Test fun `Uses method name when providing primitives`() {
        val handler = SpecialHandler()
        val config  = handler.resolve("providesTimeout")
        assertEquals(5000L, config)
    }

    @Test fun `Ignores providers that don't match`() {
        val handler = Handler()
        val foo     = handler.provide(Bar()).resolve<Foo>()
        assertNull(foo)
    }

    @Test fun `Creates a pipeline from filters`() {
        val bar     = Bar()
        val handler = FilteringHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bar))
        assertEquals(2, bar.handled)
        assertEquals(2, bar.filters.size)
        assertSame(handler, bar.filters[1])
        assertTrue { bar.filters[0] is LogFilter }
    }

    @Test fun `Infers pipeline from response`() {
        val foo     = Foo()
        val handler = FilteringHandler()
        val result  = handler.command(foo)
        assertTrue(result is SpecialFoo)
        assertEquals(1, foo.filters.size)
        assertTrue { foo.filters[0] is LogBehavior<*,*> }
    }

    @Test fun `Promotes promise pipeline`() {
        val foo     = Foo()
        val handler = FilteringHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(foo) then {
                assertTrue(it is SpecialFoo)
                done()
            }
        }
        assertEquals(1, foo.filters.size)
        assertTrue { foo.filters[0] is LogBehavior<*,*> }
    }

    @Test fun `Infers promise pipeline`() {
        val boo     = Boo()
        val handler = FilteringHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(boo) then {
                done()
            }
        }
        assertEquals(1, boo.filters.size)
        assertTrue { boo.filters[0] is LogBehavior<*,*> }
    }

    @Test fun `Infers command promise pipeline`() {
        val bee     = Bee()
        val handler = FilteringHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(bee) then {
                done()
            }
        }
        assertEquals(1, bee.filters.size)
        assertTrue { bee.filters[0] is LogBehavior<*,*> }
    }

    @Test fun `Coerces pipeline`() {
        val foo     = Foo()
        val handler = SpecialFilteredHandler() + FilteringHandler()
        val result  = handler.command(foo)
        assertTrue(result is SpecialFoo)
        assertEquals(2, foo.filters.size)
        assertTrue { foo.filters[0] is LogFilter<*,*> }
        assertTrue { foo.filters[1] is LogBehavior<*,*> }
    }

    @Test fun `Coerces promise pipeline`() {
        val baz     = Baz()
        val handler = SpecialFilteredHandler() + FilteringHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(baz) then {
                assertTrue(it is SpecialBaz)
                done()
            }
        }
        assertEquals(2, baz.filters.size)
        assertTrue { baz.filters[0] is LogFilter<*,*> }
        assertTrue { baz.filters[1] is LogBehavior<*,*> }
    }

    @Test fun `Propagates rejected filter promise`() {
        val boo     = Boo()
        val handler = SpecialFilteredHandler() + FilteringHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(boo) catch {
                assertTrue(it is IllegalStateException)
                assertEquals("System shutdown", it.message)
                done()
            }
        }
    }

    @Test fun `Ignores options at boundary `() {
        val handler = Handler().withFilters(LogFilter<Any,Any?>())
        val options = FilterOptions()
        assertEquals(HandleResult.HANDLED, handler.handle(options))
        assertEquals(1, options.providers.size)
        assertEquals(HandleResult.NOT_HANDLED, handler.stop.handle(FilterOptions()))
    }

    /** Callbacks */

    open class Testing {
        var handled     = 0
        var hasComposer = false
        val filters     = mutableListOf<Filtering<*,*>>()
    }

    open class Foo: Testing()

    class SpecialFoo : Foo()

    class FooDecorator(foo: Foo) : Foo()

    open class Bar : Testing()

    class SpecialBar : Bar()

    open class Boo : Testing()

    open class Baz : Testing()

    class SpecialBaz : Baz()

    open class BazT<T>(
            var stuff: T?    = null,
            var tag: String? = null
    ) : Baz()

    class BazTR<T, R>(
            stuff: T?    =       null,
            var otherStuff: R? = null,
            tag: String? =       null
    ) : BazT<T>(stuff, tag)

    class Bee : Testing()

    class Bam : Testing()

    /** Handlers */

    class SimpleHandler : Handler() {
        // Handles

        @Handles
        fun handleFooImplicitly(foo: Foo)
        {
            ++foo.handled
        }

        @Handles
        fun handleSpecialFooImplicitly(foo: SpecialFoo) : HandleResult?
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
        fun <T> handlesConcreteBaz(baz: BazT<Foo>, type: KType) {
            assertEquals(typeOf<BazT<Foo>>(), type)
            baz.tag = "handlesConcreteBaz"
            baz.stuff?.handled = 2
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
        fun provideBarImplicitly(): Bar  {
            return Bar().apply { handled = 1 }
        }

        @Provides
        fun provideBooImplicitly(composer: Handling) : Boo?  {
            return Boo().apply { hasComposer = true }
        }

        @Provides
        fun provideSpecialBarImplicitly(composer: Handling) : SpecialBar  {
            return SpecialBar().apply {
                handled     = 1
                hasComposer = true
            }
        }

        @Provides
        fun notProvideBazImplicitly(): Baz? = null

        @Provides
        fun <T> provideBazGenerically(): BazT<T> {
            return BazT(tag = "providesGenericBaz")
        }

        @Provides
        fun <T,R> provideGenericBazWithArity(): BazTR<T,R> {
            return BazTR(tag = "providesGenericBazArity")
        }

        @Provides
        fun provideBazExplicitly(inquiry: Inquiry, composer: Handling) {
            if (inquiry.key == typeOf<Baz>())
                inquiry.resolve(SpecialBaz(), composer)
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

    class InterfaceHandler : Handler() {
        // Handles

        @Handles
        fun handleTestingImplicitly(t: Testing, composer: Handling) {
            ++t.handled
            t.hasComposer = true
        }

        // Provides

        @Provides
        fun provideTestingImplicitly(): Testing = Bar().apply {
            handled = 3
        }
    }

    class SimpleAsyncHandler : Handler() {
        // Providers

        @Provides
        fun provideBarImplicitly(): Promise<Bar>
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
        fun provideSpecialBarImplicitly(): Promise<SpecialBar>
        {
            return Promise.resolve(SpecialBar().apply {
                handled     = 1
                hasComposer = true
            })
        }

        @Suppress("RemoveExplicitTypeArguments")
        @Provides
        fun notProvideBazImplicitly(): Promise<Baz?> =
                Promise.resolve<Baz?>(null)

        @Provides
        fun <T> provideGenericBaz(): Promise<BazT<T>> {
            return Promise.resolve(BazT(tag = "providesGenericBaz"))
        }

        @Provides
        fun <T,R> provideGenericBazWithArity(): Promise<BazTR<T,R>> {
            return Promise.resolve(BazTR(tag = "providesGenericBazArity"))
        }

        @Provides
        fun provideBazExplicitly(inquiry: Inquiry, composer: Handling) {
            if (inquiry.key == typeOf<Baz>())
                inquiry.resolve(Promise.resolve(SpecialBaz()), composer)
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
        fun providesArrayOfBams(): Array<Bam> {
            return arrayOf(Bam(), Bam(), Bam())
        }

        @Provides
        fun providesBazExplicitly(
                inquiry:  Inquiry,
                composer: Handling,
                binding:  PolicyMethodBinding
        ) {
            if (inquiry.key == typeOf<Baz>()) {
                inquiry.resolve(SpecialBaz(), composer)
                inquiry.resolve(Baz(), composer)
            }
        }

        @Provides
        @Suppress("UNCHECKED_CAST")
        fun <T: Foo> providesNewFoo(inquiry: Inquiry): T? {
            return inquiry.createKeyInstance() as? T
        }

        @Provides
        fun providesTimeout(): Long = 5000

        @Provides
        @Key("password")
        fun providesPassword(): String = "ABC123!"
    }

    class SpecialAsyncHandler: Handler() {
        @Provides
        fun providesManyBars(): Promise<List<Bar>> =
                Promise.resolve(listOf(
                    Bar().apply { handled = 1 },
                    Bar().apply { handled = 2 }))

        @Provides
        val providesPropertyBar: Promise<Bar>
            get() = Promise.resolve(Bar().apply { handled = 3 })

        @Provides
        fun providesBazExplicitly(
                inquiry:  Inquiry,
                composer: Handling,
                binding:  PolicyMethodBinding
        ) {
            if (inquiry.key == typeOf<Baz>()) {
                inquiry.resolve(Promise.resolve(SpecialBaz()), composer)
                inquiry.resolve(Promise.resolve(Baz()), composer)
            }
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

    class FilteringHandler : Handler(), Filtering<Bar, Unit> {
        override var order: Int? = null

        @Handles
        @AllFilters
        fun handleBar(bar: Bar) {
            bar.handled++
        }

        @Handles
        @AllBehaviors
        fun handleFoo(foo: Foo, composer: Handling): SpecialFoo {
            return SpecialFoo().apply { hasComposer = true }
        }

        @Handles
        @AllBehaviors
        fun handleBoo(boo: Boo, composer: Handling): Promise<Boo> {
            return Promise.resolve(Boo().apply { hasComposer= true })
        }

        @Handles
        @AllBehaviors
        fun handleStuff(command: Command): Promise<Any?> {
            if (command.callback is Bee)
                return Promise.resolve(Bee())
            return Promise.EMPTY
        }

        @Provides
        fun <T: Any, R: Any?> createFilter(inquiry: Inquiry): Filtering<T,R>?
        {
            @Suppress("UNCHECKED_CAST")
            return when (inquiry.keyClass) {
                null -> null
                LogFilter::class, Filtering::class -> LogFilter()
                else ->
                    inquiry.createKeyInstance() as Filtering<T,R>
            }
        }

        @Provides
        fun <T: Any, R: Any?> createBehavior(inquiry: Inquiry): Behavior<T,R>?
        {
            @Suppress("UNCHECKED_CAST")
            return when (inquiry.keyClass) {
                null -> null
                Behavior::class -> LogBehavior()
                else ->
                    inquiry.createKeyInstance() as Behavior<T,R>
            }
        }

        @Provides
        fun <T: Any, R: Any?> forExceptions(inquiry: Inquiry): ExceptionBehavior<T,R>?
        {
            @Suppress("UNCHECKED_CAST")
            return when (inquiry.keyClass) {
                ExceptionBehavior::class -> ExceptionBehavior()
                else -> null
            }
        }

        override fun next(
                callback: Bar,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Unit>
        ) {
            callback.filters.add(this)
            callback.handled++
            return next()
        }
    }

    class SpecialFilteredHandler : Handler() {
        @Handles
        @AllFilters
        @AllBehaviors
        fun handleFoo(foo: Foo): SpecialFoo {
            return SpecialFoo()
        }

        @Handles
        @AllFilters
        @AllBehaviors
        fun handleBaz(baz: Baz): Promise<SpecialBaz> {
            return Promise.resolve(SpecialBaz())
        }

        @Handles
        @AllFilters
        @AllBehaviors
        fun handleBaz(bar: Bar): Promise<SpecialBar> {
            return Promise.resolve(SpecialBar())
        }

        @Handles
        @Exceptions
        fun remove(boo: Boo) {
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(Filtering::class)
    annotation class AllFilters

    interface Behavior<in TReq: Any, TResp: Any?>
        : Filtering<TReq, Promise<TResp>>

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(Behavior::class)
    annotation class AllBehaviors

    class RequestFilter<in T: Any, R: Any?> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<R>
        ) = next()
    }

    class RequestFilterCb<in T: Any> : Filtering<T, Any?> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Any?>
        ) = next()
    }

    class RequestFilterRes<T> : Filtering<Any, T> {
        override var order: Int? = null

        override fun next(
                callback: Any,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<T>
        ) = next()
    }

    class LogFilter<in Cb: Any, Res: Any?> : Filtering<Cb, Res> {
        override var order: Int? = 1

        override fun next(
                callback: Cb,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Res>
        ): Res {
            val cb = extractTesting(callback)
            cb?.filters?.add(this)
            println("Filter log $cb")
            return next()
        }
    }

    class LogBehavior<in Req: Any, Res: Any?> : Behavior<Req, Res> {
        override var order: Int? = 2

        override fun next(
                callback: Req,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Promise<Res>>
        ): Promise<Res> {
            val cb = extractTesting(callback)
            cb?.filters?.add(this)
            println("Behavior log $cb")
            return next()
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(ExceptionBehavior::class)
    annotation class Exceptions

    class ExceptionBehavior<in Req: Any, Res: Any?> : Behavior<Req, Res> {
        override var order: Int? = 2

        override fun next(
                callback: Req,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Promise<Res>>
        ) = Promise.reject(IllegalStateException("System shutdown"))
    }

    companion object {
        private fun extractTesting(callback: Any): Testing? {
            return (callback as? Testing)
                    ?: (callback as? Command)?.let {
                        it.callback as? Testing
                    }
        }
    }
}