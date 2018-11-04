package com.miruken.api.oneway

import com.miruken.api.JacksonHelper
import com.miruken.assertAsync
import com.miruken.callback.plus
import com.miruken.api.GetStockQuote
import com.miruken.api.StockQuoteHandler
import com.miruken.api.send
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals

class OnewayTest {
    @Rule
    @JvmField val testName = TestName()

    @Test
    fun `Ignores response`() {
        StockQuoteHandler.called = 0
        val handler = StockQuoteHandler() + OnewayHandler()
        assertAsync(testName) { done ->
            val getQuote = GetStockQuote("AAPL")
            handler.send(getQuote.oneway) then {
                assertEquals(1, StockQuoteHandler.called)
                done()
            }
        }
    }

    @Test fun `Serializes oneway request into json`() {
        val request = GetStockQuote("AAPL").oneway
        val json     = JacksonHelper.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Mediate.Oneway.Oneway`1[[GetStockQuote]],Miruken.Mediate\",\"request\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}}", json)
    }
}