package com.miruken.api.schedule

import com.miruken.api.JacksonHelper
import com.miruken.api.cache.GetStockQuote
import org.junit.Test
import kotlin.test.assertEquals

class SequentialTest {
    @Test fun `Serializes sequential request`() {
        val request = Sequential(listOf(GetStockQuote("AAPL")))
        val json    = JacksonHelper.json.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Mediate.Schedule.Sequential,Miruken.Mediate\",\"requests\":[{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}]}", json)
    }
}