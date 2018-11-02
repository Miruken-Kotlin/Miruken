package com.miruken

import com.miruken.api.JacksonHelper
import com.miruken.api.StockQuote
import org.junit.Test
import kotlin.test.assertEquals

class EitherTest {
    @Test fun `Serializes either into json`() {
        val either = Either.Right(StockQuote("AAPL", 10.0))
        val json    = JacksonHelper.json.writeValueAsString(either)
        assertEquals(json, "{\"isLeft\":false,\"value\":{\"\$type\":\"StockQuote\",\"symbol\":\"AAPL\",\"value\":10.0}}")
    }
}