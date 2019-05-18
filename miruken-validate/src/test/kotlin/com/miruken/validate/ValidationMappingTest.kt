package com.miruken.validate

import com.miruken.callback.Handling
import com.miruken.map.map
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ValidationMappingTest {
    @Rule
    @JvmField val testName = TestName()

    private lateinit var _mapping: Handling

    @Before
    fun setup() {
        _mapping = ValidationMapping()
    }

    @Test
    fun `Maps validation errors to a ValidationException`() {
        val mapping = arrayOf(
                ValidationErrors("name",
                        errors = arrayOf("name cannot be empty")),
                ValidationErrors("certification",
                        errors = arrayOf("certification expired"),
                        nested = arrayOf(ValidationErrors("club",
                                errors = arrayOf("club not specified"))))
                ).let(::ValidationErrorMapping)

        val exception = _mapping.map<Throwable>(
                mapping, format = Throwable::class
        ) as? ValidationException

        assertNotNull(exception)

        val outcome = exception.outcome
        assertFalse(outcome.isValid)
        assertEquals("name cannot be empty", outcome["name"])
        assertEquals("certification expired",
                outcome.getResults("certification")
                .filterIsInstance<ValidationResult.Error>()
                .first().error)

        val certification = outcome.getOutcome("certification")
        assertNotNull(certification)
        assertEquals("club not specified", certification["club"])
    }
}