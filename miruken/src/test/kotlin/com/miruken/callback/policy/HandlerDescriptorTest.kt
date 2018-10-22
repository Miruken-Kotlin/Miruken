package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import io.github.classgraph.ClassGraph
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class HandlerDescriptorTest {
    @Test fun `Cannot create descriptors for interfaces`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptor<Handling>()
        }
    }

    @Test fun `Cannot create descriptors for abstract classes`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptor<CallbackPolicy>()
        }
    }

    @Test fun `Cannot create descriptors for primitive types`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptor<Int>()
        }
    }

    @Test fun `Cannot create descriptors for collection classes`() {
        assertFailsWith(IllegalStateException::class) {
            HandlerDescriptor.getDescriptor<List<Foo>>()
        }
    }

    @Test fun `Obtains same descriptor per Handler class`() {
        val descriptor = HandlerDescriptor.getDescriptor<TestHandler.Good>()
        assertEquals(TestHandler.Good::class, descriptor.handlerClass)
        assertSame(descriptor, HandlerDescriptor.getDescriptor<TestHandler.Good>())
    }

    @Test fun `Obtains descriptor with Handles method using open generics`() {
        val descriptor = HandlerDescriptor.getDescriptor<TestHandler.OpenGenerics>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Handles method with no parameters`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptor<TestHandler.NoParameters>()
        }
    }

    @Test fun `Rejects descriptor with Handles method with Nothing parameter`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptor<TestHandler.NothingParameter>()
        }
    }

    @Test fun `Obtains same descriptor per Provider class`() {
        val descriptor = HandlerDescriptor.getDescriptor<TestProvider.Good>()
        assertSame(descriptor, HandlerDescriptor.getDescriptor<TestProvider.Good>())
    }

    @Test fun `Obtains descriptor for Provider with properties`() {
        val descriptor = HandlerDescriptor.getDescriptor<TestProvider.Properties>()
        assertNotNull(descriptor)
    }

    @Test fun `Obtains descriptor for Provider with generic properties`() {
        val descriptor = HandlerDescriptor.getDescriptor<TestProvider.GenericProperties<TestProvider.Foo>>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptor<TestProvider.ReturnsNothing>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing taking callback`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptor<TestProvider.ReturnsNothingWithCallback>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Unit`() {
        assertFailsWith(PolicyRejectedException::class) {
            HandlerDescriptor.getDescriptor<TestProvider.ReturnsUnit>()
        }
    }

    @Test fun `Visits all descriptor member bindings`() {
        val bindings = mutableListOf<MemberBinding>()
        HandlerDescriptor.getDescriptor<VisitHandler> { descriptor, binding ->
            assertSame(VisitHandler::class, descriptor.handlerClass)
            bindings.add(binding)
        }
        assertEquals(3, bindings.size)
    }

    @Test fun `Scans classpath`() {
        val handling = Handling::class.java.name
        ClassGraph()
                .enableClassInfo()
                .enableExternalClasses()
                .whitelistPackages(
                        VisitHandler::class.java.`package`.name,
                        Handling::class.java.`package`.name
                )
                .blacklistClasses(
                        Batch::class.java.name
                )
                .scan().use { scan ->
                    val handlers = scan.getClassesImplementing(handling)
                    for (handler in handlers) {

                    }
                }
    }

    @Suppress("UNUSED_PARAMETER")
    class VisitHandler @Provides constructor() : Handler() {
        @Handles
        fun hello(foo: Foo) {}

        @Provides
        val foo = Foo()
    }
}