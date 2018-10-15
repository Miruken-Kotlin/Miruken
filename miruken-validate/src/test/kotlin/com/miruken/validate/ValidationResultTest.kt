package com.miruken.validate

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationResultTest {
    @Test fun `Adds simple error`() {
        val outcome = ValidationResult.Outcome()
        outcome.addError("name", "name can't be empty")
        assertEquals("name can't be empty", outcome["name"])
        assertEquals("name | name can't be empty", outcome.error)
        assertTrue(outcome.culprits.contains("name"))
        assertTrue(outcome.getResults("name")
                .filterIsInstance<ValidationResult.Error>()
                .single().error == "name can't be empty")
    }

    @Test fun `Adds simple named error with`() {
        val outcome = ValidationResult.Outcome()
        outcome.addResult("name", ValidationResult.Error("name can't be empty", "notEmpty"))
        assertEquals("name can't be empty", outcome["name"])
        assertEquals("name | name can't be empty", outcome.error)
        assertTrue(outcome.getCulprits("notEmpty").contains("name"))
    }

    @Test fun `Adds nested error`() {
        val outcome = ValidationResult.Outcome()
        outcome.addError("company.name", "name can't be empty")
        assertEquals("name | name can't be empty", outcome["company"])
        assertEquals("company | name | name can't be empty", outcome.error)
        assertTrue(outcome.culprits.contains("company"))
        val company = outcome.getOutcome("company")
        assertFalse(company!!.isValid)
        assertEquals("name can't be empty", company["name"])
        assertTrue(company.culprits.contains("name"))
        assertTrue(outcome.getResults("company")
                .filterIsInstance<ValidationResult.Outcome>()
                .single() === company)
    }

    @Test fun `Adds indexed error`() {
        val outcome = ValidationResult.Outcome()
        outcome.addError("company.addresses[0]", "city can't be empty")
        assertEquals("addresses | 0 | city can't be empty", outcome["company"])
        assertEquals("company | addresses | 0 | city can't be empty", outcome.error)
        assertTrue(outcome.culprits.contains("company"))
        val company = outcome.getOutcome("company")
        assertFalse(company!!.isValid)
        assertEquals("addresses | 0 | city can't be empty", company.error)
        assertEquals("0 | city can't be empty", company["addresses"])
        assertTrue(company.culprits.contains("addresses"))
        val addresses = company.getOutcome("addresses")
        assertFalse(addresses!!.isValid)
        assertEquals("0 | city can't be empty", addresses.error)
        assertEquals("city can't be empty", addresses["[0]"])
        assertEquals("city can't be empty", addresses["0"])
        assertTrue(addresses.culprits.contains("0"))
    }

    @Test fun `Collects validation errors`() {
        val outcome = ValidationResult.Outcome()
        outcome.addError("email", "email is not vaild")
        outcome.addError("email", "email must be at least 5 characters")
        assertEquals("email is not vaild; email must be at least 5 characters", outcome["email"])
        assertEquals("email | email is not vaild; email must be at least 5 characters", outcome.error)
        val results = outcome.getResults("email")
        assertEquals(2, results.size)
    }

    @Test fun `Merges validation outcomes`() {
        val outcome1 = ValidationResult.Outcome()
        outcome1.addError("email", "email is not vaild")
        val outcome2 = ValidationResult.Outcome()
        outcome1.addError("email", "email must be at least 5 characters")
        outcome1.merge(outcome2)
        assertEquals("email is not vaild; email must be at least 5 characters", outcome1["email"])
    }

    @Test fun `Rejects empty property names`() {
        val outcome = ValidationResult.Outcome()
        assertFailsWith(IllegalArgumentException::class) {
            outcome.addError("", "invalid")
        }
    }

    @Test fun `Rejects period property names`() {
        val outcome = ValidationResult.Outcome()
        assertFailsWith(IllegalArgumentException::class) {
            outcome.addError(".", "invalid")
        }
    }
}