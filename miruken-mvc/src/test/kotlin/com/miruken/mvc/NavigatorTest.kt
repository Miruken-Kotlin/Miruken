package com.miruken.mvc

import com.miruken.callback.Provides
import com.miruken.callback.TypeHandlers
import com.miruken.callback.handle
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.displayImmediate
import com.miruken.mvc.option.unloadRegion
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
            push<GoodbyeController> { sayGoodbye(name) }
            val options = RegionOptions()
            return io.handle(options, true).success {
                options
            }
        }
    }

    class GoodbyeController
        @Provides @Scoped
        constructor() : Controller() {

        fun sayGoodbye(name: String) {
            println("Goodbye $name")
            endContext()
        }
    }

    @Test fun `Navigates to next controller`() {
        rootContext.next<HelloController> {
            sayHello("Brenda")
            assertSame(rootContext, context)
        }
    }

    @Test fun `Navigates to push controller`() {
        rootContext.push<HelloController> {
            sayHello("Craig")
            assertSame(rootContext, context?.parent)
        }
    }

    @Test fun `Propagates next options`() {
        rootContext.unloadRegion
                .next<HelloController> {
                    val options = sayHello("Lauren")
                    assertTrue(options?.layer?.push != true)
                    assertEquals(options?.layer?.unload, true)
                }
    }

    @Test fun `Propagates push options`() {
        rootContext.displayImmediate
                .push<HelloController> {
                    val options = sayHello("Matthew")
                    assertEquals(options?.layer?.push, true)
                    assertEquals(options?.layer?.immediate, true)
                }
    }

    @Test fun `Fails navigation if no context`() {
        assertFailsWith(IllegalStateException::class) {
            navigator.next<HelloController> { sayHello("hi") }
        }
    }
}