package com.miruken.concurrent

import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TestName
import java.util.concurrent.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class PromiseCoroutineTest {
    @Rule
    @JvmField val testName = TestName()

    @Test fun `Awaits resolved promise`() = runBlocking {
        assertEquals("Hello", Promise.resolve("Hello").await())
    }

    @Test fun `Awaits rejected promise`() = runBlocking {
        try {
            Promise.reject(Exception("Error")).await()
        } catch (t: Throwable) {
            assertEquals("Error", t.message)
        }
    }

    @Test fun `Awaits cancellable promise`() = runBlocking {
        val promise = Promise<String> { _, _, _ -> }
        promise.cancel()
        try {
            promise.await()
            fail("Expected exception")
        } catch (t: Throwable) {
            assertTrue(t is CancellationException)
        }
    }

    @Test fun `Resolves promise`() = runBlocking {
        val promise = Promise<String> { resolve, _ ->
            Promise.delay(20) then {
                resolve("Hello")
            }
        }
        assertEquals("Hello", promise.await())
    }

    @Test fun `Rejects promise`() = runBlocking {
        val promise = Promise<String> { _, reject ->
            Promise.delay(20) then {
                reject(Exception("Rejected"))
            }
        }
        try {
            promise.await()
            fail("Expected an exception")
        } catch (t: Throwable) {
            assertEquals("Rejected", t.message)
        }
    }

    @Test fun `Cancels promise`() = runBlocking {
        val promise = Promise<String> { _, _ -> }
        Promise.delay(20) then {
            promise.cancel()
        }
        try {
            promise.await()
            fail("Expected an exception")
        } catch (t: Throwable) {
            assertTrue(t is CancellationException)
        }
    }
}

