package com.miruken.validate.bean

import com.miruken.callback.plus
import com.miruken.validate.*
import org.junit.After
import org.junit.Test
import java.time.LocalDate
import javax.validation.Validation
import javax.validation.ValidatorFactory
import kotlin.test.*

class BeanValidatorTest {
    private lateinit var _validatorFactory: ValidatorFactory

    @BeforeTest
    fun setup() {
        _validatorFactory = Validation
                .buildDefaultValidatorFactory()
    }

    @After
    fun cleanup() {
        _validatorFactory.close()
    }

    @Test fun `Validates target`() {
        val handler = BeanValidator(_validatorFactory)
        val player  = Player().apply {
            dob = LocalDate.of(2005, 6, 14)
        }
        val outcome = handler.validate(player)
        assertFalse(outcome.isValid)
        assertSame(outcome, player.validationOutcome)
        assertEquals("must not be empty", outcome["firstName"])
        assertEquals("must not be empty", outcome["lastName"])
    }

    @Test fun `Validates target graph`() {
        val handler = BeanValidator(_validatorFactory)
        val team    = Team().apply {
            division = "10"
            coach    = Coach()
            players  = listOf(Player(), Player().apply {
                firstName = "Cristiano"
                lastName  = "Ronaldo"
                dob       = LocalDate.of(1985, 2, 5)
            }, Player().apply { firstName = "Lionel" })
        }
        val outcome = handler.validate(team)
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