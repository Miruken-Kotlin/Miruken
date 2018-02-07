package com.miruken.concurrent

import kotlin.test.*
import org.junit.Test as test

class PromiseTest {
    @test fun `Starts in pending state`() {
        val promise = Promise<String> { _, _ -> }
        assertTrue { promise.state === PromiseState.Pending }
    }

    @test fun `Creates resolved promise`() {
        val promise = "Hello".toPromise()
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
        val promise = "Hello".toPromise().toPromise()
        assertTrue { promise.state === PromiseState.Fulfilled }
    }

    @test fun `Adopts rejected promise`() {
        val promise = Promise<String> { _, reject ->
            reject(Exception("Rejected"))
        }.toPromise()
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

    @test fun `Behaves covariantly`() {
        val promise = listOf(1, 2, 3).toPromise()
        val promise2 : Promise<Collection<Int>> = promise
        promise2.then {
            assertTrue { it.containsAll(listOf(1, 2, 3)) }
        }
    }
}