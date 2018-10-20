package com.miruken.mediate.cache

import com.miruken.mediate.Request

data class StockQuote(
        val symbol: String,
        val value:  Double
)

data class GetStockQuote(val symbol: String): Request<StockQuote>