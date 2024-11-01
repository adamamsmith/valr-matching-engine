package smith.adam.service

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import smith.adam.model.CancelOrder
import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.model.orderbook.Side

class OrderValidationService {
    fun validate(marketOrder: MarketOrder, validCurrencyPairs: MutableSet<String>) {
        val messages = listOf(
            validateCurrencyPair(marketOrder.pair, validCurrencyPairs), validateMarketOrderAmount(marketOrder)
        )
        buildException(messages, InvalidBodyException::class.java)
    }

    fun validate(limitOrder: LimitOrder, validCurrencyPairs: MutableSet<String>) {
        val messages = listOf(
            validateCurrencyPair(limitOrder.pair, validCurrencyPairs), validateSide(limitOrder)
        )
        buildException(messages, InvalidBodyException::class.java)
    }

    fun validate(cancelOrder: CancelOrder, validCurrencyPairs: MutableSet<String>) {
        val messages = listOf(
            validateCurrencyPair(cancelOrder.pair, validCurrencyPairs)
        )
        buildException(messages, InvalidBodyException::class.java)
    }

    fun validate(currencyPair: String?, validCurrencyPairs: MutableSet<String>) {
        val messages = listOf(
            validateCurrencyPair(currencyPair, validCurrencyPairs)
        )
        buildException(messages, BadRequestException::class.java)
    }

    private fun validateCurrencyPair(currencyPair: String?, validCurrencyPairs: MutableSet<String>): String {
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

    private fun <E : Exception> buildException(messages: List<String>, exceptionClass: Class<E>) {
        if (messages.isNotEmpty()) throw exceptionClass.getConstructor(String::class.java)
            .newInstance(messages.joinToString(separator = ". ", postfix = "."))
    }
}