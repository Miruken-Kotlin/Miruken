package com.miruken.validate

import com.miruken.callback.GenericWrapper
import com.miruken.callback.Handler
import com.miruken.callback.plus
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.concurrent.delay
import com.miruken.protocol.proxy
import com.miruken.test.assertAsync
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.time.LocalDate
import java.time.Period
import javax.validation.groups.Default
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ValidatorTest {
    private lateinit var factory: HandlerDescriptorFactory

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        factory = MutableHandlerDescriptorFactory().apply {
            registerDescriptor<ValidateTeam>()
            registerDescriptor<ValidatePlayer>()
            registerDescriptor<GenericWrapper>()
            HandlerDescriptorFactory.useFactory(this)
        }
    }

    @Test fun `Validates target`() {
        val handler = ValidatePlayer()
        val player  = Player().apply {
            dob = LocalDate.of(2005, 6, 14)
        }
        val outcome = handler.validate(player)
        assertFalse(outcome.isValid)
        assertSame(outcome, player.validationOutcome)
        assertEquals("first name is required", outcome["firstName"])
        assertEquals("last name is required", outcome["lastName"])
    }

    @Test fun `Validates target for scope`() {
        val handler = ValidatePlayer()
        val player  = Player().apply {
            dob = LocalDate.of(2005, 6, 14)
        }
        val outcome = handler.validate(
                player, Default::class, Recreational::class)
        assertFalse(outcome.isValid)
        assertSame(outcome, player.validationOutcome)
        assertEquals("player must be 10 or younger", outcome["dob"])
    }

    @Test fun `Validates target async`() {
        val handler = ValidateTeam()
        val team    = Team()
        assertAsync(testName) { done ->
            handler.validateAsync(team) then { outcome ->
                assertFalse(outcome.isValid)
                assertSame(outcome, team.validationOutcome)
                assertEquals("name is required", outcome["name"])
                done()
            }
        }
    }

    @Test fun `Validates target async for scope`() {
        val handler = ValidateTeam()
        val team    = Team().apply { coach = Coach() }
        assertAsync(testName) { done ->
            handler.validateAsync(team, Default::class, Ecnl::class)
                    .then { outcome ->
                        assertFalse(outcome.isValid)
                        assertSame(outcome, team.validationOutcome)
                        val coach = outcome.getOutcome("coach")
                        assertFalse(coach!!.isValid)
                        assertEquals("licensed coach is required", coach["license"])
                        done()
                    }
        }
    }

    @Test fun `Validates before method`() {
        val handler = (TeamManager()
                    + ValidatePlayer())
        val team    = Team()
        val player  = Player().apply {
            firstName = "Wayne"
            lastName  = "Rooney"
            dob       = LocalDate.of(1985,10,24)
        }
        assertAsync(testName) { done ->
            handler.valid(player).proxy<TeamManagement>()
                    .addPlayer(player, team) then {
                assertTrue(team.players!!.contains(player))
                done()
            }
        }
    }

    @Test fun `Rejects method if invalid`() {
        val handler = (TeamManager()
                    + ValidatePlayer())
        val team    = Team()
        val player  = Player()
        assertAsync(testName) { done ->
            handler.valid(player).proxy<TeamManagement>()
                    .addPlayer(player, team) cancelled {
                done()
            }
        }
    }

    @Test fun `Rejects method if invalid async`() {
        val handler = (TeamManager()
                    + ValidatePlayer())
        val team    = Team()
        val player  = Player()
        assertAsync(testName) { done ->
            handler.validAsync(player).proxy<TeamManagement>()
                    .addPlayer(player, team) cancelled {
                done()
            }
        }
    }

    interface Ecnl
    interface Recreational

    interface TeamManagement {
        fun addPlayer(player: Player, team: Team): Promise<Team>
    }

    class TeamManager : Handler(), TeamManagement {
        override fun addPlayer(
                player: Player,
                team:   Team
        ): Promise<Team> {
            team.players = (team.players ?: emptyList()) + listOf(player)
            return Promise.resolve(team)
        }
    }

    class ValidateTeam : Handler() {
        @Validates
        fun shouldHaveName(
                team:    Team,
                outcome: ValidationResult.Outcome
        ) = Promise.delay(10) then {
            if (team.name.isNullOrEmpty()) {
                outcome.addError("name", "name is required")
            }
        }

        @Validates(Ecnl::class)
        fun shouldHaveLicensesCoach(
                 team:    Team,
                 outcome: ValidationResult.Outcome
         ) = Promise.delay(10) then {
            team.coach?.also { coach ->
                if (coach.license.isNullOrEmpty()) {
                    outcome.addError("coach.license", "licensed coach is required")
                }
            } ?: outcome.addError("coach", "coach is required")
         }
    }

    class ValidatePlayer : Handler() {
        @Validates
        fun shouldHaveFullName(
                player:  Player,
                outcome: ValidationResult.Outcome
        ) {
            if (player.firstName.isNullOrEmpty()) {
                outcome.addError("firstName", "first name is required")
            }

            if (player.lastName.isNullOrEmpty()) {
                outcome.addError("lastName", "last name is required")
            }

            if (player.dob == null) {
                outcome.addError("dob", "DOB is required")
            }
        }

        @Validates(Recreational::class)
        fun mustBeTenOrUnder(
                validation: Validation
        ) {
            val outcome = validation.outcome
            val player  = validation.target as Player
            player.dob?.also {
                val age = Period.between(it, LocalDate.now())
                if (age.years > 10) {
                    outcome.addError("dob", "player must be 10 or younger")
                }
            }
        }
    }
}