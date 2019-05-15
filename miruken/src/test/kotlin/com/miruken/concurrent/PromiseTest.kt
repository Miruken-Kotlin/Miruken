package com.miruken.concurrent

import com.miruken.map
import com.miruken.mapLeft
import com.miruken.test.assertAsync
import org.junit.Rule
import org.junit.rules.TestName
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import kotlin.test.*

class PromiseTest {
    @Rule
    @JvmField val testName = TestName()

    @Test fun `Starts in pending state`() {
        val promise = Promise<String> { _, _ -> }
        assertEquals(promise.state, PromiseState.PENDING)
    }

    @Test fun `Creates resolved promise`() {
        val promise =  Promise.resolve("Hello")
        assertEquals(promise.state, PromiseState.FULFILLED)
    }

    @Test fun `Creates rejected promise`() {
        val promise = Promise.reject(Exception("Error"))
        assertEquals(promise.state, PromiseState.REJECTED)
    }

    @Test fun `Creates cancellable promise`() {
        val promise = Promise<String> { _, _, _ -> }
        assertEquals(promise.state, PromiseState.PENDING)
    }

    @Test fun `Resolves promise`() {
        val promise = Promise<String> { resolve, _ ->
            resolve("Hello")
        }
        assertEquals(promise.state, PromiseState.FULFILLED)
    }

    @Test fun `Rejects promise`() {
        val promise = Promise<String> { _, reject ->
            reject(Exception("Rejected"))
        }
        assertEquals(promise.state, PromiseState.REJECTED)
    }

    @Test fun `Adopts resolved promise statically`() {
        var called = false
        val promise = Promise.resolve(
                Promise.resolve("Hello")) then {
            assertEquals("Hello", it)
            called = true
        }
        assertEquals(promise.state, PromiseState.FULFILLED)
        assertTrue(called)
    }

    @Test fun `Adopts rejected promise statically`() {
        var called = false
        val promise = Promise.resolve(
                Promise.reject(Exception("Rejected"))) catch {
            assertEquals("Rejected", it.message)
            called = true
            throw it
        }
        assertSame(promise.state, PromiseState.REJECTED)
        assertTrue(called)
    }

    @Test fun `Adopts resolved promise dynamically`() {
        var called   = false
        val any: Any = Promise.resolve("Hello")
        val promise  = Promise.resolve(any) then {
            assertEquals("Hello", it)
            called = true
        }
        assertSame(promise.state, PromiseState.FULFILLED)
        assertTrue(called)
    }

    @Test fun `Adopts rejected promise dynamically`() {
        var called   = false
        val any: Any = Promise.reject(Exception("Rejected"))
        val promise  = Promise.resolve(any) catch {
            assertEquals("Rejected", it.message)
            called = true
            throw it
        }
        assertSame(promise.state, PromiseState.REJECTED)
        assertTrue(called)
    }

    @Test fun `Promises are covariant`() {
        val promise = Promise.resolve(listOf(1, 2, 3))
        val promise2 : Promise<Collection<Int>> = promise
        promise2.then {
            assertTrue(it.containsAll(listOf(1, 2, 3)))
        }
    }

    @Test fun `Returns canonical true promise`() {
        var called = false
        Promise.TRUE then {
            assertTrue(it)
            called = true
        }
        assertTrue(called)
        assertSame(Promise.TRUE, Promise.TRUE)
    }

    @Test fun `Returns canonical false promise`() {
        var called = false
        Promise.FALSE then {
            assertFalse(it)
            called = true
        }
        assertTrue(called)
        assertSame(Promise.FALSE, Promise.FALSE)
    }

    @Test fun `Returns canonical empty promise`() {
        var called = false
        Promise.EMPTY then {
            assertNull(it)
            called = true
        }
        assertTrue(called)
        assertSame(Promise.EMPTY, Promise.EMPTY)
    }

