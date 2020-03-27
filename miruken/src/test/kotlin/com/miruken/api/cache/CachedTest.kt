package com.miruken.api.cache

import com.miruken.api.*
import com.miruken.callback.Handling
import com.miruken.callback.plus
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.concurrent.delay
import com.miruken.test.assertAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.time.Duration
import kotlin.test.assertEquals

class CachedTest {
    private lateinit var handler: Handling

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<StockQuoteHandler>()
        factory.registerDescriptor<CachedHandler>()
        HandlerDescriptorFactory.useFactory(factory)
        handler = StockQuoteHandler() + CachedHandler()
        StockQuoteHandler.called = 0
    }

    @Test fun `Makes initial request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            handler.send(getQuote.cache()) then {
                assertEquals("AAPL", it.symbol)
                assertEquals(1, StockQuoteHandler.called)
                done()
            }
        }
    }

    @Test fun `Makes initial request suspending`() = runBlocking {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        val quote    = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote.symbol)
        assertEquals(1, StockQuoteHandler.called)
    }

    @Test fun `Caches initial request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                handler.send(getQuote.cache()) then { quote2 ->
                    assertEquals("AAPL", quote2.symbol)
                    assertEquals(1, StockQuoteHandler.called)
                    done()
                }
            }
        }
    }

    @Test fun `Caches initial request suspending`() = runBlocking {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        val quote1   = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote1.symbol)
        assertEquals(1, StockQuoteHandler.called)
        val quote2   = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote2.symbol)
        assertEquals(1, StockQuoteHandler.called)
    }

    @Test fun `Refreshes cached request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                handler.send(getQuote.cache()) then {
                    assertEquals(1, StockQuoteHandler.called)
                    handler.send(getQuote.refresh()) then { quote2 ->
                        assertEquals("AAPL", quote2.symbol)
                        assertEquals(2, StockQuoteHandler.called)
                        done()
                    }
                }
            }
        }
    }

    @Test fun `Refreshes cached request suspending`() = runBlocking {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        val quote1   = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote1.symbol)
        assertEquals(1, StockQuoteHandler.called)
        handler.sendCo(getQuote.cache())
        assertEquals(1, StockQuoteHandler.called)
        val quote2   = handler.sendCo(getQuote.refresh())
        assertEquals("AAPL", quote2.symbol)
        assertEquals(2, StockQuoteHandler.called)
    }

    @Test fun `Refreshes stale request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                Promise.delay(200) then {
                    handler.send(getQuote.cache(
                            Duration.ofMillis(100))) then { quote2 ->
                        assertEquals("AAPL", quote2.symbol)
                        assertEquals(2, StockQuoteHandler.called)
                        done()
                    }
                }
            }
        }
    }

    @Test fun `Refreshes stale request suspending`() = runBlocking {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        val quote1   = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote1.symbol)
        assertEquals(1, StockQuoteHandler.called)
        delay(200)
        val quote2   = handler.sendCo(getQuote.cache(Duration.ofMillis(100)))
        assertEquals("AAPL", quote2.symbol)
        assertEquals(2, StockQuoteHandler.called)
    }

    @Test fun `Invalidates response`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                handler.send(getQuote.invalidate()) then { quote2 ->
                    assertEquals("AAPL", quote2!!.symbol)
                    assertEquals(quote1.value, quote2.value)
                    assertEquals(1, StockQuoteHandler.called)
                    handler.send(getQuote.cache()) then { quote3 ->
                        assertEquals("AAPL", quote3.symbol)
                        assertEquals(2, StockQuoteHandler.called)
                        done()
                    }
                }
            }
        }
    }

    @Test fun `Invalidates response suspending`() = runBlocking {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        val quote1   = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote1.symbol)
        assertEquals(1, StockQuoteHandler.called)
        val quote2   = handler.sendCo(getQuote.invalidate())
        assertEquals("AAPL", quote2!!.symbol)
        assertEquals(quote1.value, quote2.value)
        assertEquals(1, StockQuoteHandler.called)
        val quote3   = handler.sendCo(getQuote.cache())
        assertEquals("AAPL", quote3.symbol)
        assertEquals(2, StockQuoteHandler.called)
    }

    @Test fun `Refreshes failed requests`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("EX")
        assertAsync(testName) { done ->
            handler.send(getQuote.cache()) catch { t ->
                assertEquals(1, StockQuoteHandler.called)
                assertEquals("Stock Exchange is down", t.message)
                handler.send(getQuote.cache()) then { quote1 ->
                    assertEquals("EX", quote1.symbol)
                    assertEquals(2, StockQuoteHandler.called)
                    done()
                }
            }
        }
    }

    @Test fun `Refreshes failed requests suspending`() = runBlocking<Unit> {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("EX")
        try {
            handler.sendCo(getQuote.cache())
        } catch (t: Throwable) {
            assertEquals(1, StockQuoteHandler.called)
            assertEquals("Stock Exchange is down", t.message)
            val quote1 = handler.sendCo(getQuote.cache())
            assertEquals("EX", quote1.symbol)
            assertEquals(2, StockQuoteHandler.called)
        }
    }

    @Test fun `Serializes cached request into json`() {
        val request  = GetStockQuote("AAPL").cache()
        val json     = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Cache.Cached`1[[StockQuote]],Miruken\",\"request\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}}", json)
    }
}