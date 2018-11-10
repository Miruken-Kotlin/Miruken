package com.miruken.callback

import com.miruken.callback.policy.Bar
import com.miruken.callback.policy.Foo
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class TargetActionBuilderTest {
    private var called = false

    @Before
    fun setup() {
        called = false
    }

    @Test fun `Creates single argument action`() {
        val foo    = Foo()
        val target = Handler().with(foo)
                .target<TargetActionBuilderTest>()
        target { a: Foo ->
            assertTrue { matches(a to foo) }
        }?.invoke(this)
        assertTrue(called)
    }

    @Test fun `Creates two argument action`() {
        val foo    = Foo()
        val bar    = Bar<String>()
        val target = Handler().with(foo).with(bar)
                .target<TargetActionBuilderTest>()
        target { a: Foo, b: Bar<String> ->
            assertTrue { matches(a to foo, b to bar) }
        }?.invoke(this)
        assertTrue(called)
    }

    @Test fun `Rejects target-action if args not resolved`() {
        val target = Handler().target<TargetActionBuilderTest>()
        assertNull(target { a: TestHandler.Foo -> })
    }

    private fun matches(vararg args: Pair<Any?, Any?>): Boolean {
        called = true
        return args.all { it.first === it.second }
    }
}