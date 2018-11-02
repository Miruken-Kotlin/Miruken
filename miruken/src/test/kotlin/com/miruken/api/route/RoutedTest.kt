package com.miruken.api.route

import com.miruken.api.JacksonHelper
import com.miruken.assertAsync
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.api.GetStockQuote
import com.miruken.api.StockQuoteHandler
import com.miruken.api.oneway.oneway
import com.miruken.api.send
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutedTest {
    private lateinit var _handler: Handling

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        _handler = (StockQuoteHandler() +
                    PassThrough() +
                    Trash())
        StockQuoteHandler.called = 0
    }

    @Test fun `Routes request`() {
        assertAsync(testName) { done ->
            _handler.send(GetStockQuote("GOOGL")
                    .oneway.routeTo("trash")) then {
                assertNull(it)
                _handler.send(GetStockQuote("GOOGL")
                        .routeTo("pass-through")) then { quote ->
                    assertEquals("GOOGL", quote.symbol)
                    assertEquals(1, StockQuoteHandler.called)
                    done()
                }
            }
        }
    }

    @Test fun `Fails missing route`() {
        assertAsync(testName) { done ->
            _handler.send(GetStockQuote("GOOGL")
                    .routeTo("nowhere")) catch { e ->
                assertTrue(e is NotHandledException)
                done()
            }
        }
    }

    @Test fun `Serializes routed request into json`() {
        val request = GetStockQuote("AAPL").routeTo("http://server/api")
        val json    = JacksonHelper.json.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Mediate.Route.RoutedRequest`1[[GetStockQuote]],Miruken.Mediate\",\"message\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"},\"route\":\"http://server/api\"}", json)
    }

    class PassThrough : Handler() {
        @Handles
        fun route(request: Routed, composer: Handling) =
                composer.takeIf { request.route == scheme }
                        ?.send(request.message, request.messageType)

        companion object {
            const val scheme = "pass-through"
        }
    }

    class Trash : Handler() {
        @Handles
        fun route(request: Routed) =
            Promise.EMPTY.takeIf { request.route == scheme }

        companion object {
            const val scheme = "trash"
        }
    }
}