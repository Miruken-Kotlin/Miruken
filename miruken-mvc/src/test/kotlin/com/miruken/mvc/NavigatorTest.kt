package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.mvc.option.NavigationOptions
import com.miruken.mvc.option.displayImmediate
import com.miruken.mvc.option.unloadRegion
import com.miruken.mvc.view.region
import com.miruken.test.assertAsync
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.*

class NavigatorTest {
    private lateinit var rootContext: Context
    private lateinit var navigator: Navigator
    private lateinit var factory: HandlerDescriptorFactory

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        rootContext = Context()
        navigator   = Navigator(TestViewRegion())
        rootContext.addHandlers(navigator, TypeHandlers)

        factory = MutableHandlerDescriptorFactory().apply {
            registerDescriptor<Navigator>()
            registerDescriptor<HelloController>()
            registerDescriptor<GoodbyeController>()
            registerDescriptor<PartialController>()
            registerDescriptor<Navigator>()
            HandlerDescriptorFactory.useFactory(this)
        }
    }

    @After
    fun cleanup() {
        rootContext.end()
    }

    class HelloController
        @Provides @Scoped
        constructor(): Controller() {

        fun sayHello(name: String): NavigationOptions? {
            println("Hello $name")
            val navigation = io.resolve<Navigation<*>>()
            assertNotNull(navigation)
            push<GoodbyeController>{ it.sayGoodbye(name) }
            return io.getOptions(NavigationOptions())
        }

        fun sayHelloRegion(name: String): NavigationOptions? {
            println("Hello region $name")
            show(context!!.region<GoodbyeController> { it.sayGoodbye("region $name") })
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

        fun nextEnd() {
            next<GoodbyeController> { it.endContext() }
        }

        fun exception() {
            throw IllegalStateException("Crashed")
        }

        fun nextException() {
            next<GoodbyeController> {
                throw IllegalStateException("No manners")
            }
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
        assertAsync(testName) { done ->
            navigator.next<HelloController> { it.sayHello("hi") } catch {
                assertTrue(it is NotHandledException)
                done()
            }
        }
    }

    @Test fun `Navigates to next controller`() {
        rootContext.next<HelloController> {
            it.sayHello("Brenda")
            assertSame(rootContext, it.context?.parent)
        }
    }

    @Test fun `Navigates to push controller`() {
        rootContext.push<HelloController> {
            it.sayHello("Craig")
            assertSame(rootContext, it.context?.parent?.parent)
        }
    }

    @Test fun `Navigates to next controller and pushes region`() {
        rootContext.next<HelloController> {
            it.sayHelloRegion("Brenda")
            assertSame(rootContext, it.context?.parent)
        }
    }

    @Test fun `Pushes a controller and joins`() {
        assertAsync(testName) { done ->
            rootContext.push<HelloController> {
                it.endContext()
                assertNull(it.context)
            } then {
                assertSame(rootContext, it.parent?.parent)
                done()
            }
        }
    }

    @Test fun `Push controller, next and join`() {
        assertAsync(testName) { done ->
            rootContext.push<HelloController> {
                it.nextEnd()
                assertNull(it.context)
            } then {
                assertSame(rootContext, it.parent?.parent)
                done()
            }
        }
    }

    @Test fun `Navigates to partial controller`() {
        rootContext.next<HelloController> {
            it.compose()
            assertNull(it.context)
        }
    }

    @Test fun `Fails navigation if action throws exception`() {
        assertAsync(testName) { done ->
            rootContext.next<HelloController> { it.exception() } catch {
                assertTrue(it is NavigationException)
                assertEquals("Crashed", it.cause?.message)
                assertSame(rootContext, it.context)
                done()
            }
        }
    }

    @Test fun `Fails navigation if next action throws exception`() {
        assertAsync(testName) { done ->
            rootContext.push<HelloController> { it.nextException() } catch {
                assertTrue(it is NavigationException)
                assertEquals("No manners", it.cause?.message)
                assertSame(rootContext, it.context.parent?.parent)
                done()
            }
        }
    }

    @Test fun `Navigates next after failed navigation`() {
        assertAsync(testName) { done ->
            rootContext.push<HelloController> { it.nextException() } catch {
                assertTrue(it is NavigationException)
                it.context.parent?.parent?.next<GoodbyeController> { it.sayGoodbye("Joe")}
                done()
            }
        }
    }

    @Test fun `Propagates next options`() {
        rootContext.unloadRegion
                .next<HelloController> {
                    val options = it.sayHello("Lauren")
                    assertNotNull(options?.region)
                    assertNull(options!!.region!!.push)
                    assertEquals(options.region!!.unload, true)
                }
    }

    @Test fun `Propagates push options`() {
        rootContext.displayImmediate
                .push<HelloController> {
                    val options = it.sayHello("Matthew")
                    assertNotNull(options?.region)
                    assertEquals(options!!.region!!.push, true)
                    assertEquals(options.region!!.immediate, true)
                }
    }

    @Test fun `Renders a view`() {
        rootContext.next<HelloController> { it.render() }
    }
}