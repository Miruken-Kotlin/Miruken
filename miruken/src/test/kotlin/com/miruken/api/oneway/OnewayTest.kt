package com.miruken.api.oneway

import com.miruken.api.GetStockQuote
import com.miruken.api.JacksonProvider
import com.miruken.api.StockQuoteHandler
import com.miruken.api.send
import com.miruken.callback.plus
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.test.assertAsync
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals

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

    @Test fun `Serializes oneway request into json`() {
        val request = GetStockQuote("AAPL").oneway
        val json     = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Oneway.Oneway,Miruken\",\"request\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}}", json)
    }
}