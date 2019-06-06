package com.miruken.api.schedule

import com.miruken.api.GetStockQuote
import com.miruken.api.JacksonProvider
import org.junit.Test
import kotlin.test.assertEquals

class ParallelTest {
    @Test fun `Serializes parallel request into json`() {
        val request = Parallel(listOf(GetStockQuote("AAPL")))
        val json    = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Schedule.Parallel,Miruken\",\"requests\":[{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}]}", json)
    }
}