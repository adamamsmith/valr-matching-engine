package smith.adam.service

import io.ktor.server.plugins.*
import smith.adam.orderbook.model.CancelOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.MarketOrder
import smith.adam.orderbook.model.Side

class OrderValidationService {
    fun validate(marketOrder: MarketOrder, validCurrencyPairs: MutableSet<String>) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(marketOrder.pair, validCurrencyPairs),
                validateMarketOrderAmount(marketOrder)
            )
        )
    }

    fun validate(limitOrder: LimitOrder, validCurrencyPairs: MutableSet<String>) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(limitOrder.pair, validCurrencyPairs),
                validateSide(limitOrder)
            )
        )
    }

    fun validate(cancelOrder: CancelOrder, validCurrencyPairs: MutableSet<String>) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(cancelOrder.pair, validCurrencyPairs)
            )
        )
    }

    fun validate(currencyPair: String?, validCurrencyPairs: MutableSet<String>) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(currencyPair, validCurrencyPairs)
            )
        )
    }

    private fun validateCurrencyPair(currencyPair: String?, validCurrencyPairs: MutableSet<String>): String {
        if (currencyPair == null) return "Market must be provided"
        return if (currencyPair !in validCurrencyPairs) "Market does not exist for pair $currencyPair" else ""
    }

    private fun validateMarketOrderAmount(marketOrder: MarketOrder): String {
        return when (Side.fromString(marketOrder.side)) {
            Side.BUY -> if (marketOrder.quoteAmount == null) "quoteAmount must be provided when side is BUY" else ""
            Side.SELL -> if (marketOrder.baseAmount == null) "baseAmount must be provided when side is SELL" else ""
            null -> "Invalid side: ${marketOrder.side}"
        }
    }

    private fun validateSide(limitOrder: LimitOrder): String {
        return when (Side.fromString(limitOrder.side)) {
            Side.BUY -> ""
            Side.SELL -> ""
            null -> "Invalid side: ${limitOrder.side}"
        }
    }

    private fun buildBadRequestException(messages: List<String>) {
        val filteredMessages = messages.filter { it != "" }
        if (filteredMessages.isNotEmpty()) throw BadRequestException(
            filteredMessages.joinToString(
                separator = ". ",
                postfix = "."
            )
        )
    }
}