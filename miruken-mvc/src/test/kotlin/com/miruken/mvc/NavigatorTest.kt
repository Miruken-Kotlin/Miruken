package com.miruken.mvc

import com.miruken.callback.handle
import com.miruken.context.Context
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.displayImmediate
import com.miruken.mvc.option.unloadRegion
import com.miruken.typeOf
import org.junit.After
import org.junit.Before
import org.junit.Test
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
        rootContext.addHandlers(navigator, TestContainer())
    }

    @After
    fun cleanup() {
        rootContext.end()
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

    class HelloController : Controller() {
        fun sayHello(name: String): RegionOptions? {
            println("Hello $name")
            push<GoodbyeController> { sayGoodbye(name) }
            val options = RegionOptions()
            return io.handle(options, true).success {
                options
            }
        }
    }

    class GoodbyeController : Controller() {
        fun sayGoodbye(name: String) {
            println("Goodbye $name")
            endContext()
        }
    }

    @Test fun `Propagates next options`() {
        rootContext.unloadRegion
                .next<HelloController> {
                    val options = sayHello("Lauren")
                    assertTrue(options?.layer?.push != true)
                    assertTrue(options?.layer?.unload == true)
                }
    }

    @Test fun `Propagates push options`() {
        rootContext.displayImmediate
                .push<HelloController> {
                    val options = sayHello("Matthew")
                    assertTrue(options?.layer?.push == true)
                    assertTrue(options?.layer?.immediate == true)
                }
    }

    @Test fun `Fails navigation if no context`() {
        assertFailsWith(IllegalStateException::class) {
            navigator.navigate(
                    typeOf<HelloController>(),
                    NavigationStyle.NEXT) {
            }
        }
    }
}