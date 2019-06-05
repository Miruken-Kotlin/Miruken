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

class MutableHandlerDescriptorFactoryTest {
    private lateinit var factory: HandlerDescriptorFactory

    @Before
    fun setup() {
        factory = MutableHandlerDescriptorFactory()
        HandlerDescriptorFactory.useFactory(factory)
    }

    @Test fun `Cannot create descriptors for interfaces`() {
        assertFailsWith(IllegalStateException::class) {
            factory.registerDescriptor<Handling>()
        }
    }

    @Test fun `Cannot create descriptors for primitive types`() {
        assertFailsWith(IllegalStateException::class) {
            factory.registerDescriptor<Int>()
        }
    }

    @Test fun `Cannot create descriptors for collection classes`() {
        assertFailsWith(IllegalStateException::class) {
            factory.registerDescriptor<List<Foo>>()
        }
    }

    @Test fun `Obtains same descriptor per Handler class`() {
        val descriptor = factory.registerDescriptor<TestHandler.Good>()
        assertNotNull(descriptor)
        assertEquals(TestHandler.Good::class, descriptor.handlerClass)
        assertSame(descriptor, factory.registerDescriptor<TestHandler.Good>())
    }

    @Test fun `Obtains descriptor with Handles method using open generics`() {
        val descriptor = factory.registerDescriptor<TestHandler.OpenGenerics>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Handles method with no parameters`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.registerDescriptor<TestHandler.NoParameters>()
        }
    }

    @Test fun `Rejects descriptor with Handles method with Nothing parameter`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.registerDescriptor<TestHandler.NothingParameter>()
        }
    }

    @Test fun `Obtains same descriptor per Provider class`() {
        val descriptor = factory.registerDescriptor<TestProvider.Good>()
        assertSame(descriptor, factory.registerDescriptor<TestProvider.Good>())
    }

    @Test fun `Obtains descriptor for Provider with properties`() {
        val descriptor = factory.registerDescriptor<TestProvider.Properties>()
        assertNotNull(descriptor)
    }

    @Test fun `Obtains descriptor for Provider with generic properties`() {
        val descriptor = factory.registerDescriptor<TestProvider.GenericProperties<TestProvider.Foo>>()
        assertNotNull(descriptor)
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.registerDescriptor<TestProvider.ReturnsNothing>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Nothing taking callback`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.registerDescriptor<TestProvider.ReturnsNothingWithCallback>()
        }
    }

    @Test fun `Rejects descriptor with Provides method returning Unit`() {
        assertFailsWith(PolicyRejectedException::class) {
            factory.registerDescriptor<TestProvider.ReturnsUnit>()
        }
    }

    @Test fun `Visits all descriptor member bindings`() {
        val bindings = mutableListOf<MemberBinding>()
        val factory  = MutableHandlerDescriptorFactory { descriptor, binding ->
            assertSame(ExampleHandler::class, descriptor.handlerClass)
            bindings.add(binding)
        }
        factory.registerDescriptor<ExampleHandler>()
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

        @get:Provides
        val foo = Foo()
    }
}