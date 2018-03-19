package com.miruken.mvc

import com.miruken.context.Context
import com.miruken.context.ContextImpl
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.displayImmediate
import com.miruken.mvc.option.unloadRegion
import com.miruken.typeOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NavigatorTest {
    private lateinit var rootContext: Context
    private lateinit var navigator: Navigator

    @Before
    fun setup() {
        rootContext = ContextImpl()
        navigator   = Navigator(TestViewRegion())
        rootContext.addHandlers(navigator, TestContainer())
    }

    @After
    fun cleanup() {
        rootContext.end()
    }

    @Test fun `Navigates to next controller`() {
        rootContext.next<HelloController> {
            sayHello()
            assertSame(rootContext, context)
        }
    }

    @Test fun `Navigates to push controller`() {
        rootContext.push<HelloController> {
            sayHello()
            assertSame(rootContext, context?.parent)
        }
    }

    class HelloController : Controller() {
        fun sayHello(): RegionOptions? {
            println("Hello")
            val options = RegionOptions()
            return io.handle(options, true).success {
                options
            }
        }
    }

    @Test fun `Propagates next options`() {
        rootContext.unloadRegion
                .next<HelloController> {
                    val options = sayHello()
                    assertTrue(options?.layer?.push != true)
                    assertTrue(options?.layer?.unload == true)
                }
    }

    @Test fun `Propagates push options`() {
        rootContext.displayImmediate
                .push<HelloController> {
                    val options = sayHello()
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