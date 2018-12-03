package com.miruken.mvc

import com.miruken.callback.Provides
import com.miruken.callback.TypeHandlers
import com.miruken.callback.getOptions
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.LazyHandlerDescriptorFactory
import com.miruken.callback.policy.getDescriptor
import com.miruken.callback.resolve
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.mvc.option.NavigationOptions
import com.miruken.mvc.option.displayImmediate
import com.miruken.mvc.option.unloadRegion
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class NavigatorTest {
    private lateinit var rootContext: Context
    private lateinit var navigator: Navigator
    private lateinit var factory: HandlerDescriptorFactory

    @Before
    fun setup() {
        rootContext = Context()
        navigator   = Navigator(TestViewRegion())
        rootContext.addHandlers(navigator, TypeHandlers)

        factory = LazyHandlerDescriptorFactory().apply {
            getDescriptor<HelloController>()
            getDescriptor<GoodbyeController>()
            getDescriptor<PartialController>()
            HandlerDescriptorFactory.current = this
        }
    }

    @After
    fun cleanup() {
        rootContext.end()
    }

    class HelloController
        @Provides @Scoped
        constructor(): Controller(), NavigatingAware {

        fun sayHello(name: String): NavigationOptions? {
            println("Hello $name")
            val navigation = io.resolve<Navigation<*>>()
            assertNotNull(navigation)
            push<GoodbyeController> { sayGoodbye(name) }
            return io.getOptions(NavigationOptions())
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

        override fun navigating(navigation: Navigation<*>) {
            println("${this::class.qualifiedName} Navigating ${navigation.style} to ${navigation.controllerKey}")
        }
    }

    class GoodbyeController
        @Provides @Scoped
        constructor() : Controller(), NavigatingAware {

        fun sayGoodbye(name: String) {
            println("Goodbye $name")
        }

        override fun navigating(navigation: Navigation<*>) {
            println("${this::class.qualifiedName} Navigating ${navigation.style} to ${navigation.controllerKey}")
        }
    }

    class PartialController
    @Provides @Scoped
    constructor() : Controller(), NavigatingAware {

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
                    assertNotNull(options?.region)
                    assertNull(options!!.region!!.push)
                    assertEquals(options.region!!.unload, true)
                }
    }

    @Test fun `Propagates push options`() {
        rootContext.displayImmediate
                .push<HelloController> {
                    val options = sayHello("Matthew")
                    assertNotNull(options?.region)
                    assertEquals(options!!.region!!.push, true)
                    assertEquals(options.region!!.immediate, true)
                }
    }

    @Test fun `Renders a view`() {
        rootContext.next<HelloController> { render() }
    }
}