package com.miruken.api.cache

import com.miruken.api.Request

data class StockQuote(
        val symbol: String,
        val value:  Double
)

data class GetStockQuote(
        val symbol: String,
        override val typeName: String = "GetStockQuote"
): Request<StockQuote>