package com.miruken.api

data class StockQuote(
        val symbol: String,
        val value:  Double
): NamedType {
    override val typeName: String = StockQuote.typeName

    companion object : NamedType {
        override val typeName = "StockQuote"
    }
}

data class GetStockQuote(
        val symbol: String
): Request<StockQuote> {
    override val typeName: String = GetStockQuote.typeName

    companion object : NamedType {
        override val typeName = "GetStockQuote"
    }
}
