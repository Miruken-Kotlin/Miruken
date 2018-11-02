package com.miruken.api.schedule

import com.miruken.api.JacksonHelper
import com.miruken.api.GetStockQuote
import org.junit.Test
import kotlin.test.assertEquals

class ConcurrentTest {
    @Test fun `Serializes concurrent request into json`() {
        val request = Concurrent(listOf(GetStockQuote("AAPL")))
        val json    = JacksonHelper.json.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Mediate.Schedule.Concurrent,Miruken.Mediate\",\"requests\":[{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}]}", json)
    }
}