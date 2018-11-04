package com.miruken.api

data class StockQuote(
        val symbol: String,
        val value:  Double,
        override val typeName: String = StockQuote.typeName
): NamedType {
    companion object : NamedType {
        override val typeName = "StockQuote"
    }
}

data class GetStockQuote(
        val symbol: String,
        override val typeName: String = GetStockQuote.typeName
): Request<StockQuote> {
    companion object : NamedType {
        override val typeName = "GetStockQuote"
    }
}
