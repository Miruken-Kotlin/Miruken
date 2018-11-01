package com.miruken.api.cache

import com.miruken.callback.Handler
import com.miruken.callback.Handles
import com.miruken.concurrent.Promise
import java.util.*

class StockQuoteHandler : Handler() {
    private val random = Random()

    @Handles
    fun getQuote(quote: GetStockQuote): Promise<StockQuote> {
        ++called

        check(called > 1 || quote.symbol != "EX") {
            "Stock Exchange is down"
        }

        return Promise.resolve(StockQuote(
                quote.symbol, random.nextDouble() * 10.0))
    }

    companion object {
        var called = 0
    }
}