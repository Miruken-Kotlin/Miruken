package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.displayImmediate
import com.miruken.mvc.option.unloadRegion
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class NavigatorTest {
    private lateinit var rootContext: Context
    private lateinit var navigator: Navigator

    @Before
    fun setup() {
        rootContext = Context()
        navigator   = Navigator(TestViewRegion())
        rootContext.addHandlers(navigator, TypeHandlers)

        HandlerDescriptor.resetDescriptors()
        HandlerDescriptor.getDescriptor<HelloController>()
        HandlerDescriptor.getDescriptor<GoodbyeController>()
        HandlerDescriptor.getDescriptor<PartialController>()
    }

    @After
    fun cleanup() {
        rootContext.end()
    }

    class HelloController
        @Provides @Scoped
        constructor(): Controller() {

        fun sayHello(name: String): RegionOptions? {
            println("Hello $name")
            val navigation = io.resolve<Navigation<*>>()
            assertNotNull(navigation)
            push<GoodbyeController> { sayGoodbye(name) }
            return io.getOptions(RegionOptions())
        }

        fun compose() {
            partial<PartialController> { render() }
        }

        fun render() {
            assertNotNull(show<TestView>())
            val navigation = io.resolve<Navigation<*>>()
            assertNotNull(navigation)
            assertSame(this, navigation.controller)
            assertSame(navigation, context!!.resolve()!!)
        }
    }

    class GoodbyeController
        @Provides @Scoped
        constructor() : Controller() {

        fun sayGoodbye(name: String) {
            println("Goodbye $name")
        }
    }

    class PartialController
    @Provides @Scoped
    constructor() : Controller() {

        fun render() {
            val navigation = io.resolve<Navigation<*>>()
            assertSame(this, navigation!!.controller)
            assertNotNull(navigation)
            val initiator = context!!.resolve<Navigation<*>>()
            assertSame(this, initiator!!.controller)
        }
    }

    @Test fun `Fails navigation if no context`() {
        assertFailsWith(IllegalStateException::class) {
            navigator.next<HelloController> { sayHello("hi") }
        }
    }

    @Test fun `Navigates to next controller`() {
        rootContext.next<HelloController> {
            sayHello("Brenda")
            assertSame(rootContext, context?.parent)
        }
    }

    @Test fun `Navigates to push controller`() {
        rootContext.push<HelloController> {
            sayHello("Craig")
            assertSame(rootContext, context?.parent)
        }
    }

    @Test fun `Navigates to partial controller`() {
        rootContext.next<HelloController> {
            compose()
            assertNull(context)
        }
    }

    @Test fun `Propagates next options`() {
        rootContext.unloadRegion
                .next<HelloController> {
                    val options = sayHello("Lauren")
                    assertNotNull(options?.layer)
                    assertNull(options!!.layer!!.push)
                    assertEquals(options.layer!!.unload, true)
                }
    }

    @Test fun `Propagates push options`() {
        rootContext.displayImmediate
                .push<HelloController> {
                    val options = sayHello("Matthew")
                    assertNotNull(options?.layer)
                    assertEquals(options!!.layer!!.push, true)
                    assertEquals(options.layer!!.immediate, true)
                }
    }

    @Test fun `Renders a view`() {
        rootContext.next<HelloController> { render() }
    }
}