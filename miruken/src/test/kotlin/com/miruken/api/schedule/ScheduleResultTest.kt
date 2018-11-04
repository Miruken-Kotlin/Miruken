package com.miruken.api.schedule

import com.fasterxml.jackson.module.kotlin.readValue
import com.miruken.api.JacksonHelper
import com.miruken.api.StockQuote
import com.miruken.api.Try
import com.miruken.api.fold
import org.junit.Test
import java.lang.IllegalStateException
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ScheduleResultTest {
    @Test fun `Serializes scheduled result into json`() {
        val result = ScheduleResult(listOf(
                Try.Success(StockQuote("AAPL", 207.48))))
        val json   = JacksonHelper.mapper.writeValueAsString(result)
        assertEquals("{\"responses\":[{\"isLeft\":false,\"value\":{\"\$type\":\"StockQuote\",\"symbol\":\"AAPL\",\"value\":207.48}}]}", json)
    }

    @Test fun `Serializes scheduled error into json`() {
        val error = ScheduleResult(listOf(
                Try.error(Exception("Validation failed"))))
        val json  = JacksonHelper.mapper.writeValueAsString(error)
        assertEquals("{\"responses\":[{\"isLeft\":true,\"value\":{\"message\":\"Validation failed\"}}]}", json)
    }

    @Test fun `Deserializes scheduled result from json`() {
        JacksonHelper.register<StockQuote>("StockQuote")
        val success = ScheduleResult(listOf(
                Try.Success(StockQuote("GOOGL", 1071.49))))
        val json    = JacksonHelper.mapper.writeValueAsString(success)
        val result  = JacksonHelper.mapper.readValue<ScheduleResult>(json)
        assertEquals(1, result.responses.size)
        result.responses[0].fold({
            fail("Expected a successful result")
        }, {
            val stockQuote = it as? StockQuote
            assertNotNull(stockQuote)
            assertEquals("GOOGL", stockQuote.symbol)
            assertEquals(1071.49, stockQuote.value)
        })
    }

    @Test fun `Deserializes scheduled error from json`() {
        val error  = ScheduleResult(listOf(
                Try.error(Exception("Bad stuff"))))
        val json   = JacksonHelper.mapper.writeValueAsString(error)
        val result = JacksonHelper.mapper.readValue<ScheduleResult>(json)
        assertEquals(1, result.responses.size)
        result.responses[0].fold({
            assertEquals("Bad stuff", it.message)
        }, {
            fail("Expected a failure")
        })
    }

    @Test fun `Deserializes mixed scheduled results from json`() {
        JacksonHelper.register<StockQuote>("StockQuote")
        val mixed  = ScheduleResult(listOf(
                Try.Success(StockQuote("MSFT", 106.16)),
                Try.error(IllegalStateException("Missing order"))))
        val json    = JacksonHelper.mapper.writeValueAsString(mixed)
        val result  = JacksonHelper.mapper.readValue<ScheduleResult>(json)
        assertEquals(2, result.responses.size)
        result.responses[0].fold({
            fail("Expected a successful result")
        }, {
            val stockQuote = it as? StockQuote
            assertNotNull(stockQuote)
            assertEquals("MSFT", stockQuote.symbol)
            assertEquals(106.16, stockQuote.value)
        })
        result.responses[1].fold({
            assertEquals("Missing order", it.message)
        }, {
            fail("Expected a failure")
        })
    }
}