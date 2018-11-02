package com.miruken.api

data class StockQuote(
        val symbol: String,
        val value:  Double,
        override val typeName: String = "StockQuote"
): NamedType

data class GetStockQuote(
        val symbol: String,
        override val typeName: String = "GetStockQuote"
): Request<StockQuote>