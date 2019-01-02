package com.miruken.callback

import com.miruken.callback.policy.Bar
import com.miruken.callback.policy.Foo
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class TargetActionBuilderTest {
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

    @Test fun `Rejects target-action if args not resolved`() {
        var called = false
        val target = targetAction<TargetActionBuilderTest, Unit> {
            assertFalse(it(Handler()))
            called = true
        }
        target { a: Foo -> }
        assertTrue(called)
    }

    private fun matches(vararg args: Pair<Any?, Any?>) =
            args.all { it.first === it.second }
}