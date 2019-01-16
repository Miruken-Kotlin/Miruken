package com.miruken.validate

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.LazyHandlerDescriptorFactory
import com.miruken.callback.policy.getDescriptor
import com.miruken.concurrent.Promise
import com.miruken.test.assertAsync
import com.miruken.validate.bean.BeanValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import javax.validation.Valid
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidateFilterTest {
    private lateinit var _handler: Handling
    private lateinit var factory: HandlerDescriptorFactory

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        factory = LazyHandlerDescriptorFactory().apply {
            getDescriptor<BeanValidator>()
            getDescriptor<ValidateFilter<*,*>>()
            getDescriptor<TeamHandler>()
            HandlerDescriptorFactory.useFactory(this)
        }

        _handler = TypeHandlers
    }

    @Test fun `Validates request`() {
        assertAsync(testName) { done ->
            _handler.infer.commandAsync(CreateTeam(Team().apply {
                name  = "Liverpool"
                coach = Coach().apply {
                    firstName = "Zinedine"
                    lastName  = "Zidane"
                    license   = "A"
                }
            })) then {
                val team = it as Team
                assertEquals(team.validationOutcome?.isValid, true)
                assertEquals(1, team.id)
                assertEquals(team.active, true)
                done()
            }
        }
    }

    @Test fun `Rejects invalid request`() {
        assertAsync(testName) { done ->
            _handler.infer.commandAsync(CreateTeam(Team())) then {
                val team = it as Team
                assertEquals(team.validationOutcome?.isValid, true)
                assertEquals(1, team.id)
                assertEquals(team.active, true)
                done()
            } catch { exception ->
                (exception as? ValidationException)?.also {
                    val outcome = it.outcome
                    assertTrue(outcome.culprits.containsAll(listOf("name", "coach")))
                    assertEquals("must not be empty", outcome["name"])
                    assertEquals("must not be null", outcome["coach"])
                    done()
                }
            }
        }
    }

    interface TeamAction {
        val team: Team
    }

    data class CreateTeam(
            @Valid
            override val team: Team
    ) : TeamAction

    data class TeamCreated(
            override val team: Team
    ) : TeamAction

    data class RemoveTeam(
            @Valid
            override val team: Team
    ) : TeamAction

    data class TeamRemoved(
            override val team: Team
    ) : TeamAction

    class TeamHandler
        @Provides @Singleton constructor() : Handler() {

        private var _teamId = 0

        @Handles
        @Validate
        fun createTeam(
                create:   CreateTeam,
                composer: Handling
        ) = create.team.let { team ->
                team.id     = ++_teamId
                team.active = true
                composer.commandAllAsync(TeamCreated(team))
                Promise.resolve(team)
            }

        @Handles
        fun removeTeam(remove: RemoveTeam, composer: Handling) {
            remove.team.also { team ->
                team.active = false
                composer.commandAllAsync(TeamRemoved(team))
            }
        }
    }
}