package com.miruken.callback

import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.LazyHandlerDescriptorFactory
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.callback.policy.bindings.QualifierFactory
import com.miruken.callback.policy.getDescriptor
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HandlerBindingTest {
    private lateinit var _handler: Handling

    @Before
    fun setup() {
        _handler = TypeHandlers
        HandlerDescriptorFactory.current =
                LazyHandlerDescriptorFactory().apply {
            getDescriptor<Client>()
            getDescriptor<LocalConfiguration>()
            getDescriptor<RemoteConfiguration>()
            getDescriptor<Hospital>()
        }
    }

    @Test fun `Resolves instance without name`() {
        val configuration = _handler.resolve<Configuration>()
        assertNotNull(configuration)
    }

    @Test fun `Resolves all instances without name`() {
        val configurations = _handler.resolveAll<Configuration>()
        assertEquals(2, configurations.size)
    }

    @Test fun `Resolves named instance`() {
        val local = _handler.resolve<Configuration> { named("local") }
        assertNotNull(local)
        assertTrue(local is LocalConfiguration)
        assertEquals("http://localhost/Server", local.serverUrl)

        val remote = _handler.resolve<Configuration> { named("remote") }
        assertNotNull(remote)
        assertTrue(remote is RemoteConfiguration)
        assertEquals("http://remote/Server", remote.serverUrl)
    }

    @Test fun `Resolves all named instance`() {
        val configurations = _handler.resolveAll<Configuration> {
            named("remote")
        }
        assertEquals(1, configurations.size)
        assertTrue(configurations[0] is RemoteConfiguration)
    }

    @Test fun `Injects dependency based on name`() {
        val client = _handler.resolve<Client>()
        assertNotNull(client)
        assertEquals("http://localhost/Server", client.local.serverUrl)
        assertEquals("http://remote/Server", client.remote.serverUrl)
    }

    @Test fun `Resolves instance based on qualifier`() {
        val doctor = PersonProvider.resolve<Person> {
            require(Qualifier<Doctor>())
        }
        assertNotNull(doctor)
        assertEquals("Jack", doctor.firstName)
        assertEquals("Zigler", doctor.lastName)

        val programmer = PersonProvider.resolve<Person> {
            require(Qualifier<Programmer>())
        }
        assertNotNull(programmer)
        assertEquals("Paul", programmer.firstName)
        assertEquals("Allen", programmer.lastName)
    }

    @Test fun `Resolves all instances based on qualifier`() {
        val programmers = PersonProvider.resolveAll<Person> {
            require(Qualifier<Programmer>())
        }
        assertEquals(1, programmers.size)
        assertEquals("Paul", programmers[0].firstName)
        assertEquals("Allen", programmers[0].lastName)
    }

    @Test fun `Resolves all instances without constraints`() {
        val people = PersonProvider.resolveAll<Person>()
        assertEquals(2, people.size)
    }

    @Test fun `Injects dependency based on qualifier`() {
        val handler  = PersonProvider + _handler
        val hosptial = handler.resolve<Hospital>()
        assertNotNull(hosptial)
        assertEquals("Jack", hosptial.doctor.firstName)
        assertEquals("Zigler", hosptial.doctor.lastName)
        assertEquals("Paul", hosptial.programmer.firstName)
        assertEquals("Allen", hosptial.programmer.lastName)
    }

    interface Person {
        var firstName: String
        var lastName: String
    }

    data class PersonData(
            override var firstName: String,
            override var lastName: String
    ) : Person

    @UseFilterProviderFactory(QualifierFactory::class)
    annotation class Doctor

    @UseFilterProviderFactory(QualifierFactory::class)
    annotation class Programmer

    class Hospital @Provides constructor(
            @Doctor     val doctor: Person,
            @Programmer val programmer: Person
    )

    object PersonProvider : Handler() {
        @Provides @Singleton @Doctor
        fun getDoctor() = PersonData("Jack", "Zigler")

        @Provides @Singleton @Programmer
        fun getProgrammer() = PersonData("Paul", "Allen")
    }

    interface Configuration {
        val serverUrl: String
    }

    class LocalConfiguration
        @Provides @Singleton @Named("local")
        constructor(): Configuration {
        override val serverUrl = "http://localhost/Server"
    }

    class RemoteConfiguration
        @Provides @Singleton @Named("remote")
        constructor(): Configuration {
        override val serverUrl = "http://remote/Server"
    }

    class Client @Provides constructor(
        @Named("local")  val local: Configuration,
        @Named("remote") val remote: Configuration
    )
}