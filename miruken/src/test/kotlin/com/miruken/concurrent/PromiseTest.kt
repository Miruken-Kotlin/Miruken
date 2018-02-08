package com.miruken.concurrent

import com.miruken.*
import java.util.concurrent.CancellationException
import kotlin.test.*
import org.junit.Test as test

class PromiseTest {
    @test fun `Starts in pending state`() {
        val promise = Promise<String> { _, _ -> }
        assertTrue { promise.state === PromiseState.Pending }
    }

    @test fun `Creates resolved promise`() {
        val promise =  Promise.resolve("Hello")
        assertTrue { promise.state === PromiseState.Fulfilled }
    }

    @test fun `Creates rejected promise`() {
        val promise = Promise.reject(Exception("Error"))
        assertTrue { promise.state === PromiseState.Rejected }
    }

    @test fun `Resolves promise`() {
        val promise = Promise<String> { resolve, _ ->
            resolve("Hello")
        }
        assertTrue { promise.state === PromiseState.Fulfilled }
    }

    @test fun `Rejects promise`() {
        val promise = Promise<String> { _, reject ->
            reject(Exception("Rejected"))
        }
        assertTrue { promise.state === PromiseState.Rejected }
    }

    @test fun `Adopts resolved promise`() {
        val promise = Promise.resolve("Hello")
        assertTrue { promise.state === PromiseState.Fulfilled }
    }

    @test fun `Adopts rejected promise`() {
        val promise = Promise.resolve(
                Promise.reject(Exception("Rejected")))
        assertTrue { promise.state === PromiseState.Rejected }
    }

    @test fun `Fulfills promise only once`() {
        var called = 0
        Promise<String> { resolve, _ ->
            resolve("Hello")
            resolve("Goodbye")
        }.then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Rejects promise only once`() {
        var called = 0
        Promise<String> { _, reject ->
            reject(Exception("Rejected"))
            reject(Exception("Uh Oh!"))
        }.catch {
            assertEquals("Rejected", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Fulfills promise with projection`() {
        var called = 0
        Promise.resolve(22).then {
            it.toString()
        }.then { num: String ->
            assertEquals("22", num)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Rejects promise with projection`() {
        var called = 0
        Promise<Int> { _, reject ->
            reject(Exception("Rejected"))
        }.catch {
            assertEquals("Rejected", it.message)
            ++called
            19
        }.then {
            it.fold({}, {
              assertEquals(19, it)
            })
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Handles fulfilled promise with fail projection`() {
        var called = 0
        Promise<Int> { resolve, _ ->
            resolve(22)
        }.then(
            { it.toString() },
            { fail("Should skip") }
        ).then {
            it.fold({}, { assertEquals("22", it) })
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Handles rejected promise with success projection`() {
        var called = 0
        Promise<Int> { _, reject ->
            reject(Exception("Halt and catch fire"))
        }.then(
            { fail("Should skip") },
            {
                assertEquals("Halt and catch fire", it.message)
                ++called
            }
        ).then {
            it.fold({ assertEquals(1, it) }, {})
        }
        assertEquals(1, called)
    }

    @test fun `Propagates fulfilled promise`() {
        var called = 0
        Promise.resolve("Hello")
         .catch { }
         .then {
             it.fold({}, {
                 assertEquals("Hello", it)
             })
             ++called
             "Goodbye"
        }.then {
            assertEquals("Goodbye", it)
        }
        assertEquals(1, called)
    }

    @test fun `Propagates rejected promise`() {
        var called  = 0
        var verify  = false
        val promise = Promise.resolve("Hello")
        promise.then {
            assertEquals("Hello", it)
            ++called
        }
        promise.then {
            throw Exception("Bad")
        }.then {
            fail("Should skip")
        }.catch {
            assertEquals("Bad", it.message)
            ++called
        }.then {
            verify = true
        }
        assertEquals(2, called)
        assertTrue { verify }
    }

    @test fun `Pipes fulfilled promise with projection`() {
        var called = 0
        Promise.resolve(22).thenp {
            Promise.resolve(it * 2)
        }.then {
            assertEquals(44, it)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Pipes rejected promise with projection`() {
        var called = 0
        Promise.resolve(22).thenp {
            Promise.reject(Exception("Crash and burn"))
        }.catch {
            assertEquals("Crash and burn", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Finalizes fulfilled promise`() {
        var called = 0
        Promise.resolve("Hello")
        .finally {
            ++called
        }.then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Finalizes fulfilled promise projection`() {
        var called = 0
        Promise.resolve("Hello")
        .finallyp {
            ++called
            Promise.resolve("Goodbye")
        }.then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Finalizes rejected promise`() {
        var called  = 0
        val promise = Promise.reject(Exception("Rejected"))
        promise.finally {
            ++called
        }.catch {
            assertEquals("Rejected", it.message)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Ignores fulfilled and rejected if cancelled`() {
        var called    = false
        var cancelled = false
        var fulfill: (String) -> Unit = {}
        var failed:  (Throwable) -> Unit = {}

        val promise = Promise<String> { resolve, reject ->
            fulfill = resolve
            failed  = reject
        }.then {
            called = true
        }.catch {
            called = true
        }.finally {
            cancelled = true
        }
        promise.cancel()
        fulfill("Hello")
        failed(Exception())
        assertEquals(PromiseState.Cancelled, promise.state)
        assertFalse { called }
        assertTrue { cancelled }
    }

    @test fun `Ignores fulfilled and rejected if CancellationException`() {
        var called    = false
        var cancelled = false
        val promise = Promise.resolve("Hello")
            .then { throw CancellationException() }
            .then { called = true }
            .catch { cancelled = true }
            .cancelled { cancelled = true }
        promise.cancel()
        assertEquals(PromiseState.Cancelled, promise.state)
        assertFalse { called }
        assertTrue { cancelled }
    }

    @test fun `Calls finally when cancelled`() {
        var called    = false
        var cancelled = false
        val promise = Promise<String> {_, _ -> }
            .finally { called = true }
        promise.cancel()
        promise.cancelled { cancelled = true }
        assertEquals(PromiseState.Cancelled, promise.state)
        assertTrue { called }
        assertTrue { cancelled }
    }

    @test fun `Calls finally if CancelledException`() {
        var called    = false
        var cancelled = false
        val promise = Promise.resolve("String")
            .then { _ -> throw CancellationException() }
            .finally { called = true }
        promise.cancelled { cancelled = true }
        assertEquals(PromiseState.Cancelled, promise.state)
        assertTrue { called }
        assertTrue { cancelled }
    }


    @test fun `Behaves covariantly`() {
        val promise = Promise.resolve(listOf(1, 2, 3))
        val promise2 : Promise<Collection<Int>> = promise
        promise2.then {
            assertTrue { it.containsAll(listOf(1, 2, 3)) }
        }
    }
}