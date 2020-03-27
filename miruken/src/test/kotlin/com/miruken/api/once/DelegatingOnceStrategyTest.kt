package com.miruken.api.once

import com.miruken.api.NamedType
import com.miruken.api.send
import com.miruken.callback.Handler
import com.miruken.callback.Handles
import com.miruken.callback.plus
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.map.Maps
import com.miruken.test.assertAsync
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.*
import kotlin.test.assertEquals

class DelegatingOnceStrategyTest {
    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<FooHandler>()
        factory.registerDescriptor<OnceHandler>()
        HandlerDescriptorFactory.useFactory(factory)
    }

    @Test
    fun `handles once`() {
        val handler = FooHandler() + OnceHandler()
        assertAsync(testName) { done ->
            val foo  = Foo()
            val once = foo.once
            handler.send(once) then {
                assertEquals(once.requestId, foo.requestId)
                done()
            }
        }
    }

    private data class Foo(var requestId: UUID? = null) : NamedType {
        override val typeName: String = "Foo"
    }

    @Suppress("UNUSED_PARAMETER")
    private class FooHandler : Handler() {
        @Handles
        fun handleFoo(foo: Foo, once: Once) {
            foo.requestId = once.requestId
        }

        @Maps
        fun once(foo: Foo) = DelegatingOnceStrategy
    }
}