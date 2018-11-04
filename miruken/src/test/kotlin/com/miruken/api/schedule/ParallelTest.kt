package com.miruken.api.schedule

import com.miruken.api.JacksonHelper
import com.miruken.api.GetStockQuote
import org.junit.Test
import kotlin.test.assertEquals

class ParallelTest {
    @Test fun `Serializes parallel request into json`() {
        val request = Parallel(listOf(GetStockQuote("AAPL")))
        val json    = JacksonHelper.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Mediate.Schedule.Parallel,Miruken.Mediate\",\"requests\":[{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}]}", json)
    }
}