    @Test fun `Fulfills promise only once`() {
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

    @Test fun `Rejects promise only once`() {
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

    @Test fun `Fulfills promise with projection`() {
        var called = 0
        Promise.resolve(22).then {
            it.toString()
        }.then { num: String ->
            assertEquals("22", num)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Rejects promise with projection`() {
        var called = 0
        Promise<Int> { _, reject ->
            reject(Exception("Rejected"))
        }.catch {
            assertEquals("Rejected", it.message)
            ++called
            19
        }.then {
            it mapLeft { result ->
              assertEquals(19, result)
              ++called
            }
        }
        assertEquals(2, called)
    }

    @Test fun `Handles fulfilled promise with fail projection`() {
        var called = 0
        Promise.resolve(22).then(
            { it.toString() },
            { fail("Should skip") }
        ).then {
            it map { result ->
                assertEquals("22", result)
                ++called
            }
        }
        assertEquals(1, called)
    }

    @Test fun `Handles fulfilled promise with fail projection of same type`() {
        var called = 0
        Promise.resolve(22).map(
                { it.toString() },
                { "Hello" }
        ).then { it: String ->
            assertEquals("22", it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Handles fulfilled promise with fail projection using Any`() {
        var called = 0
        Promise.resolve(19).map(
                { it.toString() },
                { 15 }
        ).then { it: Any ->
            assertEquals("19", it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Handles rejected promise with success projection`() {
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
            it mapLeft { result ->
                assertEquals(1, result)
                ++called
            }
        }
        assertEquals(2, called)
    }

    @Test fun `Handles rejected promise with success projection of same type`() {
        var called = 0
        Promise<Int> { _, reject ->
            reject(Exception("Halt and catch fire"))
        }.map(
                { 0 },
                {
                    assertEquals("Halt and catch fire", it.message)
                    ++called
                }
        ).then { it: Int ->
            assertEquals(1, it)
            ++called
        }
        assertEquals(2, called)
    }

    @Test fun `Infers Any type if mapping different success fail results`() {
        var called = 0
        Promise.resolve(19).map(
                { it.toString() },
                { 15 }
        ).then { it: Any ->
            assertEquals("19", it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Propagates fulfilled promise`() {
        var called = 0
        Promise.resolve("Hello")
         .catch { }
         .then {
             it map { result ->
                 assertEquals("Hello", result)
                 ++called
                 "Goodbye"
             }
        }.then {
            it map { result ->
                assertEquals("Goodbye", result)
                ++called
            }
        }
        assertEquals(2, called)
    }

    @Test fun `Propagates rejected promise`() {
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
        assertTrue(verify)
    }

    @Test fun `Unwraps fulfilled promise with projection`() {
        var called = 0
        Promise.resolve(22) flatMap {
            Promise.resolve(it * 2)
        } then {
            assertEquals(44, it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Unwraps fulfilled promise with rejection`() {
        var called = 0
        Promise.resolve(22) flatMap {
            Promise.reject(Exception("Crash and burn"))
        } catch {
            assertEquals("Crash and burn", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Unwraps rejected promise with projection`() {
        var called = 0
        Promise.reject(Exception("Wrong Order")) flatMapError {
            Promise.resolve("Soccer")
        } then {
            assertEquals("Soccer", it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Unwraps fulfilled promise with success`() {
        var called = 0
        Promise.resolve(22).flatMap(
                { Promise.resolve(it.toString()) },
                { Promise.reject(Exception("Should skip")) }
        ) then {
            it.map { result ->
                assertEquals("22", result)
                ++called
            }
        }
        assertEquals(1, called)
    }

    @Test fun `Unwraps fulfilled promise with fail`() {
        var called = 0
        Promise.resolve(22).flatMap(
                { Promise.reject(Exception("Broken")) },
                { Promise.resolve(it.toString())  }
        ) catch {
            assertEquals("Broken", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Folds fulfilled promise with success`() {
        var called = 0
        Promise.resolve(22).fold(
                { Promise.resolve(it.toString()) },
                { Promise.resolve("19") }
        ) then {
            assertEquals("22", it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Folds rejected promise with success`() {
        var called = 0
        Promise.reject(Exception("Bad parity")).fold(
                { Promise.resolve(7) },
                { Promise.resolve(24) }
        ) then {
            assertEquals(24, it)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Folds fulfilled promise with failure`() {
        var called = 0
        Promise.resolve(22).fold(
                { Promise.reject(Exception("Timeout")) },
                { Promise.resolve("19") }
        ) catch {
            assertEquals("Timeout", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Folds rejected promise with failure`() {
        var called = 0
        Promise.reject(Exception("Bad parity")).fold(
                { Promise.resolve(11) },
                { Promise.reject(Exception("Unknown Server")) }
        ) catch {
            assertEquals("Unknown Server", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Unwraps rejected promise with success`() {
        var called = 0
        Promise.reject(Exception("Bad timing")).flatMap(
                { Promise.resolve(3) },
                { Promise.resolve("Hello") }
        ) then {
            it.mapLeft { result ->
                assertEquals("Hello", result)
                ++called
            }
        }
        assertEquals(1, called)
    }

    @Test fun `Unwraps rejected promise with fail`() {
        var called = 0
        Promise.reject(Exception("Bad timing")).flatMap(
                { Promise.resolve(3) },
                { Promise.reject(Exception("Broken")) }
        ) catch {
            assertEquals("Broken", it.message)
            ++called
        }
        assertEquals(1, called)
    }

    @Test fun `Finalizes fulfilled promise`() {
        var called = 0
        Promise.resolve("Hello") finally {
            ++called
        } then {
            assertEquals("Hello", it)
            ++called
        }
        assertEquals(2, called)
    }

    @Test fun `Finalizes fulfilled promise projection`() {
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

    @Test fun `Finalizes fulfilled promise rejection`() {
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

    @Test fun `Finalizes rejected promise`() {
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

    @Test fun `Ignores fulfilled and rejected if cancelled`() {
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
        assertEquals(PromiseState.CANCELLED, promise.state)
        assertFalse(called)
        assertTrue(cancelled)
    }

    @Test fun `Ignores fulfilled and rejected if CancellationException`() {
        var called    = false
        var cancelled = false
        val promise = Promise.resolve("Hello")
            .then { throw CancellationException() }
            .then { called = true }
            .catch { cancelled = true }
            .cancelled { cancelled = true }
        promise.cancel()
        assertEquals(PromiseState.CANCELLED, promise.state)
        assertFalse(called)
        assertTrue(cancelled)
    }

    @Test fun `Calls finally when cancelled`() {
        var called    = false
        var cancelled = false
        val promise = Promise<String> {_, _ -> }
            .finally { called = true }
        promise.cancel()
        promise.cancelled { cancelled = true }
        assertEquals(PromiseState.CANCELLED, promise.state)
        assertTrue(called)
        assertTrue(cancelled)
    }

    @Test fun `Calls finally if CancelledException`() {
        var called    = false
        var cancelled = false
        val promise = Promise.resolve("String")
            .then { throw CancellationException() }
            .finally { called = true }
        promise.cancelled { cancelled = true }
        assertEquals(PromiseState.CANCELLED, promise.state)
        assertTrue(called)
        assertTrue(cancelled)
    }

    @Test fun `Fulfills promise using infix notation`() {
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

    @Test fun `Rejects promise using infix notation`() {
        var called = 0
        Promise.reject(Exception("Cool!!")) catch {
            assertEquals("Cool!!", it.message)
            ++called
            19
        } then {
            it.mapLeft { result ->
                assertEquals(19, result)
                ++called
            }
        }
        assertEquals(2, called)
    }

    @Test fun `Waits for all promises to fulfill`() {
        var called   = false
        val promises = (1..5).map { Promise.resolve(it) }
        Promise.all(promises) then {
            assertEquals(promises.size, it.size)
            assertEquals(it, (1..5).toList())
            called = true
        }
        assertTrue(called)
    }

    @Test fun `Rejects promise if any promise fails`() {
        var called   = false
        val promises = (1..5).mapIndexed { index, result ->
            if (index == 3)
                Promise.reject(Exception("$index failed"))
            else
                Promise.resolve(result)
        }
        Promise.all(promises) catch {
            assertEquals("3 failed", it.message)
            called = true
        }
        assertTrue(called)
    }

    @Test fun `Cancels promise if any promise cancelled`() {
        var called   = false
        val promises = (1..5).mapIndexed { index, result ->
            if (index == 3)
                Promise<Boolean> { _, _ -> }.apply { cancel() }
            else
                Promise.resolve(result)
        }
        Promise.all(promises) cancelled {
            called = true
        }
        assertTrue(called)
    }

    @Test fun `Converts return into a promise`() {
        var called = false
        Promise.`try` {
           2 * 3
        } then {
            assertEquals(6, it)
            called = true
        }
        assertTrue(called)
    }

    @Test fun `Ensures exception become rejected promise`() {
        var called = false
        Promise.`try` {
            val x:Any? = null
            x!!
        } catch {
            called = true
        }
        assertTrue(called)
    }

    @Test fun `Follows fulfilled promise from return`() {
        var called = false
        Promise.`try` {
            Promise.resolve("Hello")
        }.flatten() then {
            assertEquals("Hello", it)
            called = true
        }
        assertTrue(called)
    }

    @Test fun `Follows rejected promise from return`() {
        Promise.`try` {
            Promise.reject(Exception("Rejected"))
        } catch {
            assertEquals("Rejected", it.message)
        }
    }

    @Test fun `Resolves new promise after delay`() {
        assertAsync(testName) { done ->
            val start = Instant.now()
            Promise.delay(50) then {
                val elapsed = Duration.between(start, Instant.now())
                assertTrue(elapsed.toMillis() >= 50)
                done()
            }
        }
    }

    @Test fun `Resolves promise if before timeout`() {
        assertAsync(testName) { done ->
            (Promise.delay(50) then { 22 })
                    .timeout(100) then {
                assertEquals(22, it)
                done()
            }
        }
    }

    @Test fun `Rejects promise if after timeout`() {
        assertAsync(testName) { done ->
            (Promise.delay(100) then { "Hello "})
                    .timeout(50) catch {
                assertTrue(it is TimeoutException)
                done()
            }
        }
    }

    @Test fun `Cancels timeout when promise cancelled`() {
        assertAsync(testName) { done ->
            val promise = Promise.delay(100).timeout(50) then {
                fail("Should skip")
            } cancelled {
                done()
            }
            promise.cancel()
        }
    }

    @Test fun `Waits synchronously for promise to fulfill`() {
        val promise = Promise.delay(50) flatMap  {
            Promise.resolve("Hello") }
        val result = promise.get(5000)
        assertEquals("Hello", result)
    }

    @Test fun `Waits synchronously for promise to fail`() {
        val promise = Promise.delay(50) then  {
            throw IllegalArgumentException("Bad date") }
        assertFailsWith(IllegalArgumentException::class, "Bad date") {
            promise.get(5000)
        }
    }

    @Test fun `Waits until timeout for promise to fulfill`() {
        assertFailsWith(TimeoutException::class) {
            Promise<String> { _, _ -> }.get(50)
        }
    }
}

