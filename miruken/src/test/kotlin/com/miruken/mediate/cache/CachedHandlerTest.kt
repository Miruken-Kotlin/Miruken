package com.miruken.mediate.cache

import com.miruken.assertAsync
import com.miruken.callback.Handling
import com.miruken.callback.plus
import com.miruken.concurrent.Promise
import com.miruken.concurrent.delay
import com.miruken.mediate.send
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.time.Duration
import kotlin.test.assertEquals

class CachedHandlerTest {
    private lateinit var _handler: Handling

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        _handler = StockQuoteHandler() + CachedHandler()
        StockQuoteHandler.called = 0
    }

    @Test fun `Makes initial request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            _handler.send(getQuote.cache()) then {
                assertEquals("AAPL", it.symbol)
                assertEquals(1, StockQuoteHandler.called)
                done()
            }
        }
    }

    @Test fun `Caches initial request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            _handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                _handler.send(getQuote.cache()) then { quote2 ->
                    assertEquals("AAPL", quote2.symbol)
                    assertEquals(1, StockQuoteHandler.called)
                    done()
                }
            }
        }
    }

    @Test fun `Refreshes cached request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            _handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                _handler.send(getQuote.cache()) then {
                    assertEquals(1, StockQuoteHandler.called)
                    _handler.send(getQuote.refresh()) then { quote2 ->
                        assertEquals("AAPL", quote2.symbol)
                        assertEquals(2, StockQuoteHandler.called)
                        done()
                    }
                }
            }
        }
    }

    @Test fun `Refreshes stale request`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            _handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                Promise.delay(200) then {
                    _handler.send(getQuote.cache(
                            Duration.ofMillis(100))) then { quote2 ->
                        assertEquals("AAPL", quote2.symbol)
                        assertEquals(2, StockQuoteHandler.called)
                        done()
                    }
                }
            }
        }
    }

    @Test fun `Invalidates response`() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("AAPL")
        assertAsync(testName) { done ->
            _handler.send(getQuote.cache()) then { quote1 ->
                assertEquals("AAPL", quote1.symbol)
                assertEquals(1, StockQuoteHandler.called)
                _handler.send(getQuote.invalidate()) then { quote2 ->
                    assertEquals("AAPL", quote2!!.symbol)
                    assertEquals(quote1.value, quote2.value)
                    assertEquals(1, StockQuoteHandler.called)
                    _handler.send(getQuote.cache()) then { quote3 ->
                        assertEquals("AAPL", quote3.symbol)
                        assertEquals(2, StockQuoteHandler.called)
                        done()
                    }
                }
            }
        }
    }

    @Test fun `Refreshes failed requests `() {
        assertEquals(0, StockQuoteHandler.called)
        val getQuote = GetStockQuote("EX")
        assertAsync(testName) { done ->
            _handler.send(getQuote.cache()) catch { t ->
                assertEquals(1, StockQuoteHandler.called)
                assertEquals("Stock Exchange is down", t.message)
                _handler.send(getQuote.cache()) then { quote1 ->
                    assertEquals("EX", quote1.symbol)
                    assertEquals(2, StockQuoteHandler.called)
                    done()
                }
            }
        }
    }
}