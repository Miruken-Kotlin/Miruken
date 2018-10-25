package com.miruken.mediate.oneway

import com.miruken.assertAsync
import com.miruken.callback.plus
import com.miruken.mediate.cache.GetStockQuote
import com.miruken.mediate.cache.StockQuoteHandler
import com.miruken.mediate.send
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals

class OnewayHandlerTest {
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
}