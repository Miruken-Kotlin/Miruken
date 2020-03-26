@file:Suppress("UNUSED_PARAMETER")

package com.miruken.concurrent

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.test.assertAsync
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.lang.IllegalStateException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoroutineTest {
    private lateinit var factory: HandlerDescriptorFactory

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        HandlerDescriptorFactory.useFactory(
                MutableHandlerDescriptorFactory().apply {
                    registerDescriptor<SuspendingHandler>()
                    factory = this
                })
    }

    @Test
    fun `Handles suspending callbacks no return`() {
        val handler = SuspendingHandler()
        assertEquals(HandleResult.HANDLED, handler.handle(Dial("8675309")))
    }

    @Test
    fun `Handles suspending callbacks no return async`() {
        val handler = SuspendingHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(Dial("8675309")) then {
                done()
            }
        }
    }

    @Test
    fun `Handles suspending callbacks with return`() {
        val handler     = SuspendingHandler()
        val converation = handler.command(Talk("8675309")) as Conversation
        assertEquals("Hello 8675309", converation.words)
    }

    @Test
    fun `Handles suspending callbacks with return async`() {
        val handler = SuspendingHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(Talk("8675309")) then {
                val conversation = it as Conversation
                assertEquals("Hello 8675309", conversation.words)
                done()
            }
        }
    }

    @Test
    fun `Handles suspending callbacks with return exception`() {
        val handler = SuspendingHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(Talk("0")) catch {
                assertTrue(it is IllegalStateException)
                assertEquals("All circuits are busy", it.message)
                done()
            }
        }
    }

    @Test
    fun `Provides suspending callbacks`() {
        val handler = SuspendingHandler()
        assertEquals("How was your day?", handler.resolve<Conversation>()?.words)
    }

    @Test
    fun `Provides suspending callbacks async`() {
        val handler = SuspendingHandler()
        assertAsync(testName) { done ->
            handler.resolveAsync<Conversation>() then {
                assertEquals("How was your day?", it?.words)
                done()
            }
        }
    }

    data class Dial(val number: String)

    data class Talk(val number: String)

    data class Conversation(val words: String)

    class SuspendingHandler : Handler() {
        @Handles
        suspend fun dial(dial: Dial) {
            delay(10)
        }

        @Handles
        suspend fun talk(talk: Talk): Conversation {
            delay(10)
            if (talk.number == "0")
                error("All circuits are busy")
            return Conversation("Hello ${talk.number}")
        }

        @Provides
        suspend fun lastConversation(): Conversation {
            delay(10)
            return Conversation("How was your day?")
        }
    }
}