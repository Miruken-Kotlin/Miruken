package com.miruken.api.schedule

import com.miruken.api.JacksonProvider
import com.miruken.api.GetStockQuote
import org.junit.Test
import kotlin.test.assertEquals

class SequentialTest {
    @Test fun `Serializes sequential request into json`() {
        val request = Sequential(listOf(GetStockQuote("AAPL")))
        val json    = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Mediate.Schedule.Sequential,Miruken.Mediate\",\"requests\":[{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}]}", json)
    }
}