package com.miruken.api.oneway

import com.miruken.api.*
import com.miruken.callback.plus
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.test.assertAsync
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnewayTest {
    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<StockQuoteHandler>()
        factory.registerDescriptor<OnewayHandler>()
        HandlerDescriptorFactory.useFactory(factory)
    }

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

    @Test
    fun `Ignores response suspending`() = runBlocking {
        StockQuoteHandler.called = 0
        val handler  = StockQuoteHandler() + OnewayHandler()
        val getQuote = GetStockQuote("AAPL")
        handler.sendCo(getQuote.oneway)
        assertEquals(1, StockQuoteHandler.called)
    }

    @Test fun `Serializes oneway request into json`() {
        val request = GetStockQuote("AAPL").oneway
        val json     = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Oneway.Oneway,Miruken\",\"request\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}}", json)
    }
}