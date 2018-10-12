package com.miruken.validate

import com.miruken.assertAsync
import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.validate.bean.BeanValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class ValidateFilterTest {
    private lateinit var _handler: Handling

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        HandlerDescriptor.resetDescriptors()
        HandlerDescriptor.getDescriptor<BeanValidator>()
        HandlerDescriptor.getDescriptor<ValidateFilter<*,*>>()
        HandlerDescriptor.getDescriptor<TeamHandler>()

        _handler = TypeHandlers
    }

    @Test
    fun `Validates request`() {
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

                done()
            }
        }
    }

    interface TeamAction {
        val team: Team
    }

    data class CreateTeam(
        override val team: Team
    ) : TeamAction

    data class TeamCreated(
            override val team: Team
    ) : TeamAction

    data class RemoveTeam(
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