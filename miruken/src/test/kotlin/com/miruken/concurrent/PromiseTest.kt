package com.miruken.concurrent

import com.miruken.*
import org.junit.rules.Stopwatch
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
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

    @test fun `Creates cancellable promise`() {
        val promise = Promise<String> { _, _, _ -> }
        assertTrue { promise.state === PromiseState.Pending }
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

    @test fun `Adopts resolved promise statically`() {
        var called = false
        val promise = Promise.resolve(
                Promise.resolve("Hello")) then {
            assertEquals("Hello", it)
            called = true
        }
        assertTrue { promise.state === PromiseState.Fulfilled }
        assertTrue { called }
    }

    @test fun `Adopts rejected promise statically`() {
        var called = false
        val promise = Promise.resolve(
                Promise.reject(Exception("Rejected"))) catch {
            assertEquals("Rejected", it.message)
            called = true
            throw it
        }
        assertTrue { promise.state === PromiseState.Rejected }
        assertTrue { called }
    }

    @test fun `Adopts resolved promise dynamically`() {
        var called   = false
        var any: Any = Promise.resolve("Hello")
        val promise  = Promise.resolve(any) then {
            assertEquals("Hello", it)
            called = true
        }
        assertTrue { promise.state === PromiseState.Fulfilled }
        assertTrue { called }
    }

    @test fun `Adopts rejected promise dynamically`() {
        var called   = false
        var any: Any = Promise.reject(Exception("Rejected"))
        val promise  = Promise.resolve(any) catch {
            assertEquals("Rejected", it.message)
            called = true
            throw it
        }
        assertTrue { promise.state === PromiseState.Rejected }
        assertTrue { called }
    }

    @test fun `Behaves covariantly`() {
        val promise = Promise.resolve(listOf(1, 2, 3))
        val promise2 : Promise<Collection<Int>> = promise
        promise2.then {
            assertTrue { it.containsAll(listOf(1, 2, 3)) }
        }
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
            it mapLeft {
              assertEquals(19, it)
              ++called
            }
        }
        assertEquals(2, called)
    }

    @test fun `Handles fulfilled promise with fail projection`() {
        var called = 0
        Promise.resolve(22).then(
            { it.toString() },
            { fail("Should skip") }
        ).then {
            it map {
                assertEquals("22", it)
                ++called
            }
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
            it mapLeft {
                assertEquals(1, it)
                ++called
            }
        }
        assertEquals(2, called)
    }

    @test fun `Propagates fulfilled promise`() {
        var called = 0
        Promise.resolve("Hello")
         .catch { }
         .then {
             it map {
                 assertEquals("Hello", it)
                 ++called
                 "Goodbye"
             }
        }.then {
            it map {
                assertEquals("Goodbye", it)
                ++called
            }
        }
        assertEquals(2, called)
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

    @test fun `Unwraps fulfilled promise with projection`() {
        var called = 0
        Promise.resolve(22) flatMap {
            Promise.resolve(it * 2)
        } then {
            assertEquals(44, it)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Unwraps fulfilled promise with rejection`() {
        var called = 0
        Promise.resolve(22) flatMap  {
            Promise.reject(Exception("Crash and burn"))
        } catch {
            assertEquals("Crash and burn", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Unwraps rejected promise with projection`() {
        var called = 0
        Promise.reject(Exception("Wrong Order")) flatMapError  {
            Promise.resolve("Soccer")
        } then {
            assertEquals("Soccer", it)
            ++called
        }
        assertEquals(1, called)
    }

    @test fun `Unwraps fulfilled promise with success or fail`() {
        var called = 0
        Promise.resolve(22).flatMap(
                { Promise.resolve(it.toString()) },
                { fail("Should skip") }
        ).then {
            it.map {
                assertEquals("22", it)
                ++called
            }
        }
        assertEquals(1, called)
    }

    @test fun `Finalizes fulfilled promise`() {
        var called = 0
        Promise.resolve("Hello") finally {
            ++called
        } then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Finalizes fulfilled promise projection`() {
        var called = 0
        Promise.resolve("Hello") finally {
            ++called
            Promise.resolve("Goodbye")
        } then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Finalizes fulfilled promise rejection`() {
        var called = 0
        Promise.resolve("Hello") finally {
            ++called
            Promise.reject(Exception("Not good"))
        } then {
            fail("Should skip")
        } catch {
            assertEquals("Not good", it.message)
            ++called
        }
        assertEquals(2, called)
    }

    @test fun `Finalizes rejected promise`() {
        var called  = 0
        val promise = Promise.reject(Exception("Rejected"))
        promise finally {
            ++called
        } catch {
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

    @test fun `Fulfills promise using infix notation`() {
        var called = 0
        Promise.resolve("Hello") then {
            assertEquals("Hello", it)
            ++called
            24
        } then {
            assertEquals(24, it)
            ++called
        }
       assertEquals(2, called)
    }

    @test fun `Rejects promise using infix notation`() {
        var called = 0
        Promise.reject(Exception("Cool!!")) catch {
            assertEquals("Cool!!", it.message)
            ++called
            19
        } then {
            it.mapLeft {
                assertEquals(19, it)
                ++called
            }
        }
        assertEquals(2, called)
    }

    @test fun `Waits for all promises to fulfill`() {
        var called   = false
        val promises = (1..5).map { Promise.resolve(it) }
        Promise.all(promises).then {
            called = true
        }
        //assertTrue { called }
    }

    @test fun `Resolves after delay`() {
        val stopwatch = object : Stopwatch() {}
        Promise.delay(300) then {
            val elapsed = stopwatch.runtime(TimeUnit.MILLISECONDS)
        }
    }
}