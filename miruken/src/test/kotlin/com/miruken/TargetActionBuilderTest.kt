package com.miruken

import com.miruken.callback.Handler
import com.miruken.callback.Provider
import com.miruken.callback.execute
import com.miruken.callback.policy.*
import com.miruken.callback.with
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class TargetActionBuilderTest {
    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<Provider>()
        HandlerDescriptorFactory.useFactory(factory)
    }
    @Test fun `Creates single argument action`() {
        val foo    = Foo()
        var called = false
        val target = targetAction<TargetActionBuilderTest, Unit> {
            assertTrue(it(Handler().with(foo)))
            called = true
        }
        target { a: Foo ->
            assertTrue { matches(a to foo) }
        }
        assertTrue(called)
    }

    @Test fun `Creates two argument action`() {
        val foo    = Foo()
        val bar    = Bar<String>()
        var called = false
        val target = targetAction<TargetActionBuilderTest, Unit> {
            assertTrue(it(Handler().with(foo).with(bar)))
            called = true
        }
        target { a: Foo, b: Bar<String> ->
            assertTrue { matches(a to foo, b to bar) }
        }
        assertTrue(called)
    }

    @Test fun `Creates optional argument action`() {
        val foo    = Foo()
        var called = false
        val target = targetAction<TargetActionBuilderTest, Unit> {
            assertTrue(it(Handler().with(foo)))
            called = true
        }
        target { a: Optional<Foo> ->
            assertTrue { matches(a.get() to foo) }
        }
        assertTrue(called)
    }

    @Test fun `Creates empty optional argument action`() {
        var called = false
        val target = targetAction<TargetActionBuilderTest, Unit> {
            assertTrue(it(Handler()))
            called = true
        }
        target { a: Optional<Foo> -> assertFalse(a.isPresent) }
        assertTrue(called)
    }

    @Test fun `Rejects target-action if args not resolved`() {
        var called = false
        val target = targetAction<TargetActionBuilderTest, Unit> {
            assertFalse(it { emptyList() })
            called = true
        }
        target { a: Foo -> }
        assertTrue(called)
    }

    @Test fun `Calls two argument action`() {
        val foo    = Foo()
        val bar    = Bar<String>()
        val called = Handler().with(foo).with(bar).execute { a: Foo, b: Bar<String> ->
            assertTrue { matches(a to foo, b to bar) }
            true
        }
        assertTrue(called.second!!)
    }

    @Test fun `Calls optional argument action`() {
        val bar    = Bar<String>()
        val called = Handler().with(bar).execute { a: Optional<Foo>, b: Optional<Bar<String>> ->
            assertFalse(a.isPresent)
            assertTrue { matches(b.get() to bar) }
            true
        }
        assertTrue(called.second!!)
    }

    @Test fun `Rejects missing argument action`() {
        assertFailsWith(IllegalStateException::class) {
            Handler().execute { a: Foo, b: Bar<String> -> }
        }
    }

    private fun matches(vararg args: Pair<Any?, Any?>) =
            args.all { it.first === it.second }
}