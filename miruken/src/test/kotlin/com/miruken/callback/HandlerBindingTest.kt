package com.miruken.callback

import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.callback.policy.bindings.QualifierFactory
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HandlerBindingTest {
    private lateinit var _handler: Handling

    @Before
    fun setup() {
        _handler = TypeHandlers
        HandlerDescriptor.getDescriptor<Hospital>()
    }

    @Test fun `Resolves instance based on qualifier`() {
        val doctor = PersonProvider.resolve<Person> {
            require(Qualifier<Doctor>())
        }
        assertNotNull(doctor)
        assertEquals("Jack", doctor!!.firstName)
        assertEquals("Zigler", doctor.lastName)

        val programmer = PersonProvider.resolve<Person> {
            require(Qualifier<Programmer>())
        }
        assertNotNull(programmer)
        assertEquals("Paul", programmer!!.firstName)
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

    @Test fun `Injects dependency based on qualifier`() {
        val handler  = PersonProvider + _handler
        val hosptial = handler.resolve<Hospital>()
        assertNotNull(hosptial)
        assertEquals("Jack", hosptial!!.doctor.firstName)
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
}