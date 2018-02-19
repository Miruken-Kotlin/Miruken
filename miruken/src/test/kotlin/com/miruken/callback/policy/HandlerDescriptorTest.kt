package com.miruken.callback.policy

import com.miruken.callback.Handling
import com.miruken.callback.TestHandler
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class HandlerDescriptorTest {
    @Test fun `Cannot create descriptors for interfaces`() {
        assertFailsWith(IllegalArgumentException::class) {
            HandlerDescriptor.getDescriptor<Handling>()
        }
    }

    @Test fun `Cannot create descriptors for abstract classes`() {
        assertFailsWith(IllegalArgumentException::class) {
            HandlerDescriptor.getDescriptor<CallbackPolicy>()
        }
    }

    @Test fun `Cannot create descriptors for primitive types`() {
        assertFailsWith(IllegalArgumentException::class) {
            HandlerDescriptor.getDescriptor<Int>()
        }
    }

    @Test fun `Cannot create descriptors for collection classes`() {
        assertFailsWith(IllegalArgumentException::class) {
            HandlerDescriptor.getDescriptor<List<Foo>>()
        }
    }

    @Test fun `Obtains same descriptor per Handler class`() {
        val descriptor = HandlerDescriptor.getDescriptor<TestHandler>()
        assertSame(descriptor, HandlerDescriptor.getDescriptor<TestHandler>())
    }
}