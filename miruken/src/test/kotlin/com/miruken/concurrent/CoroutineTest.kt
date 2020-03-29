@file:Suppress("UNUSED_PARAMETER")

package com.miruken.concurrent

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.test.assertAsync
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.concurrent.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

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
    fun `Handles suspending callbacks no return await`() = runBlocking<Unit> {
        val handler = SuspendingHandler()
        handler.commandAsync(Dial("8675309")).await()
    }

    @Test
    fun `Handles suspending callbacks no return suspend`() = runBlocking<Unit> {
        val handler = SuspendingHandler()
        handler.commandCo(Dial("0"))
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
    fun `Handles suspending callbacks with return await`() = runBlocking {
        val handler      = SuspendingHandler()
        val conversation = handler.commandAsync(Talk("8675309")).await() as Conversation
        assertEquals("Hello 8675309", conversation.words)
    }

    @Test
    fun `Handles suspending callbacks with return suspend`() = runBlocking {
        val handler      = SuspendingHandler()
        val conversation = handler.commandCo(Talk("8675309")) as Conversation
        assertEquals("Hello 8675309", conversation.words)
    }

    @Test
    fun `Handles suspending callbacks with exception`() {
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
    fun `Handles suspending callbacks with exception suspend`() = runBlocking {
        val handler = SuspendingHandler()
        try {
            handler.commandCo(Talk("0"))
            fail("Should throw IllegalStateException")
        } catch (t: IllegalStateException) {
            assertEquals("All circuits are busy", t.message)
        }
    }

    @Test
    fun `Handles suspending callbacks with timeout cancel`() {
        val handler = SuspendingHandler()
        assertAsync(testName) { done ->
            handler.commandAsync(Talk("999")) cancelled  {
                 done()
            }
        }
    }

    @Test
    fun `Handles suspending callbacks with timeout suspend`() = runBlocking<Unit> {
        val handler = SuspendingHandler()
        assertFailsWith<CancellationException> {
            handler.commandCo(Talk("999"))
        }
    }

    @Test
    fun `Handles suspending callbacks with async coroutine`() = runBlocking {
        val handler      = SuspendingHandler()
        val conversation = handler.commandCo(Talk("+01234")) as Conversation
        assertEquals("Found long distance channel 29", conversation.words)
    }

    @Test
    fun `Handles suspending callbacks with async coroutine exception`() = runBlocking {
        val handler = SuspendingHandler()
        try {
            handler.commandCo(Talk("+0198713"))
            fail("Should throw IllegalStateException")
        } catch (t: IllegalStateException) {
            assertEquals("Long distance number +0198713 is not reachable", t.message)
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

    @Test
    fun `Provides suspending callbacks await`() = runBlocking {
        val handler      = SuspendingHandler()
        val conversation = handler.resolveAsync<Conversation>().await()
        assertEquals("How was your day?", conversation?.words)
    }

    @Test
    fun `Provides suspending callbacks suspend`() = runBlocking {
        val handler      = SuspendingHandler()
        val conversation = handler.resolveCo<Conversation>()
        assertEquals("How was your day?", conversation?.words)
    }

    data class Dial(val number: String)

    data class Talk(val number: String)

    data class Conversation(val words: String)

    class SuspendingHandler : Handler() {
        @Handles
        suspend fun dial(dial: Dial) = coroutineScope {
            if (dial.number == "0") {
                val job = launch {
                    repeat(1000) { i ->
                        println("I'm connecting $i ...")
                        delay(10)
                    }
                }
                delay(50)
                println("Unable to connect to ${dial.number}")
                job.cancel()
                job.join()
            } else {
                delay(10)
                println("Connected to ${dial.number}")
            }
        }

        @Handles
        suspend fun talk(talk: Talk): Conversation {
            delay(10)
            when {
                talk.number == "0" -> {
                    error("All circuits are busy")
                }
                talk.number == "999" -> {
                    withTimeout(50) {
                        repeat(1000) { i ->
                            println("I'm trying $i ...")
                            delay(10)
                        }
                    }
                }
                talk.number.startsWith("+01") -> {
                    val channel = withContext(Dispatchers.Default) {
                        connectLongDistance(talk.number)
                    }
                    return Conversation("Found long distance channel $channel")
                }
            }
            return Conversation("Hello ${talk.number}")
        }

        @Provides
        suspend fun lastConversation(): Conversation {
            delay(10)
            return Conversation("How was your day?")
        }

        private suspend fun connectLongDistance(number: String): Int {
            delay(10)
            check(!number.endsWith("13")) {
                "Long distance number $number is not reachable"
            }
            return 29
        }
    }
}