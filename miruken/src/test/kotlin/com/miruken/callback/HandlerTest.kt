@file:Suppress("UNUSED_PARAMETER")

package com.miruken.callback

import com.miruken.Initializing
import com.miruken.TypeReference
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.PolicyRejectedException
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.concurrent.delay
import com.miruken.context.Context
import com.miruken.context.ContextualImpl
import com.miruken.context.Scoped
import com.miruken.kTypeOf
import com.miruken.runtime.checkOpenConformance
import com.miruken.test.assertAsync
import com.miruken.typeOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import sun.plugin.dom.exception.InvalidStateException
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.test.*

class HandlerTest {
    private lateinit var factory: HandlerDescriptorFactory

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        HandlerDescriptorFactory.useFactory(
                MutableHandlerDescriptorFactory().apply {
            registerDescriptor<SimpleHandler>()
            registerDescriptor<SimpleAsyncHandler>()
            registerDescriptor<SpecialHandler>()
            registerDescriptor<SpecialAsyncHandler>()
            registerDescriptor<FilteringHandler>()
            registerDescriptor<SpecialFilteringHandler>()
            registerDescriptor<FilterHandlerTests>()
            registerDescriptor<ControllerBase>()
            registerDescriptor<ImplicitCompanionHandler>()
            registerDescriptor<ExplicitCompanionHandler>()
            registerDescriptor<InterfaceHandler>()
            registerDescriptor<SingletonHandler>()
            registerDescriptor<OpenGenericHandler>()
            registerDescriptor<BoundedGenericHandler>()
            registerDescriptor<OpenGenericRejectHandler>()
            factory = this
        })
    }

    @Test fun `Indicates not handled`() {
        val handler = SimpleHandler()
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bee()))
    }

    @Test fun `Indicates not handled using adapter`() {
        val handler = HandlerAdapter(ControllerBase())
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(Bee()))
    }

    @Test fun `Indicates not handled explicitly`() {
        val handler = SimpleHandler()
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Bam()))
    }

    @Test fun `Indicates not handled explicitly using adapter`() {
        val handler = HandlerAdapter(ControllerBase())
        assertEquals(HandleResult.NOT_HANDLED_AND_STOP, handler.handle(Bam()))
    }

    @Test fun `Handles callbacks implicitly`() {
        val foo     = Foo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles callbacks inferred greedy`() {
        val foo     = Foo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo, true))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles singleton callbacks implicitly`() {
        val foo = Foo()
        assertEquals(HandleResult.HANDLED, SingletonHandler.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles explicit callbacks implicitly`() {
        val foo = Foo()
        assertEquals(HandleResult.HANDLED, ExplicitCompanionHandler.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles implicit companion callbacks implicitly`() {
        val foo = Foo()
        assertEquals(HandleResult.HANDLED, HandlerAdapter(
                ImplicitCompanionHandler.Companion).handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles callbacks implicitly using adapter`() {
        val foo     = Foo()
        val handler = HandlerAdapter(ControllerBase())
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Handles callbacks explicitly`() {
        val bar     = Bar()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bar))
        assertTrue(bar.hasComposer)
        assertEquals(1, bar.handled)
        assertEquals(HandleResult.NOT_HANDLED, handler.handle(bar))
        assertEquals(2, bar.handled)
    }

    @Test fun `Handles interface callbacks`() {
        val foo     = SpecialFoo()
        val handler = InterfaceHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(1, foo.handled)
        assertTrue(foo.hasComposer)
    }

    @Test fun `Handles callbacks covariantly`() {
        val foo     = SpecialFoo()
        val handler = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(2, foo.handled)
        assertTrue(foo.hasComposer)
    }

    @Test fun `Handles callbacks using runtime type`() {
        val foo: Foo = SpecialFoo()
        val handler  = SimpleHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(foo))
        assertEquals(2, foo.handled)
        assertTrue(foo.hasComposer)
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

    @Test fun `Handles callbacks generically with dependencies`() {
        val foo       = Foo()
        val bar       = Bar()
        val baztr     = BazTR(bar, foo)
        val bazt      = BazT<Bar>()
        val handler   = SpecialHandler()
        assertEquals(HandleResult.HANDLED,
                handler.provide(bazt).handle(baztr))
        assertSame(baztr.stuff, bazt.stuff)
        assertNotSame(bar, baztr.stuff)
        assertNotSame(foo, baztr.otherStuff)
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
        assertTrue(foo.hasComposer)
    }

    @Test fun `Replaces composer in filter`() {
        val handler = FilterHandlerTests()
        val bar = handler.command(Foo()) as? Bar
        assertNotNull(bar)
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
        val handler = HandlerAdapter(ControllerBase())
        val bee     = handler.resolve<Bee>()
        assertNull(bee)
    }

    @Test fun `Provides callbacks implicitly`() {
        val handler = SimpleHandler()
        val bar     = handler.resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides singleton callbacks implicitly`() {
        val bar     = SingletonHandler.resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides explicit companion callbacks implicitly`() {
        val bar     = ExplicitCompanionHandler.resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides implicit companion callbacks implicitly`() {
        val bar = HandlerAdapter(
                ImplicitCompanionHandler.Companion)
                .resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides interface callbacks`() {
        val handler = InterfaceHandler()
        val testing = handler.resolve<Testing>()
        assertNotNull(testing)
        assertTrue(testing is Bar)
        assertFalse(testing.hasComposer)
        assertEquals(3, testing.handled)
    }

    @Test fun `Provides callbacks implicitly using adapter`() {
        val handler = HandlerAdapter(ControllerBase())
        val bar     = handler.resolve<Bar>()
        assertNotNull(bar)
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Provides callbacks implicitly with composer`() {
        val handler = SimpleHandler()
        val boo     = handler.resolve<Boo>()
        assertNotNull(boo)
        assertEquals(Boo::class, boo::class)
        assertTrue(boo.hasComposer)
    }

    @Test fun `Provides callbacks implicitly async`() {
        val handler = SimpleAsyncHandler()
        assertAsync(testName) { done ->
            handler.resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it.hasComposer)
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
                assertFalse(it.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
    }

    @Test fun `Provides callbacks covariantly`() {
        val handler = SimpleHandler()
        val bar     = handler.resolve<SpecialBar>()
        assertNotNull(bar)
        assertEquals(SpecialBar::class, bar::class)
        assertTrue(bar.hasComposer)
        assertEquals(1, bar.handled)
    }


    @Test fun `Provides callbacks for interface`() {
        val handler = SimpleHandler()
        val bar     = handler.resolve<BarComponent>()
        assertNotNull(bar)
        assertEquals(Bar::class, bar::class)
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
            handler.resolveAllAsync<Bar>() then { bars ->
                assertEquals(3, bars.size)
                assertTrue(bars.map { it.handled to it.hasComposer }
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
        assertNotNull(baz)
        assertEquals(SpecialBaz::class, baz::class)
        assertFalse(baz.hasComposer)
    }

    @Test fun `Provides many callbacks explicitly`() {
        val handler = SpecialHandler()
        val baz     = handler.resolve<Baz>()
        assertNotNull(baz)
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
        assertNotNull(baz)
        assertEquals("providesGenericBaz", baz.tag)
    }

    @Test fun `Provides callbacks generically using arity`() {
        val handler = SimpleHandler()
        val baz     = handler.resolve<BazTR<Int, String>>()
        assertNotNull(baz)
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
                assertFalse(it.hasComposer)
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
        assertFalse(bar.hasComposer)
        assertEquals(1, bar.handled)
    }

    @Test fun `Filters async provides async`() {
        val handler = SimpleHandler()
        assertAsync(testName) { done ->
            handler.aspectBefore({ _, _ -> Promise.TRUE })
                    .resolveAsync<Bar>() then {
                assertNotNull(it)
                assertFalse(it.hasComposer)
                assertEquals(1, it.handled)
                done()
            }
        }
    }

    @Test fun `Cancels provides async`() {
        val handler = SimpleHandler()
        assertFailsWith<RejectedException> {
            handler.aspectBefore({ _, _ -> Promise.FALSE })
                    .resolve<Bar>()
        }
    }

    @Test fun `Cancels async provides`() {
        val handler = SimpleHandler()
        assertFailsWith<RejectedException> {
            handler.aspectBefore({ _, _ -> false })
                    .resolveAsync<Bar>().get()
        }
    }

    @Test fun `Cancels async provides async`() {
        val handler = SimpleHandler()
        assertFailsWith<RejectedException> {
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
        val controller = ControllerBase()
        val handler    = HandlerAdapter(controller)
        val result     = handler.resolve<ControllerBase>()
        assertSame(controller, result)
    }

    @Test fun `Provides adapted self decorated`() {
        val controller = ControllerBase()
        val handler    = HandlerAdapter(controller)
        val result     = handler.broadcast.resolve<ControllerBase>()
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
        val foo     = handler.with(Foo()).resolve<Foo>()
        assertNotNull(foo)
    }

    @Test fun `Can override providers many`() {
        val foo1    = Foo()
        val foo2    = Foo()
        val handler = Handler()
        val foos    = handler.with(listOf(foo1, foo2))
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
        val foo     = handler.with(Bar()).resolve<Foo>()
        assertNull(foo)
    }

    @Test fun `Creates a pipeline from filters`() {
        val bar     = Bar()
        val handler = FilteringHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bar))
        assertEquals(2, bar.handled)
        assertEquals(4, bar.filters.size)
        assertTrue(bar.filters.contains(handler))
        assertEquals(bar.filters.asSequence()
                .filterIsInstance<ContravarintFilter>()
                .count(), 1)
        assertEquals(bar.filters.asSequence()
                .filterIsInstance<ExceptionBehavior<*,*>>()
                .count(), 1)
        assertEquals(bar.filters.asSequence()
                .filterIsInstance<LogFilter<*,*>>()
                .count(), 1)
    }

    @Test fun `Aborts pipeline`() {
        val bar     = Bar().apply { handled = 100 }
        val handler = FilteringHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bar))
        assertEquals(-99, bar.handled)
    }

    @Test fun `Skips filters`() {
        val bee     = Bee()
        val handler = FilteringHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(bee))
        assertEquals(0, bee.filters.size)
    }

    @Test fun `Skips non required filters`() {
        val bar     = Bar()
        val handler = FilteringHandler()
        assertEquals(HandleResult.HANDLED, handler.skipFilters().handle(bar))
        assertEquals(3, bar.filters.size)
        assertTrue(bar.filters.contains(handler))
        assertEquals(bar.filters.asSequence()
                .filterIsInstance<ContravarintFilter>()
                .count(), 1)
        assertEquals(bar.filters.asSequence()
                .filterIsInstance<ExceptionBehavior<*,*>>()
                .count(), 1)
    }

    @Test fun `Propagates rejected filter promise`() {
        val boo     = Boo()
        val handler = SpecialFilteringHandler() + FilteringHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(boo) catch {
                assertTrue(it is IllegalStateException)
                assertEquals("System shutdown", it.message)
                done()
            }
        }
    }

    @Test fun `Creates instance implicitly`() {
        factory.registerDescriptor<ControllerBase>()
        val instance = TypeHandlers.resolve<ControllerBase>()
        assertNotNull(instance)
        assertNotSame(instance, TypeHandlers.resolve()!!)
    }

    @Test fun `Creates generic instance implicitly`() {
        val view = Screen()
        val bar  = SpecialBar()
        factory.registerDescriptor<Controller<*,*>>()
        val instance = TypeHandlers
                .with(view).with(bar)
                .resolve<Controller<Screen, Bar>>()
        assertNotNull(instance)
        assertSame(view, instance.view)
        assertSame(bar, instance.model)
    }

    @Test fun `Infers instance implicitly`() {
        val foo = Foo()
        assertEquals(HandleResult.HANDLED, TypeHandlers.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Infers singleton callbacks implicitly`() {
        val foo = Foo()
        factory.registerDescriptor<SingletonHandler>()
        assertEquals(HandleResult.HANDLED, TypeHandlers.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Infers explicit companion callbacks implicitly`() {
        val foo = Foo()
        factory.registerDescriptor<ExplicitCompanionHandler>()
        assertEquals(HandleResult.HANDLED, TypeHandlers.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Infers implicit companion callbacks implicitly`() {
        val foo = Foo()
        factory.registerDescriptor<ImplicitCompanionHandler>()
        assertEquals(HandleResult.HANDLED, TypeHandlers.handle(foo))
        assertEquals(1, foo.handled)
    }

    @Test fun `Infers generic instance implicitly`() {
        val boo = Boo()
        val baz = SpecialBaz()
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<Controller<*,*>>()
        val instance = TypeHandlers
                .with(boo).with(baz)
                .resolve<Controller<Boo, Baz>>()
        assertNotNull(instance)
        assertSame(boo, instance.view)
        assertSame(baz, instance.model)
    }

    @Test fun `Provides instance implicitly`() {
        factory.registerDescriptor<ControllerBase>()
        val bar = TypeHandlers.resolve<Bar>()
        assertNotNull(bar)
    }

    @Test fun `Provides dependencies implicitly`() {
        val view = Screen()
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<Controller<*,*>>()
        val instance = TypeHandlers
                .with(view).resolve<Controller<Screen, Bar>>()
        assertNotNull(instance)
        assertSame(view, instance.view)
    }

    @Test fun `Detects circular dependencies`() {
        val view = Screen()
        HandlerDescriptorFactory.useFactory(
                MutableHandlerDescriptorFactory().apply {
                    registerDescriptor<Controller<*,*>>()
                }
        )
        val instance = TypeHandlers
                .with(view).resolve<Controller<Screen, Bar>>()
        assertNull(instance)
    }

    @Test fun `Creates singleton instance implicitly`() {
        factory.registerDescriptor<ApplicationBase>()
        val app = TypeHandlers.resolve<ApplicationBase>()
        assertNotNull(app)
        assertSame(app, TypeHandlers.resolve()!!)
    }

    @Test fun `Creates generic singleton instance implicitly`() {
        val view    = Screen()
        val handler = TypeHandlers
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<Controller<*,*>>()
        factory.registerDescriptor<Application<*>>()
        factory.registerDescriptor<InitializingComponent1>()
        factory.registerDescriptor<InitializingComponent2>()
        val app1 = handler.with(view)
                .resolve<Application<Controller<Screen, Bar>>>()
        assertNotNull(app1)
        assertSame(view, app1.rootController.view)
        assertSame(view, app1.mainScreen)
        assertTrue(app1.initialized)
        assertEquals(1, app1.initializeCount)
        assertTrue(app1.component1.initialized)
        assertTrue(app1.component2.initialized)
        val app2 = handler.with(view)
                .resolve<Application<Controller<Screen, Bar>>>()
        assertSame(app1, app2)
        assertTrue(app2!!.initialized)
        assertEquals(1, app2.initializeCount)
        val app3 = handler.with(view)
                .resolve<App<Controller<Screen, Bar>>>()
        assertSame(app1, app3)
    }

    @Test fun `Returns same scoped instance without qualifier`() {
        var screen: Screen? = null
        factory.registerDescriptor<Screen>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            screen = context.resolve()
            assertNotNull(screen)
            assertSame(context, screen!!.context)
            assertSame(screen, context.resolve())
            assertFalse(screen!!.closed)
            context.createChild().use { child ->
                val screen2 = child.resolve<Screen>()
                assertNotNull(screen2)
                assertSame(screen, screen2)
                assertSame(child.parent, screen2.context)
            }
        }
        assertTrue(screen!!.closed)
    }

    @Test fun `Creates scoped instance implicitly`() {
        var screen: Screen? = null
        factory.registerDescriptor<Screen>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            screen = context.resolve()
            assertNotNull(screen)
            assertSame(context, screen!!.context)
            assertSame(screen, context.resolve())
            assertFalse(screen!!.closed)
            context.createChild().use { child ->
                val screen2 = child.resolve<Screen> {
                    require(Qualifier<Scoped>())
                }
                assertNotNull(screen2)
                assertNotSame(screen, screen2)
                assertSame(child, screen2.context)
            }
        }
        assertTrue(screen!!.closed)
    }

    @Test fun `Creates rooted scoped instance implicitly`() {
        factory.registerDescriptor<RootedComponent>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val rooted = context.resolve<RootedComponent>()
            assertNotNull(rooted)
            assertSame(context, rooted.context)
            context.createChild().use { child ->
                val rooted2 = child.resolve<RootedComponent> {
                    require(Qualifier<Scoped>())
                }
                assertNotNull(rooted2)
                assertSame(rooted, rooted2)
                assertSame(context, rooted2.context)
            }
        }
    }

    @Test fun `Creates scpoed instance covariantly`() {
        factory.registerDescriptor<ScreenModel<*>>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val view = context.with(Bar()).resolve<View<Bar>>()
            assertNotNull(view)
            assertSame(view, context.resolve()!!)
        }
    }

    @Test fun `Creates scoped instance covariantly inferred`() {
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<ScreenModel<*>>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val view = context.resolve<View<Bar>>()
            assertNotNull(view)
            assertSame(view, context.resolve()!!)
        }
    }

    @Test fun `Creates generic scoped instance implicitly`() {
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<ScreenModel<*>>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val screen1 = context.with(Foo()).resolve<ScreenModel<Foo>>()
            assertNotNull(screen1)
            val screen2 = context.resolve<ScreenModel<Bar>>()
            assertSame(screen1, context.resolve()!!)
            assertSame(screen2, context.resolve())
        }
    }

    @Test fun `Provides generic scoped instance implicitly`() {
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<ScreenModelProvider>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val screen1 = context.with(Foo())
                    .resolve<ScreenModel<Foo>>()
            assertNotNull(screen1)
            val screen2 = context.resolve<ScreenModel<Bar>>()
            assertNotNull(screen2)
            assertSame(screen1, context.resolve()!!)
            assertSame(screen2, context.resolve()!!)
            assertNull(context.resolve<ScreenModel<Boo>>())
        }
    }

    @Test fun `Ignores scoped errors`() {
        factory.registerDescriptor<ControllerBase>()
        factory.registerDescriptor<ScopedErrorProvider>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val screen1 = context.with(Foo())
                    .resolve<ScreenModel<Foo>>()
            assertNotNull(screen1)
            val screen2 = context.resolve<ScreenModel<Bar>>()
            assertNull(screen2)
            val screen3 = context.resolve<ScreenModel<Bar>>()
            assertNotNull(screen3)
            assertSame(screen1, context.resolve()!!)
            assertSame(screen3, context.resolve()!!)
            assertNull(context.resolve<ScreenModel<Boo>>())
        }
    }

    @Test fun `Rejects scoped creation if no context`() {
        factory.registerDescriptor<Screen>()
        val screen = TypeHandlers.resolve<Screen>()
        assertNull(screen)
    }

    @Test fun `Rejects changing managed context`() {
        factory.registerDescriptor<Screen>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val screen = context.resolve<Screen>()
            assertSame(context, screen!!.context)
            assertFailsWith<IllegalStateException>(
                    "Managed instances cannot change context") {
                screen.context = Context()
            }
        }
    }

    @Test fun `Detaches context when assigned null`() {
        factory.registerDescriptor<Screen>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            val screen = context.resolve<Screen>()
            assertSame(context, screen!!.context)
            screen.context = null
            assertNotSame(screen, context.resolve()!!)
            assertTrue(screen.closed)
        }
    }

    @Test fun `Rejects constructor if initializer fails`() {
        factory.registerDescriptor<FailedInitialization>()
        assertAsync(testName) { done ->
            TypeHandlers.resolveAsync<FailedInitialization>() then {
                assertNull(it)
                done()
            }
        }
    }

    @Test fun `Rejects scoped dependency in singleton`() {
        factory.registerDescriptor<Screen>()
        factory.registerDescriptor<LifestyleMismatch>()
        Context().use { context ->
            context.addHandlers(TypeHandlers)
            assertNull(context.resolve<LifestyleMismatch>())
        }
    }

    @Test fun `Selects greediest consructor`() {
        HandlerDescriptorFactory.useFactory(
                MutableHandlerDescriptorFactory().apply {
                    registerDescriptor<OverloadedConstructors>()
                }
        )
        val ctor = TypeHandlers.resolve<OverloadedConstructors>()
        assertNotNull(ctor)
        assertNull(ctor.foo)
        assertNull(ctor.bar)
        val ctor1 = TypeHandlers.with(Foo()).resolve<OverloadedConstructors>()
        assertNotNull(ctor1)
        assertNotNull(ctor1.foo)
        assertNull(ctor1.bar)
        val ctor2 = TypeHandlers.with(Foo()).with(Bar())
                .resolve<OverloadedConstructors>()
        assertNotNull(ctor2)
        assertNotNull(ctor2.foo)
        assertNotNull(ctor2.bar)
    }

    @Test fun `Selects greediest consructor when resolving all`() {
        HandlerDescriptorFactory.useFactory(
                MutableHandlerDescriptorFactory().apply {
                    registerDescriptor<OverloadedConstructors>()
                }
        )
        val ctor = TypeHandlers.resolveAll<OverloadedConstructors>()
        assertEquals(1, ctor.size)
        assertNotNull(ctor[0])
        assertNull(ctor[0].foo)
        assertNull(ctor[0].bar)
        val ctor1 = TypeHandlers.with(Foo()).resolveAll<OverloadedConstructors>()
        assertEquals(1, ctor1.size)
        assertNotNull(ctor1[0])
        assertNotNull(ctor1[0].foo)
        assertNull(ctor1[0].bar)
        val ctor2 = TypeHandlers.with(Foo()).with(Bar())
                .resolveAll<OverloadedConstructors>()
        assertEquals(1, ctor2.size)
        assertNotNull(ctor2[0])
        assertNotNull(ctor2[0].foo)
        assertNotNull(ctor2[0].bar)
    }

    @Test fun `Ignores options at boundary `() {
        val handler = Handler().withFilters(LogFilter<Any,Any?>())
        val options = FilterOptions()
        assertEquals(HandleResult.HANDLED, handler.handle(options))
        assertEquals(1, options.providers!!.size)
        assertEquals(HandleResult.NOT_HANDLED,
                handler.stop.handle(FilterOptions()))
    }

    @Test fun `Checks filter open conformance`() {
        val openType = Filtering::class.createType(listOf(
                KTypeProjection.invariant(
                        Filtering::class.typeParameters[0].createType()),
                KTypeProjection.invariant(
                        Filtering::class.typeParameters[1].createType()))
        )
        val otherType = kTypeOf<ExceptionBehavior<Boo,String>>()
        val bindings  = mutableMapOf<KTypeParameter, KType>()
        assertNotNull(openType.checkOpenConformance(otherType, bindings))
        assertEquals(2, bindings.size)
        assertEquals(kTypeOf<Boo>(), bindings.values.first())
        assertEquals(kTypeOf<String>(), bindings.values.elementAt(1))
    }

    @Test fun `Checks filter partial conformance`() {
        val openType = Filtering::class.createType(listOf(
                KTypeProjection.contravariant(kTypeOf<Boo>()),
                KTypeProjection.invariant(
                        Filtering::class.typeParameters[1].createType()))
        )
        val otherType = kTypeOf<ExceptionBehavior<Boo,Unit>>()
        val bindings  = mutableMapOf<KTypeParameter, KType>()
        assertNotNull(openType.checkOpenConformance(otherType, bindings))
        assertEquals(1, bindings.size)
        assertEquals(TypeReference.UNIT_TYPE, bindings.values.first())
    }

    @Test fun `Rejects filter open conformance`() {
        val openType = Filtering::class.createType(listOf(
                KTypeProjection.contravariant(kTypeOf<Bar>()),
                KTypeProjection.invariant(
                        Filtering::class.typeParameters[1].createType()))
        )
        val otherType = kTypeOf<ExceptionBehavior<Boo,Unit>>()
        assertFalse(openType.checkOpenConformance(otherType))
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

    interface BarComponent {
        val handled: Int
    }

    open class Bar : Testing(), BarComponent

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
        fun handleFooImplicitly(foo: Foo) {
            ++foo.handled
        }

        @Handles
        fun handleSpecialFooImplicitly(foo: SpecialFoo) : HandleResult? {
            ++foo.handled
            foo.hasComposer = true
            return null
        }

        @Handles
        fun notHandleBeeExplicitly(bam: Bam) =
                HandleResult.NOT_HANDLED_AND_STOP

        @Handles
        fun handleBarExplicitly(bar: Bar, composer: Handling): HandleResult? {
            ++bar.handled
            bar.hasComposer = true
            return when {
                bar.handled % 2 == 1 -> HandleResult.HANDLED
                else -> null
            }
        }

        @Handles
        fun <T> handlesConcreteBaz(baz: BazT<Foo>, type: KType) {
            assertEquals(kTypeOf<BazT<Foo>>(), type)
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
        fun <T,R> handlesGenericBazr(baz: BazTR<T, R>) : HandleResult? {
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
                else -> null
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
        fun providesByName(inquiry: Inquiry): Promise<Any>? {
            return when (inquiry.key) {
                "Foo" -> Promise.resolve(Foo())
                "Bar" -> Promise.resolve(Bar())
                else -> null
            }
        }
    }

    class SpecialHandler : Handler() {
        @Handles
        fun handleAnything(cb: Any?) {}

        @Handles
        fun <T,R> handlesGenericBaz(
                baztr: BazTR<T, R>,
                bazt:  BazT<T>,
                t:     T,
                r:     R
        ) : HandleResult? {
            bazt.stuff       = t
            baztr.stuff      = t
            baztr.otherStuff = r
            return HandleResult.HANDLED
        }

        @Provides
        fun providesManyBars(): List<Bar> {
            return listOf(
                    Bar().apply { handled = 1 },
                    Bar().apply { handled = 2 })
        }

        @get:Provides
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
                binding: PolicyMemberBinding
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

        @get:Provides
        val providesPropertyBar: Promise<Bar>
            get() = Promise.resolve(Bar().apply { handled = 3 })

        @Provides
        fun providesBazExplicitly(
                inquiry:  Inquiry,
                composer: Handling,
                binding: PolicyMemberBinding
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

    object SingletonHandler : Handler() {
        @Handles
        fun handleFooImplicitly(foo: Foo) {
            ++foo.handled
        }

        @Provides
        fun provideBarImplicitly(): Bar  {
            return Bar().apply { handled = 1 }
        }
    }

    class ImplicitCompanionHandler : Handler() {
        companion object {
            @Handles
            fun handleFooImplicitly(foo: Foo) {
                ++foo.handled
            }

            @Provides
            fun provideBarImplicitly(): Bar  {
                return Bar().apply { handled = 1 }
            }
        }
    }

    class ExplicitCompanionHandler {
        companion object : Handler() {
            @Handles
            fun handleFooImplicitly(foo: Foo) {
                ++foo.handled
            }

            @Provides
            fun provideBarImplicitly(): Bar  {
                return Bar().apply { handled = 1 }
            }
        }
    }

    class InvalidHandler : Handler() {
        @Handles
        fun reset() = 22
    }

    class InvalidProvider : Handler() {
        @Provides
        fun add(op1: Int, op2: Int){}
    }

    open class ControllerBase @Provides constructor() {
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

    interface View<TModel> {
        val model: TModel
    }

    class Controller<TView, TModel> @Provides constructor(
            val view:  TView,
            val model: TModel
    ) : ControllerBase()

    open class Screen @Provides @Scoped
        constructor() : ContextualImpl(), AutoCloseable {

        var closed: Boolean = false
            private set

        override fun close() {
            closed = true
        }
    }

    class ScreenModel<M> @Provides @Scoped
        constructor(override val model: M): Screen(), View<M>

    object ScreenModelProvider {
        @Provides @Scoped
        fun <M> getScreen(model: M) =
                Promise.resolve(ScreenModel(model))
    }

    object ScopedErrorProvider {
        private var count = 0

        @Provides @Scoped
        fun <M> getScreen(model: M) =
                if (++count % 2 == 0) {
                    throw InvalidStateException("Something bad")
                } else {
                    Promise.resolve(ScreenModel(model))
                }
    }

    open class ApplicationBase
        @Provides @Singleton constructor()

    interface App<C: ControllerBase> {
        val rootController: C
        val mainScreen:     Screen
    }

    class Application<C: ControllerBase>
        @Provides @Singleton constructor(
                override val rootController: C,
                override val mainScreen:     Screen,
                val component1: InitializingComponent1,
                val component2: InitializingComponent2
        ): ApplicationBase(), App<C>, Initializing {
        override var initialized = false
        var initializeCount = 0
            private set

        override fun initialize(): Promise<*>? {
            return Promise.TRUE then {
                initialized = true
                ++initializeCount
            }
        }

        override fun failedInitialize(t: Throwable?) {
        }
    }

    open class RootedComponent @Provides @Scoped(rooted = true)
    constructor() : ContextualImpl(), AutoCloseable {

        var closed: Boolean = false
            private set

        override fun close() {
            closed = true
        }
    }

    class InitializingComponent1
        @Provides @Singleton
        constructor() : Initializing {

        override var initialized = false

        override fun initialize(): Promise<*>? {
            return Promise.delay(100) then {
                initialized = true
            }
        }

        override fun failedInitialize(t: Throwable?) {
        }
    }

    class InitializingComponent2
        @Provides @Singleton
        constructor() : Initializing {

        override var initialized = false

        override fun initialize(): Promise<*>? {
            return Promise.delay(200) then {
                initialized = true
            }
        }
        override fun failedInitialize(t: Throwable?) {
        }
    }

    class FailedInitialization
        @Provides @Singleton
        constructor(): Initializing {

        override var initialized = false

        override fun initialize(): Promise<*>? {
            return Promise.reject(InvalidStateException(
                    "Initializion failed"
            ))
        }

        override fun failedInitialize(t: Throwable?) {
            assertEquals("Initializion failed", t?.message)
        }
    }

    class LifestyleMismatch
        @Provides @Singleton constructor(
            screen: Screen
        )

    class OverloadedConstructors @Provides constructor() {
        var foo: Foo? = null
        var bar: Bar? = null

        @Provides constructor(foo: Foo) : this() {
            this.foo = foo
        }

        @Provides constructor(foo: Foo, bar: Bar) : this() {
            this.foo = foo
            this.bar = bar
        }
    }

    class FilteringHandler : Handler(), Filtering<Bar, Unit> {
        override var order: Int? = null

        @Handles
        @Log @Contravarint @Exceptions @Aborting
        fun handleBar(bar: Bar) {
            bar.handled++
        }

        @Handles
        @Log @SkipFilters
        fun handleBee(bee: Bee) {
        }

        @Handles
        fun handleStuff(command: Command): Promise<Any?>? {
            when (val callback = command.callback) {
                is Bar -> callback.handled = -99
                else -> return null
            }
            return Promise.EMPTY
        }

        @Provides
        fun <T: Any, R: Any?> forLogging(
                inquiry: Inquiry
        ): LogFilter<T,R> = LogFilter()

        @Provides
        fun createContravarintFilter() =
                ContravarintFilter()

        @Provides
        fun <T: Any, R: Any?> forExceptions(
                inquiry: Inquiry
        ): ExceptionBehavior<T,R>? {
            @Suppress("UNCHECKED_CAST")
            return when (inquiry.keyClass) {
                ExceptionBehavior::class -> ExceptionBehavior()
                else -> null
            }
        }

        @Provides
        fun <R: Any?> createAborting(
                inquiry: Inquiry
        ): AbortingFilter<R> = AbortingFilter()

        override fun next(
                callback:    Bar,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Unit>,
                provider:    FilteringProvider?
        ): Promise<Unit> {
            callback.filters.add(this)
            callback.handled++
            return next()
        }
    }

    class SpecialFilteringHandler : Handler() {
        @Handles
        @Log @Contravarint @Exceptions
        fun handleFoo(foo: Foo): SpecialFoo {
            return SpecialFoo()
        }

        @Handles
        @Log @Contravarint @Exceptions
        fun handleBaz(baz: Baz): Promise<SpecialBaz> {
            return Promise.resolve(SpecialBaz())
        }

        @Handles
        @Log @Contravarint @Exceptions
        fun handleBar(bar: Bar): Promise<SpecialBar> {
            return Promise.resolve(SpecialBar())
        }

        @Handles
        @Exceptions
        fun remove(boo: Boo) {
        }
    }

    class FilterHandlerTests : Handler() {
        @Handles
        @ReplaceComposer
        fun handleFoo(foo: Foo, bar: Bar): Bar = bar

        @Provides
        fun <T: Any, R: Any?> createFilter(
                inquiry: Inquiry
        ): ReplaceComposerFilter<T,R> = ReplaceComposerFilter()
    }

    class RequestFilter<in T: Any, R: Any?> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback:    T,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<R>,
                provider:    FilteringProvider?
        ) = next()
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(RequestFilter::class, required = true)
    annotation class Request

    class RequestCbFilter<in T: Any> : Filtering<T, Any?> {
        override var order: Int? = null

        override fun next(
                callback:    T,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Any?>,
                provider:    FilteringProvider?
        ) = next()
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(RequestCbFilter::class, required = true)
    annotation class RequestCb

    class RequestResFilter<T> : Filtering<Any, T> {
        override var order: Int? = null

        override fun next(
                callback:    Any,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<T>,
                provider:    FilteringProvider?
        ) = next()
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(RequestResFilter::class, required = true)
    annotation class RequestRes

    class ContravarintFilter : Filtering<Any, Unit> {
        override var order: Int? = null

        override fun next(
                callback:    Any,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Unit>,
                provider:    FilteringProvider?
        ): Promise<Unit> {
            val cb = extractTesting(callback)
            cb?.filters?.add(this)
            return next()
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(ContravarintFilter::class, required = true)
    annotation class Contravarint

    class LogFilter<in Cb: Any, Res: Any?> : Filtering<Cb, Res> {
        override var order: Int? = 1

        override fun next(
                callback:    Cb,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Res>,
                provider:    FilteringProvider?
        ): Promise<Res> {
            val cb = extractTesting(callback)
            cb?.filters?.add(this)
            println("Filter log $cb")
            return next()
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(LogFilter::class)
    annotation class Log

    class ReplaceComposerFilter<in Cb: Any, Res: Any?> : Filtering<Cb, Res> {
        override var order: Int? = 1

        override fun next(
                callback:    Cb,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Res>,
                provider:    FilteringProvider?
        ) = next(composer = composer.provide(Bar()))
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(ReplaceComposerFilter::class)
    annotation class ReplaceComposer

    class AbortingFilter<R: Any?> : Filtering<Bar, R> {
        override var order: Int? = 0

        override fun next(
                callback:    Bar,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<R>,
                provider:    FilteringProvider?
        ) = when {
                callback.handled > 99 -> next.abort()
                else -> next()
            }
        }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(AbortingFilter::class, required = true)
    annotation class Aborting

    class ExceptionBehavior<in Req: Any, Res: Any?> : Filtering<Req, Res> {
        override var order: Int? = 2

        override fun next(
                callback:    Req,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Res>,
                provider:    FilteringProvider?
        ): Promise<Res> {
            val cb = extractTesting(callback)
            cb?.filters?.add(this)
            val result = next()
            if (callback is Boo) {
                return Promise.reject(IllegalStateException("System shutdown"))
            }
            return result
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(ExceptionBehavior::class, required = true)
    annotation class Exceptions

    companion object {
        private fun extractTesting(callback: Any): Testing? {
            return (callback as? Testing)
                    ?: (callback as? Command)?.let {
                        it.callback as? Testing
                    }
        }
    }
}