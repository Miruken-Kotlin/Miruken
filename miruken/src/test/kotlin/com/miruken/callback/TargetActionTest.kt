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
        val target = targetAction<TargetActionBuilderTest>()
        val action = target { a: Foo ->
            assertTrue { matches(a to foo) }
        }
        assertTrue(action(Handler().with(foo)))
    }

    @Test fun `Creates two argument action`() {
        val foo    = Foo()
        val bar    = Bar<String>()
        val target = targetAction<TargetActionBuilderTest>()
        val action = target { a: Foo, b: Bar<String> ->
            assertTrue { matches(a to foo, b to bar) }
        }
        assertTrue(action(Handler().with(foo).with(bar)))
    }

    @Test fun `Rejects target-action if args not resolved`() {
        val target = targetAction<TargetActionBuilderTest>()
        val action = target { a: Foo -> }
        assertFalse(action(Handler()))
    }

    private fun matches(vararg args: Pair<Any?, Any?>) =
            args.all { it.first === it.second }
}