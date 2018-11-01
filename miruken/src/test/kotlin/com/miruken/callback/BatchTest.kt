package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.protocol.proxy
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BatchTest {
    @Rule
    @JvmField val testName = TestName()

    @Test fun `Batches protocols`() {
        val handler = EmailHandler()
        assertAsync(testName) { done ->
            assertEquals("Hello", handler.proxy<Emailing>().send("Hello"))
            handler.batch { batch ->
                assertNull(batch.proxy<Emailing>().send("Hello"))
            } then {
                assertEquals(1, it.size)
                assertEquals(listOf("Hello batch"), it[0])
                done()
            }
        }
        assertEquals("Hello", handler.proxy<Emailing>().send("Hello"))
    }

    @Test fun `Batches protocols promises`() {
        var count   = 0
        val handler = EmailHandler()
        assertAsync(testName) { done ->
            handler.proxy<Emailing>().sendConfirm("Hello") then {
                assertEquals("Hello", it)
                ++count
            }
            handler.batch { batch ->
                batch.proxy<Emailing>().sendConfirm("Hello") then {
                    assertEquals("Hello batch", it)
                    ++count
                }
            } then { result ->
                assertEquals(1, result.size)
                assertEquals(listOf("Hello"), result[0])
                handler.proxy<Emailing>().sendConfirm("Hello") then {
                    assertEquals("Hello", it)
                    ++count
                    done()
                }
            }
        }
        assertEquals(3, count)
    }

    @Test fun `Rejects Batch protocols promise`() {
        var count   = 0
        val handler = EmailHandler()
        assertAsync(testName) { done ->
            handler.batch { batch ->
                batch.proxy<Emailing>().failConfirm("Hello") catch {
                    assertEquals("Can't send message", it.message)
                    ++count
                }
            } catch {
                assertEquals("Can't send message", it.message)
                handler.proxy<Emailing>().failConfirm("Hello") catch { t ->
                    assertEquals("Can't send message", t.message)
                    ++count
                    done()
                }
            }
        }
        assertEquals(2, count)
    }

    @Test fun `Batches requested protocols`() {
        val handler = EmailHandler()
        assertAsync(testName) { done ->
            assertEquals("Hello", handler.proxy<Emailing>().send("Hello"))
            handler.batchOver<Emailing> { batch ->
                assertNull(batch.proxy<Emailing>().send("Hello"))
            } then {
                assertEquals(1, it.size)
                assertEquals(listOf("Hello batch"), it[0])
                done()
            }
        }
    }

    @Test fun `Batches requested protocols promises`() {
        var count   = 0
        val handler = EmailHandler()
        assertAsync(testName) { done ->
            handler.proxy<Emailing>().sendConfirm("Hello") then {
                assertEquals("Hello", it)
                ++count
            }
            handler.batchOver<Emailing> { batch ->
                batch.proxy<Emailing>().sendConfirm("Hello") then {
                    assertEquals("Hello batch", it)
                    ++count
                }
            } then { result ->
                assertEquals(1, result.size)
                assertEquals(listOf("Hello"), result[0])
                handler.proxy<Emailing>().sendConfirm("Hello") then {
                    assertEquals("Hello", it)
                    ++count
                    done()
                }
            }
        }
        assertEquals(3, count)
    }

    @Test fun `Ignores batches for unrequested protocols`() {
        val handler = EmailHandler()
        assertAsync(testName) { done ->
            assertEquals("Hello", handler.proxy<Emailing>().send("Hello"))
            handler.batchOver<Offline> { batch ->
                batch.proxy<Emailing>().sendConfirm("Hello") then {
                    assertEquals("Hello", it)
                    done()
                }
            }
        }
    }

    interface Emailing {
        fun send(message: Any): Any?
        fun sendConfirm(message: Any): Promise<*>
        fun fail(message: Any): Any?
        fun failConfirm(message: Any): Promise<*>
    }

    interface Offline : Emailing

    class EmailHandler : Handler(), Emailing {
        override fun send(message: Any): Any? =
                COMPOSER!!.getBatcherFor<Emailing, EmailBatch> { EmailBatch() }
                        ?.run { return send(message) }
                        ?: message

        override fun sendConfirm(message: Any): Promise<*> =
                COMPOSER!!.getBatcherFor<Emailing, EmailBatch> { EmailBatch() }
                        ?.run { return sendConfirm(message) }
                        ?: Promise.resolve(message)

        override fun fail(message: Any): Any? =
            if (message == "OFF")
                COMPOSER!!.proxy<Offline>().fail(message)
                else throw IllegalStateException("Can't send message")

        override fun failConfirm(message: Any): Promise<*> =
                COMPOSER!!.getBatcherFor<Emailing, EmailBatch> { EmailBatch() }
                        ?.run { return failConfirm(message) }
                        ?: Promise.reject(IllegalStateException(
                                "Can't send message"))
    }

    class EmailBatch : Emailing, Batching {
        private val _messages = mutableListOf<Any>()
        private val _promises = mutableListOf<Promise<*>>()
        private val _resolves = mutableListOf<() -> Any?>()

        override fun send(message: Any): Any? {
            _messages.add("$message batch")
            return null
        }

        override fun sendConfirm(message: Any): Promise<*> {
            _messages.add(message)
            val promise = Promise<Any> { resolve, _ ->
                _resolves.add { resolve("$message batch")}
            }
            _promises.add(promise)
            return promise
        }

        override fun fail(message: Any): Any? {
            notHandled()
        }

        override fun failConfirm(message: Any): Promise<*> {
            val promise = Promise<Any> { _, reject ->
                _resolves.add {
                    reject(IllegalStateException("Can't send message")) }
            }
            _promises.add(promise)
            return promise
        }

        override fun complete(composer: Handling): Any? {
            _resolves.forEach { it() }
            return composer.proxy<Emailing>().send(_messages)
                    .let { results ->
                        if (_promises.isEmpty()) results
                        else Promise.all(_promises) then { results }
                    }
        }
    }
}