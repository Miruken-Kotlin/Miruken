package com.miruken.api.route

import com.miruken.api.*
import com.miruken.api.oneway.oneway
import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.test.assertAsync
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.*

class RoutedTest {
    private lateinit var handler: Handling

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<StockQuoteHandler>()
        factory.registerDescriptor<PassThrough>()
        factory.registerDescriptor<Trash>()
        HandlerDescriptorFactory.useFactory(factory)

        handler = (StockQuoteHandler() +
                   PassThrough() +
                   Trash())
        StockQuoteHandler.called = 0
    }

    @Test fun `Routes request`() {
        assertAsync(testName) { done ->
            handler.send(GetStockQuote("GOOGL")
                    .oneway.routeTo("trash")) then {
                assertNull(it)
                handler.send(GetStockQuote("GOOGL")
                        .routeTo("pass-through")) then { quote ->
                    assertEquals("GOOGL", quote.symbol)
                    assertEquals(1, StockQuoteHandler.called)
                    done()
                }
            }
        }
    }

    @Test fun `Routes request suspending`() = runBlocking {
        val result = handler.sendCo(GetStockQuote("GOOGL").oneway.routeTo("trash"))
        assertNull(result)
        val quote = handler.sendCo(GetStockQuote("GOOGL").routeTo("pass-through"))
        assertEquals("GOOGL", quote.symbol)
        assertEquals(1, StockQuoteHandler.called)
    }

    @Test fun `Fails missing route`() {
        assertAsync(testName) { done ->
            handler.send(GetStockQuote("GOOGL")
                    .routeTo("nowhere")) catch { e ->
                assertTrue(e is NotHandledException)
                done()
            }
        }
    }

    @Test fun `Fails missing route suspending`() = runBlocking<Unit> {
        assertFailsWith<NotHandledException> {
            handler.sendCo(GetStockQuote("GOOGL").routeTo("nowhere"))
        }
    }

    @Test fun `Serializes routed request into json`() {
        val request = GetStockQuote("AAPL").routeTo("http://server/api")
        val json    = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Route.RoutedRequest`1[[StockQuote]],Miruken\",\"message\":{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"},\"route\":\"http://server/api\"}", json)
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

    @Routes("trash")
    class Trash : Handler() {
        @Handles
        fun route(request: Routed): Promise<*> {
            println("Trashed $request")
            return Promise.EMPTY
        }
    }
}