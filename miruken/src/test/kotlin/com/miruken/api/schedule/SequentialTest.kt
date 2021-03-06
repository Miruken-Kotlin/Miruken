package com.miruken.api.schedule

import com.miruken.api.GetStockQuote
import com.miruken.api.JacksonProvider
import org.junit.Test
import kotlin.test.assertEquals

class SequentialTest {
    @Test fun `Serializes sequential request into json`() {
        val request = Sequential(listOf(GetStockQuote("AAPL")))
        val json    = JacksonProvider.mapper.writeValueAsString(request)
        assertEquals("{\"\$type\":\"Miruken.Api.Schedule.Sequential,Miruken\",\"requests\":[{\"\$type\":\"GetStockQuote\",\"symbol\":\"AAPL\"}]}", json)
    }
}