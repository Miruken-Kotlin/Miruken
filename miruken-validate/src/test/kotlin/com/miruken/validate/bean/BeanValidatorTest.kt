package com.miruken.validate.bean

import com.miruken.callback.plus
import com.miruken.validate.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import javax.validation.Validation
import kotlin.test.*

class BeanValidatorTest {
    private lateinit var _validator: javax.validation.Validator

    @Before
    fun setup() {
        _validator = Validation
                .buildDefaultValidatorFactory()
                .validator
    }

    @Test fun `Validates target`() {
        val handler = (Validator()
                    + BeanValidator(_validator))
        val player  = Player().apply {
            dob = LocalDate.of(2005, 6, 14)
        }
        val outcome = Validating(handler).validate(player)
        assertFalse(outcome.isValid)
        assertSame(outcome, player.validationOutcome)
        assertEquals("must not be empty", outcome["firstName"])
        assertEquals("must not be empty", outcome["lastName"])
        assertEquals("lastName | must not be empty\n" +
                "firstName | must not be empty", outcome.error)
    }

    @Test fun `Validates nested target`() {
        val handler = (Validator()
                    + BeanValidator(_validator))
        val team    = Team().apply {
            division = "10"
            coach    = Coach()
            players  = listOf(Player(), Player().apply {
                firstName = "Cristiano"
                lastName  = "Ronaldo"
                dob       = LocalDate.of(1985, 2, 5)
            }, Player().apply { firstName = "Lionel" })
        }
        val outcome = Validating(handler).validate(team)
        assertFalse(outcome.isValid)
        assertSame(outcome, team.validationOutcome)
        assertTrue(outcome.culprits.containsAll(listOf(
                "name", "division", "coach", "players")))
        assertEquals("must not be empty", outcome["name"])
        assertEquals("division must match U followed by age", outcome["division"])

        val coach = outcome.getOutcome("coach")!!
        assertFalse(coach.isValid)
        //assertSame(coach, team.coach!!.validationOutcome)
        assertEquals("must not be empty", coach["firstName"])
        assertEquals("must not be empty", coach["lastName"])
        assertEquals("must not be empty", coach["license"])

        val players = outcome.getOutcome("players")!!
        assertFalse(players.isValid)
        assertTrue(players.culprits.containsAll(listOf("0", "2")))
        val player1 = players.getOutcome("0")!!
        //assertSame(player1, team.players!![0].validationOutcome)
        assertEquals("must not be empty", player1["firstName"])
        assertEquals("must not be empty", player1["lastName"])
        assertEquals("must not be null", player1["dob"])
        assertNull(players.getOutcome("1"))
        val player3 = players.getOutcome("2")!!
        //assertSame(player3, team.players!![2].validationOutcome)
        assertEquals("must not be empty", player3["lastName"])
        assertEquals("must not be null", player3["dob"])
    }
}