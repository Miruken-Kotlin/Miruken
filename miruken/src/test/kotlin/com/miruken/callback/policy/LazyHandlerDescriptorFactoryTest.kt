package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import io.github.classgraph.ClassGraph
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class LazyHandlerDescriptorFactoryTest {
    private lateinit var factory: HandlerDescriptorFactory

    @Before
    fun setup() {
        factory = LazyHandlerDescriptorFactory()
        HandlerDescriptorFactory.current = factory
    }

    @Test fun `Cannot create descriptors for interfaces`() {
        assertFailsWith(IllegalStateException::class) {
            factory.getDescriptor<Handling>()
        }
    }

    @Test fun `Cannot create descriptors for primitive types`() {
        assertFailsWith(IllegalStateException::class) {
            factory.getDescriptor<Int>()
        }
    }

    @Test fun `Cannot create descriptors for collection classes`() {
        assertFailsWith(IllegalStateException::class) {
            factory.getDescriptor<List<Foo>>()
        }
    }

    @Test fun `Obtains same descriptor per Handler class`() {
        val descriptor = factory.getDescriptor<TestHandler.Good>()
        assertNotNull(descriptor)
        assertEquals(TestHandler.Good::class, descriptor.handlerClass)
        assertSame(descriptor, factory.getDescriptor<TestHandler.Good>())
    }

    @Test fun `Obtains descriptor with Handles method using open generics`() {
        val descriptor = factory.getDescriptor<TestHandler.OpenGenerics>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Handles method with no parameters`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.getDescriptor<TestHandler.NoParameters>()
        }
    }

    @Test fun `Rejects descriptor with Handles method with Nothing parameter`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.getDescriptor<TestHandler.NothingParameter>()
        }
    }

    @Test fun `Obtains same descriptor per Provider class`() {
        val descriptor = factory.getDescriptor<TestProvider.Good>()
        assertSame(descriptor, factory.getDescriptor<TestProvider.Good>())
    }

    @Test fun `Obtains descriptor for Provider with properties`() {
        val descriptor = factory.getDescriptor<TestProvider.Properties>()
        assertNotNull(descriptor)
    }

    @Test fun `Obtains descriptor for Provider with generic properties`() {
        val descriptor = factory.getDescriptor<TestProvider.GenericProperties<TestProvider.Foo>>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.getDescriptor<TestProvider.ReturnsNothing>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing taking callback`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.getDescriptor<TestProvider.ReturnsNothingWithCallback>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Unit`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.getDescriptor<TestProvider.ReturnsUnit>()
        }
    }

    @Test fun `Visits all descriptor member bindings`() {
        val bindings = mutableListOf<MemberBinding>()
        val factory  = LazyHandlerDescriptorFactory { descriptor, binding ->
            assertSame(ExampleHandler::class, descriptor.handlerClass)
            bindings.add(binding)
        }
        factory.getDescriptor<ExampleHandler>()
        assertEquals(3, bindings.size)
    }

    @Test fun `Scans classpath`() {
        val handling = Handling::class.java.name
        ClassGraph()
                .enableAllInfo()
                .whitelistPackages(
                        ExampleHandler::class.java.`package`.name,
                        Handling::class.java.`package`.name
                )
                .scan().use { scan ->
                    val handlers = scan.getClassesImplementing(handling)
                    for (handler in handlers) {

                    }
                }
    }

    @Suppress("UNUSED_PARAMETER")
    class ExampleHandler @Provides constructor() : Handler() {
        @Handles
        fun hello(foo: Foo) {}

        @Provides
        val foo = Foo()
    }
}