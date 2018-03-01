package com.miruken.callback.policy

import com.miruken.callback.Handling
import com.miruken.callback.TestHandler
import com.miruken.callback.TestProvider
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class HandlerDescriptorTest {
    @Test fun `Cannot create descriptors for interfaces`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptorFor<Handling>()
        }
    }

    @Test fun `Cannot create descriptors for abstract classes`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptorFor<CallbackPolicy>()
        }
    }

    @Test fun `Cannot create descriptors for primitive types`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptorFor<Int>()
        }
    }

    @Test fun `Cannot create descriptors for collection classes`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptorFor<List<Foo>>()
        }
    }

    @Test fun `Obtains same descriptor per Handler class`() {
        val descriptor = HandlerDescriptor.getDescriptorFor<TestHandler.Good>()
        assertSame(descriptor, HandlerDescriptor.getDescriptorFor<TestHandler.Good>())
    }

    @Test fun `Obtains descriptor with Handles method using open generics`() {
        val descriptor = HandlerDescriptor.getDescriptorFor<TestHandler.OpenGenerics>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Handles method with no parameters`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptorFor<TestHandler.NoParameters>()
        }
    }

    @Test fun `Rejects descriptor with Handles method with Nothing parameter`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptorFor<TestHandler.NothingParameter>()
        }
    }

    @Test fun `Obtains same descriptor per Provider class`() {
        val descriptor = HandlerDescriptor.getDescriptorFor<TestProvider.Good>()
        assertSame(descriptor, HandlerDescriptor.getDescriptorFor<TestProvider.Good>())
    }

    @Test fun `Obtains descriptor for Provider with properties`() {
        val descriptor = HandlerDescriptor.getDescriptorFor<TestProvider.Properties>()
        assertNotNull(descriptor)
    }

    @Test fun `Obtains descriptor for Provider with generic properties`() {
        val descriptor = HandlerDescriptor.getDescriptorFor<TestProvider.GenericProperties<TestProvider.Foo>>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptorFor<TestProvider.ReturnsNothing>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing taking callback`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptorFor<TestProvider.ReturnsNothingWithCallback>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Unit`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptorFor<TestProvider.ReturnsUnit>()
        }
    }
}