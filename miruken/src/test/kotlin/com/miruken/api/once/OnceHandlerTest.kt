package com.miruken.api.once

import com.miruken.api.*
import com.miruken.callback.Handler
import com.miruken.callback.Handling
import com.miruken.callback.NotHandledException
import com.miruken.callback.plus
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.map.Maps
import com.miruken.test.assertAsync
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnceHandlerTest {
    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<StockQuoteHandler>()
        factory.registerDescriptor<OnceHandler>()
        factory.registerDescriptor<TestOnceStrategy>()
        HandlerDescriptorFactory.useFactory(factory)
    }

    @Test
    fun `handles once`() {
        StockQuoteHandler.called = 0
        val handler = (StockQuoteHandler()
                    + OnceHandler()
                    + TestOnceStrategy())
        assertAsync(testName) { done ->
            val getQuote = GetStockQuote("AAPL").once
            handler.send(getQuote) then {
                assertEquals(1, StockQuoteHandler.called)
                handler.send(getQuote)
            } then {
                assertEquals(1, StockQuoteHandler.called)
                handler.send(GetStockQuote("AAPL"))
            } then {
                assertEquals(2, StockQuoteHandler.called)
                done()
            }
        }
    }

    @Test
    fun `handles once suspending`() = runBlocking {
        StockQuoteHandler.called = 0
        val handler = (StockQuoteHandler()
                    + OnceHandler()
                    + TestOnceStrategy())
        val getQuote = GetStockQuote("AAPL").once
        handler.sendCo(getQuote)
        assertEquals(1, StockQuoteHandler.called)
        handler.sendCo(getQuote)
        assertEquals(1, StockQuoteHandler.called)
        handler.sendCo(GetStockQuote("AAPL"))
        assertEquals(2, StockQuoteHandler.called)
    }

    @Test
    fun `rejects once if no strategy found`() {
        StockQuoteHandler.called = 0
        val handler = (StockQuoteHandler()
                    + OnceHandler()
                    + TestOnceStrategy())
        assertAsync(testName) { done ->
            val getQuote = GetStockQuote("ABC").once
            handler.send(getQuote) catch {
                assertTrue(it is NotHandledException)
                assertEquals(0, StockQuoteHandler.called)
                done()
            }
        }
    }

    @Test
    fun `rejects once if no strategy found suspending`() = runBlocking<Unit> {
        StockQuoteHandler.called = 0
        val handler = (StockQuoteHandler()
                    + OnceHandler()
                    + TestOnceStrategy())
        val getQuote = GetStockQuote("ABC").once
        try {
            handler.sendCo(getQuote)
        } catch (t: NotHandledException) {
            assertEquals(0, StockQuoteHandler.called)
        }
    }

    @Test fun `Serializes once request into json`() {
        val request  = GetStockQuote("AAPL").once
        val json     = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Once.Once,Miruken\",\"request\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"},\"requestId\":\"${request.requestId}\"}", json)
    }

    @Suppress("UNUSED_PARAMETER")
    class TestOnceStrategy : Handler(), OnceStrategy {
        private val _requests = mutableSetOf<UUID>()

        @Maps
        fun once(request: GetStockQuote) =
                if (request.symbol == "ABC") null else this

        override fun complete(once: Once, composer: Handling) =
            if (_requests.contains(once.requestId)) {
                Promise.EMPTY
            } else {
                composer.send(once.request, once.requestType) then {
                    _requests.add(once.requestId)
                }
            }
    }
}