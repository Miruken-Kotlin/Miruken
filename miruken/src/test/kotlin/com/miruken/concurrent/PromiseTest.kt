package com.miruken.concurrent

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
                Promise<String> { _, reject ->
            reject(Exception("Rejected"))
        })
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
        Promise<Int> { resolve, _ ->
            resolve(22)
        }.then {
            it.toString()
        }.then { num: String ->
            assertEquals("22", num)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Propagates fulfilled promise`() {
        var called = 0
        Promise<String> { resolve, _ ->
            resolve("Hello")
        }.catch { }
         .then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Propagates rejected promise`() {
        var called  = 0
        var verify  = false
        val promise = Promise<String> { resolve, _ ->
            resolve("Hello")
        }
        promise.then {
            assertEquals("Hello", it)
            ++called
        }
        promise.then {
            throw Exception("Bad")
        }.then {
            fail("Should Skip")
        }.catch {
            assertEquals("Bad", it.message)
            ++called
        }.then {
            verify = true
        }
        assertEquals(2, called)
        assertTrue { verify }
    }

    @test fun `Finalizes fulfilled promise`() {
        var called = 0
        Promise<String> { resolve, _ ->
            resolve("Hello")
        }.finally {
            ++called
        }.then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Finalizes rejected promise`() {
        var called  = 0
        val promise = Promise<String> { _, reject ->
            reject(Exception("Rejected"))
        }
        promise.finally {
            ++called
        }.catch {
            assertEquals("Rejected", it.message)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Ignores fulfilled and rejected if cancelled`() {
        var called = false
        var cancel = false
        var fulfill: (String) -> Unit
        var fail:    (Throwable) -> Unit

        val promise = Promise<String> { resolve, reject ->
            fulfill = resolve
            fail    = reject
        }.then {
            called = true
        }.catch {
            //called = true
        }
    }

    @test fun `Behaves covariantly`() {
        val promise = Promise.resolve(listOf(1, 2, 3))
        val promise2 : Promise<Collection<Int>> = promise
        promise2.then {
            assertTrue { it.containsAll(listOf(1, 2, 3)) }
        }
    }
}